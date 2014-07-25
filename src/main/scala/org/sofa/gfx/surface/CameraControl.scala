package org.sofa.gfx.surface

import scala.math._
import org.sofa.math._
import org.sofa.gfx.Camera
import org.sofa.gfx.surface.event._


class BasicCameraController(val camera:Camera) {
	
	protected[this] var step = 0.1
	
	def gesture(surface:Surface, e:GestureEvent) {
		e match {
			case scroll:ScrollEvent => {
				camera.rotateEyeHorizontal(scroll.delta.x * step * 0.05)
				camera.rotateEyeVertical(scroll.delta.y * step * 0.05)
			}
			case scale:ScaleEvent => {
				camera.eyeTraveling(scale.delta * 0.05)
			}
			case _ => {}
		}
	} 
	
	def actionKey(surface:Surface, e:ActionKeyEvent) {
	    import org.sofa.gfx.surface.event.ActionKey._

		e.key match {
	    	case PageUp   => camera.eyeTraveling(-step)
	    	case PageDown => camera.eyeTraveling(step)
	    	case Up       => camera.rotateEyeVertical(step)
	    	case Down     => camera.rotateEyeVertical(-step)
	    	case Left     => camera.rotateEyeHorizontal(-step)
	    	case Right    => camera.rotateEyeHorizontal(step)
	    	case _        => {}
		}
	} 

	def unicode(surface:Surface, e:UnicodeEvent) {
	}  
	
	def motion(surface:Surface, e:MotionEvent) {
	}
}