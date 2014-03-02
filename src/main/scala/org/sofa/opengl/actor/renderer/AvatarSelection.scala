package org.sofa.opengl.actor.renderer

import scala.collection.mutable.HashSet


/** A set of avatars. */
class AvatarSelection extends HashSet[Avatar] {
	def broadcastEvent(event:AvatarEvent) {
		foreach { avatar => avatar.events.consumeEvent(event) }
	}
}