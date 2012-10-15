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
import org.sofa.opengl.Texture
import org.sofa.math.IsoSurfaceSimple
import org.sofa.opengl.mesh.DynTriangleMesh
import org.sofa.opengl.surface.KeyEvent
import org.sofa.math.IsoSurface
import org.sofa.opengl.mesh.DynIndexedTriangleMesh
import org.sofa.simu.ViscoElasticSimulation
import org.sofa.simu.Particle

object TestViscoElasticSimulation {
	def main(args:Array[String]) = (new TestViscoElasticSimulation).test
}

class TestViscoElasticSimulation extends SurfaceRenderer {
	// ----------------------------------------------------------------------------------
	// Parameters
	
	val maxDynTriangles = 6000
	val size = 1000
	val emitPoint = Point3(-5, 5, 0)
	val emitVelocity1 = Vector3(15, 6, 0)
	val emitVelocity2 = Vector3(15, 8, 0)
	
	/** Number of iso-cubes inside a space hash cube. */
	var isoDiv = 3.0
	
	val isoCellSize = Particle.spacialHashBucketSize / isoDiv
	
	/** Limit of the iso-surface in the implicit function used to define the surface. */
	val isoLimit = 0.65

	var drawParticlesFlag = false
	var drawSpaceHashFlag = false
	var drawIsoCubesFlag = false
	var drawIsoSurfaceFlag = true
	
	val isoSurfaceColor = Rgba.white
	val particleColor = Rgba(0.7, 0.7, 1, 0.3)
	val clearColor = Rgba.grey10
	val planeColor = Rgba.grey80
	val light1 = Vector4(4, 4, 4, 1)	
	val particleSizePx = 160f // 160f
	
	protected val timeToWait = 20
	protected val timeToEmit =  7
	protected var curParticle = 0
	protected var waiting = timeToEmit
	protected var emiting = true
	protected var velocityMode = true
	
	// ----------------------------------------------------------------------------------
	// Fields
	
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
	var wcube2:VertexArray = null
	var isoSurface:VertexArray = null
	
	var axisMesh = new Axis(10)
	var planeMesh = new Plane(2, 2, 10, 10)
	var particlesMesh:DynPointsMesh = null
	var wcubeMesh:WireCube = null
	var wcubeMesh2:WireCube = null
	var isoSurfaceMesh = new DynIndexedTriangleMesh(maxDynTriangles)
	
	var camera:Camera = null
	var ctrl:BasicCameraController = null
	
	val random = new scala.util.Random()
	var step = 0
	var running = true
	val simu = new ViscoElasticSimulation 
	var isoSurfaceComp:IsoSurface = null

	// ----------------------------------------------------------------------------------
	// Commands

	def pausePlay() { running = ! running }

