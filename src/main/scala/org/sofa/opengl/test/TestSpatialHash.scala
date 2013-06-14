package org.sofa.opengl.test

import org.sofa.math.{Point3, SpatialObject, SpatialPoint, SpatialCube, SpatialHash, Rgba, Matrix4, Vector3, Vector4}
import org.sofa.opengl.surface.{SurfaceRenderer, Surface, BasicCameraController}
import org.sofa.opengl.{SGL, MatrixStack, ShaderProgram, Camera, VertexArray, Shader}
import org.sofa.opengl.mesh.{PlaneMesh, VertexAttribute, PointsMesh, CubeMesh, WireCubeMesh, AxisMesh}
import javax.media.opengl.{GLCapabilities, GLProfile}
import scala.collection.mutable.ArrayBuffer

object TestSpatialHash {
	def main(args:Array[String]) = (new TestSpatialHash).test
}

trait TestObject extends SpatialObject {
	val v = Vector3((math.random*2-1)*0.05, (math.random*2-1)*0.05, (math.random*2-1)*0.05)
	protected def move(p:Point3) {
		p.addBy(v)
		val lim = 5
		if(p.x > lim) { p.x = lim; v.x = -v.x }
		else if(p.x < -lim) { p.x = -lim; v.x = -v.x }
		if(p.y > lim) { p.y = lim; v.y = -v.y }
		else if(p.y < -lim) { p.y = -lim; v.y = -v.y }
		if(p.z > lim) { p.z = lim; v.z = -v.z }
		else if(p.z < -lim) { p.z = -lim; v.z = -v.z }
	}
}

class TestParticle(xx:Double, yy:Double, zz:Double) extends SpatialPoint with TestObject {
	val x = Point3(xx, yy, zz)
	def from:Point3 = x
	def to:Point3 = x
	def move() {
		move(x)
	}
}

class TestVolume(val side:Double) extends SpatialCube with TestObject {
	val x = Point3(-side/2, -side/2, -side/2)
	val y = Point3(side/2, side/2, side/2)
	def from:Point3 = x 
	def to:Point3 = y
	def move() {
		move(x)
		move(y)
	}
}

class TestSpatialHash extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	var phongShad:ShaderProgram = null
	var particlesShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	
	var plane:VertexArray = null
	var axis:VertexArray = null
	var particles:VertexArray = null	
	var wcube:VertexArray = null
	var cube:VertexArray = null
	
	var axisMesh = new AxisMesh(10)
	var planeMesh = new PlaneMesh(2, 2, 4, 4)
	var particlesMesh:PointsMesh = null
	var wcubeMesh:WireCubeMesh = null
	var cubeMesh = new CubeMesh(1)
	
	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.Grey20
	val planeColor = Rgba.Grey80
	val light1 = Vector4(2, 2, 2, 1)
	
	val size = 50
	val bucketSize = 0.5
	val random = new scala.util.Random()

	var spaceHash = new SpatialHash[TestObject,TestParticle,TestVolume](bucketSize)
	var simu:ArrayBuffer[TestParticle] = null
	val simuCube = new TestVolume(0.4)
	val simuCube2 = new TestVolume(0.8)
	
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
							camera, "Test SPH", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)	
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.path += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
			
		initGL(sgl)
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
		gl.enable(gl.PROGRAM_POINT_SIZE)	// Necessary on my ES2 implementation ?? 
	}
	
	def initShaders() {
		phongShad = ShaderProgram(gl, "phong shader", "es2/phonghi.vert.glsl", "es2/phonghi.frag.glsl")
		particlesShad = ShaderProgram(gl, "particles shader", "es2/particles.vert.glsl", "es2/particles.frag.glsl")
		plainShad = ShaderProgram(gl, "plain shader", "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
	}
	
	def initGeometry() {
		import VertexAttribute._

		initParticles(size)
		cubeMesh.setColor(Rgba.Red)
		wcubeMesh = new WireCubeMesh(bucketSize.toFloat)
		wcubeMesh.setColor(new Rgba(1, 1, 1, 0.1))
		
		plane     = planeMesh.newVertexArray(gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		cube      = cubeMesh.newVertexArray(gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")		
		wcube     = wcubeMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
		axis      = axisMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
		particles = particlesMesh.newVertexArray(gl, gl.STATIC_DRAW, particlesShad, Vertex -> "position", Color -> "color")
	}
	
	protected def initParticles(n:Int) {
		simu = new ArrayBuffer[TestParticle](size)
		particlesMesh = new PointsMesh(n) 
		
		for(i <- 0 until n) {
			val p = new TestParticle(random.nextFloat, random.nextFloat, random.nextFloat)
			simu += p
			spaceHash.add(p)
			particlesMesh.setPoint(i, p.x)
			particlesMesh.setColor(i, Rgba.White)
		}
		
		spaceHash.add(simuCube)
		spaceHash.add(simuCube2)
	}
	
	def display(surface:Surface) {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		gl.frontFace(gl.CW)
		
		camera.lookAt

		// Plane
		
//		phongShad.use
//		useLights(phongShad)
//		camera.uniform(phongShad)
//		plane.draw(planeMesh.drawAs)
		
		// Axis
		
		gl.enable(gl.BLEND)
		plainShad.use
		camera.uniformMVP(plainShad)
		axis.draw(axisMesh.drawAs)
		
		// Space hash
		
		val cs = bucketSize
		val cs2 = cs/2
		spaceHash.buckets.foreach { bucket =>
			camera.pushpop {
				val p = bucket._2.position
				camera.translate((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
				camera.uniformMVP(plainShad)
				wcube.draw(wcubeMesh.drawAs)
			}
		}
		gl.disable(gl.BLEND)
		
		// Particles
		
		particlesShad.use
		camera.uniformMVP(particlesShad)
		particlesShad.uniform("pointSize", 30f)
		particles.draw(particlesMesh.drawAs)
		
		// Cube
		
		phongShad.use
		useLights(phongShad)
		drawCube(simuCube)
		drawCube(simuCube2)
		
		surface.swapBuffers
		gl.checkErrors
		
		updateParticles
	}

	protected def drawCube(simuCube:TestVolume) {
		camera.pushpop {
			val side = simuCube.side
			camera.translate(simuCube.x.x+side/2, simuCube.x.y+side/2, simuCube.x.z+side/2)
			camera.scale(side, side, side)
			camera.uniform(phongShad)
			cube.draw(cubeMesh.drawAs)
		}
	}
	
	protected def updateParticles() {
		var i = 0
		simu.foreach { particle => 
			particle.move()
			particlesMesh.setPoint(i, particle.x)
			spaceHash.move(particle)
			i += 1
		}
		
		simuCube.move()
		simuCube2.move()
		spaceHash.move(simuCube)
		spaceHash.move(simuCube2)
		
		particlesMesh.updateVertexArray(gl, true, true)
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