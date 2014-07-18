package org.sofa.opengl.backend

import scala.collection.mutable.SynchronizedQueue

import org.sofa.opengl.{SGL, Camera}
import org.sofa.opengl.surface._

import java.awt.{Frame=>AWTFrame}
import javax.media.opengl.{GLCapabilities, GLEventListener, GLAutoDrawable}
import javax.media.opengl.{GLDrawableFactory, GLRunnable}
import javax.media.opengl.awt.GLCanvas
import javax.media.opengl.glu.GLU
import java.awt.event.{MouseListener=>AWTMouseListener,KeyListener=>AWTKeyListener,WindowListener=>AWTWindowListener, KeyEvent=>AWTKeyEvent, MouseEvent=>AWTMouseEvent, WindowEvent=>AWTWindowEvent, MouseWheelListener=>AWTMouseWheelListener, MouseWheelEvent=>AWTMouseWheelEvent}

import com.jogamp.newt.opengl.GLWindow
import com.jogamp.newt.event.{NEWTEvent=>JoglEvent, KeyEvent=>JoglKeyEvent, MouseEvent=>JoglMouseEvent, MouseListener=>JoglMouseListener, WindowListener=>JoglWindowListener, KeyListener=>JoglKeyListener, WindowEvent=>JoglWindowEvent, WindowUpdateEvent=>JoglWindowUpdateEvent}
import com.jogamp.opengl.util.FPSAnimator


object SurfaceNewtGLBackend extends Enumeration {
	val GL2ES2 = Value
	val GL3 = Value
	type SurfaceNewtGLBackend = Value
}


