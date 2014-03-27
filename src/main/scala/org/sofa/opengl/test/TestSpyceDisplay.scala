package org.sofa.opengl.test

import org.sofa.Timer
import org.sofa.opengl.surface.{SurfaceRenderer, Surface, BasicCameraController}
import org.sofa.opengl.{SGL, ShaderProgram, MatrixStack, VertexArray, Camera, Shader, TextureFramebuffer}
import org.sofa.opengl.mesh.{PlaneMesh, CubeMesh, WireCubeMesh, AxisMesh, PointsMesh, TrianglesMesh, VertexAttribute}
import org.sofa.opengl.text.{GLFont, GLString}
import javax.media.opengl.{GLCapabilities, GLProfile}
import scala.collection.mutable.{ArrayBuffer, HashSet, Set}
import org.sofa.math.{Point3, Vector3, Vector4, Rgba, Matrix4}
import org.sofa.collection.{SpatialPoint, SpatialCube, SpatialHash, SpatialObject}
import scala.math._

object TestSpyceDisplay {
	def main(args:Array[String]) = (new TestSpyceDisplay ).test
}

class TestSpyceDisplay extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	var phongShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	var phtexShad:ShaderProgram = null
	var spyceShad:ShaderProgram = null
	var textShad:ShaderProgram = null

	val wallMesh:Array[PlaneMesh] = new Array[PlaneMesh](4)	
	val axisMesh = new AxisMesh(10)
	var trianglesMesh = new TrianglesMesh(8)

	var wall:Array[VertexArray] = new Array[VertexArray](8)
	var axis:VertexArray = null
	var triangles:VertexArray = null

	var camera:Camera = null
	var cameraTex:Camera = null
	var ctrl:BasicCameraController = null
	
	var heaFont:GLFont = null
	var subFont:GLFont = null
	var stdFont:GLFont = null

	var text:Array[GLString] = new Array[GLString](8)

	val clearColor = Rgba.Grey20
	val light1 = Vector4(0.5, 1, 0.5, 1)

	val fbWidth = 512
	val fbHeight = 256
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
		
		camera         = new Camera(); camera.viewport = (1280, 800)
		cameraTex      = new Camera(); cameraTex.viewport = (fbWidth, fbHeight)
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
		Shader.path += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
			
		initGL(sgl)
		initShaders
		initFonts
		initTextureFB
		initGeometry
		
		camera.eyeCartesian(0, 0.4, 2.7)
		camera.setFocus(0, 0.5, 0)
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
		spyceShad = ShaderProgram(gl, "spyce wall shader", "es2/spyce.vert.glsl", "es2/spyce.frag.glsl")
		textShad  = ShaderProgram(gl, "text shader", "es2/text.vert.glsl", "es2/text.frag.glsl")
	}

	def initFonts() {
		GLFont.path += "/Users/antoine/Library/Fonts"

		heaFont = new GLFont(gl, "Ubuntu-B.ttf", 40, textShad)
		subFont = new GLFont(gl, "Ubuntu-B.ttf", 30, textShad)
		stdFont = new GLFont(gl, "Ubuntu-B.ttf", 25, textShad)
		
		text(0) = new GLString(gl, heaFont, 256);	text(4) = new GLString(gl, heaFont, 256)
		text(1) = new GLString(gl, subFont, 256);	text(5) = new GLString(gl, subFont, 256)
		text(2) = new GLString(gl, stdFont, 256);	text(6) = new GLString(gl, stdFont, 256)
		text(3) = new GLString(gl, stdFont, 256);	text(7) = new GLString(gl, stdFont, 256)

		for(i <- 0 until text.length) {
			text(i).setColor(Rgba.White)
		}

		text(0).build("Player 1");			text(4).build("Player 2")
		text(1).build("Score 5000 pts");	text(5).build("Score 4000 pts")
		text(2).build("voilà, voilà");		text(6).build("ha ha ha")
		text(3).build(":-)");				text(7).build("^v^")
	}

	def initTextureFB() {
		fb = new TextureFramebuffer(gl, fbWidth, fbHeight, gl.LINEAR, gl.LINEAR)

		cameraTex.viewportPx(fb.width, fb.height)
//		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		cameraTex.orthographic(0, fb.width, 0, fb.height, -1, 1)
	}
	
	val Ground = 0
	val BackWall = 1
	val RightWall = 2
	val LeftWall = 3

	def initGeometry() {
		import VertexAttribute._

		wallMesh(Ground)    = new PlaneMesh(2, 2, 2, 1, false)
		wallMesh(BackWall)  = new PlaneMesh(2, 2, 2, 1, true)
		wallMesh(RightWall) = null
		wallMesh(LeftWall)  = null

		wallMesh(Ground).setColor(Rgba.Black)
		wallMesh(BackWall).setTextureRepeat(1,1)

		// Init the triangles

		val C = Point3(0, 0, 0)
		var i = 1
		var r = 0.0
		val radius = 10
		val color = Rgba(1, 0.8, 0, 0.4)

		trianglesMesh.setPoint(0, C)
		trianglesMesh.setPointColor(0, color)

		while(i < 17) {
			trianglesMesh.setPoint(i, cos(r).toFloat*radius, sin(r).toFloat*radius, 0)
			trianglesMesh.setPointColor(i, color)
			i += 1
			r += Pi/8
		}

		i = 0
		var p = 1;

		while(i < 8) {
			trianglesMesh.setTriangle(i, 0, p, p+1)
			p += 2
			i += 1
		}	

		// Creater VAs

		wall(Ground) = wallMesh(Ground).newVertexArray(gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")		
		wall(BackWall) = wallMesh(BackWall).newVertexArray(gl, phtexShad, Vertex -> "position", Normal -> "normal", TexCoord -> "texCoords")
		axis = axisMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
		triangles = trianglesMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
	}	

	var angle = 0.0

	def display(surface:Surface) {
		fb.display {
			gl.frontFace(gl.CW)
			gl.disable(gl.BLEND)
			gl.disable(gl.DEPTH_TEST)
			gl.clearColor(0, 0, 0, 0)
			gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
			//cameraTex.rotateViewHorizontal(0.1)
			cameraTex.viewIdentity

			// Triangles

			cameraTex.pushpop {
				gl.enable(gl.BLEND)
				gl.frontFace(gl.CCW)
				plainShad.use
				cameraTex.translate(fbWidth/2, fbHeight/2, 0)
				cameraTex.scale(40, 40, 1)
				cameraTex.rotate(angle, 0, 0, 1)
				cameraTex.uniformMVP(plainShad)
				triangles.draw(trianglesMesh.drawAs, 8*3)

				angle += 0.01
				if(angle > 2*Pi) angle = 0
			}

			// Text 
						
			textShad.use
			cameraTex.uniformMVP(textShad)
			
			cameraTex.pushpop {
				cameraTex.translate(20, fbHeight-50, 0)
				text(0).draw(cameraTex)
				cameraTex.translate(0, -40, 0)
				text(1).draw(cameraTex)
				cameraTex.translate(0, -30, 0)
				text(2).draw(cameraTex)
				cameraTex.translate(0, -25, 0)
				text(3).draw(cameraTex)
			}

			cameraTex.pushpop {
				cameraTex.translate(fbWidth-text(4).advance-20, fbHeight-50, 0)
				text(4).draw(cameraTex)
				cameraTex.translate(text(4).advance-text(5).advance, -40, 0)
				text(5).draw(cameraTex)
				cameraTex.translate(text(5).advance-text(6).advance, -30, 0)
				text(6).draw(cameraTex)
				cameraTex.translate(text(6).advance-text(7).advance, -25, 0)
				text(7).draw(cameraTex)
			}

			gl.checkErrors
			gl.enable(gl.DEPTH_TEST)
			gl.enable(gl.BLEND)
		}

		gl.clearColor(clearColor)
		gl.viewport(0, 0, surface.width, surface.height)
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		
		camera.lookAt

		// Axis
		
		gl.enable(gl.BLEND)
		plainShad.use
		camera.uniformMVP(plainShad)
		axis.draw(axisMesh.drawAs)

		// Planes
		
		gl.frontFace(gl.CW)
		camera.pushpop {
			phongShad.use
			useLights(phongShad)
			camera.translate(0,0,0.5f)
			camera.uniform(phongShad)
			wall(Ground).draw(wallMesh(Ground).drawAs)
		}
		
		gl.frontFace(gl.CCW)
		camera.pushpop {
			spyceShad.use
			fb.bindColorTextureTo(gl.TEXTURE0)
	    	spyceShad.uniform("texColor", 0)	// Texture Unit 0
			useLights(spyceShad)
			camera.translate(0,0.5,0)
			camera.uniform(spyceShad)
			wall(BackWall).draw(wallMesh(BackWall).drawAs)
			gl.bindTexture(gl.TEXTURE_2D, null)
		}

		surface.swapBuffers
		gl.checkErrors
	}

var zoom = 0.5
	
	def reshape(surface:Surface) {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		camera.frustum(-camera.viewportRatio*zoom, camera.viewportRatio*zoom, -1*zoom, 1*zoom, 2)
	}
	
	protected def useLights(shader:ShaderProgram) {
		shader.uniform("light.pos", Vector3(camera.modelview.top * light1))
		shader.uniform("light.intensity", 5f)
		shader.uniform("light.ambient", 0.1f)
		shader.uniform("light.specular", 10f)
	}
}