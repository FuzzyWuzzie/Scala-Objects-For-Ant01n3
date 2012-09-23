package org.sofa.opengl.surface

import scala.math._
import org.sofa.math._
import org.sofa.opengl.Camera

class BasicCameraController(val camera:Camera) {
	
	protected val step = 0.1
	
	def scroll(surface:Surface, e:ScrollEvent) {
	    camera.rotateViewHorizontal(e.amount * step * 0.05)
	} 
	
	def key(surface:Surface, e:KeyEvent) {
	    import e.ActionChar._
	    if(! e.isPrintable) {
	    	e.actionChar match {
		    	case PageUp   => { camera.zoomView(-step) } 
		    	case PageDown => { camera.zoomView(step) }
		    	case Up       => { camera.rotateViewVertical(step) }
		    	case Down     => { camera.rotateViewVertical(-step) }
		    	case Left     => { camera.rotateViewHorizontal(-step) }
		    	case Right    => { camera.rotateViewHorizontal(step) }
		    	case _        => {}
	    	}
	    }
	}       
	
	def motion(surface:Surface, e:MotionEvent) {
	}
}