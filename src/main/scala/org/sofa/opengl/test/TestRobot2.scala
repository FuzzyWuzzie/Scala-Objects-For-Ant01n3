// package org.sofa.opengl.test

// import scala.math._
// import scala.collection.mutable.HashMap
// import scala.concurrent.duration._
// import scala.language.postfixOps
// import scala.compat.Platform

// import javax.media.opengl._
// import javax.media.opengl.glu._
// import com.jogamp.opengl.util._
// import com.jogamp.newt.event._
// import com.jogamp.newt.opengl._

// import org.sofa.nio._
// import org.sofa.math.{Rgba, Point2, Point3, Vector3, Vector4, Axes, AxisRange}
// import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight, TexParams, TexMin, TexMag, TexMipMap, TexAlpha, Libraries, ShaderResource, TextureResource, ArmatureResource}
// import org.sofa.opengl.io.collada.{ColladaFile}
// import org.sofa.opengl.armature.{Armature, Joint}
// import org.sofa.opengl.armature.behavior.{ArmatureBehavior, ArmatureBehaviorLoader, ArmatureKeyInterp, JointVisibilitySwitch, BehaviorLoop}
// import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController, ScrollEvent, MotionEvent, KeyEvent}
// import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}
// import org.sofa.opengl.mesh.skeleton.{Bone => SkelBone}
// import org.sofa.opengl.text.{GLFont, GLString}


// // == TestRobot2 and Renderer ========================================================


// object TestRobot2 extends App {
	
// 	case class Start()

// 	case class Exit()

// 	start 

// 	def start() {
// 		val actorSystem = actorSystem("Robot", SurfaceExecutorService.config(10L))
// 		val test        = actorSystem.actorOf(Props[TestRobot2], name="test")

// 		test ! Start
// 	}
// }

// class TestRobot2 extends Actor {
// 	import TestRobot2._

// 	val renderer = Renderer(self)

// 	val rendererActor:ActorRef = RendererActor(context.system, renderer, new RobotAvatarFactory(renderer))

// 	def receive = {
// 		case Start => start
// 		case Exit  => exit
// 	}

// 	protected def start() {
// 		declareResources
// 		setupScreen
// 		setupScene
// 		setupActors
// 	}

// 	protected def declareResources() {
// 		import RendererActor._

// 		rendererActor ! AddResource(ShaderResource("image-shader", "image_shader.vert.glsl", "image_shader.frag.glsl"))
// 		rendererActor ! AddResource(ShaderResource("plain-shader", "plain_shader.vert.glsl", "plain_shader.frag.glsl"))
// 		rendererActor ! AddResource(TextureResource("scene-near-bg", "scene_near_bg.png", TexParams()))
// 		rendererActor ! AddResource(TextureResource("scene-far-bg", "scene_far_bg.png", TexParams()))
// 		rendererActor ! AddResource(TextureResource("scene-fg", "scene_fg.png", TexParams()))
// 		rendererActor ! AddResource(TextureResource("robot", "robot.png", TexParams()))
// 		//rendererActor ! AddResource(ArmatureResource())
// 	}

// 	protected def setupScreen() {
// 		import RendererActor._

// 		rendererActor ! AddScreen("robot-screen", "robot-screen")
// 		rendererActor ! SwitchScreen("robot-screen")
// 		rendererActor ! ChangeScreenSize(Axes((-0.5, 0.5), (-0.5, 0.5), (-1.0, 1.0)), 0.05)
// 	}

// 	protected def setupScene() {

// 	}

// 	protected def setupActors() {

// 	}

// 	protected def exit() { context.stop(self) /* All other actors are child to this. */ }

// 	override def postStop() { sys.exit }
// }