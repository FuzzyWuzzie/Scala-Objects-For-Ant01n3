package org.sofa.opengl.test

import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.math._
import org.sofa.nio._
import org.sofa.opengl._

import javax.media.opengl.glu._
import javax.media.opengl._
import scala.math._

object PixelFlow2 {
	def main(args:Array[String]):Unit = {
	    (new PixelFlow2).test
	}
}

class PixelFlow2 extends WindowAdapter with GLEventListener {
    var gl:SGL = null
    var cube:VertexArray = null
    var cubeShad:ShaderProgram = null
    
    val cubeVert = FloatBuffer( 
        -0.5f, -0.5f,  0.5f,
         0.5f, -0.5f,  0.5f,
         0.5f,  0.5f,  0.5f,
        -0.5f,  0.5f,  0.5f,
        -0.5f, -0.5f, -0.5f,
         0.5f, -0.5f, -0.5f,
         0.5f,  0.5f, -0.5f,
        -0.5f,  0.5f, -0.5f)
         
    val cubeClr = FloatBuffer(
        1.0f, 0.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 1.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        0.5f, 0.5f, 0.5f, 1.0f)
        
    val cubeInd = IntBuffer(
            // Front
            0, 1, 2,
            0, 2, 3,
            // Right
            1, 5, 6,
            1, 6, 2,
            // Back
            5, 4, 7,
            5, 7, 6,
            // Left
            4, 0, 3,
            4, 3, 7,
            // Top
            3, 2, 6,
            3, 6, 7,
            // Bottom
            4, 5, 0,
            5, 1, 0)
    
    val projection:Matrix4 = new NioBufferMatrix4
    
    val modelview = new MatrixStack(new NioBufferMatrix4)
    
// As comments, under are the versions for OpenGL 3, OpenGL ES 2 is very close.
    
//    val vertexShader = Array[String](
//    		"#version 330\n",
//    		"layout(location=0) in vec4 in_Position;\n",
//    		"layout(location=1) in vec4 in_Color;\n",
//    		"uniform mat4 projection;\n",
//    		"uniform mat4 modelview;\n",
//    		"out vec4 ex_Color;\n",
//
//    		"void main(void) {\n",
//    		"	vec4 p = in_Position;\n",
//    		"   p = modelview * p;\n",
//    		"	gl_Position = projection * p;\n",
//    		"	ex_Color = in_Color;\n",
//    		"}\n")
//        
//    val fragmentShader = Array[String](
//    		"#version 330\n",
// 
//    		"in vec4 ex_Color;\n",
//    		"out vec4 out_Color;\n",
// 
//    		"void main(void) {\n",
//    		"	out_Color = ex_Color;\n",
//    		"}\n")

    val vertexShader = Array[String](
    		"#version 120\n",
    		"attribute vec4 in_Position;\n",
    		"attribute vec4 in_Color;\n",
    		"uniform mat4 projection;\n",
    		"uniform mat4 modelview;\n",
    		"varying vec4 ex_Color;\n",

    		"void main(void) {\n",
    		"	vec4 p = in_Position;\n",
    		"   p = modelview * p;\n",
    		"	gl_Position = projection * p;\n",
    		"	ex_Color = in_Color;\n",
    		"}\n")
        
    val fragmentShader = Array[String](
    		"#version 120\n",
 
    		"varying vec4 ex_Color;\n",
 
    		"void main(void) {\n",
    		"	gl_FragColor = ex_Color;\n",
    		"}\n")
    
    def test() {
//		val prof = GLProfile.get(GLProfile.GL3)
        val prof = GLProfile.get(GLProfile.GL2ES2)
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
        win.setSize(800, 600)
        win.setTitle("Basic OpenGL setup")
        win.setVisible(true)
        
        anim.start
    }
    
    override def windowDestroyNotify(ev:WindowEvent) { sys.exit }
    
    def init(win:GLAutoDrawable) {
//		gl = new backend.SGLJogl3(win.getGL.getGL3, GLU.createGLU)
        gl = new backend.SGLJogl2ES2(win.getGL.getGL2ES2, GLU.createGLU)
        
        gl.printInfos
        gl.clearColor(0f, 0f, 0f, 0f)
        gl.clearDepth(1f)
        gl.enable(gl.DEPTH_TEST)
        gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CCW)
        
        cubeShad = new ShaderProgram(gl, "basic shader",
                new VertexShader(gl, "basic", vertexShader),
                new FragmentShader(gl, "basic", fragmentShader))

        projection.setIdentity
        projection.frustum(-1, 1, -1, 1, 1, 20)
        cubeShad.uniformMatrix("projection", projection)
        gl.checkErrors
        
        val p = cubeShad.getAttribLocation("in_Position")
        val c = cubeShad.getAttribLocation("in_Color")
        
        cube = new VertexArray(gl, cubeInd, ("vertices", p, 3, cubeVert), ("colors", c, 4, cubeClr))
    }
    
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) {
        gl.viewport(0, 0, width, height)
    }
    
    var eyeX=0.0
    var eyeY=0.0
    var eyeZ=0.0
    var eyeD=0.0

    def display(win:GLAutoDrawable) {
        gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
        
        cubeShad.use
        modelview.setIdentity
        modelview.lookAt(eyeX, eyeY, eyeZ, 0, 0, 0, 0, 1, 0)
        modelview.translate(0, 1, 0)
        cubeShad.uniformMatrix("modelview", modelview.top)        
        cube.drawTriangles

        modelview.translate(0, -2, 0)
        modelview.rotate(180, 0, 1, 0)
        cubeShad.uniformMatrix("modelview", modelview.top)
        cube.drawTriangles

        modelview.translate(1, 0, 0)
        cubeShad.uniformMatrix("modelview", modelview.top)
        cube.drawTriangles
        
        //win.swapBuffers // Automatic
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