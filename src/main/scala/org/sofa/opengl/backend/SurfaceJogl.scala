package org.sofa.opengl.backend

import org.sofa.opengl.{SGL, Camera}
import org.sofa.opengl.surface._
import javax.media.opengl.{GLCapabilities, GLEventListener, GLAutoDrawable}
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.newt.event.{KeyEvent=>JoglKeyEvent, MouseEvent=>JoglMouseEvent, MouseListener=>JoglMouseListener, WindowListener=>JoglWindowListener, KeyListener=>JoglKeyListener, WindowEvent=>JoglWindowEvent, WindowUpdateEvent=>JoglWindowUpdateEvent}
import java.awt.{Frame=>AWTFrame}
import javax.media.opengl.{GLDrawableFactory, GLRunnable}
import javax.media.opengl.awt.GLCanvas
import java.awt.event.{MouseListener=>AWTMouseListener,KeyListener=>AWTKeyListener,WindowListener=>AWTWindowListener, KeyEvent=>AWTKeyEvent, MouseEvent=>AWTMouseEvent, WindowEvent=>AWTWindowEvent, MouseWheelListener=>AWTMouseWheelListener, MouseWheelEvent=>AWTMouseWheelEvent}
import com.jogamp.opengl.util.FPSAnimator
import javax.media.opengl.glu.GLU

object SurfaceNewtGLBackend extends Enumeration {
	val GL2ES2 = Value
	val GL3 = Value
}

class SurfaceGLCanvas(
    val renderer:SurfaceRenderer,
    var w:Int, var h:Int,
    val title:String,
    val caps:GLCapabilities,
    val backend:SurfaceNewtGLBackend.Value,
    var fps:Int)
    extends Surface
    with AWTWindowListener
    with AWTKeyListener
    with AWTMouseListener
    with AWTMouseWheelListener
    with GLEventListener {

    def this(renderer:SurfaceRenderer,
    		 camera:Camera,
    		 title:String,
    		 caps:GLCapabilities,
             backend:SurfaceNewtGLBackend.Value, fps:Int) { 
    	this(renderer, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt, title, caps, backend, fps)
    }	

    protected var win:AWTFrame = null
    protected var anim:FPSAnimator = null
    protected var sgl:SGL = null

    protected var canvas:GLCanvas = null

    build(backend)

    protected def build(backend:SurfaceNewtGLBackend.Value) {
        canvas = new GLCanvas(caps)
        win    = new AWTFrame()
        anim   = new FPSAnimator(canvas, fps)
        sgl    = null

        canvas.addGLEventListener(this)
        win.addWindowListener(this)
        win.addMouseListener(this)
        win.addMouseWheelListener(this)
        win.addKeyListener(this)
        win.add(canvas)
        win.setSize(w, h)
        win.setTitle(title)
        win.setVisible(true)

        anim.start
    }
    
    def gl:SGL = {
        if(sgl eq null) {
            sgl = backend match {
                case SurfaceNewtGLBackend.GL2ES2 => new SGLJogl2ES2(canvas.getGL.getGL2ES2, GLU.createGLU)
                case SurfaceNewtGLBackend.GL3    => new SGLJogl3(canvas.getGL.getGL3, GLU.createGLU)
            }
        }
        sgl
    }
    def swapBuffers():Unit = {}//win.swapBuffers // Automatic
    def width = w
    def height = h
    
    def init(win:GLAutoDrawable) { renderer.initSurface(gl, this) }
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) { w = width; h = height; if(renderer.surfaceChanged ne null) renderer.surfaceChanged(this) }
    def display(win:GLAutoDrawable) { if(renderer.frame ne null) renderer.frame(this) }
    def dispose(win:GLAutoDrawable) { if(renderer.close ne null) renderer.close(this) }

    def invoke(code:(Surface)=>Boolean) {
    	val me = this
    	canvas.invoke(false,
    		new GLRunnable() { override def run(win:GLAutoDrawable):Boolean = { code(me) } }
    	) 
   	}

    def invoke(runnable:Runnable) {
    	val me=this
    	canvas.invoke(false,
    		new GLRunnable() { override def run(win:GLAutoDrawable) = { runnable.run; true } }
    	)
   	}

    def windowActivated(e:AWTWindowEvent) {}
    def windowClosed(e:AWTWindowEvent) {}
    def windowClosing(e:AWTWindowEvent) {}
    def windowDeactivated(e:AWTWindowEvent) {}
    def windowDeiconified(e:AWTWindowEvent) {}
    def windowIconified(e:AWTWindowEvent) {}
    def windowOpened(e:AWTWindowEvent) {}

    def mouseClicked(e:AWTMouseEvent) {}
    def mouseEntered(e:AWTMouseEvent) {}
    def mouseExited(e:AWTMouseEvent) {}
    def mousePressed(e:AWTMouseEvent) { if(renderer.motion ne null) renderer.motion(this, new MotionEventAWT(e, true, false)) }
    def mouseReleased(e:AWTMouseEvent) { if(renderer.motion ne null) renderer.motion(this, new MotionEventAWT(e, false, true)) }

    def mouseWheelMoved(e:AWTMouseWheelEvent) {}

    def keyPressed(e:AWTKeyEvent) {}
    def keyReleased(e:AWTKeyEvent) {}
    def keyTyped(e:AWTKeyEvent) { if(renderer.key ne null) renderer.key(this, new KeyEventAWT(e)) }

    def resize(newWidth:Int, newHeight:Int) {
    	win.setSize(newWidth, newHeight)
    	w = newWidth
    	h = newHeight
    }
}

