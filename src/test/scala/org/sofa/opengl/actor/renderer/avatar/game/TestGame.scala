package org.sofa.opengl.actor.renderer.avatar.game

import org.scalatest.FlatSpec

import akka.actor.ActorSystem

import org.sofa.math.{Vector3, Point3}

import org.sofa.opengl.{Shader, Texture}
import org.sofa.opengl.actor.renderer.{AvatarName, AvatarBaseStates}
import org.sofa.opengl.actor.renderer.backend.RendererNewt
import org.sofa.opengl.text.GLFont

//import org.sofa.opengl.actor.renderer.avatar.game.IsoCellGridRelief


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

		renderer.start("title", initialWidth=800, initialHeight=600, fps=24, decorated=false, fullscreen=false, overSample=4, runAtInit = { ()=>

			renderer.addScreen("default-screen")
			assertResult(1, "only one screen") { renderer.screenCount }
		
			renderer.switchToScreen("default-screen")
			assert(renderer.hasCurrentScreen, "renderer has a current screen")
		
			val screen = renderer.currentScreen

			screen.addAvatar(AvatarName("root"), "iso-root")
			assertResult(1, "only one avatar") { renderer.currentScreen.subCount }

			screen.addAvatar(AvatarName("root.world"), "iso-world")

			val cellgrid0 = AvatarName("root.world.cellgrid0")
			val cellgrid1 = AvatarName("root.world.cellgrid1")
			val cellgrid2 = AvatarName("root.world.cellgrid2")
			val cellgrid3 = AvatarName("root.world.cellgrid3")

			screen.addAvatar(cellgrid0, "iso-cell-grid")
			screen.addAvatar(cellgrid1, "iso-cell-grid")
			screen.addAvatar(cellgrid2, "iso-cell-grid")
			screen.addAvatar(cellgrid3, "iso-cell-grid")

			screen.changeAvatar(cellgrid1, AvatarBaseStates.MoveAt(Point3(0,-1,0)) )
			screen.changeAvatar(cellgrid2, AvatarBaseStates.MoveAt(Point3(-1,0,0)) )
			screen.changeAvatar(cellgrid3, AvatarBaseStates.MoveAt(Point3(-1,-1,0)) )

			//val config = Array.ofDim[IsoCellConfig](10,10)
			val config = Array.fill[IsoCellConfig](10,10) { IsoCellConfig(0, 1, 1) }

			//     y  x
			config(2)(2) = IsoCellConfig(-1f, 2, 0)
			config(2)(3) = IsoCellConfig(-2f, 3, 0)
			config(2)(4) = IsoCellConfig(-1f, 4, 0)

			config(3)(2) = IsoCellConfig(0, 4, 0)
			config(3)(3) = IsoCellConfig(0, 5, 0)
			config(3)(4) = IsoCellConfig(0, 0, 0)

			screen.changeAvatar(cellgrid0, IsoCellGridConfig(config))
			screen.changeAvatar(cellgrid1, IsoCellGridConfig(config))
			screen.changeAvatar(cellgrid2, IsoCellGridConfig(config))
			screen.changeAvatar(cellgrid3, IsoCellGridConfig(config))
		})
	}

	it should "exit gracefully" in {
		Thread.sleep(60000)
		renderer.destroy
	}
}