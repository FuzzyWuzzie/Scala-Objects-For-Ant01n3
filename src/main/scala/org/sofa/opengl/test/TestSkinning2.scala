package org.sofa.opengl.test

import scala.math._
import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.nio._
import org.sofa.math.{Rgba, Vector3, Vector4}
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, BoneMesh, EditableMesh, VertexAttribute}
import org.sofa.opengl.mesh.skeleton._

object TestSkinning2 {
    def main(args:Array[String]):Unit = (new TestSkinning2)
}

class TestSkinning2 extends SurfaceRenderer {
// General
    
    var gl:SGL = null
    var surface:Surface = null
	
// View
    
    var camera:Camera = null
    var ctrl:BasicCameraController = null
    
// Geometry

    var ground:VertexArray = null
    var thing:VertexArray = null
    var bone:VertexArray = null
    
    val groundMesh = new PlaneMesh(2, 2, 4 , 4)
    var thingMesh:Mesh = null
    val boneMesh = new BoneMesh()
    
    var skeleton:Bone = null
    
// Shading
    
    val clearColor = Rgba.grey30
    
    var phongShader:ShaderProgram = null
    var nmapShader:ShaderProgram = null
    var plainShader:ShaderProgram = null
    var boneShader:ShaderProgram = null
    
    var light1 = Vector4(2, 2, 2, 1)
    
    var groundColor:Texture = null
    var groundNMap:Texture = null
    var thingColor:Texture = null

// Go
        
    build()