class KeyEventAWT(val source:AWTKeyEvent) extends KeyEvent {
    def isPrintable:Boolean = !source.isActionKey
    def unicodeChar:Char = source.getKeyChar
    def actionChar:ActionChar.Value = {
        import AWTKeyEvent._
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
    
    def isControlDown:Boolean = source.isControlDown
    def isAltDown:Boolean = source.isAltDown
    def isAltGrDown:Boolean = source.isAltGraphDown
    def isShiftDown:Boolean = source.isShiftDown
    def isMetaDown:Boolean = source.isMetaDown
}

class MotionEventAWT(source:AWTMouseEvent, pressed:Boolean, released:Boolean) extends MotionEvent {
    def deviceType:DeviceType.Value = DeviceType.Mouse
    def isStart:Boolean = pressed
    def isEnd:Boolean = released
    def x:Double = source.getX
    def y:Double = source.getY
    def pressure:Double = 1 //source.getPressure
    def x(pointer:Int):Double = source.getX
    def y(pointer:Int):Double = source.getY
    def pressure(pointer:Int):Double = 1 //source.getPressure(pointer)
    def pointerCount:Int = 1 //source.getPointerCount
    def sourceEvent:AnyRef = source
}

// =======================================================================================================================================
// == NEWT ===============================================================================================================================
// =======================================================================================================================================

class SurfaceNewt(
    val renderer:SurfaceRenderer,
    var w:Int, var h:Int,
    val title:String,
    val caps:GLCapabilities,
    val backend:SurfaceNewtGLBackend.Value,
    var fps:Int = 30,
    var decorated:Boolean = true)
	extends Surface
	with    JoglWindowListener
	with    JoglKeyListener
	with    JoglMouseListener
	with    GLEventListener {

    def this(renderer:SurfaceRenderer,
    		 camera:Camera,
    		 title:String,
    		 caps:GLCapabilities,
             backend:SurfaceNewtGLBackend.Value, fps:Int = 30, decorated:Boolean = true) { 
    	this(renderer, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt, title, caps, backend, fps, decorated)
    }	

    protected var win:GLWindow = null
    protected var anim:FPSAnimator = null
    protected var sgl:SGL = null
    
    build(backend)
    
    protected def build(backend:SurfaceNewtGLBackend.Value) {
        win  = GLWindow.create(caps)
        anim = new FPSAnimator(win, fps)
        sgl  = null

        win.setUndecorated(!decorated)
	   	win.setVisible(true)	
	   	// XXX The jogl specs tell to create the window before setting the size in order to know the native
	   	// XXX decoration insets. However it clearly do not work. Subsequent messages when the window is
	   	// resized will send the correct size, leading to an incoherent behavior (the sizes given cannot
	   	// to be trusted, when the window appear, the reshape receives the size with the insets, subsequent
	   	// resets will receive a size without the insets ... how to tell when ?).
		// Is this only on Os X ?

	    win.setSize(w+win.getInsets.getTotalWidth, h+win.getInsets.getTotalHeight)
	    win.setTitle(title)

	    win.addWindowListener(this)
	    win.addMouseListener(this)
	    win.addKeyListener(this)
	    win.addGLEventListener(this)

	    anim.start
    }
    
    def gl:SGL = {
    	if(sgl eq null) {
    		sgl = backend match {
        		case SurfaceNewtGLBackend.GL2ES2 => new SGLJogl2ES2(win.getGL.getGL2ES2, GLU.createGLU)
        		case SurfaceNewtGLBackend.GL3    => new SGLJogl3(win.getGL.getGL3, GLU.createGLU)
    		}
    	}
    	sgl
    }
    def swapBuffers():Unit = {}//win.swapBuffers // Automatic
    def width = w
    def height = h
    
    def init(win:GLAutoDrawable) { renderer.initSurface(gl, this) }
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) { w = width; h = height; if(renderer.surfaceChanged ne null) renderer.surfaceChanged(this) }
    def display(win:GLAutoDrawable) { if(renderer.frame ne null) renderer.frame(this) }
    def dispose(win:GLAutoDrawable) { if(renderer.close ne null) renderer.close(this) }
    
