package org.sofa.simu.oberon

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef}
import org.sofa.math.Axes
import org.sofa.simu.oberon.renderer.{Screen, Avatar, MenuScreen, AvatarFactory, Renderer, RendererActor, NoSuchScreenException, NoSuchAvatarException, ShaderResource, TextureResource}
import org.sofa.opengl.{Shader, Texture}
import org.sofa.opengl.io.collada.{ColladaFile}

class BruceAvatarFactory(val renderer:Renderer) extends AvatarFactory {
	def screenFor(name:String, screenType:String):Screen = {
		screenType match {
			case "menu" => new MenuScreen(name, renderer)
			case _ => throw NoSuchScreenException("cannot create a screen of type %s, unknown type".format(screenType))
		}
	}
	def avatarFor(name:String, avatarType:String):Avatar = {
		avatarType match {
			case _ => throw new NoSuchAvatarException("cannot create an avatar of type %s, unknown type".format(avatarType))
		}
	}
}

object Game {
	case class Start
	case class Exit

	def main(args:Array[String]) {
		val actorSystem = ActorSystem("Bruce")
		val game = actorSystem.actorOf(Props[GameActor], name = "game")

		game ! Game.Start
	}
}

/** The top level actor, starts and control all the others. */
class GameActor extends Actor {
	val renderer = new Renderer(self)

	val rendererActor:ActorRef = RendererActor(context.system, renderer, new BruceAvatarFactory(renderer), "Bruce the miner", 800, 600)

	var menuActor:ActorRef = null

	def receive() = {
		case Game.Start ⇒ {
		    Shader.path      += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
		    Shader.path      += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/es2/"
		    Shader.path      += "shaders/"
	    	Texture.path     += "/Users/antoine/Documents/Programs/SOFA/textures"
	    	Texture.path     += "textures/"
			ColladaFile.path += "/Users/antoine/Documents/Art/Sculptures/Blender/"
			ColladaFile.path += "meshes/"

			rendererActor!RendererActor.AddResource(new ShaderResource("image-shader", "image_shader.vert.glsl", "image_shader.frag.glsl"))
			rendererActor!RendererActor.AddResource(new TextureResource("intro-screen", "bruce_intro_screen.png", false))
			rendererActor!RendererActor.Start(21)
			
			menuActor = context.actorOf(Props[MenuActor], name = "menu")
			
			menuActor!MenuActor.Start(rendererActor)
		}
		case Game.Exit  ⇒ { println("exiting..."); context.stop(self) }
	}

	override def postStop() { sys.exit }
}

object MenuActor {
	case class Start(rendererActor:ActorRef)	
}

/** Represent the game menu. */
class MenuActor extends Actor {

	var rendererActor:ActorRef = null

	def receive() = {
		case MenuActor.Start(rActor) ⇒ {
			rendererActor = rActor
			rendererActor!RendererActor.AddScreen("menu", "menu")
			rendererActor!RendererActor.SwitchScreen("menu")
			rendererActor!RendererActor.ChangeScreenSize(Axes((0., 1.), (0., 1.), (-1., 1.)))
			rendererActor!RendererActor.ChangeScreen("background-image", "intro-screen")
		}
	}
}