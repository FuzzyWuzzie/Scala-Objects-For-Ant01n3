package org.sofa.gfx.renderer

import org.sofa.math.{Point3, Vector3}


/** Basic messages almos all avatars should handle. */
object AvatarBaseStates {

	/** Ask an avatar to resize to a given `size`. */
	case class Resize(size:Vector3) extends AvatarSpaceState {}

	/** Ask an avatar to jump to the given `position`. */
	case class MoveAt(postion:Point3) extends AvatarSpaceState {}

	/** Ask an avatar to move by an `offset`. */
	case class Move(offset:Vector3) extends AvatarSpaceState {}

	/** Ask an avatar to show only a sub-group of its direct sub-avatars
	  * according to a filter. The filter allows to select the avatars
	  * and also gives their rendering order. The fitler is run anew
	  * if an avatar is added or removed. A filter request can also be
	  * triggered either by sending a RenderFilterRequest or if some
	  * avatar directly triggers it. Send a [[RenderFilter]] with a null
	  * argument to remove the filter. */
	case class RenderFilter(filter:org.sofa.gfx.renderer.RenderFilter) extends AvatarState {}

	/** If a render filter has been set, ask for a new filering at 
	  * next frame. By default, a filter request is done when a new
	  * avatar is added or one is removed. */
	case class RenderFilterRequest() extends AvatarState {}
}