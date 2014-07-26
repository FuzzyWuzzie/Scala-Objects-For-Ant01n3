package org.sofa.gfx.backend

import org.sofa.math.{Vector3, Point3}
import org.sofa.gfx.{SGL, Camera}
import org.sofa.gfx.surface._
import org.sofa.gfx.surface.event._

import android.view.{InputEvent=>AndroidInputEvent, MotionEvent=>AndroidMotionEvent, KeyEvent=>AndroidKeyEvent, InputDevice=>AndroidInputDevice}


/** Base utility class for Android event implementations. */
abstract class EventAndroid() {
	def source:AnyRef

	def device:DeviceType.Value = {
		import DeviceType._
		import AndroidInputDevice._
		val e = source.asInstanceOf[AndroidInputEvent]
		e.getDevice.getId match {
			case SOURCE_DPAD			 => Keyboard
			case SOURCE_GAMEPAD			 => Game
			case SOURCE_JOYSTICK		 => Game
			case SOURCE_KEYBOARD		 => Keyboard
			case SOURCE_MOUSE			 => Mouse
			case SOURCE_STYLUS			 => Pen
			case SOURCE_TOUCHPAD		 => Touch
			case SOURCE_TOUCHSCREEN		 => Touch
//			case SOURCE_TOUCH_NAVIGATION => Touch 			// API >= 18
			case SOURCE_TRACKBALL		 => Mouse
			case SOURCE_UNKNOWN			 => Unknown
			case _                       => Unknown
		}
	}
}


abstract class KeyEventAndroid(override val source:AndroidKeyEvent) extends EventAndroid() {
	
	def isStart:Boolean = (source.getAction == AndroidKeyEvent.ACTION_DOWN)

	def isEnd:Boolean = (source.getAction == AndroidKeyEvent.ACTION_UP)
}


abstract class SpatialEventAndroid(override val source:AndroidMotionEvent) extends EventAndroid() {
	
	def isStart:Boolean = (source.getActionMasked == AndroidMotionEvent.ACTION_DOWN)

	def isEnd:Boolean = (source.getActionMasked == AndroidMotionEvent.ACTION_UP)

    def position(pointer:Int=0):Point3 = Point3(source.getX(pointer), source.getY(pointer), 0)

    def pointerCount:Int = source.getPointerCount
}



// -- BASE EVENTS -------------------------------------------------





case class ActionKeyEventAndroid(override val source:AndroidKeyEvent)
	extends KeyEventAndroid(source) with ActionKeyEvent {
	
    def key:ActionKey.Value = {
        import AndroidKeyEvent._
        import ActionKey._

        source.getKeyCode match {
            case KEYCODE_PAGE_UP       => PageUp  
            case KEYCODE_PAGE_DOWN     => PageDown
            case KEYCODE_DPAD_UP       => Up
            case KEYCODE_DPAD_DOWN     => Down
            case KEYCODE_DPAD_RIGHT    => Right
            case KEYCODE_DPAD_LEFT     => Left
            case KEYCODE_ESCAPE        => Escape
            case KEYCODE_ALT_LEFT      => Alt
            case KEYCODE_ALT_RIGHT     => AltGr
            case KEYCODE_CTRL_LEFT     => Ctrl 
            case KEYCODE_CTRL_RIGHT    => Ctrl  
            case KEYCODE_META_LEFT     => Meta  
            case KEYCODE_META_RIGHT    => Meta  
            case KEYCODE_WINDOW        => Meta  
            case KEYCODE_SHIFT_LEFT    => Shift 
            case KEYCODE_SHIFT_RIGHT   => Shift 
            case KEYCODE_MOVE_HOME     => Begin 
            case KEYCODE_HOME          => Begin
            case KEYCODE_MOVE_END      => End   
            case _                     => Unknown
        }
    }
}


case class UnicodeEventAndroid(override val source:AndroidKeyEvent)
	extends KeyEventAndroid(source) with UnicodeEvent {
	
    def unicode:Char = source.getUnicodeChar.toChar
}


case class MotionEventAndroid(override val source:AndroidMotionEvent)
	extends SpatialEventAndroid(source) with MotionEvent {

    def button(button:Int, pointer:Int=0):Boolean = button match {
		case 1 => ((source.getButtonState & AndroidMotionEvent.BUTTON_PRIMARY)   != 0)
		case 2 => ((source.getButtonState & AndroidMotionEvent.BUTTON_SECONDARY) != 0)
		case 3 => ((source.getButtonState & AndroidMotionEvent.BUTTON_TERTIARY)  != 0)
		case _ => false
    }

    def pressure(pointer:Int=0):Double = source.getPressure(pointer)

    def tilt(pointer:Int=0):Vector3 = Vector3(0,0,0)
}


// // -- HI-LEVEL EVENTS -------------------------------------------


case class TapEventAndroid(override val source:AndroidMotionEvent)
	extends SpatialEventAndroid(source) with TapEvent {}


case class SingleTapEventAndroid(override val source:AndroidMotionEvent)
	extends SpatialEventAndroid(source) with SingleTapEvent {}


case class DoubleTapEventAndroid(override val source:AndroidMotionEvent)
	extends SpatialEventAndroid(source) with DoubleTapEvent {}


case class LongPressEventAndroid(override val source:AndroidMotionEvent)
	extends SpatialEventAndroid(source) with LongPressEvent {}


case class ScrollEventAndroid(override val source:AndroidMotionEvent, override val delta:Vector3)
	extends SpatialEventAndroid(source) with ScrollEvent {
	def velocity:Double = 0.0//throw new RuntimeException("TODO ScrollEventAndroid velocity")
}


case class ScaleEventAndroid(val delta:Double, time:Int=0)
	extends ScaleEvent {

	def source:AnyRef = null

	def position(pointer:Int=0):Point3 = Point3(0, 0, 0)

	def pointerCount:Int = 2

	def isStart:Boolean = time < 0

	def isEnd:Boolean = time > 0

	def device:DeviceType.Value = DeviceType.Touch
}


case class FlingEventAndroid(override val source:AndroidMotionEvent, override val velocity:Vector3)
	extends SpatialEventAndroid(source) with FlingEvent {
}


// TODO other gestures ??? Shortcuts ???

