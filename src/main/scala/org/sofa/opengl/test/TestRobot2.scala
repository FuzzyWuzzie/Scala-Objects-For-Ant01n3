package org.sofa.opengl.test

import scala.math._
import scala.collection.mutable.HashMap
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.compat.Platform

import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef, ActorContext, Terminated}

import org.sofa.nio._
import org.sofa.math.Axis._
import org.sofa.math.{Rgba, Point2, Point3, Vector3, Vector4, Axes, AxisRange, Axis}
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight, TexParams, TexMin, TexMag, TexMipMap, TexAlpha, Libraries, ShaderResource, TextureResource, ArmatureResource}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.armature.{Armature, Joint}
import org.sofa.opengl.armature.behavior.{ArmatureBehavior, ArmatureBehaviorLoader, LerpKeyArmature, Switch, Loop}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController, ScrollEvent, MotionEvent, KeyEvent}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}
import org.sofa.opengl.mesh.skeleton.{Bone => SkelBone}
import org.sofa.opengl.text.{GLFont, GLString}
import org.sofa.opengl.akka.SurfaceExecutorService
import org.sofa.opengl.avatar.renderer.{Renderer, RendererActor, AvatarFactory, Screen, Avatar, NoSuchAvatarException, NoSuchScreenException, SizeFromTextureHeight}
import org.sofa.opengl.avatar.renderer.sprite.{TilesSprite, ImageSprite}
import org.sofa.opengl.avatar.renderer.screen.PerspectiveScreen


// == TestRobot2 and Renderer ========================================================


class RobotAvatarFactory(val renderer:Renderer) extends AvatarFactory {
	def screenFor(name:String, screenType:String):Screen = screenType match {
		case "perspective" ⇒ new PerspectiveScreen(name, renderer)
		case _             ⇒ throw NoSuchScreenException("cannot create a screen of type %s, unknown type".format(screenType))
	}
	def avatarFor(name:String, avatarType:String, indexed:Boolean):Avatar = avatarType match {
		case "image" ⇒ new ImageSprite(name, renderer.screen, indexed)
		case _       ⇒ throw new NoSuchAvatarException("cannot create an avatar of type %s, unknown type".format(avatarType))
	}	
}


object TestRobot2 extends App {
	
	case class Start()

	case class Exit()

	val actorSystem = ActorSystem("Robot", SurfaceExecutorService.config(10L))
	
	val test = actorSystem.actorOf(Props[TestRobot2], name="test")

	test ! Start
}


class TestRobot2 extends Actor {
	import TestRobot2.{Start, Exit}

	val renderer = Renderer(self)

	val rendererActor:ActorRef = RendererActor(context.system, renderer, new RobotAvatarFactory(renderer), "Robot", 1200, 744, 30)

	def receive = {
		case Start => start
		case Exit  => exit
	}

	protected def exit() { context.stop(self) /* All other actors are child to this. */ }

	override def postStop() { sys.exit }

	protected def start() {
		declareResources
		setupScreen
		setupScene
		setupActors
	}

	protected def declareResources() {
		import RendererActor._

		rendererActor ! AddResources("/TestRobot2.xml")
	}

	protected def setupScreen() {
		import RendererActor._
		import PerspectiveScreen._

		rendererActor ! AddScreen("robot-screen", "perspective")
		rendererActor ! SwitchScreen("robot-screen")
		// TODO should be named ChangeScreeenCamera
		rendererActor ! ChangeScreenSize(Axes((-0.25, 0.25), (-0.25, 0.25), (-1.0, 0.5)), 0.05)
		rendererActor ! ChangeScreen(EyeCartesian(0, 0, 1.5))
	}

	protected def setupScene() {
		import RendererActor._
		import ImageSprite._

		rendererActor ! AddAvatar("bg", "image", false)
		rendererActor ! ChangeAvatar("bg", AddState("bg-image", "default", true))
		rendererActor ! ChangeAvatarPosition("bg", Point3(0, 0.25, 0.1))
		rendererActor ! ChangeAvatarSize("bg", SizeFromTextureHeight(1.0, "bg-image"))

		rendererActor ! AddAvatar("ground", "image", false)
		rendererActor ! ChangeAvatar("ground", AddState("ground-image", "default", true))
		rendererActor ! ChangeAvatarPosition("ground", Point3(0, -0.25, 0.6))
		rendererActor ! ChangeAvatarSize("ground", SizeFromTextureHeight(1.0, "ground-image"))
		rendererActor ! ChangeAvatar("ground", ChangeOrientation(Axis.Y))
	}

	protected def setupActors() {

	}
}