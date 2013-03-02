package org.sofa.simu.oberon

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef, ActorContext}
import org.sofa.math.{Axes, Vector3, Point3, NumberSeq3}
import org.sofa.simu.oberon.renderer.{Screen, Avatar, MenuScreen, AvatarFactory, Renderer, RendererActor, NoSuchScreenException, NoSuchAvatarException, ShaderResource, TextureResource, ImageSprite, Size, SizeTriplet, SizeFromTextureHeight, SizeFromTextureWidth, SizeFromScreenWidth}
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
	import Game._

	val renderer = new Renderer(self)

	val rendererActor:ActorRef = RendererActor(context.system, renderer, new BruceAvatarFactory(renderer), "Bruce the miner", 1280, 800)

	var menuActor:ActorRef = null

	def receive() = {
		case Start ⇒ {
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
			rendererActor ! AddResource(TextureResource("intro-play", "play.png", false))
			rendererActor ! AddResource(TextureResource("intro-quit", "quit.png", false))
			rendererActor ! AddResource(TextureResource("intro-quit-broken", "quit_broken.png", false))
			rendererActor ! AddResource(TextureResource("intro-title", "title.png", false))
			rendererActor ! AddResource(TextureResource("intro-moutains", "mountains.png", false))
			rendererActor ! AddResource(TextureResource("intro-cloud", "cloud.png", false))
			rendererActor ! Start(21)
			
			menuActor = context.actorOf(Props[MenuActor], name = "menu")
			
			menuActor!MenuActor.Start(rendererActor, self)
		}
		case Exit  ⇒ { println("exiting..."); context.stop(self) }
	}

	override def postStop() { sys.exit }
}

object MenuActor {
	case class Start(rendererActor:ActorRef, gameActor:ActorRef)	
}

/** Represent the game menu. */
class MenuActor extends Actor {
	import MenuActor._

	var rendererActor:ActorRef = null

	var gameActor:ActorRef = null

	var playButton:ActorRef = null

	var quitButton:ActorRef = null

	var mountains:ActorRef = null

	var title:ActorRef = null

	var cloud:ActorRef = null

	def receive() = {
		case Start(rActor, gActor) ⇒ {
			import RendererActor._

			rendererActor = rActor
			gameActor     = gActor

			rendererActor ! AddScreen("menu", "menu")
			rendererActor ! SwitchScreen("menu")
			rendererActor ! ChangeScreenSize(Axes((-.5, .5), (-.5, .5), (-1., 1.)))
			rendererActor ! ChangeScreen("background-image", "screen-intro")

			playButton = ButtonActor(context, "play")

			playButton ! ButtonActor.Start("play", rendererActor, gameActor, "image")
			playButton ! ButtonActor.Move(Point3(0, 0, 0))
			playButton ! ButtonActor.Resize(SizeFromTextureHeight(0.1, "intro-play"))
			playButton ! ButtonActor.DefineState("intro-play", ButtonActor.State.Normal, true)
			playButton ! ButtonActor.TouchBehavior((me,isStart,isEnd) => { Console.err.println(s"${me.name} coucou") })
			
			quitButton = ButtonActor(context, "quit")

			quitButton ! ButtonActor.Start("quit", rendererActor, gameActor, "image")
			quitButton ! ButtonActor.Move(Point3(0, -0.15, 0))
			quitButton ! ButtonActor.Resize(SizeFromTextureHeight(0.1, "intro-quit"))
			quitButton ! ButtonActor.DefineState("intro-quit", ButtonActor.State.Normal, true)
			quitButton ! ButtonActor.DefineState("intro-quit-broken", ButtonActor.State.Activated, false)
			quitButton ! ButtonActor.TouchBehavior((me,isStart,isEnd) => { if(isStart) {
					me.changeState(ButtonActor.State.Activated)
				} else if(isEnd) {
					me.changeState(ButtonActor.State.Normal)
				}
			})

			mountains = SpriteActor(context, "mountains")

			mountains ! SpriteActor.Start("mountains", rendererActor, gameActor, "image", "intro-moutains")
			mountains ! SpriteActor.Move(Point3(0, -0.2, 0))
			mountains ! SpriteActor.Resize(SizeFromScreenWidth(1, "intro-moutains"))

			title = SpriteActor(context, "title")

			title ! SpriteActor.Start("title", rendererActor, gameActor, "image", "intro-title")
			title ! SpriteActor.Move(Point3(0, 0.3, 0))
			title ! SpriteActor.Resize(SizeFromScreenWidth(0.7, "intro-title"))

			cloud = SpriteActor(context, "cloud")

			cloud ! SpriteActor.Start("cloud", rendererActor, gameActor, "image", "intro-cloud")
			cloud ! SpriteActor.Move(Point3(-0.5, 0.25, 0))
			cloud ! SpriteActor.Resize(SizeFromTextureWidth(0.4, "intro-cloud"))

		}
		case Acquaintance.TouchEvent(from,isStart,isEnd) ⇒ {
			import RendererActor._
			from match {
				case "play" => {}
				case "quit" => { rendererActor ! ChangeAvatar("quit", ImageSprite.ChangeState("activated")); /*gameActor ! Game.Exit*/ }
				case _ => { Console.err.println("screen caugth a touch on %s".format(from)) }
			}
		}
	}
}

