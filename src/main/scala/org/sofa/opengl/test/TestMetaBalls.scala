package org.sofa.opengl.test

import org.sofa.opengl.{SGL, MatrixStack, ShaderProgram, VertexArray, Camera, Shader}
import org.sofa.opengl.surface.{SurfaceRenderer, Surface, BasicCameraController, KeyEvent}
import org.sofa.opengl.mesh.{PlaneMesh, PointsMesh, CubeMesh, AxisMesh, WireCubeMesh, TrianglesMesh, VertexAttribute}
import org.sofa.math.{Matrix4, Rgba, Vector4, Vector3, Point3, IsoSurfaceSimple, IsoSurface, IsoCube}
import org.sofa.collection.{SpatialPoint, SpatialCube, SpatialHash}
import javax.media.opengl.{GLCapabilities, GLProfile}
import scala.collection.mutable.ArrayBuffer

object TestMetaBalls {
	def main(args:Array[String]) = (new TestMetaBalls).test
}

object MetaBall {
	/** Speed factor of meta balls. */
	val speed = 0.05
}

class MetaBall(xx:Double, yy:Double, zz:Double) extends SpatialPoint {
	val x = Point3(xx, yy, zz)
	val v = Vector3((math.random*2-1)*MetaBall.speed,
			        (math.random*2-1)*MetaBall.speed,
			        (math.random*2-1)*MetaBall.speed)
	def from:Point3 = x
	def to:Point3 = x
	def move() {
		move(x)
	}
	protected def move(p:Point3) {
//		p.brownianMotion(0.05)
		p.addBy(v)
		val lim = 1.5
		if(p.x > lim) { p.x = lim; v.x = -v.x }
		else if(p.x < -lim) { p.x = -lim; v.x = -v.x }
		if(p.y > lim) { p.y = lim; v.y = -v.y }
		else if(p.y < -lim) { p.y = -lim; v.y = -v.y }
		if(p.z > lim) { p.z = lim; v.z = -v.z }
		else if(p.z < -lim) { p.z = -lim; v.z = -v.z }
	}
}

class TestMetaBalls extends SurfaceRenderer {
	
	// --------------------------------------------------------------
	// Settings
	
	/** Show the space hash used to fasten particles retrieving ? */
	var showSpaceHash  = false
	
	/** Show the cubes used to compute the iso-surface ? */
	var showIsoCubes = false
	
	/** Show the particles used as centers for the meta balls. */
	var showParticles = false
	
	/** Max number of triangles to use to draw the iso-surface. */
	val maxDynTriangles = 6000
	
	/** Number meta-balls. */
	val metaBallCount = 10
	
	/** Size of a space-hash cube. */
	val bucketSize = 0.5
	
	/** Number of iso-cubes inside a space hash cube. */
	var isoDiv = 4.0
	
	val isoCellSize = bucketSize / isoDiv
	
	/** Limit of the iso-surface in the implicit function used to define the surface. */
	val isoLimit = 13.0
	
	/** Size of the particles on screen. */
	val pointSize = 30f
	
	val clearColor = Rgba.Grey10
	val planeColor = Rgba.Grey80
	val surfaceColor = Rgba(1, 0.6, 0, 1)
	val light1 = Vector4(3, 3, 3, 1)

	// --------------------------------------------------------------
	// Values	
	
	var gl:SGL = null
	var surface:Surface = null

	val random = new scala.util.Random()
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	var phongShad:ShaderProgram = null
	var particlesShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	
	var axis:VertexArray = null
	var particles:VertexArray = null	
	var wcube:VertexArray = null
	var wcubeIso1:VertexArray = null
	var wcubeIso2:VertexArray = null
	var isoSurface:VertexArray = null
	
	var axisMesh = new AxisMesh(10)
	var particlesMesh:PointsMesh = null
	var wcubeMesh:WireCubeMesh = null
	var wcubeIsoMesh:WireCubeMesh = null
	var isoSurfaceMesh:TrianglesMesh = new TrianglesMesh(maxDynTriangles)
	
	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	var play = true
	var isoSurfaceComp:IsoSurface = null
	var spaceHash = new SpatialHash[MetaBall,MetaBall,SpatialCube](bucketSize)
	var balls:ArrayBuffer[MetaBall] = null
	
	// --------------------------------------------------------------
	// Commands
	
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
		ctrl           = new MetaBallsCameraController(camera, this)
		initSurface    = initializeSurface
		frame          = display
		surfaceChanged = reshape
		key            = ctrl.key
		motion         = ctrl.motion
		scroll         = ctrl.scroll
		close          = { surface => sys.exit }

