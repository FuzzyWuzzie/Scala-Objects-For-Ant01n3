package org.sofa.opengl.actor.renderer.avatar.game

import org.scalatest.FlatSpec

import akka.actor.ActorSystem

import org.sofa.math.{Vector3, Point3}

import org.sofa.opengl.{Shader, Texture}
import org.sofa.opengl.actor.renderer.{AvatarName, AvatarBaseStates}
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

		renderer.start("title", initialWidth=800, initialHeight=600, fps=24, decorated=false, fullscreen=false, overSample=4, runAtInit = { ()=>

			renderer.addScreen("default-screen")
			assertResult(1, "only one screen") { renderer.screenCount }
		
			renderer.switchToScreen("default-screen")
			assert(renderer.hasCurrentScreen, "renderer has a current screen")
		
			val screen = renderer.currentScreen

			screen.addAvatar(AvatarName("root"), "iso-root")
			assertResult(1, "only one avatar") { renderer.currentScreen.subCount }

			screen.addAvatar(AvatarName("root.world"), "iso-world")

			val cell0 = AvatarName("root.world.cell0")
			val cell1 = AvatarName("root.world.cell1")
			val cell2 = AvatarName("root.world.cell2")
			val cell3 = AvatarName("root.world.cell3")
			
			val cell4 = AvatarName("root.world.cell4")
			val cell5 = AvatarName("root.world.cell5")
			val cell6 = AvatarName("root.world.cell6")
			val cell7 = AvatarName("root.world.cell7")

			val cell8 = AvatarName("root.world.cell8")
			val cell9 = AvatarName("root.world.cell9")
			val cell10 = AvatarName("root.world.cell10")
			val cell11 = AvatarName("root.world.cell11")

			val cell12 = AvatarName("root.world.cell12")
			val cell13 = AvatarName("root.world.cell13")
			val cell14 = AvatarName("root.world.cell14")
			val cell15 = AvatarName("root.world.cell15")

			screen.addAvatar(cell0, "iso-cell")
			screen.addAvatar(cell1, "iso-cell")
			screen.addAvatar(cell2, "iso-cell")
			screen.addAvatar(cell3, "iso-cell")
			
			screen.addAvatar(cell4, "iso-cell")
			screen.addAvatar(cell5, "iso-cell")
			screen.addAvatar(cell6, "iso-cell")
			screen.addAvatar(cell7, "iso-cell")

			screen.addAvatar(cell8, "iso-cell")
			screen.addAvatar(cell9, "iso-cell")
			screen.addAvatar(cell10, "iso-cell")
			screen.addAvatar(cell11, "iso-cell")

			screen.addAvatar(cell12, "iso-cell")
			screen.addAvatar(cell13, "iso-cell")
			screen.addAvatar(cell14, "iso-cell")
			screen.addAvatar(cell15, "iso-cell")

			screen.changeAvatar(cell0, AvatarBaseStates.MoveAt(Point3(0,0,0)))
			screen.changeAvatar(cell1, AvatarBaseStates.MoveAt(Point3(-1,0,-2)))
			screen.changeAvatar(cell2, AvatarBaseStates.MoveAt(Point3(1,0,-3)))
			screen.changeAvatar(cell3, AvatarBaseStates.MoveAt(Point3(2,0,0)))

			screen.changeAvatar(cell4, AvatarBaseStates.MoveAt(Point3(0,-1,0)))
			screen.changeAvatar(cell5, AvatarBaseStates.MoveAt(Point3(-1,-1,0)))
			screen.changeAvatar(cell6, AvatarBaseStates.MoveAt(Point3(1,-1,0)))
			screen.changeAvatar(cell7, AvatarBaseStates.MoveAt(Point3(-2,-1,0)))

			screen.changeAvatar(cell8, AvatarBaseStates.MoveAt(Point3(0,1,0)))
			screen.changeAvatar(cell9, AvatarBaseStates.MoveAt(Point3(-1,1,0)))
			screen.changeAvatar(cell10, AvatarBaseStates.MoveAt(Point3(1,1,0)))
			screen.changeAvatar(cell11, AvatarBaseStates.MoveAt(Point3(-2,1,0)))

			screen.changeAvatar(cell12, AvatarBaseStates.MoveAt(Point3(0,-2,0)))
			screen.changeAvatar(cell13, AvatarBaseStates.MoveAt(Point3(-1,-2,-1)))
			screen.changeAvatar(cell14, AvatarBaseStates.MoveAt(Point3(1,-2,-2)))
			screen.changeAvatar(cell15, AvatarBaseStates.MoveAt(Point3(2,-2,0)))

			//val e0 = AvatarName("root.world.e0")

			//screen.addAvatar(e0, "iso-entity")

			//screen.changeAvatar(e0, AvatarBaseStates.Move(1, 0, 0))
		})
	}

	it should "exit gracefully" in {
		Thread.sleep(60000)
		renderer.destroy
	}
}