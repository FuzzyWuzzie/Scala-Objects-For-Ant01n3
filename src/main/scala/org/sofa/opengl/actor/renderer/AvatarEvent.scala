package org.sofa.opengl.actor.renderer

import scala.collection.mutable.{ArrayBuffer, HashMap}
import org.sofa.math.{Point3, Vector3}
import akka.actor.{ActorRef}


/** Represent the avatar acquaintances and a way to transmit events
  * and interaction occuring on the avatar to the world of actors
  * representing the controller and model.
  */
trait AvatarEvent {
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

	/** The avatar has been touched. By default this sends the event to all acquaintances. */
	def touched(e:TouchEvent, name:AvatarName) {
		if(acquaintances ne null) {
			acquaintances.foreach {
				_ ! Acquaintance.TouchEvent(name, e.isStart, e.isEnd) 
			}
		}
	}
}