		//camera.viewport = (1600,600)

		surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
							camera, "Meta bouboules", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)	
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.path += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
			
		initGL(sgl)
		initShaders
		initGeometry
		
		camera.eyeCartesian(3, 3, 3)
		camera.setFocus(0, 0, 0)
		reshape(surface)
	}
	
	protected def initGL(sgl:SGL) {
		gl = sgl
		
		gl.clearColor(clearColor)
		gl.clearDepth(1f)
		gl.enable(gl.DEPTH_TEST)
		gl.disable(gl.CULL_FACE)
		gl.disable(gl.BLEND)
		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
		gl.enable(gl.PROGRAM_POINT_SIZE)	// Necessary on my ES2 implementation ?? 
	}
	
	def pausePlay() {
		play = !play
	}
	
	def initShaders() {
		phongShad     = ShaderProgram(gl, "phong shader",     "es2/phonghi.vert.glsl",    "es2/phonghi.frag.glsl")
		particlesShad = ShaderProgram(gl, "particles shader", "es2/particles.vert.glsl",  "es2/particles.frag.glsl")
		plainShad     = ShaderProgram(gl, "plain shader",     "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
	}
	
	def initGeometry() {
		import VertexAttribute._

		initBalls(metaBallCount)

		isoSurface = isoSurfaceMesh.newVertexArray(gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		
		wcubeMesh = new WireCubeMesh(bucketSize.toFloat)
		wcube = wcubeMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
		
		wcubeMesh.setColor(Rgba(1, 1, 1, 0.1))
		wcubeIsoMesh = new WireCubeMesh(isoCellSize.toFloat)
		
		wcubeIsoMesh.setColor(Rgba(1, 0, 0, 0.2))
		wcubeIso1 = wcubeIsoMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
		
		wcubeIsoMesh.setColor(Rgba(0, 0, 1, 0.2))
		wcubeIso2 = wcubeIsoMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
		
		axis = axisMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")
		
		particles = particlesMesh.newVertexArray(gl, particlesShad, Vertex -> "position", Color -> "color")

		for(i <- 0 until maxDynTriangles*3) {
			isoSurfaceMesh.setPointColor(i, surfaceColor)
		}		
	}
	
	protected def initBalls(n:Int) {
		balls = new ArrayBuffer[MetaBall](n)
		particlesMesh = new PointsMesh(n) 
		
		for(i <- 0 until n) {
			val p = new MetaBall(random.nextDouble*2-1, random.nextDouble*2-1, random.nextDouble*2-1)
			balls += p
			spaceHash.add(p)
			particlesMesh.setPoint(i, p.x)
			particlesMesh.setColor(i, Rgba.Magenta)
		}
	}
	
	var T1:Long = 0
	var avgT:Double = 0.0
	var steps = 1
	
	def display(surface:Surface) {

		if(play)
			updateParticles
		
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		
		camera.lookAt

		drawIsoSurface
		drawAxis
		drawSpaceHash
		drawIsoCubes
		drawParticles

		surface.swapBuffers
		gl.checkErrors
		
		val T = scala.compat.Platform.currentTime
		val fps = 1000.0 / (T-T1)
		avgT += fps
		println("%.2f fps (avg %.2f)".format(fps, avgT/steps))
		T1 = T
		steps+=1
	}

	protected def drawAxis() {
		gl.enable(gl.BLEND)
		plainShad.use
		camera.uniformMVP(plainShad)
		axis.draw(axisMesh.drawAs(gl))
		gl.disable(gl.BLEND)
	}
	
	protected def drawSpaceHash() {
		if(showSpaceHash) {
			gl.enable(gl.BLEND)
			var cs = bucketSize
			var cs2 = cs/2
			spaceHash.buckets.foreach { bucket =>
				camera.pushpop {
					val p = bucket._2.position
					camera.translate((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.uniformMVP(plainShad)
					wcube.draw(wcubeMesh.drawAs(gl))
				}
			}
			gl.disable(gl.BLEND)
		}
	}
	
	protected def drawIsoCubes() {
		if(showIsoCubes) {
			gl.enable(gl.BLEND)
			var cs = isoSurfaceComp.cellSize
			var cs2 = cs/2
			isoSurfaceComp.cubes.foreach { cube=>
				camera.pushpop {
					val p = cube.pos
					camera.translate((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.uniformMVP(plainShad)
					if(!cube.isEmpty) { wcubeIso1.draw(wcubeIsoMesh.drawAs(gl)) }
//				    else { wcubeIso2.draw(wcubeIsoMesh.drawAs) }
				}
			}
			gl.disable(gl.BLEND)
		}
	}

	protected def drawIsoSurface() {
		if(isoSurfaceComp.triangleCount > 0) {
//println("drawing %d triangles (%d points)".format(nTriangles, nTriangles*3))
			phongShad.use
			useLights(phongShad)
			camera.uniform(phongShad)
			isoSurface.draw(isoSurfaceMesh.drawAs(gl), isoSurfaceComp.triangleCount*3)
		}
	}

	protected def drawParticles() {
		if(showParticles) {
			gl.disable(gl.DEPTH_TEST)
			particlesShad.use
			camera.uniformMVP(particlesShad)
			particlesShad.uniform("pointSize", pointSize)
			particles.draw(particlesMesh.drawAs(gl))
			gl.enable(gl.DEPTH_TEST)
		}
	}
	
	protected def buildIsoSurface() {
		// We use the buckets to locate areas where there exist particles.
		// We put a given number of marching cubes inside one bucket, but
		// marching cubes never overlap a bucket boundary, their size is
		// a multiple of the bucket size.
		
		isoSurfaceComp = new IsoSurface(isoCellSize)

		isoSurfaceComp.autoComputeNormals(false)

		exploreSpaceHash
//		exploreSpaceCube
		
		// Now build the mesh

		if(isoSurfaceComp.triangleCount > 0) {
			
			// Insert the parallel vertex and normal arrays.
			
			isoSurfaceComp.foreachTrianglePoint{ (i, p) =>
				isoSurfaceMesh.setPoint(i, p)
				isoSurfaceMesh.setPointNormal(i, evalNormal(p))				
			}
			
			// Insert the triangles as indices into the vertex and normal arrays.
			
			isoSurfaceComp.foreachTriangle { (i, cube, triangle) =>
				if(i < maxDynTriangles) {
					isoSurfaceMesh.setTriangle(i, triangle.a, triangle.b, triangle.c)
				}
			}
			
			isoSurfaceMesh.updateVertexArray(gl, true, true, true, false)
		}
	}
	
	protected def exploreSpaceCube() {
		isoSurfaceComp.addCubesAt((-2.0*isoDiv).toInt, (-2*isoDiv).toInt, (-2*isoDiv).toInt, (4*isoDiv).toInt, (4*isoDiv).toInt, (4*isoDiv).toInt, eval, isoLimit)
	}

	protected def exploreSpaceHash() {
		
		var i = 0
		
		spaceHash.buckets.foreach { bucket =>
			val x = (bucket._1.x-1) * bucketSize
			val y = (bucket._1.y-1) * bucketSize
			val z = (bucket._1.z-1) * bucketSize
			
			val p = isoSurfaceComp.nearestCubePos(x, y, z)

			isoSurfaceComp.addCubesAt(
					p.x.toInt,        p.y.toInt,        p.z.toInt,
					(3*isoDiv).toInt, (3*isoDiv).toInt, (3*isoDiv).toInt,
					eval, isoLimit)
			i+=1
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
		
		particlesMesh.updateVertexArray(gl, true, true)
		
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

	/** Evaluate the meta-balls iso-surface normal at x. */
	def evalNormal(x:Point3):Vector3 = {
		val n = Vector3()
		
		balls.foreach { i =>
			val d = Vector3(i.from, x)
			val l = d.normalize
			d /= l*l
			n += d
		}
		
		n.normalize
		n
	}
	
	def reshape(surface:Surface) {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		camera.frustum(-camera.viewportRatio, camera.viewportRatio, -1, 1, 2)
	}
	
	protected def useLights(shader:ShaderProgram) {
		shader.uniform("light.pos", Vector3(camera.modelview.top * light1))
		shader.uniform("light.intensity", 15f)
		shader.uniform("light.ambient", 0.15f)
		shader.uniform("light.specular", 400f)
	}
}

class MetaBallsCameraController(camera:Camera, val mb:TestMetaBalls) extends BasicCameraController(camera) {
    override def key(surface:Surface, keyEvent:KeyEvent) {
        import keyEvent.ActionChar._
        if(keyEvent.isPrintable) {
        	keyEvent.unicodeChar match {
            	case ' ' => { mb.pausePlay }
            	case 'p' => { mb.showParticles = !mb.showParticles }
            	case 's' => { mb.showSpaceHash = !mb.showSpaceHash }
            	case 'c' => { mb.showIsoCubes = !mb.showIsoCubes }
            	case _ => super.key(surface, keyEvent)
            }
        } else {
            super.key(surface, keyEvent)
        }
    }
}