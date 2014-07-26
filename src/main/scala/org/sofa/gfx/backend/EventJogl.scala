package org.sofa.gfx.backend

import org.sofa.math.{Vector3, Point3}
import org.sofa.gfx.SGL
import org.sofa.gfx.surface._
import org.sofa.gfx.surface.event._

import java.awt.event.{MouseListener=>AWTMouseListener,KeyListener=>AWTKeyListener,WindowListener=>AWTWindowListener, KeyEvent=>AWTKeyEvent, MouseEvent=>AWTMouseEvent, WindowEvent=>AWTWindowEvent, MouseWheelListener=>AWTMouseWheelListener, MouseWheelEvent=>AWTMouseWheelEvent}

import com.jogamp.newt.opengl.GLWindow
import com.jogamp.newt.event.{NEWTEvent=>JoglEvent, KeyEvent=>JoglKeyEvent, MouseEvent=>JoglMouseEvent, MouseListener=>JoglMouseListener, WindowListener=>JoglWindowListener, KeyListener=>JoglKeyListener, WindowEvent=>JoglWindowEvent, WindowUpdateEvent=>JoglWindowUpdateEvent}


/** Base utility class for jogl event implementations. */
abstract class EventJogl(val time:Int = 0) {
	def isStart:Boolean = time < 0

	def isEnd:Boolean = time > 0
}


// -- BASE EVENTS -------------------------------------------------


case class ActionKeyEventJogl(override val source:JoglKeyEvent, override val time:Int = 0)
	extends EventJogl(time) with ActionKeyEvent {
	
	def device:DeviceType.Value = DeviceType.Keyboard

    def key:ActionKey.Value = {
        import JoglKeyEvent._
        import ActionKey._

        source.getKeyCode match {
            case VK_PAGE_UP   => PageUp
            case VK_PAGE_DOWN => PageDown
            case VK_UP        => Up
            case VK_DOWN      => Down
            case VK_RIGHT     => Right
            case VK_LEFT      => Left
            case VK_ESCAPE    => Escape
            case VK_ALT       => Alt
            case VK_ALT_GRAPH => AltGr
            case VK_CONTROL   => Ctrl
            case VK_META      => Meta
            case VK_WINDOWS   => Meta
            case VK_SHIFT     => Shift
            case VK_HOME      => Begin
            case VK_BEGIN     => Begin
            case VK_END       => End
            case _            => Unknown
        }
    }
}


case class UnicodeEventJogl(override val source:JoglKeyEvent, override val time:Int = 0)
	extends EventJogl(time) with UnicodeEvent {
	
	def device:DeviceType.Value = DeviceType.Keyboard

    def unicode:Char = source.getKeyChar
}


case class MotionEventJogl(override val source:JoglMouseEvent, override val time:Int = 0)
	extends EventJogl(time) with MotionEvent {

	def device:DeviceType.Value = DeviceType.Mouse

    def position(pointer:Int=0):Point3 = Point3(source.getX(pointer), source.getY(pointer), 0)

    def pointerCount:Int = source.getPointerCount

    def button(button:Int, pointer:Int=0):Boolean = (source.getButton == button)

    def pressure(pointer:Int=0):Double = source.getPressure(pointer, true)

    def tilt(pointer:Int=0):Vector3 = Vector3(0,0,0)
}


// -- HI-LEVEL EVENTS -------------------------------------------


abstract class SpatialEventJogl(val source:JoglMouseEvent, time:Int = 0) extends EventJogl(time) {
    def position(pointer:Int=0):Point3 = Point3(source.getX(pointer), source.getY(pointer), 0)

    def pointerCount:Int = source.getPointerCount
}


case class TapEventJogl(override val source:JoglMouseEvent, override val time:Int = 0)
	extends SpatialEventJogl(source, time) with TapEvent {
	def device:DeviceType.Value = DeviceType.Mouse
}


case class SingleTapEventJogl(override val source:JoglMouseEvent, override val time:Int = 0)
	extends SpatialEventJogl(source, time) with SingleTapEvent {
	def device:DeviceType.Value = DeviceType.Mouse
}


case class DoubleTapEventJogl(override val source:JoglMouseEvent, override val time:Int = 0)
	extends SpatialEventJogl(source, time) with DoubleTapEvent {
	def device:DeviceType.Value = DeviceType.Mouse
}


case class LongPressEventJogl(override val source:JoglMouseEvent, override val time:Int = 0)
	extends SpatialEventJogl(source, time) with LongPressEvent {
	def device:DeviceType.Value = DeviceType.Mouse
}


case class ScrollEventJogl(override val source:JoglMouseEvent, override val time:Int = 0,
	override val delta:Vector3)
	extends SpatialEventJogl(source, time) with ScrollEvent {

	def device:DeviceType.Value = DeviceType.Mouse

	def velocity:Double = 0.0//throw new RuntimeException("TODO ScrollEventJogl velocity")
}


case class ScaleEventJogl(override val source:JoglMouseEvent, override val time:Int = 0)
	extends SpatialEventJogl(source, time) with ScaleEvent {

	def device:DeviceType.Value = DeviceType.Mouse

	def delta:Double = { val a = source.getRotation; a(1) }
}


// TODO other gestures ??? Shortcuts ???

