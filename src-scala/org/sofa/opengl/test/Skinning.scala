package org.sofa.opengl.test

import scala.math._
import java.awt.Color

import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.nio._
import org.sofa.math._
import org.sofa.opengl._
import org.sofa.opengl.mesh._

import GL._
import GL2._
import GL2ES2._
import GL2GL3._
import GL3._ 

object Skinning {
    def main(args:Array[String]):Unit = (new Skinning).show
}

class Skinning extends WindowAdapter with GLEventListener {
// General
    
    var gl:SGL = null

// View
    
    val camera = Camera()
    var camCtrl = new CameraController(camera)
    var fps = 30
    
// Geometry
    
    val planeMesh = new Plane(2, 2, 4 , 4)
    val tubeMesh = new Cylinder(0.5f, 1, 16, 2)
    val boneMesh = new Bone()
    var plane:VertexArray = null
    var tube:VertexArray = null
    var bone:VertexArray = null
    
// Shading
    
    val clearColor = Color.black
    var shader1:ShaderProgram = null
    var shader2:ShaderProgram = null
    var light1 = Vector4(2, 2, 2, 1)
    var tex1uv:Texture = null
    var tex1nm:Texture = null
    
// Init
    
	def show() {
	    val prof = GLProfile.get(GLProfile.GL3)
	    val caps = new GLCapabilities(prof)
	    
	    caps.setDoubleBuffered(true)
	    caps.setHardwareAccelerated(true)
	    caps.setSampleBuffers(true)
	    caps.setNumSamples(4)
	    
	    val win  = GLWindow.create(caps)
	    val anim = new FPSAnimator(win, fps)
	    
	    win.addWindowListener(this)
	    win.addGLEventListener(this)
	    win.addMouseListener(camCtrl)
	    win.addKeyListener(camCtrl)
	    win.setSize(camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
	    win.setTitle("Skinning")
	    win.setVisible(true)
	    
	    anim.start
	}
    
    override def windowDestroyNotify(ev:WindowEvent) {
	    exit
	}
	
	def init(win:GLAutoDrawable) {
	    gl = new SGL(win.getGL.getGL3, GLU.createGLU)
	    
	    initGL
        initShaders
	    initGeometry
	    initTextures
	    
	    camera.viewCartesian(2, 2, 2)
	    reshape(null, 0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
	}

	protected def initGL() {
        gl.clearColor(clearColor)
	    gl.clearDepth(1f)
	    gl.enable(GL_DEPTH_TEST)
	    
	    gl.enable(GL_CULL_FACE)
        gl.cullFace(GL_BACK)
        gl.frontFace(GL_CW)
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
	    tubeMesh.setBottomDiskColor(Color.red)
	    tubeMesh.setCylinderColor(Color.blue)
	    tubeMesh.setTopDiskColor(Color.red)
	    
	    plane = planeMesh.newVertexArray(gl)
	    tube  = tubeMesh.newVertexArray(gl)
	    bone  = boneMesh.newVertexArray(gl)
	}
	
	protected def initTextures() {
	    tex1uv = new Texture(gl, "textures/grey-concrete-texture.jpg", true)
	    tex1uv.minMagFilter(GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
	    tex1uv.wrap(GL_REPEAT)

	    tex1nm = new Texture(gl, "textures/Sample64.png", true)
	    tex1nm.minMagFilter(GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
	    tex1nm.wrap(GL_REPEAT)
	}
	
	def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) {
	    camera.viewportPx(width, height)
        var ratio = camera.viewportRatio
        
        gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
        camera.frustum(-ratio, ratio, -1, 1, 1)
	}
	
	def display(win:GLAutoDrawable) {
	    gl.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
	    
	    shader1.use
	    camera.setupView
	    useLights(shader1)
	    useTextures(shader1)
	    useMVP(shader1)
	    gl.polygonMode(GL_FRONT_AND_BACK, GL_FILL)
	    plane.draw(planeMesh.drawAs)
	    
	    shader2.use
	    gl.polygonMode(GL_FRONT_AND_BACK, GL_LINE)
	    camera.pushpop {
	        camera.translateModel(0, 1, 0)
	        camera.rotateModel(Pi/2, 1, 0, 0)
	        camera.translateModel(0, -1, 0)
	        camera.scaleModel(1, 2, 1)
	        useMVP(shader2)
	        tube.draw(tubeMesh.drawAs)
	        camera.scaleModel(0.5, 0.5, 0.5)
	        useMVP(shader2)
	        bone.draw(boneMesh.drawAs)
	        camera.translateModel(0, 1, 0)
	        useMVP(shader2)
	        bone.draw(boneMesh.drawAs)
	    }
	    
	    win.swapBuffers
	}
	
	def useMVP(shader:ShaderProgram) {
	    shader.uniformMatrix("MV", camera.modelview.top)
	    shader.uniformMatrix("MV3x3", camera.modelview.top3x3)
	    shader.uniformMatrix("MVP", camera.projection * camera.modelview)
	}
	
	def useLights(shader:ShaderProgram) {
	    shader.uniform("lights[0].pos", Vector3(camera.modelview.top * light1))
	    shader.uniform("lights[0].intensity", 5f)
	    shader.uniform("lights[0].ambient", 0.1f)
	    shader.uniform("lights[0].specular", 16f)
	}
	
	def useTextures(shader:ShaderProgram) {
	    tex1uv.bindTo(GL_TEXTURE0)
	    shader.uniform("tex.color", 0)
	    tex1nm.bindTo(GL_TEXTURE1)
	    shader.uniform("tex.normal", 1)
	}
	
	def dispose(win:GLAutoDrawable) {}
}

class CameraController(val camera:Camera) extends KeyListener with MouseListener {
	
	protected val step = 0.1
	
	def mouseClicked(e:MouseEvent) {} 
           
	def mouseDragged(e:MouseEvent) {} 
           
	def mouseEntered(e:MouseEvent) {} 
           
	def mouseExited(e:MouseEvent) {} 
           
	def mouseMoved(e:MouseEvent) {}
           
	def mousePressed(e:MouseEvent) {} 
           
	def mouseReleased(e:MouseEvent) {} 
           
	def mouseWheelMoved(e:MouseEvent) {
	    camera.rotateViewHorizontal(e.getWheelRotation * step)
	} 
	
	def keyPressed(e:KeyEvent) {
	} 
           
	def keyReleased(e:KeyEvent) {
	}
           
	def keyTyped(e:KeyEvent) {
		e.getKeyCode match {
		    case KeyEvent.VK_PAGE_UP   => { camera.zoomView(-step) } 
		    case KeyEvent.VK_PAGE_DOWN => { camera.zoomView(step) }
		    case KeyEvent.VK_UP        => { camera.rotateViewVertical(step) }
		    case KeyEvent.VK_DOWN      => { camera.rotateViewVertical(-step) }
		    case KeyEvent.VK_LEFT      => { camera.rotateViewHorizontal(-step) }
		    case KeyEvent.VK_RIGHT     => { camera.rotateViewHorizontal(step) }
		    case _ => {}
		}
	}       
}