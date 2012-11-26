package org.sofa.opengl.test

import org.sofa.opengl.VertexArray
import org.sofa.opengl.SGL
import org.sofa.opengl.mesh.Plane
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
import org.sofa.opengl.mesh.MeshDrawMode
import org.sofa.opengl.mesh.DynTriangleMesh

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
	val thingMesh = new DynTriangleMesh(maxTriangles)
	val planeMesh = new Plane(2, 2, 4, 4)

	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.grey20
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
		key            = ctrl.key
		motion         = ctrl.motion
		scroll         = ctrl.scroll
		close          = { surface => sys.exit }
		surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
							camera, "Test EditableMesh", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.includePath += "src-scala/org/sofa/opengl/shaders"
			
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
		var v = planeShad.getAttribLocation("position")
		var c = planeShad.getAttribLocation("color")
		var n = planeShad.getAttribLocation("normal")
		
		plane = planeMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
		
		initThing

		v = thingShad.getAttribLocation("position")
		c = thingShad.getAttribLocation("color")
		n = thingShad.getAttribLocation("normal")
		
		thing = thingMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
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
		camera.viewLookAt
		useLights(planeShad)
		camera.uniformMVP(planeShad)
		plane.draw(planeMesh.drawAs)
		
		// TODO Thing
		thingShad.use
//		thingShad.uniform("color", Rgba.red)
		useLights(thingShad)
		camera.uniformMVP(thingShad)
		thing.draw(thingMesh.drawAs, maxTriangles*3)
		
		surface.swapBuffers
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
		
		thingMesh.updateVertexArray(gl, "vertices", "colors", "normals")
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