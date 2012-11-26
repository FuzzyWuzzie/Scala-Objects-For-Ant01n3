package org.sofa.simu.test

import org.sofa.{Timer, Environment}
import org.sofa.simu.{ViscoElasticSimulation, Particle, QuadWall}
import org.sofa.math.{Rgba, Matrix4, Vector4, Vector3, Vector2, Point3, SpatialPoint, SpatialCube, SpatialHash, IsoSurface, IsoSurfaceSimple, IsoContour}

import org.sofa.opengl.{SGL, MatrixStack, Shader, ShaderProgram, VertexArray, Camera, Texture}
import org.sofa.opengl.mesh.{Mesh, Plane, Cube, DynPointsMesh, WireCube, ColoredLineSet, Axis, DynTriangleMesh, DynIndexedTriangleMesh, TriangleSet, ColoredTriangleSet, ColoredSurfaceTriangleSet, Cylinder}
import org.sofa.opengl.surface.{SurfaceRenderer, BasicCameraController, Surface, KeyEvent, ScrollEvent, MotionEvent}

import javax.media.opengl.{GLProfile, GLCapabilities}

import scala.collection.mutable.ArrayBuffer

object JuiceLauncher {
	def main(args:Array[String]) = {
		GLProfile.initSingleton()
		Thread.sleep(1000)
		(new JuiceLauncher).launch
	}
}

class JuiceLauncher {
	/** View point. */
	var camera = new Camera()

	/** Interaction. */
	var ctrl:BasicCameraController = null
	
	/** Rendering surface. */
	var surface:Surface = null
	
	/** The visco-elastic simulator. */
	var scene = new JuiceScene(camera)

	/** Launch the game main screen. */
	def launch() {
		val caps = new GLCapabilities(GLProfile.getGL2ES2)
		
		caps.setRedBits(8)
		caps.setGreenBits(8)
		caps.setBlueBits(8)
		caps.setAlphaBits(8)
		caps.setNumSamples(4)
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		
		Environment.path += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/simu/test/"
		Environment.readConfigFile("juiceConfig.txt")
		Environment.initializeFieldsOf(Particle)
		Environment.initializeFieldsOf(scene)
		Environment.initializeFieldsOf(camera)
		Environment.printParameters(Console.out)

		ctrl                 = new JuiceInteractions(camera, scene)
		scene.initSurface    = scene.initializeSurface
		scene.frame          = scene.display
		scene.surfaceChanged = scene.reshape
		scene.key            = ctrl.key
		scene.motion         = ctrl.motion
		scene.scroll         = ctrl.scroll
		scene.close          = { surface => sys.exit }
		surface              = new org.sofa.opengl.backend.SurfaceNewt(
								scene, camera, "Juice", caps,
								org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)	
	}
}

class Player(val emitPoint:Point3, val velocity:Vector3, var timeToEmit:Int, var timeToWait:Int) {
	var emiting = false

	var waiting = timeToWait

	def step() {
		waiting -= 1

		if(waiting <= 0) {
			emiting = ! emiting

			if(emiting) {
				waiting = timeToEmit
			} else {
				waiting = timeToWait
			}
		}
	}
}

class JuiceScene(val camera:Camera) extends SurfaceRenderer {	

// == Settings ====================================================

	/** max number of triangles for the iso-surface. */
	var maxDynTriangles = 3000

	/** max number of lines for the iso-contour. */
	var maxDynLines = 3000

	/** max number of springs to draw. */
	var maxSprings = 2000

	/** max number of particles in the simulation. */
	var size = 500

	var playfield = Vector2(30, 10)

	// -- Biroute config --------------------------------------------

	val player1 = new Player(Point3(-12,5,0), Vector3(15,6,0), 7, 20)

	val player2 = new Player(Point3(12,5,0), Vector3(-15,6,0), 7, 20)

	var curParticle = 10
	
	// -- Iso Surface -----------------------------------------------

	/** Number of iso-cubes inside a space hash cube. */
	var isoDiv = 2.0
	
	/** Size of an iso-cell. */
	var isoCellSize = Particle.spacialHashBucketSize / isoDiv
	
	/** Limit of the iso-surface in the implicit function used to define the surface. */
	var isoLimit = 0.65

	// -- Rendering config -------------------------------------------

	var drawParticlesFlag = true
	
	var drawSpringsFlag = false
	
	var drawIsoSurfaceFlag = false
	
	var drawIsoContourFlag = false
	
	var drawIsoPlaneFlag = false
	
	var drawParticlesQuadFlag = true
	
	var particlesToRemovePerStep = 10
	
