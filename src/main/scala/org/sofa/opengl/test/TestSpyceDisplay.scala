package org.sofa.opengl.test

import org.sofa.Timer
import org.sofa.opengl.surface.{SurfaceRenderer, Surface, BasicCameraController}
import org.sofa.opengl.{SGL, ShaderProgram, MatrixStack, VertexArray, Camera, Shader, TextureFramebuffer}
import org.sofa.opengl.mesh.{Plane, Cube, WireCube, Axis, DynPointsMesh}
import org.sofa.opengl.text.{GLFont, GLString}
import javax.media.opengl.{GLCapabilities, GLProfile}
import scala.collection.mutable.{ArrayBuffer, HashSet, Set}
import org.sofa.math.{SpatialPoint, SpatialCube, SpatialHash, SpatialObject, Point3, Vector3, Vector4, Rgba, Matrix4}

object TestSpyceDisplay {
	def main(args:Array[String]) = (new TestSpyceDisplay
	).test
}

class TestSpyceDisplay extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	var phongShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	var phtexShad:ShaderProgram = null
	var textShad:ShaderProgram = null

	val wallMesh:Array[Plane] = new Array[Plane](4)	
	var axisMesh = new Axis(10)

	var wall:Array[VertexArray] = new Array[VertexArray](4)
	var axis:VertexArray = null

	var camera:Camera = null
	var cameraTex:Camera = null
	var ctrl:BasicCameraController = null
	
	var heaFont:GLFont = null
	var subFont:GLFont = null
	var stdFont:GLFont = null

	var text:Array[GLString] = new Array[GLString](4)

	val clearColor = Rgba.grey20
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
		cameraTex      = new Camera()
		ctrl           = new MyCameraController(camera, light1)
		initSurface    = initializeSurface
		frame          = display
		surfaceChanged = reshape
		key            = ctrl.key
		motion         = ctrl.motion
		scroll         = ctrl.scroll
		close          = { surface => sys.exit }
		surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
							camera, "Spyce Like Wall", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)	
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.includePath += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
			
		initGL(sgl)
		initShaders
		initFonts
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
	}
	
	def initShaders() {
		phongShad = ShaderProgram(gl, "phong shader", "es2/phonghi.vert.glsl", "es2/phonghi.frag.glsl")
		plainShad = ShaderProgram(gl, "plain shader", "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
		phtexShad = ShaderProgram(gl, "textured phong shader", "es2/phonghitex.vert.glsl", "es2/phonghitex.frag.glsl")
		textShad  = ShaderProgram(gl, "text shader", "es2/text.vert.glsl", "es2/text.frag.glsl")
	}

	def initFonts() {
		GLFont.path += "/Users/antoine/Library/Fonts"

		heaFont = new GLFont(gl, "Ubuntu-B.ttf", 40, 0, 0)
		subFont = new GLFont(gl, "Ubuntu-RI.ttf", 30, 0, 0)
		stdFont = new GLFont(gl, "Ubuntu-R.ttf", 20, 0, 0)
		
		heaFont.minMagFilter(gl.LINEAR, gl.LINEAR)
		subFont.minMagFilter(gl.LINEAR, gl.LINEAR)
		stdFont.minMagFilter(gl.LINEAR, gl.LINEAR)

		text(0) = new GLString(gl, heaFont, 256)
		text(1) = new GLString(gl, subFont, 256)
		text(2) = new GLString(gl, stdFont, 256)
		text(3) = new GLString(gl, stdFont, 256)

		for(i <- 0 until text.length) {
			text(i).setColor(Rgba.white)
		}

		text(0).build("Player 1")
		text(1).build("Score 5000 pt")
		text(2).build("voilà, voilà")
		text(3).build("...")
	}

	def initTextureFB() {
		fb = new TextureFramebuffer(gl, 1024, 1024, gl.LINEAR, gl.LINEAR)

		cameraTex.viewportPx(fb.width, fb.height)
//		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		cameraTex.orthographic(0, fb.width, 0, fb.height, -1, 1)
	}
	
	val Ground = 0
	val BackWall = 1
	val RightWall = 2
	val LeftWall = 3

	def initGeometry() {
		wallMesh(Ground)    = new Plane(2, 2, 2, 1, false)
		wallMesh(BackWall)  = new Plane(2, 2, 2, 1, true)
		wallMesh(RightWall) = null
		wallMesh(LeftWall)  = null

		var v = phongShad.getAttribLocation("position")
		var c = phongShad.getAttribLocation("color")
		var n = phongShad.getAttribLocation("normal")

		wallMesh(Ground).setColor(Rgba.red)
		wall(Ground) = wallMesh(Ground).newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
		
		v = phtexShad.getAttribLocation("position")
		n = phtexShad.getAttribLocation("normal")
		c = phtexShad.getAttribLocation("texCoords")

		wallMesh(BackWall).setTextureRepeat(1,1)
		wall(BackWall) = wallMesh(BackWall).newVertexArray(gl, ("vertices", v), ("normals", n), ("texcoords", c))

		v = plainShad.getAttribLocation("position")
		c = plainShad.getAttribLocation("color")
		
		axis = axisMesh.newVertexArray(gl, ("vertices", v), ("colors", c))		
	}	

	def display(surface:Surface) {
		fb.display {
			gl.frontFace(gl.CW)
			gl.disable(gl.BLEND)
			gl.disable(gl.DEPTH_TEST)
			gl.clearColor(0, 0, 0, 0)
			gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
			//cameraTex.rotateViewHorizontal(0.1)
			cameraTex.viewIdentity
			
			textShad.use
			cameraTex.setUniformMVP(textShad)
			
			cameraTex.pushpop {
				cameraTex.translateModel(20, 1000, 0)
				text(0).draw(cameraTex)
				cameraTex.translateModel(0, -40, 0)
				text(1).draw(cameraTex)
				cameraTex.translateModel(0, -30, 0)
				text(2).draw(cameraTex)
				cameraTex.translateModel(0, -20, 0)
				text(3).draw(cameraTex)
			}

			gl.checkErrors
			gl.enable(gl.DEPTH_TEST)
			gl.enable(gl.BLEND)
		}

		gl.clearColor(clearColor)
		gl.viewport(0, 0, surface.width, surface.height)
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		gl.frontFace(gl.CW)
		
		camera.viewLookAt

		// Axis
		
		gl.enable(gl.BLEND)
		plainShad.use
		camera.setUniformMVP(plainShad)
		axis.draw(axisMesh.drawAs)

		// Planes
		
		camera.pushpop {
			phongShad.use
			useLights(phongShad)
			camera.translateModel(0,0,0.5f)
			camera.uniformMVP(phongShad)
			wall(Ground).draw(wallMesh(Ground).drawAs)
		}
		
		camera.pushpop {
			phtexShad.use
			fb.bindColorTextureTo(gl.TEXTURE0)
	    	phtexShad.uniform("texColor", 0)	// Texture Unit 0
			useLights(phtexShad)
			camera.translateModel(0,0.5,0)
			camera.uniformMVP(phtexShad)
			wall(BackWall).draw(wallMesh(BackWall).drawAs)
			gl.bindTexture(gl.TEXTURE_2D, 0)
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
		shader.uniform("light.intensity", 4f)
		shader.uniform("light.ambient", 0.1f)
		shader.uniform("light.specular", 100f)
	}
}