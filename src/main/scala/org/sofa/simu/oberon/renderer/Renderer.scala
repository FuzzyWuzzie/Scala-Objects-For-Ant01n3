package org.sofa.simu.oberon.renderer

import scala.math._
import scala.collection.mutable.HashMap

import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.nio._
import org.sofa.math.{Rgba, Vector3, Vector4, Axes, Point3, NumberSeq3}
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController, MotionEvent, KeyEvent, ScrollEvent}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute}
import org.sofa.opengl.mesh.skeleton.{Bone ⇒ SkelBone}

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef}
import scala.concurrent.duration._
import org.sofa.simu.oberon.SurfaceExecutorService

import org.sofa.simu.oberon.{Game}

case class RendererException(msg:String) extends Exception(msg)

// ================================================================================================================
// The renderer actor.

trait Size {}

case class SizeTriplet(x:Double, y:Double, z:Double) extends Size {}

case class SizeFromTextureWidth(scale:Double, fromTexture:String) extends Size {}

case class SizeFromTextureHeight(scale:Double, fromTexture:String) extends Size {}

/** Create the renderer actor and associate it with the rendering thread uniquely. */
object RendererActor {
	// == Accepted messages ================================

	/** Start the renderer animation loop. */
	case class Start(fps:Int)
	
	/** Define a new resource in the renderer. */
	case class AddResource(res:ResourceDescriptor[AnyRef])

	/** A a new avatar in the current screen. The name of the avatar is free. Its type
	  * depends on the [[AvatarFactory]], and the indexed flag tells if the screen must
	  * index the avatar position to send it touch events.  */
	case class AddAvatar(name:String, avatarType:String, indexed:Boolean)
	
	/** Add a new screen. The name of the screen is free. The type depends on
	  * the [[AvatarFactory]]. */
	case class AddScreen(name:String, screenType:String)
	
	/** Change the current screen. The end message is sent to the current screen and all its avatar. The
	  * begin message is sent to the new screen and all its avatars. */
	case class SwitchScreen(name:String)
	
	/** Change the current screen user size. This set the environment space where avatars can be positionned. */
	case class ChangeScreenSize(axes:Axes)

	/** Change some value for a screen. Possible axes depend on the screen type. */
	case class ChangeScreen(axis:String, values:AnyRef*)

	/** Change an avatar position. */
	case class ChangeAvatarPosition(name:String, newPos:NumberSeq3)

	/** Change an avatar size. */
	case class ChangeAvatarSize(name:String, newSize:Size)

	/** Change some value for an avatar. Possible axes depend on the avatar type. */
	case class ChangeAvatar(name:String, state:AvatarState)

	/** Ask the avatar `name` to send messages to `acquaintance` when something occurs. */
	case class AddAvatarAcquaintance(name:String, acqaintance:ActorRef)

	//== Creation ===========================================

	/** Create a new renderer actor (usually only one is needed) that will run all its messages
	  * in the same thread as the one of the renderer object and its underlying OpenGL surface.
	  * At the same time, configure this renderer object and its surface to their initial
	  * size and eventual title (on desktops). */
	def apply(system:ActorSystem, renderer:Renderer, avatarFactory:AvatarFactory, title:String, width:Int, height:Int):ActorRef = {
		renderer.start(title, width, height)
		// Tell our specific executor service which thread to use (the same as the one of the OpenGL surface used).
		SurfaceExecutorService.setSurface(renderer.surface)
		// Create our renderer actor with the specific executor service so that all its messages
		// are executed in the same thread as the OpenGL surface.
		system.actorOf(Props(new RendererActor(renderer, avatarFactory)).withDispatcher("oberon.surface-dispatcher"), name="renderer-actor")
	}
}

/** Control the renderer, you can send messages to this actor to modify the renderer.
  * All the messages of this specific actor are executed in the same thread as
  * the real renderer object. */
class RendererActor(val renderer:Renderer, val avatarFactory:AvatarFactory) extends Actor {
	import RendererActor._

