package org.sofa.gfx.avatar.game.test

import scala.compat.Platform
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.{ActorSystem, Actor, ActorRef, Props, ReceiveTimeout}

import org.sofa.math.{Point3, Vector3, Matrix4}

import org.sofa.Timer
import org.sofa.gfx.akka.SurfaceExecutorService
import org.sofa.gfx.actor.renderer.{Renderer, Screen, AvatarName, AvatarBaseStates, RendererActor, RendererController}
import org.sofa.gfx.actor.renderer.backend.RendererNewt
import org.sofa.gfx.actor.renderer.avatar.ui.{UIAvatarFactory}


object TestEvents extends App {
	final val Title = "Events"
	SurfaceExecutorService.configure
	val system = ActorSystem(Title)
	val isoGame = system.actorOf(Props[Events], name=Title)

	RendererActor(system, isoGame, 
		Renderer(new UIAvatarFactory()), TestEvents.Title, 800, 600, 30, true, false, 4)
}


class Events extends Actor {
	import RendererActor._

	protected[this] var renderer:ActorRef = null

	def receive = {
		case RendererController.Start(renderer) => {
			this.renderer = renderer
			initGame
			context.setReceiveTimeout(100 milliseconds)
		}
		case RendererController.Exit => {
			println("renderer stoped")
			stopGame
			renderer = null
		}
		case ReceiveTimeout => {
			controlGame
		}
	}

	protected def initGame() {
		renderer ! AddScreen("main-screen")
		renderer ! SwitchScreen("main-screen")

		val root = AvatarName("root")

		renderer ! AddAvatar("ui.root-events", root)
	}

	protected def stopGame() {
		sys.exit
	}

//	protected[this] var step = 0

	protected def controlGame() {
		// if(step % 10 == 0)
		// 	Timer.timer.printAvgs("TestEvents")
		// step += 1
	}
}