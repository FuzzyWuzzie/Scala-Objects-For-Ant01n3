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
    
    val planeMesh = new PlaneMesh(2, 2, 4, 4)
    val cubeMesh = new CubeMesh(1)
    
    var cubeShad:ShaderProgram = null
    
    val projection:Matrix4 = new Matrix4
    val modelview = new MatrixStack(new Matrix4)
    
    var width = 800.0
    var height = 600.0
    
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
        win.setSize(width.toInt, height.toInt)
        win.setTitle("Basic OpenGL setup")
        win.setVisible(true)
        
        anim.start
    }
    
    override def windowDestroyNotify(ev:WindowEvent) { sys.exit }
    
    def init(win:GLAutoDrawable) {
//        gl = new backend.SGLJogl3(win.getGL.getGL3, GLU.createGLU)
        gl = new backend.SGLJogl2ES2(win.getGL.getGL2ES2, GLU.createGLU, win.getContext.getGLSLVersionString)

        Shader.path  += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"

        
        gl.printInfos
        gl.clearColor(0f, 0f, 0f, 0f)
        gl.clearDepth(1f)
        gl.enable(gl.DEPTH_TEST)
        gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CW)
    
        cubeShad = new ShaderProgram(gl, "phong shader",
                new VertexShader(gl, "phong", "es2/pixelFlow4VertexShader.glsl"),
                new FragmentShader(gl, "phong", "es2/pixelFlow3FragmentShader.glsl"))
//                new VertexShader(gl, "src-scala/org/sofa/opengl/shaders/pixelFlow4VertexShader.glsl"),
//                new FragmentShader(gl, "src-scala/org/sofa/opengl/shaders/pixelFlow3FragmentShader.glsl"))

        projection.setIdentity
        projection.frustum(-1, 1*(width/height), -1, 1*(width/height), 1, 20)
        cubeShad.uniformMatrix("projection", projection)
        
        val p = cubeShad.getAttribLocation("position")
        val c = cubeShad.getAttribLocation("color")
        val n = cubeShad.getAttribLocation("normal")
        
        cube  = cubeMesh.newVertexArray( gl, (VertexAttribute.Vertex.toString, p), (VertexAttribute.Color.toString, c), (VertexAttribute.Normal.toString, n))
        plane = planeMesh.newVertexArray(gl, (VertexAttribute.Vertex.toString, p), (VertexAttribute.Color.toString, c), (VertexAttribute.Normal.toString, n))
//        cube = new VertexArray(gl, cubeMesh.indices, ("vertices", p, 3, cubeMesh.vertices), ("colors", c, 4, cubeMesh.colors), ("normals", n, 3, cubeMesh.normals))
//        plane = new VertexArray(gl, planeMesh.indices, ("vertices", p, 3, planeMesh.vertices), ("colors", c, 4, planeMesh.colors), ("normals", n, 3, planeMesh.normals))
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
        gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
     
        cubeShad.use
        modelview.setIdentity
        modelview.lookAt(eyeX, eyeY, eyeZ, 0, 0, 0, 0, 1, 0)
        
        cubeShad.uniform("lightPos", (modelview.top * new Vector4(0, 1, -2, 1)))
        cubeShad.uniform("lightIntensity", 1.0f)
        cubeShad.uniform("ambientIntensity", 0.05f)

        modelview.push
        modelview.translate(0, 1, 0)
        cubeShad.uniformMatrix("modelview", modelview.top)
        cubeShad.uniformMatrix("nmodelview", modelview.top.top3x3)
        cube.draw(cubeMesh.drawAs(gl))
        modelview.pop
        
        modelview.push
        modelview.translate(0, -1, 0)
        modelview.rotate(Pi, 0, 1, 0)
        cubeShad.uniformMatrix("modelview", modelview.top)
        cubeShad.uniformMatrix("nmodelview", modelview.top.top3x3)
        cube.draw(cubeMesh.drawAs(gl))
        modelview.pop
        
        modelview.push
        modelview.translate(0, -1.5, 0)
        cubeShad.uniformMatrix("modelview", modelview.top)
        cubeShad.uniformMatrix("nmodelview", modelview.top.top3x3)
        plane.draw(cubeMesh.drawAs(gl))
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