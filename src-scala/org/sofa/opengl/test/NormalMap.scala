package org.sofa.opengl.test

import scala.math._
import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._
import org.sofa.opengl._
import org.sofa.opengl.mesh._
import org.sofa.nio._
import org.sofa.math._
import java.awt.Color

object NormalMap {
	def main(args:Array[String]):Unit = (new NormalMap).show
}

class NormalMap extends WindowAdapter with GLEventListener with MouseListener with KeyListener {
    var gl:SGL = null
    val projection:Matrix4 = new ArrayMatrix4
    val modelview = new MatrixStack(new ArrayMatrix4)
    var nmapShader:ShaderProgram = null 
    var widthPx = 800.0
    var heightPx = 600.0
    var maxDepth = 1000.0
    val planeMesh = new Plane(2, 2, 4, 4)
    var plane:VertexArray = null
    val tubeMesh = new Cylinder(0.5f, 1, 16, 1)
    var tube:VertexArray = null
    var uvTex:Texture = null
    var specTex:Texture = null
    var nmapTex:Texture = null
    
    val clearColor = Color.black
    val eye = Vector3(1, 1.5, 0)
    var radius = 4.0
    val lookAt = Vector3(0, 0, 0)
    val up = Vector3(0, 1, 0)
    val light1 = Vector4(0, 2, 0, 1)
    
	def show() {
	    val prof = GLProfile.get(GLProfile.GL3)
	    val caps = new GLCapabilities(prof)
	    
	    caps.setDoubleBuffered(true)
	    caps.setHardwareAccelerated(true)
	    caps.setSampleBuffers(true)
	    caps.setRedBits(8)
	    caps.setGreenBits(8)
	    caps.setBlueBits(8)
	    caps.setAlphaBits(8)
	    caps.setNumSamples(4)
	    
	    val win = GLWindow.create(caps)
	    val anim = new FPSAnimator(win, 30)
	    
	    win.addWindowListener(this)
	    win.addGLEventListener(this)
	    win.addMouseListener(this)
	    win.addKeyListener(this)
	    win.setSize(widthPx.toInt, heightPx.toInt)
	    win.setTitle("Normal Mapping")
	    win.setVisible(true)
	    
	    anim.start
	}
	
	override def windowDestroyNotify(ev:WindowEvent) {
	    exit
	}
	
	def init(win:GLAutoDrawable) {
	    gl = new SGLJogl(win.getGL.getGL3, GLU.createGLU)
	    
	    gl.clearColor(clearColor)
	    gl.clearDepth(1f)
	    gl.enable(gl.DEPTH_TEST)
	    //gl.polygonMode(GL_FRONT_AND_BACK, GL_LINE)
	    
	    gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CW)

