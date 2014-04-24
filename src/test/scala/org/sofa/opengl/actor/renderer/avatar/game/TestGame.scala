package org.sofa.opengl.actor.renderer.avatar.game

import org.scalatest.FlatSpec

import akka.actor.ActorSystem

import org.sofa.opengl.{Shader, Texture}
import org.sofa.opengl.actor.renderer.{AvatarName}
import org.sofa.opengl.actor.renderer.backend.RendererNewt
import org.sofa.opengl.text.GLFont


class TestGame extends FlatSpec {

	// The renderer to test.
	val renderer = new RendererNewt(null, new IsoWorldAvatarFactory())//actorSystem.deadLetters)

	"A game" should "allow adding a root" in {

		Shader.path += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/es2"
		Shader.path += "shaders"
		GLFont.path += "/Users/antoine/Library/Fonts"
		GLFont.path += "Fonts"
		Texture.path += "/Users/antoine/Documents/Art/Images/HexaLife"
		Texture.path += "/Users/antoine/Documents/Art/Images"
		Texture.path += "textures"

		renderer.start("title", initialWidth=320, initialHeight=240, fps=24, decorated=false, fullscreen=false, overSample=4, runAtInit = { ()=>

			renderer.addScreen("default-screen")
			assertResult(1, "only one screen") { renderer.screenCount }
		
			renderer.switchToScreen("default-screen")
			assert(renderer.hasCurrentScreen, "renderer has a current screen")
		
			val screen = renderer.currentScreen

			screen.addAvatar(AvatarName("root"), "iso-root")
			assertResult(1, "only one avatar") { renderer.currentScreen.subCount }

			screen.addAvatar(AvatarName("root.world"), "iso-world")

			val cell1 = AvatarName("root.world.cell1")

			screen.addAvatar(cell1, "iso-cell-grid")
			screen.changeAvatar(cell1, IsoCellGridSize(10, 10))
		})
	}

	it should "exit gracefully" in {
		Thread.sleep(60000)
		renderer.destroy
	}
}