    def invoke(code:(Surface)=>Boolean) {
    	val me = this
    	win.invoke(false,
    		new GLRunnable() { override def run(win:GLAutoDrawable):Boolean = { code(me) } }
    	)
    }

    def invoke(runnable:Runnable) {
    	val me = this
    	win.invoke(false,
    		new GLRunnable() { override def run(win:GLAutoDrawable) = { runnable.run; true } }
    	)
   	}

    def windowDestroyNotify(ev:JoglWindowEvent) {}
    def windowDestroyed(e:JoglWindowEvent) {}
    def windowGainedFocus(e:JoglWindowEvent) {} 
    def windowLostFocus(e:JoglWindowEvent) {} 
    def windowMoved(e:JoglWindowEvent) {}
    def windowRepaint(e:JoglWindowUpdateEvent) {} 
    def windowResized(e:JoglWindowEvent) {Console.err.println("resized w=%d h=%d".format(win.getWidth, win.getHeight))} 
    
	def keyPressed(e:JoglKeyEvent) {} 
	def keyReleased(e:JoglKeyEvent) {}
	def keyTyped(e:JoglKeyEvent) { if(renderer.key ne null) renderer.key(this, new KeyEventJogl(e)) }
	
    def mouseClicked(e:JoglMouseEvent) {
        e.getButton match {
            case 1 => { if(renderer.action ne null) renderer.action(this, new ActionEvent) }
            case 3 => { if(renderer.configure ne null) renderer.configure(this, new ConfigureEvent) }
            case _ => {}
        }
    }
    def mouseEntered(e:JoglMouseEvent) {}
    def mouseExited(e:JoglMouseEvent) {}
    def mouseMoved(e:JoglMouseEvent) {}
    def mousePressed(e:JoglMouseEvent) { if(renderer.motion ne null) renderer.motion(this, new MotionEventJogl(e, true, false)) }
    def mouseDragged(e:JoglMouseEvent) { if(renderer.motion ne null) renderer.motion(this, new MotionEventJogl(e, false, false)) }
    def mouseReleased(e:JoglMouseEvent) { if(renderer.motion ne null) renderer.motion(this, new MotionEventJogl(e, false, true)) }
    def mouseWheelMoved(e:JoglMouseEvent) { if(renderer.scroll ne null) renderer.scroll(this, new ScrollEventJogl(e)) }

    def resize(newWidth:Int, newHeight:Int) {
    	win.setSize(newWidth, newHeight)
    	w = newWidth
    	h = newHeight
    }
}

class KeyEventJogl(val source:JoglKeyEvent) extends KeyEvent {
    def isPrintable:Boolean = !source.isActionKey
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
    
    def isControlDown:Boolean = source.isControlDown
    def isAltDown:Boolean = source.isAltDown
    def isAltGrDown:Boolean = source.isAltGraphDown
    def isShiftDown:Boolean = source.isShiftDown
    def isMetaDown:Boolean = source.isMetaDown
}

class ScrollEventJogl(source:JoglMouseEvent) extends ScrollEvent {
    def amount:Int = source.getWheelRotation.toInt
}

class MotionEventJogl(source:JoglMouseEvent, pressed:Boolean, released:Boolean) extends MotionEvent {
    def deviceType:DeviceType.Value = DeviceType.Mouse
    def isStart:Boolean = pressed
    def isEnd:Boolean = released
    def x:Double = source.getX
    def y:Double = source.getY
    def pressure:Double = source.getPressure
    def x(pointer:Int):Double = source.getX(pointer)
    def y(pointer:Int):Double = source.getY(pointer)
    def pressure(pointer:Int):Double = source.getPressure(pointer)
    def pointerCount:Int = source.getPointerCount
    def sourceEvent:AnyRef = source
    override def toString():String = "motion[%s%.1f, %.1f (%d)]".format(if(isStart) ">" else if(isEnd) "<" else "", x, y, pointerCount)
}