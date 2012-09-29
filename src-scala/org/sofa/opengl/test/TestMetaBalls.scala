package org.sofa.opengl.test

import org.sofa.opengl.surface.SurfaceRenderer
import org.sofa.opengl.SGL
import org.sofa.opengl.surface.Surface
import org.sofa.math.Matrix4
import org.sofa.math.ArrayMatrix4
import org.sofa.opengl.MatrixStack
import org.sofa.opengl.ShaderProgram
import org.sofa.opengl.mesh.Plane
import org.sofa.opengl.VertexArray
import org.sofa.opengl.Camera
import org.sofa.opengl.surface.BasicCameraController
import org.sofa.math.Rgba
import org.sofa.math.Vector4
import javax.media.opengl.GLCapabilities
import javax.media.opengl.GLProfile
import org.sofa.math.Vector3
import org.sofa.opengl.Shader
import org.sofa.opengl.mesh.DynPointsMesh
import scala.collection.mutable.ArrayBuffer
import org.sofa.math.Point3
import org.sofa.opengl.mesh.Cube
import org.sofa.opengl.mesh.WireCube
import org.sofa.opengl.mesh.WireCube
import org.sofa.opengl.mesh.Axis
import org.sofa.math.SpatialPoint
import org.sofa.math.SpatialCube
import org.sofa.math.SpatialHash
import org.sofa.math.IsoSurfaceSimple
import org.sofa.opengl.mesh.DynTriangleMesh
import org.sofa.math.IsoSurface
import org.sofa.opengl.mesh.DynIndexedTriangleMesh

object TestMetaBalls {
	def main(args:Array[String]) = (new TestMetaBalls).test
}

class MetaBall(xx:Double, yy:Double, zz:Double) extends SpatialPoint {
	val x = Point3(xx, yy, zz)
	val v = Vector3((math.random*2-1)*0.01, (math.random*2-1)*0.01, (math.random*2-1)*0.01)
	def from:Point3 = x
	def to:Point3 = x
	def move() {
		move(x)
	}
	protected def move(p:Point3) {
//		p.brownianMotion(0.05)
		p.addBy(v)
		val lim = 1
		if(p.x > lim) { p.x = lim; v.x = -v.x }
		else if(p.x < -lim) { p.x = -lim; v.x = -v.x }
		if(p.y > lim) { p.y = lim; v.y = -v.y }
		else if(p.y < -lim) { p.y = -lim; v.y = -v.y }
		if(p.z > lim) { p.z = lim; v.z = -v.z }
		else if(p.z < -lim) { p.z = -lim; v.z = -v.z }
	}
}

class TestMetaBalls extends SurfaceRenderer {
	var gl:SGL = null
	var surface:Surface = null
	
	val projection:Matrix4 = new ArrayMatrix4
	val modelview = new MatrixStack(new ArrayMatrix4)
	
	var phongShad:ShaderProgram = null
	var particlesShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	
	var plane:VertexArray = null
	var axis:VertexArray = null
	var particles:VertexArray = null	
	var wcube:VertexArray = null
	var wcubeIso1:VertexArray = null
	var wcubeIso2:VertexArray = null
	var isoSurface:VertexArray = null
	
	var axisMesh = new Axis(10)
	var planeMesh = new Plane(2, 2, 4, 4)
	var particlesMesh:DynPointsMesh = null
	var wcubeMesh:WireCube = null
	var wcubeIsoMesh:WireCube = null
	val maxDynTriangles = 6000
	var isoSurfaceMesh:DynIndexedTriangleMesh = new DynIndexedTriangleMesh(maxDynTriangles)
//	var isoSurfaceMesh:DynTriangleMesh = new DynTriangleMesh(maxDynTriangles)
	
	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val clearColor = Rgba.grey20
	val planeColor = Rgba.grey80
	val light1 = Vector4(2, 2, 2, 1)
	
	val showSpaceHash = false
	val showIsoCubes = false
	
	val size = 10
	val bucketSize = 0.5
	val random = new scala.util.Random()
	var isoDiv = 6
	val isoCellSize = 1.0 / isoDiv
	val isoLimit = 10.0
	var nTriangles = 0
	var isoSurfaceComp:IsoSurface = null
//	var isoSurfaceComp:IsoSurfaceSimple = null
	
