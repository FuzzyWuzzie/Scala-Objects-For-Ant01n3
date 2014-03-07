package org.sofa.opengl.actor.renderer

import scala.collection.mutable.{ArrayBuffer, HashMap}
import akka.actor.{ActorRef}

import org.sofa.math.{Vector3, Point3, NumberSeq3, Rgba}
import org.sofa.collection.{SpatialCube, SpatialHash, SpatialHashException}
import org.sofa.opengl.{Space}
import org.sofa.opengl.transforms.{SpaceTransform}


/** The base type of all messages going toward an avatar renderer. */
trait AvatarRenderState extends AvatarState {}


/** A renderer for an avatar.
  *
  * The role of the renderer is to change the appearance of the avatar.
  * The renderer acts knowing the space of the avatar.
  */
trait AvatarRender {
	/** The avatar being rendered. */
	protected def self:Avatar 

	/** The screen where the avatar is rendered. */
	def screen:Screen

	/** Called at each frame before `render()` when the avatar is animated. */
	def animateRender() {}

	/** Change the renderer. */
	def changeRender(state:AvatarRenderState) {}

	/** By default push the avatar sub-space, render the sub-avatars then
	  * pop the avatar sub-space (all this is done by the `renderSubs()`
	  * utility method). You can override this behavior, and eventually
	  * render each sub when you want (and if you want). All the resources
	  * to draw are located in the `screen`  or given by the `avatar`. See
	  * [[Avatar.space]], [[Screen.gl]], [[Screen.surface]], and [[Screen.space]]. */
	def render() {
		self.space.pushSubSpace
		self.renderSubs
		self.space.popSubSpace		
	}
}