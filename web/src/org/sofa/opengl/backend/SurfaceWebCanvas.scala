package org.sofa.opengl.backend

import scala.collection.mutable.SynchronizedQueue

import org.sofa.opengl.{SGL, Camera}
import org.sofa.opengl.surface._


//object SurfaceWebCanvasBackend extends Enumeration {
//	val GL2ES2 = Value
//	val GL3 = Value
//	type SurfaceWebCanvasBackend = Value
//}


class SurfaceWebCanvas(
    val renderer:SurfaceRenderer,
    val canvasElement:String,
    val title:String,
    var fps:Int
)
extends Surface {

	/** Synchronized queue for events coming from the EDT (event dispatching thread), as
	  * with NEWT, events are handled in a distinct thread from rendering. */
//	private[this] val eventQueue = new SynchronizedQueue[Event]

	/** NEWT window. */
    protected var cancas:js.Any = null
    
    /** Animator thread. */
//    protected var anim:FPSAnimator = null
    
    /** OpenGL. */
    protected var sgl:SGL = null
    
    build()
    
    protected def build() {
    	val document = js.Dynamic.global.document
		canvas = document.getElementById(canvasElement)

  //       win  = GLWindow.create(caps)
  //       anim = new FPSAnimator(win, fps)
  //       sgl  = null

  //       win.setFullscreen(fullScreen)
  //       win.setUndecorated(! decorated)
	 //   	win.setVisible(true)	
	   	
	 //   	// XXX The jogl specs tell to create the window before setting the size in order to know the native
	 //   	// XXX decoration insets. However it clearly does not work. Subsequent messages when the window is
	 //   	// resized will send the correct size, leading to an incoherent behavior (the sizes given cannot
	 //   	// to be trusted, when the window appear, the reshape receives the size with the insets, subsequent
	 //   	// resets will receive a size without the insets ... how to tell when ?).
		// // Is this only on Os X ?

	 //    win.setSize(w + win.getInsets.getTotalWidth, h + win.getInsets.getTotalHeight)
	 //    win.setTitle(title)

	 //    win.addWindowListener(this)
	 //    win.addMouseListener(this)
	 //    win.addKeyListener(this)
	 //    win.addGLEventListener(this)

	 //    if(! caps.getGLProfile.isHardwareRasterizer) {
	 //    	Console.err.println("### ATTENTION : using a software rasterizer !!! ###")
	 //    	Console.err.println("%s".format(win.getContext.getGLVersion))
	 //  	} else {
	 //  		println("%s".format(win.getContext.getGLVersion))
	 //  	} 

  //       //printCaps

	 //    anim.start
    }
    
    def gl:SGL = {
    	// if(sgl eq null) {
    	// 	sgl = backend match {
     //    		case SurfaceNewtGLBackend.GL2ES2 => new SGLJogl2ES2(win.getGL.getGL2ES2, GLU.createGLU, win.getContext.getGLSLVersionString)
     //    		case SurfaceNewtGLBackend.GL3    => new SGLJogl3(win.getGL.getGL3, GLU.createGLU, win.getContext.getGLSLVersionString)
    	// 	}
    	// }
    	// sgl
    	null
    }

    def swapBuffers():Unit = {} // Automatic, nothing to do.
    
    def width = 1//w
    
    def height = 1//h
    
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
    	// win.setSize(newWidth, newHeight)
    	// w = newWidth
    	// h = newHeight
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