	var isoSurfaceColor = Rgba(1,1,1,0.9)

	var particleColor = Rgba(0.7, 0.7, 1, 0.9)
	
	var clearColor = Rgba.black
	
	var groundColor = Rgba.grey80
	
	val light1 = Vector4(0, 7, 3, 1)	
	
	var particleSizePx = 30f // 160f
	
	var particleQuadSize = 0.8f
	
	var birouteSize = 2f
	
// == Utility =====================================================

	def printConfig() {
		println("VES config:")
		println("  Visibility:")
		println("    draw particles ............ %b".format(drawParticlesFlag))
		println("    draw springs .............. %b".format(drawSpringsFlag))
		println("    draw iso contour .......... %b".format(drawIsoContourFlag))
		println("    draw iso plane ............ %b".format(drawIsoPlaneFlag))
		println("  Iso surface:")
		println("    isoDiv .................... %.4f".format(isoDiv))
		println("    isoCellSize ............... %.4f".format(isoCellSize))
		println("    isoLimit .................. %.4f".format(isoLimit))
		println("  Simu:")
		println("    particles to remove ....... %d".format(particlesToRemovePerStep))
	}

// == Fields ===========================================================

	// -- View -------------------------------------

	var gl:SGL = null
	
	val projection:Matrix4 = new Matrix4
	val modelview = new MatrixStack(new Matrix4)
	
	// -- Shading -----------------------------------

	var phongShad:ShaderProgram = null
	var particlesShad:ShaderProgram = null
	var particlesQuadShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	var nmapShad:ShaderProgram = null
	var wallShad:ShaderProgram = null
	
	// -- Model ---------------------------------------

	var ground:VertexArray = null
	var axis:VertexArray = null
	var particles:VertexArray = null	
	var isoSurface:VertexArray = null
	var isoPlane:VertexArray = null
	var isoContour:VertexArray = null
	var obstacles:VertexArray = null
	var springs:VertexArray = null
	var quad:VertexArray = null
	var biroute:VertexArray = null
	var wall:VertexArray = null

	var axisMesh = new Axis(10)
	var groundMesh = new Plane(2, 2, playfield.x.toFloat, playfield.x.toFloat)
	var particlesMesh:DynPointsMesh = null
	var isoSurfaceMesh = new DynIndexedTriangleMesh(maxDynTriangles)
	var isoPlaneMesh = new DynIndexedTriangleMesh(maxDynTriangles)
	var isoContourMesh = new ColoredLineSet(maxDynLines)
	var obstaclesMesh = new ColoredSurfaceTriangleSet(4)
	var springsMesh = new ColoredLineSet(maxSprings)
	var quadMesh = new Plane(2, 2, particleQuadSize, particleQuadSize, true)
	var birouteMesh = new Cylinder(birouteSize*0.25f, birouteSize, 8, 1)
	var wallMesh:Mesh = null

	// -- Texture -------------------------------------------

	var pointTex:Texture = null

	var groundTex:Texture = null

	var groundNMapTex:Texture = null

	// -- General -------------------------------------------

	val random = new scala.util.Random()
	
	var step = 0
	
	var running = true
	
	val simu = new ViscoElasticSimulation 
	
	var isoSurfaceComp:IsoSurface = null
	
	var isoContourComp:IsoContour = null

	val timer = new Timer(Console.out)

// == Commands =====================================================================

	def pausePlay() { running = ! running }

// == Init. ========================================================================
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.includePath  += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
		Shader.includePath  += "src/com/chouquette/tests"
		Texture.includePath += "/Users/antoine/Documents/Programs/SOFA/"
		Texture.includePath += "textures/"
			
		initSimuParams
		initGL(sgl)
		initTextures
		initShaders
		initMeshes
		initGeometry
		initCamera(surface)

