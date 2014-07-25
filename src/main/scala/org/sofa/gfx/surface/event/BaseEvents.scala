package org.sofa.gfx.surface.event

import org.sofa.math.{Vector3, Point3}


/** Any event that directly comes from underlying devices. Base event concentrate
  * on the repport of axes values (position, buttons, pressure, tilt) and keys (actions,
  * unicode). They are basic and may require to receive a sequence of events to
  * describe a specific action. See [[HighLevelEvent]] for events that may aggregate
  * several base events, and represent user actions instead of device axes.
  *
  * - An axe on a device is something that can change value in a domain.
  * - A key on a decice is something that can be on or off (strictly speaking, it is
  * a boolean axe, but it is  easier to consider them apart).
  *
  * In base events, some buttons are considered as axes others as keys, where
  * more appropriate.
  *
  * Users never see axes, this is only a way to consider things. Base events
  * build on axes to present events in a more usable way. */
trait BaseEvent {}


// -- KEYS ------------------------------------------------------------------------


/** Event for action keys. See the [[ActionKey]] contants. */
trait ActionKeyEvent extends Event {
	/** The action key pressed. */
	def key:ActionKey.Value

	override def toString():String = "ActionKey[%s] %s".format(key, if(isStart) "BEG" else if(isEnd) "END" else "")
}


/** Event for unicode keys, keys that can be mapped in the Unicode tables. */
trait UnicodeEvent extends Event {
	/** The character key pressed. */
	def unicode:Char

	override def toString():String = "Unicode[%c] %s".format(unicode, if(isStart) "BEG" else if(isEnd) "END" else "")
}


// -- AXES -------------------------------------------------------------------------


/** Event for any spatial device that moved to a new position or modified its button state,
  * pressure state or tilt state. */
trait MotionEvent extends SpatialEvent {
	/** Actual configuration of the device buttons. By default the buttons
	  * for the first pointer is given. You can obtain the number of pointers
	  * with `pointerCount`. */
	def button(button:Int,pointer:Int=0):Boolean

	/** Actual pressure of the device. By default the pressure for the first
	  * pointer is given. You can obtain the number of pointers with
        `pointerCount`. */
	def pressure(pointer:Int=0):Double

	/** Actual angles of the device. By default the pressure for the first
	  * pointer is given. You can obtain the number of pointers with
	  * `pointerCount`. */
	def tilt(pointer:Int=0):Vector3

	override def toString():String = "Motion[pos=%s] %s".format(position(), if(isStart) "BEG" else if(isEnd) "END" else "")
}