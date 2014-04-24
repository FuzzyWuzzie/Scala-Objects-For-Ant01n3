package org.sofa.opengl.backend

import org.sofa.opengl.{SGL, Camera}
import org.sofa.opengl.surface._

import scala.scalajs.js
import js.Dynamic.{global => g}
import org.scalajs.dom
import org.scalajs.dom.{HTMLElement, HTMLCanvasElement, KeyboardEvent, MouseEvent, WheelEvent}


/** A [[Surface]] that can be compiled by Scala.js using a DOM canvas with WebGL.
  *
  * @param renderer Surface events receiver.
  * @param canvasElement id of the canvas in the document.
  * @param fps The desired number of frames per second. */
class SurfaceWebCanvas(
    val renderer:SurfaceRenderer,
    val canvasElement:String,
    private[this] var fps:Int
)
extends Surface {

	/** NEWT window. */
    protected[this] var canvas:HTMLCanvasElement = null
    
    /** OpenGL. */
    protected[this] var sgl:SGL = null

    /** Handle on the animator. */
    protected[this] var intervalHandle:js.Number = null

    build()
    
    protected def build() {
		canvas = dom.document.getElementById(canvasElement).asInstanceOf[HTMLCanvasElement]

		if(canvas ne null) {
			val gl = canvas.getContext("webgl").asInstanceOf[WebGLRenderingContext]

			if(gl eq null) {
				throw new RuntimeException("cannot init GL")
			} else {
				sgl = new SGLWeb(gl, "TODO")
				renderer.initSurface(sgl, this)
			}

			sgl.printInfos

			dom.document.onkeypress   = onKey _
			dom.document.onkeydown    = onKeyDown _
			canvas.onmousedown        = onMouseDown _
			canvas.onmouseup          = onMouseUp _
			canvas.onmousemove        = onMouseMove _
			dom.document.onmousewheel = onMouseWheel _

			intervalHandle = dom.setInterval(animate _, 1000.0/fps)
		} else {
			throw new RuntimeException("no canvas element named '%s'".format(canvasElement))
		}
    }

    protected def animate() { if(renderer ne null) renderer.frame(this) }
    
    def gl:SGL = sgl

    def swapBuffers():Unit = {} // Automatic, nothing to do.
    
    def width = canvas.width.toInt
    
    def height = canvas.height.toInt
    
    def invoke(code:(Surface)=>Boolean) {
    	throw new RuntimeException("invoke not implemented in SurfaceWebCanvas")
    }

    def invoke(runnable:Runnable) {
    	throw new RuntimeException("invoke not implemented in SurfaceWebCanvas")
   	}

   	def fullscreen(on:Boolean) {}// win.setFullscreen(on) }

    def resize(newWidth:Int, newHeight:Int) {
    	canvas.width = newWidth
    	canvas.height = newHeight
    }

    def destroy() {
    	if(intervalHandle ne null) {
    		dom.clearInterval(intervalHandle)
    		intervalHandle = null
    	}
    }

    protected def onKey(event:KeyboardEvent) {
    	// Called by JS only for characters.
     	if(renderer ne null) renderer.key(this, new KeyEventWeb(event, true))
    }

    protected def onKeyDown(event:KeyboardEvent) {
    	// Called by JS for each character, we use it only for action chars.
    	if((renderer ne null) && event.keyCode >= 37 && event.keyCode <= 40) {
    		renderer.key(this, new KeyEventWeb(event, false))
    	}
    }

    protected[this] var mousedrag = false

    protected def onMouseDown(event:MouseEvent) {
    	if(renderer ne null) {
    		renderer.motion(this, new MotionEventWeb(event, true, false))
    		mousedrag = true
    	}
    }

    protected def onMouseMove(event:MouseEvent) {
    	if((renderer ne null) && mousedrag) {
    		renderer.motion(this, new MotionEventWeb(event, false, false))	
    	}
    }

    protected def onMouseUp(event:MouseEvent) {
    	if(renderer ne null) {
    		mousedrag = false
    		renderer.motion(this, new MotionEventWeb(event, false, true))
    	}
    }

    protected def onMouseWheel(event:WheelEvent) {
    	if(renderer ne null) {
    		renderer.scroll(this, new ScrollEventWeb(event))
    	}
    }
}

// -- Web events -------------------------------------------------------------------------------------------


class KeyEventWeb(val source:KeyboardEvent, val isPrintable:Boolean) extends KeyEvent {
    
    def unicodeChar:Char = if(isPrintable) source.keyCode.toChar else 0    
    
    def actionChar:ActionChar.Value = {
        import ActionChar._

	   	if(source.keyCode == (37: js.Number)) Left
   		else if(source.keyCode == (38 :js.Number)) Up
   		else if(source.keyCode == (39 :js.Number)) Right
   		else if(source.keyCode == (40 :js.Number)) Down
   		else if(source.keyCode == (27 :js.Number)) Escape
   		else if(source.keyCode == (33 :js.Number)) PageUp
   		else if(source.keyCode == (34 :js.Number)) PageDown
   		else Unknown

        // source.keyCode match {
        // //     case VK_SPACE     => Space
        // }
    }
    
//    def isPrintable:Boolean = !(source.keyCode >= 37 && source.keyCode <= 40) && !(source.keyCode >= 33 && source.keyCode <= 34)
    def isControlDown:Boolean = source.getModifierState("Control")
    def isAltDown:Boolean = source.getModifierState("Alt")
    def isAltGrDown:Boolean = source.getModifierState("AltGraph")
    def isShiftDown:Boolean = source.getModifierState("Shift")
    def isMetaDown:Boolean = source.getModifierState("Meta")
}


class ScrollEventWeb(val source:WheelEvent) extends ScrollEvent {
     def amount:Double = source.deltaX.toInt
}


class MotionEventWeb(val source:MouseEvent, val pressed:Boolean, val released:Boolean) extends MotionEvent {
    def deviceType:DeviceType.Value = DeviceType.Mouse
    def isStart:Boolean = pressed
    def isEnd:Boolean = released
    def x:Double = source.clientX
	def y:Double = source.clientY
    def pressure:Double = 1.0
    def x(pointer:Int):Double = source.clientX
	def y(pointer:Int):Double = source.clientY
	def pressure(pointer:Int):Double = pressure
	def pointerCount:Int = 1
	def sourceEvent:AnyRef = source
	override def toString():String = "motion[%s%.1f, %.1f (%d)]".format(if(isStart) ">" else if(isEnd) "<" else "", x, y, pointerCount)
}