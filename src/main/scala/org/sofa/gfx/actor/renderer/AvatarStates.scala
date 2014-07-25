package org.sofa.gfx.actor.renderer

import org.sofa.math.{Point3, Vector3}


/** Basic messages almos all avatars should handle. */
object AvatarBaseStates {

	/** Ask an avatar to resize to a given `size`. */
	case class Resize(size:Vector3) extends AvatarSpaceState {}

	/** Ask an avatar to jump to the given `position`. */
	case class MoveAt(postion:Point3) extends AvatarSpaceState {}

	/** Ask an avatar to move by an `offset`. */
	case class Move(offset:Vector3) extends AvatarSpaceState {}
}