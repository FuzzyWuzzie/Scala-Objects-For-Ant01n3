package org.sofa.simu.test

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
import org.sofa.opengl.mesh.ColoredLineSet
import org.sofa.opengl.mesh.Axis
import org.sofa.math.SpatialPoint
import org.sofa.math.SpatialCube
import org.sofa.math.SpatialHash
import org.sofa.opengl.Texture
import org.sofa.math.IsoSurfaceSimple
import org.sofa.opengl.mesh.DynTriangleMesh
import org.sofa.opengl.surface.KeyEvent
import org.sofa.math.IsoSurface
import org.sofa.math.IsoContour
import org.sofa.opengl.mesh.DynIndexedTriangleMesh
import org.sofa.simu.ViscoElasticSimulation
import org.sofa.simu.Particle
import org.sofa.simu.QuadWall
import org.sofa.opengl.mesh.TriangleSet
import org.sofa.opengl.mesh.ColoredTriangleSet
import org.sofa.opengl.mesh.ColoredSurfaceTriangleSet
import org.sofa.Environment

object TestViscoElasticSimulation2D {
	def main(args:Array[String]) = (new TestViscoElasticSimulation2D).test
}

class TestViscoElasticSimulation2D {
	var camera = new Camera()
	var ctrl:BasicCameraController = null
	var surface:Surface = null
	var simu = new ViscoElasticSimulationViewer2D(camera)

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
		
		Environment.readConfigFile("/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/simu/test/config1.txt")
		Environment.initializeFieldsOf(Particle)
		Environment.initializeFieldsOf(simu)
		Environment.printParameters(Console.out)

		ctrl                = new TVESCameraController2D(camera, simu)
		simu.initSurface    = simu.initializeSurface
		simu.frame          = simu.display
		simu.surfaceChanged = simu.reshape
		simu.key            = ctrl.key
		simu.motion         = ctrl.motion
		simu.scroll         = ctrl.scroll
		simu.close          = { surface => sys.exit }
		surface             = new org.sofa.opengl.backend.SurfaceNewt(simu,
							camera, "P'tit jet !", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)	
	}
}

class ViscoElasticSimulationViewer2D(val camera:Camera) extends SurfaceRenderer {	

// Settings --------------------------------------------------

	/** max number of triangles for the iso-surface. */
	var maxDynTriangles = 3000

	/** max number of lines for the iso-contour. */
	var maxDynLines = 3000

	/** max number of springs to draw. */
	var maxSprings = 1000

	/** max number of particles in the simulation. */
	var size = 500

	var emitPoint = Point3(-5, 5, 0)
	var emitVelocity1 = Vector3(15, 6, 0)
	var emitVelocity2 = Vector3(15, 8, 0)
	
	/** Number of iso-cubes inside a space hash cube. */
	var isoDiv = 3.0
	
	var isoCellSize = Particle.spacialHashBucketSize / isoDiv
	
	/** Limit of the iso-surface in the implicit function used to define the surface. */
	var isoLimit = 0.65

	var drawParticlesFlag = true
	var drawSpringsFlag = false
	var drawSpaceHashFlag = false
	var drawIsoCubesFlag = false
	var drawIsoSurfaceFlag = false
	var drawIsoContourFlag = false
	var drawIsoSquaresFlag = false
	var drawIsoPlaneFlag = false
	
	var isoSurfaceColor = Rgba(1,1,1,0.9)
	var particleColor = Rgba(0.7, 0.7, 1, 0.3)
	var clearColor = Rgba.grey10
	var planeColor = Rgba.grey80
	val light1 = Vector4(0, 7, 3, 1)	
	var particleSizePx = 160f // 160f
	
	val timeToWait = 20
	val timeToEmit =  7
	var curParticle = 0
	var waiting = timeToEmit
	var emiting = true
	var velocityMode = true	

