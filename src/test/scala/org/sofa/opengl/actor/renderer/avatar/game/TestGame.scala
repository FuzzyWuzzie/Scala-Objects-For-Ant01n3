// package org.sofa.opengl.actor.renderer.avatar.game

// import org.scalatest.FlatSpec

// import akka.actor.ActorSystem

// import org.sofa.math.{Vector3, Point3}

// import org.sofa.opengl.{Shader, Texture}
// import org.sofa.opengl.actor.renderer.{AvatarName, AvatarBaseStates}
// import org.sofa.opengl.actor.renderer.backend.RendererNewt
// import org.sofa.opengl.text.GLFont

//import org.sofa.opengl.actor.renderer.avatar.game.IsoCellGridRelief


// class TestGame extends FlatSpec {

// 	// The renderer to test.
// 	val renderer = new RendererNewt(new IsoWorldAvatarFactory())//actorSystem.deadLetters)

// 	"A game" should "allow adding a root" in {

// 		renderer.start("title", initialWidth=800, initialHeight=600, fps=24, decorated=false, fullscreen=false, overSample=4, runAtInit = { ()=>

// 			renderer.addScreen("default-screen")
// 			assertResult(1, "only one screen") { renderer.screenCount }
		
// 			renderer.switchToScreen("default-screen")
// 			assertResult(true, "renderer has a current screen") {renderer.hasCurrentScreen}
		
// 			val screen = renderer.currentScreen

// 			screen.libraries.addResources("/Gloubs.xml")

// 			screen.addAvatar(AvatarName("root"), "iso-root")
// 			assertResult(1, "only one avatar") { renderer.currentScreen.subCount }

// 			screen.addAvatar(AvatarName("root.world"), "iso-world")

// 			// ---------------

// 			val underground = AvatarName("root.world.underground")
// 			val ground      = AvatarName("root.world.ground")
// 			val entities    = AvatarName("root.world.entities")

// 			screen.addAvatar(underground, "iso-layer")
// 			screen.addAvatar(ground, "iso-layer")
// 			screen.addAvatar(entities, "iso-layer")

// 			// ---------------

// 			val cellgrid0 = AvatarName("root.world.ground.cellgrid0")
// 			val cellgrid1 = AvatarName("root.world.ground.cellgrid1")
// 			val cellgrid2 = AvatarName("root.world.ground.cellgrid2")
// 			val cellgrid3 = AvatarName("root.world.ground.cellgrid3")

// 			screen.addAvatar(cellgrid1, "iso-cell-grid")
// 			screen.addAvatar(cellgrid0, "iso-cell-grid")
// 			//screen.addAvatar(cellgrid2, "iso-cell-grid")
// 			//screen.addAvatar(cellgrid3, "iso-cell-grid")

// 			//val config = Array.ofDim[IsoCellConfig](10,10)
// 			val relief = Array.fill[IsoCellGridRelief](4,4) { IsoCellGridRelief(0, 1, 1) }
// 			val shape  = IsoCellGridShape(1f+(1f/8f), 0, -1f/16f)
// 			val shade  = IsoCellGridShade("iso-shader", "ground-color-1", "ground-mask-1", 0.433f, 0.281f, 
// 								Array[Float](0.027f, 0.027f, 0.027f, 0.514f, 0.514f, 0.514f),
// 								Array[Float](0.046f, 0.359f, 0.671f, 0.046f, 0.359f, 0.671f))

// 			screen.changeAvatar(cellgrid0, IsoCellGridConfig(shade, shape, relief))
// 			screen.changeAvatar(cellgrid1, IsoCellGridConfig(shade, shape, relief))
// 			//screen.changeAvatar(cellgrid2, IsoCellGridConfig(shade, shape, relief))
// 			//screen.changeAvatar(cellgrid3, IsoCellGridConfig(shade, shape, relief))

// 			screen.changeAvatar(cellgrid1, AvatarBaseStates.MoveAt(Point3(0,1,0)) )
// 			//screen.changeAvatar(cellgrid2, AvatarBaseStates.MoveAt(Point3(-1,0,0)) )
// 			//screen.changeAvatar(cellgrid3, AvatarBaseStates.MoveAt(Point3(-1,1,0)) )

// 			// ---------------

// 			val gloub1 = AvatarName("root.world.entities.gloub1")

// 			val gconfig = IsoEntityConfig("gloub-armature", "eye-loop", "gloub-mask")

// 			screen.addAvatar(gloub1, "iso-entity")
// 			screen.changeAvatar(gloub1, gconfig)

// 			val habitat1 = AvatarName("root.world.entities.habitat1")
// 			val hconfig = IsoEntityConfig("habitat1-armature", "<none>", "habitat1-mask")

// 			screen.addAvatar(habitat1, "iso-entity")
// 			screen.changeAvatar(habitat1, hconfig)
// 			screen.changeAvatar(habitat1, AvatarBaseStates.MoveAt(Point3(1,0,0)))
// 		})
// 	}

// 	it should "exit gracefully" in {
// 		Thread.sleep(60000)
// 		renderer.destroy
// 	}
// }