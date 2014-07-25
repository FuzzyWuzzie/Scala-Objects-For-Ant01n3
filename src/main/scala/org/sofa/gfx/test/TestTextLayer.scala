package org.sofa.gfx.test

import org.sofa.Timer
import org.sofa.math.{Point3, Rgba, Matrix4, Vector3, Vector4}
import org.sofa.collection.{SpatialObject, SpatialPoint, SpatialCube, SpatialHash}
import org.sofa.gfx.surface.{SurfaceRenderer, Surface, BasicCameraController}
import org.sofa.gfx.{SGL, MatrixStack, ShaderProgram, Camera, VertexArray, Shader}
import org.sofa.gfx.mesh.{PlaneMesh, VertexAttribute, PointsMesh, CubeMesh, WireCubeMesh, AxisMesh}
import org.sofa.gfx.text.{TextLayer, GLFont}

import javax.media.opengl.{GLCapabilities, GLProfile}

import scala.collection.mutable.ArrayBuffer


object TestTextLayer extends App {
	new TestTextLayer().test
}


// trait TestObject extends SpatialObject {
	
// 	val v = Vector3((math.random*2-1)*0.05, (math.random*2-1)*0.05, (math.random*2-1)*0.05)

// 	protected def move(p:Point3) {
// 		p.addBy(v)
		
// 		val lim = 5
		
// 		if(p.x > lim) { p.x = lim; v.x = -v.x }
// 		else if(p.x < -lim) { p.x = -lim; v.x = -v.x }
		
// 		if(p.y > lim) { p.y = lim; v.y = -v.y }
// 		else if(p.y < -lim) { p.y = -lim; v.y = -v.y }
		
// 		if(p.z > lim) { p.z = lim; v.z = -v.z }
// 		else if(p.z < -lim) { p.z = -lim; v.z = -v.z }
// 	}
// }


// class TestParticle(xx:Double, yy:Double, zz:Double) extends SpatialPoint with TestObject {
	
// 	val from:Point3 = Point3(xx, yy, zz)
	
// 	val to:Point3 = from
	
// 	def move() { move(from) }
// }


// class TestVolume(val side:Double) extends SpatialCube with TestObject {
		
// 	val from:Point3 = Point3(-side/2, -side/2, -side/2)
	
// 	val to:Point3 = Point3(side/2, side/2, side/2)
	
// 	def move() { move(from); move(to) }
// }


class TestTextLayer extends SurfaceRenderer {

// Constants

	val ParticleCount = 50
	val CubeCount = 20
	val BucketSize = 0.5

// Attributes

	var gl:SGL = null
	var surface:Surface = null
		
	var phongShad:ShaderProgram = null
	var particlesShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	var textShad:ShaderProgram = null
		
	val axis = new AxisMesh(10)
	var particles:PointsMesh = null
	var wcube:WireCubeMesh = null
	val cube = new CubeMesh(1)
	
	val clearColor = Rgba.Grey20
	val planeColor = Rgba.Grey80
	val light1 = Vector4(2, 2, 2, 1)
	
	val camera = new Camera()
	val ctrl:BasicCameraController = new MyCameraController(camera, light1)
	
	val random = new scala.util.Random()

	val spaceHash = new SpatialHash[TestObject,TestParticle,TestVolume](BucketSize)
	var particleSet:ArrayBuffer[TestParticle] = null
	var cubeSet:ArrayBuffer[TestVolume] = null

	var textLayer:TextLayer = null

	val timer = new Timer()
	
// Init

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
		
		initSurface    = initializeSurface
		frame          = display
		surfaceChanged = reshape
		actionKey      = ctrl.actionKey
		motion         = ctrl.motion
		gesture        = ctrl.gesture
		close          = { surface => sys.exit }
		surface        = new org.sofa.gfx.backend.SurfaceNewt(this,
							camera, "Test TextLayer", caps,
							org.sofa.gfx.backend.SurfaceNewtGLBackend.GL2ES2)	
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.path += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/gfx/shaders/"
			
		initGL(sgl)
		initShaders
		initGeometry
		initText
		
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
	
