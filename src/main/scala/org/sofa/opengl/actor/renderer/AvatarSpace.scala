package org.sofa.opengl.actor.renderer

import org.sofa.math.{Point3, Vector3, Box3}


/** The base type of all messages going toward an avatar space. */
trait AvatarSpaceState extends AvatarState {}


/** Represent the avatar position and dimensions in the parent space as well as
  * a transformation to a sub-space for sub-avatars and the rendering of this
  * avatar.
  *
  * The avatar space represents the same things in two distinct points of view.
  * It first represent the dimensions and position of the avatar. These are
  * expressed in the space of the parent avatar. Then, it represents the same
  * space but as seen by sub-avatars and by the [[AvatarRender]] of this avatar.
  * In this case we call it sub-space. It defines
  * a transformation that push the new space onto graphic state. It then allows
  * to pop it back to restore the avatar space.
  */
trait AvatarSpace {
	/** The avatar. */
	protected def self:Avatar 

	/** The number of units that represent 1 centimeter inside the sub-space of this avatar avatar. */
	def scale1cm:Double

	/** Handle changes in the position and eventually communicate it to the sub avatars.
	  * If there is a layout for sub-avatars, it takes place here. */
	def animateSpace()

	/** Change the space. */
	def changeSpace(newState:AvatarSpaceState)

	/** Setup the sub-space for sub avatars. */
	def pushSubSpace()

	/** Revert to the space before the call to `push()`. */
	def popSubSpace()

	/** Position and size of this avatar in the parent space. */
	def thisSpace:Box3

	/** Position and size of this avatar in its own sub-space. */
	def subSpace:Box3
}