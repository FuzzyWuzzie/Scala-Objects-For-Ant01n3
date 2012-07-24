package org.sofa.opengl.backend

import org.sofa.opengl.{SGL, Camera}
import org.sofa.opengl.surface._
import javax.media.opengl.{GLCapabilities, GLEventListener, GLAutoDrawable}
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.newt.event.{KeyEvent=>JoglKeyEvent, MouseEvent, MouseListener, WindowListener, KeyListener, WindowEvent, WindowUpdateEvent}
import com.jogamp.opengl.util.FPSAnimator
import javax.media.opengl.glu.GLU

class SurfaceNewt(
    val renderer:SurfaceRenderer,
    val camera:Camera,
    val title:String,
    val caps:GLCapabilities)
	extends Surface
	with    WindowListener
	with    KeyListener
	with    MouseListener
	with    GLEventListener {

    protected var fps = 30
    protected var win:GLWindow = null
    protected var anim:FPSAnimator = null
    protected var sgl:SGL = null
    protected var w:Int = 0
    protected var h:Int = 0
    
    build
    
    protected def build() {
        win  =GLWindow.create(caps)
        anim = new FPSAnimator(win, fps)
        sgl  = null
        w    = camera.viewportPx.x.toInt
        h    = camera.viewportPx.y.toInt

	    win.addWindowListener(this)
	    win.addGLEventListener(this)
	    win.addMouseListener(this)
	    win.addKeyListener(this)
	    win.setSize(w, h)
	    win.setTitle(title)
	    win.setVisible(true)

	    anim.start
    }
    
    def gl:SGL = { if(sgl==null) sgl = new SGLJogl3(win.getGL.getGL3, GLU.createGLU); sgl }
    def swapBuffers():Unit = win.swapBuffers
    def width = w
    def height = h
    
    def init(win:GLAutoDrawable) { renderer.initSurface(gl, this) }
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) { w = width; h = height; if(renderer.surfaceChanged ne null) renderer.surfaceChanged(this) }
    def display(win:GLAutoDrawable) { if(renderer.frame ne null) renderer.frame(this) }
    def dispose(win:GLAutoDrawable) { if(renderer.close ne null) renderer.close(this) }
    
    def windowDestroyNotify(ev:WindowEvent) {}
    def windowDestroyed(e:WindowEvent) {}
    def windowGainedFocus(e:WindowEvent) {} 
    def windowLostFocus(e:WindowEvent) {} 
    def windowMoved(e:WindowEvent) {}
    def windowRepaint(e:WindowUpdateEvent) {} 
    def windowResized(e:WindowEvent) {} 
    
	def keyPressed(e:JoglKeyEvent) {} 
	def keyReleased(e:JoglKeyEvent) {}
	def keyTyped(e:JoglKeyEvent) { if(renderer.key ne null) renderer.key(this, new KeyEventJogl(e)) }
	
    def mouseClicked(e:MouseEvent) {
        e.getButton match {
            case 1 => { if(renderer.action ne null) renderer.action(this, new ActionEvent) }
            case 3 => { if(renderer.configure ne null) renderer.configure(this, new ConfigureEvent) }
            case _ => {}
        }
    }
    def mouseEntered(e:MouseEvent) {}
    def mouseExited(e:MouseEvent) {}
    def mouseMoved(e:MouseEvent) {}
    def mousePressed(e:MouseEvent) { if(renderer.motion ne null) renderer.motion(this, new MotionEventJogl(e, true, false)) }
    def mouseDragged(e:MouseEvent) { if(renderer.motion ne null) renderer.motion(this, new MotionEventJogl(e, false, false)) }
    def mouseReleased(e:MouseEvent) { if(renderer.motion ne null) renderer.motion(this, new MotionEventJogl(e, false, true)) }
    def mouseWheelMoved(e:MouseEvent) { if(renderer.scroll ne null) renderer.scroll(this, new ScrollEventJogl(e)) }
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
            case _            => Unknown
        }
    }
    
    def isControlDown:Boolean = source.isControlDown
    def isAltDown:Boolean = source.isAltDown
    def isAltGrDown:Boolean = source.isAltGraphDown
    def isShiftDown:Boolean = source.isShiftDown
    def isMetaDown:Boolean = source.isMetaDown
}

class ScrollEventJogl(source:MouseEvent) extends ScrollEvent {
    def amount:Int = source.getWheelRotation
}

class MotionEventJogl(source:MouseEvent, pressed:Boolean, released:Boolean) extends MotionEvent {
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
}