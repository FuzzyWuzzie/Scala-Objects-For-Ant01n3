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
    
    val clearColor = Rgba.black
    var nmapShader:ShaderProgram = null
    var plainShader:ShaderProgram = null
    var boneShader:ShaderProgram = null
    var light1 = Vector4(2, 2, 2, 1)
    var tex1uv:Texture = null
    var tex1nm:Texture = null

// Go
        
    build
    
    private def build() {
	    camera   = Camera()
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
	    surface        = new org.sofa.opengl.backend.SurfaceNewt(this, camera, "Bistouquette", caps)
	}
    
// Rendering
    
	def initializeSuface(gl:SGL, surface:Surface) {
	    Shader.includePath += "src-scala/org/sofa/opengl/shaders/"
	    
	    initGL(gl)
	    initSkeleton
        initShaders
	    initGeometry
	    initTextures
	    
	    camera.viewCartesian(0, 5, 5)
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
	    nmapShader = new ShaderProgram(gl,
	            new VertexShader(gl, "stock/phongNmap.vert"),
	            new FragmentShader(gl, "stock/phongNmap.frag"))
	    
	    plainShader = new ShaderProgram(gl,
	            new VertexShader(gl, "uniformColor.vert"),
	            new FragmentShader(gl, "uniformColor.frag"))
	    
	    boneShader = new ShaderProgram(gl,
	            new VertexShader(gl, "bonePhong.vert"),
	            new FragmentShader(gl, "bonePhong.frag"))
	
	    boneShader.uniform("bone[0].color", skeleton.color)
	    boneShader.uniform("bone[1].color", skeleton(0).color)
	    boneShader.uniform("bone[2].color", (skeleton(0))(0).color)
	    
	    plainShader.uniform("uniformColor", Rgba.white)
	}
	
	protected def initGeometry() {
	    tubeMesh.setBottomDiskColor(Rgba.red)
	    tubeMesh.setCylinderColor(Rgba.blue)
	    tubeMesh.setTopDiskColor(Rgba.red)
	    
	    plane = planeMesh.newVertexArray(gl)
	    tube  = new VertexArray(gl, tubeMesh.indices,
	            	(boneShader.getAttribLocation("position"),  3, tubeMesh.vertices),
	            	(boneShader.getAttribLocation("normal"),    3, tubeMesh.normals),
	            	(boneShader.getAttribLocation("boneIndex"), 1, tubeMesh.bones))
	    bone  = boneMesh.newVertexArray(gl)
	}
	
	protected def initTextures() {
	    tex1uv = new Texture(gl, "textures/grey-concrete-texture.jpg", true)
	    tex1uv.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    tex1uv.wrap(gl.REPEAT)

	    tex1nm = new Texture(gl, "textures/Sample64.png", true)
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
	    
	    gl.polygonMode(gl.FRONT_AND_BACK, gl.FILL)
	    boneShader.use
	    useLights(boneShader)
	    camera.pushpop {
	        skeleton.drawModel(gl, camera, tubeMesh, tube, boneShader)	        
	    }
	    
	    surface.swapBuffers
	    gl.checkErrors
	    animate
	}
	
	var join0 = 0.0
	var join1 = 0.0
	var join2 = 0.0
	var join1Dir = +0.01
	var join2Dir = +0.01
	
	def animate() {
	    join1 += join1Dir
	    join2 += join2Dir
	    
	    if(join1 > Pi/8 || join1 < -Pi/8) join1Dir = -join1Dir
	    if(join2 > Pi/8 || join2 < -Pi/8) join2Dir = -join2Dir

	    skeleton.identity
	    //skeleton.scale(1, 3, 1)
	    skeleton(0).identity
	    skeleton(0).rotate(join1, 0, 0, 1)
//	    skeleton(0).scale(1, 2, 1)
	    skeleton(0)(0).identity
	    skeleton(0)(0).rotate(join2, 0, 0, 1)
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