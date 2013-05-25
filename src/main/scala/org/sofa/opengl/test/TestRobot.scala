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
import org.sofa.opengl.armature.behavior._
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



/** Models the behavior of the robot (give hi-level orders). */
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
				
				if(renderer.robotBehavior.behavior.finished(T)) {
					if(renderer.robotBehavior.behavior.name == "walkRight")
					     renderer.robotBehavior.behavior = renderer.robotBehavior.walkLeft.start(T)
					else renderer.robotBehavior.behavior = renderer.robotBehavior.walkRight.start(T)
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

        renderer.ctrl           = new RobotInteraction(renderer.camera, renderer)
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
	var robotBehavior:RobotBehavior = null

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

	def initBehaviors() {
		robotBehavior = new RobotBehavior(armature)
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
		if((robotBehavior ne null) && (robotBehavior.behavior ne null))
			robotBehavior.behavior.animate(Platform.currentTime)
	}	
}


// -- Robot Behavior -----------------------------------------------------------------------------


class RobotBehavior(val armature:Armature) {
	var behavior:ArmatureBehavior = null

	init

	private[this] final val durationMs = 100

	protected def init() {
		(armature \\ "lleg").transform.angle     = -0.3
		(armature \\ "lforeleg").transform.angle =  0.3
		(armature \\ "lfoot").transform.angle    =  0.0

		behavior = walkRight.start(Platform.currentTime) //upRLeg(Platform.currentTime)
	}

	def walkRight = new DoInSequence("walkRight", upRLeg, moveRight, downRLeg, upLLeg, downLLeg)

	def walkLeft = new DoInSequence("walkLeft", upLLeg, moveLeft, downLLeg, upRLeg, downRLeg)

	def upRLeg = new DoInParallel("upRLeg",
		new InterpToAngle("a", (armature \\ "rleg"),      0.7, durationMs),
		new InterpToAngle("b", (armature \\ "rforeleg"), -0.5, durationMs),
		new InterpToAngle("c", (armature \\ "rfoot"),    -0.2, durationMs)
	)

	def upLLeg = new DoInParallel("upLLeg",
		new InterpToAngle("a", (armature \\ "lleg"),     -0.7, durationMs),
		new InterpToAngle("b", (armature \\ "lforeleg"),  0.5, durationMs),
		new InterpToAngle("c", (armature \\ "lfoot"),     0.2, durationMs)
	)
	
	def downRLeg = new DoInParallel("downRLeg",
		new InterpToAngle("a", (armature \\ "rleg"),      0.3, durationMs),
		new InterpToAngle("b", (armature \\ "rforeleg"), -0.3, durationMs),
		new InterpToAngle("c", (armature \\ "rfoot"),     0.0, durationMs)
	)

	def downLLeg = new DoInParallel("downLLeg",
		new InterpToAngle("a", (armature \\ "lleg"),     -0.3, durationMs),
		new InterpToAngle("b", (armature \\ "lforeleg"),  0.3, durationMs),
		new InterpToAngle("c", (armature \\ "lfoot"),     0.0, durationMs)
	)

	def moveLeft = new DoInParallel("moveLeft",
		new InterpMove("a",    (armature \\ "root"),    (-0.025, 0), durationMs),
		new InterpToAngle("b", (armature \\ "rleg"),      0.4,       durationMs),
		new InterpToAngle("c", (armature \\ "rforeleg"),  0.0,       durationMs),
		new InterpToAngle("d", (armature \\ "rfoot"),    -0.1,       durationMs)
	)
	
	def moveRight = new DoInParallel("moveRight",
		new InterpMove("a",    (armature \\ "root"),     (0.025, 0), durationMs),
		new InterpToAngle("b", (armature \\ "lleg"),     -0.4,       durationMs),
		new InterpToAngle("c", (armature \\ "lforeleg"),  0.0,       durationMs),
		new InterpToAngle("d", (armature \\ "lfoot"),     0.1,       durationMs)
	)
}


// -- User Interaction ---------------------------------------------------------------------------


class RobotInteraction(camera:Camera, val renderer:TestRobot) extends BasicCameraController(camera) {
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