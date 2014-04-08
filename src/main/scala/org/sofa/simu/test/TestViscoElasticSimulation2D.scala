package org.sofa.simu.test

import org.sofa.{Timer, Environment}
import org.sofa.simu.{ViscoElasticSimulation, Particle, QuadWall}
import org.sofa.math.{Rgba, Matrix4, Vector4, Vector3, Point3, IsoSurface, IsoSurfaceSimple, IsoContour}
import org.sofa.collection.{SpatialPoint, SpatialCube, SpatialHash}

import org.sofa.opengl.{SGL, MatrixStack, Shader, ShaderProgram, VertexArray, Camera, Texture, TexParams, TexMipMap}
import org.sofa.opengl.mesh.{PlaneMesh, CubeMesh, PointsMesh, WireCubeMesh, LinesMesh, AxisMesh, TrianglesMesh, CylinderMesh, UnindexedTrianglesMesh, VertexAttribute}
import org.sofa.opengl.surface.{SurfaceRenderer, BasicCameraController, Surface, KeyEvent}

import javax.media.opengl.{GLProfile, GLCapabilities}

import scala.collection.mutable.ArrayBuffer

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
	var maxSprings = 2000

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
	var drawParticlesQuadFlag = true
	var particlesToRemovePerStep = 10
	
	var isoSurfaceColor = Rgba(1,1,1,0.9)
	var particleColor = Rgba(0.7, 0.7, 1, 0.9)
	var clearColor = Rgba.Grey10
	var planeColor = Rgba.Grey80
	val light1 = Vector4(0, 7, 3, 1)	
	var particleSizePx = 30f // 160f
	
	var particleQuadSize = 0.8f
	var birouteSize = 2f
	var timeToWait = 20
	var timeToEmit =  7
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
		println("  Simu:")
		println("    particles to remove ....... %d".format(particlesToRemovePerStep))
	}

