package org.sofa.simu.oberon

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef, ActorContext}
import scala.concurrent.duration._

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

	var cloud = new Array[ActorRef](10)

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

			mountains ! SpriteActor.Start("mountains", rendererActor, gameActor, "image", "intro-moutains", null)
			mountains ! SpriteActor.Move(Point3(0, -0.2, 0))
			mountains ! SpriteActor.Resize(SizeFromScreenWidth(1, "intro-moutains"))

			title = SpriteActor(context, "title")

			title ! SpriteActor.Start("title", rendererActor, gameActor, "image", "intro-title", null)
			title ! SpriteActor.Move(Point3(0, 0.3, 0))
			title ! SpriteActor.Resize(SizeFromScreenWidth(0.7, "intro-title"))

			import scala.math._

			for(i <- 0 until cloud.length) {
				cloud(i) = SpriteActor(context, "cloud%d".format(i))

				val animator = new LineAnimator()
				animator.incr.x = (((random*2)-1)*0.004)
				animator.lo.x = -0.8
				animator.hi.x = 0.8
				animator.pos.y = 0.2 + (((random*2)-1)*0.1)
				animator.pos.x = (((random*2)-1)*0.6)

				cloud(i) ! SpriteActor.Start("cloud%d".format(i), rendererActor, gameActor, "image", "intro-cloud", animator)
				cloud(i) ! SpriteActor.Resize(SizeFromTextureWidth(0.3+(random*0.2), "intro-cloud"))
				//cloud(i) ! SpriteActor.AnimationBehavior(41, (me) => { me.move(animator.nextPos) })
			}
		}
	}
}

class LineAnimator extends ImageSprite.Animator {
	val pos = Point3(0,0,0)
	val incr = Vector3(0,0,0)
	val lo = Point3(0,0,0)
	val hi = Point3(1,1,1)

	def nextSize(time:Long, inOut:NumberSeq3) {}

	def nextPosition(time:Long, inOut:NumberSeq3) {
		pos.x += incr.x 
		if(pos.x > hi.x ) { pos.x = hi.x; incr.x = -incr.x }
		if(pos.x < lo.x)  { pos.x = lo.x; incr.x = -incr.x }
		pos.y += incr.y 
		if(pos.y > hi.y ) { pos.y = hi.y; incr.y = -incr.y }
		if(pos.y < lo.y)  { pos.y = lo.y; incr.y = -incr.y }
		pos.z += incr.z 
		if(pos.z > hi.z ) { pos.z = hi.z; incr.z = -incr.z }
		if(pos.z < lo.z)  { pos.z = lo.z; incr.z = -incr.z }
		
		inOut.copy(pos)
	}

	def positionChanged(p:NumberSeq3) {}
	def sizeChanged(s:NumberSeq3) {}
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

	case class Start(name:String, rActor:ActorRef, gActor:ActorRef, avatarType:String, resTexture:String, animator:ImageSprite.Animator)
	case class Resize(size:Size)
	case class Move(pos:NumberSeq3)
	case class AnimationBehavior(fps:Int, animBehavior:(SpriteActor) => Unit)
	case class Animate()
}

class SpriteActor extends Actor {
	import SpriteActor._

//var T = 0L

	var name:String = null

	var gameActor:ActorRef = null

	var rendererActor:ActorRef = null

	var animationBehavior:(SpriteActor)=>Unit = null

	def resize(newSize:Size) { rendererActor ! RendererActor.ChangeAvatarSize(name, newSize) }

	def move(newPos:NumberSeq3) { rendererActor ! RendererActor.ChangeAvatarPosition(name, newPos) }

	def receive() = {
		case Start(nm, ra, ga, avatarType, res, anim) ⇒ {
			name = nm
			gameActor = ga
			rendererActor = ra

			rendererActor ! RendererActor.AddAvatar(name, avatarType, false)

			if(anim ne null)
			     rendererActor ! RendererActor.ChangeAvatar(name, ImageSprite.AddAnimatedState(res, anim, "default", true))
			else rendererActor ! RendererActor.ChangeAvatar(name, ImageSprite.AddState(res, "default", true))
		}
		case Resize(size) ⇒ { resize(size) }
		case Move(pos) ⇒ { move(pos) }
		case AnimationBehavior(fps, animBehavior) ⇒ {
			animationBehavior = animBehavior
			context.setReceiveTimeout(fps milliseconds)
		}
		case ReceiveTimeout ⇒ {
			if(animationBehavior ne null) {
//				val T1 = System.currentTimeMillis
//				println("time %s = %d".format(name, T1-T))
//				T = T1
				animationBehavior(this)
			} else {
				throw new RuntimeException("animation behavior is null an receive a timeout ??")
			}
		}
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