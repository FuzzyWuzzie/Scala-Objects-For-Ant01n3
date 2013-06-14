package org.sofa.opengl.test

import org.sofa.math.{Matrix4, Rgba, Vector4, Vector3}
import org.sofa.opengl.{SGL, VertexArray, ShaderProgram, MatrixStack, Camera, Shader}
import org.sofa.opengl.mesh.{PlaneMesh, EditableMesh, VertexAttribute}
import org.sofa.opengl.surface.{Surface, BasicCameraController, SurfaceRenderer}

import javax.media.opengl.{GLAutoDrawable, GLProfile, GLCapabilities, GLEventListener}
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.util.FPSAnimator
import com.jogamp.newt.event.WindowAdapter

object TestEditableMesh {
	def main(args:Array[String]):Unit = { (new TestEditableMesh).test }
}

class TestEditableMesh extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	var planeShad:ShaderProgram = null
	var thingShad:ShaderProgram = null
	
	var thing:VertexArray = null
	var plane:VertexArray = null
	
	val thingMesh = new EditableMesh()
	val planeMesh = new PlaneMesh(2, 2, 4, 4)

	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.Grey20
	val light1 = Vector4(1, 1, 0.7, 1)
	
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
        ctrl           = new MyCameraController(camera, light1)
		initSurface    = initializeSurface
		frame          = display
		surfaceChanged = reshape
		key            = ctrl.key
		motion         = ctrl.motion
		scroll         = ctrl.scroll
		close          = { surface => sys.exit }
		surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
							camera, "Test EditableMesh", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.path += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
		
		initGL(sgl)
		initShaders
		initGeometry
		
		camera.eyeCartesian(5, 2, 5)
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
	}
	
	def initShaders() {
		planeShad = ShaderProgram(gl, "plane shader",
				"es2/phonghi.vert.glsl", "es2/phonghi.frag.glsl")
		thingShad = ShaderProgram(gl, "thing shader",
				"es2/phonghi.vert.glsl", "es2/phonghi.frag.glsl")
	}
	
	def initGeometry() {
		import VertexAttribute._

		initThing
		
		plane = planeMesh.newVertexArray(gl, planeShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		thing = thingMesh.newVertexArray(gl, planeShad, Vertex -> "position", Color -> "color", Normal -> "normal")
	}
	
	def initThing() {
		thingMesh.begin
			thingMesh.color(1, 0, 0)
			thingMesh.normal(0, 0, 1)
			thingMesh.vertex(-1, 0, 0)	// 0
			thingMesh.vertex(0, 1, 0)	// 1
			thingMesh.vertex(1, 0, 0)	// 2
			thingMesh.color(0, 1, 0)
			thingMesh.vertex(-1, 2, 0)	// 3
			thingMesh.vertex(1, 2, 0)	// 4
		thingMesh.end
		thingMesh.beginIndices
			thingMesh.index(0)
			thingMesh.index(1)
			thingMesh.index(2)
			
			thingMesh.index(3)
			thingMesh.index(4)
			thingMesh.index(1)

			thingMesh.index(1)
			thingMesh.index(4)
			thingMesh.index(2)

			thingMesh.index(1)
			thingMesh.index(0)
			thingMesh.index(3)
		thingMesh.endIndices
	}
	
	def display(surface:Surface) {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		
		planeShad.use
		camera.lookAt
		useLights(planeShad)
		camera.uniform(planeShad)
		plane.draw(planeMesh.drawAs)
		
		// TODO Thing
		thingShad.use
		useLights(thingShad)
		camera.uniform(thingShad)
		thing.draw(thingMesh.drawAs)
		
		surface.swapBuffers
		gl.checkErrors
	}
	
	def reshape(surface:Surface) {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		camera.frustum(-camera.viewportRatio, camera.viewportRatio, -1, 1, 2)
	}
	
	protected def useLights(shader:ShaderProgram) {
		shader.uniform("light.pos", Vector3(camera.modelview.top * light1))
		shader.uniform("light.intensity", 0.8f)
		shader.uniform("light.ambient", 0.1f)
		shader.uniform("light.specular", 10f)
	}
}