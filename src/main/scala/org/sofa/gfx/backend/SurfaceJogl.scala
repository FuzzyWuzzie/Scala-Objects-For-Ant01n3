package org.sofa.gfx.backend

import scala.collection.mutable.SynchronizedQueue

import org.sofa.math.{Point3, Vector3}
import org.sofa.gfx.{SGL, Camera}
import org.sofa.gfx.surface._
import org.sofa.gfx.surface.event._

import java.awt.{Frame=>AWTFrame}
import javax.media.opengl.{GLCapabilities, GLEventListener, GLAutoDrawable}
import javax.media.opengl.{GLDrawableFactory, GLRunnable}
import javax.media.opengl.awt.GLCanvas
import javax.media.opengl.glu.GLU
import java.awt.event.{MouseListener=>AWTMouseListener,KeyListener=>AWTKeyListener,WindowListener=>AWTWindowListener, KeyEvent=>AWTKeyEvent, MouseEvent=>AWTMouseEvent, WindowEvent=>AWTWindowEvent, MouseWheelListener=>AWTMouseWheelListener, MouseWheelEvent=>AWTMouseWheelEvent}

import com.jogamp.newt.opengl.GLWindow
import com.jogamp.newt.event.{NEWTEvent=>JoglEvent, KeyEvent=>JoglKeyEvent, MouseEvent=>JoglMouseEvent, MouseListener=>JoglMouseListener, WindowListener=>JoglWindowListener, KeyListener=>JoglKeyListener, WindowEvent=>JoglWindowEvent, WindowUpdateEvent=>JoglWindowUpdateEvent}
import com.jogamp.opengl.util.FPSAnimator


/** Allow to specify either OpenGL3 and up compatibility (desktop) or ES2 and up compatibility (mobile). */
object SurfaceNewtGLBackend extends Enumeration {
	/** Mobile profile. */
	val GL2ES2 = Value

	/** Desktop profile. */
	val GL3 = Value

	type SurfaceNewtGLBackend = Value
}


/** A surface implementation that uses the Jogl Newt framework.
  *
  * Implementation note: this class is used by two threads :
  *  - The renderer thread.
  *  - The EDT (event dispatching thread) thread.
  * Care has been taken to separate (and indicate) fields that are used in
  * a thread, from fields used in the other. */
class SurfaceNewt(
    protected[this] val renderer:SurfaceRenderer,
    protected[this] var w:Int,
    protected[this] var h:Int,
    protected[this] val title:String,
    protected[this] val caps:GLCapabilities,
    protected[this] val backend:SurfaceNewtGLBackend.Value,
    protected[this] var expectedFps:Int,
    protected[this] var decorated:Boolean,
    protected[this] var fullScreen:Boolean)
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
             expectedFps:Int = 30,
             decorated:Boolean = true,
             fullScreen:Boolean = false) { 
    	this(renderer, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt,
    		 title, caps, backend, expectedFps, decorated, fullScreen)
    }	

	/** Synchronized queue for events coming from the EDT (event dispatching thread), as
	  * with NEWT, events are handled in a distinct thread from rendering. */
	private[this] val eventQueue = new java.util.concurrent.ConcurrentLinkedQueue[Event]()// new SynchronizedQueue[Event] // Waiting for SynchronizedQueue to be corrected.

	/** NEWT window. */
    protected[this] var win:GLWindow = null
    
    /** Animator thread. */
    protected[this] var anim:FPSAnimator = null

    /** Used to fasten invoke, in order to test if we are in the correct thread. */
    private[this] var invokeThread:Thread = null
    
    /** OpenGL. */
    protected[this] var sgl:SGL = null
    
    /** Send motion events ? This field is used only in the EDT thread. */
    protected[this] var motionEvents:Boolean = true

    /** Send high-level events ? This field is used only in the EDT thread. */
    protected[this] var hlEvents:Boolean = true

    /** Delay between button press and release to consider it as a long press event.
      * This field is used only in the EDT thread. */
    protected[this] var pressDelay:Long = 200000000L // 2 sec.

    /** Delay between two clicks under which the second click generates a double tap. */
    protected[this] val doubleTapDelay:Long = 180000000L // 1 sec.

    /** If this field is not zero, it contains the last date at which a single tap
      * has been issued. In this case if the difference between this and the current
      * date is > doubleTapDelay, a [[SingleTapEventJogl]] is sent and this field is
      * set to zero. If in between a [[DoubleTapEventJogl]] is issued the field is
      * reset to zero without sending a [[SingleTapEventJogl]]. This field is used
      * only in the renderer thread. */
    protected[this] var needValidateSingleTap:Long = 0

    /** Send motion events for the mouse even when no button is pressed ? This field
      * is used only in the EDT thread. */
    protected[this] var sendNoButMotionEvents:Boolean = false

    /** If true the mouse scroll wheel event is transformed to a [[ScaleEventJogl]],
      * else a [[ScrollEventJogl]] with a delta y is sent. */
    protected[this] var mouseWheelSendsScale:Boolean = true

    build(backend)
    
    protected def build(backend:SurfaceNewtGLBackend) {
		if(expectedFps <= 0)
    		throw new RuntimeException(s"invalid FPS ${expectedFps}, please specify a number > 0")

        win  = GLWindow.create(caps)
        anim = new FPSAnimator(win, expectedFps)
        sgl  = null

        win.setFullscreen(fullScreen)
        win.setUndecorated(! decorated)
	   	win.setVisible(true)	
	   	
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

// -- Newt GLEventListener --------------------

    def init(win:GLAutoDrawable) { if(renderer.initSurface ne null) renderer.initSurface(gl, this) }
    
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) { w = width; h = height; if(renderer.surfaceChanged ne null) renderer.surfaceChanged(this) }
    
    def display(win:GLAutoDrawable) {
    	processEvents
    	singleTapEventValidation

    	if(renderer.frame ne null)
    		renderer.frame(this)
    }
    
    def dispose(win:GLAutoDrawable) { if(renderer.close ne null) renderer.close(this) }
    
