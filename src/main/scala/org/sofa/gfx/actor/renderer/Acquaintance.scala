package org.sofa.gfx.actor.renderer


/** Set of messages sent back by avatars when something occurs on them. */
object Acquaintance {
	case class Clicked()

	case class LongClicked()

	case class Key()

	case class Motion()
}