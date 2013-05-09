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
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight, TexParams, TexMin, TexMag, TexMipMap}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}
import org.sofa.opengl.mesh.skeleton.{Bone => SkelBone}

import org.sofa.simu.oberon.renderer._

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

// Geometry

	val grid = new LinesMesh(40)

    var armature:Armature = null

// Shading
    
    val clearColor = Rgba.grey90
    val gridColor = Rgba.grey40
    
    var gridShader:ShaderProgram = null
    
// Go
        
    def test() {
	    camera   = Camera(); camera.viewportPx(1280,800)
	    val caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2))
	    
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(8)

        ctrl           = new BasicCameraController(camera)
	    initSurface    = initializeSurface
	    frame          = display
	    surfaceChanged = reshape
	    close          = { surface => sys.exit }
	    key            = ctrl.key
	    motion         = ctrl.motion
	    scroll         = ctrl.scroll
	    surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
	    					camera, "Aramature loader test", caps,
	    					org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)
	}
    
// Rendering
    
	def initializeSurface(gl:SGL, surface:Surface) {
		Shader.path      += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
		Shader.path      += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/es2/"
		Shader.path      += "shaders/"
	    Texture.path     += "/Users/antoine/Documents/Programs/SOFA/textures"
	    Texture.path     += "/Users/antoine/Documents/Art/Images/Bruce_Art/"
	    Texture.path     += "textures/"
		ColladaFile.path += "/Users/antoine/Documents/Art/Sculptures/Blender/"
		ColladaFile.path += "meshes/"

	    initGL(gl)
        initShaders
	    initTextures("/Users/antoine/Documents/Art/Images/Bruce_Art/TestAreas.png")
        initArmatures("/Users/antoine/Documents/Art/Images/Bruce_Art/TestAreas.svg")
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
		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
//		gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)		// Premultiplied alpha

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
			TexParams(mipMap=TexMipMap.Generate,minFilter=TexMin.LinearAndMipMapLinear,magFilter=TexMag.Linear))
	}

	protected def initArmatures(armatureFileName:String) {
		(new SVGArmatureLoader()).cache("armature-test", "armature-texture", "armature-shader", armatureFileName)
	}
	
	protected def initGeometry() {
		import VertexAttribute._

		grid.setXYGrid(1f, 1f, 0f, 0f, 20, 20, 0.1f, 0.1f, gridColor)
		grid.newVertexArray(gl, gridShader, Vertex -> "position", Color -> "color")

		armature = Armature.armatures.get("armature-test").getOrElse(throw new RuntimeException("not found armature 'armature-test' ?"))

		armature.init(gl, libraries)
		
		println(armature.toIndentedString)
	}
	
	def reshape(surface:Surface) {
	    camera.viewportPx(surface.width, surface.height)
        var ratio = camera.viewportRatio
        gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		camera.orthographic(axes.x.from*(ratio), axes.x.to*(ratio), axes.y.from, axes.y.to, axes.z.to, axes.z.from)
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
		camera.setUniformMVP(gridShader)
		grid.lastVertexArray.draw(grid.drawAs)
	}
	
	class JointAnim(var from:Double, var to:Double, var step:Double) {
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

	val rootAnim = new JointAnim(-0.2, 0.2, 0.001)
	val bAnim    = new JointAnim(-0.3, 0.3, 0.05)
	val cAnim    = new JointAnim(-0.4, 0.4, 0.05)
	val dAnim    = new JointAnim(-0.8, 0.8, 0.05)

	def animate() {
		val root = armature.root

 		(root).angle = rootAnim.animate
 		(root -> "b").angle = bAnim.animate
 		(root -> "c").angle = cAnim.animate
 		(root -> "c" -> "d").angle = dAnim.animate
	}
}