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

import org.sofa.nio._
import org.sofa.math.{Rgba, Point2, Point3, Vector3, Vector4, Axes, AxisRange}
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight, TexParams, TexMin, TexMag, TexMipMap, TexAlpha, Libraries, ShaderResource, TextureResource, ArmatureResource}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.armature.{Armature, Joint}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController, ScrollEvent, MotionEvent, KeyEvent}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}
import org.sofa.opengl.mesh.skeleton.{Bone => SkelBone}
import org.sofa.opengl.akka.SurfaceExecutorService

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef}

// The goald of this test is to demonstrate how to use actors (in their own thread) to
// command armatures displayed in the UI thread. 

// == Actors ========================================================================

object RendererActor {
	def apply(system:ActorSystem, renderer:TestRobot):ActorRef = {
		SurfaceExecutorService.setSurface(renderer.surface)
		system.actorOf(Props(new RendererActor(renderer)).withDispatcher(SurfaceExecutorService.configKey), name="renderer-actor")
	}
}

class RendererActor(val renderer:TestRobot) extends Actor {
	var count = 0
	
	var startTime = Platform.currentTime

	def receive() = {
		case ReceiveTimeout ⇒ {
			val T        = Platform.currentTime
			val duration = T - startTime
			startTime    = T

			if(count > 300) {
				self ! "kill!"
			} else {
				count += 1
				
				if(renderer.behavior.finished(T)) {
					if(renderer.behavior.name == "rup") {
						println("goright")
						renderer.behavior = renderer.moveRight(T)
					} else if(renderer.behavior.name == "goright") {
						println("rdown")
						renderer.behavior = renderer.downRLeg(T)
					} else if(renderer.behavior.name == "rdown") {
						println("lup")
						renderer.behavior = renderer.upLLeg(T)
					} else if(renderer.behavior.name == "lup") {
						println("ldown")
						renderer.behavior = renderer.downLLeg(T)
					} else if(renderer.behavior.name == "ldown") {
						//println("goleft")
						//renderer.behavior = renderer.moveLeft(T)
					//} else if( renderer.behavior.name == "goleft") {
						println("rup")
						renderer.behavior = renderer.upRLeg(T)
					}
				}
			}
//			println("T=%d".format(duration))
		}
		case "start" ⇒ {
			context.setReceiveTimeout(100 millisecond)
		}
		case "kill!" ⇒ {
			context.stop(self)
		}
	}

	override def postStop() {
		sys.exit
	}
}

// == TestRobot and Renderer ========================================================

object TestRobot extends App {
	SurfaceExecutorService.configure(50)

	start

	def start() {
		val renderer    = new TestRobot()
	    renderer.camera = Camera()
	    val caps        = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2))
	    
	    renderer.camera.viewportPx(1280,800)
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(8)

        renderer.ctrl           = new RobotCameraController(renderer.camera, renderer)
	    renderer.initSurface    = renderer.initializeSurface
	    renderer.frame          = renderer.display
	    renderer.surfaceChanged = renderer.reshape
	    renderer.close          = { surface => sys.exit }
	    renderer.key            = renderer.ctrl.key
	    renderer.motion         = renderer.ctrl.motion
	    renderer.scroll         = renderer.ctrl.scroll
	    renderer.surface        = new org.sofa.opengl.backend.SurfaceNewt(renderer,
	    						renderer.camera, "Robot !", caps,
	    						org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)

	    renderer.configureActors
	}
}

class TestRobot extends SurfaceRenderer {
// General

    var gl:SGL = null
    var surface:Surface = null
	
// View
    
	var axes = Axes(AxisRange(-0.5,0.5), AxisRange(-0.5,0.5), AxisRange(-1,1))
    var camera:Camera = null
    var ctrl:BasicCameraController = null
    var libraries:Libraries = null
    var zoom = 1.0

// Geometry

	val grid = new LinesMesh(40+2)

    var armature:Armature = null

// Shading
    