/** A surface implementation that uses the Jogl Newt framwork. */
class SurfaceNewt(
    
    val renderer:SurfaceRenderer,
    var w:Int,
    var h:Int,
    val title:String,
    val caps:GLCapabilities,
    val backend:SurfaceNewtGLBackend.Value,
    var fps:Int,
    var decorated:Boolean,
    var fullScreen:Boolean)
	
	extends Surface
	with    JoglWindowListener
	with    JoglKeyListener
	with    JoglMouseListener
	with    GLEventListener {

	import SurfaceNewtGLBackend._

    def this(renderer:SurfaceRenderer,
    		 camera:Camera,
    		 title:String,
    		 caps:GLCapabilities,
             backend:SurfaceNewtGLBackend.Value,
             fps:Int = 30,
             decorated:Boolean = true,
             fullScreen:Boolean = false) { 
    	this(renderer, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt,
    		 title, caps, backend, fps, decorated, fullScreen)
    }	

	/** Synchronized queue for events coming from the EDT (event dispatching thread), as
	  * with NEWT, events are handled in a distinct thread from rendering. */
	private[this] val eventQueue = new java.util.concurrent.ConcurrentLinkedQueue[Event]()// new SynchronizedQueue[Event] // Waiting for SynchronizedQueue to be corrected.

	/** NEWT window. */
    protected var win:GLWindow = null
    
    /** Animator thread. */
    protected var anim:FPSAnimator = null

    /** Used to fasten invoke, in order to test if we are in the correct thread. */
    private[this] var invokeThread:Thread = null
    
    /** OpenGL. */
    protected var sgl:SGL = null
    
    build(backend)
    
    protected def build(backend:SurfaceNewtGLBackend) {
        win  = GLWindow.create(caps)
        anim = new FPSAnimator(win, fps)
        sgl  = null

        win.setFullscreen(fullScreen)
        win.setUndecorated(! decorated)
	   	win.setVisible(true)	
	   	
	   	// XXX The jogl specs tell to create the window before setting the size in order to know the native
	   	// XXX decoration insets. However it clearly does not work. Subsequent messages when the window is
	   	// resized will send the correct size, leading to an incoherent behavior (the sizes given cannot
	   	// to be trusted, when the window appear, the reshape receives the size with the insets, subsequent
	   	// resets will receive a size without the insets ... how to tell when ?).
		// Is this only on Os X ?
		//
		// XXX TODO --> bug fixed ? Test !

	    win.setSize(w + win.getInsets.getTotalWidth, h + win.getInsets.getTotalHeight)
	    win.setTitle(title)

	    win.addWindowListener(this)
	    win.addMouseListener(this)
	    win.addKeyListener(this)
	    win.addGLEventListener(this)

	    if(! caps.getGLProfile.isHardwareRasterizer) {
	    	Console.err.println("### ATTENTION : using a software rasterizer !!! ###")
	    	Console.err.println("%s".format(win.getContext.getGLVersion))
	  	} else {
	  		println("%s".format(win.getContext.getGLVersion))
	  	} 

        //printCaps

	    anim.start
    }
    
    def gl:SGL = {
    	if(sgl eq null) {
    		sgl = backend match {
        		case SurfaceNewtGLBackend.GL2ES2 => new SGLJogl2ES2(win.getGL.getGL2ES2, GLU.createGLU, win.getContext.getGLSLVersionString)
        		case SurfaceNewtGLBackend.GL3    => new SGLJogl3(win.getGL.getGL3, GLU.createGLU, win.getContext.getGLSLVersionString)
    		}
    	}
    	sgl
    }

    def swapBuffers():Unit = {} // Automatic, nothing to do.
    
    def width = w
    
    def height = h
    
    def init(win:GLAutoDrawable) { if(renderer.initSurface ne null) renderer.initSurface(gl, this) }
    
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) { w = width; h = height; if(renderer.surfaceChanged ne null) renderer.surfaceChanged(this) }
    
    def display(win:GLAutoDrawable) { processEvents; if(renderer.frame ne null) renderer.frame(this) }
    
    def dispose(win:GLAutoDrawable) { if(renderer.close ne null) renderer.close(this) }
    
    def invoke(code:(Surface)=>Boolean) {
    	val me = this

    	if(invokeThread eq Thread.currentThread) {
    		code(me)	
    	} else {
    		win.invoke(false,
    			new GLRunnable() { override def run(win:GLAutoDrawable):Boolean = { if(invokeThread eq null) invokeThread = Thread.currentThread; code(me) } }
    		)
    	}
    }

    def invoke(runnable:Runnable) {
    	val me = this

    	if(invokeThread eq Thread.currentThread) {
	    	runnable.run
    	} else {
    		if(win ne null) {
    			win.invoke(false,
	    			new GLRunnable() { override def run(win:GLAutoDrawable) = { if(invokeThread eq null) invokeThread = Thread.currentThread; runnable.run; true } }
    			)
    		}
    	}
   	}

   	def fullscreen(on:Boolean) { win.setFullscreen(on) }

   	/** Process all pending events in the event queue. */
   	protected def processEvents() {
   		while(! eventQueue.isEmpty) {
   			eventQueue.poll match {
   				case e:KeyEvent       => { if(renderer.key       ne null) renderer.key(this, e) }
   				case e:ActionEvent    => { if(renderer.action    ne null) renderer.action(this, e) }
				case e:ConfigureEvent => { if(renderer.configure ne null) renderer.configure(this, e) }
				case e:MotionEvent    => { if(renderer.motion    ne null) renderer.motion(this, e) }
				case e:ScrollEvent    => { if(renderer.scroll    ne null) renderer.scroll(this, e) }
   			}
   		}
   	}

    def resize(newWidth:Int, newHeight:Int) {
    	win.setSize(newWidth, newHeight)
    	w = newWidth
    	h = newHeight
    }

    def animation(on:Boolean) {
    	if(on) anim.resume
    	else anim.pause
    }

    def destroy() {
    	anim.stop
    	win.destroy
    	win = null
    }

   	// -- GUI Events --------------------------------------------------------------
   	//
   	// All events add to the event queue and are processed by processEvents() when
   	// the surface display() method is called.

    def windowDestroyNotify(ev:JoglWindowEvent) {}
    def windowDestroyed(e:JoglWindowEvent) {}
    def windowGainedFocus(e:JoglWindowEvent) {} 
    def windowLostFocus(e:JoglWindowEvent) {} 
    def windowMoved(e:JoglWindowEvent) {}
    def windowRepaint(e:JoglWindowUpdateEvent) {} 
    def windowResized(e:JoglWindowEvent) {/*Console.err.println("resized w=%d h=%d".format(win.getWidth, win.getHeight))*/} 
    
	def keyPressed(e:JoglKeyEvent) {} 
	def keyReleased(e:JoglKeyEvent) { eventQueue.add(new KeyEventJogl(e)) }
	def keyTyped(e:JoglKeyEvent) {}
    
    def mouseClicked(e:JoglMouseEvent) {
        e.getButton match {
            case 1 => { eventQueue.add(new ActionEvent()) }
            case 3 => { eventQueue.add(new ConfigureEvent()) }
            case _ => {}
        }
    }

    private[this] var inMotion = false

    def mouseEntered(e:JoglMouseEvent) {}
 
    def mouseExited(e:JoglMouseEvent) { 
    	// Ensure we receive a motion end when the mouse leave the window.
    	// (Two case, when the mouse is released out of the window or when coming back in it).
    	if(inMotion) {
    		eventQueue.add(new MotionEventJogl(e, false, true))
    		inMotion = false 
    	}
    }
 
    def mouseMoved(e:JoglMouseEvent) {}
 
    def mousePressed(e:JoglMouseEvent) {
    	inMotion = true;
    	eventQueue.add(new MotionEventJogl(e, true, false)) 
   	}
 
    def mouseDragged(e:JoglMouseEvent) { 
    	if(inMotion) {
    		eventQueue.add(new MotionEventJogl(e, false, false)) 
    	}
   	}
 
    def mouseReleased(e:JoglMouseEvent) {
    	if(inMotion) {
    		inMotion = false
    		eventQueue.add(new MotionEventJogl(e, false, true)) 
    	}
    }
 
    def mouseWheelMoved(e:JoglMouseEvent) {
    	eventQueue.add(new ScrollEventJogl(e)) 
    }

    def printCaps() {
    	val prof = caps.getGLProfile
    	val ctx  = win.getContext
    	println("profile %s".format(prof.getName))
    	println("impl    %s".format(prof.getImplName))
    	println("HW      %s".format(prof.isHardwareRasterizer))
    	println("GSSL    %s".format(ctx.getGLSLVersionString))
    	println("version %s".format(ctx.getGLVersion))
    }
}