	// ----------------------------------------------------------------------------------
	// Init.
	
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
		ctrl           = new TVESCameraController(camera, this)
		initSurface    = initializeSurface
		frame          = display
		surfaceChanged = reshape
		key            = ctrl.key
		motion         = ctrl.motion
		scroll         = ctrl.scroll
		close          = { surface => sys.exit }
		surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
							camera, "P'tit jet !", caps,
							org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)	
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
		Shader.includePath += "/Users/antoine/Documents/Programs/SOFA/src-scala/org/sofa/opengl/shaders/"
		Shader.includePath += "src/com/chouquette/tests"
			
		initGL(sgl)
		initShaders
		initGeometry
		
		camera.viewCartesian(0, 5, 14)
		camera.setFocus(0, 4, 0)
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
		phongShad = ShaderProgram(gl, "phong shader", "es2/phonghi.vert.glsl", "es2/phonghi.frag.glsl")
		particlesShad = ShaderProgram(gl, "particles shader", "es2/particles.vert.glsl", "es2/particles.frag.glsl")
		plainShad = ShaderProgram(gl, "plain shader", "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
	}
	
	protected def initGeometry() {
		initParticles
		
		var v = phongShad.getAttribLocation("position")
		var c = phongShad.getAttribLocation("color")
		var n = phongShad.getAttribLocation("normal")
		
		planeMesh.setColor(Rgba.red)
		
		plane = planeMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
		isoSurface = isoSurfaceMesh.newVertexArray(gl, ("vertices", v), ("colors", c), ("normals", n))
		
		v = plainShad.getAttribLocation("position")
		c = plainShad.getAttribLocation("color")
		
		wcubeMesh = new WireCube(simu.spaceHash.bucketSize.toFloat)
		wcubeMesh2 = new WireCube(isoCellSize.toFloat)
		wcubeMesh.setColor(Rgba(1, 1, 1, 0.5))
		wcubeMesh2.setColor(Rgba(1, 0, 0, 0.2))
		wcube = wcubeMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
		wcube2 = wcubeMesh2.newVertexArray(gl, ("vertices", v), ("colors", c))
		
		axis = axisMesh.newVertexArray(gl, ("vertices", v), ("colors", c))
		
		v = particlesShad.getAttribLocation("position")
		c = particlesShad.getAttribLocation("color") 
		
		particles = particlesMesh.newVertexArray(gl, gl.STATIC_DRAW, ("vertices", v), ("colors", c))

		for(i <- 0 until maxDynTriangles*3) {
			isoSurfaceMesh.setPointColor(i, isoSurfaceColor)
		}
	}
	
	protected def initParticles {
		particlesMesh = new DynPointsMesh(size) 
		
		for(i <- 0 until size) {
			particlesMesh.setPoint(i, 0, 200, 0) // make them invisible.
			particlesMesh.setColor(i, particleColor)
		}
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
		drawIsoSurface
		drawAxis
		drawSpaceHash
		drawIsoCubes
		drawParticles
		
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
				particlesMesh.setPoint(curParticle, p.x)
				particlesMesh.setColor(curParticle, particleColor)
				curParticle += 1
			}
		}
	}	
	
	protected def removeSomeParticles() {
		if(!emiting && simu.size > 100) {
			var p = simu.randomParticle(random)
			var i = 0
			
			while(p.x.y > 0.2 && i<10) {
				p = simu.randomParticle(random)
				i += 1
			}
			
			if(p.x.y <= 0.2) {
				Console.err.println("removing particle %d".format(p.index))
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
	
	protected def drawSpaceHash() {
		if(drawSpaceHashFlag) {
			gl.enable(gl.BLEND)
			plainShad.use
			val cs = simu.spaceHash.bucketSize
			val cs2 = cs/2
			simu.spaceHash.buckets.foreach { bucket =>
				camera.pushpop {
					val p = bucket._2.position
					camera.translateModel((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.setUniformMVP(plainShad)
					wcube.draw(wcubeMesh.drawAs)
				}
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
				camera.pushpop {
					val p = cube.pos
					camera.translateModel((p.x*cs)+cs2, (p.y*cs)+cs2, (p.z*cs)+cs2)
					camera.setUniformMVP(plainShad)
					if(!cube.isEmpty)
						wcube2.draw(wcubeMesh.drawAs)
				}
			}
			gl.disable(gl.BLEND)
		}
	}
	
	protected def drawIsoSurface() {
		if(drawIsoSurfaceFlag && isoSurfaceComp.triangleCount > 0) {
			phongShad.use
			useLights(phongShad)
			camera.uniformMVP(phongShad)
			isoSurface.draw(isoSurfaceMesh.drawAs, isoSurfaceComp.triangleCount*3)
		}
	}
	
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
		
		buildIsoSurface
	}
	
	protected def buildIsoSurface() {
		isoSurfaceComp = new IsoSurface(isoCellSize)
		isoSurfaceComp.autoComputeNormals(false)
		exploreSpaceHash
		updateIsoSurfaceRepresentation
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
	
	protected def exploreSpaceHash() {
		var i = 0
		simu.spaceHash.buckets.foreach { bucket =>
			val x = (bucket._1.x-1) * simu.spaceHash.bucketSize
			val y = (bucket._1.y-1) * simu.spaceHash.bucketSize
			val z = (bucket._1.z-1) * simu.spaceHash.bucketSize
			
			val p = isoSurfaceComp.nearestCubePos(x, y, z)

			isoSurfaceComp.addCubesAt(
					p.x.toInt,        p.y.toInt,        p.z.toInt,
					(3*isoDiv).toInt, (3*isoDiv).toInt, (3*isoDiv).toInt,
					simu.evalIsoSurface, isoLimit)
			i+=1
		}
	}

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
class TVESCameraController(camera:Camera, val ves:TestViscoElasticSimulation) extends BasicCameraController(camera) {
    override def key(surface:Surface, keyEvent:KeyEvent) {
        import keyEvent.ActionChar._
        if(keyEvent.isPrintable) {
        	keyEvent.unicodeChar match {
            	case ' ' => { ves.pausePlay }
            	case 'p' => { ves.drawParticlesFlag = !ves.drawParticlesFlag }
            	case 'h' => { ves.drawSpaceHashFlag = !ves.drawSpaceHashFlag }
            	case 'c' => { ves.drawIsoCubesFlag = !ves.drawIsoCubesFlag }
            	case 's' => { ves.drawIsoSurfaceFlag = !ves.drawIsoSurfaceFlag }
            	case 'q' => { sys.exit(0) }
            	case _ => super.key(surface, keyEvent)
            }
        } else {
            super.key(surface, keyEvent)
        }
    }
}