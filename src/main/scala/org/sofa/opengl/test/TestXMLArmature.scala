package org.sofa.opengl.test

import scala.math._
import scala.collection.mutable.HashMap
import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.nio._
import org.sofa.math.{Rgba, Vector3, Vector4, Axes, AxisRange}
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight, TexParams, TexMin, TexMag, TexMipMap, TexAlpha, Libraries, ShaderResource, TextureResource, ArmatureResource}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.armature.Armature
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController, ScrollEvent, MotionEvent, KeyEvent}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}
import org.sofa.opengl.mesh.skeleton.{Bone => SkelBone}

object TestXMLArmature { def main(args:Array[String]):Unit = (new TestXMLArmature).test }

class TestXMLArmature extends SurfaceRenderer {
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

	val grid = new LinesMesh(42)

    var armature:Armature = null

// Shading
    
    val clearColor = Rgba.Grey90
    val gridColor = Rgba.Grey40
    
    var gridShader:ShaderProgram = null
    
// Go
        
    def test() {
	    camera   = Camera(); camera.viewportPx(1280,800)
	    val caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2))
	    
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(8)

        ctrl           = new TestXMLArmatureCameraController(camera,this)
	    initSurface    = initializeSurface
	    frame          = display
	    surfaceChanged = reshape
	    close          = { surface => sys.exit }
	    key            = ctrl.key
	    motion         = ctrl.motion
	    scroll         = ctrl.scroll
	    surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
	    					camera, "Armature loader test", caps,
	    					org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)
	}
    
// Rendering
    
	def initializeSurface(gl:SGL, surface:Surface) {
		Shader.path      += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/es2"
		Shader.path      += "shaders"
	    Texture.path     += "/Users/antoine/Documents/Art/Images/Bruce_Art"
	    Texture.path     += "textures"
	    Armature.path    += "/Users/antoine/Documents/Art/Images/Bruce_Art"
	    Armature.path    += "svg"

	    initGL(gl)
        initShaders
	    initTextures("Robot.png")
        initArmatures("Robot.svg")
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

	    animate

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
		camera.uniformMVP(gridShader)
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
			(armature \\ "leyebrow").transform.angle = 0
			(armature \\ "reyebrow").transform.angle = 0
		} else {
			(armature \\ "leyebrow").transform.angle =  0.2
			(armature \\ "reyebrow").transform.angle = -0.2		
		}

		(armature \\ "antena").transform.angle = antenaAnim.animate
		(armature \\ "antena").transform.scale.set(1, 1+antenaScale.animate)

		(armature \\ "head").transform.angle = headAnim.animate 
		(armature \\ "larm").transform.angle = larmAnim.animate
		(armature \\ "rarm").transform.angle = -rarmAnim.animate

		val clawAngle = clawAnim.animate

		(armature \\ "lupclaw").transform.angle   =  clawAngle
		(armature \\ "ldownclaw").transform.angle = -clawAngle
		(armature \\ "rupclaw").transform.angle   = -clawAngle
		(armature \\ "rdownclaw").transform.angle =  clawAngle

		val value = bodyAnim.animate
		(armature \\ "root").transform.translation.set(value*0.1, 0)
		(armature \\ "lleg").transform.angle = value //translation.set(0,  value*0.001)
		(armature \\ "rleg").transform.angle = -value//   translation.set(0, -value*0.001)
	}
}

class TestXMLArmatureCameraController(camera:Camera, val app:TestXMLArmature) extends BasicCameraController(camera) {
	override def scroll(surface:Surface, e:ScrollEvent) {
	    app.zoom += (e.amount * step * 0.05)
	    
	    if(app.zoom < 0.1) app.zoom = 0.1
	    else if(app.zoom > 100) app.zoom = 100.0
	    
	    app.resetCameraProjection
	} 
	
	override def key(surface:Surface, e:KeyEvent) {
	    import e.ActionChar._
	 
	    if(! e.isPrintable) {
	    	e.actionChar match {
		    	// case PageUp   => { camera.zoomView(-step) } 
		    	// case PageDown => { camera.zoomView(step) }
		    	// case Up       => { camera.rotateViewVertical(step) }
		    	// case Down     => { camera.rotateViewVertical(-step) }
		    	// case Left     => { camera.rotateViewHorizontal(-step) }
		    	// case Right    => { camera.rotateViewHorizontal(step) }
		    	case Escape   => { app.zoom = 1.0; app.resetCameraProjection }
		    	case Space    => { app.zoom = 1.0; app.resetCameraProjection }
		    	case _        => {}
	    	}
	    }
	}       
	
	override def motion(surface:Surface, e:MotionEvent) {
	}
}