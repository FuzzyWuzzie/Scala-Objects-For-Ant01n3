package org.sofa.opengl.actor.renderer

import scala.math._
import scala.collection.mutable.HashMap

import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._

import org.sofa.nio._
import org.sofa.math.{Rgba, Vector3, Vector4, Axes, Point3, NumberSeq3}
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController, MotionEvent, KeyEvent, ScrollEvent}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute}
import org.sofa.opengl.mesh.skeleton.{Bone ⇒ SkelBone}

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef}
import scala.concurrent.duration._
import org.sofa.opengl.akka.SurfaceExecutorService


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
	var factory:RendererFactory = new org.sofa.opengl.actor.renderer.backend.RendererFactoryNewt()

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
  * underlying system, and [[org.sofa.opengl.surface.Surface]] event callbacks
  * `onKey()`, `onMotion()` and `onScroll()`.
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
      * @param overSample Defaults to 4, if set to 1 or less, oversampling is disabled. */
    def start(title:String, initialWidth:Int, initialHeight:Int, fps:Int,
              decorated:Boolean=false, fullscreen:Boolean=false, overSample:Int = 4, runAtInit: () => Unit = null) {
	    this.runAtInit = runAtInit
	    initSurface    = initRenderer
	    frame          = render
	    surfaceChanged = reshape
	    close          = onClose
	    key            = onKey
	    motion         = onMotion
	    scroll         = onScroll
	    surface        = newSurface(this, initialWidth, initialHeight, title, fps,
	    							decorated, fullscreen, overSample)
	}

	/** Set or change the `controlActor`. The old one, if present, is erased. No message is sent to the old or new one. */
	def setController(controlActor:ActorRef) { controller = controlActor }

	/** Only method to implement in descendant classes to create a surface that suits the system. */
	protected def newSurface(renderer:SurfaceRenderer, width:Int, height:Int, title:String, fps:Int, decorated:Boolean, fullscreen:Boolean, overSample:Int):Surface

	/** Completely stop the renderer and release resources. The renderer will
	  * not be reusable. */
	def destroy() {
		switchToScreen(null)
		screens.clear //foreach(removeScreen(_.1))

		initSurface    = null
		frame          = null
		surfaceChanged = null
		close          = null
		key            = null
		motion         = null
		scroll         = null
		screen         = null
		gl             = null

		if(SurfaceExecutorService.isSurface(surface))
			SurfaceExecutorService.setSurface(null)

		surface.destroy

		surface = null
		inited = false
	}

// == Surface events ===========================

	def onStart(rendererActor:ActorRef) {
		this.rendererActor = rendererActor

		if(controller ne null) controller ! RendererController.Start(rendererActor)
	}

	def onClose(surface:Surface) {
		if(controller ne null) controller ! RendererController.Exit

		destroy
	}

	def onScroll(surface:Surface, e:ScrollEvent) {
		if(screen ne null) {
			screen.propagateEvent(NewtAvatarZoomEvent(e))
		}
	}

	def onKey(surface:Surface, e:KeyEvent) {
		if(screen ne null) {
			screen.propagateEvent(NewtAvatarKeyEvent(e))
		}
	}

	protected var prevMotionEvent:MotionEvent = null

	protected var prevMotionEventTime:Long = 0L

	def onMotion(surface:Surface, e:MotionEvent) {
		if(screen ne null) {
			// We used isStart/isEnd to track motion
			// click and long clicks.

			if(prevMotionEvent eq null) {
				//assert(e.isStart)
				prevMotionEvent = e
				prevMotionEventTime = System.currentTimeMillis
			} else {
				if(e.isEnd) {
					if(prevMotionEvent.isStart) {
						val deltaTime = System.currentTimeMillis - prevMotionEventTime
						if(deltaTime >= 1000) {
							// Long click.
							//println("@@ long click")
							screen.propagateEvent(NewtAvatarLongClickEvent(e))
						} else {
							// Click
							//println("@@ click")
							screen.propagateEvent(NewtAvatarClickEvent(e))
						}
					} else {
						// Send end motion
						screen.propagateEvent(NewtAvatarMotionEvent(e))
						//println("@@ motion end")

					}
					prevMotionEvent = null
				} else {
					prevMotionEvent = e
					// Send motion
					//println("@@ motion")
					screen.propagateEvent(NewtAvatarMotionEvent(e))
				}
			}
		}
	}

// == Rendering ================================
    
	def initRenderer(gl:SGL, surface:Surface) {
		this.gl   = gl
		libraries = Libraries(gl)
		inited    = true

		if(runAtInit ne null)
			runAtInit()

		runAtInit = null
	}
	
	def reshape(surface:Surface) { if(screen ne null) { screen.reshape } }
	
	/** Render the current screen. If no screen is current, a red background should be drawn. */
	def render(surface:Surface) {
		animate

		if(screen ne null) {
			screen.render
		} else {
			gl.clearColor(Rgba.Red)
			gl.clear(gl.COLOR_BUFFER_BIT)
		}

	    surface.swapBuffers
	    gl.checkErrors
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


object NewtAvatarClickEvent { def apply(source:MotionEvent) = new NewtAvatarClickEvent(source) }
class NewtAvatarClickEvent(val source:MotionEvent) extends AvatarClickEvent {
	def position:Point3 = Point3(source.x, source.y, 0)
}


object NewtAvatarLongClickEvent { def apply(source:MotionEvent) = new NewtAvatarLongClickEvent(source) }
class NewtAvatarLongClickEvent(val source:MotionEvent) extends AvatarLongClickEvent {
	def position:Point3 = Point3(source.x, source.y, 0)
}


object NewtAvatarMotionEvent { def apply(source:MotionEvent) = new NewtAvatarMotionEvent(source) }
class NewtAvatarMotionEvent(val source:MotionEvent) extends AvatarMotionEvent {
	def position:Point3 = Point3(source.x, source.y, 0)

	def pointerCount:Int = source.pointerCount

	def position(i:Int):Point3 = Point3(source.x(i), source.y(i), 0)

	def isStart:Boolean = source.isStart

	def isEnd:Boolean = source.isEnd

	def pressure:Double = source.pressure

	def pressure(i:Int):Double = source.pressure(i)

}

object NewtAvatarZoomEvent { def apply(source:ScrollEvent) = new NewtAvatarZoomEvent(source) }
class NewtAvatarZoomEvent(val source:ScrollEvent) extends AvatarZoomEvent {
	// TODO probably add a factor...
	def amount:Double = source.amount
}


object NewtAvatarKeyEvent { def apply(source:KeyEvent) = new NewtAvatarKeyEvent(source) }
class NewtAvatarKeyEvent(val source:KeyEvent) extends AvatarKeyEvent {
	def actionChar:org.sofa.opengl.surface.ActionChar.Value = source.actionChar
	def unicodeChar:Char = source.unicodeChar
	def isControlDown:Boolean = source.isControlDown
    def isAltDown:Boolean = source.isAltDown
    def isAltGrDown:Boolean = source.isAltGrDown
    def isShiftDown:Boolean = source.isShiftDown
    def isMetaDown:Boolean = source.isMetaDown
}