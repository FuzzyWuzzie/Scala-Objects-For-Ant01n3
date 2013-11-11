package org.sofa.opengl.test

import scala.math._
import scala.concurrent.duration._
import scala.compat.Platform

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef}

import org.sofa.math.Axes
import org.sofa.opengl.akka.SurfaceExecutorService
import org.sofa.opengl.avatar.renderer.{Renderer, RendererActor, AvatarFactory, Screen, Avatar, NoSuchAvatarException, NoSuchScreenException}
import org.sofa.opengl.avatar.renderer.sprite.{ImageSprite}
import org.sofa.opengl.avatar.renderer.screen.{HexaTilesScreen}

class HexaAvatarFactory(val renderer:Renderer) extends AvatarFactory {
	def screenFor(name:String, screenType:String):Screen = screenType match {
		case "hexatiles" ⇒ new HexaTilesScreen(name, renderer)
		case _           ⇒ throw new NoSuchScreenException(s"cannot create a screen of type $screenType, unknown type")
	}

	def avatarFor(name:String, avatarType:String, indexed:Boolean):Avatar = avatarType match {
//		case "hexatile" ⇒ new HexaTileAvatar(name, renderer.screen, indexed)
		case "image"    ⇒ new ImageSprite(name, renderer.screen, indexed)
		case _          ⇒ throw new NoSuchAvatarException(s"cannot create an avatar of type $avatarType, unknown type")
	}
}


object TestHexaTilesScreen extends App {
	case class Start()
	case class Exit()

	val actorSystem = ActorSystem("HexaWorld", SurfaceExecutorService.config(10L))

	val test = actorSystem.actorOf(Props[HexaWorld], name="test")

	test ! Start
}


class HexaWorld extends Actor {
	import TestHexaTilesScreen.{Start, Exit}

	val renderer = Renderer(self)

	val rendererActor = RendererActor(context.system, renderer, new HexaAvatarFactory(renderer), "HexaWorld", 1200, 744, 30)

	def receive = {
		case Start ⇒ start
		case Exit  ⇒ exit
	}

	protected def exit() { context.stop(self) }

	override def postStop() { sys.exit }

	protected def start() {
		declareResources
		setupScreen
	}

	protected def declareResources() {
		import RendererActor._

		rendererActor ! AddResources("/TestHexaTilesScreen.xml")
	}

	protected def setupScreen() {
		import RendererActor._
		import HexaTilesScreen._

		rendererActor ! AddScreen("hexaworld-screen", "hexatiles")
		rendererActor ! SwitchScreen("hexaworld-screen")
		rendererActor ! ChangeScreenSize(Axes((0.0, 10.0), (0.0, 10.0), (0.0, 0.0)), 1.0)
	}
}