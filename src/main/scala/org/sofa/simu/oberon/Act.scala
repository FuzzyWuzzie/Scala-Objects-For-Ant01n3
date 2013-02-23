// package org.sofa.simu.oberon

// import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef}
// import scala.concurrent.duration._

// // -- Messages ------------------------------------------------------------------

// trait OberonMessage

// // -- Renderer Messages ---------

// trait RendererMessage extends OberonMessage
// case class AddActor(id:String) extends RendererMessage
// case class RemoveActor(id:String) extends RendererMessage
// case class ChangeActorState(id:String,newState:Int) extends RendererMessage
// case class ChangeActorValue(id:String,axis:String,newValue:Int) extends RendererMessage
// case class ActorTap(count:Int) extends RendererMessage
// case class ActorVisibilityChange(visible:Boolean) extends RendererMessage

// // -- Actors ----------------------------------------------------------------------

// // class Game extends Actor {
// // 	var startTime = 0L
// // 	var count = 0

// // 	def receive = {
// // 		case m:OberonMessage => {}
// // 		case "start" => { context.setReceiveTimeout(1 nanosecond) }
// // 		case "kill" => { context.stop(self) }
// // 		case "tick" => {
// // 			var T = System.currentTimeMillis
// // 			var D = T - startTime
// // 			startTime = System.currentTimeMillis
// // 			println("T = %d".format(D))
// // 			count += 1
// // 			if(count > 100)
// // 				self ! "kill"			
// // 		}
// // 		case x:ReceiveTimeout => {
// // 			var T = System.currentTimeMillis
// // 			var D = T - startTime
// // 			startTime = System.currentTimeMillis
// // 			println("T = %d".format(D))
// // 			count += 1
// // 			if(count > 100)
// // 				self ! "kill"
// // 		}
// // 	}

// // 	override def postStop() {
// // 		sys.exit
// // 	}
// // }

// // object OberonTest {
// // 	def main(args:Array[String]) {
// // 		val system = ActorSystem("test")
// // 		val actor = system.actorOf(Props[Game], name="game")

// // 		import system.dispatcher

// // 		//actor ! "start"
// // 		system.scheduler.schedule(1 milliseconds, 1 milliseconds, actor, "tick")
// // 	}
// // }