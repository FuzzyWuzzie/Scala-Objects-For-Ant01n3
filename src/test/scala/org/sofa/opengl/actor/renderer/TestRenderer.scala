package org.sofa.opengl.actor.renderer

import org.scalatest.FlatSpec

import akka.actor.ActorSystem
import org.sofa.opengl.actor.renderer.backend.RendererNewt


class TestRenderer extends FlatSpec {
	// Dummy actor system.
	//val actorSystem = ActorSystem.create("test-renderer")

	// The renderer to test.
	val renderer = new RendererNewt(null)//actorSystem.deadLetters)

	//val rendererActor = RendererActor(actorSystem, renderer, "test-renderer", 320, 240, 30)

	"A Renderer" should "start properly" in {
		renderer.start("title", initialWidth=320, initialHeight=240, fps=30, decorated=false, fullscreen=false, overSample=4)
		//SurfaceExecutorService.setSurface(renderer.surface)


		var counter = 0

		while(! renderer.isInitialized) {
			Thread.sleep(1)
			counter += 1
			assertResult(true) { counter < 480 }	// should suffice ?
		}

		assertResult(true, "renderer surface is ok") { renderer.surface ne null }
		assertResult(true, "renderer GL is ok") { renderer.gl ne null }
		assertResult(0, "renderer has no screen") { renderer.screenCount }
		assertResult(false, "renderer has no current screen") { renderer.hasCurrentScreen }
	}

	it should "allow to add basic screen at least" in {
		renderer.addScreen("default-screen") 

		assertResult(false, "current screen is not set") {  renderer.hasCurrentScreen }
		assertResult(1, "only one screen") { renderer.screenCount }
		renderer.switchToScreen("default-screen")
		assertResult(true, "renderer has a current screen") { renderer.hasCurrentScreen }
		renderer.switchToScreen("unknown")
		assertResult(false, "no more current screen") { renderer.hasCurrentScreen }
		renderer.switchToScreen("default-screen")
		assertResult(true, "renderer has anew a current screen") { renderer.hasCurrentScreen }
	}

	it should "exit gracefully" in {
		renderer.destroy

		assertResult(0, "rendere has no more screens") { renderer.screenCount }
		assertResult(false, "no current screen") { renderer.hasCurrentScreen }
		assertResult(false, "no more usable") { renderer.isInitialized }
		assertResult(null, "surface should be freed") { renderer.surface }
		assertResult(null, "GL should be freed") { renderer.gl }
	}
}