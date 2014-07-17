package org.sofa.opengl.akka

import com.typesafe.config.{ConfigFactory, Config}
import org.sofa.opengl.surface.Surface
import akka.dispatch.{DispatcherPrerequisites, ExecutorServiceFactory, ExecutorServiceConfigurator}
import java.util.concurrent.{ExecutorService, AbstractExecutorService, ThreadFactory, TimeUnit}
import scala.collection.mutable.HashMap
import java.util.Collections
import scala.collection.JavaConversions._


/** Wraps a `Surface.invoke()` as an Akka ExecutorService. This service can then be
  * used in a `ExecutorServiceConfigurator`, specifically here 
  * SurfaceExecutorConfigurator`, that is created from the application.conf.
  * 
  * Code derived from Viktor Klang: https://gist.github.com/viktorklang/2422443 */
object SurfaceExecutorService extends AbstractExecutorService { 

	/** Key name in the configuration. */
	final val configKey = "org.sofa.opengl.akka.surface-dispatcher"

	/** You can call this in static context before the Akka conf is loaded to avoid creating an
	  * "application.conf" and still setup the SurfaceExecutorService. */
	def configure() { configure(10L) }

	/** You can call this in static context before the Akka conf is loaded 
	  * (before the ActorSystem is built) to avoid creating an
	  * "application.conf" and still setup the SurfaceExecutorService.
	  * @param tickDurationMS the interval at wich the scheduler wakes up. */
	def configure(tickDurationMs:Long) {
		sys.props += ("akka.scheduler.tick-duration" → f"$tickDurationMs%dms")
		sys.props += (s"${configKey}.type"           → "Dispatcher")
		sys.props += (s"${configKey}.executor"       → "org.sofa.opengl.akka.SurfaceExecutorServiceConfigurator")
		sys.props += (s"${configKey}.throughput"     → "1")
	}

	/** You can call this to create a config to pass (or to merge) to the ActorSystem when 
	  * building it. This configures the scheduler tick-duration (default 10ms), the
	  * dispatcher and the executor. */
	def config():Config = config(10L)

	/** You can call this to create a config to pass (or to merge) to the ActorSystem when 
	  * building it. This configures the scheduler tick-duration (default 100ms !), the
	  * dispatcher and the executor.
	  * @param tickDurationMS the interval at wich the scheduler wakes up. */
	def config(tickDurationMs:Long):Config = {
		ConfigFactory.parseMap(HashMap[String,AnyRef](
				("akka.scheduler.tick-duration" → f"$tickDurationMs%dms"),
				(s"${configKey}.type"           → "Dispatcher"),
				(s"${configKey}.executor"       → "org.sofa.opengl.akka.SurfaceExecutorServiceConfigurator"),
				(s"${configKey}.throughput"     → "1")
			))
	}

	/** The actual surface used for rendering. */
	private[this] var surface:Surface = null

	/** The surface, if set, else null. */
	def getSurface:Surface = surface

	/** The actual surface used for rendering. There can be only one such surface
	  * at a time. It is used to execute actions in the thread managing the surface.
	  * This is a field that is set at runtime for the only actor that will
	  * use this executor. We have no other mean to do it, since executors
	  * are not created and managed by us. */
	def setSurface(surface:Surface) { this.surface = surface }

	/** True if this service uses the given `surface`. */
	def isSurface(surface:Surface):Boolean = this.surface == surface

	def execute(command:Runnable) = {
		if(surface ne null) {
//Console.err.println("***(surface-dispatcher)*** BEFORE from thread %s".format(Thread.currentThread.getName))
			surface.invoke(command)
//Console.err.println("***(surface-dispatcher)*** AFTER from thread %s".format(Thread.currentThread.getName))
		} else {
			throw new RuntimeException("SurfaceExecutorService.surface not set, see setSurface()")
		}
	}

	def shutdown(): Unit = ()

	def shutdownNow() = Collections.emptyList[Runnable]

	def isShutdown = false

	def isTerminated = false

	def awaitTermination(l:Long, timeUnit:TimeUnit) = true
}

/** Create an ExecutorServiceConfigurator so that Akka can use our
  * [[SurfaceExecutorService]] for the dispatchers.
  *
  * Then to use it add :
  *    surface-dispatcher {
  *        type       = "Dispatcher"
  *        executor   = "org.sofa.opengl.akka.SurfaceExecutorServiceConfigurator"
  *        throughput = 1
  *    }
  * To your application.conf. To use it with an actor use it like this:
  *    val guiActor = context.actorOf(Props[YourGUIActor].withDispatcher(SurfaceExecutorService.configKey), name="gui-actor")
  */
class SurfaceExecutorServiceConfigurator(config:Config, prerequisites:DispatcherPrerequisites)
		extends ExecutorServiceConfigurator(config, prerequisites) {
	
	private[this] val f = new ExecutorServiceFactory {
		def createExecutorService:ExecutorService = { SurfaceExecutorService }
	}
	
	def createExecutorServiceFactory(id:String, threadFactory:ThreadFactory):ExecutorServiceFactory = f
}