// -- NEWT events -------------------------------------------------------------------------------------------


class KeyEventJogl(val source:JoglKeyEvent) extends KeyEvent {
    
    def unicodeChar:Char = source.getKeyChar
    
    def actionChar:ActionChar.Value = {
        import JoglKeyEvent._
        import ActionChar._
        source.getKeyCode match {
            case VK_PAGE_UP   => PageUp
            case VK_PAGE_DOWN => PageDown
            case VK_UP        => Up
            case VK_DOWN      => Down
            case VK_RIGHT     => Right
            case VK_LEFT      => Left
            case VK_ESCAPE    => Escape
            case VK_SPACE     => Space
            case _            => Unknown
        }
    }
    
    def isPrintable:Boolean = !source.isActionKey
    def isControlDown:Boolean = source.isControlDown
    def isAltDown:Boolean = source.isAltDown
    def isAltGrDown:Boolean = source.isAltGraphDown
    def isShiftDown:Boolean = source.isShiftDown
    def isMetaDown:Boolean = source.isMetaDown
}

class ScrollEventJogl(source:JoglMouseEvent) extends ScrollEvent {
    def amount:Double = source.getRotation()(1)
}

class MotionEventJogl(source:JoglMouseEvent, pressed:Boolean, released:Boolean) extends MotionEvent {
    def deviceType:DeviceType.Value = DeviceType.Mouse
    def isStart:Boolean = pressed
    def isEnd:Boolean = released
    def x:Double = source.getX
    def y:Double = source.getY
    def pressure:Double = source.getPressure(true)
    def x(pointer:Int):Double = source.getX(pointer)
    def y(pointer:Int):Double = source.getY(pointer)
    def pressure(pointer:Int):Double = source.getPressure(pointer, true)
    def pointerCount:Int = source.getPointerCount
    def sourceEvent:AnyRef = source
    override def toString():String = "motion[%s%.1f, %.1f (%d)]".format(if(isStart) ">" else if(isEnd) "<" else "", x, y, pointerCount)
}