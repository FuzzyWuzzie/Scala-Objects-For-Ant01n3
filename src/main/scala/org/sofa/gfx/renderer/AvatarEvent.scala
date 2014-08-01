package org.sofa.gfx.renderer

import scala.collection.mutable.{ArrayBuffer, HashMap}
import org.sofa.math.{Point3, Vector3}
import akka.actor.{ActorRef}

import org.sofa.gfx.surface.event.Event


// // How events flow:
// //
// // - If their is a selection, the event is sent to the selection.
// // - Else
// //     - If the event is not a spatial event, send the event to the root avatar,
// //         - propagate it until the event is consumed.
// //     - Else propagate the event through the avatar hierarchy under the event location, until:
// //         - An avatar consumed the event,
// //         - or there are no sub avatar containing the position of the event, in this case the receiver is the last reached avatar,
// //         - ot there are no sub avatars, in this case, the receiver is the last reached avatar,
// //
// //

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
	def consumeOrPropagateEvent(event:Event):Boolean = {
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
	def consumeEvent(event:Event):Boolean

	// /** The avatar has been touched. By default this sends the event to all acquaintances. */
	// def touched(e:TouchEvent, name:AvatarName) {
	// 	if(acquaintances ne null) {
	// 		acquaintances.foreach {
	// 			_ ! Acquaintance.TouchEvent(name, e.isStart, e.isEnd) 
	// 		}
	// 	}
	// }
}