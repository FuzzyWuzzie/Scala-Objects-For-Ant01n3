package org.sofa.gfx.renderer

import scala.math._
import scala.collection.mutable.HashMap

import org.sofa.nio._
import org.sofa.math.{Rgba, Vector3, Vector4, Axes, Point3, NumberSeq3}
import org.sofa.gfx.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.gfx.io.collada.{ColladaFile}
import org.sofa.gfx.surface.{Surface, SurfaceRenderer, BasicCameraController}
import org.sofa.gfx.surface.event.{Event, MotionEvent, ActionKeyEvent, UnicodeEvent, ScrollEvent, GestureEvent, ShortCutEvent}
import org.sofa.gfx.mesh.{Mesh, VertexAttribute}
import org.sofa.gfx.mesh.skeleton.{Bone ⇒ SkelBone}

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef}
import scala.concurrent.duration._
import org.sofa.gfx.akka.SurfaceExecutorService

import org.sofa.Timer


/** Base exception for errors when executing a renderer. */
case class RendererException(msg:String) extends Exception(msg)


trait RendererFactory {
	/** Create a new renderer. */
	def newRenderer(avatarFactory:AvatarFactory):Renderer
}


/** Renderer companion object. By default, creates renderers for desktop systems.
  * Override for other kinds or renderers. Change the `factory` field to create
  * renderers for other systems. */
object Renderer {
	var factory:RendererFactory = new org.sofa.gfx.renderer.backend.RendererFactoryNewt()

	def apply(avatarFactory:AvatarFactory=null):Renderer = factory.newRenderer(avatarFactory)
}


/** A rendering service.
  *
  * This manages the communication with the OpenGL surface, renders each
  * screen and their set of avatars and animate them. This starts the
  * rendering process and controls the whole rendering system.
  *
  * This renderer is maintained by a `RendererActor`. However, it can be used
  * without it.
  *
  * The `controler` is an actor that will receive events from the renderer and
  * renderer actor. The events are defined in the [[RendererControler]]
  * object.
  *
  * A renderer can be seen as a set of screens. It has one current screen (or
  * none), and can switch between them. Only one screen is visible at a time.
  * Then recursively, screens can be seen as a set of avatars. See [[Screen]]
  * and [[Avatar]].
  *
  * This renderer class is abstract, since it depends on a kind of surface and
  * can adapt to various platforms. The only methods to implement are
  * `newSurface()` which will create a surface depending on the type of
  * underlying system, and [[org.sofa.gfx.surface.Surface]] event callbacks
  * `onKey()`, `onMotion()` and `onScroll()`.
  *
  * The renderer by default will invoke its render method continuously to achieve
  * the given frame-rate. However you can ask to render only when needed. In
  * this render mode, the renderer will trigger a render only when the surface
  * changed, or if the `requestRender()` method has been called. Avatars and
  * screens are responsible for calling this method. Some avatar libraries are
  * done for continuous rendering (games or simulations for example), others for
  * rendering on demand (UI for example).
  *
  * Be very careful. Excepted the creation phase and the `start()` method, the
  * renderer is made to run in the same thread as its surface. Methods are not
  * synchronized for efficiency reasons, but you should call them only from
  * the surface thread (see the [[Surface]] `invoke()` methods). Most of the
  * time you must use the Controler actor to run the renderer, this actor
  * using a scheduler that uses the surface thread. However you can also pass
  * code to the `start()` method that will be run by the renderer just after
  * its initialization, this is the `runAtInit` parameter:
  *
  *   renderer.start("title", 640, 480, runAtInit = { () =>
  *       // your code here.
  *       // run after init of the renderer.
  *       // inside the surface thread.
  *   })
  */
abstract class Renderer(var factory:AvatarFactory = null) extends SurfaceRenderer {
// General

	/** OpenGL. */    
    var gl:SGL = null

    /** Frame buffer. */
    var surface:Surface = null

	/** Set of screens. */
	protected[this] val screens = new HashMap[String,Screen]()

	/** Current screen. */
	protected[this] var screen:Screen = null

	/** Code to run upon initialization. */
	protected[this] var runAtInit: () => Unit = null