    private def build() {
	    camera   = Camera(); camera.viewportPx(1280,800)
	    val caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2))
	    
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(4)

        ctrl           = new MyCameraController(camera, light1)
	    initSurface    = initializeSuface
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
    
	def initializeSuface(gl:SGL, surface:Surface) {
	    Shader.path += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
	    Texture.path += "/Users/antoine/Documents/Programs/SOFA/textures"
		ColladaFile.path += "/Users/antoine/Documents/Art/Sculptures/Blender/"

	    initGL(gl)
	    initSkeleton
	    initTextures
        initShaders
        initModels
	    initGeometry
	    
	    camera.viewCartesian(0, 5, 5)
	    camera.setFocus(0, 1.5, 0)
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
	}

	protected def initSkeleton() {
	    skeleton = new Bone(0)
	    skeleton.addChild(1)
	    //skeleton.orientationScale(0.3333, 0.3333, 0.3333)
	    skeleton.color = Rgba.red
	    skeleton(0).orientationTranslate(0, 1, 0)
	    skeleton(0).color = Rgba.green
	    skeleton(0).addChild(2)
//	    skeleton(0).rotate(Pi/8, 0, 0, 1)
	    skeleton(0)(0).orientationTranslate(0, 1, 0)
	    skeleton(0)(0).color = Rgba.blue
//	    skeleton(0)(0).rotate(Pi/8, 0, 0, 1)
	}
	
	protected def initShaders() {
	    phongShader = ShaderProgram(gl, "phong tex",   "es2/phonghitex.vert.glsl",   "es2/phonghitex.frag.glsl")
	    nmapShader  = ShaderProgram(gl, "phong n-map", "es2/phonghinmapw.vert.glsl", "es2/phonghinmapw.frag.glsl")
	    plainShader = ShaderProgram(gl, "plain",       "es2/plainColor.vert.glsl",   "es2/plainColor.frag.glsl")
	//	boneShader  = ShaderProgram(gl, "phong bones", "es2/phonghibone.vert.glsl",  "es2/phonghibone.frag.glsl")

	//    boneShader.uniform("bone[0].color", skeleton.color)
	//    boneShader.uniform("bone[1].color", skeleton(0).color)
	//    boneShader.uniform("bone[2].color", (skeleton(0))(0).color)
	    
	//    plainShader.uniform("uniformColor", Rgba.white)
	}

	protected def initModels() {
		val model = new ColladaFile("Armature_001.dae")

Console.err.println("%s".format(model))

		model.library.geometry("Cube").mesh.mergeVertices(true)
		model.library.geometry("Cube").mesh.blenderToOpenGL(true)

		thingMesh = model.library.geometry("Cube").mesh.toMesh

		thingMesh.asInstanceOf[EditableMesh].autoComputeTangents(true)
	}
	
	protected def initGeometry() {
		import VertexAttribute._

		groundMesh.setTextureRepeat(30, 30)

	    ground = groundMesh.newVertexArray(gl, nmapShader, Vertex -> "position", Normal -> "normal", Tangent -> "tangent", TexCoord -> "texCoords")
	    thing   = thingMesh.newVertexArray(gl, phongShader, Vertex -> "position", Normal -> "normal", TexCoord -> "texCoords")
	    
//	    bone  = boneMesh.newVertexArray(gl)
	}
	
	protected def initTextures() {
	    groundColor = new Texture(gl, "textures/Ground.png", true)
	    groundColor.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    groundColor.wrap(gl.REPEAT)

	    groundNMap = new Texture(gl, "textures/GroundNMap.png", true)
	    groundNMap.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    groundNMap.wrap(gl.REPEAT)

	    thingColor = new Texture(gl, "textures/Armature_Color_001.png", true)
	    thingColor.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    thingColor.wrap(gl.REPEAT)

	    // tubeNMap = new Texture(gl, "textures/Armature_NMap.png", true)
	    // tubeNMap.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    // tubeNMap.wrap(gl.REPEAT)
	}
	
	def reshape(surface:Surface) {
	    camera.viewportPx(surface.width, surface.height)
        var ratio = camera.viewportRatio
        
        gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
        camera.frustum(-ratio, ratio, -1, 1, 2)
	}
	
	def display(surface:Surface) {
	    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
	    
	    camera.viewLookAt

/*
	    gl.polygonMode(gl.FRONT_AND_BACK, gl.LINE)
	    plainShader.use
	    camera.pushpop {
	        skeleton.drawSkeleton(gl, camera, plainShader, "uniformColor")
	    }

	    boneShader.use
	    useLights(boneShader)
	    camera.pushpop {
	    	setBonesColor(1)
	        skeleton.drawModel(gl, camera, thingMesh, thing, boneShader)	        
	    	gl.polygonMode(gl.FRONT_AND_BACK, gl.FILL)
	    	setBonesColor(0.5f)
	    	skeleton.drawModel(gl, camera, thingMesh, thing, boneShader)
			gl.disable(gl.BLEND)
	    }
*/	    
		// Thing just phong textured.

		phongShader.use
		useLights(phongShader)
	    thingColor.bindTo(gl.TEXTURE0)
	    phongShader.uniform("texColor", 0)
		gl.frontFace(gl.CCW)
		camera.pushpop {
			camera.translateModel(0, 0.5, 0)
			camera.scaleModel(0.5, 0.5, 0.5)
			camera.uniformMVP(phongShader)
			thing.draw(thingMesh.drawAs)
		}
		gl.frontFace(gl.CW)

		// Ground

	  	gl.enable(gl.BLEND)
	    nmapShader.use
	    useLights(nmapShader)
	    useTextures(nmapShader)
	    camera.uniformMVP(nmapShader)
	    //gl.polygonMode(gl.FRONT_AND_BACK, gl.FILL)
	    ground.draw(groundMesh.drawAs)
		gl.disable(gl.BLEND)
	    
	    // Ok

	    surface.swapBuffers
	    gl.checkErrors
	    animate
	}

	def setBonesColor(alpha:Float) {
	   if(alpha == 1) {
		   boneShader.uniform("bone[0].color", skeleton.color)
		   boneShader.uniform("bone[1].color", skeleton(0).color)
		   boneShader.uniform("bone[2].color", (skeleton(0))(0).color)
	   } else {
		   val c1 = skeleton.color
		   val c2 = skeleton(0).color
		   val c3 = skeleton(0)(0).color
		   boneShader.uniform("bone[0].color", Rgba(c1.red, c1.green, c1.blue, alpha))
		   boneShader.uniform("bone[1].color", Rgba(c2.red, c2.green, c2.blue, alpha))
		   boneShader.uniform("bone[2].color", Rgba(c3.red, c3.green, c3.blue, alpha))
	   }
	}

	var join0 = 0.0
	var join1 = 0.0
	var join2 = 0.0
	var join0Dir = +0.005
	var join1Dir = +0.05
	var join2Dir = +0.1
	
	def animate() {
//	    join0 += join1Dir
	    join1 += join1Dir
	    join2 += join2Dir
	    
//	    if(join0 > Pi/20 || join1 < -Pi/20) join0Dir = -join0Dir
	    if(join1 > Pi/4 || join1 < -Pi/4) join1Dir = -join1Dir
	    if(join2 > Pi/4 || join2 < -Pi/4) join2Dir = -join2Dir

//	    skeleton.identity
//	    skeleton.rotate(join0, 1, 0, 0)
	    skeleton(0).identity
	    skeleton(0).rotate(join1, 0, 0, 1)
	    //skeleton(0).scale(0.7, 1, 0.7)
	    skeleton(0)(0).identity
	    skeleton(0)(0).rotate(join2, 1, 0, 0)
	    //skeleton(0)(0).scale(0.7, 1, 0.7)
	}
	
	def useLights(shader:ShaderProgram) {
	    shader.uniform("light.pos", Vector3(camera.modelview.top * light1))
		shader.uniform("light.intensity", 5f)
		shader.uniform("light.ambient", 0.1f)
		shader.uniform("light.specular", 16f)
	}
	
	def useTextures(shader:ShaderProgram) {
	    groundColor.bindTo(gl.TEXTURE0)
	    shader.uniform("texColor", 0)
	    groundNMap.bindTo(gl.TEXTURE1)
	    shader.uniform("texNormal", 1)
	}
}