package org.sofa.opengl.surface

import scala.math._
import org.sofa.math._
import org.sofa.opengl.Camera

class BasicCameraController(val camera:Camera) {
	
	protected var step = 0.1
	
	def scroll(surface:Surface, e:ScrollEvent) {
	    camera.rotateEyeHorizontal(e.amount * step * 0.05)
	} 
	
	def key(surface:Surface, e:KeyEvent) {
	    import org.sofa.opengl.surface.ActionChar._
	    if(! e.isPrintable) {
	    	e.actionChar match {
		    	case PageUp   => { camera.eyeTraveling(-step) } 
		    	case PageDown => { camera.eyeTraveling(step) }
		    	case Up       => { camera.rotateEyeVertical(step) }
		    	case Down     => { camera.rotateEyeVertical(-step) }
		    	case Left     => { camera.rotateEyeHorizontal(-step) }
		    	case Right    => { camera.rotateEyeHorizontal(step) }
		    	case _        => {}
	    	}
	    }
	}       
	
	def motion(surface:Surface, e:MotionEvent) {
	}
}