	/** True as soon as initialized. */
	protected[this] var inited:Boolean = false

	/** An actor that is controlling the renderer actor and this renderer. Can be null if the renderer
	  * is used as a stand-alone renderer. */
	protected[this] var controller:ActorRef = null

	/** An actor that controls the renderer and runs in the same thread. Can be null if the renderer
	  * is used as a stand-alone renderer. */
	protected[this] var rendererActor:ActorRef = null

	/** The actual rendering mode, either continuous, or as needed.
	  * Render as needed is done for example if the surface is resized,
	  * its contents damaged, or something changed in the screen or
	  * avatar hierarchy. */
	protected[this] var continuousRender = true

	/** A flag used in "render-when-needed" mode to ask for a new render.
	  * This flag is set to true in the following situations (this list is
	  * exaustive and should be kept up to date):
	  *   - The surface size changed.
	  *   - The `requestRender` method has been called.
	  */
	protected[this] var dirty = true

	if(factory eq null)
		factory = new DefaultAvatarFactory()
   
// == Resources ==============================

	/** The set of resources. */
	var libraries:Libraries = null

// == Init. ==================================
        
    /** Initialize the surface with its size, and optionnal title (on desktops). Once finished,
      * we have an OpenGL context and a frame buffer. The `fps` parameter tells the renderer
      * internal animation loop will try to draw as much frames per second. This internal loop
      * is responsible for running the avatars and screens animate methods and redrawing the screen.
      *
      * This method is called when building the renderer actor, see `RendererActor.apply()`.
      *
      * @param title The title of the window if windowed.
      * @param initialWidth The requested width in pixels of the surface.
      * @param initialHeight The requested height in pixels of the surface.
      * @param fps The requested frames-per-second, the renderer will try to match this.
      * @param decorated If false, no system borders are drawn around the window (if in a windowed system).
      * @param fullscreen If in a windowed system, try to open a full screen surface.
      * @param continuous Ask for continuous rendering at given fps or render as needed.
      * @param overSample Defaults to 4, if set to 1 or less, oversampling is disabled. */
    def start(title:String, initialWidth:Int, initialHeight:Int, fps:Int,
              decorated:Boolean=false, fullscreen:Boolean=false, continuous:Boolean=true, overSample:Int = 4, runAtInit: () => Unit = null) {
	    this.runAtInit   = runAtInit
	    initSurface      = initRenderer
	    frame            = render
	    surfaceChanged   = reshape
	    close            = onClose
	    actionKey        = onActionKey
	    unicode          = onUnicode
	    motion           = onMotion
	    gesture          = onGesture
	    shortCut         = onShortCut
	    continuousRender = continuous
	    surface          = newSurface(this, initialWidth, initialHeight, title, fps,
	    							decorated, fullscreen, overSample)
	}

	/** Set or change the `controlActor`. The old one, if present, is erased. No message is sent to the old or new one. */
	def setController(controlActor:ActorRef) { controller = controlActor }

	/** Only method to implement in descendant classes to create a surface that suits the system. */
	protected def newSurface(renderer:SurfaceRenderer, width:Int, height:Int, title:String, fps:Int, decorated:Boolean, fullscreen:Boolean, multiSample:Int):Surface

	/** Completely stop the renderer and release resources. The renderer will
	  * not be reusable. */
	def destroy() {
		switchToScreen(null)
		screens.clear //foreach(removeScreen(_.1))

		initSurface    = null
		frame          = null
		surfaceChanged = null
		close          = null
		actionKey      = null
		unicode        = null
		motion         = null
		gesture        = null
		shortCut       = null
		screen         = null
		gl             = null

		if(SurfaceExecutorService.isSurface(surface))
			SurfaceExecutorService.setSurface(null)

		surface.destroy

		surface = null
		inited = false

		if(controller ne null) controller ! RendererController.Stopped

		controller = null
	}

// == Surface events ===========================

	def onStart(rendererActor:ActorRef) {
		this.rendererActor = rendererActor

		if(controller ne null) controller ! RendererController.Started(rendererActor)
	}

	def onClose(surface:Surface) {
		if(controller ne null) controller ! RendererController.Closed
		//destroy
	}