// == Acquaintance ==========================================================================================================

/** Set of messages sent back by avatars when something occur on them. */
object Acquaintance {
	/** An avatar has been touched. */
	case class TouchEvent(from:String, isStart:Boolean, isEnd:Boolean)	
}

// == Sprite ================================================================================================================

object SpriteActor {
	def apply(context:ActorContext, name:String):ActorRef = context.actorOf(Props[SpriteActor], name) 

	case class Start(name:String, rActor:ActorRef, gActor:ActorRef, avatarType:String, resTexture:String)
	case class Resize(size:Size)
	case class Move(pos:NumberSeq3)
}

class SpriteActor extends Actor {
	import SpriteActor._

	var name:String = null

	var gameActor:ActorRef = null

	var rendererActor:ActorRef = null

	def receive() = {
		case Start(nm, ra, ga, avatarType, res) ⇒ {
			name = nm
			gameActor = ga
			rendererActor = ra

			rendererActor ! RendererActor.AddAvatar(name, avatarType, false)
			rendererActor ! RendererActor.ChangeAvatar(name, ImageSprite.AddState(res, "default", true))
		}
		case Resize(size) ⇒ { rendererActor ! RendererActor.ChangeAvatarSize(name, size) }
		case Move(pos) ⇒ { rendererActor ! RendererActor.ChangeAvatarPosition(name, pos) }
	}
}

// ==  Button ===============================================================================================================

object ButtonActor {
	object State extends Enumeration {
		type State = Value
		val Normal = Value
		val Activated = Value
	}
	import State._

	def apply(context:ActorContext, name:String):ActorRef = context.actorOf(Props[ButtonActor], name)

	case class Start(name:String, rActor:ActorRef, gActor:ActorRef, avatarType:String)
	case class Resize(size:Size)
	case class Move(pos:NumberSeq3)
	case class DefineState(res:String, state:State, change:Boolean)
	case class ChangeState(state:State)
	case class TouchBehavior(touchBehavior:(ButtonActor,Boolean,Boolean) => Unit)
}

class ButtonActor extends Actor {
	import ButtonActor._
	import ButtonActor.State._

	/** Me. */
	var name:String = null

	/** The game actor. */
	var gameActor:ActorRef = null

	/** The renderer actor. */
	var rendererActor:ActorRef = null

	/** What to do when touched. */
	var touchBehavior:(ButtonActor,Boolean,Boolean)=>Unit = null

	/** Change the state and the avatar representation. */
	def changeState(st:State) {
		 rendererActor ! RendererActor.ChangeAvatar(name, ImageSprite.ChangeState(st.toString))
	}

	def receive() = {
		case Start(nm, rActor, gActor, avatarType) ⇒ {
			name = nm
			gameActor = gActor
			rendererActor = rActor

			rendererActor ! RendererActor.AddAvatar(name, avatarType, true)
			rendererActor ! RendererActor.AddAvatarAcquaintance(name, self)	
		}
		
		case Resize(size) ⇒ { rendererActor ! RendererActor.ChangeAvatarSize(name, size) }

		case Move(pos) ⇒ { rendererActor ! RendererActor.ChangeAvatarPosition(name, pos) }
		
		case DefineState(res,state,change) ⇒ { rendererActor ! RendererActor.ChangeAvatar(name, ImageSprite.AddState(res, state.toString, change)) }
		
		case ChangeState(state) ⇒ { changeState(state) }
		
		case TouchBehavior(behavior) ⇒ { touchBehavior = behavior }

		case Acquaintance.TouchEvent(from,isStart,isEnd) ⇒ {
			import RendererActor._
			from match {
				case nm:String => { if(nm == name) {
					if(touchBehavior ne null) {
						touchBehavior(this, isStart, isEnd)
					} else {
						Console.err;println("caught a touch from %s (%b %b), touch behavior is undefined".format(from,isStart,isEnd))
					}
				}else {
					Console.err;println("uncaught a touch from %s %b %b".format(from,isStart,isEnd))
				} }
				case _ => { Console.err.println("button %s caugth a touch from %s ??".format(name, from)) }
			}
		}
	}
}