package org.sofa.simu.oberon

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef, ActorContext, Terminated}
import scala.collection.mutable.HashMap
import scala.concurrent.duration._

import org.sofa.math.{Axes, Vector3, Point3, NumberSeq3}
import org.sofa.simu.oberon.renderer.{Screen, Avatar, AvatarFactory, Renderer, RendererActor, NoSuchScreenException, NoSuchAvatarException, ShaderResource, TextureResource, Size, SizeTriplet, SizeFromTextureHeight, SizeFromTextureWidth, SizeFromScreenWidth}
import org.sofa.simu.oberon.renderer.screen.{MenuScreen, TileScreen}
import org.sofa.simu.oberon.renderer.sprite.{ImageSprite, TilesSprite}
import org.sofa.opengl.{Shader, Texture}
import org.sofa.opengl.io.collada.{ColladaFile}

object GameMap {
	val maps = new HashMap[String,GameMap]()
	def apply(texres:String, size:(Double,Double), pos:(Double,Double),
		actions:Array[TilesSprite.StateChangeAction]):GameMap = new GameMap(texres, size, pos, actions)
}

class GameMap(val texres:String, val size:(Double,Double), val pos:(Double,Double),
	val actions:Array[TilesSprite.StateChangeAction]) {}

class Level1MapMud {
	import TilesSprite._

	GameMap.maps += "mud-tile-sprite" -> GameMap("tile-mud",
		(28, 20), (-14, -10),
		Array(AddState("mud0", 0, 0, 0.5, 0.5),
		      AddState("mud1", 0.5, 0, 1, 0.5),
		      AddState("mud2", 0, 0.5, 0.5, 1),
		      AddState("mud3", 0.5, 0.5, 1, 1),
		      FillState( 0,  0, 14, 10, "mud0"),
		      FillState(14,  0, 28, 10, "mud1"),
		      FillState( 0, 10, 14, 20, "mud2"),
		      FillState(14, 10, 28, 20, "mud3"))
		)
}

/** Utility to death-watch a set of actors. Allow to count them. */
class WatchList {
	/** The set of watched actors. */
	protected[this] val watchList = new scala.collection.mutable.HashSet[String]()

	/** If not yet watched, watch for the death of the given actor. */
	def watch(actor:ActorRef,context:ActorContext) {
		if(! watchList.contains(actor.path.name)) {
			context.watch(actor)
			watchList += actor.path.name
		}
	}

	/** Does the watch list contains the given actor ? */
	def contains(actor:ActorRef):Boolean = watchList.contains(actor.path.name)

	/** A Terminated message has been received for the given actor. */
	def terminated(actor:ActorRef) { watchList -= actor.path.name }

	/** No more actor to watch. Ok all clean. */
	def isEmpty:Boolean = watchList.isEmpty

	/** Number of actors actually watched. */
	def size:Int = watchList.size

	override def toString():String = "{%s}".format(watchList.mkString(","))
}

class BruceAvatarFactory(val renderer:Renderer) extends AvatarFactory {
	def screenFor(name:String, screenType:String):Screen = {
		screenType match {
			case "menu" ⇒ new MenuScreen(name, renderer)
			case "tile" ⇒ new TileScreen(name, renderer)
			case _      ⇒ throw NoSuchScreenException("cannot create a screen of type %s, unknown type".format(screenType))
		}
	}
	def avatarFor(name:String, avatarType:String, indexed:Boolean):Avatar = {
		avatarType match {
			case "tiles" ⇒ new TilesSprite(name, renderer.screen, false)
			case "image" ⇒ new ImageSprite(name, renderer.screen, indexed)
			case _       ⇒ throw new NoSuchAvatarException("cannot create an avatar of type %s, unknown type".format(avatarType))
		}
	}
}

// Lots of questions
// How to extract the non specific code of this ?
// Almost all seems to be specific to each game (excepted the renderer part that is in its own package)
// Maybe create utility traits that provide some aspects allowing to automatize some tasks ?

object GameActor {
	case class Start
	case class Exit
	case class NextLevel(level:String)

	def main(args:Array[String]) {
		val actorSystem = ActorSystem("Bruce")
		val game = actorSystem.actorOf(Props[GameActor], name = "game")

		game ! GameActor.Start
	}
}

