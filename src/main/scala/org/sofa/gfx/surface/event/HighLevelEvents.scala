package org.sofa.gfx.surface.event

import org.sofa.math.{Point3, Vector3}


/** Mark any event that represent a high-level user action. Such events do not
  * concentrate on the device used, but on the action of the user. They are
  * often the combination of several base events. See [[BaseEvent]]. Hi-level
  * events are seen as actions or gestures, like a tap (or click) a double-tap
  * (or double-click) a long press, a fling, a scale gesture (or scroll
  * wheel), etc.
  *
  * A high-level event will not represent data from any kind of device
  * (although you may know the device). For example a [[TapEvent]] action
  * event is not necessarily considered a left-button mouse click, it is a
  * just a tap (although most often indeed a left-button mouse click on non-
  * tablet devices). The [[LongPressEvent]] action is also most often a right-
  * button mouse click on desktop.
  *
  * If you need to know the device, the buttons, etc, you can look at
  * [[BaseEvent]] and its descendants.
  *
  * High-level actions can also use the keyboard. Shortcuts are considered
  * high-level events.
  */
trait HighLevelEvent {}


// -- GESTURES ------------------------------------------------------------

trait GestureEvent extends SpatialEvent with HighLevelEvent {}


trait FlingEvent extends GestureEvent {
	/** Speed factor along three axes. */
	def velocity:Vector3

	override def toString():String = "Fling[velocity=%s] %s".format(velocity, if(isStart) "BEG" else if(isEnd) "END" else "")
}


trait ScrollEvent extends GestureEvent {
	/** Amount of displacement since last scroll event (cumulative). */
	def delta:Vector3

	/** Speed factor. */
	def velocity:Double

	override def toString():String = "Scroll[delta=%s velocity=%f] %s".format(delta, velocity, if(isStart) "BEG" else if(isEnd) "END" else "")
}


trait TapEvent extends GestureEvent { 
	override def toString():String = "Tap[] %s".format(if(isStart) "BEG" else if(isEnd) "END" else "")
}


trait SingleTapEvent extends GestureEvent {
	override def toString():String = "SingleTap[] %s".format(if(isStart) "BEG" else if(isEnd) "END" else "")	
}


trait DoubleTapEvent extends GestureEvent {
	override def toString():String = "DoubleTap[] %s".format(if(isStart) "BEG" else if(isEnd) "END" else "")
}


trait LongPressEvent extends GestureEvent {
	override def toString():String = "LongPress[] %s".format(if(isStart) "BEG" else if(isEnd) "END" else "")
}


trait ScaleEvent extends GestureEvent {
	/** Positive or negative amount of scale since last scale event (cumulative). */
	def delta:Double

	override def toString():String = "Scale[delta=%f] %s".format(delta, if(isStart) "BEG" else if(isEnd) "END" else "")
}


// -- SHORTCUTS -----------------------------------------------------------


trait ShortCutEvent extends Event with HighLevelEvent {
	/** Representation of the shortcut as a string.
	  * The keys always appear in the same order, separted by pipes '|', first the
	  * action keys, then the unicode chatacters:
	  * 1. The actions keys if several are present appear in alphabetical order.
	  *    the action names are the name of the constant in [[ActionKeys]].
	  * 2. he unicode characters always appear in alphabetical order.
	  * Examples:
	  *		- <CTRL>-<META>-u     = "CtrlMeta|u" (this is small letter u, not capital)
	  *		- <SHIFT>-U           = "Shift|U" (if shift is on, you cannot have small letter u)
	  *		- <ALT-GR>-<UP-ARROW> = "AltGrUp|" (no unicode)
	  *		- Y-V-Z               = "|vyz"
	  *     - <META>-B-A          = "Meta|ab"
	  */
	def shortcut:String

	override def toString():String = "ShortCut[%s] %s".format(shortcut, if(isStart) "BEG" else if(isEnd) "END" else "")
}