	var spaceHash = new SpatialHash[MetaBall](bucketSize)
	var balls:ArrayBuffer[MetaBall] = null
	
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
							camera, "Metaballs !", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)	
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.includePath += "/Users/antoine/Documents/Programs/SOFA/src-scala/org/sofa/opengl/shaders/"
			
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
		gl.disable(gl.CULL_FACE)
//		gl.enable(gl.CULL_FACE)
//		gl.cullFace(gl.BACK)
//		gl.frontFace(gl.CW)
		gl.disable(gl.BLEND)
//		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
//		gl.enable(gl.PROGRAM_POINT_SIZE)	// Necessary on my ES2 implementation ?? 
	}
	
	def initShaders() {
		phongShad     = ShaderProgram(gl, "phong shader", "es2/phonghi.vert.glsl", "es2/phonghi.frag.glsl")
		particlesShad = ShaderProgram(gl, "particles shader", "es2/particles.vert.glsl", "es2/particles.frag.glsl")
		plainShad     = ShaderProgram(gl, "plain shader", "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
	}
	
	def initGeometry() {
		initBalls(size)

		initIsoSurface
		
		var v = phongShad.getAttribLocation("position")
		var c = phongShad.getAttribLocation("color")
		var n = phongShad.getAttribLocation("normal")
		
		plane = planeMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
		isoSurface = isoSurfaceMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))

		v = plainShad.getAttribLocation("position")
		c = plainShad.getAttribLocation("color")
		
		wcubeMesh = new WireCube(bucketSize.toFloat)
		wcube = wcubeMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
		
		wcubeMesh.setColor(Rgba(1, 1, 1, 0.1))
		wcubeIsoMesh = new WireCube(isoCellSize.toFloat)
		
		wcubeIsoMesh.setColor(Rgba(1, 1, 1, 0.2))
		wcubeIso1 = wcubeIsoMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
		
		wcubeIsoMesh.setColor(Rgba(1, 1, 1, 0.1))
		wcubeIso2 = wcubeIsoMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
		
		axis = axisMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
		
		v = particlesShad.getAttribLocation("position")
		c = particlesShad.getAttribLocation("color") 
		
		particles = particlesMesh.newVertexArray(gl, gl.STATIC_DRAW, ("vertices", v), ("colors", c))

		for(i <- 0 until maxDynTriangles*3) {
			isoSurfaceMesh.setPointColor(i, Rgba.red)
		}		
	}
	
	protected def initIsoSurface() {
//		for(i <- 0 until maxDynTriangles*3) {
//			isoSurfaceMesh.setPointColor(i, Rgba.white)
//		}		
		isoSurfaceMesh.setPoint(0, 0, 0, 0)
		isoSurfaceMesh.setPoint(1, 1, 0, 0)
		isoSurfaceMesh.setPoint(2, 0.5f, 1, 0)
		isoSurfaceMesh.setPoint(3, -0.5f, 1, 0)
		isoSurfaceMesh.setPoint(4, -1, 0, 0)
		isoSurfaceMesh.setPoint(5, -0.5f, -1, 0)
		isoSurfaceMesh.setPoint(6, 0.5f, -1, 0)
		isoSurfaceMesh.setPoint(7, 1.5f, -1, 0)
		isoSurfaceMesh.setPoint(8, 0, 2, 0)
		isoSurfaceMesh.setPoint(9, -1.5f, -1, 0)
		
		isoSurfaceMesh.setPointColor(0, Rgba.white)
		isoSurfaceMesh.setPointColor(1, Rgba.red)
		isoSurfaceMesh.setPointColor(2, Rgba.green)
		isoSurfaceMesh.setPointColor(3, Rgba.blue)
		isoSurfaceMesh.setPointColor(4, Rgba.cyan)
		isoSurfaceMesh.setPointColor(5, Rgba.magenta)
		isoSurfaceMesh.setPointColor(6, Rgba.yellow)
		isoSurfaceMesh.setPointColor(7, Rgba.grey20)
		isoSurfaceMesh.setPointColor(8, Rgba.grey50)
		isoSurfaceMesh.setPointColor(9, Rgba.grey80)
		
		isoSurfaceMesh.setPointNormal(0, 0, 0, 1)
		isoSurfaceMesh.setPointNormal(1, 0, 0, 1)
		isoSurfaceMesh.setPointNormal(2, 0, 0, 1)
		isoSurfaceMesh.setPointNormal(3, 0, 0, 1)
		isoSurfaceMesh.setPointNormal(4, 0, 0, 1)
		isoSurfaceMesh.setPointNormal(5, 0, 0, 1)
		isoSurfaceMesh.setPointNormal(6, 0, 0, 1)
		isoSurfaceMesh.setPointNormal(7, 0, 0, 1)
		isoSurfaceMesh.setPointNormal(8, 0, 0, 1)
		isoSurfaceMesh.setPointNormal(9, 0, 0, 1)
		
		isoSurfaceMesh.setTriangle(0, 0, 1, 2)
		isoSurfaceMesh.setTriangle(1, 0, 2, 3)
		isoSurfaceMesh.setTriangle(2, 0, 3, 4)
		isoSurfaceMesh.setTriangle(3, 0, 4, 5)
		isoSurfaceMesh.setTriangle(4, 0, 5, 6)
		isoSurfaceMesh.setTriangle(5, 0, 6, 1)
		isoSurfaceMesh.setTriangle(6, 3, 2, 8)
		isoSurfaceMesh.setTriangle(7, 4, 9, 5)
		isoSurfaceMesh.setTriangle(8, 1, 6, 7)
		nTriangles = 9
	}
	
	protected def initBalls(n:Int) {
		balls = new ArrayBuffer[MetaBall](size)
		particlesMesh = new DynPointsMesh(n) 
		
		var p = new MetaBall(0, 0, 0); balls += p; spaceHash.add(p); particlesMesh.setPoint(0, p.x); particlesMesh.setColor(0, Rgba.red)
		val angle = (math.Pi*2) / (n-1)
		var a = 0.0
		
		for(i <- 1 until n) {
			p = new MetaBall(math.cos(a), 0, math.sin(a))
			a += angle
			balls += p
			spaceHash.add(p)
			particlesMesh.setPoint(i, p.x)
			particlesMesh.setColor(i, Rgba.white)
		}
	}
	
	var T1:Long = 0
	
	def display(surface:Surface) {

		updateParticles
		
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		
		camera.setupView

		// Plane
		
//		phongShad.use
//		useLights(phongShad)
//		camera.uniformMVP(phongShad)
//		plane.draw(planeMesh.drawAs)
		
		// Axis
		
		gl.enable(gl.BLEND)
		plainShad.use
		camera.setUniformMVP(plainShad)
		axis.draw(axisMesh.drawAs)
		
		// Space hash

		drawSpaceHash
		
		// Iso-cubes

		drawIsoCubes
		
		gl.disable(gl.BLEND)
		
		// Particles
		
		particlesShad.use
		camera.setUniformMVP(particlesShad)
		particlesShad.uniform("pointSize", 30f)
		particles.draw(particlesMesh.drawAs)
		
		// Iso-Surface
		
		drawIsoSurface

		surface.swapBuffers
		gl.checkErrors
		
		val T = scala.compat.Platform.currentTime
		println("%f fps".format( 1000.0 / (T-T1)))
		T1 = T
	}

	protected def drawSpaceHash() {
		if(showSpaceHash) {
			var cs = bucketSize
			var cs2 = cs/2
			spaceHash.buckets.foreach { bucket =>
				camera.pushpop {
					val p = bucket._2.position
					camera.translateModel((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.setUniformMVP(plainShad)
					wcube.draw(wcubeMesh.drawAs)
				}
			}
		}
	}
	
	protected def drawIsoCubes() {
		if(showIsoCubes) {
			var cs = isoSurfaceComp.cellSize
			var cs2 = cs/2
			isoSurfaceComp.cubes.foreach { cube=>
				camera.pushpop {
					val p = cube.pos
					camera.translateModel((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.setUniformMVP(plainShad)
					if(!cube.isEmpty)
						wcubeIso1.draw(wcubeIsoMesh.drawAs)
					//	else wcubeIso2.draw(wcubeIsoMesh.drawAs)
				}
			}
		}
	}

	protected def drawIsoSurface() {
		if(nTriangles > 0) {
println("drawing %d triangles (%d points)".format(nTriangles, nTriangles*3))
			phongShad.use
			useLights(phongShad)
			camera.uniformMVP(phongShad)
			isoSurface.draw(isoSurfaceMesh.drawAs, nTriangles*3)
		}
	}
	
	protected def buildIsoSurface() {
		// We use the buckets to locate areas where there exist particles.
		// We put a given number of marching cubes inside one bucket, but
		// marching cubes never overlap a bucket boundary, their size is
		// a multiple of the bucket size.
		
//		val cubesByBucket = 3
//		val bucketSize = spaceHash.bucketSize
//		var cubesSize = bucketSize / cubesByBucket
//		isoSurfaceComp = new IsoSurfaceSimple(cubesSize)
//		
//		spaceHash.buckets.foreach { bucket =>
//			val x = bucket._1.x * bucketSize
//			val y = bucket._1.y * bucketSize
//			val z = bucket._1.z * bucketSize
//			
//			isoSurfaceComp.addCubesAt(x, y, z, cubesByBucket, eval, 20)
//		}
		
	
//		isoSurfaceComp = new IsoSurfaceSimple(isoCellSize)
//		isoSurfaceComp.addCubesAt(-2*isoDiv, -2*isoDiv, -2*isoDiv, 4*isoDiv, eval, isoLimit)
		isoSurfaceComp = new IsoSurface(isoCellSize)
		isoSurfaceComp.addCubesAt(-2*isoDiv, -2*isoDiv, -2*isoDiv, 4*isoDiv, 4*isoDiv, 4*isoDiv, eval, isoLimit)
		isoSurfaceComp.computeNormals
		
//print("built %s tri-points, %s cubes, ".format(isoSurfaceComp.triPoints.size, isoSurfaceComp.cubes.size))
//println("built %d triangles".format(isoSurfaceComp.triangles.size))

		// Now build the mesh

		if(isoSurfaceComp.triPoints.size > 0) {
			
			// Insert the parallel vertex, normal and color arrays.
			
			var i = 0
			var n = isoSurfaceComp.triPoints.size
			
			while(i<n) {
				isoSurfaceMesh.setPoint(i, isoSurfaceComp.triPoints(i))
				isoSurfaceMesh.setPointNormal(i, isoSurfaceComp.normals(i))
//				isoSurfaceMesh.setPointColor(i, Rgba.red)
				i += 1
			}
			
			// Insert the triangles as indices
			
			nTriangles = 0
			isoSurfaceComp.nonEmptyCubes.foreach { cube =>
				if(cube.triangles ne null) {
					cube.triangles.foreach { triangle =>
						if(nTriangles < maxDynTriangles) {
							isoSurfaceMesh.setTriangle(nTriangles, triangle.a, triangle.b, triangle.c)
							nTriangles += 1
						}
					}
				}
			}

//			isoSurfaceComp.cubes.foreach { cube =>
//				if(cube.triangles ne null) {
//					cubeCount += 1
//					cube.triangles.foreach { triangle =>
//						if(i < maxDynTriangles) {
//							nTriangles += 1
//							val p0 = isoSurfaceComp.triPoints(triangle.a)
//							val p1 = isoSurfaceComp.triPoints(triangle.b)
//							val p2 = isoSurfaceComp.triPoints(triangle.c)
//							isoSurfaceMesh.setTriangle(i, p0, p1, p2)
//							val n0 = isoSurfaceComp.normals(triangle.a)
//							val n1 = isoSurfaceComp.normals(triangle.b)
//							val n2 = isoSurfaceComp.normals(triangle.c)
//							isoSurfaceMesh.setNormal(i, n0, n1, n2)
//							//isoSurfaceMesh.autoComputeNormal(i)
//							//println("=> %s [%s, %s, %s]".format(triangle, p0, p1, p2))
//						} else {
//							println("HAHAHAHAHAHA!!!")
//						}
//						i += 1
//					}
//				}
//			}
			
			isoSurfaceMesh.updateVertexArray(gl, "vertices", "colors", "normals")
		}
	}

	
	protected def updateParticles() {
		val potential = spaceHash.neighborsInBox(balls(0), 4)
		
		var i = 0
		balls.foreach { particle => 
			particle.move()
			particlesMesh.setPoint(i, particle.x)
			spaceHash.move(particle)
			i += 1
		}
		
		particlesMesh.updateVertexArray(gl, "vertices", "colors")
		
		buildIsoSurface
		
		gl.checkErrors
	}
	
	/** Evaluate the meta-balls iso-surface at x. */
	def eval(x:Point3):Double = {
		var value = 0.0
		
		balls.foreach { i =>
			val xx = (x.x - i.x.x)
			val yy = (x.y - i.x.y)
			val zz = (x.z - i.x.z)
			
			value += 1.0 / ( xx*xx + yy*yy + zz*zz )  
		}

		value
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