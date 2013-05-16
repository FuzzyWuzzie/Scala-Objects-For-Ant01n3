package org.sofa.opengl.test

import org.sofa.opengl.{SGL, VertexArray, ShaderProgram, MatrixStack, Camera, Shader, Texture, TexParams, TexMipMap}
import org.sofa.opengl.mesh.{Mesh, PlaneMesh, EditableMesh, LinesMesh}
import org.sofa.opengl.surface.{Surface, BasicCameraController, SurfaceRenderer}
import org.sofa.math.{Matrix4, Rgba, Vector4, Vector3, Point3}
import javax.media.opengl.{GLProfile, GLAutoDrawable, GLCapabilities, GLEventListener}
import com.jogamp.newt.opengl.{GLWindow}
import com.jogamp.newt.event.{WindowAdapter}
import com.jogamp.opengl.util.{FPSAnimator}

object TestEditableMesh2 {
	def main(args:Array[String]):Unit = { (new TestEditableMesh2).test }
}

class TestEditableMesh2 extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	var planeShad:ShaderProgram = null
	var thingShad:ShaderProgram = null
	var normalsShad:ShaderProgram = null
	
	var thing:VertexArray = null
	var plane:VertexArray = null
	var normals:VertexArray = null
	
	val thingMesh = new EditableMesh()
	val planeMesh = new PlaneMesh(2, 2, 4, 4)
	var normalsMesh:Mesh = null

	var colorTex:Texture = null
	var nMapTex:Texture = null

	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.Grey20
	val normalColor = Rgba.Red
	val tangentColor = Rgba.Green
	val light1 = Vector4(1.5, 1, 0.3, 1)
	
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
		Shader.path  += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
		Texture.path += "/Users/antoine/Documents/Programs/SOFA/textures/"

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
		gl.enable(gl.BLEND)
		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
		
		gl.disable(gl.BLEND)
	}

	protected def initTextures() {
		colorTex = new Texture(gl, "grey-concrete-texture.png", TexParams(mipMap=TexMipMap.Generate))
	    colorTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    colorTex.wrap(gl.REPEAT)

		nMapTex = new Texture(gl, "Plan_Deforme_NMap.png", TexParams(mipMap=TexMipMap.Generate))
	    nMapTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    nMapTex.wrap(gl.REPEAT)
	}
	
	protected def initShaders() {
		planeShad   = ShaderProgram(gl, "plane shader",     "es2/phonghi.vert.glsl",    "es2/phonghi.frag.glsl")
		thingShad   = ShaderProgram(gl, "normal map phong", "es2/nmapPhong.vert",       "es2/nmapPhong.frag")
		normalsShad = ShaderProgram(gl, "normals shader",   "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
	}
	
	protected def initGeometry() {
		initThing
	
		var v = thingShad.getAttribLocation("position")
	    var n = thingShad.getAttribLocation("normal")
	    var t = thingShad.getAttribLocation("tangent")
	    var u = thingShad.getAttribLocation("texCoords")

		thing = thingMesh.newVertexArray(gl, ("vertices", v), ("normals", n), ("tangents", t), ("texCoords", u))
	
		v     = planeShad.getAttribLocation("position")
		var c = planeShad.getAttribLocation("color")
		n     = planeShad.getAttribLocation("normal")

		plane = planeMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))

		v = normalsShad.getAttribLocation("position")
		c = normalsShad.getAttribLocation("color")

		normals = normalsMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
	}
	
	protected def initThing() {
		//thingMesh.storeVertexTriangleRelations(true)
		thingMesh.buildAttributes {
			thingMesh.color(1, 0, 0)
			thingMesh.normal(0, 0, 1)
			thingMesh.texCoord(0, 0)
			thingMesh.vertex(-1, 0, 0)	// 0
			thingMesh.texCoord(0.5f, 0.5f)
			thingMesh.vertex(0, 1, 0)	// 1
			thingMesh.texCoord(1, 0)
			thingMesh.vertex(1, 0, 0)	// 2
			thingMesh.color(0, 1, 0)
			thingMesh.texCoord(0, 1)
			thingMesh.vertex(-1, 2, 0)	// 3
			thingMesh.texCoord(1, 1)
			thingMesh.vertex(1, 2, 0)	// 4
		}
		
		thingMesh.buildIndices {
			thingMesh.index(0)
			thingMesh.index(1)
			thingMesh.index(2)
			
			thingMesh.index(3)
			thingMesh.index(4)
			thingMesh.index(1)

			thingMesh.index(1)
			thingMesh.index(4)
			thingMesh.index(2)

			thingMesh.index(1)
			thingMesh.index(0)
			thingMesh.index(3)
		}

		thingMesh.autoComputeTangents

		// Now show the normals and tangents

		normalsMesh = thingMesh.newNormalsTangentsMesh(Rgba(1,0,0,0.5), Rgba(0,1,0,0.5))
	}
	
	def display(surface:Surface) {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		gl.disable(gl.BLEND)

		camera.viewLookAt
		
		planeShad.use
		useLights(planeShad)
		camera.uniformMVP(planeShad)
		plane.draw(planeMesh.drawAs)
		

		thingShad.use		
		colorTex.bindTo(gl.TEXTURE0)
	    thingShad.uniform("texColor", 0)	// Texture Unit 0
	    nMapTex.bindTo(gl.TEXTURE2)
	    thingShad.uniform("texNormal", 2)	// Texture Unit 2
		//useLights(thingShad)
			thingShad.uniform("lightPos", Vector3(camera.modelview.top * light1))
		    thingShad.uniform("lightIntensity", 1f)
		    thingShad.uniform("ambientIntensity", 0.2f)
		    thingShad.uniform("specularPow", 128f)
		camera.uniformMVP(thingShad)
		thing.draw(thingMesh.drawAs)

		gl.enable(gl.BLEND)
		normalsShad.use
		camera.setUniformMVP(normalsShad)
		normals.draw(normalsMesh.drawAs)
		gl.disable(gl.BLEND)

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
		shader.uniform("light.intensity", 0.5f)
		shader.uniform("light.ambient", 0.1f)
		shader.uniform("light.specular", 10f)
	}
}