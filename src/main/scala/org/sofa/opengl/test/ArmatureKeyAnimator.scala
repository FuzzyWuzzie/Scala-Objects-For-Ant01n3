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
import org.sofa.opengl.armature.behavior.{ArmatureBehavior, ArmatureBehaviorLoader, LerpKeyArmature, Switch, Loop}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController, ScrollEvent, MotionEvent, KeyEvent}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}
import org.sofa.opengl.mesh.skeleton.{Bone => SkelBone}
import org.sofa.opengl.text.{GLFont, GLString}


// == ArmatureKeyAnimator and Renderer ========================================================

object ArmatureKeyAnimator extends App {
//	SurfaceExecutorService.configure(50)

	start

	def start() {
		val renderer    = new ArmatureKeyAnimator()
	    renderer.camera = Camera()
	    val caps        = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2))
	    
	    renderer.camera.viewportPx(1280,800)
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(8)

        renderer.ctrl           = new ArmatureKeyAnimatorInteraction(renderer.camera, renderer)
	    renderer.initSurface    = renderer.initializeSurface
	    renderer.frame          = renderer.display
	    renderer.surfaceChanged = renderer.reshape
	    renderer.close          = { surface => sys.exit }
	    renderer.key            = renderer.ctrl.key
	    renderer.motion         = renderer.ctrl.motion
	    renderer.scroll         = renderer.ctrl.scroll
	    renderer.surface        = new org.sofa.opengl.backend.SurfaceNewt(renderer,
	    						renderer.camera, "Armature Animator", caps,
	    						org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)
	}

	final val TextFrameNo = 0
	final val TextPartId  = 1
}

class ArmatureKeyAnimator extends SurfaceRenderer {
	import ArmatureKeyAnimator._

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

// Text

	var font:GLFont = null
	var text = new Array[GLString](10)

// Shading
    
    val clearColor = Rgba.Grey90
    val gridColor = Rgba.Grey40
    val xAxisColor = Rgba(0.6,0,0.7,1)
    val yAxisColor = Rgba(0.9,0.7,0,1)
    
    var gridShader:ShaderProgram = null
    var textShader:ShaderProgram = null


// Behavior

	var robot:RobotZazou2 = null
    
// Rendering
    
	def initializeSurface(gl:SGL, surface:Surface) {
		Shader.path   += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/es2"
		Shader.path   += "shaders"
	    Texture.path  += "/Users/antoine/Documents/Art/Images/Bruce_Art"
	    Texture.path  += "textures"
	    Armature.path += "/Users/antoine/Documents/Art/Images/Bruce_Art"
	    Armature.path += "svg"
	    GLFont.path   += "/Users/antoine/Library/Fonts"
	    ArmatureBehavior.path += "/Users/antoine/Downloads/Bordel/"

	    initGL(gl)
        initShaders
	    initGLText
	    initTextures("Robot2.png")
        initArmatures("Robot2.svg")
	    initGeometry
	    initBehaviors
	    
	    camera.eyeCartesian(0, 10, 10)
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
		gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)		// Premultiplied alpha

