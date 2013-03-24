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
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}
import org.sofa.opengl.mesh.skeleton.{Bone => SkelBone}

import org.sofa.simu.oberon.renderer._

import com.typesafe.config.{ConfigFactory, Config}


// A possible DSL for an armature.
// You give the hierachy, the Z level, a bouding box in pixels inside the
// texture, the pivot point for each box, and the anchor point inside the 
// parent box.

class BrucePicaxe {
  	Armature.armatures += "bruce-picaxe" -> Armature (
        "bruce-picaxe", (687, 772), "bruce-picaxe", "armature-shader",
        Joint ("chest", -0.02, (37,242,162,174), (119,412), (0,0), // <- anchor of the chest could be the initial position of the model.
        	Joint ("head", -0.03 ,(42,23,154,206), (116,189), (113,248),
        		Joint ("r-eyebrow",    0.01, (205, 54, 38, 19), (225, 64), (133, 88)),
        		Joint ("l-eyebrow",    0.01, (249, 54, 34, 19), (267, 64), (176, 88)),
        		Joint ("helmet",       0.02, (302, 28,154, 79), (370, 98), (118, 84)),
        		Joint ("mouth-grin",   0.01, (202,134, 69, 43), (241,156), (158,153)),
        		Joint ("mouth-unsure", 0.01, (302,141, 35, 29), (322,154), (158,153))
        	),
        	Joint ("r-arm", 0.00, (214,193,155,133), (252,242), (68,276),
        		Joint ("r-forearm", -0.01, (385,111,283,250), (420,306), (351,284)),
        		Joint ("l-forearm", -0.03, (521,376,109, 60), (526,433), (351,284))
        	),
        	Joint ("pelvis", 0.00, (44,448,136, 97), (117,468), (119,412),
        		Joint ("l-leg", -0.03, (358,371,137,134), (416,426), (122,526), 
        		  	Joint ("l-foreleg", -0.04, (358,521,137,134), (416,578), (438,493),
        		  	  	Joint("l-shoe", -0.06, (327,671,145, 70), (372,700), (418,642))
        		  	)
        		),
        		Joint ("r-leg", -0.01, (209,363,132,134), (286,418), (101,523),
        		  	Joint ("r-foreleg", -0.02, (185,517,137,135), (262,574), (269,487),
        		  		Joint ("r-shoe", -0.05, (160,675,145, 65), (199,700), (220,640))
        		  	)
        		)
        	)
        )
    )
}

object TestArmature { def main(args:Array[String]):Unit = (new TestArmature).test }

class TestArmature extends SurfaceRenderer {
// General
    
    var gl:SGL = null
    var surface:Surface = null
    var config:Config = null
	
// View
    
	var axes = Axes(AxisRange(-1,1), AxisRange(-1,1), AxisRange(-1,1))
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

    var bruceTex:Texture = null
    
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
	    					camera, "Aramature test", caps,
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

		initConfig
	    initGL(gl)
	    initTextures
        initShaders
	    initGeometry
	    
	    camera.viewCartesian(0, 10, 10)
	    camera.setFocus(0, 2, 0)
	    reshape(surface)
	}

	protected def initConfig() {
		var file = new java.io.File("/Users/antoine/Documents/Art/Images/Bruce_Art/Bruce_Picaxe_150dpi.config")

		if(!file.exists)
			throw new RuntimeException("prout")
		
		config = ConfigFactory.parseFile(file).getConfig("game.resources.armatures.bruce_picaxe")

		println("%s".format(config))
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

        libraries = Libraries(gl)
	}
	
	protected def initShaders() {
		libraries.shaders += ShaderResource("plain-shader", "plain_shader.vert.glsl", "plain_shader.frag.glsl")
		libraries.shaders += ShaderResource("armature-shader", "armature_shader.vert.glsl", "armature_shader.frag.glsl")

		gridShader = libraries.shaders.get(gl, "plain-shader")
	}
	
	protected def initTextures() {
		libraries.textures += TextureResource("bruce-picaxe", config.getString("picture"), false)
	}
	
	protected def initGeometry() {
		import VertexAttribute._

		grid.setXYGrid(1f, 1f, 0f, 0f, 20, 20, 0.1f, 0.1f, gridColor)
		grid.newVertexArray(gl, gridShader, Vertex -> "position", Color -> "color")

		new BrucePicaxe()

		armature = Armature.armatures.get("bruce-picaxe").getOrElse(throw new RuntimeException("not found bruce-picaxe ?"))

		armature.init(gl, libraries)
//println("%s".format(armature))
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
	    gl.enable(gl.CULL_FACE)
	 	armature.display(gl, camera)

	    surface.swapBuffers
	    gl.checkErrors
	}

	protected def displayGrid() {
		gridShader.use
		camera.setUniformMVP(gridShader)
		grid.lastVertexArray.draw(grid.drawAs)
	}
	
	def animate() {
	}
	
	// def useTextures(shader:ShaderProgram, color:Texture, nmap:Texture) {
	//    	color.bindTo(gl.TEXTURE0)
	//     shader.uniform("texColor", 0)
	//     nmap.bindTo(gl.TEXTURE1)
	//     shader.uniform("texNormal", 1)
	// }
}