	def printConfig() {
		println("VES config:")
		println("  Visibility:")
		println("    draw particles ............ %b".format(drawParticlesFlag))
		println("    draw springs .............. %b".format(drawSpringsFlag))
		println("    draw space hash ........... %b".format(drawSpaceHashFlag))
		println("    draw iso cubes ............ %b".format(drawIsoCubesFlag))
		println("    draw iso contour .......... %b".format(drawIsoContourFlag))
		println("    draw iso squares .......... %b".format(drawIsoSquaresFlag))
		println("    draw iso plane ............ %b".format(drawIsoPlaneFlag))
		println("  Iso surface:")
		println("    isoDiv .................... %.4f".format(isoDiv))
		println("    isoCellSize ............... %.4f".format(isoCellSize))
		println("    isoLimit .................. %.4f".format(isoLimit))
	}

// Fields --------------------------------------------------

	var gl:SGL = null
	
	val projection:Matrix4 = new ArrayMatrix4
	val modelview = new MatrixStack(new ArrayMatrix4)
	
	var phongShad:ShaderProgram = null
	var particlesShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	
	var plane:VertexArray = null
	var axis:VertexArray = null
	var particles:VertexArray = null	
	var wcube:VertexArray = null
	var wcube2:VertexArray = null
	var wcube3:VertexArray = null
	var isoSurface:VertexArray = null
	var isoPlane:VertexArray = null
	var isoContour:VertexArray = null
	var obstacles:VertexArray = null
	var springs:VertexArray = null
	
	var axisMesh = new Axis(10)
	var planeMesh = new Plane(2, 2, 10, 10)
	var particlesMesh:DynPointsMesh = null
	var wcubeMesh:WireCube = null
	var wcubeMesh2:WireCube = null
	var wcubeMesh3:WireCube = null
	var isoSurfaceMesh = new DynIndexedTriangleMesh(maxDynTriangles)
	var isoPlaneMesh = new DynIndexedTriangleMesh(maxDynTriangles)
	var isoContourMesh = new ColoredLineSet(maxDynLines)
	var obstaclesMesh = new ColoredSurfaceTriangleSet(4)
	var springsMesh = new ColoredLineSet(maxSprings)
		
	val random = new scala.util.Random()
	var step = 0
	var running = true
	val simu = new ViscoElasticSimulation 
	var isoSurfaceComp:IsoSurface = null
	var isoContourComp:IsoContour = null

	// ----------------------------------------------------------------------------------
	// Commands

	def pausePlay() { running = ! running }

	// ----------------------------------------------------------------------------------
	// Init.
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.includePath += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
		Shader.includePath += "src/com/chouquette/tests"
			
		initSimuParams
		initGL(sgl)
		initShaders
		initGeometry
		
