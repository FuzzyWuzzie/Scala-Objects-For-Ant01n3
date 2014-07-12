package org.sofa.opengl.avatar.game.test

import scala.compat.Platform
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.{ActorSystem, Actor, ActorRef, Props, ReceiveTimeout}

import org.sofa.math.{Point3, Vector3, Matrix4}

import org.sofa.Timer
import org.sofa.opengl.akka.SurfaceExecutorService
import org.sofa.opengl.actor.renderer.{Renderer, Screen, AvatarName, AvatarBaseStates, RendererActor, RendererController}
import org.sofa.opengl.actor.renderer.backend.RendererNewt
import org.sofa.opengl.actor.renderer.avatar.game.{IsoWorldAvatarFactory, IsoCellGridConfig, IsoCellGridRelief, IsoCellGridShape, IsoCellGridShade, IsoEntityConfig, IsoCellGridTex}


object TestIsoGame extends App {
	final val Title = "IsoGame"
	SurfaceExecutorService.configure
	val system = ActorSystem(Title)
	val isoGame = system.actorOf(Props[IsoGame], name=Title)
	isoGame ! "start"
}


class IsoGame extends Actor {
	import RendererActor._

	protected[this] var renderer:ActorRef = null

	def receive = {
		case "start" => {
			RendererActor(context, self, 
				Renderer(new IsoWorldAvatarFactory()), TestIsoGame.Title, 800, 600, 30, true, false, 4)
		}
		case RendererController.Start(renderer) => {
			this.renderer = renderer
			initGame
			context.setReceiveTimeout(100 milliseconds)
		}
		case RendererController.Exit => {
			println("renderer stoped")
			stopGame
			renderer = null
		}
		case ReceiveTimeout => {
			controlGame
		}
	}

