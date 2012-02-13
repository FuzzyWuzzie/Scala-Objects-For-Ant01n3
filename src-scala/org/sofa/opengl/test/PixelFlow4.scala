package org.sofa.opengl.test

import org.sofa.math._
import org.sofa.nio._

import org.sofa.opengl.mesh._
import scala.util.Random
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._
import org.sofa.opengl._

import javax.media.opengl.glu._
import javax.media.opengl._
import scala.math._

import GL._
import GL2._
import GL2ES2._
import GL3._ 

object PixelFlow4 {
	def main(args:Array[String]):Unit = {
	    (new PixelFlow4).test
	}
	
	def randomColors(n:Int):FloatBuffer = {
	    val buffer = FloatBuffer(n*4)
	    val random = new Random(1)
	    
	    for(i <- 0 until n) {
	        buffer(i*4+0) = random.nextFloat
	        buffer(i*4+1) = random.nextFloat
	        buffer(i*4+2) = random.nextFloat
	        buffer(i*4+3) = 1.0f
	    }
	    
	    buffer
	}
}

class PixelFlow4 extends WindowAdapter with GLEventListener {
    var gl:SGL = null
    var cube:VertexArray = null
    var plane:VertexArray = null
    
    val planeMesh = new Plane(2, 2, 4, 4)
    val cubeMesh = new Cube(1)
    
    var cubeShad:ShaderProgram = null
    
    val projection:Matrix4 = new ArrayMatrix4
    val modelview = new MatrixStack(new ArrayMatrix4)
    
    var width = 800.0
    var height = 600.0
    
    def test() {
        val prof = GLProfile.get(GLProfile.GL3)
        val caps = new GLCapabilities(prof)
    
        caps.setDoubleBuffered(true)
        caps.setRedBits(8)
        caps.setGreenBits(8)
        caps.setBlueBits(8)
        caps.setAlphaBits(8)
        caps.setNumSamples(4)
        caps.setHardwareAccelerated(true)
        caps.setSampleBuffers(true)
        
        val win = GLWindow.create(caps)
        val anim = new FPSAnimator(win , 60)

        win.addWindowListener(this)
        win.addGLEventListener(this)
        win.setSize(width.toInt, height.toInt)
        win.setTitle("Basic OpenGL setup")
        win.setVisible(true)
        
        anim.start
    }
    
    override def windowDestroyNotify(ev:WindowEvent) { exit }
    
    def init(win:GLAutoDrawable) {
        gl = new SGL(win.getGL.getGL3, GLU.createGLU)
        
        gl.printInfos
        gl.clearColor(0f, 0f, 0f, 0f)
        gl.clearDepth(1f)
        gl.enable(GL_DEPTH_TEST)
        gl.enable(GL_CULL_FACE)
        gl.cullFace(GL_BACK)
        gl.frontFace(GL_CCW)
    
        cubeShad = new ShaderProgram(gl,
                new VertexShader(gl, "src-scala/org/sofa/opengl/shaders/pixelFlow4VertexShader.glsl"),
                new FragmentShader(gl, "src-scala/org/sofa/opengl/shaders/pixelFlow3FragmentShader.glsl"))

        projection.setIdentity
        projection.frustum(-1, 1*(width/height), -1, 1*(width/height), 1, 20)
        cubeShad.uniformMatrix("projection", projection)
        
        cube = new VertexArray(gl, cubeMesh.indices, (3, cubeMesh.vertices), (4, cubeMesh.colors), (3, cubeMesh.normals))
        plane = new VertexArray(gl, planeMesh.indices, (3, planeMesh.vertices), (4, planeMesh.colors), (3, planeMesh.normals))
    }
    
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) {
        this.width = width
        this.height = height
        gl.viewport(0, 0, width, height)
        projection.setIdentity
        projection.frustum(-1, 1*(this.width/this.height), -1, 1*(this.width/this.height), 1, 20)
        cubeShad.uniformMatrix("projection", projection)
    }
    
    var eyeX=0.0
    var eyeY=0.0
    var eyeZ=0.0
    var eyeD=0.0

    def display(win:GLAutoDrawable) {
        gl.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
     
        cubeShad.use
        modelview.setIdentity
        modelview.lookAt(eyeX, eyeY, eyeZ, 0, 0, 0, 0, 1, 0)
        
        cubeShad.uniform("lightPos", (modelview.top * new ArrayVector4(0, 1, -2, 1)))
        cubeShad.uniform("lightIntensity", 1.0f)
        cubeShad.uniform("ambientIntensity", 0.05f)

        modelview.push
        modelview.translate(0, 1, 0)
        cubeShad.uniformMatrix("modelview", modelview)
        cubeShad.uniformMatrix("nmodelview", modelview.top3x3)
        cube.draw(cubeMesh.drawAs)
        modelview.pop
        
        modelview.push
        modelview.translate(0, -1, 0)
        modelview.rotate(Pi, 0, 1, 0)
        cubeShad.uniformMatrix("modelview", modelview)
        cubeShad.uniformMatrix("nmodelview", modelview.top3x3)
        cube.draw(cubeMesh.drawAs)
        modelview.pop
        
        modelview.push
        modelview.translate(0, -1.5, 0)
        cubeShad.uniformMatrix("modelview", modelview)
        cubeShad.uniformMatrix("nmodelview", modelview.top3x3)
        plane.draw(cubeMesh.drawAs)
        modelview.pop
        
        win.swapBuffers
        animate
    }
    
    def animate() {
        eyeD += Pi/100.0;
        eyeX = cos(eyeD)*2
        eyeZ = sin(eyeD)*2
        if(eyeD > 2*Pi) eyeD = 0
    }
    
    def dispose(win:GLAutoDrawable) {}
}