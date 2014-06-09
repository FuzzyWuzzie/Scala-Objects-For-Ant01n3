package org.sofa.opengl.actor.renderer

import scala.math._
import scala.collection.mutable.HashMap

import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.nio._
import org.sofa.math.{Rgba, Vector3, Vector4, Axes, Point3, NumberSeq3}
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController, MotionEvent, KeyEvent, ScrollEvent}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute}
import org.sofa.opengl.mesh.skeleton.{Bone ⇒ SkelBone}

import akka.actor.{Actor, Props, ActorSystem, ActorRefFactory, ReceiveTimeout, ActorRef}
import scala.concurrent.duration._
import org.sofa.opengl.akka.SurfaceExecutorService


// **** All this is still very experimental ****


/** Represents the messages actors controling the renderer can receive from it. */
object RendererController {
	/** Sent when the renderer started and is ready to render. */
	case class Start(rendererActor:ActorRef)

	/** Request to exit the application: a window or screen is closed by the user. */
	case class Exit()
}


/** Create the renderer actor and associate it with the rendering thread uniquely.
  * Also define all the messages the renderer actor can receive. */
object RendererActor {
	// == Accepted messages ================================

	/** Start the renderer. */
	case class Start()
	
	/** Define a new resource in the renderer. */
	case class AddResource(res:ResourceDescriptor[AnyRef])

	/** Setup pathes and resources from an XML configuration file. */
	case class AddResources(xmlFileName:String)

	/** Add a new screen. The name of the screen is free. */
	case class AddScreen(name:String, screenType:String = "default")

	/** Remove a screen, as well as all its avatars. The screen cannot be the current
	  * one in the renderer. */
	case class RemoveScreen(name:String)
	
	/** Change the current screen. The end message is sent to the current screen and all its avatar. The
	  * begin message is sent to the new screen and all its avatars. */
	case class SwitchScreen(name:String)

	/** Change some value for the current screen. Possible axes depend on the screen type. */
	case class ChangeScreen(state:ScreenState)

	/** Add a new avatar in the current screen. The name of the avatar is free. Its type
	  * depends on the `avatarType`. */
	case class AddAvatar(avatarType:String, name:AvatarName)

	/** Remove an avatar from the current screen. */
	case class RemoveAvatar(name:AvatarName)

	/** Change some value for an avatar. Possible axes depend on the avatar type. */
	case class ChangeAvatar(name:AvatarName, state:AvatarState)

	/** Ask the avatar `name` to send messages to `acquaintance` when something occurs. */
	case class AddAvatarAcquaintance(name:AvatarName, acquaintance:ActorRef)

	/** Ask the renderer to print its status on the console. */
	case class PrintStatus()

	//== Creation ===========================================

	/** Create a new renderer actor (usually only one is needed) that will run all its messages
	  * in the same thread as the one of the renderer object and its underlying OpenGL surface.
	  *
	  * At the same time, configure this renderer object and its surface to their initial
	  * size and eventual title (on desktops).
	  *
	  * The `fps` specifies how often the internal animation loop will be run per second.
	  * This is indicative, and the renderer will try fulfill it to achieve a regular frame
	  * per second rendering. If you put animators in the avatar, they will be run by this
	  * internal temporized loop.
	  *
	  * @param system The actor system.
	  * @param controller An actor that controls the renderer, the renderer actor-ref will be sent to it when ready.
	  * @param renderer The renderer (created depending on the underlying system).
	  * @param title The window title if in a windowed system and not fullscreen.
	  * @param width The initial width in pixels of the surface (if possible).
	  * @param heigth The initial height in pixels of the surface (if possible).
	  * @param fps The requested frames per second (indicative).
	  * @param decorated If in a windowing system add system decorations or not (default no).
	  * @param fullscreen If in a windowing system try to open a full screen surface (default no).
	  * @param overSample If larger than one, try to oversample each pixel by the amount given (default 4).
	  * @param rendererActorName The default is "renderer-actor", but you can change it if needed. There is
	  *        always only one renderer actor. */
	def apply(system:ActorRefFactory,
			  controller:ActorRef,
	          renderer:Renderer,
	          title:String,
	          width:Int,
	          height:Int,
	          fps:Int,
	          decorated:Boolean = false,
	          fullscreen:Boolean = false,
	          overSample:Int = 4,
	          rendererActorName:String = "renderer-actor") {
		renderer.start(title, width, height, fps, decorated, fullscreen, overSample, { () => 
			// This is executed when the renderer is ready. In the OpenGL surface thread.

			// Tell our specific executor service which thread to use (the same as the one of the OpenGL surface used).
			SurfaceExecutorService.setSurface(renderer.surface)
			
			renderer.setController(controller)

			// Create our renderer actor with the specific executor service so that all its messages
			// are executed in the same thread as the OpenGL surface.
			val ractor = system.actorOf(Props(new RendererActor(renderer)).withDispatcher(SurfaceExecutorService.configKey), name=rendererActorName)

			ractor ! Start
		})
	}
}


/** Reprensentation of the rendering system, and unique entry point for it.
  *
  * Control the renderer, you can send messages to this actor to modify the renderer.
  * All the messages of this specific actor are executed in the same thread as
  * the real renderer object. This thread is the graphic thread.
  */
class RendererActor(val renderer:Renderer) extends Actor {
	import RendererActor._

	def receive() = {
		case Start                                    ⇒ renderer.onStart(self)
		case AddScreen(name, screenType)              ⇒ {println("adding screen");renderer.addScreen(name, screenType)}
		case RemoveScreen(name)                       ⇒ renderer.removeScreen(name)
		case SwitchScreen(name)                       ⇒ renderer.switchToScreen(name)
		case AddAvatar(atype, name)                   ⇒ renderer.currentScreen.addAvatar(name, atype)
		case RemoveAvatar(name)                       ⇒ renderer.currentScreen.removeAvatar(name)
		case ChangeScreen(state)                      ⇒ renderer.currentScreen.change(state)
		case ChangeAvatar(name, state)                ⇒ renderer.currentScreen.changeAvatar(name, state)
		case AddAvatarAcquaintance(name, acqaintance) ⇒ renderer.currentScreen.addAvatarAcquaintance(name, acqaintance)
		case AddResource(res)                         ⇒ renderer.libraries.addResource(res)
		case AddResources(xmlFileName)                ⇒ {println("adding resources ${xmlFileName}"); renderer.libraries.addResources(xmlFileName)}
		case PrintStatus                              ⇒ println(renderer)
		case x                                        ⇒ println(s"RendererActor unknown message ${x}")
	}
}