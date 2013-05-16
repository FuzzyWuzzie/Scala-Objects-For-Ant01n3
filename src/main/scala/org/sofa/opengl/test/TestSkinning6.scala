package org.sofa.opengl.test

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

object TestSkinning6 {
    def main(args:Array[String]):Unit = (new TestSkinning6).test
}

class TestSkinning6 extends SurfaceRenderer {
// General
    
    var gl:SGL = null
    var surface:Surface = null
	
// View
    
    var camera:Camera = null
    var ctrl:BasicCameraController = null
    
// Geometry

    var ground:VertexArray = null
    var thing:VertexArray = null
    
    val groundMesh = new PlaneMesh(2, 2, 6 , 6)
    var thingMesh:Mesh = null
    val boneMesh = new BoneMesh()
    
    var skeleton:SkelBone = null
    
// Shading
    
    val clearColor = Rgba.Grey30
    
    var groundShader:ShaderProgram = null
    var thingShader:ShaderProgram = null
    var boneShader:ShaderProgram = null
    
    var light1 = new HemisphereLight(0, 10, 0, Rgba.White, Rgba.Grey30) //WhiteLight(0, 2, 2,  5f, 16f, 0.1f)
    
    // var groundColor:Texture = null
    // var groundNMap:Texture = null
    // var thingColor:Texture = null
    // var thingNMap:Texture = null

// Go
        
    def test() {
	    camera   = Camera(); camera.viewportPx(1280,800)
	    val caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2))
	    
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(4)

        ctrl           = new MyCameraController(camera, light1.pos)
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
	    initTextures
        initShaders
        initModels
	    initGeometry
	    
	    camera.viewCartesian(0, 10, 10)
	    camera.setFocus(0, 2, 0)
	    reshape(surface)
	}

	protected def initGL(sgl:SGL) {
	    gl = sgl

println("initGL Inside thread %s".format(Thread.currentThread.getName))

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
	    boneShader   = ShaderProgram(gl, "bone",   "es2/bone.vert.glsl",                "es2/bone.frag.glsl")
		groundShader = ShaderProgram(gl, "ground", "es2/hemiLightUniClr.vert.glsl",     "es2/hemiLightUniClr.frag.glsl")
		thingShader  = ShaderProgram(gl, "thing",  "es2/hemiLightUniClrBone.vert.glsl", "es2/hemiLightUniClrBone.frag.glsl")
	}

	protected def initModels() {
		val model = new ColladaFile("Cross.dae")

		model.library.geometry("Cross").get.mesh.mergeVertices(true)
		model.library.geometry("Cross").get.mesh.blenderToOpenGL(true)

		// Build a SOFA bone hierarchy and a SOFA mesh from the Collada data.

		model.library.visualScene("Scene").get.blenderToOpenGL(true)

	    skeleton  = model.library.visualScene("Scene").get.toSkeleton("Armature", model.library.controller("Armature").get)
		thingMesh = model.library.geometry("Cross").get.mesh.toMesh

		// Ask the mesh to auto compute tangents that are sadly lacking from Collada export of Blender :'(

		thingMesh.asInstanceOf[EditableMesh].autoComputeTangents(true)

		skeleton.color = Rgba(1.0, 0.1, 0.0, 1)
		skeleton("Body").color = Rgba(1.0, 0.6, 0.1, 1)
		skeleton("Body")("Chest").color = Rgba(0.7, 0.2, 0.7, 1)
		skeleton("Body")("Chest")("LeftArm").color = Rgba(0.3, 0.1, 1.0, 1)
		skeleton("Body")("Chest")("RightArm").color = Rgba(0.1, 0.4, 1.0, 1)
		skeleton("Body")("Chest")("Head").color = Rgba(0.0, 0.8, 1.0, 1)
	}
	
	protected def initGeometry() {
		import VertexAttribute._

//		groundMesh.setTextureRepeat(30, 30)

	    ground        = groundMesh.newVertexArray(gl, groundShader, Vertex -> "position", Normal -> "normal")//, Tangent -> "tangent", TexCoord -> "texCoords")
	    thing         = thingMesh.newVertexArray( gl, thingShader,  Vertex -> "position", Normal -> "normal", Bone -> "boneIndex", Weight -> "boneWeight")
	    SkelBone.bone = boneMesh.newVertexArray(  gl, boneShader,   Vertex -> "position")
	}
	
	protected def initTextures() {
	    // groundColor = new Texture(gl, "textures/Ground.png", true)
	    // groundColor.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    // groundColor.wrap(gl.REPEAT)

	    // groundNMap = new Texture(gl, "textures/GroundNMap.png", true)
	    // groundNMap.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    // groundNMap.wrap(gl.REPEAT)

	    // thingColor = new Texture(gl, "textures/BruceColor.png", true)
	    // thingColor.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    // thingColor.wrap(gl.REPEAT)

	    // thingNMap = new Texture(gl, "textures/BruceNMap.png", true)
	    // thingNMap.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    // thingNMap.wrap(gl.CLAMP_TO_EDGE)
	}
	
	def reshape(surface:Surface) {
	    camera.viewportPx(surface.width, surface.height)
        var ratio = camera.viewportRatio
        
        gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
        camera.frustum(-ratio, ratio, -1, 1, 2)

println("Reshape Inside thread %s".format(Thread.currentThread.getName))
	}
	
	def display(surface:Surface) {
	    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)

