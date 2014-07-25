package org.sofa.gfx.actor.renderer

import scala.collection.mutable.HashSet

import org.sofa.gfx.surface.event.Event


/** A set of avatars. */
class AvatarSelection extends HashSet[Avatar] {
	def broadcastEvent(event:Event) {
		foreach { avatar => avatar.events.consumeEvent(event) }
	}
}