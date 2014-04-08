package org.sofa.simu.test

import org.sofa.{Timer, Environment}
import org.sofa.simu.{ViscoElasticSimulation, Particle, QuadWall}
import org.sofa.math.{Rgba, Matrix4, Vector4, Vector3, Vector2, Point3, IsoSurface, IsoSurfaceSimple, IsoContour}
import org.sofa.collection.{SpatialPoint, SpatialCube, SpatialHash}

import org.sofa.opengl.{SGL, MatrixStack, Shader, ShaderProgram, VertexArray, Camera, Texture, TextureFramebuffer, TexParams, TexMipMap}
import org.sofa.opengl.mesh.{Mesh, PlaneMesh, CubeMesh, PointsMesh, WireCubeMesh, LinesMesh, AxisMesh, TrianglesMesh, CylinderMesh, EditableMesh, UnindexedTrianglesMesh, VertexAttribute}
import org.sofa.opengl.text.{GLFont, GLString}
import org.sofa.opengl.surface.{SurfaceRenderer, BasicCameraController, Surface, KeyEvent, ScrollEvent, MotionEvent}
import org.sofa.opengl.io.collada.{ColladaFile}

import javax.media.opengl.{GLProfile, GLCapabilities}

import scala.collection.mutable.ArrayBuffer

import scala.math._

object JuiceLauncher {
	def main(args:Array[String]) = { (new JuiceLauncher).launch }
}

class JuiceLauncher {
	/** View point. */
	var camera = Camera()

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

	val player1 = new Player(Point3(-10,5.5,0), Vector3(15,4.5,0), 7, 20)

	val player2 = new Player(Point3(10,5.5,0), Vector3(-15,4.5,0), 7, 20)

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
	
	var clearColor = Rgba.Black
	
	var groundColor = Rgba.Grey80

	var ambientIntensity = 0.1f
	
	val light1 = Vector4(0, 10, 3, 1)	
	
	var particleSizePx = 30f // 160f
	
	var particleQuadSize = 0.8f
	
	var birouteSize = 2f

	var groundTexRepeat = 70
	
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

	var cameraTex:Camera = null
	
	// -- Text --------------------------------------
		
	var heaFont:GLFont = null
	var subFont:GLFont = null
	var stdFont:GLFont = null

	var text:Array[GLString] = new Array[GLString](8)

	// -- Second framebuffer ------------------------

	val fbWidth = 512
	val fbHeight = 256
	var fb:TextureFramebuffer = null

	// -- Shading -----------------------------------

	var phongNoClrShad:ShaderProgram = null
	var phongShad:ShaderProgram = null
	var phongTexShad:ShaderProgram = null
	var particlesShad:ShaderProgram = null
	var particlesQuadShad:ShaderProgram = null
	var plainShad:ShaderProgram = null
	var nmapShad:ShaderProgram = null
	var spyceShad:ShaderProgram = null
	var textShad:ShaderProgram = null
	
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
	var tube:VertexArray = null
	var biroute:VertexArray = null
	var wall:VertexArray = null
	var ledWall:VertexArray = null
	var triangles:VertexArray = null

	var axisMesh = new AxisMesh(10)
	var groundMesh = new PlaneMesh(2, 2, playfield.x.toFloat, playfield.x.toFloat)
	var particlesMesh:PointsMesh = null
	var isoSurfaceMesh = new TrianglesMesh(maxDynTriangles)
	var isoPlaneMesh = new TrianglesMesh(maxDynTriangles)
	var isoContourMesh = new LinesMesh(maxDynLines)
	var obstaclesMesh = new UnindexedTrianglesMesh(4)
	var springsMesh = new LinesMesh(maxSprings)
	var quadMesh = new PlaneMesh(2, 2, particleQuadSize, particleQuadSize, true)
	var tubeMesh = new CylinderMesh(birouteSize*0.25f, birouteSize, 8, 1)
	var wallMesh:Mesh = null
	var birouteMesh:Mesh = null
	var ledWallMesh:PlaneMesh = new PlaneMesh(2, 2, playfield.x.toFloat, playfield.x.toFloat/1.7f, true)
	var trianglesMesh = new TrianglesMesh(8)

	// -- Texture -------------------------------------------

	var pointTex:Texture = null
	var groundTex:Texture = null
	var groundNMapTex:Texture = null
	var wallTex:Texture = null
	var birouteTex:Texture = null
	var birouteNMapTex:Texture = null

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
		Shader.path      += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
		Shader.path      += "src/com/chouquette/tests"
		Texture.path     += "/Users/antoine/Documents/Programs/SOFA/"
		Texture.path     += "textures/"
		GLFont.path      += "/Users/antoine/Library/Fonts"
		GLFont.path      += "fonts/"
		ColladaFile.path += "/Users/antoine/Documents/Art/Sculptures/Blender/"
		ColladaFile.path += "meshes/"

