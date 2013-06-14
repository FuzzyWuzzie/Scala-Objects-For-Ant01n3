package org.sofa.opengl.test

import org.sofa.opengl.{SGL, VertexArray, ShaderProgram, MatrixStack, Camera, Shader, Texture, TexParams, TexMipMap}
import org.sofa.opengl.mesh.{PlaneMesh, EditableMesh, Mesh, VertexAttribute}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController}
import org.sofa.math.{Matrix4, Rgba, Vector4, Vector3}
import org.sofa.opengl.io.collada.{ColladaFile}

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
	val planeMesh = new PlaneMesh(2, 2, 4, 4)
	var normalsMesh:Mesh = null

	var colorTex:Texture = null
	var nMapTex:Texture = null

	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.Grey20
	var lightZ = 3.0
	val light1 = Vector4(1.7, 2.3, lightZ, 1)
	
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
		Shader.path  += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
		Texture.path += "/Users/antoine/Documents/Programs/SOFA/textures"
		ColladaFile.path += "/Users/antoine/Documents/Art/Sculptures/Blender/"

		initGL(sgl)
		initTextures
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
		
		gl.enable(gl.CULL_FACE)
		gl.cullFace(gl.BACK)
		gl.frontFace(gl.CW)
		
		gl.disable(gl.BLEND)
		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
	}

	protected def initTextures() {
		colorTex = new Texture(gl, "CubicThing_Color.png", TexParams(mipMap=TexMipMap.Generate))
	    colorTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    colorTex.wrap(gl.REPEAT)

		nMapTex = new Texture(gl, "CubicThing_NMap.png", TexParams(mipMap=TexMipMap.Generate))
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
		import VertexAttribute._

		initThing
	
		thing   = thingMesh.newVertexArray(gl, nMapShad, Vertex -> "position", Normal -> "normal", Tangent -> "tangent", TexCoord -> "texCoords")
		plane   = planeMesh.newVertexArray(gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		normals = normalsMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
	}
	
	def initThing() {
		val model = new ColladaFile("CubicThing_001.dae")

		model.library.geometry("Cube").get.mesh.mergeVertices(true)

		thingMesh = model.library.geometry("Cube").get.mesh.toMesh

		thingMesh.asInstanceOf[EditableMesh].autoComputeTangents(true)			// Also compute handedness and store 4-component tangents

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

		camera.lookAt
		
		// Plane

		phongShad.use
		useLights(phongShad)
		camera.uniform(phongShad)
		plane.draw(planeMesh.drawAs)
		
		// Thing

		nMapShad.use		
		colorTex.bindTo(gl.TEXTURE0)
	    nMapShad.uniform("texColor", 0)	// Texture Unit 0
	    nMapTex.bindTo(gl.TEXTURE2)
	    nMapShad.uniform("texNormal", 2)	// Texture Unit 2
			nMapShad.uniform("whitelight.pos", Vector3(camera.modelview.top * light1))
		    nMapShad.uniform("whitelight.intensity", 2f)
		    nMapShad.uniform("whitelight.ambient", 0.2f)
		    nMapShad.uniform("whitelight.specular", 256f)
		camera.uniform(nMapShad)
//		thing.draw(thingMesh.drawAs)
		camera.pushpop {
			//gl.disable(gl.CULL_FACE)
			gl.frontFace(gl.CCW)
			//camera.scaleModel(0.01, 0.01, 0.01)
			camera.translate(0, 1, 0)
			camera.uniform(nMapShad)
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