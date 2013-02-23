package org.sofa.simu.oberon

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef}
import org.sofa.simu.oberon.renderer._

class BruceAvatarFactory extends AvatarFactory {
	def avatarFor(name:String, avatarType:String):Avatar = {
		throw new RuntimeException("TODO")
	}
}

object Game {
	case class Start
	case class Exit

	def main(args:Array[String]) {
		val actorSystem = ActorSystem("Game")
		val game = actorSystem.actorOf(Props[GameActor], name = "game")

		game ! Game.Start
	}
}

/** The top level actor, starts and control all the others. */
class GameActor extends Actor {
	val renderer = new Renderer(self)

	val rendererActor:ActorRef = RendererActor(context.system, renderer, new BruceAvatarFactory(), "Bruce the miner", 800, 600)

	def receive() = {
		case Game.Start ⇒ { rendererActor ! RendererActor.Start(21); self ! "menu" }
		case "menu"     ⇒ { rendererActor ! RendererActor.AddScreen("menu", new MenuScreen(renderer)); rendererActor ! RendererActor.SwitchScreen("menu") }
		case "play"     ⇒ { println("play") }
		case "over"     ⇒ { println("game over") }
		case Game.Exit  ⇒ { println("exiting..."); context.stop(self) }
	}

	override def postStop() { sys.exit }
}