println("Display Inside thread %s".format(Thread.currentThread.getName))

	    camera.viewLookAt

		// Ground

	  	gl.enable(gl.BLEND)
	    groundShader.use
	    light1.uniform(groundShader, camera)
	    groundShader.uniform("color", Rgba.White)
	    camera.uniformMVP(groundShader)
	    ground.draw(groundMesh.drawAs)
		gl.disable(gl.BLEND)

		animate

		gl.frontFace(gl.CCW)
	    thingShader.use
	    thingShader.uniform("color", Rgba(0.375,0.441,0.5,1))
	   	light1.uniform(thingShader, camera)
	   	skeleton.uniform(thingShader)
	    camera.uniformMVP(thingShader)
	    thing.draw(thingMesh.drawAs)
		gl.frontFace(gl.CW)

 		// Skeleton

 		gl.disable(gl.DEPTH_TEST)
	    boneShader.use
	    skeleton.drawSkeleton(gl, camera, boneShader, "color")
 		gl.enable(gl.DEPTH_TEST)

	    // Ok

	    surface.swapBuffers
	    gl.checkErrors
	}

	class Join(var value:Double, var dir:Double, val min:Double, val max:Double) {
		def animate() {
			value += dir

			if(value > max || value < min) dir = -dir
		}
	}

	var join0 = new Join(0, 0.005, -Pi/20, Pi/20)
	var join1 = new Join(0, 0.005, -Pi/4, Pi/4)
	var join2 = new Join(0, 0.05, -Pi/4, Pi/4)
	var join3 = new Join(0, 0.07, -Pi/10, Pi/10)
	var join4 = new Join(0, 0.07, -Pi/3, Pi/3)
	
	def animate() {
	    join0.animate
	    join1.animate
	    join2.animate
	    join3.animate
	    join4.animate
	    
	    // Body
	    skeleton.identity
//	    skeleton.rotate(join0, 1, 0, 0)
		
		val body = skeleton("Body")

	    body.identity
	    body.rotate(join1.value, 0, 0, 1)

	    val chest = body("Chest")

	    chest.identity
	    chest.rotate(join2.value, 1, 0, 0)

	    // Arms
	    chest("LeftArm").identity
	    chest("LeftArm").rotate(join3.value, 1, 0, 0)
	    chest("RightArm").identity
	    chest("RightArm").rotate(join3.value, 1, 0, 0)
	    
	    // Head
	    chest("Head").identity
	    chest("Head").rotate(join4.value, 0, 1, 0)
	}
	
	def useTextures(shader:ShaderProgram, color:Texture, nmap:Texture) {
	   	color.bindTo(gl.TEXTURE0)
	    shader.uniform("texColor", 0)
	    nmap.bindTo(gl.TEXTURE1)
	    shader.uniform("texNormal", 1)
	}
}