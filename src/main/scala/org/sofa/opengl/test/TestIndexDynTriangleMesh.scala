package org.sofa.opengl.test

import org.sofa.opengl.VertexArray
import org.sofa.opengl.SGL
import org.sofa.opengl.mesh.PlaneMesh
import org.sofa.opengl.mesh.EditableMesh
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
import org.sofa.opengl.mesh.TrianglesMesh
import org.sofa.opengl.mesh.VertexAttribute

object TestIndexedDynTriangleMesh {
	def main(args:Array[String]):Unit = { (new TestIndexedDynTriangleMesh).test }
}

class TestIndexedDynTriangleMesh extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	var planeShad:ShaderProgram = null
	var thingShad:ShaderProgram = null
	
	var thing:VertexArray = null
	var plane:VertexArray = null
	
	val maxTriangles = 9
	val thingMesh = new TrianglesMesh(maxTriangles)
	val planeMesh = new PlaneMesh(2, 2, 4, 4)

	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.Grey20
	val light1 = Vector4(0.5, 0.5, 0.5, 1)
	
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
		
		gl.disable(gl.CULL_FACE)
		//gl.cullFace(gl.BACK)
		//gl.frontFace(gl.CW)
		
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
		thing = thingMesh.newVertexArray(gl, thingShad, Vertex -> "position", Color -> "color", Normal -> "normal")
	}
	
	def initThing() {
		import math._
		
		thingMesh.setPoint(0, 0, 0, 0)
		thingMesh.setPoint(1, 1, 0, 0)
		thingMesh.setPoint(2, 0.5f, 1, 0)
		thingMesh.setPoint(3, -0.5f, 1, 0)
		thingMesh.setPoint(4, -1, 0, 0)
		thingMesh.setPoint(5, -0.5f, -1, 0)
		thingMesh.setPoint(6, 0.5f, -1, 0)
		thingMesh.setPoint(7, 1.5f, -1, 0)
		thingMesh.setPoint(8, 0, 2, 0)
		thingMesh.setPoint(9, -1.5f, -1, 0)
		
		thingMesh.setPointColor(0, Rgba.White)
		thingMesh.setPointColor(1, Rgba.Red)
		thingMesh.setPointColor(2, Rgba.Green)
		thingMesh.setPointColor(3, Rgba.Blue)
		thingMesh.setPointColor(4, Rgba.Cyan)
		thingMesh.setPointColor(5, Rgba.Magenta)
		thingMesh.setPointColor(6, Rgba.Yellow)
		thingMesh.setPointColor(7, Rgba.Grey20)
		thingMesh.setPointColor(8, Rgba.Grey50)
		thingMesh.setPointColor(9, Rgba.Grey80)
		
		thingMesh.setPointNormal(0, 0, 0, 1)
		thingMesh.setPointNormal(1, 0, 0, 1)
		thingMesh.setPointNormal(2, 0, 0, 1)
		thingMesh.setPointNormal(3, 0, 0, 1)
		thingMesh.setPointNormal(4, 0, 0, 1)
		thingMesh.setPointNormal(5, 0, 0, 1)
		thingMesh.setPointNormal(6, 0, 0, 1)
		thingMesh.setPointNormal(7, 0, 0, 1)
		thingMesh.setPointNormal(8, 0, 0, 1)
		thingMesh.setPointNormal(9, 0, 0, 1)
		
		thingMesh.setTriangle(0, 0, 1, 2)
		thingMesh.setTriangle(1, 0, 2, 3)
		thingMesh.setTriangle(2, 0, 3, 4)
		thingMesh.setTriangle(3, 0, 4, 5)
		thingMesh.setTriangle(4, 0, 5, 6)
		thingMesh.setTriangle(5, 0, 6, 1)
		thingMesh.setTriangle(6, 3, 2, 8)
		thingMesh.setTriangle(7, 4, 9, 5)
		thingMesh.setTriangle(8, 1, 6, 7)
	}
	
	def display(surface:Surface) {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		
		planeShad.use
		camera.lookAt
		useLights(planeShad)
		camera.uniform(planeShad)
		plane.draw(planeMesh.drawAs(gl))
		
		// TODO Thing
		thingShad.use
//		thingShad.uniform("color", Rgba.red)
		useLights(thingShad)
		camera.uniform(thingShad)
		thing.draw(thingMesh.drawAs(gl), maxTriangles*3)
		
		surface.swapBuffers
		gl.checkErrors
		
		updateTriangles
	}
	
	var foo = 0.0
	var fooInc = 0.01

	def updateTriangles() {
		for(i <- 2 until 10 by 2) {
			val p = thingMesh.getPoint(i)
			
			foo += fooInc

			if(foo > 1) { foo = 1; fooInc = -fooInc }
			else if(foo < -1) { foo = -1; fooInc = -fooInc }

			p.z = foo
			
			thingMesh.setPoint(i, p)
			//thingMesh.autoComputeNormal(i)
		}
		thingMesh.setPointNormal(0, 0, 0, 1)
		thingMesh.setPointNormal(1, 0, 0, 1)
		thingMesh.setPointNormal(2, 0, 0, 1)
		thingMesh.setPointNormal(3, 0, 0, 1)
		thingMesh.setPointNormal(4, 0, 0, 1)
		thingMesh.setPointNormal(5, 0, 0, 1)
		thingMesh.setPointNormal(6, 0, 0, 1)
		thingMesh.setPointNormal(7, 0, 0, 1)
		thingMesh.setPointNormal(8, 0, 0, 1)
		thingMesh.setPointNormal(9, 0, 0, 1)
		
		thingMesh.updateVertexArray(gl, true, true ,true, false)
	}
	
	def reshape(surface:Surface) {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		camera.frustum(-camera.viewportRatio, camera.viewportRatio, -1, 1, 2)
	}
	
	protected def useLights(shader:ShaderProgram) {
		shader.uniform("light.pos", Vector3(camera.modelview.top * light1))
		shader.uniform("light.intensity", 4f)
		shader.uniform("light.ambient", 0.1f)
		shader.uniform("light.specular", 10f)
	}
}