package org.sofa.opengl

import scala.math._

import org.sofa.math._

import com.jogamp.newt.event._

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