	    setup
	}
	
	def setup() {
	    nmapShader = new ShaderProgram(gl,
	            new VertexShader(gl, "src-scala/org/sofa/opengl/shaders/nmapPhong.vert"),
	            new FragmentShader(gl, "src-scala/org/sofa/opengl/shaders/nmapPhong.frag"))

	    reshape(null, 0, 0, widthPx.toInt, heightPx.toInt)
	    
	    tubeMesh.setTopDiskColor(Color.yellow)
	    tubeMesh.setBottomDiskColor(Color.yellow)
	    //tubeMesh.setDiskColor(4, Color.red)
	    tubeMesh.setCylinderColor(Color.blue);
	    planeMesh.setColor(Color.magenta)
	    
	    plane = planeMesh.newVertexArray(gl)
	    tube  = tubeMesh.newVertexArray(gl)
	    
	    uvTex = new Texture(gl, "textures/stone_wall__.jpg", true)
//	    uvTex = new Texture(gl, "textures/face.jpg", true)
	    uvTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    uvTex.wrap(gl.REPEAT)

	    specTex = new Texture(gl, "textures/specular.png", true)
	    specTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    specTex.wrap(gl.REPEAT)
	    
//	    nmapTex = new Texture(gl, "textures/NormalFlat.png", false)
	    nmapTex = new Texture(gl, "textures/normal.jpg", true)
//	    nmapTex = new Texture(gl, "textures/facenrm.jpg", true)
	    nmapTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    nmapTex.wrap(gl.REPEAT)
	}
	
	def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) {
	    widthPx = width
        heightPx = height
        var ratio = widthPx / heightPx
        
        gl.viewport(0, 0, width, height)
        projection.setIdentity
        projection.frustum(-ratio, ratio, -1, 1, 1, maxDepth)
	}
	
	def display(win:GLAutoDrawable) {
	    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
	    
	    nmapShader.use
	    
	    modelview.setIdentity
	    modelview.lookAt(eye, lookAt, up)
	    
	    setupLights
	    setupTextures
	    
	    nmapShader.uniformMatrix("MV", modelview.top)
	    nmapShader.uniformMatrix("MV3x3", modelview.top3x3)
	    nmapShader.uniformMatrix("MVP", projection * modelview)
	    plane.draw(planeMesh.drawAs)
/*	    tube.draw(tubeMesh.drawAs)

	    modelview.pushpop {
		    modelview.translate(0, 1.1, 0)
		    nmapShader.uniformMatrix("MV", modelview.top)
		    nmapShader.uniformMatrix("MV3x3", modelview.top3x3)
		    nmapShader.uniformMatrix("MVP", projection * modelview)
		    tube.draw(tubeMesh.drawAs)
	    }
*/
	    modelview.pushpop {
		    modelview.translate(-1, 0, -1)
		    nmapShader.uniformMatrix("MV", modelview.top)
		    nmapShader.uniformMatrix("MV3x3", modelview.top3x3)
		    nmapShader.uniformMatrix("MVP", projection * modelview)
		    tube.draw(tubeMesh.drawAs)
		    
		    modelview.translate(0, 1.1, 0)
		    nmapShader.uniformMatrix("MV", modelview.top)
		    nmapShader.uniformMatrix("MV3x3", modelview.top3x3)
		    nmapShader.uniformMatrix("MVP", projection * modelview)
		    tube.draw(tubeMesh.drawAs)
	    }
	    
	    win.swapBuffers
	    
	    //animate
	}
	
	def setupLights() {
	    // We need to position the light by ourself, but avoid doing
	    // it at each pixel in the shader.
	    
	    nmapShader.uniform("lightPos", Vector3(modelview.top * light1))
	    nmapShader.uniform("lightIntensity", 5f)
	    nmapShader.uniform("ambientIntensity", 0.1f)
	    nmapShader.uniform("specularPow", 16f)
	}
	
	def setupTextures() {
	    uvTex.bindTo(gl.TEXTURE0)
	    nmapShader.uniform("texColor", 0)	// Texture Unit 0
	    specTex.bindTo(gl.TEXTURE1)
	    nmapShader.uniform("texSpec", 1)	// Texture Unit 1
	    nmapTex.bindTo(gl.TEXTURE2)
	    nmapShader.uniform("texNormal", 2)	// Texture Unit 2
	}
	
	def dispose(win:GLAutoDrawable) {}
	
	var angle = 0.0
	
	def animate() {
	    angle += 0.01
	    
	    if(angle > 360) angle = 0
	    
	    eye.x = (cos(angle)*4).toFloat
	    eye.z = (sin(angle)*4).toFloat
	}
	
	// Events
	
	def mouseClicked(e:MouseEvent) {} 
           
	def mouseDragged(e:MouseEvent) {} 
           
	def mouseEntered(e:MouseEvent) {} 
           
	def mouseExited(e:MouseEvent) {} 
           
	def mouseMoved(e:MouseEvent) {}
           
	def mousePressed(e:MouseEvent) {} 
           
	def mouseReleased(e:MouseEvent) {} 
           
	def mouseWheelMoved(e:MouseEvent) {
	    val rotation = e.getWheelRotation / 100f
	    
	    angle = (angle + rotation) % (2*Pi)
	    eye.x = (cos(angle)*radius).toFloat
	    eye.z = (sin(angle)*radius).toFloat
	} 
	
	def keyPressed(e:KeyEvent) {
	} 
           
	def keyReleased(e:KeyEvent) {
	}
           
	def keyTyped(e:KeyEvent) {
		e.getKeyCode match {
		    case KeyEvent.VK_PAGE_UP   => { pageUp(e) } 
		    case KeyEvent.VK_PAGE_DOWN => { pageDown(e) }
		    case KeyEvent.VK_UP        => { up(e) }
		    case KeyEvent.VK_DOWN      => { down(e) }
		    case KeyEvent.VK_LEFT      => { left(e) }
		    case KeyEvent.VK_RIGHT     => { right(e) }
		    case _ => {}
		}
		
	}       

	protected def pageUp(e:KeyEvent) {
	    radius -= 0.5
		eye.x = (cos(angle)*radius).toFloat
	    eye.z = (sin(angle)*radius).toFloat
	}
	
	protected def pageDown(e:KeyEvent) {
	    radius += 0.5
		eye.x = (cos(angle)*radius).toFloat
	    eye.z = (sin(angle)*radius).toFloat
	}
	
	protected def up(e:KeyEvent) {
	    if(e.isShiftDown()) {
	        light1.x -= 0.1
	    } else {
	    	eye.y  += 0.1
	    }
	}
	
	protected def down(e:KeyEvent) {
	    if(e.isShiftDown()) {
	        light1.x += 0.1	        
	    } else {
	    	eye.y  -= 0.1
	    }
	}
	
	protected def left(e:KeyEvent) {
	    if(e.isShiftDown()) {
	        light1.z += 0.1
	    } else {
	    	angle = (angle - 0.1f) % (2*Pi)
	    	eye.x = (cos(angle)*radius).toFloat
	    	eye.z = (sin(angle)*radius).toFloat
	    }
	}
	
	protected def right(e:KeyEvent) {
	    if(e.isShiftDown()) {
	        light1.z -= 0.1
	    } else {
	    	angle = (angle + 0.1f) % (2*Pi)
	    	eye.x = (cos(angle)*radius).toFloat
			eye.z = (sin(angle)*radius).toFloat
	    }
	}
}