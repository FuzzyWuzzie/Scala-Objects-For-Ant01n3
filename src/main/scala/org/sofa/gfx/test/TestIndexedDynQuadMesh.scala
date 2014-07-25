package org.sofa.gfx.test

import org.sofa.gfx.VertexArray
import org.sofa.gfx.SGL
import org.sofa.gfx.mesh.PlaneMesh
import org.sofa.gfx.mesh.EditableMesh
import org.sofa.gfx.ShaderProgram
import org.sofa.math.Matrix4
import org.sofa.gfx.MatrixStack
import javax.media.opengl.GLAutoDrawable
import javax.media.opengl.GLProfile
import javax.media.opengl.GLCapabilities
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.util.FPSAnimator
import com.jogamp.newt.event.WindowAdapter
import javax.media.opengl.GLEventListener
import org.sofa.gfx.surface.Surface
import org.sofa.gfx.surface.BasicCameraController
import org.sofa.gfx.Camera
import org.sofa.math.Rgba
import org.sofa.gfx.surface.SurfaceRenderer
import org.sofa.math.Vector4
import org.sofa.gfx.Shader
import org.sofa.math.Vector3
import org.sofa.gfx.mesh.QuadsMesh
import org.sofa.gfx.mesh.VertexAttribute

object TestIndexedDynQuadMesh {
	def main(args:Array[String]):Unit = { (new TestIndexedDynQuadMesh).test }
}

class TestIndexedDynQuadMesh extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	var planeShad:ShaderProgram = null
	var thingShad:ShaderProgram = null
	
	var thing:VertexArray = null
	var plane:VertexArray = null
	
	val maxQuads = 9
	val thingMesh = new QuadsMesh(maxQuads)
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
		actionKey      = ctrl.actionKey
		motion         = ctrl.motion
		gesture        = ctrl.gesture
		close          = { surface => sys.exit }
		surface        = new org.sofa.gfx.backend.SurfaceNewt(this,
							camera, "Test EditableMesh", caps,
							org.sofa.gfx.backend.SurfaceNewtGLBackend.GL2ES2)
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.path += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/gfx/shaders/"
			
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
		
		thingMesh.setPoint( 0, -2, 0, 0)
		thingMesh.setPoint( 1, -1, 0, 0)
		thingMesh.setPoint( 2,  0, 0, 0)
		thingMesh.setPoint( 3,  1, 0, 0)
		thingMesh.setPoint( 4,  2, 0, 0)
		thingMesh.setPoint( 5, -2, 1, 0)
		thingMesh.setPoint( 6, -1, 1, 0)
		thingMesh.setPoint( 7,  0, 1, 0)
		thingMesh.setPoint( 8,  1, 1, 0)
		thingMesh.setPoint( 9,  2, 1, 0)
		thingMesh.setPoint(10, -2, 2, 0)
		thingMesh.setPoint(11, -1, 2, 0)
		thingMesh.setPoint(12,  0, 2, 0)
		thingMesh.setPoint(13,  1, 2, 0)
		thingMesh.setPoint(14,  2, 2, 0)
		
		thingMesh.setPointColor( 0, Rgba.Red)
		thingMesh.setPointColor( 5, Rgba.Red)
		thingMesh.setPointColor(10, Rgba.Red)
		thingMesh.setPointColor( 1, Rgba.Green)
		thingMesh.setPointColor( 6, Rgba.Green)
		thingMesh.setPointColor(11, Rgba.Green)
		thingMesh.setPointColor( 2, Rgba.Blue)
		thingMesh.setPointColor( 7, Rgba.Blue)
		thingMesh.setPointColor(12, Rgba.Blue)
		thingMesh.setPointColor( 3, Rgba.Yellow)
		thingMesh.setPointColor( 8, Rgba.Yellow)
		thingMesh.setPointColor(13, Rgba.Yellow)
		thingMesh.setPointColor( 4, Rgba.Cyan)
		thingMesh.setPointColor( 9, Rgba.Cyan)
		thingMesh.setPointColor(14, Rgba.Cyan)

		thingMesh.setPointNormal( 0, 0, 0, 1)
		thingMesh.setPointNormal( 1, 0, 0, 1)
		thingMesh.setPointNormal( 2, 0, 0, 1)
		thingMesh.setPointNormal( 3, 0, 0, 1)
		thingMesh.setPointNormal( 4, 0, 0, 1)
		thingMesh.setPointNormal( 5, 0, 0, 1)
		thingMesh.setPointNormal( 6, 0, 0, 1)
		thingMesh.setPointNormal( 7, 0, 0, 1)
		thingMesh.setPointNormal( 8, 0, 0, 1)
		thingMesh.setPointNormal( 9, 0, 0, 1)
		thingMesh.setPointNormal(10, 0, 0, 1)
		thingMesh.setPointNormal(11, 0, 0, 1)
		thingMesh.setPointNormal(12, 0, 0, 1)
		thingMesh.setPointNormal(13, 0, 0, 1)
		thingMesh.setPointNormal(14, 0, 0, 1)
		
		thingMesh.setQuad(0, 0, 1, 6, 5)
		thingMesh.setQuad(1, 1, 2, 7, 6)
		thingMesh.setQuad(2, 2, 3, 8, 7)
		thingMesh.setQuad(3, 3, 4, 9, 8)
		thingMesh.setQuad(4, 5, 6, 11, 10)
		thingMesh.setQuad(5, 6, 7, 12, 11)
		thingMesh.setQuad(6, 7, 8, 13, 12)
		thingMesh.setQuad(7, 8, 9, 14, 13)
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
		thing.draw(thingMesh.drawAs(gl), maxQuads*4)
		
		//surface.swapBuffers
		gl.checkErrors
		
		updateQuads
	}
	
	var foo = 0.0
	var fooInc = 0.01

	def updateQuads() {
//		Array[Int](1,6,11,3,8,13) foreach { i =>
		Array[Int](1,6,11) foreach { i =>
//		for(i <- 5 until 15) {
			val p = thingMesh.getPoint(i)
			foo += fooInc

			if(foo > 1) { foo = 1; fooInc = -fooInc }
			else if(foo < -1) { foo = -1; fooInc = -fooInc }

			p.z = foo
			
			thingMesh.setPoint(i, p)
		}

		// thingMesh.setPointNormal(0, 0, 0, 1)
		// thingMesh.setPointNormal(1, 0, 0, 1)
		// thingMesh.setPointNormal(2, 0, 0, 1)
		// thingMesh.setPointNormal(3, 0, 0, 1)
		// thingMesh.setPointNormal(4, 0, 0, 1)
		// thingMesh.setPointNormal(5, 0, 0, 1)
		// thingMesh.setPointNormal(6, 0, 0, 1)
		// thingMesh.setPointNormal(7, 0, 0, 1)
		// thingMesh.setPointNormal(8, 0, 0, 1)
		// thingMesh.setPointNormal(9, 0, 0, 1)
		
		thingMesh.updateVertexArray(gl, true, false, true, false)
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