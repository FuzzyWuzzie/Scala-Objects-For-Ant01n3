package org.sofa.opengl.test

import scala.util.Random
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.math._
import org.sofa.nio._
import org.sofa.opengl._

import javax.media.opengl.glu._
import javax.media.opengl._
import scala.math._

object PixelFlow3 {
	def main(args:Array[String]):Unit = {
	    (new PixelFlow3).test
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

class PixelFlow3 extends WindowAdapter with GLEventListener {
    var gl:SGL = null
    var cube:VertexArray = null
    var cubeShad:ShaderProgram = null
    
    val cubeVert = FloatBuffer(
        // Front
        -0.5f, -0.5f,  0.5f,			// 0
         0.5f, -0.5f,  0.5f,			// 1
         0.5f,  0.5f,  0.5f,			// 2
        -0.5f,  0.5f,  0.5f,			// 3
        // Right
         0.5f, -0.5f,  0.5f,			// 4
         0.5f, -0.5f, -0.5f,			// 5
         0.5f,  0.5f, -0.5f,			// 6
         0.5f,  0.5f,  0.5f,			// 7
        // Back
         0.5f, -0.5f, -0.5f,			// 8
        -0.5f, -0.5f, -0.5f,			// 9
        -0.5f,  0.5f, -0.5f,		 	// 10
         0.5f,  0.5f, -0.5f,			// 11
        // Left
        -0.5f, -0.5f, -0.5f,			// 12
        -0.5f, -0.5f,  0.5f,			// 13
        -0.5f,  0.5f,  0.5f,			// 14
        -0.5f,  0.5f, -0.5f,			// 15
        // Top
        -0.5f,  0.5f,  0.5f,			// 16
         0.5f,  0.5f,  0.5f,			// 17
         0.5f,  0.5f, -0.5f,			// 18
        -0.5f,  0.5f, -0.5f,			// 19
        // Bottom
        -0.5f, -0.5f, -0.5f,			// 20
         0.5f, -0.5f, -0.5f,			// 21
         0.5f, -0.5f,  0.5f,			// 22
        -0.5f, -0.5f,  0.5f)			// 23
         
    val cubeNorm = FloatBuffer(
    	// Front
         0,  0,  1,
         0,  0,  1,
         0,  0,  1,
         0,  0,  1,
    	// Right
         1,  0,  0,
         1,  0,  0,
         1,  0,  0,
         1,  0,  0,
    	// Back
         0,  0, -1,
         0,  0, -1,
         0,  0, -1,
         0,  0, -1,
    	// Left
        -1,  0,  0,
        -1,  0,  0,
        -1,  0,  0,
        -1,  0,  0,
    	// Top
         0,  1,  0,
         0,  1,  0,
         0,  1,  0,
         0,  1,  0,
    	// Bottom
         0, -1,  0,
         0, -1,  0,
         0, -1,  0,
         0, -1,  0)
        
    val cubeClr = //PixelFlow3.randomColors(24)
        
        FloatBuffer(
        // Front
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        // Right
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        // Back
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        // Left
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        // Top
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        // Bottom
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f)
        
    val cubeInd = IntBuffer(
            // Front
            0, 1, 2,
            0, 2, 3,
            // Right
            4, 5, 6,
            4, 6, 7,
            // Back
            8, 9, 10,
            8, 10, 11,
            // Left
            12, 13, 14,
            12, 14, 15,
            // Top
            16, 17, 18,
            16, 18, 19,
            // Bottom
            20, 21, 22,
            20, 22, 23)
    
    val projection:Matrix4 = new NioBufferMatrix4
    
    val modelview = new MatrixStack(new NioBufferMatrix4)
    
    def test() {
//        val prof = GLProfile.get(GLProfile.GL3)
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
//        gl = new backend.SGLJogl3(win.getGL.getGL3, GLU.createGLU)
        gl = new backend.SGLJogl2ES2(win.getGL.getGL2ES2, GLU.createGLU)
        
        gl.printInfos
        gl.clearColor(0f, 0f, 0f, 0f)
        gl.clearDepth(1f)
        gl.enable(gl.DEPTH_TEST)
        gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CCW)
    
        cubeShad = new ShaderProgram(gl, "gouraud shader",
                new VertexShader(gl, "src-scala/org/sofa/opengl/shaders/es2/pixelFlow3VertexShader.glsl"),
                new FragmentShader(gl, "src-scala/org/sofa/opengl/shaders/es2/pixelFlow3FragmentShader.glsl"))
//                new VertexShader(gl, "src-scala/org/sofa/opengl/shaders/pixelFlow3VertexShader.glsl"),
//                new FragmentShader(gl, "src-scala/org/sofa/opengl/shaders/pixelFlow3FragmentShader.glsl"))

        val p = cubeShad.getAttribLocation("position")
        val c = cubeShad.getAttribLocation("color")
        val n = cubeShad.getAttribLocation("normal")

        projection.setIdentity
        projection.frustum(-1, 1, -1, 1, 1, 20)
        cubeShad.uniformMatrix("projection", projection)

        cube = new VertexArray(gl, cubeInd, (p, 3, cubeVert), (c, 4, cubeClr), (n, 3, cubeNorm))
    }
    
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) {
        gl.viewport(0, 0, width, height)
    }
    
    var eyeX=0.0
    var eyeY=0.0
    var eyeZ=0.0
    var eyeD=0.0

    def display(win:GLAutoDrawable) {
        animate

        gl.checkErrors
        gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
     
        cubeShad.use
        modelview.setIdentity
        modelview.lookAt(eyeX, eyeY, eyeZ, 0, 0, 0, 0, 1, 0)

        cubeShad.uniform("lightDir", (modelview.top * new ArrayVector4(0, -0.2, 1, 0)))
        cubeShad.uniform("lightIntensity", 1.0f)
        cubeShad.uniform("ambientIntensity", 0.05f)
        
        modelview.translate(0, 1, 0)
        cubeShad.uniformMatrix("modelview", modelview.top)
        cubeShad.uniformMatrix("nmodelview", modelview.top.top3x3)
        cube.drawTriangles
        
        modelview.translate(0, -2, 0)
        modelview.rotate(180, 0, 1, 0)
        cubeShad.uniformMatrix("modelview", modelview.top)
        cubeShad.uniformMatrix("nmodelview", modelview.top.top3x3)
        cube.drawTriangles
        
        //win.swapBuffers // Automatic
    }
    
    def animate() {
        eyeD += Pi/100.0;
        eyeX = cos(eyeD)*2
        eyeZ = sin(eyeD)*2
        if(eyeD > 2*Pi) eyeD = 0
    }
    
    def dispose(win:GLAutoDrawable) {}
}