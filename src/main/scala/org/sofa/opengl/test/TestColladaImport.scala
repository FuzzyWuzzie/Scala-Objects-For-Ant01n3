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
	
	var planeShad:ShaderProgram = null
	var thingShad:ShaderProgram = null
	
	var thing:VertexArray = null
	var plane:VertexArray = null
	
	var thingMesh:Mesh = null
	val planeMesh = new Plane(2, 2, 4, 4)

	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.grey20
	val thingColor = new Rgba(1, 0.8, 0, 1)
	val light1 = Vector4(1.7, 2, 1.7, 1)
	
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
							camera, "Test Collada import", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.includePath  += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
			
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
		
		gl.enable(gl.CULL_FACE)
		gl.cullFace(gl.BACK)
		gl.frontFace(gl.CW)
		
		gl.disable(gl.BLEND)
	}
	
	def initShaders() {
		planeShad = ShaderProgram(gl, "plane shader",
				"es2/phonghi.vert.glsl", "es2/phonghi.frag.glsl")
		thingShad = ShaderProgram(gl, "thing shader",
				"es2/phonghiuniformcolor.vert.glsl", "es2/phonghiuniformcolor.frag.glsl")
	}
	
	def initGeometry() {
		initThing

		var v = planeShad.getAttribLocation("position")
		var c = planeShad.getAttribLocation("color")
		var n = planeShad.getAttribLocation("normal")

		plane = planeMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
		
		v = thingShad.getAttribLocation("position")
		n = thingShad.getAttribLocation("normal")
		
		thing = thingMesh.newVertexArray(gl, ("vertices", v), ("normals", n))
	}
	
	def initThing() {
//		val model = new File(scala.xml.XML.loadFile("/Users/antoine/Desktop/duck_triangulate.dae").child)
		val model = new ColladaFile(scala.xml.XML.loadFile("/Users/antoine/Documents/Art/Sculptures/Blender/Suzanne2.dae").child)
		thingMesh = model.library.geometry("Monkey").mesh.toMesh 
	}
	
	def display(surface:Surface) {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		
		gl.frontFace(gl.CW)
		planeShad.use
		camera.viewLookAt
		useLights(planeShad)
		camera.uniformMVP(planeShad)
		plane.draw(planeMesh.drawAs)
		
		// TODO Thing
		thingShad.use
		useLights(thingShad)
		thingShad.uniform("color", thingColor)
		camera.pushpop {
			gl.frontFace(gl.CCW)
			//camera.scaleModel(0.01, 0.01, 0.01)
			camera.translateModel(0, 1, 0)
			camera.uniformMVP(thingShad)
			thing.draw(thingMesh.drawAs)
		}
		
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
		shader.uniform("light.intensity", 2f)
		shader.uniform("light.ambient", 0.1f)
		shader.uniform("light.specular", 100f)
	}
}