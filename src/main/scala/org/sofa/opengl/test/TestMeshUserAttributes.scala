package org.sofa.opengl.test

import scala.math._
import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.nio._
import org.sofa.math.{Rgba, Vector3, Vector4}
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, WhiteLight, ColoredLight, Light}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, EditableMesh, VertexAttribute}

object TestMeshUserAttributes {
    def main(args:Array[String]):Unit = (new TestMeshUserAttributes)
}

class TestMeshUserAttributes extends SurfaceRenderer {
// General
    
    var gl:SGL = null
    var surface:Surface = null
	
// View
    
    var camera:Camera = null
    var ctrl:BasicCameraController = null
    
// Geometry

    var ground:VertexArray = null    
    val groundMesh = new PlaneMesh(4, 4, 4, 4)
        
// Shading

	var groundShader:ShaderProgram = null
	var groundColor:Texture = null
    val clearColor = Rgba.Grey30

// Go
        
    build()

    private def build() {
	    camera   = Camera(); camera.viewportPx(1280, 800)
	    val caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2))
	    
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(4)

        ctrl           = new BasicCameraController(camera)
	    initSurface    = initializeSuface
	    frame          = display
	    surfaceChanged = reshape
	    close          = { surface => sys.exit }
	    key            = ctrl.key
	    motion         = ctrl.motion
	    scroll         = ctrl.scroll
	    surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
	    					camera, "Mesh user attributes test", caps,
	    					org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)
	}
    
// Rendering
    
	def initializeSuface(gl:SGL, surface:Surface) {
	    Shader.path      += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
	    Texture.path     += "/Users/antoine/Documents/Programs/SOFA/textures"
		ColladaFile.path += "/Users/antoine/Documents/Art/Sculptures/Blender/"

	    initGL(gl)
	    initTextures
        initShaders
	    initGeometry
	    
	    camera.eyeCartesian(0, 5, 4)
	    camera.setFocus(0, 0, 0)
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
	}
	
	protected def initShaders() {
	    groundShader = ShaderProgram(gl, "movingTex", "es2/movingTex.vert.glsl", "es2/movingTex.frag.glsl")
	}
	
	protected def initGeometry() {
		import VertexAttribute._

		groundMesh.setTextureRepeat(30, 30)
		// groundMesh.addUserAttribute("texOffset", 3)

		// ground.setUserData((0, 0), (0, 0, 0))
		// ground.setUserData((1, 0), (0, 0, 0))
		// ground.setUserData((2, 0), (0, 0, 0))
		// ground.setUserData((3, 0), (0, 0, 0))
		
		// ground.setUserData((0, 1), (0, 0, 0))
		// ground.setUserData((1, 1), (1, 0, 0))
		// ground.setUserData((2, 1), (1, 0, 0))
		// ground.setUserData((3, 1), (0, 0, 0))
		
		// ground.setUserData((0, 2), (0, 0, 0))
		// ground.setUserData((1, 2), (1, 0, 0))
		// ground.setUserData((2, 2), (1, 0, 0))
		// ground.setUserData((3, 2), (0, 0, 0))
		
		// ground.setUserData((0, 3), (0, 0, 0))
		// ground.setUserData((1, 3), (0, 0, 0))
		// ground.setUserData((2, 3), (0, 0, 0))
		// ground.setUserData((3, 3), (0, 0, 0))		

	    ground = groundMesh.newVertexArray(gl, groundShader, Vertex -> "position", Color -> "color")
	}
	
	protected def initTextures() {
	//    groundColor = new Texture(gl, "textures/Ground.png", true)
	}
	
	def reshape(surface:Surface) {
	    camera.viewportPx(surface.width, surface.height)
        var ratio = camera.viewportRatio
        gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
        camera.frustum(-ratio, ratio, -1, 1, 2)
	}
	
	def display(surface:Surface) {
	    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
	    
	    camera.lookAt

		// Ground

	  	gl.enable(gl.BLEND)
	    groundShader.use
//	   	color.bindTo(gl.TEXTURE0)
	    groundShader.uniform("texColor", 0)
//	    useTextures(groundShader, groundColor, groundNMap)
	    camera.uniformMVP(groundShader)
	    ground.draw(groundMesh.drawAs(gl))
		gl.disable(gl.BLEND)
	    
	    // Ok

	    surface.swapBuffers
	    gl.checkErrors
	}
}