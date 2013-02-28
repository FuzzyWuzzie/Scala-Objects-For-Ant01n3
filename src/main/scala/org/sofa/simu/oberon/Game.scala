package org.sofa.simu.oberon

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef}
import org.sofa.math.{Axes, Vector3}
import org.sofa.simu.oberon.renderer.{Screen, Avatar, MenuScreen, AvatarFactory, Renderer, RendererActor, NoSuchScreenException, NoSuchAvatarException, ShaderResource, TextureResource, ImageSprite, SizeTriplet, SizeFromTextureHeight}
import org.sofa.opengl.{Shader, Texture}
import org.sofa.opengl.io.collada.{ColladaFile}

class BruceAvatarFactory(val renderer:Renderer) extends AvatarFactory {
	def screenFor(name:String, screenType:String):Screen = {
		screenType match {
			case "menu" ⇒ new MenuScreen(name, renderer)
			case _ ⇒ throw NoSuchScreenException("cannot create a screen of type %s, unknown type".format(screenType))
		}
	}
	def avatarFor(name:String, avatarType:String, indexed:Boolean):Avatar = {
		avatarType match {
			case "image" ⇒ new ImageSprite(name, renderer.screen, indexed)
			case _ ⇒ throw new NoSuchAvatarException("cannot create an avatar of type %s, unknown type".format(avatarType))
		}
	}
}

// Lots of questions
// How to extract the non specific code of this ?
// Almost all seems to be specific to each game (excepted the renderer part that is in its own package)
// Maybe create utility traits that provide some aspects allowing to automatize some tasks ?

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

	val rendererActor:ActorRef = RendererActor(context.system, renderer, new BruceAvatarFactory(renderer), "Bruce the miner", 1280, 800)

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

			import RendererActor._
			
			rendererActor ! AddResource(ShaderResource("image-shader", "image_shader.vert.glsl", "image_shader.frag.glsl"))
			rendererActor ! AddResource(ShaderResource("plain-shader", "plain_shader.vert.glsl", "plain_shader.frag.glsl"))
			rendererActor ! AddResource(TextureResource("screen-intro", "bruce_intro_screen.png", false))
			rendererActor ! AddResource(TextureResource("button-play", "play.png", false))
			rendererActor ! Start(21)
			
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
			import RendererActor._
			rendererActor = rActor

			rendererActor ! AddScreen("menu", "menu")
			rendererActor ! SwitchScreen("menu")
			rendererActor ! ChangeScreenSize(Axes((-.5, .5), (-.5, .5), (-1., 1.)))

			rendererActor ! AddAvatar("play", "image", true)
			rendererActor ! ChangeAvatar("play", "image", "button-play")
			rendererActor ! ChangeAvatarSize("play", SizeFromTextureHeight(0.1, "button-play"))

			rendererActor ! ChangeScreen("background-image", "screen-intro")
		}
	}
}