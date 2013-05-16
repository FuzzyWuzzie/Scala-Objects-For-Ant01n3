package org.sofa.opengl.test

import scala.math._
import scala.collection.mutable.HashMap
import scala.concurrent.duration._
import scala.language.postfixOps

import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.nio._
import org.sofa.math.{Rgba, Point3, Vector3, Vector4, Axes, AxisRange}
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight, TexParams, TexMin, TexMag, TexMipMap, TexAlpha, Libraries, ShaderResource, TextureResource, ArmatureResource}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.armature.Armature
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
	
	var startTime = 0L

	def receive() = {
		case ReceiveTimeout ⇒ {
			val T        = System.currentTimeMillis
			val duration = T - startTime
			startTime    = T

			if(count > 300) {
				self ! "kill!"
			} else {
				count += 1
				renderer.animate 
			}
			println("T=%d".format(duration))
			println(s"executing in ${Thread.currentThread.getName} (count=${count})")
		}
		case "start" ⇒ {
			println("@surface-renderer-actor started...")
			println(s"executing in ${Thread.currentThread.getName} (count=${count})")
			context.setReceiveTimeout(21 millisecond)
		}
		case "kill!" ⇒ {
			println("@surface-renderer-actor exiting...")
			println(s"executing in ${Thread.currentThread.getName} (count=${count})")
			context.stop(self)
		}
	}

	override def postStop() {
		sys.exit
	}
}

// == TestRobot and Renderer ========================================================

object TestRobot extends App {
	SurfaceExecutorService.configure

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

	val grid = new LinesMesh(40)

    var armature:Armature = null

// Shading
    
    val clearColor = Rgba.grey90
    val gridColor = Rgba.grey40
    
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

		grid.setXYGrid(1f, 1f, 0f, 0f, 20, 20, 0.1f, 0.1f, gridColor)
		grid.newVertexArray(gl, gridShader, Vertex -> "position", Color -> "color")

		armature = libraries.armatures.get(gl, "armature-test")

		armature.init(gl, libraries)

		(armature \\ "bipbip2").visible = false
		(armature \\ "mouthoh").visible = false
		
		println(armature.toIndentedString)
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
	
	def display(surface:Surface) {
	    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)

	 	displayGrid

	 	gl.enable(gl.BLEND)
	 	gl.disable(gl.DEPTH_TEST)
	    gl.disable(gl.CULL_FACE)
	 	armature.display(gl, camera)

	    surface.swapBuffers
	    gl.checkErrors
	}

	protected def displayGrid() {
		gridShader.use
		camera.setUniformMVP(gridShader)
		grid.lastVertexArray.draw(grid.drawAs)
	}
	
	case class JointAnim(var from:Double, var to:Double, var step:Double) {
		var value = from

		def animate():Double = {
			value += step 

			if(value > to)
				{ value = to; step = -step }
			else if(value < from)
				{ value = from; step = -step }

			value
		}
	}

	case class FireTimer(val duration:Int) {
		protected var time = 0

		def animate():Boolean = {
			time += 1

			if(time == duration) {
				time = 0
				true
			} else {
				false
			}
		}
	}

	case class TwoStatesTimer(val duration1:Int, val duration2:Int) {
		protected var time = 0
		var state =  false

		def animate():Boolean = {
			time += 1

			if(state && time == duration1) {
				time = 0
				state = ! state
			} else if(!state && time == duration2) {
				time = 0
				state = ! state
			}

			state
		}
	}

	val bipAnim     = TwoStatesTimer(30, 30)
	val grinAnim    = TwoStatesTimer(60, 20)
	val antenaAnim  = JointAnim(-0.2, 0.2, 0.02)
	val antenaScale = JointAnim(-0.2, 0, 0.04)
	val larmAnim    = JointAnim(-0.2, 0.2, 0.03)
	val rarmAnim    = JointAnim(-0.2, 0.2, 0.03)
	val headAnim    = JointAnim(-0.1, 0.1, 0.01)
	val clawAnim    = JointAnim(-0.2, 0.2, 0.06)
	val bodyAnim    = JointAnim(-0.1, 0.1, 0.02)

	def animate() {
		val bip = bipAnim.animate
		(armature \\ "bipbip2").visible = bip
		(armature \\ "bipbip1").visible = ! bip

		val grin = grinAnim.animate
		(armature \\ "mouthgrin").visible = grin
		(armature \\ "mouthoh").visible = ! grin

		if(grin) {
			(armature \\ "leyebrow").angle = 0
			(armature \\ "reyebrow").angle = 0
		} else {
			(armature \\ "leyebrow").angle =  0.2
			(armature \\ "reyebrow").angle = -0.2		
		}

		(armature \\ "antena").angle = antenaAnim.animate
		(armature \\ "antena").scale.set(1, 1+antenaScale.animate)

		(armature \\ "head").angle   = headAnim.animate 
		(armature \\ "larm").angle   = larmAnim.animate
		(armature \\ "rarm").angle   = -rarmAnim.animate

		val clawAngle = clawAnim.animate

		(armature \\ "lupclaw").angle   =  clawAngle
		(armature \\ "ldownclaw").angle = -clawAngle
		(armature \\ "rupclaw").angle   = -clawAngle
		(armature \\ "rdownclaw").angle =  clawAngle

		val value = bodyAnim.animate
		(armature \\ "root").translation.set(value*0.1, 0)
		(armature \\ "lleg").angle = value //translation.set(0,  value*0.001)
		(armature \\ "rleg").angle = -value//   translation.set(0, -value*0.001)
	}
}

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