package org.sofa.opengl.test

import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.nio._
import org.sofa.opengl._

import javax.media.opengl.glu._
import javax.media.opengl._

object PixelFlow {
	def main(args:Array[String]):Unit = {
	    (new PixelFlow).test
	}
}

class PixelFlow extends WindowAdapter with GLEventListener {
    var gl:SGL = null
    var triangle:VertexArray = null
    var shadProg:ShaderProgram = null
    
    val vertices = FloatBuffer( 
        -0.8f, -0.8f, 0.0f, 1.0f,
         0.8f, -0.8f, 0.0f, 1.0f,
         0.8f,  0.8f, 0.0f, 1.0f,
        -0.8f,  0.8f, 0.0f, 1.0f)
         
    val colors = FloatBuffer(
        1.0f, 0.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f)
        
    val indices = IntBuffer(0, 1, 2, 0, 2, 3)
    
//    val vertexShader = Array[String](
//    		"#version 330\n",
//    		"layout(location=0) in vec4 in_Position;\n",
//    		"layout(location=1) in vec4 in_Color;\n",
//    		"out vec4 ex_Color;\n",
//
//    		"void main(void) {\n",
//    		"	gl_Position = in_Position;\n",
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
    		"varying vec4 ex_Color;\n",

    		"void main(void) {\n",
    		"	gl_Position = in_Position;\n",
    		"	ex_Color = in_Color;\n",
    		"}\n")
        
    val fragmentShader = Array[String](
    		"#version 120\n",
    		
    		"varying vec4 ex_Color;\n",
 
    		"void main(void) {\n",
    		"	gl_FragColor = ex_Color;\n",
    		"}\n")
    
    def test() {
        val prof = GLProfile.get(GLProfile.GL2ES2)
        val caps = new GLCapabilities(prof)
    
        caps.setDoubleBuffered(true)
        caps.setRedBits(8)
        caps.setGreenBits(8)
        caps.setBlueBits(8)
        caps.setAlphaBits(8)
        
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
    	gl = new backend.SGLJogl2ES2(win.getGL.getGL2ES2, GLU.createGLU, win.getContext.getGLSLVersionString)
        
        gl.printInfos
        gl.clearColor(0f, 0f, 0f, 0f)
        gl.clearDepth(1f)
        gl.enable(gl.DEPTH_TEST)
    
        shadProg = new ShaderProgram(gl, "basic shader",
                new VertexShader(gl, "basic", vertexShader),
                new FragmentShader(gl, "basic", fragmentShader))

        triangle = new VertexArray(gl, indices, ("vertices", 0, 4, vertices), ("colors", 1, 4, colors))
    }
    
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) {
        gl.viewport(0, 0, width, height)
    }
    
    def display(win:GLAutoDrawable) {
        gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
        shadProg.use
        triangle.drawTriangles
        //win.swapBuffers // NOT Needed !!
    }
    
    def dispose(win:GLAutoDrawable) {}
}