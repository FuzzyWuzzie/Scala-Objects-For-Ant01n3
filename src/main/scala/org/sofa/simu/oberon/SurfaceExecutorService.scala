package org.sofa.simu.oberon

import com.typesafe.config.Config
import org.sofa.opengl.surface.Surface
import akka.dispatch.{DispatcherPrerequisites, ExecutorServiceFactory, ExecutorServiceConfigurator}
import java.util.concurrent.{ExecutorService, AbstractExecutorService, ThreadFactory, TimeUnit}
import java.util.Collections

/** Wraps a [[Surface.invoke()]] as an ExecutorService. This service can then be
  * used in a [[ExecutorServiceConfigurator]], specifically here 
  * [[SurfaceExecutorConfigurator]], that is created from the application.conf.
  * 
  * Code derived from Viktor Klang: https://gist.github.com/viktorklang/2422443 */
object SurfaceExecutorService extends AbstractExecutorService { 

	/** The actual surface used for rendering. */
	private[this] var surface:Surface = null

	/** The actual surface used for rendering. There can be only one such surface
	  * at a time. It is used to execute actions in the thread managing the surface.
	  * This is a field that is set at runtime for the only actor that will
	  * use this executor. We have no other mean to do it, since executors
	  * are not created and managed by us. */
	def setSurface(surface:Surface) {
		this.surface = surface
	}

	def execute(command:Runnable) = if(surface ne null) surface.invoke({ surface => command.run(); true }) else throw new RuntimeException("JoglExecutorService.surface not set")

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
  *        executor   = "org.sofa.simu.oberon.JoglEventThreadExecutorServiceConfigurator"
  *        throughput = 1
  *    }
  * To your application.conf. To use it with an actor use it like this:
  *    val guiActor = context.actorOf(Props[GUIActor].withDispatcher("surface-dispatcher"), "gui-actor")    
  * */
class SurfaceExecutorServiceConfigurator(config:Config, prerequisites:DispatcherPrerequisites)
	extends ExecutorServiceConfigurator(config, prerequisites) {
	private[this] val f = new ExecutorServiceFactory { def createExecutorService:ExecutorService = SurfaceExecutorService }
	def createExecutorServiceFactory(id:String, threadFactory:ThreadFactory):ExecutorServiceFactory = f
}