		camera.viewport = (1280, 800)
		cameraTex = new Camera(); cameraTex.viewport = (fbWidth, fbHeight)

		initSimuParams
		initGL(sgl)
		initTextures
		initTextureFB
		initShaders
		initFonts
		initMeshes
		initGeometry
		initCamera(surface)

		player2.waiting = 0
	}

	protected def initCamera(surface:Surface) {
		camera.setFocus(0, 4, 0)
//		camera.eyeSpherical(0, 0, 50)
		camera.eyeCartesian(0, 15, 50)
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

	protected def initFonts() {
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

	protected def initTextures() {
		pointTex = new Texture(gl, "Point.png", TexParams(mipMap=TexMipMap.Generate))
	    pointTex.minMagFilter(gl.LINEAR, gl.LINEAR)
	    pointTex.wrap(gl.REPEAT)

		groundTex = new Texture(gl, "Ground.png", TexParams(mipMap=TexMipMap.Generate))
	    groundTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    groundTex.wrap(gl.REPEAT)

		groundNMapTex = new Texture(gl, "GroundNMap.png", TexParams(mipMap=TexMipMap.Generate))
	    groundNMapTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    groundNMapTex.wrap(gl.REPEAT)

		birouteTex = new Texture(gl, "BruceColor.png", TexParams(mipMap=TexMipMap.Generate))
	    birouteTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    birouteTex.wrap(gl.REPEAT)

		birouteNMapTex = new Texture(gl, "BruceNMap.png", TexParams(mipMap=TexMipMap.Generate))
	    birouteNMapTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    birouteNMapTex.wrap(gl.REPEAT)

	    wallTex = new Texture(gl, "Wall.jpg", TexParams(mipMap=TexMipMap.Generate))
	    wallTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    wallTex.wrap(gl.REPEAT)
	}
	
	protected def initShaders() {
		phongShad         = ShaderProgram(gl, "phong shader",          "es2/phonghi.vert.glsl",      "es2/phonghi.frag.glsl")
		phongTexShad      = ShaderProgram(gl, "phong textured shader", "es2/phonghitex.vert.glsl",   "es2/phonghitex.frag.glsl")
		plainShad         = ShaderProgram(gl, "plain shader",          "es2/plainColor.vert.glsl",   "es2/plainColor.frag.glsl")
		particlesShad     = ShaderProgram(gl, "particles shader",      "es2/particles.vert.glsl",    "es2/particles.frag.glsl")
		particlesQuadShad = ShaderProgram(gl, "particles quad shader", "es2/particlesTex.vert.glsl", "es2/particlesTex.frag.glsl")
		nmapShad          = ShaderProgram(gl, "normal map phong",      "es2/nmapPhong.vert",         "es2/nmapPhong.frag")
		spyceShad         = ShaderProgram(gl, "spyce wall shader",     "es2/spyce.vert.glsl",        "es2/spyce.frag.glsl")
		textShad          = ShaderProgram(gl, "text shader",           "es2/text.vert.glsl",         "es2/text.frag.glsl")
		phongNoClrShad    = ShaderProgram(gl, "phong uni clr shader",  "es2/phonghiuniformcolor.vert.glsl", "es2/phonghiuniformcolor.frag.glsl")
	}

	protected def initMeshes() {
		val wallModel    = new ColladaFile("MurJuice.dae")
		val birouteModel = new ColladaFile("Bruce_004.dae")
	
		birouteModel.library.geometry("BirouteLowPoly").get.mesh.mergeVertices(true)
		birouteModel.library.geometry("BirouteLowPoly").get.mesh.blenderToOpenGL(true)

		wallMesh    = wallModel.library.geometry("Plane.007").get.mesh.toMesh	
		birouteMesh = birouteModel.library.geometry("BirouteLowPoly").get.mesh.toMesh

		birouteMesh.asInstanceOf[EditableMesh].autoComputeTangents
	}
	
	protected def initGeometry() {
		import VertexAttribute._

		initParticles		
		initSimu

		// NMap shader

		//groundMesh.setColor(groundColor)		
		groundMesh.setTextureRepeat(groundTexRepeat, groundTexRepeat)
		
		ground  = groundMesh.newVertexArray( gl, nmapShad, Vertex -> "position", Normal -> "normal", Tangent -> "tangent", TexCoord -> "texCoords")
		biroute = birouteMesh.newVertexArray(gl, nmapShad, Vertex -> "position", Normal -> "normal", Tangent -> "tangent", TexCoord -> "texCoords")

		// Phong shader
		
		isoSurface = isoSurfaceMesh.newVertexArray(gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		isoPlane   = isoPlaneMesh.newVertexArray(  gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")
		obstacles  = obstaclesMesh.newVertexArray( gl, phongShad, Vertex -> "position", Color -> "color", Normal -> "normal")

		// Plain shader
		
		axis       = axisMesh.newVertexArray(      gl, plainShad, Vertex -> "position", Color -> "color")
		springs    = springsMesh.newVertexArray(   gl, plainShad, Vertex -> "position", Color -> "color")
		isoContour = isoContourMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")

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

		triangles = trianglesMesh.newVertexArray(gl, plainShad, Vertex -> "position", Color -> "color")

		// Particles shader
		
		particles = particlesMesh.newVertexArray(gl, gl.STATIC_DRAW, particlesShad, Vertex -> "position", Color -> "color")

		quadMesh.setTextureRepeat(1, 1)
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

		// Walls
	
		ledWallMesh.setTextureRepeat(1,1)
		wall    = wallMesh.newVertexArray(   gl, phongTexShad, Vertex -> "position", Normal -> "normal", TexCoord -> "texCoords")
		ledWall = ledWallMesh.newVertexArray(gl, phongTexShad, Vertex -> "position", Normal -> "normal", TexCoord -> "texCoords")
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

var angle = 0.0
	
	def display(surface:Surface) {
		// if((step+1)%10 == 0)
		// 	println("--step %d ---------------------".format(step+1))
		
		if(running) {
			player1.step
			player2.step
			if(player1.emiting) juicePlayer(player1)
			if(player2.emiting) juicePlayer(player2)
			updateParticles
		}
		
		// Render the led wall.

		fb.display {
			gl.frontFace(gl.CW)
			gl.enable(gl.BLEND)
			gl.disable(gl.DEPTH_TEST)
			gl.clearColor(0, 0, 0, 0)
			gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
			//cameraTex.rotateViewHorizontal(0.1)
			cameraTex.viewIdentity

			// Triangles

			cameraTex.pushpop {
				gl.frontFace(gl.CCW)
				plainShad.use
				cameraTex.translate(fbWidth/2, fbHeight/2, 0)
				cameraTex.scale(40, 40, 1)
				cameraTex.rotate(angle, 0, 0, 1)
				cameraTex.uniformMVP(plainShad)
				triangles.draw(trianglesMesh.drawAs(gl), 8*3)

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

		// Render the scene.

		gl.clearColor(clearColor)
		gl.viewport(0, 0, surface.width, surface.height)
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		
//		camera.rotateViewHorizontal(0.1)
		camera.lookAt

		drawWalls
		drawObstacles
		drawIsoSurface
		drawAxis
		drawSprings
		drawParticles
		drawBiroutes
		drawIsoPlane
		drawIsoContour
		drawGround
		drawParticlesQuads
		
		surface.swapBuffers
		gl.checkErrors
		
		if(running) {
			removeSomeParticles
		}
		
		step += 1

		// if(step % 10 == 0) {
		// 	timer.printAvgs("-- Iso ------")
		// }
		// if(step % 1000 == 0) {
		// 	timer.reset
		// }
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
	    nmapShad.uniform("ambientIntensity", ambientIntensity)
	    nmapShad.uniform("specularPow", 128f)

		camera.uniform(nmapShad)
		ground.draw(groundMesh.drawAs(gl))
		gl.disable(gl.BLEND)
	}

	protected def drawWalls() {
		gl.enable(gl.DEPTH_TEST)
		gl.frontFace(gl.CCW)
		phongTexShad.use
		wallTex.bindTo(gl.TEXTURE0)
		phongTexShad.uniform("texColor", 0)
		useLights(phongTexShad)
	//	phongTexShad.uniform("color", Rgba.black)
		camera.pushpop {
			camera.translate(-15, 4, 0)
			camera.scale(10,10,10)
			camera.rotate(math.Pi/2, 0, 1, 0)
			camera.uniform(phongTexShad)
			wall.draw(wallMesh.drawAs(gl))
		}
		camera.pushpop {
			camera.translate(15, 4, 0)
			camera.scale(10,10,10)
			camera.rotate(math.Pi/2, 0, 1, 0)
			camera.uniform(phongTexShad)
			wall.draw(wallMesh.drawAs(gl))
		}
		gl.frontFace(gl.CW)

		camera.pushpop {
			spyceShad.use
			fb.bindColorTextureTo(gl.TEXTURE0)
	    	spyceShad.uniform("texColor", 0)	// Texture Unit 0
			useLights(spyceShad, 1000f, 100f)
			camera.translate(0,4,-10)
			camera.uniform(spyceShad)
			ledWall.draw(ledWallMesh.drawAs(gl))
			gl.bindTexture(gl.TEXTURE_2D, null)
		}

		gl.disable(gl.DEPTH_TEST)
	}

val birouteColor = Rgba(209.0/255.0, 189.0/255.0, 168.0/255.0)

	protected def drawBiroutes() {
		gl.enable(gl.DEPTH_TEST)
		gl.disable(gl.CULL_FACE)
		gl.frontFace(gl.CW)
		//phongNoClrShad.use
		//phongNoClrShad.uniform("color", birouteColor)
		//useLights(phongNoClrShad, 120f, 10f)
		
		nmapShad.use

		birouteTex.bindTo(gl.TEXTURE0)
	    nmapShad.uniform("texColor", 0)	// Texture Unit 0
	    birouteNMapTex.bindTo(gl.TEXTURE2)
	    nmapShad.uniform("texNormal", 2)	// Texture Unit 2
		
		//useLights(nmapShad)
		nmapShad.uniform("lightPos", Vector3(camera.modelview.top * light1))
	    nmapShad.uniform("lightIntensity", 128f)
	    nmapShad.uniform("ambientIntensity", ambientIntensity+0.3f)
	    nmapShad.uniform("specularPow", 128f)

//		camera.uniform(nmapShad)

		drawBiroute(player1)
		drawBiroute(player2)
		gl.enable(gl.CULL_FACE)
		gl.disable(gl.DEPTH_TEST)
	}

	protected def drawBiroute(player:Player) {
		var angle = 0.0

		camera.pushpop {
			camera.translate(player.emitPoint)
			camera.scale(0.5, 0.5, 0.5)
			if(player.velocity.x < 0) {
				camera.rotate(Pi, 0, 1, 0)
				angle = (Pi/2.0) - Vector3(0,1,0).angle(player.velocity)
			} else {
				angle = (Pi/2.0) - Vector3(0,1,0).angle(player.velocity)
			}
			camera.rotate(angle, 0, 0, 1)
//			camera.uniform(phongNoClrShad)
			camera.uniform(nmapShad)
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
			val up = Vector3(0,1,0)
			val dir = Vector3()
			while(I < N) {
				val particle = simu(I)
				camera.push
				camera.translate(particle.x.x, particle.x.y, particle.x.z)
				dir.set(particle.x, particle.xprev)
				var l = dir.norm
				var angle = dir.angle(up) - (math.Pi/2)
				if(dir.x > 0)
					angle = -angle
				if(l > 1)  l = 1
 				camera.rotate(angle, 0, 0, 1)
				camera.scale(1+l, 1-l, 1)
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
		//simu.simulationStepWithTimer(0.044)
		
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

	protected def useLights(shader:ShaderProgram, intensity:Float, specular:Float) {
		shader.uniform("light.pos",       Vector3(camera.modelview.top * light1))
		shader.uniform("light.intensity", intensity)
		shader.uniform("light.ambient",   ambientIntensity)
		shader.uniform("light.specular",  specular)
	}

	protected def useLights(shader:ShaderProgram) {
		shader.uniform("light.pos",       Vector3(camera.modelview.top * light1))
		shader.uniform("light.intensity", 5f)
		shader.uniform("light.ambient",   ambientIntensity)
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
		    	case PageUp   => { camera.eyeTraveling(-step) } 
		    	case PageDown => { camera.eyeTraveling(step) }
		    	case Up       => { camera.rotateEyeVertical(step) }
		    	case Down     => { camera.rotateEyeVertical(-step) }
		    	case Left     => { camera.rotateEyeHorizontal(-step) }
		    	case Right    => { camera.rotateEyeHorizontal(step) }
		    	case _        => { super.key(surface, keyEvent) }
	    	}

            super.key(surface, keyEvent)
        }
    }
	
	override def scroll(surface:Surface, e:ScrollEvent) {
	    camera.radius = camera.radius + e.amount * step
	    if(camera.radius < 30) camera.radius = 30
	    else if(camera.radius > 50) camera.radius = 50
	} 	
	
	override def motion(surface:Surface, e:MotionEvent) {
		super.motion(surface, e)
	}
}