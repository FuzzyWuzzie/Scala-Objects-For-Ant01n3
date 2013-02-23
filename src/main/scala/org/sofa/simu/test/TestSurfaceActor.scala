package org.sofa.simu.oberon.test

import scala.math._
import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.nio._
import org.sofa.math.{Rgba, Vector3, Vector4}
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, HemisphereLight}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute}
import org.sofa.opengl.mesh.skeleton.{Bone => SkelBone}

import akka.actor.{Actor, Props, ActorSystem, ReceiveTimeout, ActorRef}
import scala.concurrent.duration._
import org.sofa.simu.oberon.SurfaceExecutorService

object SurfaceRendererActor {
	def apply(system:ActorSystem, context:TestSurfaceActor):ActorRef = {
		SurfaceExecutorService.setSurface(context.surface)
		system.actorOf(Props(new SurfaceRendererActor(context)).withDispatcher("oberon.surface-dispatcher"), name="surface-renderer-actor")
	}
}

class SurfaceRendererActor(val ctx:TestSurfaceActor) extends Actor {
	var count = 0
	var startTime = 0L

	def receive() = {
		case ReceiveTimeout ⇒ { val T=System.currentTimeMillis; val duration=T-startTime; startTime=System.currentTimeMillis; if(count > 1000) { self ! "kill!" } else { count += 1; ctx.animate }; }//println("T=%d".format(duration)); }//println(s"executing in ${Thread.currentThread.getName} (count=${count})") }
		case "start" ⇒ { context.setReceiveTimeout(21 millisecond) }
		case "kill!" ⇒ { println("exiting..."); context.stop(self) }
	}

	override def postStop() {
		sys.exit
	}
}


object TestSurfaceActor {
    def main(args:Array[String]):Unit = (new TestSurfaceActor).test
}

class TestSurfaceActor extends SurfaceRenderer {
// General
    
    var gl:SGL = null
    var surface:Surface = null
	var actorSystem:ActorSystem = null
	var surfaceActor:ActorRef = null

// View
    
    var camera:Camera = null
    var ctrl:BasicCameraController = null
    
// Geometry

    var ground:VertexArray = null
    var thing:VertexArray = null
    
    val groundMesh = new PlaneMesh(2, 2, 6 , 6)
    var thingMesh:Mesh = null

// Shading
    
    val clearColor = Rgba.grey30
    
    var groundShader:ShaderProgram = null
    var thingShader:ShaderProgram = null
    
    var light1 = new HemisphereLight(0, 10, 0, Rgba.white, Rgba.grey30) //WhiteLight(0, 2, 2,  5f, 16f, 0.1f)
    
// Go
        
    def test() {
	    camera   = Camera(); camera.viewportPx(1280,800)
	    val caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2))
	    
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(4)

        ctrl           = new BasicCameraController(camera)
	    initSurface    = initializeSurface
	    frame          = display
	    surfaceChanged = reshape
	    close          = { surface => sys.exit }
	    key            = ctrl.key
	    motion         = ctrl.motion
	    scroll         = ctrl.scroll
	    surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
	    					camera, "Skinning", caps,
	    					org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)

	    actorSystem    = ActorSystem("test")
		surfaceActor   = SurfaceRendererActor(actorSystem, this)

		surfaceActor ! "start"
	}
    
// Rendering
    
	def initializeSurface(gl:SGL, surface:Surface) {
	    Shader.path      += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
	    Shader.path      += "shaders/"
	    Texture.path     += "/Users/antoine/Documents/Programs/SOFA/textures"
	    Texture.path     += "textures/"
		ColladaFile.path += "/Users/antoine/Documents/Art/Sculptures/Blender/"
		ColladaFile.path += "meshes/"

	    initGL(gl)
        initShaders
        initModels
	    initGeometry
	    
	    camera.viewCartesian(0, 10, 10)
	    camera.setFocus(0, 2, 0)
	    reshape(surface)
	}

	protected def initGL(sgl:SGL) {
	    gl = sgl

//println("initGL Inside thread %s".format(Thread.currentThread.getName))

        gl.clearColor(clearColor)
	    gl.clearDepth(1f)
	    gl.enable(gl.DEPTH_TEST)
	    
	    gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CW)
        
        gl.disable(gl.BLEND)
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
	}
	
	protected def initShaders() {
		groundShader = ShaderProgram(gl, "ground", "es2/hemiLightUniClr.vert.glsl", "es2/hemiLightUniClr.frag.glsl")
		thingShader  = ShaderProgram(gl, "thing",  "es2/hemiLightUniClr.vert.glsl", "es2/hemiLightUniClr.frag.glsl")
	}

	protected def initModels() {
		val model = new ColladaFile("Cross.dae")

		model.library.geometry("Cross").get.mesh.mergeVertices(true)
		model.library.geometry("Cross").get.mesh.blenderToOpenGL(true)

		thingMesh = model.library.geometry("Cross").get.mesh.toMesh
	}

	protected def initGeometry() {
		import VertexAttribute._

//		groundMesh.setTextureRepeat(30, 30)

	    ground = groundMesh.newVertexArray(gl, groundShader, Vertex -> "position", Normal -> "normal")
	    thing  = thingMesh.newVertexArray( gl, thingShader,  Vertex -> "position", Normal -> "normal")
	}
	
	def reshape(surface:Surface) {
	    camera.viewportPx(surface.width, surface.height)
        var ratio = camera.viewportRatio
        
        gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
        camera.frustum(-ratio, ratio, -1, 1, 2)

//println("Reshape Inside thread %s".format(Thread.currentThread.getName))
	}
	
	def display(surface:Surface) {
	    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)

//println("Display Inside thread %s".format(Thread.currentThread.getName))

	    camera.viewLookAt

		// Ground

	  	gl.enable(gl.BLEND)
	    groundShader.use
	    light1.uniform(groundShader, camera)
	    groundShader.uniform("color", Rgba.white)
	    camera.uniformMVP(groundShader)
	    ground.draw(groundMesh.drawAs)
		gl.disable(gl.BLEND)

		gl.frontFace(gl.CCW)
	    thingShader.use
	    thingShader.uniform("color", Rgba(0.375,0.441,0.5,1))
	   	light1.uniform(thingShader, camera)
	    camera.uniformMVP(thingShader)
	    thing.draw(thingMesh.drawAs)
		gl.frontFace(gl.CW)

	    // Ok

	    surface.swapBuffers
	    gl.checkErrors
	}

	def animate() {
		camera.rotateViewHorizontal(0.01)
	}
}