		camera.viewCartesian(0, 5, 14)
		camera.setFocus(0, 4, 0)
		reshape(surface)
	}

	def initSimuParams() {
		// Adapt parameters dependent of others, and eventually changed by Environment.
		isoCellSize = Particle.spacialHashBucketSize / isoDiv

		printConfig
		Particle.printConfig
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
		//gl.enable(gl.PROGRAM_POINT_SIZE)	// Necessary on my ES2 implementation ?? 
		gl.checkErrors
	}
	
	protected def initShaders() {
		phongShad = ShaderProgram(gl, "phong shader", "es2/phonghi.vert.glsl", "es2/phonghi.frag.glsl")
		particlesShad = ShaderProgram(gl, "particles shader", "es2/particles.vert.glsl", "es2/particles.frag.glsl")
		plainShad = ShaderProgram(gl, "plain shader", "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
	}
	
	protected def initGeometry() {
		initParticles		
		initSimu

		// Phong shader
		
		var v = phongShad.getAttribLocation("position")
		var c = phongShad.getAttribLocation("color")
		var n = phongShad.getAttribLocation("normal")
		
		planeMesh.setColor(Rgba.red)
		
		plane      = planeMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
		isoSurface = isoSurfaceMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
		isoPlane   = isoPlaneMesh.newVertexArray(gl, ("vertices",v), ("colors", c), ("normals", n))
		obstacles  = obstaclesMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
		
		// Plain shader
		
		v = plainShad.getAttribLocation("position")
		c = plainShad.getAttribLocation("color")
		
		wcubeMesh = new WireCube(simu.spaceHash.bucketSize.toFloat)
		wcubeMesh2 = new WireCube(isoCellSize.toFloat)
		wcubeMesh3 = new WireCube(isoCellSize.toFloat)
		wcubeMesh.setColor(Rgba(1, 1, 1, 0.5))
		wcubeMesh2.setColor(Rgba(1, 0, 0, 0.2))
		wcubeMesh3.setColor(Rgba(0, 1, 0, 0.2))
		wcube = wcubeMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
		wcube2 = wcubeMesh2.newVertexArray(gl, ("vertices", v), ("colors", c))
		wcube3 = wcubeMesh3.newVertexArray(gl, ("vertices", v), ("colors", c))
		
		axis = axisMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
		springs = springsMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
		isoContour = isoContourMesh.newVertexArray(gl, ("vertices", v), ("colors", c))

		// Particles shader
		
		v = particlesShad.getAttribLocation("position")
		c = particlesShad.getAttribLocation("color") 
		
		particles = particlesMesh.newVertexArray(gl, gl.STATIC_DRAW, ("vertices", v), ("colors", c))

		for(i <- 0 until maxDynTriangles*3) {
			isoSurfaceMesh.setPointColor(i, isoSurfaceColor)
			isoPlaneMesh.setPointColor(i, isoSurfaceColor)
			isoPlaneMesh.setPointNormal(i, 0, 0, 1)
		}

		var springColor = Rgba(1, 0, 0, 0.3)
		for(i <- 0 until maxSprings) {
			springsMesh.setColor(i, springColor)
		}
	}
	
	protected def initParticles {
		particlesMesh = new DynPointsMesh(size) 
		
		for(i <- 0 until size) {
			particlesMesh.setPoint(i, 0, 200, 0) // make them invisible.
			particlesMesh.setColor(i, particleColor)
		}
	}
	
	protected def initSimu() {
		addObstacle(0, new QuadWall(Point3(2,5,-1), Vector3(1,2,0), Vector3(0,0,2)))
		addObstacle(2, new QuadWall(Point3(4,2,-1), Vector3(2,2,0), Vector3(0,0,2)))
	}
	
	protected def addObstacle(i:Int, wall:QuadWall) {
		obstaclesMesh.setTriangle(i, wall.tri1)
		obstaclesMesh.setNormal(i, wall.tri1.normal)
		obstaclesMesh.setColor(i, Rgba.blue)

		obstaclesMesh.setTriangle(i+1, wall.tri2)
		obstaclesMesh.setNormal(i+1, wall.tri2.normal)
		obstaclesMesh.setColor(i+1, Rgba.blue)
		
		simu.addObstacle(wall)
	}
	
	// ----------------------------------------------------------------------------------
	// Rendering
	
	def display(surface:Surface) {
		if(running) {
			launchSomeParticles
			updateParticles
		}
		
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		
		camera.setupView

		drawPlane
		drawObstacles
		drawIsoSurface
		drawAxis
		drawSpaceHash
		drawIsoCubes
		drawIsoSquares
		drawSprings
		drawParticles
		drawIsoPlane
		drawIsoContour
		
		surface.swapBuffers
		gl.checkErrors
		
		if(running) {
			removeSomeParticles
		}
		
		step += 1
	}
	
	protected def launchSomeParticles() {
		waiting -= 1
		if(waiting <= 0) {
			emiting = ! emiting
			if(emiting)
				waiting = timeToEmit
			else waiting = timeToWait
			if(emiting) {
				velocityMode = ! velocityMode
			}
		} 
		if(emiting) {
			if(simu.size < size) {
				val emitVelocity = if(velocityMode) emitVelocity1 else emitVelocity2
				val p = simu.addParticle(emitPoint.x, emitPoint.y, emitPoint.z,
						emitVelocity.x+math.random*0.1, emitVelocity.y+math.random*0.1, emitVelocity.z)//+math.random*0.05)
			//	particlesMesh.setPoint(curParticle, p.x)
			//	particlesMesh.setColor(curParticle, particleColor)
				curParticle += 1
			}
		}
	}	
	
	protected def removeSomeParticles() {
		if(!emiting && simu.size > 100) {
			var p = simu.randomParticle(random)
			var i = 0
			
			while(p.x.y > 0.2 && i < 10) {
				p = simu.randomParticle(random)
				i += 1
			}
			
			if(p.x.y <= 0.2) {
//				Console.err.println("removing particle %d".format(p.index))
				simu.removeParticle(p)
			}
		}
	}

	protected def drawPlane() {
		phongShad.use
		useLights(phongShad)
		camera.uniformMVP(phongShad)
		plane.draw(planeMesh.drawAs)
	}
	
	protected def drawObstacles() {
		gl.disable(gl.CULL_FACE)
		phongShad.use
		useLights(phongShad)
		camera.uniformMVP(phongShad)
		obstacles.draw(obstaclesMesh.drawAs)
		gl.enable(gl.CULL_FACE)
	}
	
	protected def drawAxis() {
		gl.enable(gl.BLEND)
		plainShad.use
		camera.setUniformMVP(plainShad)
		axis.draw(axisMesh.drawAs)
		gl.disable(gl.BLEND)
	}
	
	protected def drawParticles() {
		if(drawParticlesFlag) {
			gl.enable(gl.BLEND)
			particlesShad.use
			camera.setUniformMVP(particlesShad)
			particlesShad.uniform("pointSize", particleSizePx)
			particles.draw(particlesMesh.drawAs, simu.size)
			gl.disable(gl.BLEND)
		}
	}

	protected def drawSprings() {
		if(drawSpringsFlag) {
			gl.enable(gl.BLEND)
			plainShad.use
			camera.setUniformMVP(plainShad)
			springs.draw(springsMesh.drawAs, simu.springs.size*2)
			gl.disable(gl.BLEND)
		}
	}
	
	protected def drawSpaceHash() {
		if(drawSpaceHashFlag) {
			gl.enable(gl.BLEND)
			plainShad.use
			val cs = simu.spaceHash.bucketSize
			val cs2 = cs/2
			simu.spaceHash.buckets.foreach { bucket =>
				//if(bucket._2.points > 0) {
				camera.pushpop {
					val p = bucket._2.position
					camera.translateModel((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.setUniformMVP(plainShad)
					wcube.draw(wcubeMesh.drawAs)
				}
				//}
			}
			gl.disable(gl.BLEND)
		}
	}

	protected def drawIsoCubes() {
		if(drawIsoCubesFlag) {
			gl.enable(gl.BLEND)
			plainShad.use
			var cs = isoSurfaceComp.cellSize
			var cs2 = cs/2
			isoSurfaceComp.cubes.foreach { cube=>
				if(!cube.isEmpty) camera.pushpop {
					val p = cube.pos
					camera.translateModel((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.setUniformMVP(plainShad)
					wcube2.draw(wcubeMesh.drawAs)
				}
			}
			gl.disable(gl.BLEND)
		}
	}

	protected def drawIsoSquares() {
		if(drawIsoSquaresFlag) {
			var empty = 0
			gl.enable(gl.BLEND)
			plainShad.use
			var cs = isoContourComp.cellSize
			var cs2 = cs/2
			isoContourComp.squares.foreach { square =>
				if(! square.isEmpty) camera.pushpop {
					val p = square.pos
					camera.translateModel((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.setUniformMVP(plainShad)
					wcube3.draw(wcubeMesh.drawAs)
				}
			}
			gl.disable(gl.BLEND)
		}
	}
	
	protected def drawIsoSurface() {
		if(drawIsoSurfaceFlag) {
			if((isoSurfaceComp ne null) && isoSurfaceComp.triangleCount > 0) {
				gl.enable(gl.BLEND)
				phongShad.use
				useLights(phongShad)
				camera.uniformMVP(phongShad)
				isoSurface.draw(isoSurfaceMesh.drawAs, isoSurfaceComp.triangleCount*3)
				gl.disable(gl.BLEND)
			}
		}
	}

	protected def drawIsoPlane() {
		if(drawIsoPlaneFlag && isoContourComp.triangleCount > 0) {
//			gl.enable(gl.BLEND)
//Console.err.println("Drawing iso plane %d triangles".format(isoContourComp.triangleCount))
			phongShad.use
			useLights(phongShad)
			camera.uniformMVP(phongShad)
//			isoPlane.draw(isoPlaneMesh.drawAs, isoContourComp.triangleCount*3)
			isoPlane.draw(isoPlaneMesh.drawAs, triangleCount*3)
//			gl.disable(gl.BLEND)
		}
	}

	protected def drawIsoContour() {
		if(drawIsoContourFlag && isoContourComp.segmentCount > 0) {
			gl.enable(gl.BLEND)
			plainShad.use
			camera.setUniformMVP(plainShad)
			//springs.draw(springsMesh.drawAs, simu.springs.size*2)
			isoContour.draw(isoContourMesh.drawAs, isoContourComp.segmentCount*2)
			gl.disable(gl.BLEND)
		}
	}

	// --------------------------------------------------------------------------------
	// Simulation updating.

	protected def updateParticles() {
		simu.simulationStep(0.044)
		
		if(drawParticlesFlag) {
			var i = 0
			simu.foreach { particle => 
				particlesMesh.setPoint(i, particle.x)
				i += 1
			}
		
			particlesMesh.updateVertexArray(gl, "vertices", "colors")
			gl.checkErrors
		}

		if(drawSpringsFlag) {
			var i = 0
			simu.springs.foreach { spring =>
				springsMesh.setLine(i, spring.i.x, spring.j.x)
				//springsMesh.setColor(i, Rgba.red)
				i += 1
			}

			springsMesh.updateVertexArray(gl, "vertices", "colors")
			gl.checkErrors
		}
		
		if(drawIsoSurfaceFlag)
			buildIsoSurface

		if(drawIsoContourFlag || drawIsoPlaneFlag)
			buildIsoContour
	}
	
	protected def buildIsoSurface() {
		isoSurfaceComp = new IsoSurface(isoCellSize)
		isoSurfaceComp.autoComputeNormals(false)
		exploreSpaceHashForIsoSurface
		updateIsoSurfaceRepresentation
	}

	protected def buildIsoContour() {
		isoContourComp = new IsoContour(isoCellSize)
		isoContourComp.computeSurface(true)
		exploreSpaceHashForIsoContour
		updateIsoContourRepresentation
		updateIsoPlaneRepresentation
	}
	
	protected def updateIsoSurfaceRepresentation() {
		if(isoSurfaceComp.triangleCount > 0) {
			isoSurfaceComp.foreachTrianglePoint { (i, p) =>
				isoSurfaceMesh.setPoint(i, p)
				isoSurfaceMesh.setPointNormal(i, simu.evalIsoNormal(p))				
			}
			
			isoSurfaceComp.foreachTriangle { (i, cube, triangle) =>
				if(i < maxDynTriangles) {
					isoSurfaceMesh.setTriangle(i, triangle.a, triangle.b, triangle.c)
				}				
			}
			
			isoSurfaceMesh.updateVertexArray(gl, "vertices", "colors", "normals")
		}
	}

	protected def updateIsoContourRepresentation() {
		if(isoContourComp.segmentCount > 0) {
			isoContourComp.foreachSegment { (i, square, segment) =>
				if(i < maxDynLines) {
					val p0 = isoContourComp.segPoints(segment.a)
					val p1 = isoContourComp.segPoints(segment.b)
					isoContourMesh.setLine(i, Point3(p0.x,p0.y,0), Point3(p1.x,p1.y,0))
					isoContourMesh.setColor(i, Rgba.white)
				}
			}

			isoContourMesh.updateVertexArray(gl, "vertices", "colors")
		}
	}

	var triangleCount = 0
	var densityMax = 0.0
	var avgDensity = 0.0

	protected def updateIsoPlaneRepresentation() {
		if(isoContourComp.triangleCount > 0 && drawIsoPlaneFlag) {
			var i = 0
			avgDensity = 0.0
			
			isoContourComp.segPoints.foreach { p =>
				val density = simu.evalIsoDensity(p)
				avgDensity += density
				if(density > densityMax) densityMax = density
				val d = math.min(1,(density/120))

				isoPlaneMesh.setPoint(i, p.x.toFloat, p.y.toFloat, 0)//d.toFloat)
//				isoPlaneMesh.setPointNormal(i, 0, 1, 0)
				isoPlaneMesh.setPointColor(i, Rgba(d, 1, 1, 1))
				i += 1
			}

			val segCount = i
			
			isoContourComp.points.foreach { p => 
				val density = simu.evalIsoDensity(p)
				avgDensity += density
				if(density > densityMax) densityMax = density
				val d = math.min(1,(density/120))

				isoPlaneMesh.setPoint(i, p.x.toFloat, p.y.toFloat, 0)//d.toFloat)
//				isoPlaneMesh.setPointNormal(i, 0, 0, 1)
				isoPlaneMesh.setPointColor(i, Rgba(d, 1, 1, 1))
				i += 1
			}
avgDensity/=i
//Console.err.println("avg density = %.4f (count=%d) max=%.4f".format(avgDensity, i, densityMax))
			val ptCount = i

//			createNormals(segCount, ptCount)

			i = 0

triangleCount = 0
			isoContourComp.nonEmptySquares.foreach { square =>
				square.triangles.foreach { triangle =>
					var a = if(triangle.a < 0) ((-triangle.a)-1)+segCount else triangle.a
					var b = if(triangle.b < 0) ((-triangle.b)-1)+segCount else triangle.b
					var c = if(triangle.c < 0) ((-triangle.c)-1)+segCount else triangle.c
					isoPlaneMesh.setTriangle(i, a, b, c)
					i += 1
					triangleCount += 1
				}
			}

//			assert(i == isoContourComp.triangleCount)

			isoPlaneMesh.updateVertexArray(gl, "vertices", "colors", "normals")
		}
	}

	class Normal() {
		val normal = Vector3(0,0,1)
		var count = 0
	}

	protected def createNormals(segCount:Int, ptCount:Int) {
		val normals = 

		isoContourComp.nonEmptySquares.foreach { square =>
			if(square.hasSegments) {
				// Pour tous les segments
				//  - pour les points du segment (x,y,0)
				//     - ajouter la normale au segment (-y,x,0)
				//     - incrémenter le normal count des points
				// Pour tous les triangles,
				//  - pour les points qui ne sont pas sur des segments:
				//     - ajouter la normale du segment (-y,x,0.5)
				//     - incrémenter le normal count du point.
				//     XXX comment connaître les segments correspondants aux triangles.
			} else if(square.hasTriangles) {
				// pour tous les triangles retrouver les points.
				// incrémenter leur normal count et ajouter (0,0,1) à la normale.
			}
		}
	}
	
	protected def exploreSpaceHashForIsoSurface() {
		simu.spaceHash.buckets.foreach { bucket =>
			if(bucket._2.points > 0) {
				val x = (bucket._1.x-1) * simu.spaceHash.bucketSize
				val y = (bucket._1.y-1) * simu.spaceHash.bucketSize
				val z = (bucket._1.z-1) * simu.spaceHash.bucketSize
			
				val p = isoSurfaceComp.nearestCubePos(x, y, z)

				isoSurfaceComp.addCubesAt(
					p.x.toInt,        p.y.toInt,        p.z.toInt,
					(3*isoDiv).toInt, (3*isoDiv).toInt, (3*isoDiv).toInt,
					simu.evalIsoSurface, isoLimit)
			}
		}
	}

	protected def exploreSpaceHashForIsoContour() {
		simu.spaceHash.buckets.foreach { bucket =>
			if(bucket._2.points > 0) {
				// Only long the XY plane
				if(bucket._1.z == 0) {
					val x = (bucket._1.x-1) * simu.spaceHash.bucketSize
					val y = (bucket._1.y-1) * simu.spaceHash.bucketSize
					val p = isoContourComp.nearestSquarePos(x, y)

					isoContourComp.addSquaresAt(
						p.x.toInt,        p.y.toInt,
						(3*isoDiv).toInt, (3*isoDiv).toInt,
						simu.evalIsoSurface, isoLimit)
				}				
			}
		}
	}

	// --------------------------------------------------------------------------------------
	// Utility

	def reshape(surface:Surface) {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		camera.frustum(-camera.viewportRatio, camera.viewportRatio, -1, 1, 2)
	}
	
	protected def useLights(shader:ShaderProgram) {
		shader.uniform("light.pos", Vector3(camera.modelview.top * light1))
		shader.uniform("light.intensity", 10f)
		shader.uniform("light.ambient", 0.2f)
		shader.uniform("light.specular", 1000f)
	}
}

/** A simple mouse/key controller for the camera and simulation. */
class TVESCameraController2D(camera:Camera, val ves:ViscoElasticSimulationViewer2D) extends BasicCameraController(camera) {
    override def key(surface:Surface, keyEvent:KeyEvent) {
        import keyEvent.ActionChar._
        if(keyEvent.isPrintable) {
        	Console.err.println("KEY=%c".format(keyEvent.unicodeChar))
        	keyEvent.unicodeChar match {
            	case ' ' => { ves.pausePlay }
            	case 'p' => { ves.drawParticlesFlag = !ves.drawParticlesFlag }
            	case 'P' => { ves.drawIsoPlaneFlag = !ves.drawIsoPlaneFlag }
            	case 'h' => { ves.drawSpaceHashFlag = !ves.drawSpaceHashFlag }
            	case 'c' => { ves.drawIsoCubesFlag = !ves.drawIsoCubesFlag }
            	case 's' => { ves.drawIsoSurfaceFlag = !ves.drawIsoSurfaceFlag }
            	case 'O' => { ves.drawIsoContourFlag = !ves.drawIsoContourFlag }
            	case 'S' => { ves.drawIsoSquaresFlag = !ves.drawIsoSquaresFlag }
            	case 'i' => { ves.drawSpringsFlag = !ves.drawSpringsFlag }
            	case 'q' => { sys.exit(0) }
            	case 'H' => {
            		println("Keys:")
            		println("    <space>  pause/play the simulation.")
            		println("    p        toggle draw particles.")
            		println("    P        toggle draw the iso plane.")
            		println("    h        toggle draw the space hash.")
            		println("    c        toggle draw the iso surface cubes.")
            		println("    s        toggle draw the iso surface.")
            		println("    O        toggle draw the iso contour.")
            		println("    S        toogle draw the iso squares.")
            		println("    i        toogle draw the springs.")
            		println("    q        quit (no questions, it quits!).")
            	}
            	case _ => { super.key(surface, keyEvent) }
            }
        } else {
        	//Console.err.println("KEYCODE=%d".format(keyEvent.key))
            super.key(surface, keyEvent)
        }
    }
}