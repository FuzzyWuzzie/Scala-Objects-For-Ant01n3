package org.sofa.opengl.test

import scala.math._
import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._
import org.sofa.nio._
import org.sofa.math._
import org.sofa.opengl._
import org.sofa.opengl.surface._
import org.sofa.opengl.mesh._
import org.sofa.opengl.mesh.skeleton._

object Skinning {
    def main(args:Array[String]):Unit = (new Skinning)
}

class Skinning extends SurfaceRenderer {
// General
    
    var gl:SGL = null
    var surface:Surface = null
    var caps:GLCapabilities = null
	
// View
    
    var camera:Camera = null
    var fps = 30
    
// Geometry
    
    val planeMesh = new Plane(2, 2, 4 , 4)
    val tubeMesh = new Cylinder(0.5f, 3, 16, 3)
    val boneMesh = new BoneMesh()
    var plane:VertexArray = null
    var tube:VertexArray = null
    var bone:VertexArray = null
    
    var skeleton:Bone = null
    
// Shading
    
    val clearColor = Rgba.grey30
    var nmapShader:ShaderProgram = null
    var plainShader:ShaderProgram = null
    var boneShader:ShaderProgram = null
    var light1 = Vector4(2, 2, 2, 1)
    var tex1uv:Texture = null
    var tex1nm:Texture = null

// Go
        
    build()

    private def build() {
	    camera   = Camera(); camera.viewportPx(600,800)
	    caps     = new GLCapabilities(GLProfile.get(GLProfile.GL3))
	    val ctrl = new BasicCameraController(camera)
	    
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(4)

	    initSurface    = initializeSuface
	    frame          = display
	    surfaceChanged = reshape
	    close          = { surface => exit }
	    key            = ctrl.key
	    motion         = ctrl.motion
	    scroll         = ctrl.scroll
	    surface        = new org.sofa.opengl.backend.SurfaceNewt(this, camera, "Bistouquette", caps,
	    					org.sofa.opengl.backend.SurfaceNewtGLBackend.GL3)
	}
    
// Rendering
    
	def initializeSuface(gl:SGL, surface:Surface) {
	    Shader.includePath += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
	    
	    initGL(gl)
	    initSkeleton
        initShaders
	    initGeometry
	    initTextures
	    
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
Console.err.println("A")
	    nmapShader = new ShaderProgram(gl, "phong n-map",
	            new VertexShader(gl, "nmap", "stock/phongNmap.vert.glsl"),
	            new FragmentShader(gl, "nmap", "stock/phongNmap.frag.glsl"))
	    
Console.err.println("B")
	    plainShader = new ShaderProgram(gl, "plain", 
	            new VertexShader(gl, "uniform", "uniformColor.vert"),
	            new FragmentShader(gl, "uniform", "uniformColor.frag"))
	    
Console.err.println("C") // The error seems to appear in the vertex shader at compilation.
	    boneShader = new ShaderProgram(gl, "phong n-map with bones",
	            new VertexShader(gl, "bone", "bonePhong.vert.glsl"),
	            new FragmentShader(gl, "bone", "bonePhong.frag.glsl"))
Console.err.println("D")

	    boneShader.uniform("bone[0].color", skeleton.color)
	    boneShader.uniform("bone[1].color", skeleton(0).color)
	    boneShader.uniform("bone[2].color", (skeleton(0))(0).color)
	    
	    plainShader.uniform("uniformColor", Rgba.white)
	}
	
	protected def initGeometry() {
	    tubeMesh.setBottomDiskColor(Rgba.red)
	    tubeMesh.setCylinderColor(Rgba.blue)
	    tubeMesh.setTopDiskColor(Rgba.red)
	    
	    plane = planeMesh.newVertexArray(gl)//new VertexArray(gl, planeMesh.indices,
//	    			(nmapShader.getAttribLocation("pos"),     3, planeMesh.vertices),
//	    			(nmapShader.getAttribLocation("normal"),  3, planeMesh.normals),
//	    			(nmapShader.getAttribLocation("tangent"), 3, planeMesh.normals),
//	    			(nmapShader.getAttribLocation("texPos"),  2, planeMesh.normals))
	    tube  = new VertexArray(gl, tubeMesh.indices,
	            	("vertices", boneShader.getAttribLocation("position"),  3, tubeMesh.vertices),
	            	("normals", boneShader.getAttribLocation("normal"),    3, tubeMesh.normals),
	            	("bones", boneShader.getAttribLocation("boneIndex"), 1, tubeMesh.bones))
	    bone  = boneMesh.newVertexArray(gl)//new VertexArray(gl, boneMesh.indices,
	    		//	(plainShader.getAttribLocation("position"), 3, boneMesh.vertices))
	}
	
	protected def initTextures() {
	    tex1uv = new Texture(gl, "textures/stone_wall__.jpg", true)
	    tex1uv.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    tex1uv.wrap(gl.REPEAT)

	    tex1nm = new Texture(gl, "textures/stone_wall_normal_map__.jpg", true)
	    tex1nm.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    tex1nm.wrap(gl.REPEAT)
	}
	
	def reshape(surface:Surface) {
	    camera.viewportPx(surface.width, surface.height)
        var ratio = camera.viewportRatio
        
        gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
        camera.frustum(-ratio, ratio, -1, 1, 2)
	}
	
	def display(surface:Surface) {
	    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
	    
	    nmapShader.use
	    camera.setupView
	    useLights(nmapShader)
	    useTextures(nmapShader)
	    camera.uniformMVP(nmapShader)
	    gl.polygonMode(gl.FRONT_AND_BACK, gl.FILL)
	    plane.draw(planeMesh.drawAs)
	    
	    gl.polygonMode(gl.FRONT_AND_BACK, gl.LINE)
	    plainShader.use
	    camera.pushpop {
	        skeleton.drawSkeleton(gl, camera, plainShader, "uniformColor")
	    }
	    
	    boneShader.use
	    useLights(boneShader)
	    camera.pushpop {
	    	setBonesColor(1)
	        skeleton.drawModel(gl, camera, tubeMesh, tube, boneShader)	        
	    	gl.polygonMode(gl.FRONT_AND_BACK, gl.FILL)
	    	setBonesColor(0.5f)
	    	gl.enable(gl.BLEND)
	    	skeleton.drawModel(gl, camera, tubeMesh, tube, boneShader)
	    	gl.disable(gl.BLEND)
	    }
	    
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
	    shader.uniform("lights[0].pos", Vector3(camera.modelview.top * light1))
	    shader.uniform("lights[0].intensity", 5f)
	    shader.uniform("lights[0].ambient", 0.1f)
	    shader.uniform("lights[0].specular", 16f)
	}
	
	def useTextures(shader:ShaderProgram) {
	    tex1uv.bindTo(gl.TEXTURE0)
	    shader.uniform("tex.color", 0)
	    tex1nm.bindTo(gl.TEXTURE1)
	    shader.uniform("tex.normal", 1)
	}
}