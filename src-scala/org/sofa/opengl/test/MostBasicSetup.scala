package org.sofa.opengl.test

import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import javax.media.opengl._

import GL._
import GL3._ 

object MostBasicSetup {
	def main(args:Array[String]):Unit = {
	    (new MostBasicSetup).test
	}
}

class MostBasicSetup extends WindowAdapter with GLEventListener {
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
    
    override def windowDestroyNotify(ev:WindowEvent) { exit }
    
    def init(win:GLAutoDrawable) {
        val gl = win.getGL.getGL2ES2; import gl._;
        
        glClearColor(0.7f, 0f, 0.9f, 0f)
        glClearDepth(1f)
        glEnable(GL_DEPTH_TEST)
    }
    
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) {
        val gl = win.getGL.getGL2ES2; import gl._;
        
        glViewport(0, 0, width, height)
    }
    
    def display(win:GLAutoDrawable) {
        val gl = win.getGL.getGL2ES2; import gl._;
    
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
        
        // Your drawing code here.
        
        win.swapBuffers
    }
    
    def dispose(win:GLAutoDrawable) {
        val gl = win.getGL.asInstanceOf[GL3]; import gl._;
        sys.exit
    }
}