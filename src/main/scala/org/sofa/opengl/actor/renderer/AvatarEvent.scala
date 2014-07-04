package org.sofa.opengl.actor.renderer

import scala.collection.mutable.{ArrayBuffer, HashMap}
import org.sofa.math.{Point3, Vector3}
import akka.actor.{ActorRef}


// How events flow:
//
// - If their is a selection, the event is sent to the selection.
// - Else
//     - If the event is not a spatial event, send the event to the root avatar,
//         - propagate it until the event is consumed.
//     - Else propagate the event through the avatar hierarchy, until:
//         - An avatar consumed the event,
//         - or there are no sub avatar containing the position of the event, in this case the receiver is the last reached avatar,
//         - ot there are no sub avatars, in this case, the receiver is the last reached avatar,
//
// For 


/** An interaction event occured. */
trait AvatarEvent {
}


/** An interaction event occured at a given position in the screen space.
  * Root of all spatialized avatar events. */
trait  AvatarSpatialEvent extends AvatarEvent {
	/** Position of the only pointer or average of position of all cursors. */
	def position:Point3
}


/** Sent when the pointer is clicked and just after released. */
trait AvatarClickEvent extends AvatarSpatialEvent {
	def position:Point3
}


/** Sent when the pointer is clicked and released after a longer period of time, without moving. */
trait AvatarLongClickEvent extends AvatarSpatialEvent {
	def position:Point3
}


/** The pointer has been clicked and started moving. */
trait AvatarMotionEvent extends AvatarSpatialEvent {
	def position:Point3

	/** Number of cursors active at a time. */
	def pointerCount:Int

	/** Individual position of each pointer. */
	def position(i:Int):Point3

	def isStart:Boolean

	def isEnd:Boolean

	/** Pressure of the only pointer or average of pressurs of all cursors. */
	def pressure:Double

	/** Pressure of each individual pointer. */
	def pressure(i:Int):Double
}


/** A trait for zoom events (pinch on touch devices or scroll wheel on mouses for example). */
trait AvatarZoomEvent extends AvatarEvent {
	/** Positive or negative amount of zoom. */
	def amount:Double
}


/** A key has been pressed and released. */
trait AvatarKeyEvent extends AvatarEvent {
	/** An action (from [[org.sofa.opengl.surface.KeyEvent.ActionChar]]).
	  * This equals to action `Unknown` if another key is pressed. */
	def actionChar:org.sofa.opengl.surface.ActionChar.Value

	/** The unicode representation of the key if available. */
	def unicodeChar:Char 

	/** True if one of the control keys is pressed. */
	def isControlDown:Boolean
    
    /** True if the left alt key is pressed. */
    def isAltDown:Boolean
    
    /** True if the right alt key is pressed. */
    def isAltGrDown:Boolean
    
    /** True if one of the shift key is pressed. */
    def isShiftDown:Boolean
    
    /** True if one of the meta (or Windows or command) key is pressed. */
    def isMetaDown:Boolean
}


/** Represent the avatar acquaintances and a way to transmit events
  * and interaction occuring on the avatar to the world of actors
  * representing the controller and model.
  *
  * There are two services inside this trait:
  * - Collect a list of acquaintances that can receive hi-level notifications 
  *   when the avatar is interacted upon.
  * - Receive events from the renderer and propagate them to the right avatar.
  */
trait AvatarInteraction {
	/** The avatar interacted upon. */
	protected def self:Avatar 

	/** Set of actors interested in events from this avatar. Null as long as there are not acquaintances. */
	protected var acquaintances:ArrayBuffer[ActorRef] = null

	/** Add an actor as listener for events on this avatar. */
	def addAcquaintance(a:ActorRef) {
		if(acquaintances eq null)
			acquaintances = new ArrayBuffer[ActorRef]

		acquaintances += a
	}

	/** Remove an actor as listener for events on this avatar. */
	def removeAcquaintance(a:ActorRef) {
		if(acquaintances ne null)
			acquaintances -= a 
	}

	/** Either consume the event or propagate it through the avatar hierarchy.
	  * Returns true if consumed by this or the sub-hierarchy. */
	def consumeOrPropagateEvent(event:AvatarEvent):Boolean = {
		if(! consumeEvent(event)) {
			self.findSub { sub => sub.events.consumeOrPropagateEvent(event) } match {
				case Some(a) => true
				case _       => false
			}
		} else {
			true
		}
	}

	/** An event is sent directly to this avatar. */
	def consumeEvent(event:AvatarEvent):Boolean

	// /** The avatar has been touched. By default this sends the event to all acquaintances. */
	// def touched(e:TouchEvent, name:AvatarName) {
	// 	if(acquaintances ne null) {
	// 		acquaintances.foreach {
	// 			_ ! Acquaintance.TouchEvent(name, e.isStart, e.isEnd) 
	// 		}
	// 	}
	// }
}