	protected def initShaders() {
		phongShad     = ShaderProgram(gl, "phong shader",     "es2/phonghi.vert.glsl",    "es2/phonghi.frag.glsl")
		particlesShad = ShaderProgram(gl, "particles shader", "es2/particles.vert.glsl",  "es2/particles.frag.glsl")
		plainShad     = ShaderProgram(gl, "plain shader",     "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
		textShad      = ShaderProgram(gl, "text shader",      "es2/text.vert.glsl",       "es2/text.frag.glsl")
	}
	
	protected def initGeometry() {
		import VertexAttribute._

		initParticles(ParticleCount, CubeCount)
		
		cube.setColor(Rgba(0.7,0.3,0.6,1))
		wcube = new WireCubeMesh(BucketSize.toFloat)
		wcube.setColor(new Rgba(1, 1, 1, 0.1))
		
		cube.newVertexArray( gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")		
		wcube.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
		axis.newVertexArray( gl, plainShad, Vertex -> "position", Color -> "color")
		particles.newVertexArray(gl, gl.STATIC_DRAW, particlesShad, Vertex -> "position", Color -> "color")
	}
	
	protected def initParticles(pCount:Int, cCount:Int) {
		particleSet = new ArrayBuffer[TestParticle](pCount)
		particles = new PointsMesh(pCount) 
		
		for(i <- 0 until pCount) {
			val p = new TestParticle(random.nextFloat, random.nextFloat, random.nextFloat)
			particleSet += p
			spaceHash.add(p)
			particles.setPoint(i, p.from)
			particles.setColor(i, Rgba.White)
		}
		
		cubeSet = new ArrayBuffer[TestVolume](cCount)

		for(i <- 0 until cCount) {
			val c = new TestVolume(i%2 + 0.5)
			cubeSet += c
			spaceHash.add(c)
		}
	}

	protected def initText() {
		GLFont.path += "/Users/antoine/Library/Fonts"
		GLFont.path += "Fonts"

		textLayer = new TextLayer(gl, textShad)
	}

// Rendering
	
	def display(surface:Surface) {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		gl.frontFace(gl.CW)
		
		camera.lookAt

		// Axis
		
		gl.enable(gl.BLEND)
		plainShad.use
		camera.uniformMVP(plainShad)
		axis.lastva.draw(axis.drawAs(gl))
		
		// Space hash
		
		val cs = BucketSize
		val cs2 = cs/2
		spaceHash.buckets.foreach { bucket =>
			camera.pushpop {
				val p = bucket._2.position
				camera.translate((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
				camera.uniformMVP(plainShad)
				wcube.lastva.draw(wcube.drawAs(gl))
			}
		}
		gl.disable(gl.BLEND)
		
		// Particles
		
		particlesShad.use
		camera.uniformMVP(particlesShad)
		particlesShad.uniform("pointSize", 30f)
		particles.lastva.draw(particles.drawAs(gl))
		
		// Cubes
		
		var i = 0

		textLayer.font("Ubuntu-L.ttf", 13)
		phongShad.use
		useLights(phongShad)
		cubeSet.foreach { cube => drawCube(cube, i); i += 1 }
				
		//surface.swapBuffers
		gl.checkErrors
		
		// Text

		textLayer.font("Ubuntu-B.ttf", 20)
		textLayer.color(Rgba(1, 0.6, 0))
		textLayer.stringpx("'TextLayer'", 10, 10)
		textLayer.color(Rgba.White)
		textLayer.stringpx("Test", 10, 10+textLayer.lastHeight+10)
		textLayer.render(camera)

		// Recompute

		textLayer.color(Rgba(1, 0.6, 0))
		updateParticles
	}

	protected def drawCube(aCube:TestVolume, i:Int) {
		camera.pushpop {
			val side = aCube.side
			camera.translate(aCube.from.x+side/2, aCube.from.y+side/2, aCube.from.z+side/2)
			textLayer.color(Rgba.White)
			textLayer.string("cube%d".format(i), 0, 0, 0, camera)
			camera.scale(side, side, side)
			camera.uniform(phongShad)
			cube.lastva.draw(cube.drawAs(gl))
		}
	}
	
	protected def updateParticles() {
		var i = 0

		textLayer.font("Ubuntu-L.ttf", 13)
//		timer.measure("points") {
			particleSet.foreach { particle => 
				particle.move()
				particles.setPoint(i, particle.from)
				spaceHash.move(particle)
				textLayer.string("p%d".format(i), particle.from, camera)
				i += 1
			}
//		}
		
//		timer.measure("cubes") {
			cubeSet.foreach { cube =>
				cube.move()
				spaceHash.move(cube)
			}
//		}
		
		particles.updateVertexArray(gl, true, true)
		gl.checkErrors

//		timer.printAvgs("TestSpatialHash")
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