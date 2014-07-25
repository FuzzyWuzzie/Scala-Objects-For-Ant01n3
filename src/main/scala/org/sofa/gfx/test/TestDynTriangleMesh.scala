package org.sofa.gfx.test

import javax.media.opengl.GLAutoDrawable
import javax.media.opengl.GLProfile
import javax.media.opengl.GLCapabilities
import javax.media.opengl.GLEventListener

import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.util.FPSAnimator
import com.jogamp.newt.event.WindowAdapter

import org.sofa.gfx.{SGL, VertexArray, ShaderProgram, MatrixStack, Shader, Camera}
import org.sofa.gfx.mesh.{PlaneMesh, EditableMesh}
import org.sofa.gfx.surface.{Surface, BasicCameraController, SurfaceRenderer}
import org.sofa.math.{Vector3, Vector4, Rgba, Matrix4}
import org.sofa.gfx.mesh.{VertexAttribute, UnindexedTrianglesMesh}


object TestDynTriangleMesh {
	def main(args:Array[String]):Unit = { (new TestDynTriangleMesh).test }
}

class TestDynTriangleMesh extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	var planeShad:ShaderProgram = null
	var thingShad:ShaderProgram = null
	
	var thing:VertexArray = null
	var plane:VertexArray = null
	
	val maxTriangles = 10
	val thingMesh = new UnindexedTrianglesMesh(maxTriangles)
	val planeMesh = new PlaneMesh(2, 2, 4, 4)

	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.Grey20
	val light1 = Vector4(0, 0.5, 0, 1)
	
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
		Shader.path += "src/main/scala/org/sofa/gfx/shaders/"
			
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
		
		plane = planeMesh.newVertexArray(gl, planeShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		
		initThing
		
		thing = thingMesh.newVertexArray(gl, thingShad, Vertex -> "position", Color -> "color", Normal -> "normal")
	}
	
	def initThing() {
		import math._
		var x = -3.0f
		var y = 0.5f
		var z = -1.0f
		val step = 1f
		for(i <- 0 until maxTriangles) {
			thingMesh.setTriangle(i,
					x, y, z, x+step, y, z, x, y+step, z)
					x += step
					
//					random.toFloat*4-2, random.toFloat*4-2, random.toFloat*4-2,
//					random.toFloat*2,   random.toFloat*2,   random.toFloat*2,
//					random.toFloat*2*0.01f-1, random.toFloat*2*0.01f-1, random.toFloat*2*0.01f-1)
			thingMesh.autoComputeNormal(i)
			thingMesh.setColor(i, Rgba(random, random, random))
		}
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
		
		//surface.swapBuffers
		gl.checkErrors
		
		updateTriangles
	}
	
	def updateTriangles() {
		for(i <- 0 until maxTriangles) {
			val (p0, p1, p2) = thingMesh.getTriangle(i)
			
			p0.brownianMotion(0.1)
			p1.brownianMotion(0.1)
			p2.brownianMotion(0.1)
			
			thingMesh.setTriangle(i, p0, p1, p2)
			thingMesh.autoComputeNormal(i)
		}
		
		thingMesh.updateVertexArray(gl, true, true, true)
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