	def onActionKey(surface:Surface, e:ActionKeyEvent) {
		if(screen ne null)
			screen.propagateEvent(e)
	}

	def onUnicode(surface:Surface, e:UnicodeEvent) {
		if(screen ne null)
			screen.propagateEvent(e)
	}

	def onMotion(surface:Surface, e:MotionEvent) {
		if(screen ne null)
			screen.propagateEvent(e)
	}

	def onGesture(surface:Surface, e:GestureEvent) {
		if(screen ne null)
			screen.propagateEvent(e)
	}

	def onShortCut(surface:Surface, e:ShortCutEvent) {
		if(screen ne null)
			screen.propagateEvent(e)
	}

// == Rendering ================================
    
	def initRenderer(gl:SGL, surface:Surface) {
		this.gl   = gl
		libraries = Libraries(gl)
		inited    = true
		dirty     = true

		if(runAtInit ne null)
			runAtInit()

		runAtInit = null
	}
	
	def reshape(surface:Surface) { if(screen ne null) { screen.reshape }; dirty = true }
	
	/** Enable or disable continuous rendering. If enabled the renderer
	  * renders the current screen at a given frame rate (see contructor).
	  * Else render only when the rendering surface needs it or something
	  * changed in the screen and avatar hierarchy. On by default. */
	def continuousRenderMode(on:Boolean) {
		continuousRender = on
	}

	/** If not in continuous rendering mode, request the renderer to do a
	  * render at next frame slot. Never call render directly, use this method
	  * instead. */
	def requestRender() { dirty = true }

	/** Render the current screen. If no screen is current, a red background is drawn.
	  * This method is automatically called, never call it directly. See `requestRender()`. */
	def render(surface:Surface):Boolean = {
		var swap = false

Timer.timer.measure("Renderer.animate") {
		animate
}
Timer.timer.measure("Renderer.render") {
		if(screen ne null) {
			if(continuousRender) {
				screen.render
				swap = true
			} else {
				if(dirty) {
					dirty = false	// Before in case a sub-avatar resquest a render during the rendering process.
					screen.render
					swap = true
				}
			}
		} else {
			gl.clearColor(Rgba.Red)
			gl.clear(gl.COLOR_BUFFER_BIT)
		}

	    gl.checkErrors
}
		swap
	}

	def animate() { if(screen ne null) screen.animate }

	/** True as soon as rendering is possible. */
	def isInitialized:Boolean = inited

// == Screens ====================================

	/** Number of screens. */
	def screenCount:Int = screens.size

	/** Does screen `name` exists ? */
	def hasScreen(name:String):Boolean = screens.get(name) match {
		case Some(screen) => true
		case None         => false
	}

	/** Access to a given screen. */
	def screen(name:String):Option[Screen] = screens.get(name)

	def addScreen(name:String, screenType:String = "default") {
		if(!screens.contains(name)) {
			screens += (name -> factory.screenFor(name, this, screenType))
		} else {
			throw RendererException("Cannot add screen %s, already present".format(name))
		}
	}

	def removeScreen(name:String) {
		val s = screens.get(name).getOrElse(throw NoSuchScreenException("cannot remove non existing screen %s".format(name)))
		
		if(s ne screen) {
			screens -= s.name
		} else {
			throw RendererException("cannot remove screen %s since it is current".format(name))
		}
	}

	def switchToScreen(name:String) {
		if(screen ne null) { screen.end }
		
		screen = screens.get(name).getOrElse(null)

		if(screen ne null) { screen.begin }

		dirty = true
	}

	/** True if the renderer has a current screen. */
	def hasCurrentScreen:Boolean = (screen ne null)

	/** The current screen or a NoSuchScreenException if no screen is current. */
	def currentScreen():Screen = {
		if(screen ne null) {
			screen
		} else {
			throw NoSuchScreenException("renderer has no current screen")
		}
	}

// == Utility ===================================

	override def toString():String = {
		val result = new StringBuilder()

		result ++= "renderer (%d screens)%n".format(screens.size)
		result ++= "libraries :%n%s".format(libraries.toString)

		result.toString
	}
}
