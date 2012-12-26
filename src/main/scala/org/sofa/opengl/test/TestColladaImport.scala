package org.sofa.opengl.test

import org.sofa.opengl.VertexArray
import org.sofa.opengl.{SGL, HemisphereLight}
import org.sofa.opengl.mesh.{PlaneMesh, EditableMesh, VertexAttribute}
import org.sofa.opengl.ShaderProgram
import org.sofa.math.Matrix4
import org.sofa.opengl.MatrixStack
import javax.media.opengl.GLAutoDrawable
import javax.media.opengl.GLProfile
import javax.media.opengl.GLCapabilities
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.util.FPSAnimator
import com.jogamp.newt.event.WindowAdapter
import javax.media.opengl.GLEventListener
import org.sofa.opengl.surface.Surface
import org.sofa.opengl.surface.BasicCameraController
import org.sofa.opengl.Camera
import org.sofa.math.Rgba
import org.sofa.opengl.surface.SurfaceRenderer
import org.sofa.math.Vector4
import org.sofa.opengl.Shader
import org.sofa.math.Vector3
import org.sofa.opengl.mesh.Mesh
import org.sofa.opengl.io.collada.ColladaFile

object TestColladaImport {
	def main(args:Array[String]):Unit = { (new TestColladaImport).test }
}

class TestColladaImport extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	var thingShad:ShaderProgram = null
	
	var thing:VertexArray = null
	var plane:VertexArray = null
	
	var thingMesh:Mesh = null
	val planeMesh = new PlaneMesh(2, 2, 10, 10)

	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.grey90
	val thingColor = new Rgba(1, 0.9, 0.6, 1)
	val planeColor = Rgba.grey40

	val hemiLight = new HemisphereLight(0, 5, 0, Rgba.white, Rgba.grey20)
	
	def test() {
		val caps = new GLCapabilities(GLProfile.getGL2ES2)
		
		caps.setRedBits(8)
        caps.setGreenBits(8)
        caps.setBlueBits(8)
        caps.setAlphaBits(8)
        caps.setNumSamples(4)
		caps.setDoubleBuffered(true)
        caps.setHardwareAccelerated(true)
        caps.setSampleBuffers(true)

        camera         = Camera()
        ctrl           = new MyCameraController(camera, hemiLight.pos)
		initSurface    = initializeSurface
		frame          = display
		surfaceChanged = reshape
		key            = ctrl.key
		motion         = ctrl.motion
		scroll         = ctrl.scroll
		close          = { surface => sys.exit }
		surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
							camera, "Test Collada import", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.path  += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
		Shader.path  += "shaders/"
		ColladaFile.path += "/Users/antoine/Documents/Art/Sculptures/Blender/"
		ColladaFile.path += "meshes/"
			
		initGL(sgl)
		initShaders
		initGeometry
		
		camera.viewCartesian(5, 2, 5)
		camera.setFocus(0, 0, 0)
		reshape(surface)
	}
	
	protected def initGL(sgl:SGL) {
		gl = sgl
		
		gl.clearColor(clearColor)
		gl.clearDepth(1f)
		gl.enable(gl.DEPTH_TEST)
		gl.depthFunc(gl.LEQUAL)
		
		gl.enable(gl.CULL_FACE)
		gl.cullFace(gl.BACK)
		gl.frontFace(gl.CW)
		
		gl.disable(gl.BLEND)
	}
	
	def initShaders() {
		thingShad = ShaderProgram(gl, "thing shader", "es2/hemiLightUniClr.vert.glsl", "es2/hemiLightUniClr.frag.glsl")
	}
	
	def initGeometry() {
		import VertexAttribute._

		initThing

		plane = planeMesh.newVertexArray(gl, thingShad, Vertex -> "position", Normal -> "normal")
		thing = thingMesh.newVertexArray(gl, thingShad, Vertex -> "position", Normal -> "normal")
	}
	
	def initThing() {
		val model = new ColladaFile("Suzanne2.dae")
		model.library.geometry("Monkey").get.mesh.mergeVertices(true)
		thingMesh = model.library.geometry("Monkey").get.mesh.toMesh 
	}
	
	def display(surface:Surface) {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)		

		camera.viewLookAt

		// Plane

		thingShad.use
		thingShad.uniform("color", planeColor)
		hemiLight.uniform(thingShad, camera)
		camera.uniformMVP(thingShad)
		plane.draw(planeMesh.drawAs)

		// Thing
		
		gl.enable(gl.DEPTH_TEST)
		gl.frontFace(gl.CCW)
		thingShad.uniform("color", thingColor)
		hemiLight.uniform(thingShad, camera)
		camera.pushpop {
			//camera.scaleModel(0.01, 0.01, 0.01)
			camera.translateModel(0, 1, 0)
			camera.uniformMVP(thingShad)
			thing.draw(thingMesh.drawAs)
		}
		gl.frontFace(gl.CW)
		
		surface.swapBuffers
		gl.checkErrors
	}
	
	def reshape(surface:Surface) {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		camera.frustum(-camera.viewportRatio, camera.viewportRatio, -1, 1, 2)
	}	
}