// Fields --------------------------------------------------

	var gl:SGL = null
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	var phongShad:ShaderProgram = null
	var particlesShad:ShaderProgram = null
	var particlesQuadShad:ShaderProgram = null
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
	var quad:VertexArray = null
	var biroute:VertexArray = null

	var axisMesh = new AxisMesh(10)
	var planeMesh = new PlaneMesh(2, 2, 10, 10)
	var particlesMesh:PointsMesh = null
	var wcubeMesh:WireCubeMesh = null
	var wcubeMesh2:WireCubeMesh = null
	var wcubeMesh3:WireCubeMesh = null
	var isoSurfaceMesh = new TrianglesMesh(maxDynTriangles)
	var isoPlaneMesh = new TrianglesMesh(maxDynTriangles)
	var isoContourMesh = new LinesMesh(maxDynLines)
	var obstaclesMesh = new UnindexedTrianglesMesh(4)
	var springsMesh = new LinesMesh(maxSprings)
	var quadMesh = new PlaneMesh(2, 2, particleQuadSize, particleQuadSize, true)
	var birouteMesh = new CylinderMesh(birouteSize*0.25f, birouteSize, 8, 1)

	val random = new scala.util.Random()
	var step = 0
	var running = true
	val simu = new ViscoElasticSimulation 
	var isoSurfaceComp:IsoSurface = null
	var isoContourComp:IsoContour = null

	var pointTex:Texture = null

	val timer = new Timer(Console.out)

	// ----------------------------------------------------------------------------------
	// Commands

	def pausePlay() { running = ! running }

	// ----------------------------------------------------------------------------------
	// Init.
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.path += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
		Shader.path += "src/com/chouquette/tests"
		Texture.path += "/Users/antoine/Documents/Programs/SOFA/textures"
			
		initSimuParams
		initGL(sgl)
		initTextures
		initShaders
		initGeometry

		camera.eyeCartesian(0, 5, 14)
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

	protected def initTextures() {
		pointTex = new Texture(gl, "Point.png", TexParams(mipMap=TexMipMap.Generate))
	    pointTex.minMagFilter(gl.LINEAR, gl.LINEAR)
	    pointTex.wrap(gl.REPEAT)
	}
	
	protected def initShaders() {
		phongShad = ShaderProgram(gl, "phong shader", "es2/phonghi.vert.glsl", "es2/phonghi.frag.glsl")
		plainShad = ShaderProgram(gl, "plain shader", "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
//		particlesShad = ShaderProgram(gl, "particles shader", "es2/particlesTex.vert.glsl", "es2/particlesTex.frag.glsl")
		particlesShad = ShaderProgram(gl, "particles shader", "es2/particles.vert.glsl", "es2/particles.frag.glsl")
		particlesQuadShad = ShaderProgram(gl, "particles quad shader", "es2/particlesTex.vert.glsl", "es2/particlesTex.frag.glsl")
	}
	
	protected def initGeometry() {
		import VertexAttribute._

		initParticles		
		initSimu

		planeMesh.setColor(Rgba.Red)
		wcubeMesh = new WireCubeMesh(simu.spaceHash.bucketSize.toFloat)
		wcubeMesh2 = new WireCubeMesh(isoCellSize.toFloat)
		wcubeMesh3 = new WireCubeMesh(isoCellSize.toFloat)
		wcubeMesh.setColor(Rgba(1, 1, 1, 0.5))
		wcubeMesh2.setColor(Rgba(1, 0, 0, 0.2))
		wcubeMesh3.setColor(Rgba(0, 1, 0, 0.2))
		quadMesh.setTextureRepeat(1, 1)

		// Phong shader
		
		plane      = planeMesh.newVertexArray(     gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		isoSurface = isoSurfaceMesh.newVertexArray(gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		isoPlane   = isoPlaneMesh.newVertexArray(  gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		obstacles  = obstaclesMesh.newVertexArray( gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		biroute    = birouteMesh.newVertexArray(   gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		
		// Plain shader
		
		wcube      = wcubeMesh.newVertexArray(     gl, plainShad, Vertex -> "position", Color -> "color")
		wcube2     = wcubeMesh2.newVertexArray(    gl, plainShad, Vertex -> "position", Color -> "color")
		wcube3     = wcubeMesh3.newVertexArray(    gl, plainShad, Vertex -> "position", Color -> "color")
		axis       = axisMesh.newVertexArray(      gl, plainShad, Vertex -> "position", Color -> "color")
		springs    = springsMesh.newVertexArray(   gl, plainShad, Vertex -> "position", Color -> "color")
		isoContour = isoContourMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")

		// Particles shader
				
		particles = particlesMesh.newVertexArray(gl, gl.STATIC_DRAW, particlesShad, Vertex -> "position", Color -> "color")

		quad = quadMesh.newVertexArray(gl, gl.STATIC_DRAW, particlesQuadShad, Vertex -> "position", TexCoord -> "texCoords")

		for(i <- 0 until maxDynTriangles*3) {
			isoSurfaceMesh.setPointColor(i, isoSurfaceColor)
			isoPlaneMesh.setPointColor(i, isoSurfaceColor)
			isoPlaneMesh.setPointNormal(i, 0, 0, 1)
		}

		var springColor = Rgba(1, 1, 1, 0.8)
		for(i <- 0 until maxSprings) {
			springsMesh.setColor(i, springColor)
		}
	}
	
	protected def initParticles {
		particlesMesh = new PointsMesh(size) 
		
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
		obstaclesMesh.setTriangle(i, wall.tri0)
		obstaclesMesh.setNormal(i, wall.tri0.normal)
		obstaclesMesh.setColor(i, Rgba.Blue)

		obstaclesMesh.setTriangle(i+1, wall.tri1)
		obstaclesMesh.setNormal(i+1, wall.tri1.normal)
		obstaclesMesh.setColor(i+1, Rgba.Blue)
		
		simu.addObstacle(wall)
	}
	
	// ----------------------------------------------------------------------------------
	// Rendering
	
	def display(surface:Surface) {
		if((step+1)%10 == 0)
			println("--step %d ---------------------".format(step+1))
		if(running) {
			launchSomeParticles
			updateParticles
		}
		
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		
		camera.lookAt

		drawPlane
		drawObstacles
		drawIsoSurface
		drawAxis
		drawSpaceHash
		drawIsoCubes
		drawIsoSquares
		drawSprings
		drawParticles
		drawBiroute
		drawIsoPlane
		drawParticlesQuads
		drawIsoContour
		
		surface.swapBuffers
		gl.checkErrors
		
		if(running) {
			removeSomeParticles
		}
		
		step += 1

		if(step % 10 == 0) {
			timer.printAvgs("-- Iso ------")
		}
		if(step % 1000 == 0) {
			timer.reset
		}
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
		if(!emiting && simu.size > 10) {
			var p = simu.randomParticle(random)
			var i = 0
			
			while(p.x.y > 0.2 && i < particlesToRemovePerStep) {
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
		camera.uniform(phongShad)
		plane.draw(planeMesh.drawAs(gl))
	}

	protected def drawBiroute() {
		phongShad.use
		useLights(phongShad)
		val angle = Vector3(0,1,0).angle(emitVelocity1)

		camera.pushpop {
			camera.translate(emitPoint)
			camera.translate(0, -birouteSize, 0)
			camera.rotate(-angle, 0, 0, 1)
			camera.uniform(phongShad)
			biroute.draw(birouteMesh.drawAs(gl))
		}
	}
	
	protected def drawObstacles() {
		gl.disable(gl.CULL_FACE)
		phongShad.use
		useLights(phongShad)
		camera.uniform(phongShad)
		obstacles.draw(obstaclesMesh.drawAs(gl))
		gl.enable(gl.CULL_FACE)
	}
	
	protected def drawAxis() {
		gl.enable(gl.BLEND)
		plainShad.use
		camera.uniformMVP(plainShad)
		axis.draw(axisMesh.drawAs(gl))
		gl.disable(gl.BLEND)
	}
	
	protected def drawParticles() {
		if(drawParticlesFlag) {
			gl.enable(gl.BLEND)
//			gl.enable(gl.TEXTURE_2D)
			particlesShad.use
//			pointTex.bindTo(gl.TEXTURE0)
//	    	particlesShad.uniform("texColor", 0)	// Texture Unit 0
			camera.uniformMVP(particlesShad)
			particlesShad.uniform("pointSize", particleSizePx)
			particles.draw(particlesMesh.drawAs(gl), simu.size)
			gl.disable(gl.BLEND)
		}
	}

	protected def drawParticlesQuads() {
		if(drawParticlesQuadFlag) {
			gl.enable(gl.BLEND)
			gl.disable(gl.DEPTH_TEST)
			particlesQuadShad.use
			pointTex.bindTo(gl.TEXTURE0)
	    	particlesQuadShad.uniform("texColor", 0)	// Texture Unit 0
timer.measure("draw quads") {
			var I = 0
			val N = simu.size
			while(I < N) {
				val particle = simu(I)
				camera.push
				camera.translate(particle.x.x, particle.x.y, particle.x.z)
// 				camera.rotate(math.Pi/2, 1, 0, 0)
				camera.uniformMVP(particlesQuadShad)
				quad.draw(quadMesh.drawAs(gl))
				camera.pop
				I += 1
			}
}
			gl.enable(gl.DEPTH_TEST)
			gl.disable(gl.BLEND)
		}
	}

	protected def drawSprings() {
		if(drawSpringsFlag) {
			gl.disable(gl.DEPTH_TEST)
			gl.enable(gl.BLEND)
			gl.lineWidth(2)
			plainShad.use
			camera.uniformMVP(plainShad)
			springs.draw(springsMesh.drawAs(gl), simu.springs.size*2)
			gl.lineWidth(1)
			gl.disable(gl.BLEND)
			gl.enable(gl.DEPTH_TEST)
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
					camera.translate((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.uniformMVP(plainShad)
					wcube.draw(wcubeMesh.drawAs(gl))
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
					camera.translate((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.uniformMVP(plainShad)
					wcube2.draw(wcubeMesh.drawAs(gl))
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
					camera.translate((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.uniformMVP(plainShad)
					wcube3.draw(wcubeMesh.drawAs(gl))
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
				camera.uniform(phongShad)
				isoSurface.draw(isoSurfaceMesh.drawAs(gl), isoSurfaceComp.triangleCount*3)
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
			camera.uniform(phongShad)
//			isoPlane.draw(isoPlaneMesh.drawAs, isoContourComp.triangleCount*3)
			isoPlane.draw(isoPlaneMesh.drawAs(gl), triangleCount*3)
//			gl.disable(gl.BLEND)
		}
	}

	protected def drawIsoContour() {
		if(drawIsoContourFlag && isoContourComp.segmentCount > 0) {
			gl.enable(gl.BLEND)
			plainShad.use
			camera.uniformMVP(plainShad)
			//springs.draw(springsMesh.drawAs, simu.springs.size*2)
			isoContour.draw(isoContourMesh.drawAs(gl), isoContourComp.segmentCount*2)
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
		
			particlesMesh.updateVertexArray(gl, true, true)
			gl.checkErrors
		}

		if(drawSpringsFlag) {
			var i = 0
			simu.springs.foreach { spring =>
				springsMesh.setLine(i, spring.i.x, spring.j.x)
				//springsMesh.setColor(i, Rgba.red)
				i += 1
			}

			springsMesh.updateVertexArray(gl, true, true)
			gl.checkErrors
		}
		
		if(drawIsoSurfaceFlag || drawIsoCubesFlag)
			buildIsoSurface

		if(drawIsoContourFlag || drawIsoPlaneFlag || drawIsoSquaresFlag)
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
		timer.measure("build Iso Contour") {
			exploreSpaceHashForIsoContour
		}
		timer.measure("update Iso Repr") {
			updateIsoContourRepresentation
			updateIsoPlaneRepresentation
		}
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
			
			isoSurfaceMesh.updateVertexArray(gl, true, true, true, false)
		}
	}

	protected def updateIsoContourRepresentation() {
		if(isoContourComp.segmentCount > 0) {
			isoContourComp.foreachSegment { (i, square, segment) =>
				if(i < maxDynLines) {
					val p0 = isoContourComp.segPoints(segment.a)
					val p1 = isoContourComp.segPoints(segment.b)
					isoContourMesh.setLine(i, Point3(p0.x,p0.y,0), Point3(p1.x,p1.y,0))
					isoContourMesh.setColor(i, Rgba.White)
				}
			}

			isoContourMesh.updateVertexArray(gl, true, true)
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

			isoPlaneMesh.updateVertexArray(gl, true, true, true, false)
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
			if((bucket._2.points ne null) && bucket._2.points.size > 0) {
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
//simu.evalCount = 0
//var bucketCount = 0
		simu.spaceHash.buckets.foreach { bucket =>
			if((bucket._2.points ne null) && bucket._2.points.size > 0) {
				// Only long the XY plane
				if(bucket._1.z == 0) {
//bucketCount += 1
					val x = (bucket._1.x-1) * simu.spaceHash.bucketSize
					val y = (bucket._1.y-1) * simu.spaceHash.bucketSize
					val p = isoContourComp.nearestSquarePos(x, y)

					isoContourComp.addSquaresAt(
						p.x.toInt,        p.y.toInt,
						(3*isoDiv).toInt, (3*isoDiv).toInt,
						simu.evalIsoSurface, isoLimit)
					// val x = (bucket._1.x * simu.spaceHash.bucketSize) - (simu.spaceHash.bucketSize*2)
					// val y = (bucket._1.y * simu.spaceHash.bucketSize) - (simu.spaceHash.bucketSize*2)
					// val p = isoContourComp.nearestSquarePos(x, y)

					// isoContourComp.addSquaresAt(
					// 	p.x.toInt,        p.y.toInt,
					// 	(isoDiv+4).toInt, (isoDiv+4).toInt,
					// 	simu.evalIsoSurface, isoLimit)
				}				
			}
		}
//println("## %d (%.1f) buckets %d evals".format(bucketCount, bucketCount*isoDiv*isoDiv*4, simu.evalCount))
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