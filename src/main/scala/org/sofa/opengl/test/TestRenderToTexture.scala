package org.sofa.opengl.test

import org.sofa.Timer
import org.sofa.opengl.surface.{SurfaceRenderer, Surface, BasicCameraController}
import org.sofa.opengl.{SGL, ShaderProgram, MatrixStack, VertexArray, Camera, Shader, TextureFramebuffer}
import org.sofa.opengl.mesh.{Plane, Cube, WireCube, Axis, DynPointsMesh}
import javax.media.opengl.{GLCapabilities, GLProfile}
import scala.collection.mutable.{ArrayBuffer, HashSet, Set}
import org.sofa.math.{SpatialPoint, SpatialCube, SpatialHash, SpatialObject, Point3, Vector3, Vector4, Rgba, Matrix4}

object TestRenderToTexture {
	def main(args:Array[String]) = (new TestRenderToTexture).test
}

class TestRenderToTexture extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	var phongShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	var phongTexShad:ShaderProgram = null
	
	var plane:VertexArray = null
	var axis:VertexArray = null
	var cube:VertexArray = null
	
	var axisMesh = new Axis(10)
	var planeMesh = new Plane(2, 2, 4, 4)
	var cubeMesh = new Cube(0.5f)
	
	var camera:Camera = null
	var camera2:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.grey20
	val planeColor = Rgba.grey80
	val light1 = Vector4(1, 2, 1, 1)

	var fb:TextureFramebuffer = null
		
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
		
		camera         = new Camera()
		camera2        = new Camera()
		ctrl           = new MyCameraController(camera, light1)
		initSurface    = initializeSurface
		frame          = display
		surfaceChanged = reshape
		key            = ctrl.key
		motion         = ctrl.motion
		scroll         = ctrl.scroll
		close          = { surface => sys.exit }
		surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
							camera, "Test SPH", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)	
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.includePath += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
			
		initGL(sgl)
		initShaders
		initTextureFB
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
		gl.enable(gl.CULL_FACE)
		gl.cullFace(gl.BACK)
		gl.frontFace(gl.CW)
		gl.disable(gl.BLEND)
		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
		gl.enable(gl.PROGRAM_POINT_SIZE)	// Necessary on my ES2 implementation ?? 
	}
	
	def initShaders() {
		phongShad = ShaderProgram(gl, "phong shader", "es2/phonghi.vert.glsl", "es2/phonghi.frag.glsl")
		plainShad = ShaderProgram(gl, "plain shader", "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
		phongTexShad = ShaderProgram(gl, "textured phong shader", "es2/phonghitex.vert.glsl", "es2/phonghitex.frag.glsl")
	}

	def initTextureFB() {
		fb = new TextureFramebuffer(gl, 128, 128)

		camera2.viewCartesian(2, 2, 2)
		camera2.setFocus(0, 0, 0)
		camera2.viewportPx(fb.width, fb.height)
		camera2.frustum(-camera.viewportRatio, camera.viewportRatio, -1, 1, 2)
	}
	
	def initGeometry() {
		var v = phongShad.getAttribLocation("position")
		var c = phongShad.getAttribLocation("color")
		var n = phongShad.getAttribLocation("normal")

		cubeMesh.setColor(Rgba.yellow)
		cube = cubeMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
		
		v = plainShad.getAttribLocation("position")
		c = plainShad.getAttribLocation("color")
		
		//cube  = cubeMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
		axis = axisMesh.newVertexArray(gl, ("vertices", v), ("colors", c))		

		v = phongTexShad.getAttribLocation("position")
		n = phongTexShad.getAttribLocation("normal")
		c = phongTexShad.getAttribLocation("texCoords")

		planeMesh.setTextureRepeat(1,1)
		plane = planeMesh.newVertexArray(gl, ("vertices", v), ("normals", n), ("texcoords", c))
	}	

	def display(surface:Surface) {
		fb.display {
			gl.frontFace(gl.CW)
			gl.disable(gl.BLEND)
			gl.disable(gl.DEPTH_TEST)
			gl.clearColor(Rgba.blue)
			gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
			camera2.rotateViewHorizontal(0.1)
			camera2.setupView
			
			//plainShad.use
			//camera2.setUniformMVP(plainShad)
			//cube.draw(cubeMesh.drawAs)
			
			phongShad.use
			useLights2(phongShad)
			camera2.uniformMVP(phongShad)
			cube.draw(cubeMesh.drawAs)

			gl.checkErrors
			gl.enable(gl.DEPTH_TEST)
			gl.enable(gl.BLEND)
		}

		gl.clearColor(0,0,0,1)
		gl.viewport(0, 0, surface.width, surface.height)
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		gl.frontFace(gl.CW)
		
		camera.setupView

		// Plane
		
		phongTexShad.use
		fb.bindColorTextureTo(gl.TEXTURE0)
	    phongTexShad.uniform("texColor", 0)	// Texture Unit 0
		useLights(phongTexShad)
		camera.uniformMVP(phongTexShad)
		plane.draw(planeMesh.drawAs)
		gl.bindTexture(gl.TEXTURE_2D, 0)
		
		// Axis
		
		gl.enable(gl.BLEND)
		plainShad.use
		camera.setUniformMVP(plainShad)
		axis.draw(axisMesh.drawAs)
		
			// phongShad.use
			// useLights(phongShad)
			//camera.setUniformMVP(plainShad)
			//cube.draw(cubeMesh.drawAs)
		
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
		shader.uniform("light.intensity", 4f)
		shader.uniform("light.ambient", 0.1f)
		shader.uniform("light.specular", 100f)
	}
	protected def useLights2(shader:ShaderProgram) {
		shader.uniform("light.pos", Vector3(camera2.modelview.top * light1))
		shader.uniform("light.intensity", 4f)
		shader.uniform("light.ambient", 0.1f)
		shader.uniform("light.specular", 100f)
	}
}