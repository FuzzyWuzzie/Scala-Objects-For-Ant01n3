package org.sofa.opengl.test

import org.sofa.Timer
import org.sofa.opengl.surface.{SurfaceRenderer, Surface, BasicCameraController}
import org.sofa.opengl.{SGL, ShaderProgram, MatrixStack, VertexArray, Camera, Shader, TextureFramebuffer}
import org.sofa.opengl.mesh.{Plane, Cube, WireCube, Axis, DynPointsMesh}
import org.sofa.opengl.text.{GLFont, GLString}
import javax.media.opengl.{GLCapabilities, GLProfile}
import scala.collection.mutable.{ArrayBuffer, HashSet, Set}
import org.sofa.math.{SpatialPoint, SpatialCube, SpatialHash, SpatialObject, Point3, Vector3, Vector4, Rgba, Matrix4}

object TestText2 {
	def main(args:Array[String]) = (new TestText2).test

	val lorem = Array[String](
		"Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
		"Sed non risus. Suspendisse lectus tortor, dignissim sit",
		"amet, adipiscing, nec, ultricies sed, dolor. Cras",
		"elementum ultrices diam. Maecenas ligula massa, varius a,",
		"semper congue, euismod non, mi. Proin porttitor, orci nec",
		"nonummy molestie, enim est eleifend mi, non fermentum",
		"diam nisl sit amet erat. Duis semper. Duis arcu massa,",
		"scelerisque vitae, consequat in, pretium a, enim.",
		"Pellentesque congue. Ut in risus volutpat libero pharetra",
		"tempor. Cras vestibulum bibendum augue. Praesent egestas",
		"leo in pede. Praesent blandit odio eu enim. Pellentesque",
		"sed dui ut augue blandit sodales. Vestibulum ante ipsum",
		"primis in faucibus orci luctus et ultrices posuere cubilia",
		"Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque",
		"fermentum. Maecenas adipiscing ante non diam sodales",
		"hendrerit. Ut velit mauris, egestas sed, gravida nec, ornare",
		"ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla",
		"sollicitudin. Fusce varius, ligula non tempus aliquam, nunc",
		"turpis ullamcorper nibh, in tempus sapien eros vitae ligula.",
		"Pellentesque rhoncus nunc et augue. Integer id felis.",
		"Curabitur aliquet pellentesque diam. Integer quis metus",
		"vitae elit lobortis egestas. Lorem ipsum dolor sit amet,",
		"consectetuer adipiscing elit. Morbi vel erat non mauris",
		"convallis vehicula. Nulla et sapien. Integer tortor tellus,",
		"aliquam faucibus, convallis id, congue eu, quam. Mauris",
		"ullamcorper felis vitae erat. Proin feugiat, augue non",
		"elementum posuere, metus purus iaculis lectus, et tristique",
		"ligula justo vitae magna. Aliquam convallis sollicitudin",
		"purus. Praesent aliquam, enim at fermentum mollis, ligula",
		"massa adipiscing nisl, ac euismod nibh nisl eu lectus.",
		"Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod",
		"libero eu enim. Nulla nec felis sed leo placerat imperdiet.",
		"Aenean suscipit nulla in justo. Suspendisse cursus rutrum",
		"augue. Nulla tincidunt tincidunt mi. Curabitur iaculis,",
		"lorem vel rhoncus faucibus, felis magna fermentum augue,",
		"et ultricies lacus lorem varius purus. Curabitur eu amet."
	)
}

class TestText2 extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	var textShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	
	//var plane:VertexArray = null
	var axis:VertexArray = null
	
	var axisMesh = new Axis(10)
	
	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.white
	val light1 = Vector4(1, 2, 1, 1)

	var font:GLFont = null
	var text:Array[GLString] = new Array[GLString](TestText2.lorem.length)
		
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
		Shader.includePath += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
			
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
		gl.enable(gl.PROGRAM_POINT_SIZE)	// Necessary on my ES2 implementation ?? 
	}
	
	def initShaders() {
		textShad = ShaderProgram(gl, "phong shader", "es2/text.vert.glsl", "es2/text.frag.glsl")
		plainShad = ShaderProgram(gl, "plain shader", "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
	}

	def initGLText() {
		println("unpack alignment = %d".format(gl.getInteger(gl.UNPACK_ALIGNMENT)))

		GLFont.path += "/Users/antoine/Library/Fonts"

//		font = new GLFont(gl, "DroidSerif-Italic.ttf", 12, 0, 0)
		font = new GLFont(gl, "SourceSansPro-Black.ttf", 20, 0, 0)
//		font.minMagFilter(gl.LINEAR, gl.LINEAR)

		for(i <- 0 until text.length) {
			text(i) = new GLString(gl, font, 256)
			text(i).setColor(Rgba.grey20)
			text(i).build(TestText2.lorem(i))
		}
	}
	
	def initGeometry() {
		val v = plainShad.getAttribLocation("position")
		val c = plainShad.getAttribLocation("color")
		
		axis = axisMesh.newVertexArray(gl, ("vertices", v), ("colors", c))		
	}	

	def display(surface:Surface) {
		gl.viewport(0, 0, surface.width, surface.height)
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		gl.frontFace(gl.CW)
		
		camera.viewIdentity
		
		// Axis
		
		gl.enable(gl.BLEND)
		plainShad.use
		camera.setUniformMVP(plainShad)
		axis.draw(axisMesh.drawAs)

		// GLString

		for(i <- 0 until text.length) {
			gl.disable(gl.DEPTH_TEST)
			camera.pushpop {
				//val scale = 5.0 / text(0).advance
				//camera.scaleModel(scale, scale, scale)
				camera.translateModel(0, (text.length-i)*font.cellHeight, 0)
				text(i).draw(camera)
			}
			gl.enable(gl.DEPTH_TEST)
		}

		gl.disable(gl.BLEND)
		
		surface.swapBuffers
		gl.checkErrors
	}
	
	def reshape(surface:Surface) {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		camera.orthographic(0, surface.width, 0, surface.height, -1, 1)
	}	
}