/** The top level actor, starts and control all the others. */
class GameActor extends Actor {
	import GameActor._

	val renderer = new Renderer(self)

	val rendererActor:ActorRef = RendererActor(context.system, renderer, new BruceAvatarFactory(renderer), "Bruce the miner", 1280, 800, 30/*fps*/)

	var menuActor:ActorRef = null

	var levelActor:ActorRef = null

	var level:String = "menu"

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
			rendererActor ! AddResource(TextureResource("bruce-thumb-up", "bruce_thumb_up.png", false))
			rendererActor ! AddResource(TextureResource("tile-nothing", "tile_nothing.png", false))
			rendererActor ! AddResource(TextureResource("tile-mud", "tile_mud.png", false))
			rendererActor ! AddResource(TextureResource("tile-stone", "tile_stone.png", false))
			rendererActor ! Start(21)
			
			menuActor = context.actorOf(Props[MenuActor], name = "menu")
			
			menuActor!MenuActor.Start(rendererActor, self)
			context.watch(menuActor)
			level = "menu"
		}
		
		case Exit ⇒ {
			println("exiting..."); context.stop(self)
		}
		
		case Terminated(actor) ⇒ {
			actor.path.name match {
				case "menu" ⇒ {
					println("next level : %s".format(level))
					levelActor = context.actorOf(Props[LevelActor], name = "level")

					levelActor ! LevelActor.Start(rendererActor, self, 1)
					context.watch(levelActor)
				}
				case "level" ⇒ {
					println("TODO handle Level termination")
				}
			}
		}

		case NextLevel(level) ⇒ {
			if(this.level == "menu") {
				menuActor ! MenuActor.Stop
				menuActor = null
			} else {
				levelActor ! LevelActor.Stop
				levelActor = null
			}
			this.level = level
		}
	}

	override def postStop() { sys.exit }
}

object MenuActor {
	case class Start(rendererActor:ActorRef, gameActor:ActorRef)
	case class Stop
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

	var bruce:ActorRef = null

	var cloud = new Array[ActorRef](10)

	val watchList = new WatchList()

