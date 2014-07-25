package org.sofa.gfx.surface

import scala.math._
import org.sofa.math._
import org.sofa.gfx.Camera
import org.sofa.gfx.surface.event._


class BasicCameraController(val camera:Camera) {
	
	protected var step = 0.1
	
	def gesture(surface:Surface, e:GestureEvent) {
		println("TODO BasicCameraController.gesture (scroll)")
	//    camera.rotateEyeHorizontal(e.amount * step * 0.05)
	} 
	
	def actionKey(surface:Surface, e:ActionKeyEvent) {
	println("TODO BasicCameraController.actionKey")
	    // import org.sofa.gfx.surface.ActionChar._
	    // if(! e.isPrintable) {
	    // 	e.actionChar match {
		   //  	case PageUp   => { camera.eyeTraveling(-step) } 
		   //  	case PageDown => { camera.eyeTraveling(step) }
		   //  	case Up       => { camera.rotateEyeVertical(step) }
		   //  	case Down     => { camera.rotateEyeVertical(-step) }
		   //  	case Left     => { camera.rotateEyeHorizontal(-step) }
		   //  	case Right    => { camera.rotateEyeHorizontal(step) }
		   //  	case _        => {}
	    // 	}
	    // }
	}       
	
	def motion(surface:Surface, e:MotionEvent) {
	}
}