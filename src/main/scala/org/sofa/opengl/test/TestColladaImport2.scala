package org.sofa.opengl.test

import org.sofa.opengl.{SGL, VertexArray, ShaderProgram, MatrixStack, Camera, Shader, Texture}
import org.sofa.opengl.mesh.{Plane, EditableMesh, MeshDrawMode, Mesh}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController}
import org.sofa.math.{Matrix4, Rgba, Vector4, Vector3}
import org.sofa.opengl.io.collada.{File}

import javax.media.opengl.{GLAutoDrawable, GLProfile, GLCapabilities, GLEventListener}
import com.jogamp.newt.opengl.{GLWindow}
import com.jogamp.opengl.util.{FPSAnimator}
import com.jogamp.newt.event.{WindowAdapter}

object TestColladaImport2 {
	def main(args:Array[String]):Unit = { (new TestColladaImport2).test }
}

class TestColladaImport2 extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	var phongShad:ShaderProgram = null
	var nMapShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	
	var thing:VertexArray = null
	var plane:VertexArray = null
	var normals:VertexArray = null

	var thingMesh:Mesh = null
	val planeMesh = new Plane(2, 2, 4, 4)
	var normalsMesh:Mesh = null

	var colorTex:Texture = null
	var nMapTex:Texture = null

	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.grey20
	var lightZ = 3.0
	val light1 = Vector4(1.7, 2, lightZ, 1)
	
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
							camera, "Test Collada import & nomal mapping", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.includePath  += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
		Texture.includePath += "/Users/antoine/Documents/Programs/SOFA/textures"

		initGL(sgl)
		initTextures
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
		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
	}

	protected def initTextures() {
		colorTex = new Texture(gl, "Thing_Color.png", true)
	    colorTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    colorTex.wrap(gl.REPEAT)

		nMapTex = new Texture(gl, "Thing_NMap2.png", true)
	    nMapTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    nMapTex.wrap(gl.REPEAT)
	}

	def initShaders() {
		phongShad = ShaderProgram(gl, "phong shader",     "es2/phonghi.vert.glsl",       "es2/phonghi.frag.glsl")
//		nMapShad  = ShaderProgram(gl, "normal map phong", "es2/phonghinmapbi.vert.glsl", "es2/phonghinmapbi.frag.glsl")
		nMapShad  = ShaderProgram(gl, "normal map phong", "es2/phonghinmapw.vert.glsl", "es2/phonghinmapw.frag.glsl")
		plainShad = ShaderProgram(gl, "plain shader",     "es2/plainColor.vert.glsl",    "es2/plainColor.frag.glsl")
	}
	
	def initGeometry() {
		initThing
	
		var v = nMapShad.getAttribLocation("position")
	    var n = nMapShad.getAttribLocation("normal")
	    var t = nMapShad.getAttribLocation("tangent")
//	    var b = nMapShad.getAttribLocation("bitangent")
	    var u = nMapShad.getAttribLocation("texCoords")

//		thing = thingMesh.newVertexArray(gl, ("vertices", v), ("normals", n), ("tangents", t), ("bitangents", b), ("texCoords", u))
		thing = thingMesh.newVertexArray(gl, ("vertices", v), ("normals", n), ("tangents", t), ("texCoords", u))
	
		v     = phongShad.getAttribLocation("position")
		var c = phongShad.getAttribLocation("color")
		n     = phongShad.getAttribLocation("normal")

		plane = planeMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))

		v = plainShad.getAttribLocation("position")
		c = plainShad.getAttribLocation("color")

		normals = normalsMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
	}
	
	def initThing() {
		val model = new File(scala.xml.XML.loadFile("/Users/antoine/Documents/Art/Sculptures/Blender/Thing_001.dae").child)
		thingMesh = model.library.geometry("Thing").mesh.toMesh

		thingMesh.asInstanceOf[EditableMesh].autoComputeTangents(true)			// Also compute handedness and store 4-component tangents
//		thingMesh.asInstanceOf[EditableMesh].autoComputeTangents(false,true)	// Also compute bitangentss

		// Now show the normals and tangents

		normalsMesh = thingMesh.asInstanceOf[EditableMesh].newNormalsTangentsMesh(Rgba(1,0,0,0.1), Rgba(0,1,0,0.1))
	}
	
	var lightZdir = -0.1

	def display(surface:Surface) {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		gl.frontFace(gl.CW)
		
		lightZ += lightZdir

		if(lightZ > 3) {
			lightZdir = -lightZdir
			lightZ = 3
		} else if(lightZ < -3) {
			lightZdir = -lightZdir
			lightZ = -3
		}

		light1.z = lightZ

		camera.viewLookAt
		
		// Plane

		phongShad.use
		useLights(phongShad)
		camera.uniformMVP(phongShad)
		plane.draw(planeMesh.drawAs)
		
		// Thing

		nMapShad.use		
		colorTex.bindTo(gl.TEXTURE0)
	    nMapShad.uniform("texColor", 0)	// Texture Unit 0
	    nMapTex.bindTo(gl.TEXTURE2)
	    nMapShad.uniform("texNormal", 2)	// Texture Unit 2
			nMapShad.uniform("lightPos", Vector3(camera.modelview.top * light1))
		    nMapShad.uniform("lightIntensity", 2f)
		    nMapShad.uniform("ambientIntensity", 0.2f)
		    nMapShad.uniform("specularPow", 256f)
		camera.uniformMVP(nMapShad)
//		thing.draw(thingMesh.drawAs)
		camera.pushpop {
			//gl.disable(gl.CULL_FACE)
			gl.frontFace(gl.CCW)
			//camera.scaleModel(0.01, 0.01, 0.01)
			camera.translateModel(0, 1, 0)
			camera.uniformMVP(nMapShad)
			thing.draw(thingMesh.drawAs)
			gl.enable(gl.CULL_FACE)
		}
	
		// Normals & Tangents

		// gl.enable(gl.BLEND)
		// plainShad.use
		// camera.pushpop {
		// 	camera.translateModel(0, 1, 0)
		// 	camera.setUniformMVP(plainShad)
		// 	normals.draw(normalsMesh.drawAs)
		// }
		// gl.disable(gl.BLEND)

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