    val clearColor = Rgba.Grey90
    val gridColor = Rgba.Grey40
    val xAxisColor = Rgba(0.6,0,0.7,1)
    val yAxisColor = Rgba(0.9,0.7,0,1)
    
    var gridShader:ShaderProgram = null
    
// Actor

	var system:ActorSystem = null
	var surfaceActor:ActorRef =  null

// Go
            
    /** Configure the actors used to drive the animation. This is
      * called when this class is created automatically. */
	def configureActors() {
		system = ActorSystem("Robot")
		surfaceActor = RendererActor(system, this)
		surfaceActor ! "start"
	}

// Rendering
    
	def initializeSurface(gl:SGL, surface:Surface) {
		Shader.path   += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/es2"
		Shader.path   += "shaders"
	    Texture.path  += "/Users/antoine/Documents/Art/Images/Bruce_Art"
	    Texture.path  += "textures"
	    Armature.path += "/Users/antoine/Documents/Art/Images/Bruce_Art"
	    Armature.path += "svg"

	    initGL(gl)
        initShaders
	    initTextures("Robot2.png")
        initArmatures("Robot2.svg")
	    initGeometry
	    initBehaviors
	    
	    camera.viewCartesian(0, 10, 10)
	    camera.setFocus(0, 2, 0)
	    reshape(surface)
	}

	protected def initGL(sgl:SGL) {
	    gl = sgl
	    
        gl.clearColor(clearColor)
	    gl.clearDepth(1f)
	    gl.enable(gl.DEPTH_TEST)
	    
	    gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CW)
        
