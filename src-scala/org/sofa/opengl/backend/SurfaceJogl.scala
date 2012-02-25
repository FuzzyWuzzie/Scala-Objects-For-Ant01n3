package org.sofa.opengl.backend

import org.sofa.opengl.{SGL, Camera}
import org.sofa.opengl.surface._
import javax.media.opengl.{GLCapabilities, GLEventListener, GLAutoDrawable}
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.newt.event.{KeyEvent=>JoglKeyEvent, MouseEvent, MouseListener, WindowListener, KeyListener, WindowEvent, WindowUpdateEvent}
import com.jogamp.opengl.util.FPSAnimator
import javax.media.opengl.glu.GLU

class SurfaceNewt(
    renderer:SurfaceRenderer,
    val camera:Camera,
    val title:String,
    val caps:GLCapabilities)
	extends Surface(renderer)
	with    WindowListener
	with    KeyListener
	with    MouseListener
	with    GLEventListener {

    var fps = 30
    val win = GLWindow.create(caps)
    val anim = new FPSAnimator(win, fps)
    
    win.addWindowListener(this)
    win.addGLEventListener(this)
    win.addMouseListener(this)
    win.addKeyListener(this)
    win.setSize(camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
    win.setTitle(title)
    win.setVisible(true)
    anim.start
    
    val gl:SGL = new SGLJogl(win.getGL.getGL3, GLU.createGLU)
    
    def swapBuffers():Unit = win.swapBuffers
    
    def init(win:GLAutoDrawable) { renderer.initSurface() }
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) { renderer.surfaceChanged(width, height) }
    def display(win:GLAutoDrawable) { renderer.frame() }
    def dispose(win:GLAutoDrawable) { renderer.close() }
    
    def windowDestroyNotify(ev:WindowEvent) {}
    def windowDestroyed(e:WindowEvent) {}
    def windowGainedFocus(e:WindowEvent) {} 
    def windowLostFocus(e:WindowEvent) {} 
    def windowMoved(e:WindowEvent) {}
    def windowRepaint(e:WindowUpdateEvent) {} 
    def windowResized(e:WindowEvent) {} 
    
	def keyPressed(e:JoglKeyEvent) {} 
	def keyReleased(e:JoglKeyEvent) {}
	def keyTyped(e:JoglKeyEvent) { renderer.key(new KeyEventJogl(e)) }
	
    def mouseClicked(e:MouseEvent) {
        e.getButton match {
            case 1 => { renderer.action(new ActionEvent) }
            case 3 => { renderer.configure(new ConfigureEvent) }
            case _ => {}
        }
    }
    def mouseEntered(e:MouseEvent) {}
    def mouseExited(e:MouseEvent) {}
    def mouseMoved(e:MouseEvent) {}
    def mousePressed(e:MouseEvent) { renderer.motion(new MotionEventJogl(e, true, false)) }
    def mouseDragged(e:MouseEvent) { renderer.motion(new MotionEventJogl(e, false, false)) }
    def mouseReleased(e:MouseEvent) { renderer.motion(new MotionEventJogl(e, false, true)) }
    def mouseWheelMoved(e:MouseEvent) { renderer.scroll(new ScrollEventJogl(e)) }
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
}