	def receive() = {
		case Start(rActor, gActor) ⇒ {
			import RendererActor._

			rendererActor = rActor
			gameActor     = gActor

			rendererActor ! AddScreen("menu", "menu")
			rendererActor ! SwitchScreen("menu")
			rendererActor ! ChangeScreenSize(Axes((-.5, .5), (-.5, .5), (-1., 1.)), 0.05)
			rendererActor ! ChangeScreen("background-image", "screen-intro")

			playButton = ButtonActor(context, "play")
			watchList.watch(playButton, context)

			playButton ! ButtonActor.Start("play", rendererActor, gameActor, "image")
			playButton ! ButtonActor.Move(Point3(0, 0, 0.05))
			playButton ! ButtonActor.Resize(SizeFromTextureHeight(0.1, "intro-play"))
			playButton ! ButtonActor.DefineState("intro-play", ButtonActor.State.Normal, true)
			playButton ! ButtonActor.TouchBehavior((me,isStart,isEnd) ⇒ { 
				if(isEnd) {
					gameActor ! GameActor.NextLevel("level1")
				}
			})
			
			quitButton = ButtonActor(context, "quit")
			watchList.watch(quitButton, context)

			quitButton ! ButtonActor.Start("quit", rendererActor, gameActor, "image")
			quitButton ! ButtonActor.Move(Point3(0, -0.15, 0.05))
			quitButton ! ButtonActor.Resize(SizeFromTextureHeight(0.1, "intro-quit"))
			quitButton ! ButtonActor.DefineState("intro-quit", ButtonActor.State.Normal, true)
			quitButton ! ButtonActor.DefineState("intro-quit-broken", ButtonActor.State.Activated, false)
			quitButton ! ButtonActor.TouchBehavior((me,isStart,isEnd) ⇒ { if(isStart) {
					me.changeState(ButtonActor.State.Activated)
				} else if(isEnd) {
					me.changeState(ButtonActor.State.Normal)
					gameActor ! GameActor.Exit
				}
			})

			mountains = SpriteActor(context, "mountains")
			watchList.watch(mountains, context)

			mountains ! SpriteActor.Start("mountains", rendererActor, gameActor, "image", "intro-moutains", null)
			mountains ! SpriteActor.Move(Point3(0, -0.2, 0.03))
			mountains ! SpriteActor.Resize(SizeFromScreenWidth(1, "intro-moutains"))

			title = SpriteActor(context, "title")
			watchList.watch(title, context)

			title ! SpriteActor.Start("title", rendererActor, gameActor, "image", "intro-title", null)
			title ! SpriteActor.Move(Point3(0, 0.3, 0.04))
			title ! SpriteActor.Resize(SizeFromScreenWidth(0.7, "intro-title"))

			bruce = SpriteActor(context, "bruce")
			watchList.watch(bruce, context)

			bruce ! SpriteActor.Start("bruce", rendererActor, gameActor, "image", "bruce-thumb-up", null)
			bruce ! SpriteActor.Move(Point3(-0.45, -0.27, 0.06))
			bruce ! SpriteActor.Resize(SizeFromTextureWidth(0.45, "bruce-thumb-up"))

			import scala.math._

			for(i <- 0 until cloud.length) {
				cloud(i) = SpriteActor(context, "cloud%d".format(i))
				watchList.watch(cloud(i), context)

				val animator = new LineAnimator()
				animator.incr.x =  (((random*2)-1)*0.004)
				animator.lo.x   = -1.1
				animator.hi.x   =  1.1
				animator.pos.y  =  0.3 + (((random*2)-1)*0.3)
				animator.pos.x  =  (((random*2)-1)*0.8)
				animator.pos.z  =  0.01

				cloud(i) ! SpriteActor.Start("cloud%d".format(i), rendererActor, gameActor, "image", "intro-cloud", animator)
				cloud(i) ! SpriteActor.Resize(SizeFromTextureWidth(0.3+(random*0.2), "intro-cloud"))
				//cloud(i) ! SpriteActor.AnimationBehavior(41, (me) => { me.move(animator.nextPos) })
			}
		}

		case Terminated(actor) ⇒ {
			watchList.terminated(actor)

			if(watchList.isEmpty) {
				rendererActor ! RendererActor.SwitchScreen("none")
				rendererActor ! RendererActor.RemoveScreen("menu")
				context.stop(self)
			}
		}

		case Stop ⇒ {
			playButton ! ButtonActor.Stop
			quitButton ! ButtonActor.Stop
			mountains ! SpriteActor.Stop
			title ! SpriteActor.Stop
			bruce ! SpriteActor.Stop

			for(i <- 0 until cloud.length) {
				cloud(i) ! SpriteActor.Stop
			}
		}
	}
}

class LineAnimator extends ImageSprite.Animator {
	val pos = Point3(0,0,0)
	val incr = Vector3(0,0,0)
	val lo = Point3(0,0,0)
	val hi = Point3(1,1,1)

	override def hasNextSize = false

	def nextSize(time:Long):NumberSeq3 = { null }

	def nextPosition(time:Long):NumberSeq3 = {
		pos.x += incr.x 
		if(pos.x > hi.x ) { pos.x = hi.x; incr.x = -incr.x }
		if(pos.x < lo.x)  { pos.x = lo.x; incr.x = -incr.x }
		pos.y += incr.y 
		if(pos.y > hi.y ) { pos.y = hi.y; incr.y = -incr.y }
		if(pos.y < lo.y)  { pos.y = lo.y; incr.y = -incr.y }
		pos.z += incr.z 
		if(pos.z > hi.z ) { pos.z = hi.z; incr.z = -incr.z }
		if(pos.z < lo.z)  { pos.z = lo.z; incr.z = -incr.z }
		pos
	}
}

// == LevelActor ==========================================================================================================

object LevelActor {
	case class Start(rendererActor:ActorRef, gameActor:ActorRef, level:Int)
	case class Stop
}

class LevelActor extends Actor {
	import LevelActor._

	var rendererActor:ActorRef = null

	var gameActor:ActorRef = null

	var level:Int = 0

	var screenName:String = null

	val mud = new Array[ActorRef](1)