// -- Invokation on the rendering thread --------------

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

// -- Surface interface -----------------
    
    def width = w
    
    def height = h

   	def fullscreen(on:Boolean) { win.setFullscreen(on) }

    def resize(newWidth:Int, newHeight:Int) {
    	win.setSize(newWidth, newHeight)
    	w = newWidth
    	h = newHeight
    }

    def sendMotionEvents(on:Boolean) { motionEvents = on }

    def sendHighLevelEvents(on:Boolean) { hlEvents = on }

    def fps:Int = expectedFps

    def fps(newFps:Int) {
    	// Have not seen a method to change the FPS in FpsAnimator ... strange
    	if(newFps > 0) {
    		val paused = anim.isPaused

    		anim.stop
    		anim.setFPS(newFps)
    		anim.start

    		if(paused) anim.pause
    	} else {
    		throw new RuntimeException(s"invalid FPS ${newFps}, please specify a number > 0")
    	}
    }

    /** Set the delay between a button press and release above which this is considered as a long press event. */
    def longPressDelay(delayMs:Long) {
    	pressDelay = delayMs * 100000
    }

    /** Send motion events even when no mouse button is pressed ? */
    def sendNoButtonMotionEvents(on:Boolean) {
    	sendNoButMotionEvents = on
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

    /** Allow to track the fact we are sending events, waiting a end event, but exited the surface. This
      * field is used only in the EDT thread. */
    private[this] var inMotion = false

    /** Previous position of the mouse for Scroll events. This field is used only in the EDT thread. */
    private[this] var previousPos:Point3 = null

    /** Used to measure click duration. This field is used only in the EDT thread. */
    private[this] var clickTime:Long = 0L

    def windowDestroyNotify(ev:JoglWindowEvent) {}
    def windowDestroyed(e:JoglWindowEvent) {}
    def windowGainedFocus(e:JoglWindowEvent) {} 
    def windowLostFocus(e:JoglWindowEvent) {} 
    def windowMoved(e:JoglWindowEvent) {}
    def windowRepaint(e:JoglWindowUpdateEvent) {} 
    def windowResized(e:JoglWindowEvent) {/*Console.err.println("resized w=%d h=%d".format(win.getWidth, win.getHeight))*/} 
    
    // ActionKey and Unicode

	def keyPressed(e:JoglKeyEvent) {
		if(e.isPrintableKey)
			 eventQueue.add(UnicodeEventJogl(e, -1))
		else eventQueue.add(ActionKeyEventJogl(e, -1))
	} 
	
	def keyReleased(e:JoglKeyEvent) {
		if(e.isPrintableKey)
			 eventQueue.add(UnicodeEventJogl(e, 1))
		else eventQueue.add(ActionKeyEventJogl(e, 1))
	}
	
	def keyTyped(e:JoglKeyEvent) {}
    
// Motion, Scroll, Scale, Click, DoubleClick

    def mouseClicked(e:JoglMouseEvent) {
    	// Not usable since when the click is too long, the event is not triggered.
    }

    def mouseEntered(e:JoglMouseEvent) {}
 
    def mouseExited(e:JoglMouseEvent) {
    	// Ensure we receive a motion end when the mouse leave the window.
    	// (Two case, when the mouse is released out of the window or when coming back in it).
    	if(inMotion) {
    		if(motionEvents)
    			eventQueue.add(MotionEventJogl(e, 1))

    		if(previousPos ne null) {
	    		eventQueue.add(ScrollEventJogl(e, 1, Vector3(e.getX-previousPos.x, e.getY-previousPos.y, 0)))
    			previousPos = null 
    		}

    		inMotion = false 
    	}
    }
 
    def mouseMoved(e:JoglMouseEvent) {
    	if(sendNoButMotionEvents)
			eventQueue.add(MotionEventJogl(e, 0))
    }
 
    def mousePressed(e:JoglMouseEvent) {
    	inMotion = true
    	clickTime = System.nanoTime
    	eventQueue.add(MotionEventJogl(e, -1)) 
   	}
 
    def mouseDragged(e:JoglMouseEvent) {
    	if(inMotion) {
    		if(motionEvents)
    			eventQueue.add(MotionEventJogl(e, 0)) 

    		if(hlEvents) {
    			if(previousPos eq null) {
    				previousPos = Point3(e.getX, e.getY, 0)
    				eventQueue.add(ScrollEventJogl(e, -1, Vector3(0,0,0)))
    			} else {
    				eventQueue.add(ScrollEventJogl(e, 0, Vector3(e.getX-previousPos.x, e.getY-previousPos.y, 0)))
    				previousPos.set(e.getX, e.getY, 0)
    			}
    		}
    	}
   	}
 
    def mouseReleased(e:JoglMouseEvent) {
    	if(inMotion) {
    		inMotion = false

    		if(motionEvents)
    			eventQueue.add(MotionEventJogl(e, 1))

    		if(previousPos ne null) {
    			eventQueue.add(ScrollEventJogl(e, 1, Vector3(e.getX-previousPos.x, e.getY-previousPos.y, 0)))
    			previousPos = null 
    		} else if(hlEvents) {
        		e.getButton match {
            		case 1 => {
            			if(e.getClickCount > 1) {
            				eventQueue.add(DoubleTapEventJogl(e))
            			} else {
	            			val t = System.nanoTime
    	        			
    	        			if((t - clickTime) > pressDelay) {
        	    			    eventQueue.add(LongPressEventJogl(e))
        	    			} else {
            					eventQueue.add(TapEventJogl(e))
            				}
							//println(s"** clickCount ${e.getClickCount} duration ${(t-clickTime)/100000} delay ${pressDelay}")
            			}
					}
            		case 3 => eventQueue.add(LongPressEventJogl(e))
            		case _ => {}
        		}
        	}
    	}
    }
 
    def mouseWheelMoved(e:JoglMouseEvent) {
    	if(hlEvents) {
    		if(mouseWheelSendsScale) {
				eventQueue.add(ScaleEventJogl(e, 0))
			} else {
				val a = e.getRotation
				eventQueue.add(ScrollEventJogl(e, 0, Vector3(0, a(1), 0)))
			}
    	}
    }

// -- Utility ---------------------
    
   	/** Process all pending events in the event queue. */
   	protected def processEvents() {
   		while(! eventQueue.isEmpty) {
   			eventQueue.poll match {
   				case e:ActionKeyEvent => { if(renderer.actionKey ne null) renderer.actionKey(this, e) }
   				case e:UnicodeEvent   => { if(renderer.unicode   ne null) renderer.unicode(  this, e) }
   				case e:MotionEvent    => { if(renderer.motion    ne null) renderer.motion(   this, e) }
   				case e:ShortCutEvent  => { if(renderer.shortCut  ne null) renderer.shortCut( this, e) }
   				// Special case gesture events:
   				case e:TapEvent => { 
   					needValidateSingleTap = System.nanoTime				
   					if(renderer.gesture ne null) renderer.gesture(this, e) 
   				}
   				case e:DoubleTapEvent => {
   					needValidateSingleTap = 0
   					if(renderer.gesture ne null) renderer.gesture(this, e) 
   				}
   				// All other gesture events:
   				case e:GestureEvent => {
   					if(renderer.gesture ne null) renderer.gesture(this, e) 
   				}
   				case _ => { throw new RuntimeException("unknown event") }
   			}
   		}
   	}

   	/** Validate a single tap if the double-tap time is ellapsed and no double tap
   	  * has been issued. */
   	protected def singleTapEventValidation() {
   		if(needValidateSingleTap > 0) {
   			val t = System.nanoTime
   			
   			if((t - needValidateSingleTap) > doubleTapDelay) {
   				if(renderer.gesture ne null)
   					renderer.gesture(this, SingleTapEventJogl(null, 0))
   				
   				needValidateSingleTap = 0
   			}
   		}
   	}

   	/** Print the capacities of the surface GL context. */
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
