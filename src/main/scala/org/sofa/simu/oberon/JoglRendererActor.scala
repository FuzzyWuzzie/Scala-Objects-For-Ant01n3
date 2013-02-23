// package org.sofa.simu.oberon

// import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef}
// import scala.concurrent.duration._
// import org.sofa.opengl.surface._

// object SurfaceRendererActor {
// 	def apply(system:ActorSystem, surface:Surface):ActorRef = {
// 		SurfaceExecutorService.setSurface(surface)
// 		system.actorOf(Props[SurfaceRendererActor].withDispatcher("oberon.surface-dispatcher"), name="surface-renderer-actor")
// 	}
// }

// class SurfaceRendererActor extends Actor {
// 	var count = 0

// 	def receive() = {
// 		case AddActor(id) ⇒ {}
// 		case RemoveActor(id) ⇒ {}
// 		case ChangeActorState(id,newState) ⇒ {}
// 		case ChangeActorValue(id,axis,newValue) ⇒ {}
// 		case ReceiveTimeout ⇒ { if(count > 100) self ! "kill!" else count += 1; println(s"executing in ${Thread.currentThread.getName} (count=${count})") }
// 		case "start" ⇒ { context.setReceiveTimeout(100 millis) }
// 		case "kill!" ⇒ { println("exiting..."); context.stop(self) }
// 	}

// 	override def postStop() {
// 		sys.exit
// 	}
// }

// // object TestJoglRendererActor {
// // 	def main(args:Array[String]) {
// // 		System.getProperties.put("config.trace","loads")
		
// // 		val system = ActorSystem("foo")
// // 		val root = SurfaceRendererActor(system, "jogl-renderer-actor")
		
// // 		root ! "wait"
	
// // 		println("end main")
// // 	}
// // }