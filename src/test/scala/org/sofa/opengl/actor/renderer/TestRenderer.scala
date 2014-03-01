package org.sofa.opengl.actor.renderer

import org.scalatest.FlatSpec

import akka.actor.ActorSystem


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
			assert(counter < 480)	// should suffice ?
		}

		assert(renderer.surface ne null, "renderer surface is ok")
		assert(renderer.gl ne null, "renderer GL is ok")
		assertResult(0, "renderer has no screen") { renderer.screenCount }
		assert(! renderer.hasCurrentScreen, "renderer has no current screen")
	}

	it should "allow to add basic screen at least" in {
		renderer.addScreen("default-screen") 

		assert(! renderer.hasCurrentScreen, "current screen is not set")
		assertResult(1, "only one screen") { renderer.screenCount }
		renderer.switchToScreen("default-screen")
		assert(renderer.hasCurrentScreen, "renderer has a current screen")
		renderer.switchToScreen("unknown")
		assert(! renderer.hasCurrentScreen, "no more current screen")
		renderer.switchToScreen("default-screen")
		assert(renderer.hasCurrentScreen, "renderer has anew a current screen")
	}

	it should "exit gracefully" in {
		renderer.destroy

		assertResult(0, "rendere has no more screens") { renderer.screenCount }
		assert(! renderer.hasCurrentScreen, "no current screen")
		assert(! renderer.isInitialized, "no more usable")
		assert(renderer.surface eq null, "surface should be freed")
		assert(renderer.gl eq null, "GL should be freed")
	}
}