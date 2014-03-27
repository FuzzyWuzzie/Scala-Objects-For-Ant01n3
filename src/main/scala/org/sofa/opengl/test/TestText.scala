package org.sofa.opengl.test

import org.sofa.Timer
import org.sofa.opengl.surface.{SurfaceRenderer, Surface, BasicCameraController}
import org.sofa.opengl.{SGL, ShaderProgram, MatrixStack, VertexArray, Camera, Shader, TextureFramebuffer}
import org.sofa.opengl.mesh.{PlaneMesh, CubeMesh, WireCubeMesh, AxisMesh, PointsMesh, VertexAttribute}
import org.sofa.opengl.text.{GLFont, GLString}
import javax.media.opengl.{GLCapabilities, GLProfile}
import scala.collection.mutable.{ArrayBuffer, HashSet, Set}
import org.sofa.math.{Point3, Vector3, Vector4, Rgba, Matrix4}
import org.sofa.collection.{SpatialPoint, SpatialCube, SpatialHash, SpatialObject}

object TestText {
	def main(args:Array[String]) = (new TestText).test
}

class TestText extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	var textShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	
	var plane:VertexArray = null
	var axis:VertexArray = null
	
	var axisMesh = new AxisMesh(10)
	var planeMesh = new PlaneMesh(2, 2, 10, 10)
	
	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.White
	val planeColor = Rgba.Grey80
	val light1 = Vector4(1, 2, 1, 1)

	var font:GLFont = null
	var text:GLString = null
		
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
		ctrl           = new MyCameraController(camera, light1)
		initSurface    = initializeSurface
		frame          = display
		surfaceChanged = reshape
		key            = ctrl.key
		motion         = ctrl.motion
		scroll         = ctrl.scroll
		close          = { surface => sys.exit }
		surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
							camera, "Test Text", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)	
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.path += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
			
		initGL(sgl)
		initShaders
		initGLText
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
		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
		gl.enable(gl.PROGRAM_POINT_SIZE)	// Necessary on my ES2 implementation ?? 
	}
	
	def initShaders() {
		textShad = ShaderProgram(gl, "text shader", "es2/text.vert.glsl", "es2/text.frag.glsl")
		plainShad = ShaderProgram(gl, "plain shader", "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
	}

	def initGLText() {
		GLFont.path += "/Users/antoine/Library/Fonts"

		font = new GLFont(gl, "Ubuntu-R.ttf", 80, textShad, true)
//		font = new GLFont(gl, "DroidSerif-Italic.ttf", 80, true)
//		font = new GLFont(gl, "SourceSansPro-Black.ttf", 80, true)
		text = new GLString(gl, font, 256)

		text.setColor(Rgba.Red)
		text.build("This is GL text !")
	}
	
	def initGeometry() {
		import VertexAttribute._

		planeMesh.setTextureRepeat(1,1)

		plane = planeMesh.newVertexArray(gl, textShad, Vertex -> "position", TexCoord -> "texCoords")		
		axis  = axisMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
	}	

	val textData = Array("This is GL text !", "An OpenGL text Library", "Easy to use !", "#@&%$*")
	var textIndex = 0
	var count = 0

	def display(surface:Surface) {
		gl.viewport(0, 0, surface.width, surface.height)
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		gl.frontFace(gl.CW)
		
		count += 1

		if(count > 50) {
			textIndex = (textIndex+1)%textData.size
			text.build(textData(textIndex))
			count = 0
		}

		camera.lookAt
		
		// Axis
		
		gl.enable(gl.BLEND)
		plainShad.use
		camera.uniformMVP(plainShad)
		axis.draw(axisMesh.drawAs)

		// Plane
		
		camera.pushpop {
			val scale = (font.texture.width / 100.0)

			gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)
			textShad.use
			font.texture.bindTo(gl.TEXTURE0)
			textShad.uniform("textColor", Rgba.Red)
	    	textShad.uniform("texColor", 0)	// Texture Unit 0
			camera.uniformMVP(textShad)
			plane.draw(planeMesh.drawAs)
			gl.bindTexture(gl.TEXTURE_2D, null)
		}

		// GLString

		camera.pushpop {
			gl.disable(gl.DEPTH_TEST)
			val scale = 5.0 / text.advance
			camera.scale(scale, scale, scale)
			text.draw(camera)
			gl.enable(gl.DEPTH_TEST)
		}

		gl.disable(gl.BLEND)
		
		surface.swapBuffers
		gl.checkErrors
	}
	
	def reshape(surface:Surface) {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		camera.frustum(-camera.viewportRatio, camera.viewportRatio, -1, 1, 2)
	}
}