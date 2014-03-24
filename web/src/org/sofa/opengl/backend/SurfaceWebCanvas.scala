package org.sofa.opengl.backend

import org.sofa.opengl.{SGL, Camera}
import org.sofa.opengl.surface._

import scala.scalajs.js
import js.Dynamic.{global => g}
import org.scalajs.dom
import org.scalajs.dom.{HTMLElement, HTMLCanvasElement}


class SurfaceWebCanvas(
    val renderer:SurfaceRenderer,
    val canvasElement:String,
    val title:String,
    var fps:Int
)
extends Surface {

	/** NEWT window. */
    protected var canvas:HTMLCanvasElement = null
    
    /** OpenGL. */
    protected var sgl:SGL = null

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

			// TODO animation

			// TODO events
		} else {
			throw new RuntimeException("no canvas element named '%s'".format(canvasElement))
		}
    }
    
    def gl:SGL = sgl

    def swapBuffers():Unit = {} // Automatic, nothing to do.
    
    def width = canvas.width.toInt
    
    def height = canvas.height.toInt
    
    // def init(win:GLAutoDrawable) {}// if(renderer.initSurface ne null) renderer.initSurface(gl, this) }
    
    // def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) {}// w = width; h = height; if(renderer.surfaceChanged ne null) renderer.surfaceChanged(this) }
    
    // def display(win:GLAutoDrawable) {}// processEvents; if(renderer.frame ne null) renderer.frame(this) }
    
    // def dispose(win:GLAutoDrawable) { if(renderer.close ne null) renderer.close(this) }
    
    def invoke(code:(Surface)=>Boolean) {
    	// val me = this

    	// if(invokeThread eq Thread.currentThread) {
    	// 	code(me)	
    	// } else {
    	// 	win.invoke(false,
    	// 		new GLRunnable() { override def run(win:GLAutoDrawable):Boolean = { if(invokeThread eq null) invokeThread = Thread.currentThread; code(me) } }
    	// 	)
    	// }
    }

    def invoke(runnable:Runnable) {
    	// val me = this

    	// if(invokeThread eq Thread.currentThread) {
	    // 	runnable.run
    	// } else {
    	// 	win.invoke(false,
	    // 		new GLRunnable() { override def run(win:GLAutoDrawable) = { if(invokeThread eq null) invokeThread = Thread.currentThread; runnable.run; true } }
    	// 	)
    	// }
   	}

   	def fullscreen(on:Boolean) {}// win.setFullscreen(on) }

    def resize(newWidth:Int, newHeight:Int) {
    	// No-Op we cannot control the size of the canvas.
    }

    def destroy() {
    	// anim.stop
    	// win.destroy
    }
}


// -- Web events -------------------------------------------------------------------------------------------


// class KeyEventWeb(val source:JoglKeyEvent) extends KeyEvent {
    
//     def unicodeChar:Char = source.getKeyChar
    
//     def actionChar:ActionChar.Value = {
//         import JoglKeyEvent._
//         import ActionChar._
//         source.getKeyCode match {
//             case VK_PAGE_UP   => PageUp
//             case VK_PAGE_DOWN => PageDown
//             case VK_UP        => Up
//             case VK_DOWN      => Down
//             case VK_RIGHT     => Right
//             case VK_LEFT      => Left
//             case VK_ESCAPE    => Escape
//             case VK_SPACE     => Space
//             case _            => Unknown
//         }
//     }
    
//     def isPrintable:Boolean = !source.isActionKey
//     def isControlDown:Boolean = source.isControlDown
//     def isAltDown:Boolean = source.isAltDown
//     def isAltGrDown:Boolean = source.isAltGraphDown
//     def isShiftDown:Boolean = source.isShiftDown
//     def isMetaDown:Boolean = source.isMetaDown
// }

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