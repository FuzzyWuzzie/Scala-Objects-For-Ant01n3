package org.sofa.gfx.surface.event

import org.sofa.math.{Vector3, Point3}


//	- Base Events
//		- Motion (x, xy, xyz, Mouse, Pen, GameController, one ore more pointers at a time)
//		- Unicode (any Unicode character)
//		- ActionKey (ctrl, shift, meta, esc, arrows, game-controller-button, etc.)
//	- High-Level Events
//		- Gesture (Touch/Mouse/Pen)
//			- Fling
//			- Scroll
//			- ShowPress
//			- Tap (or click)
//			- DoubleTap (or double-click)
//			- LongPress (or configure)
//			- Scale
//		- Shortcut (composed events with zero or more Unicodes and zero or more Actions)


// -- DEVICES -----------------------------------------------------------


/** Predefined values for interaction device types. */
object DeviceType extends Enumeration {
// Devices

	final val Keyboard = Value
	final val Mouse    = Value
	final val Pen      = Value
	final val Touch    = Value
	final val Game     = Value

// Unknown

	final val Unknown = Value

// Type

	type DeviceType = Value
}


// -- ACTIONS KEYS ---------------------------------------------------------


/** Predefined and unique values of action keys. */
object ActionKey extends Enumeration {
// Base actions

	final val Escape   = Value
	final val PageUp   = Value
	final val PageDown = Value
	final val Up       = Value
	final val Down     = Value
	final val Right    = Value
	final val Left     = Value
	final val Begin    = Value // A.k.a. Home
	final val End      = Value

// Modifiers

	final val Alt   = Value
	final val AltGr = Value
	final val Ctrl  = Value
	final val Meta  = Value
	final val Shift = Value

// Game Controller

	final val A        = Value
	final val B        = Value
	final val C        = Value
	final val X        = Value
	final val Y        = Value
	final val Z        = Value
	final val Select   = Value
	final val Start    = Value
	final val R1       = Value
	final val R2       = Value
	final val L1       = Value
	final val L2       = Value

// Unknown

	final val Unknown  = Value

// Type

	type ActionKey = Value
}


// -- EVENT ------------------------------------------------------------


/** Generic event.
  *
  * Here an event is something occuring on an interaction device. The event
  * hierarchy in the [[Surface]] framework classify events in two main categories:
  *
  * - Base events, concentrated on reporting devices values (position, buttons, keys...).
  * - High-level events, concentrated on reporting user actions (gestures, double-clicks, scrolls...).
  *
  * All these kinds of events inherit this base trait. Some events (high-level or not) will
  * also inherit the [[SpatialEvent]] trait that marks events from devices or user actions
  * that have a position in space.
  */
trait Event {
	/** The kind of device sending this event. */
	def device:DeviceType.Value

	/** True if this event is the start of a sequence of same-kind events. */
	def isStart:Boolean

	/** True if this event is the end of a sequence of same-kind events. */
	def isEnd:Boolean

	/** Eventual underlying platform-dependant event. Can be null. */
	def source:AnyRef
}


/** Mark an event as having a position in space or a pointer. A spatial event may describe
  * several positions or pointers at once. This is usually the case with touch
  * devices where one or more fingers can be used to do an action. */
trait SpatialEvent extends Event {
	/** Actual position of the device pointer(s) in pixels with X axis positive going right,
	  * Y axis positive going down and origin at upper-left corner.
	  * By default the first pointer position is given. You can obtain the
	  * number of pointers with `pointerCount`. */
	def position(pointer:Int=0):Point3

	/** Number of pointers active for this event. Be careful that this is
	  * not the total number of possible pointers, but a variable number
	  * of pointers depending of the use of the device (think a multitouch
	  * device with one or more fingers on it). */
	def pointerCount:Int
}
