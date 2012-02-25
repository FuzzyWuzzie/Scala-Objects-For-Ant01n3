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
    var camCtrl = new CameraController(camera)
    var fps = 30
    
// Geometry
    
    val planeMesh = new Plane(2, 2, 4 , 4)
    val tubeMesh = new Cylinder(0.5f, 1, 16, 3)
    val boneMesh = new BoneMesh()
    var plane:VertexArray = null
    var tube:VertexArray = null
    var bone:VertexArray = null
    
    var skeleton:Bone = null
    
// Shading
    
    val clearColor = Rgba.black
    var shader1:ShaderProgram = null
    var shader2:ShaderProgram = null
    var light1 = Vector4(2, 2, 2, 1)
    var tex1uv:Texture = null
    var tex1nm:Texture = null

// Go
        
    build
    
    private def build() {
	    camera = Camera()
	    caps   =  new GLCapabilities(GLProfile.get(GLProfile.GL3))

		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(4)

	    initSurface    = initializeSuface
	    frame          = display
	    surfaceChanged = reshape
	    close          = { surface => exit }
	    surface        = new org.sofa.opengl.backend.SurfaceNewt(this, camera, "Skining test", caps)
	}
    
// Rendering
    
	def initializeSuface(gl:SGL, surface:Surface) {
	    initGL(gl)
        initShaders
	    initGeometry
	    initTextures
	    
	    camera.viewCartesian(2, 2, 2)
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
	
	protected def initShaders() {
	    shader1 = new ShaderProgram(gl,
	            new VertexShader(gl, "src-scala/org/sofa/opengl/shaders/stock/phongNmap.vert"),
	            new FragmentShader(gl, "src-scala/org/sofa/opengl/shaders/stock/phongNmap.frag"))
	    
	    shader2 = new ShaderProgram(gl,
	            new VertexShader(gl, "src-scala/org/sofa/opengl/shaders/stock/plainColor.vert"),
	            new FragmentShader(gl, "src-scala/org/sofa/opengl/shaders/stock/plainColor.frag"))
	}
	
	protected def initGeometry() {
	    tubeMesh.setBottomDiskColor(Rgba.red)
	    tubeMesh.setCylinderColor(Rgba.blue)
	    tubeMesh.setTopDiskColor(Rgba.red)
	    
	    plane = planeMesh.newVertexArray(gl)
	    tube  = tubeMesh.newVertexArray(gl)
	    bone  = boneMesh.newVertexArray(gl)
	    
	    skeleton = new Bone(0)
	    skeleton.addChild(1)
	    skeleton.scale(0.3333, 0.3333, 0.3333)
	    skeleton.children(0).translate(0, 1, 0)
	    skeleton.children(0).addChild(2)
	    skeleton.children(0).children(0).translate(0, 1, 0)
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
        camera.frustum(-ratio, ratio, -1, 1, 1)
	}
	
	def display(surface:Surface) {
	    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
	    
	    shader1.use
	    camera.setupView
	    useLights(shader1)
	    useTextures(shader1)
	    camera.uniformMVP(shader1)
	    gl.polygonMode(gl.FRONT_AND_BACK, gl.FILL)
	    plane.draw(planeMesh.drawAs)
	    
	    shader2.use
	    gl.polygonMode(gl.FRONT_AND_BACK, gl.LINE)
	    camera.pushpop {
	        //camera.translateModel(0, 1, 0)
	        //camera.rotateModel(Pi/2, 1, 0, 0)
	        //camera.translateModel(0, -1, 0)
	        camera.scaleModel(1, 3, 1)
	        camera.uniformMVP(shader2)
	        tube.draw(tubeMesh.drawAs)
	        
	        skeleton.drawSkeleton(gl, camera, shader2)
	    }
	    
	    surface.swapBuffers
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