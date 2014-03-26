package org.sofa.opengl.backend

import org.sofa.opengl.{SGL, Camera}
import org.sofa.opengl.surface._

import scala.scalajs.js
import js.Dynamic.{global => g}
import org.scalajs.dom
import org.scalajs.dom.{HTMLElement, HTMLCanvasElement, KeyboardEvent}


class SurfaceWebCanvas(
    val renderer:SurfaceRenderer,
    val canvasElement:String,
    val title:String,
    private[this] var fps:Int
)
extends Surface {

	/** NEWT window. */
    protected var canvas:HTMLCanvasElement = null
    
    /** OpenGL. */
    protected var sgl:SGL = null

    protected[this] var intervalHandle:js.Number = null

    /** Animator. */
//    protected var anim:FPSAnimator = null
    
    build()
    
    protected def build() {
		canvas = dom.document.getElementById(canvasElement).asInstanceOf[HTMLCanvasElement]

		if(canvas ne null) {
			val gl = canvas.getContext("webgl").asInstanceOf[WebGLRenderingContext]
			//val gl = g.initWebGL(canvas).asInstanceOf[WebGLRenderingContext]//WebGLGlobalScope.initWebGL(canvas)//g.initWebGL(canvas)

			if(gl eq null) {
				throw new RuntimeException("cannot init GL")
			} else {
				sgl = new SGLWeb(gl, "TODO")
				renderer.initSurface(sgl, this)
			}

			// TODO events
			dom.document.onkeypress = onKey _
			dom.document.onkeydown = onKeyDown _

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
        // //     case _            => Unknown
        // }
    }
    
//    def isPrintable:Boolean = !(source.keyCode >= 37 && source.keyCode <= 40) && !(source.keyCode >= 33 && source.keyCode <= 34)
    def isControlDown:Boolean = source.getModifierState("Control")
    def isAltDown:Boolean = source.getModifierState("Alt")
    def isAltGrDown:Boolean = source.getModifierState("AltGraph")
    def isShiftDown:Boolean = source.getModifierState("Shift")
    def isMetaDown:Boolean = source.getModifierState("Meta")
}

// class ScrollEventWeb(source:JoglMouseEvent) extends ScrollEvent {
//     def amount:Int = source.getRotation()(0).toInt//source.getWheelRotation.toInt
// }

// class MotionEventWeb(source:JoglMouseEvent, pressed:Boolean, released:Boolean) extends MotionEvent {
//     def deviceType:DeviceType.Value = DeviceType.Mouse
//     def isStart:Boolean = pressed
//     def isEnd:Boolean = released
//     def x:Double = source.getX
//     def y:Double = source.getY
//     def pressure:Double = source.getPressure(true)
//     def x(pointer:Int):Double = source.getX(pointer)
//     def y(pointer:Int):Double = source.getY(pointer)
//     def pressure(pointer:Int):Double = source.getPressure(pointer, true)
//     def pointerCount:Int = source.getPointerCount
//     def sourceEvent:AnyRef = source
//     override def toString():String = "motion[%s%.1f, %.1f (%d)]".format(if(isStart) ">" else if(isEnd) "<" else "", x, y, pointerCount)
// }