        gl.disable(gl.BLEND)
//		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
		gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)		// Premultiplied alpha

        libraries = Libraries(gl)
	}
	
	protected def initShaders() {
		libraries.shaders += ShaderResource("plain-shader", "plain_shader.vert.glsl", "plain_shader.frag.glsl")
		libraries.shaders += ShaderResource("armature-shader", "armature_shader.vert.glsl", "armature_shader.frag.glsl")

		gridShader = libraries.shaders.get(gl, "plain-shader")
	}
	
	protected def initTextures(texFileName:String) {
		// TODO make a TextureParams class
		// allowing to describe the texture repeat, filters, files, mipmaps, etc.

		libraries.textures += TextureResource("armature-texture", texFileName,
			TexParams(mipMap=TexMipMap.Load,alpha=TexAlpha.Premultiply,
				      minFilter=TexMin.LinearAndMipMapLinear,magFilter=TexMag.Linear))
	}

	protected def initArmatures(armatureFileName:String) {
		libraries.armatures += ArmatureResource("armature-test", "armature-texture", "armature-shader", armatureFileName)
	}
	
	protected def initGeometry() {
		import VertexAttribute._

		grid.setXYGrid(1f, 1f, 0f, 0f, 20, 20, 0.1f, 0.1f, gridColor, xAxisColor, yAxisColor)
		grid.newVertexArray(gl, gridShader, Vertex -> "position", Color -> "color")

		armature = libraries.armatures.get(gl, "armature-test")

		armature.init(gl, libraries)

		(armature \\ "bipbip2").visible = false
		(armature \\ "mouthoh").visible = false
		
		println(armature.toIndentedString)
	}

	var behavior:Behavior = null

	protected def initBehaviors() {
		(armature \\ "lleg").angle     = -0.3
		(armature \\ "lforeleg").angle =  0.3
		(armature \\ "lfoot").angle    =  0.0

		behavior = upRLeg(Platform.currentTime)
	}

	def upRLeg(t:Long):Behavior = {
		val b = new AggregateBehavior("rup",
			new InterpolateToAngleBehavior("leg",     (armature \\ "rleg"),      0.7, t+100),
			new InterpolateToAngleBehavior("foreleg", (armature \\ "rforeleg"), -0.5, t+100),
			new InterpolateToAngleBehavior("foot",    (armature \\ "rfoot"),    -0.2, t+100)
		)
		b.start(t)
		b
	}

	def downRLeg(t:Long):Behavior = {
		val b = new AggregateBehavior("rdown",
			new InterpolateToAngleBehavior("leg",     (armature \\ "rleg"),      0.3, t+100),
			new InterpolateToAngleBehavior("foreleg", (armature \\ "rforeleg"), -0.3, t+100),
			new InterpolateToAngleBehavior("foot",    (armature \\ "rfoot"),     0.0, t+100)
		)
		b.start(t)
		b
	}

	def moveRight(t:Long):Behavior = {
		val b = new AggregateBehavior("goright", 
			new InterpolateMoveBehavior(   "body",    (armature \\ "root"),     (0.025, 0), t+100),
			new InterpolateToAngleBehavior("leg",     (armature \\ "lleg"),     -0.4,       t+100),
			new InterpolateToAngleBehavior("foreleg", (armature \\ "lforeleg"),  0.0,       t+100),
			new InterpolateToAngleBehavior("foot",    (armature \\ "lfoot"),     0.1,       t+100)
		)
		b.start(t)
		b
	}

	def upLLeg(t:Long):Behavior = {
		val b = new AggregateBehavior("lup",
			new InterpolateToAngleBehavior(   "leg",     (armature \\ "lleg"),     -0.7,       t+100),
			new InterpolateToAngleBehavior(   "foreleg", (armature \\ "lforeleg"),  0.5,       t+100),
			new InterpolateToAngleBehavior(   "foot",    (armature \\ "lfoot"),     0.2,       t+100)
		)
		b.start(t)
		b
	}

	def downLLeg(t:Long):Behavior = {
		val b = new AggregateBehavior("ldown",
			new InterpolateToAngleBehavior("leg",     (armature \\ "lleg"),     -0.3, t+100),
			new InterpolateToAngleBehavior("foreleg", (armature \\ "lforeleg"),  0.3, t+100),
			new InterpolateToAngleBehavior("foot",    (armature \\ "lfoot"),     0.0, t+100)
		)
		b.start(t)
		b
	}

	def moveLeft(t:Long):Behavior = {
		val b = new AggregateBehavior("goleft",
				new InterpolateToPositionBehavior("body", (armature \\ "root"), (0, 0), t+100)
		)
		b.start(t)
		b
	}
	
	def reshape(surface:Surface) {
	    camera.viewportPx(surface.width, surface.height)
        gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
        resetCameraProjection
	}

	def resetCameraProjection() {
        var ratio = camera.viewportRatio
		camera.orthographic(axes.x.from*ratio*zoom, axes.x.to*ratio*zoom, axes.y.from*zoom, axes.y.to*zoom, axes.z.to, axes.z.from)
	}
	
//private[this] var T = 0L

	def display(surface:Surface) {
	    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)

//	    var t = Platform.currentTime
//	    var d = t - T
//	    T = t

	    animate

	 	displayGrid

	 	gl.enable(gl.BLEND)
	 	gl.disable(gl.DEPTH_TEST)
	    gl.disable(gl.CULL_FACE)
	 	armature.display(gl, camera)

	    surface.swapBuffers
	    gl.checkErrors
//println("animate T = %d".format(d))
	}

	protected def displayGrid() {
		gridShader.use
		camera.setUniformMVP(gridShader)
		grid.lastVertexArray.draw(grid.drawAs)
	}

	def animate() {
		if(behavior ne null)
			behavior.animate(Platform.currentTime)
	}	
}


// -- Behaviors ----------------------------------------------------------------------------


abstract class Behavior(val name:String) {
	def start(t:Long)
	def animate(t:Long)
	def finished(t:Long):Boolean
}

class AggregateBehavior(name:String, val behaviors:Behavior *) extends Behavior(name) {
	def start(t:Long) { behaviors.foreach { _.start(t) } }
	def animate(t:Long) { behaviors.foreach { _.animate(t) } }
	def finished(t:Long):Boolean = { behaviors.find { b => b.finished(t) == false } match {
			case None => true
			case _    => false
		}
	}
}

abstract class JointBehavior(name:String, val joint:Joint) extends Behavior(name) {
}

