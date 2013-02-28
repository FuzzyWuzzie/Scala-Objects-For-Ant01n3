import javax.media.opengl.{GLCapabilities, GLEventListener, GLAutoDrawable, GLProfile}
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.newt.event.{KeyEvent=>JoglKeyEvent, MouseEvent=>JoglMouseEvent, MouseListener=>JoglMouseListener, WindowListener=>JoglWindowListener, KeyListener=>JoglKeyListener, WindowEvent=>JoglWindowEvent, WindowUpdateEvent=>JoglWindowUpdateEvent}
import javax.media.opengl.{GLDrawableFactory, GLRunnable}
import javax.media.opengl.awt.GLCanvas
import com.jogamp.opengl.util.FPSAnimator
import javax.media.opengl.glu.GLU

object TestGLWindow {
	def main(args:Array[String]) {
		(new TestGLWindow).test("test", 800, 600)
	}
}

class TestGLWindow 
 	extends JoglWindowListener
	with    JoglKeyListener
	with    JoglMouseListener
	with    GLEventListener {

	var win:GLWindow = null
	var anim:FPSAnimator = null

	def test(title:String, w:Int, h:Int) {
		val caps = new GLCapabilities(GLProfile.getGL2ES2)
		
		// caps.setRedBits(8)
		// caps.setGreenBits(8)
		// caps.setBlueBits(8)
		// caps.setAlphaBits(8)
		// caps.setNumSamples(4)
		// caps.setDoubleBuffered(true)
		// caps.setHardwareAccelerated(true)
		// caps.setSampleBuffers(true)

        win  = GLWindow.create(caps)
//        anim = new FPSAnimator(win, 24)

//	    win.addWindowListener(this)
	    win.addGLEventListener(this)
//	    win.addMouseListener(this)
//	    win.addKeyListener(this)
	    win.setSize(w, h)
	    win.setTitle(title)
	    win.setVisible(true)

//	    anim.start
	}

    def init(win:GLAutoDrawable) {}
    def reshape(win:GLAutoDrawable, x:Int, y:Int, width:Int, height:Int) { Console.err.println("height=%d".format(height)) }
    def display(win:GLAutoDrawable) {}
    def dispose(win:GLAutoDrawable) {}
    
    def windowDestroyNotify(ev:JoglWindowEvent) {}
    def windowDestroyed(e:JoglWindowEvent) { sys.exit }
    def windowGainedFocus(e:JoglWindowEvent) {} 
    def windowLostFocus(e:JoglWindowEvent) {} 
    def windowMoved(e:JoglWindowEvent) {Console.err.println("***MOVED to (%d, %d)".format(win.getX, win.getY))}
    def windowRepaint(e:JoglWindowUpdateEvent) {} 
    def windowResized(e:JoglWindowEvent) {Console.err.println("###RESIZED to %d x %d".format(win.getWidth, win.getHeight))} 
    
	def keyPressed(e:JoglKeyEvent) {} 
	def keyReleased(e:JoglKeyEvent) {}
	def keyTyped(e:JoglKeyEvent) {}
	
    def mouseClicked(e:JoglMouseEvent) {}
    def mouseEntered(e:JoglMouseEvent) {}
    def mouseExited(e:JoglMouseEvent) {}
    def mouseMoved(e:JoglMouseEvent) {}
    def mousePressed(e:JoglMouseEvent) {}
    def mouseDragged(e:JoglMouseEvent) {}
    def mouseReleased(e:JoglMouseEvent) {}
    def mouseWheelMoved(e:JoglMouseEvent) {}
}