	protected def initGame() {
		// val rot = Matrix4()
		// rot.setIdentity
		// rot.rotate(math.Pi/4.0, 1, 0, 0)
		// rot.rotate(-math.Pi/4.0, 0, 1, 0)
		// println(s"rot=${rot}")

		renderer ! AddResources("/Gloubs.xml")
		renderer ! AddScreen("main-screen")
		renderer ! SwitchScreen("main-screen")

		// Root & World

		val root = AvatarName("root")
		val world = AvatarName("root.world")

		renderer ! AddAvatar("iso-root", root)
		renderer ! AddAvatar("iso-world", world)

		// Layers

		val underground = AvatarName("root.world.underground")
		val ground      = AvatarName("root.world.ground")
		val entities    = AvatarName("root.world.entities")

		renderer ! AddAvatar("iso-layer", underground)
		renderer ! AddAvatar("iso-layer", ground)
		renderer ! AddAvatar("iso-layer", entities)

		// Ground

		val cellgrid0 = AvatarName("root.world.ground.cellgrid0")
		val cellgrid1 = AvatarName("root.world.ground.cellgrid1")
		val cellgrid2 = AvatarName("root.world.ground.cellgrid2")
		val cellgrid3 = AvatarName("root.world.ground.cellgrid3")

		renderer ! AddAvatar("iso-cell-grid", cellgrid3)
		renderer ! AddAvatar("iso-cell-grid", cellgrid2)
		renderer ! AddAvatar("iso-cell-grid", cellgrid1)
		renderer ! AddAvatar("iso-cell-grid", cellgrid0)

		var relief = Array.fill[IsoCellGridRelief](4,4) { IsoCellGridRelief(0, 0, 0) }
		val shape  = IsoCellGridShape(0, 0)
		val shade  = IsoCellGridShade("iso-cell-grid-shader", "ground-color-1", "ground-mask-1",
			IsoCellGridTex(0.433f, 0.250f, 
							Array[Float](0.027f, 0.514f, 0.270f, 0.027f, 0.514f, 0.270f),
							Array[Float](0.296f, 0.296f, 0.437f, 0.578f, 0.578f, 0.718f)),
			IsoCellGridTex(0.433f, 0.375f,
							Array[Float](0.027f, 0.514f),
							Array[Float](0.015f, 0.015f)))

		//relief(1)(1) = IsoCellGridRelief(0, 0, 1)
		//relief(1)(2) = IsoCellGridRelief(0, 0, 1)

		renderer ! ChangeAvatar(cellgrid0, IsoCellGridConfig(shade, shape, relief))

		relief = Array.fill[IsoCellGridRelief](4,4) { IsoCellGridRelief(0, 3, -1, true) }
		relief(1)(1) = IsoCellGridRelief(0.1f,  1, 1)
		relief(1)(2) = IsoCellGridRelief(0.15f, 1, 1)
		relief(2)(1) = IsoCellGridRelief(0.05f, 1, 1)
		relief(2)(2) = IsoCellGridRelief(0.0f,  1, 1)

		renderer ! ChangeAvatar(cellgrid1, IsoCellGridConfig(shade, shape, relief))

		relief = Array.fill[IsoCellGridRelief](4,4) { IsoCellGridRelief(0, 0, 0) }
		relief(1)(1) = IsoCellGridRelief(0, 5, 0)
		relief(1)(2) = IsoCellGridRelief(0, 4, 0)

		renderer ! ChangeAvatar(cellgrid2, IsoCellGridConfig(shade, shape, relief))

		relief = Array.fill[IsoCellGridRelief](4,4) { IsoCellGridRelief(0, 0, 0) }
		relief(1)(1) = IsoCellGridRelief(0, 2, 0)
		relief(1)(2) = IsoCellGridRelief(0, 2, 0)

		renderer ! ChangeAvatar(cellgrid3, IsoCellGridConfig(shade, shape, relief))

		renderer ! ChangeAvatar(cellgrid1, AvatarBaseStates.MoveAt(Point3(0,1,0)) )
		renderer ! ChangeAvatar(cellgrid2, AvatarBaseStates.MoveAt(Point3(-1,0,0)) )
		renderer ! ChangeAvatar(cellgrid3, AvatarBaseStates.MoveAt(Point3(-1,1,0)) )

		// Entities

//		val gloub1    = AvatarName("root.world.entities.gloub1")
		val bolok1    = AvatarName("root.world.entities.bolok1")
		val bolok2    = AvatarName("root.world.entities.bolok2")
		val dock1     = AvatarName("root.world.entities.dock1")
		val dock2     = AvatarName("root.world.entities.dock2")
		val habitat1  = AvatarName("root.world.entities.habitat1")
		val somadrop1 = AvatarName("root.world.entities.somadrop1")

//		val gloub1Config    = IsoEntityConfig("gloub-armature", "gloub-eye-loop", "gloub-mask")
		val bolok1Config    = IsoEntityConfig("bolok-armature", "bolok-eye-loop", "bolok-mask")
		val dock1Config     = IsoEntityConfig("dock-armature", "dock-grow-shrink-loop", "bolok-mask")
		val dock2Config     = IsoEntityConfig("dock2-armature", "<none>", "bolok-mask")
		val habitat1Config  = IsoEntityConfig("habitat1-armature", "<none>", "habitat1-mask")
		val somadrop1Config = IsoEntityConfig("somadrop-armature", "<none>", "habitat1-mask")

		// renderer ! AddAvatar("iso-entity", gloub1)
		// renderer ! ChangeAvatar(gloub1, gloub1Config)
		// renderer ! ChangeAvatar(gloub1, AvatarBaseStates.MoveAt(Point3(1,0,0)))

		renderer ! AddAvatar("iso-entity", bolok1)
		renderer ! ChangeAvatar(bolok1, bolok1Config)

		//renderer ! AddAvatar("iso-entity", bolok2)
		//renderer ! ChangeAvatar(bolok2, bolok1Config)
		//renderer ! ChangeAvatar(bolok2, AvatarBaseStates.MoveAt(Point3(-1,0,0)))

		renderer ! AddAvatar("iso-entity", dock1)
		renderer ! ChangeAvatar(dock1, dock1Config)
		renderer ! ChangeAvatar(dock1, AvatarBaseStates.MoveAt(Point3(-2,-1,0)))

		renderer ! AddAvatar("iso-entity", dock2)
		renderer ! ChangeAvatar(dock2, dock2Config)
		renderer ! ChangeAvatar(dock2, AvatarBaseStates.MoveAt(Point3(-1,-1,0)))

		renderer ! AddAvatar("iso-entity", habitat1)
		renderer ! ChangeAvatar(habitat1, habitat1Config)
		renderer ! ChangeAvatar(habitat1, AvatarBaseStates.MoveAt(Point3(-2,3,0)))

		renderer ! AddAvatar("iso-entity", somadrop1)
		renderer ! ChangeAvatar(somadrop1, somadrop1Config)
		renderer ! ChangeAvatar(somadrop1, AvatarBaseStates.MoveAt(Point3(-1,1,0)))
	}

	protected def stopGame() {
		sys.exit
	}

	protected def controlGame() {
		Timer.timer.printAvgs("TestIsoGame")
	}
}