		player2.waiting = 0
	}

	protected def initCamera(surface:Surface) {
		camera.setFocus(0, 4, 0)
//		camera.viewSpherical(0, 0, 50)
		camera.viewCartesian(0, 15, 50)
		reshape(surface)
	}

	def reshape(surface:Surface) {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
		camera.frustum(-camera.viewportRatio, camera.viewportRatio, -1, 1, 5)
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
		gl.enable(gl.BLEND)
		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
		//gl.enable(gl.PROGRAM_POINT_SIZE)	// WTF ?
		
		gl.checkErrors
	}

	protected def initTextures() {
		pointTex = new Texture(gl, "Point.png", true)
	    pointTex.minMagFilter(gl.LINEAR, gl.LINEAR)
	    pointTex.wrap(gl.REPEAT)

		groundTex = new Texture(gl, "Ground.png", true)
	    groundTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    groundTex.wrap(gl.REPEAT)

		groundNMapTex = new Texture(gl, "GroundNMap.png", true)
	    groundNMapTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    groundNMapTex.wrap(gl.REPEAT)
	}
	
	protected def initShaders() {
		phongShad         = ShaderProgram(gl, "phong shader",          "es2/phonghi.vert.glsl",      "es2/phonghi.frag.glsl")
		plainShad         = ShaderProgram(gl, "plain shader",          "es2/plainColor.vert.glsl",   "es2/plainColor.frag.glsl")
		particlesShad     = ShaderProgram(gl, "particles shader",      "es2/particles.vert.glsl",    "es2/particles.frag.glsl")
		particlesQuadShad = ShaderProgram(gl, "particles quad shader", "es2/particlesTex.vert.glsl", "es2/particlesTex.frag.glsl")
		nmapShad          = ShaderProgram(gl, "normal map phong",      "es2/nmapPhong.vert",         "es2/nmapPhong.frag")
		wallShad          = ShaderProgram(gl, "wall shader",           "es2/phonghiuniformcolor.vert.glsl", "es2/phonghiuniformcolor.frag.glsl")

	}

	protected def initMeshes() {
		val model = new org.sofa.opengl.io.collada.File(scala.xml.XML.loadFile("/Users/antoine/Documents/Art/Sculptures/Blender/MurJuice.dae").child)
		wallMesh = model.library.geometries.get("geometry").get.meshes.get("mesh").get.toMesh 
	}
	
	protected def initGeometry() {
		initParticles		
		initSimu

		// NMap shader

		var v = nmapShad.getAttribLocation("position")
	    var n = nmapShad.getAttribLocation("normal")
	    var t = nmapShad.getAttribLocation("tangent")
	    var u = nmapShad.getAttribLocation("texCoords")

		//groundMesh.setColor(groundColor)
		
		groundMesh.setTextureRepeat(50,50)
		ground = groundMesh.newVertexArray(gl, ("vertices", v), ("normals", n), ("tangents", t), ("texcoords", u))

		// Phong shader
		
		v     = phongShad.getAttribLocation("position")
		var c = phongShad.getAttribLocation("color")
		n     = phongShad.getAttribLocation("normal")
		
		isoSurface = isoSurfaceMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
		isoPlane   = isoPlaneMesh.newVertexArray(  gl, ("vertices", v), ("colors", c), ("normals", n))
		obstacles  = obstaclesMesh.newVertexArray( gl, ("vertices", v), ("colors", c), ("normals", n))
		biroute    = birouteMesh.newVertexArray(   gl, ("vertices", v), ("colors", c), ("normals", n))
		
		// Plain shader
		
		v = plainShad.getAttribLocation("position")
		c = plainShad.getAttribLocation("color")
				
		axis       = axisMesh.newVertexArray(      gl, ("vertices", v), ("colors", c))
		springs    = springsMesh.newVertexArray(   gl, ("vertices", v), ("colors", c))
		isoContour = isoContourMesh.newVertexArray(gl, ("vertices", v), ("colors", c))

		// Particles shader
		
		v = particlesShad.getAttribLocation("position")
		c = particlesShad.getAttribLocation("color") 
		
		particles = particlesMesh.newVertexArray(gl, gl.STATIC_DRAW, ("vertices", v), ("colors", c))

		v = particlesQuadShad.getAttribLocation("position")
		t = particlesQuadShad.getAttribLocation("texCoords")

		quadMesh.setTextureRepeat(1, 1)
		quad = quadMesh.newVertexArray(gl, gl.STATIC_DRAW, ("vertices", v), ("texcoords", t))

		for(i <- 0 until maxDynTriangles*3) {
			isoSurfaceMesh.setPointColor(i, isoSurfaceColor)
			isoPlaneMesh.setPointColor(i, isoSurfaceColor)
			isoPlaneMesh.setPointNormal(i, 0, 0, 1)
		}

		var springColor = Rgba(1, 1, 1, 0.8)
		for(i <- 0 until maxSprings) {
			springsMesh.setColor(i, springColor)
		}

		// Murs
	
		v = wallShad.getAttribLocation("position")
		n = wallShad.getAttribLocation("normal")

		wall = wallMesh.newVertexArray(gl, ("vertices", v), ("normals", n))
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
		obstaclesMesh.setTriangle(i, wall.tri0)
		obstaclesMesh.setNormal(i, wall.tri0.normal)
		obstaclesMesh.setColor(i, Rgba.blue)

		obstaclesMesh.setTriangle(i+1, wall.tri1)
		obstaclesMesh.setNormal(i+1, wall.tri1.normal)
		obstaclesMesh.setColor(i+1, Rgba.blue)
		
		simu.addObstacle(wall)
	}
	
	// ----------------------------------------------------------------------------------
	// Rendering
	
	def display(surface:Surface) {
		if((step+1)%10 == 0)
			println("--step %d ---------------------".format(step+1))
		
		if(running) {
			player1.step
			player2.step
			if(player1.emiting) juicePlayer(player1)
			if(player2.emiting) juicePlayer(player2)
			updateParticles
		}
		
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		
//		camera.rotateViewHorizontal(0.1)
		camera.setupView

		drawGround
		drawWalls
		drawObstacles
		drawIsoSurface
		drawAxis
		drawSprings
		drawParticles
		drawBiroutes
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
	
	protected def juicePlayer(player:Player) {
		if(simu.size < size) {
			simu.addParticle(
					player.emitPoint,
					player.velocity.x+random.nextFloat*0.1,
					player.velocity.y+random.nextFloat*0.1,
					player.velocity.z)
			curParticle += 1
		}
	}

	protected def removeSomeParticles() {
		var I = 0
		val N = simu.size
		var removed = 0
		val toRemove = new ArrayBuffer[Particle]()
		while(I < N) {
			var p = simu(I)

			if(p.x.y < (Particle.ground+0.1) && removed < particlesToRemovePerStep) {
				toRemove += p
			}

			I += 1
		}

		toRemove.foreach { simu.removeParticle(_) }
	}

	protected def drawGround() {
		gl.enable(gl.BLEND)
		nmapShad.use
		
		groundTex.bindTo(gl.TEXTURE0)
	    nmapShad.uniform("texColor", 0)	// Texture Unit 0
	    groundNMapTex.bindTo(gl.TEXTURE2)
	    nmapShad.uniform("texNormal", 2)	// Texture Unit 2
		
		//useLights(nmapShad)
		nmapShad.uniform("lightPos", Vector3(camera.modelview.top * light1))
	    nmapShad.uniform("lightIntensity", 100f)
	    nmapShad.uniform("ambientIntensity", 0.2f)
	    nmapShad.uniform("specularPow", 128f)

		camera.uniformMVP(nmapShad)
		ground.draw(groundMesh.drawAs)
		gl.disable(gl.BLEND)
	}

	protected def drawWalls() {
		gl.frontFace(gl.CCW)
		wallShad.use
		useLights(wallShad)
		wallShad.uniform("color", Rgba.white)
		camera.pushpop {
			camera.translateModel(-15, 4, 0)
			camera.scaleModel(10,10,10)
			camera.rotateModel(math.Pi/2, 0, 1, 0)
			camera.uniformMVP(wallShad)
			wall.draw(wallMesh.drawAs)
		}
		camera.pushpop {
			camera.translateModel(15, 4, 0)
			camera.scaleModel(10,10,10)
			camera.rotateModel(math.Pi/2, 0, 1, 0)
			camera.uniformMVP(wallShad)
			wall.draw(wallMesh.drawAs)
		}
		gl.frontFace(gl.CW)
	}

	protected def drawBiroutes() {
		phongShad.use
		useLights(phongShad)
		drawBiroute(player1)
		drawBiroute(player2)
	}

	protected def drawBiroute(player:Player) {
		var angle = Vector3(0,1,0).angle(player.velocity)

		if(player.velocity.x > 0)
			angle = -angle

		camera.pushpop {
			camera.translateModel(player.emitPoint)
			//camera.translateModel(0, -birouteSize/2, 0)
			camera.rotateModel(angle, 0, 0, 1)
			camera.uniformMVP(phongShad)
			biroute.draw(birouteMesh.drawAs)
		}
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
//			gl.enable(gl.TEXTURE_2D)
			particlesShad.use
//			pointTex.bindTo(gl.TEXTURE0)
//	    	particlesShad.uniform("texColor", 0)	// Texture Unit 0
			camera.setUniformMVP(particlesShad)
			particlesShad.uniform("pointSize", particleSizePx)
			particles.draw(particlesMesh.drawAs, simu.size)
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
			val up = Vector3(0,1,0)
			val dir = Vector3()
			while(I < N) {
				val particle = simu(I)
				camera.push
				camera.translateModel(particle.x.x, particle.x.y, particle.x.z)
				dir.set(particle.x, particle.xprev)
				var l = dir.norm
				var angle = dir.angle(up) - (math.Pi/2)
				if(dir.x > 0)
					angle = -angle
				if(l > 1)  l = 1
 				camera.rotateModel(angle, 0, 0, 1)
				camera.scaleModel(1+l, 1-l, 1)
				camera.setUniformMVP(particlesQuadShad)
				quad.draw(quadMesh.drawAs)
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
			camera.setUniformMVP(plainShad)
			springs.draw(springsMesh.drawAs, simu.springs.size*2)
			gl.lineWidth(1)
			gl.disable(gl.BLEND)
			gl.enable(gl.DEPTH_TEST)
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
		
		// if(drawParticlesFlag) {
		// 	var i = 0
		// 	simu.foreach { particle => 
		// 		particlesMesh.setPoint(i, particle.x)
		// 		i += 1
		// 	}
		
		// 	particlesMesh.updateVertexArray(gl, "vertices", "colors")
		// 	gl.checkErrors
		// }

		// if(drawSpringsFlag) {
		// 	var i = 0
		// 	simu.springs.foreach { spring =>
		// 		springsMesh.setLine(i, spring.i.x, spring.j.x)
		// 		i += 1
		// 	}

		// 	springsMesh.updateVertexArray(gl, "vertices", "colors")
		// 	gl.checkErrors
		// }
		
		// if(drawIsoSurfaceFlag)
		// 	buildIsoSurface

		// if(drawIsoContourFlag || drawIsoPlaneFlag)
		// 	buildIsoContour
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
			
			isoSurfaceMesh.updateVertexArray(gl, "vertices", "colors", "normals", null)
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

			isoPlaneMesh.updateVertexArray(gl, "vertices", "colors", "normals", null)
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
		simu.spaceHash.buckets.foreach { bucket =>
			if((bucket._2.points ne null) && bucket._2.points.size > 0) {
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

	protected def useLights(shader:ShaderProgram) {
		shader.uniform("light.pos",       Vector3(camera.modelview.top * light1))
		shader.uniform("light.intensity", 5f)
		shader.uniform("light.ambient",   0.2f)
		shader.uniform("light.specular",  1000f)
	}
}

/** A simple mouse/key controller for the camera and simulation. */
class JuiceInteractions(camera:Camera, val scene:JuiceScene) extends BasicCameraController(camera) {
    override def key(surface:Surface, keyEvent:KeyEvent) {
        import keyEvent.ActionChar._
        if(keyEvent.isPrintable) {
        	keyEvent.unicodeChar match {
            	case ' ' => { scene.pausePlay }
            	case 'p' => { scene.drawParticlesFlag  = !scene.drawParticlesFlag  }
            	case 'P' => { scene.drawIsoPlaneFlag   = !scene.drawIsoPlaneFlag   }
            	case 's' => { scene.drawIsoSurfaceFlag = !scene.drawIsoSurfaceFlag }
            	case 'O' => { scene.drawIsoContourFlag = !scene.drawIsoContourFlag }
            	case 'i' => { scene.drawSpringsFlag    = !scene.drawSpringsFlag    }
            	case 'q' => { sys.exit(0) }
            	case '1' => { scene.player1.velocity.y -= 0.5 }
            	case '7' => { scene.player1.velocity.y += 0.5 }
            	case '3' => { scene.player2.velocity.y -= 0.5 }
            	case '9' => { scene.player2.velocity.y += 0.5 }
            	case 'H' => {
            		println("Keys:")
            		println("    <space>  pause/play the simulation.")
            		println("    p        toggle draw particles.")
            		println("    P        toggle draw the iso plane.")
            		println("    s        toggle draw the iso surface.")
            		println("    O        toggle draw the iso contour.")
            		println("    i        toogle draw the springs.")
            		println("    q        quit (no questions, it quits!).")
            	}
            	case _ => { super.key(surface, keyEvent) }
            }
        } else {
	    	keyEvent.actionChar match {
		    	case PageUp   => { camera.zoomView(-step) } 
		    	case PageDown => { camera.zoomView(step) }
		    	case Up       => { camera.rotateViewVertical(step) }
		    	case Down     => { camera.rotateViewVertical(-step) }
		    	case Left     => { camera.rotateViewHorizontal(-step) }
		    	case Right    => { camera.rotateViewHorizontal(step) }
		    	case _        => { super.key(surface, keyEvent) }
	    	}

            super.key(surface, keyEvent)
        }
    }
	
	override def scroll(surface:Surface, e:ScrollEvent) {
	    camera.zoom = camera.zoom + e.amount * step
	    if(camera.zoom < 30) camera.zoom = 30
	    else if(camera.zoom > 50) camera.zoom = 50
	} 	
	
	override def motion(surface:Surface, e:MotionEvent) {
		super.motion(surface, e)
	}
}