	def receive() = {
		case Start(fps) ⇒ {
			context.setReceiveTimeout(fps milliseconds)
		}
		case AddScreen(name, stype) ⇒ {
			renderer.addScreen(name, avatarFactory.screenFor(name, stype))
		}
		case SwitchScreen(name) ⇒ {
			renderer.switchToScreen(name)
		}
		case ChangeScreenSize(axes) ⇒ {
			renderer.currentScreen.changeAxes(axes)
		}
		case AddAvatar(name,atype,indexed) ⇒ {
			renderer.currentScreen.addAvatar(name, avatarFactory.avatarFor(name, atype, indexed)) 
		}
		case ChangeScreen(axis,values) ⇒ {
			renderer.currentScreen.change(axis, values)
		}
		case ChangeAvatarPosition(name, newPos) ⇒ {
			renderer.currentScreen.changeAvatarPosition(name, newPos)
		}
		case ChangeAvatarSize(name, newSize) ⇒ {
			renderer.currentScreen.changeAvatarSize(name, renderer.toTriplet(newSize))
		}
		case ChangeAvatar(name, state) ⇒ {
			renderer.currentScreen.changeAvatar(name, state)
		}
		case AddAvatarAcquaintance(name,acqaintance) ⇒ {
			renderer.currentScreen.addAvatarAcquaintance(name, acqaintance)
		}
		case AddResource(res) ⇒ {
			renderer.libraries.addResource(res)
		}
		case ReceiveTimeout ⇒ {
			renderer.animate
		}
	}
}

// ==============================================================================================================
// The renderer class

/** The real rendering service. This manages the communication with
  * the OpenGL surface and renders each screen and their set of avatars
  * and animate them. */
class Renderer(val gameActor:ActorRef) extends SurfaceRenderer {
// General

	/** OpenGL. */    
    var gl:SGL = null

    /** Frame buffer. */
    var surface:Surface = null

	/** Set of screens. */
	val screens = new HashMap[String,Screen]()

	/** Current screen. */
	var screen:Screen = null
   
// == Resources ==============================

	/** The set of shaders. */
	var libraries:Libraries = null

// == Init. ==================================
        
    /** Initialize the surface with its size, and optionnal title (on
      * desktops). Once finished, we have an OpenGL context and a
      * frame buffer. */
    def start(title:String, initialWidth:Int, initialHeight:Int) {
	    val caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2))
	    
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(4)

	    initSurface    = initRenderer
	    frame          = render
	    surfaceChanged = reshape
	    close          = { surface ⇒ gameActor ! Game.Exit }
	    key            = onKey
	    motion         = onMotion
	    scroll         = onScroll
	    surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
	    					initialWidth, initialHeight, title, caps,
	    					org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2, false/*undecorated*/)

	    libraries = Libraries(gl)
	}

	/** The current screen or a NoSuchScreenException if no screen is current. */
	def currentScreen():Screen = {
		if(screen ne null) {
			screen
		} else {
			throw NoSuchScreenException("renderer has no current screen")
		}
	}

// == Surface events ===========================

	def onScroll(surface:Surface, e:ScrollEvent) {} 
	def onKey(surface:Surface, e:KeyEvent) {}
	def onMotion(surface:Surface, e:MotionEvent) {
		if(screen ne null) {
			screen.motion(e)
		} 
	}

// == Rendering ================================
    
	def initRenderer(gl:SGL, surface:Surface) {
		this.gl = gl
	}
	
	def reshape(surface:Surface) {
		if(screen ne null) {
			screen.reshape
		}
	}
	
	def render(surface:Surface) {
		if(screen ne null) {
			screen.render
		} else {
			gl.clearColor(Rgba.black)
			gl.clear(gl.COLOR_BUFFER_BIT)
		}

	    surface.swapBuffers
	    gl.checkErrors
	}

	def animate() {
		if(screen ne null) {
			screen.animate
		}
	}

// == Screens ====================================

	def addScreen(name:String, screen:Screen) {
		if(!screens.contains(name)) {
			screens += (name -> screen)
		} else {
			throw RendererException("Cannot add screen %s, already present".format(name))
		}
	}

	def removeScreen(name:String) {
		screens -= name
	}

	def switchToScreen(name:String) {
		if(screen ne null) { screen.end }
		
		screen = screens.get(name).getOrElse(null)

		if(screen ne null) { screen.begin }
	}

// == Utility ===================================

	/** Transforms a Size into a triplet of values. */
	def toTriplet(sz:Size):NumberSeq3 = {
		sz match {
			case fromTex:SizeFromTextureWidth => {
				val tex = libraries.textures.get(gl, fromTex.fromTexture)
				val ratio = (tex.height.toDouble / tex.width.toDouble)
				Vector3(fromTex.scale, fromTex.scale * ratio, 0.01)
			}
			case fromTex:SizeFromTextureHeight => {
				val tex = libraries.textures.get(gl, fromTex.fromTexture)
				val ratio = tex.ratio
				Vector3(fromTex.scale * ratio, fromTex.scale, 0.01)
			}
			case triplet:SizeTriplet => { Vector3(triplet.x, triplet.y, triplet.z) }
		}
	}
}