abstract class InterpolateBehavior(name:String, joint:Joint, val to:Long) extends JointBehavior(name, joint) {
	var from:Double = 0.0
	
	def start(t:Long) {	from = t }

	protected def interpolation(t:Long):Double = (t-from).toDouble / (to-from).toDouble

	def finished(t:Long):Boolean =  (t >= to)
}

class InterpolateToAngleBehavior(name:String, joint:Joint, val targetAngle:Double, to:Long) extends InterpolateBehavior(name, joint, to) {
	var startAngle:Double = 0.0

	override def start(t:Long) {
		super.start(t)
		startAngle = joint.angle
	}

	def animate(t:Long) {
		if(finished(t)) {
			joint.angle = targetAngle
		} else {
			joint.angle = startAngle + ((targetAngle - startAngle) * interpolation(t))
//println("joint %s angle %f (%% == %f)".format(joint.name, joint.angle, percent))
		}
	}
}

// XXX TODO redo this with Point2 and arimthemtic operators !!! XXX
class InterpolateToPositionBehavior(name:String, joint:Joint, val targetPosition:(Double,Double), to:Long) extends InterpolateBehavior(name, joint, to) {
	var startPosition = new Point2(0,0)

	override def start(t:Long) {
		super.start(t)
		startPosition.copy(joint.translation)
	}

	def animate(t:Long) {
		if(finished(t)) {
			joint.translation.set(targetPosition._1, targetPosition._2)
		} else {
			val interp =  interpolation(t)
			joint.translation.set(
				startPosition.x + ((targetPosition._1 - startPosition.x) * interp),
				startPosition.y + ((targetPosition._2 - startPosition.y) * interp)
			)
		}
	}
}

class InterpolateMoveBehavior(name:String, joint:Joint, val displacement:(Double,Double), to:Long) extends InterpolateBehavior(name, joint, to) {
	var startPosition = new Point2(0,0)

	override def start(t:Long) {
		super.start(t)
		startPosition.copy(joint.translation)
	}

	def animate(t:Long) {
		if(finished(t)) {
			joint.translation.set(startPosition.x + displacement._1, startPosition.y + displacement._2)
		} else {
			val interp = interpolation(t)
			joint.translation.set(
				startPosition.x + (displacement._1 * interp),
				startPosition.y + (displacement._2 * interp)
			)
		}
	}
}


// -- User Interaction ---------------------------------------------------------------------------


class RobotCameraController(camera:Camera, val renderer:TestRobot) extends BasicCameraController(camera) {
	val oldPos = Point3(0,0,0)
	
	val vector = Vector3()

	override def key(surface:Surface, e:KeyEvent) {
	    import e.ActionChar._
	 
	    if(! e.isPrintable) {
	    	e.actionChar match {
		    	case Escape   => { renderer.zoom = 1.0; renderer.resetCameraProjection }
		    	case Space    => { renderer.zoom = 1.0; renderer.resetCameraProjection }
		    	case _        => {}
	    	}
	    }
	}       
	
	override def scroll(surface:Surface, e:ScrollEvent) {
		zoom(e.amount * step * 0.05)
	} 

	override def motion(surface:Surface, event:MotionEvent) {
		if(event.isStart) {
			oldPos.set(event.x, event.y, event.pressure)
		} else if(event.isEnd) {
			vector.set(event.x-oldPos.x, event.y-oldPos.y, 0)
			oldPos.set(event.x, event.y, event.pressure)
			zoom(vector.y*0.005)
		} else {
			vector.set(event.x-oldPos.x, event.y-oldPos.y, 0)
			oldPos.set(event.x, event.y, event.pressure)
			zoom(vector.y*0.005)
		}
	}

	protected def zoom(amount:Double) {
		renderer.zoom += amount

		if(renderer.zoom < 0.1) renderer.zoom = 0.1
	    else if(renderer.zoom > 100) renderer.zoom = 100.0
	    
	    renderer.resetCameraProjection
	}
}