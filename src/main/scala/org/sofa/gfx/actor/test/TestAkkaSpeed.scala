package org.sofa.gfx.actor.test

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef, ActorContext}
import scala.concurrent.duration._
import scala.language.postfixOps

object TestAkkaSpeed {
	val ms =  20L;

	case class Start()
	case class Exit()

	def main(args:Array[String]) {
		val actorSystem = ActorSystem("TestAkkaSpeed")
		val mainActor = actorSystem.actorOf(Props[TestAkkaSpeed], name = "testakkaspeed")

		mainActor ! "start"
	}
}

class TestAkkaSpeed extends Actor {
	var count = 0

	def receive() = {
		case "start" => {
			val tick1 = context.actorOf(Props[TickActor], name = "tick1")
			val tick2 = context.actorOf(Props[TickActor], name = "tick2")
			val tick3 = context.actorOf(Props[TickActor], name = "tick3")
			val tick4 = context.actorOf(Props[TickActor], name = "tick4")
			val tick5 = context.actorOf(Props[TickActor], name = "tick5")
			val tick6 = context.actorOf(Props[TickActor], name = "tick6")

			tick1 ! Start("tick1", TestAkkaSpeed.ms)
			tick2 ! Start("tick2", TestAkkaSpeed.ms)
			tick3 ! Start("tick3", TestAkkaSpeed.ms)
			tick4 ! Start("tick4", TestAkkaSpeed.ms)
			tick5 ! Start("tick5", TestAkkaSpeed.ms)
			tick6 ! Start("tick6", TestAkkaSpeed.ms)

			context.setReceiveTimeout(TestAkkaSpeed.ms*4 milliseconds)
		}
		case ReceiveTimeout => { count += 1; println("----"); if(count > 50) context.stop(self) }
	}

	override def postStop() { sys.exit }
}

case class Start(name:String, fps:Long)

class TickActor extends Actor {
	var T1 = 0L
	var name = ""

	def receive() = {
		case Start(name,fps) => { this.name = name; context.setReceiveTimeout(fps milliseconds) }
		case ReceiveTimeout => { val T = System.currentTimeMillis; println("%s %d".format(name, T-T1)); T1=T }
	}
}
