package org.sofa.opengl.test

import org.sofa.Timer
import org.sofa.opengl.surface.{SurfaceRenderer, Surface, BasicCameraController}
import org.sofa.opengl.{SGL, ShaderProgram, MatrixStack, VertexArray, Camera, Shader, TextureFramebuffer}
import org.sofa.opengl.mesh.{PlaneMesh, CubeMesh, WireCubeMesh, AxisMesh, LinesMesh, VertexAttribute}
import org.sofa.opengl.text.{GLFont, GLString}
import javax.media.opengl.{GLCapabilities, GLProfile}
import scala.collection.mutable.{ArrayBuffer, HashSet, Set}
import org.sofa.math.{SpatialPoint, SpatialCube, SpatialHash, SpatialObject, Point3, Vector3, Vector4, Rgba, Matrix4}

object TestText2 {
	def main(args:Array[String]) = {
		val desktopHints = java.awt.Toolkit.getDefaultToolkit.getDesktopProperty("awt.font.desktophints")

		Console.err.println(desktopHints)

		(new TestText2).test
	}

	val cyrano = Array[String](
		"RAGUENEAU, devant la cheminée",
		"Ma Muse, éloigne-toi, pour que tes yeux charmants",
		"N'aillent pas se rougir au feu de ces sarments !",
		"À un pâtissier, lui montrant des pains.",
		"Vous avez mal placé la fente de ces miches",
		"Au milieu la césure, -entre les hémistiches !",
		"À un autre, lui montrant un pâté inachevé.",
		"À ce palais de croûte, il faut, vous, mettre un toit...",
		"À un jeune apprenti, qui, assis par terre, embroche des volailles.",
		"Et toi, sur cette broche interminable, toi,",
		"Le modeste poulet et la dinde superbe,",
		"Alterne-les, mon fils, comme le vieux Malherbe",
		"Alternait les grands vers avec les plus petits,",
		"Et fais tourner au feu des strophes de rôtis !"
		)
}

class TestText2 extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	val linesMesh = new LinesMesh(3000)
	var lines:VertexArray = null

	var plainShad:ShaderProgram = null
	var textShad:ShaderProgram = null
	
	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.White

	var fonts = new Array[GLFont](3)
	var text:Array[GLString] = new Array[GLString](TestText2.cyrano.length)
		
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
		
		camera         = new Camera(); camera.viewportPx.set(1280, 800)
		ctrl           = new MyCameraController(camera, null)
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
	
	def initShaders() {
		plainShad = ShaderProgram(gl, "plain shader", "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
		textShad  = ShaderProgram(gl, "phong shader", "es2/text.vert.glsl", "es2/text.frag.glsl")
	}

	def initGLText() {
//		println("unpack alignment = %d".format(gl.getInteger(gl.UNPACK_ALIGNMENT)))

		GLFont.path += "/Users/antoine/Library/Fonts"
		GLFont.path += "Fonts"

//		font = new GLFont(gl, "NoticiaText-Italic.ttf", 40)
//		font = new GLFont(gl, "Ubuntu-RI.ttf", 40)
		fonts(0) = new GLFont(gl, "Ubuntu-R.ttf", 45)
//		fonts(1) = new GLFont(gl, "Ubuntu-RI.ttf", 45)
		fonts(2) = new GLFont(gl, "Ubuntu-L.ttf", 40)

		fonts(0).minMagFilter(gl.LINEAR, gl.LINEAR)
//		fonts(1).minMagFilter(gl.LINEAR, gl.LINEAR)
		fonts(2).minMagFilter(gl.LINEAR, gl.LINEAR)

		text(0) = new GLString(gl, fonts(0), TestText2.cyrano(0).length, textShad)
		text(0).setColor(Rgba.Red)
		text(0).build(TestText2.cyrano(0))

		for(i <- 1 until text.length) {
			text(i) = new GLString(gl, fonts(2), TestText2.cyrano(i).length, textShad)
			text(i).setColor(Rgba.Black)
			text(i).build(TestText2.cyrano(i))
		}
	}
	
	def initGeometry() {
		import VertexAttribute._
		linesMesh.horizontalRuler(0, 0, 10, 100, 10, 2, Rgba.Black, Rgba(0.6, 0, 0, 1), Rgba(0.3, 0, 0, 1))
		lines = linesMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
	}	

	def display(surface:Surface) {
		gl.viewport(0, 0, surface.width, surface.height)
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		gl.frontFace(gl.CW)
		
		camera.viewIdentity

		gl.enable(gl.BLEND)
//		gl.disable(gl.LINE_SMOOTH)
		gl.lineWidth(1)
		
		// Ruler

		plainShad.use
		camera.setUniformMVP(plainShad)
		lines.draw(linesMesh.drawAs)		

		// GLString

		for(i <- 0 until text.length) {
			gl.disable(gl.DEPTH_TEST)
			camera.pushpop {
				//val scale = 5.0 / text(0).advance
				//camera.scaleModel(scale, scale, scale)
				camera.translateModel(10, (text.length-i)*fonts(2).height, 0)
				text(i).draw(camera)
			}
			gl.enable(gl.DEPTH_TEST)
		}

		gl.disable(gl.BLEND)
		
		surface.swapBuffers
		gl.checkErrors
	}
	
	def reshape(surface:Surface) {
Console.err.println("### Surface (%d x %d)".format(surface.width, surface.height))
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		camera.orthographic(0, surface.width, 0, surface.height, -1, 1)
	}	
}