	def receive() = {
		case Start(rActor, gActor, level) ⇒ {
			import RendererActor._

			rendererActor = rActor
			gameActor     = gActor
			this.level    = level
			screenName    = "level%d".format(level)

			rendererActor ! AddScreen(screenName, "tile")
			rendererActor ! SwitchScreen(screenName)
			rendererActor ! ChangeScreenSize(Axes((-14., 14.), (-10., 10.), (-1., 1.)), 1)
			rendererActor ! ChangeScreen("background-image", "tile-nothing")


			mud(0) = TilesActor(context, "mud0")
//			watchList.watch(mud(0), context)

			mud(0) ! TilesActor.Start("mud0", rendererActor, gameActor, 28, 20, "tile-mud")
			mud(0) ! TilesActor.Move(Point3(-14, -10, 0))
			mud(0) ! TilesActor.Resize(SizeTriplet(1, 1, 1))
		}

		case Stop ⇒ {
			rendererActor ! RendererActor.SwitchScreen("none")
			rendererActor ! RendererActor.RemoveScreen(screenName)

			screenName = null
			level      = 0
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

	case class Start(name:String, rActor:ActorRef, gActor:ActorRef, avatarType:String, resTexture:String, animator:ImageSprite.Animator)
	case class Stop
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
		case Stop ⇒ {
			rendererActor ! RendererActor.RemoveAvatar(name)
			context.stop(self)
		}
		case Resize(size) ⇒ { resize(size) }
		case Move(pos) ⇒ { move(pos) }
		case AnimationBehavior(fps, animBehavior) ⇒ {
			animationBehavior = animBehavior
			context.setReceiveTimeout(fps milliseconds)
		}
		case ReceiveTimeout ⇒ {
			if(animationBehavior ne null) {
				animationBehavior(this)
			} else {
				throw new RuntimeException("animation behavior is null an receive a timeout ??")
			}
		}
	}
}

// == TilesActor ============================================================================================================

object TilesActor {
	def apply(context:ActorContext, name:String):ActorRef = context.actorOf(Props[TilesActor], name) 

	case class Start(name:String, rActor:ActorRef, gActor:ActorRef, width:Int, height:Int, resTexture:String)
	case class Stop
	case class Resize(size:Size)
	case class Move(pos:NumberSeq3)
}

class TilesActor extends Actor {
	import TilesActor._

//var T = 0L

	var name:String = null

	var gameActor:ActorRef = null

	var rendererActor:ActorRef = null

	def resize(newSize:Size) { rendererActor ! RendererActor.ChangeAvatarSize(name, newSize) }

	def move(newPos:NumberSeq3) { rendererActor ! RendererActor.ChangeAvatarPosition(name, newPos) }

	def receive() = {
		case Start(nm, ra, ga, width, height, res) ⇒ {
			import TilesSprite._
			name = nm
			gameActor = ga
			rendererActor = ra

			rendererActor ! RendererActor.AddAvatar(name, "tiles", false)
			rendererActor ! RendererActor.ChangeAvatar(name, GridInit(width, height, res))
			
			rendererActor ! RendererActor.ChangeAvatar(name, AddState("mud0", 0, 0, 0.5, 0.5))
			rendererActor ! RendererActor.ChangeAvatar(name, AddState("mud1", 0.5, 0, 1, 0.5))
			rendererActor ! RendererActor.ChangeAvatar(name, AddState("mud2", 0, 0.5, 0.5, 1))
			rendererActor ! RendererActor.ChangeAvatar(name, AddState("mud3", 0.5, 0.5, 1, 1))
			
			rendererActor ! RendererActor.ChangeAvatar(name, FillState( 0,  0, 14, 10, "mud0"))
			rendererActor ! RendererActor.ChangeAvatar(name, FillState(14,  0, 28, 10, "mud1"))
			rendererActor ! RendererActor.ChangeAvatar(name, FillState( 0, 10, 14, 20, "mud2"))
			rendererActor ! RendererActor.ChangeAvatar(name, FillState(14, 10, 28, 20, "mud3"))
		}
		case Stop ⇒ {
			rendererActor ! RendererActor.RemoveAvatar(name)
			context.stop(self)
		}
		case Resize(size) ⇒ { resize(size) }
		case Move(pos) ⇒ { move(pos) }
		case ReceiveTimeout ⇒ {
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

	case class Stop
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

		case Stop ⇒ {
			rendererActor ! RendererActor.RemoveAvatar(name)
			context.stop(self)			
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