        libraries = Libraries(gl)
	}
	
	protected def initShaders() {
		libraries.shaders += ShaderResource("plain-shader", "plain_shader.vert.glsl", "plain_shader.frag.glsl")
		libraries.shaders += ShaderResource("armature-shader", "armature_shader.vert.glsl", "armature_shader.frag.glsl")
		libraries.shaders += ShaderResource("text-shader", "text.vert.glsl", "text.frag.glsl")

		gridShader = libraries.shaders.get(gl, "plain-shader")
		textShader = libraries.shaders.get(gl, "text-shader")
	}
	
	protected def initTextures(texFileName:String) {
		// TODO make a TextureParams class
		// allowing to describe the texture repeat, filters, files, mipmaps, etc.

		libraries.textures += TextureResource("armature-texture", texFileName,
			TexParams(mipMap=TexMipMap.Load,alpha=TexAlpha.Premultiply,
				      minFilter=TexMin.LinearAndMipMapLinear,magFilter=TexMag.Linear))
	}

	protected def initGLText() {
		font = new GLFont(gl, "Ubuntu-R.ttf", 80, textShader, true)

		for(i <- 0 until text.size) {
			text(i) = new GLString(gl, font, 256)
			text(i).setColor(Rgba(0,0,0,0.8))
		}

		text(TextFrameNo).build("0ms")
		text(TextPartId).build("?")
	}

	protected def initArmatures(armatureFileName:String) {
		libraries.armatures += ArmatureResource("armature-test", "armature-texture", "armature-shader", armatureFileName, "Armature", libraries, 0.001)
	}
	
	protected def initGeometry() {
		import VertexAttribute._

		grid.setXYGrid(1f, 1f, 0f, 0f, 20, 20, 0.1f, 0.1f, gridColor, xAxisColor, yAxisColor)
		grid.newVertexArray(gl, gridShader, Vertex -> "position", Color -> "color")

		armature = libraries.armatures.get(gl, "armature-test")

		(armature \\ "bipbip2").visible = false
		(armature \\ "mouthoh").visible = false
		
		selected = (armature \\ "root")
		selected.selected = true

		//println(armature.toIndentedString)
		text(TextPartId).build("[%s]".format(selected.name))
		text(TextPartId).setColor(Rgba(0.2, 0.0, 0.3, 0.9))
		text(TextFrameNo).setColor(Rgba(0.3, 0.2, 0, 0.9))
	}

	protected def initBehaviors() {
		robot = new RobotZazou2(armature)
		robot.start
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

	    animate

	 	displayGrid
	 	displayInfo

	 	gl.enable(gl.BLEND)
	 	gl.disable(gl.DEPTH_TEST)
	    gl.disable(gl.CULL_FACE)
	 	armature.display(gl, camera)

	    surface.swapBuffers
	    gl.checkErrors
	}

	protected def displayGrid() {
		gridShader.use
		camera.uniformMVP(gridShader)
		grid.lastVertexArray.draw(grid.drawAs(gl))
	}

	protected def displayInfo() {
		gl.disable(gl.DEPTH_TEST)
		
		camera.pushpop {
			val scale = 0.0007
			camera.translate(-0.7, 0.3, 0)			
			camera.pushpop {
				camera.scale(scale, scale, scale)
				text(TextFrameNo).draw(camera)
			}
			camera.translate(0, -0.1, 0)			
			camera.pushpop {
				camera.scale(scale, scale, scale)
				text(TextPartId).draw(camera)
			}
		}
		
		gl.enable(gl.DEPTH_TEST)
	}

	def animate() {
		robot.animate()
		text(TextFrameNo).build("%dms".format(robot.endTime-robot.startTime))
	}

	var selected:Joint = null
	var selindex = 0

	def selectParent() {
		if(selected ne null) {
			if(selected.parent ne null) {
				selected.selected = false
				selected = selected.parent
				selected.selected = true
				text(TextPartId).build("[%s]".format(selected.name))
				println("-> parent")
			}
		}
	}	

	def selectFirstSub() {
		if(selected ne null) {
			if(selected.subCount > 0) {
				selected.selected = false
				selected = selected.sub(0)
				selected.selected = true
				text(TextPartId).build("[%s]".format(selected.name))
				selindex = 0
				println("-> first sub")
			}
		}
	}

	def selectNextSibling() {
		if((selected ne null) && (selected.parent ne null)) {
			if(selindex+1 < selected.parent.subCount) {
				selindex += 1
				selected.selected = false
				selected = selected.parent.sub(selindex)
				selected.selected = true
				text(TextPartId).build("[%s]".format(selected.name))
				println("-> next sibling")
			}
		}
	}

	def selectPrevSibling() {
		if((selected ne null) && (selected.parent ne null)) {
			if(selindex-1 >= 0) {
				selindex -= 1
				selected.selected = false
				selected = selected.parent.sub(selindex)
				selected.selected = true
				text(TextPartId).build("[%s]".format(selected.name))
				println("-> prev sibling")
			}
		}
	}
}


class RobotZazou2(val armature:Armature) {

	var startTime = 0L

	var endTime = 0L

	var walkBehavior:ArmatureBehavior = new LerpKeyArmature("walk", armature, "Robot3.sifz", 0.05)

	var mouthBehavior:ArmatureBehavior = Loop("mouth", 0, Switch("mouth", 500, armature \\ "mouthgrin", armature \\ "mouthoh"))

	var bipbipBehavior:ArmatureBehavior = Loop("bibbip", 0, Switch("bipbip", 300, armature \\ "bipbip1", armature \\ "bipbip2"))

	def start() {
		val t = Platform.currentTime

		walkBehavior.start(t)
		mouthBehavior.start(t)
		bipbipBehavior.start(t)
	}

	def animate() {
		endTime = Platform.currentTime

		if(startTime == 0L)
			startTime = endTime

		if(walkBehavior.finished(endTime)) {
			walkBehavior.start(endTime)
			startTime = endTime
		} else {
			walkBehavior.animate(endTime)
		}

		mouthBehavior.animate(endTime)
		bipbipBehavior.animate(endTime)
	}
}

// -- User Interaction ---------------------------------------------------------------------------


class ArmatureKeyAnimatorInteraction(camera:Camera, val renderer:ArmatureKeyAnimator) extends BasicCameraController(camera) {
	val oldPos = Point3(0,0,0)
	
	val vector = Vector3()

	override def key(surface:Surface, e:KeyEvent) {
	    import org.sofa.opengl.surface.ActionChar._

	    if(! e.isPrintable) {
	    	e.actionChar match {
		    	case Escape   => { renderer.zoom = 1.0; renderer.resetCameraProjection }
		    	case Space    => { renderer.zoom = 1.0; renderer.resetCameraProjection }
		    	case Up       => { renderer.selectParent }
		    	case Down     => { renderer.selectFirstSub }
		    	case Right    => { renderer.selectNextSibling }
		    	case Left     => { renderer.selectPrevSibling }
		    	case _        => {}
	    	}	    	
	    } else {
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