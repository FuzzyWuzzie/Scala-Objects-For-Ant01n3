package org.sofa.opengl.actor.renderer

import scala.collection.mutable.{ArrayBuffer, HashMap}
import akka.actor.{ActorRef}

import org.sofa.math.{Vector3, Point3, NumberSeq3, Rgba}
import org.sofa.collection.{SpatialCube, SpatialHash, SpatialHashException}
import org.sofa.opengl.{Space}
import org.sofa.opengl.transforms.{SpaceTransform}


/** All specific avatar messages */
trait AvatarState {}


/** When an avatar is not found. */
case class NoSuchAvatarException(msg:String) extends Exception(msg)


/** When a state does not exist in an avatar or screen. */
case class NoSuchAvatarStateException(state:AvatarState) extends Exception(state.toString)


/** Graphical representation of an actor in the renderer.
  *
  * Avatars have a name (a path) that identify them uniquely in the hierarchy of
  * avatars. They also know the screen they are attached to actually.
  *
  * Avatars are containers for other avatars. This forms a hierarchy of
  * nested graphical elements.
  *
  * Avatars also maintain a set of "acquaintances" that is actor refs where
  * they can directly send events when interacted upon.
  *
  * Avatars also define their position and size as an [[AvatarSpace]] that can
  * be specific to each avatar. This space both defines the size and position
  * of the avatar inside its parent, and a "sub-space" for sub-avatars. It can
  * receive "animate()" calls, and is therefore able to layout sub-avatars.
  *
  * Finally, avatars also define an [[AvatarRender]] object that is in charge of
  * representing the avatar on screen.
  *
  * This modularity allows to decouple the various tasks an avatar has to handle:
  *
  * - [[AvatarContainer]]: Hierarchy (mixed).
  * - [[AvatarEvent]]: Events (contained or mixed).
  * - [[AvatarSpace]]: Position, Dimension for this avatar an space for sub-avatars (contained or mixed).
  * - [[AvatarRender]]: Graphical representation (contained or mixed).
  *
  * Furthermore, each module can be specific for each avatar. The modules are created when the
  * avatar is created by the screen, depending on the type of avatar. The three modules event, space
  * and render are traits. They can either be mixed with an avatar class (see [[DefaultAvatarMixed]])
  * or they can be distinct instances (see [[DefaultAvatarComposed]]). The first case avoid consuming
  * memory for distinct objects that will hold a reference to their container avatar. The second
  * solution allows to dynamically change the renderer and space/position during the avatar lifetime.
  *
  * The avatar class is a tool that allows to easilly build nodes of a render tree that can apply
  * to simulations, UI, or games, hence the flexibility in the composition of each component (container,
  * event, space, render).
  */
abstract class Avatar(
	var name:AvatarName,
	val screen:Screen) extends Renderable with AvatarContainer {

	/** The avatar parent, if any, set automatically when added into another avatar. */
	protected var above:Avatar = null

	/** True after begin and before end. */
	protected var rendering = false

// Access to components

	/** The component of the avatar that handles position */
	def space:AvatarSpace

	def renderer:AvatarRender

	def events:AvatarEvent

// Interaction

	/** By default handles avatar space and render states otherwise throw a NoSuchAvatarStateException. */
	def change(state:AvatarState) {
		state match {
			case st:AvatarSpaceState => space.changeSpace(st)
			case st:AvatarRenderState => renderer.changeRender(st)
			case _ => throw NoSuchAvatarStateException(state)
		}
	}

// Sub-Avatars

	/** Add a sub-avatar, either in this avatar or recursively in one
	  * of the sub-avatars, depending on the given `path`. The kind
	  * of avatar to create is also given as `avatarType`.
	  * If the `path` describe a non existing path (the container
	  * avatars cannot be found) a `NoSuchAvatarException` is raised. 
	  * The `prefix` if given, correspond to the position in the path. */
	def addSub(path:AvatarName, avatarType:String, prefix:Int = -1):Avatar

	/** Remove a sub-avatar, either in this avatar or recursively in one
	  * of the sub-avatrars, depending on the given `path`. If the
	  * avatar is not found or its path is invalid a `NoSuchAvatarException`
	  * is thrown.
	  */
	def removeSub(path:AvatarName, prefix:Int = -1):Avatar

	/** This avatar, used in traits that mix with Avatar. */
	protected def self:Avatar = this

	/** Reparent this avatar as a sub of `newParent`. */
	def reparent(newParent:Avatar) {
		// TODO, check this ? Check the name ? 
		above = newParent 
	}

	/** True if this avatar has a parent. */
	def hasParent:Boolean = (above ne null)

	/** Parent avatar read-only. */
	def parent:Avatar = above

// Renderable

	/** By default set the rendering flag to true. */
	def begin() { rendering = true }
	
	/** By default set the rendering flag to false. */
	def end() { rendering = false }

	/** Call the renderer. */
	def render()

	/** Render the sub-avatars. */
	def renderSubs()

	/** Animate the sapce, the renderer and call `animateSubs()`. */
	def animate() { space.animateSpace; renderer.animateRender; animateSubs }

	/** Animate the sub-avatars. */
	def animateSubs()

	def foreachSub(code:(Avatar) => Unit)

// Utility

	override def toString() = "%s(%d subs)".format(name, subCount)

	/** See [[AvatarContainer]]. */
	def hierarchyToString(depth:Int=0):String
}


/** A default avatar class with a container hierarchy implemented by an array
  * and a set of acquaintance also implemented by an array. */
abstract class DefaultAvatar(
		name:AvatarName,
		screen:Screen)
	extends Avatar(
		name, screen)
	with AvatarContainerArray with AvatarEvent {

	def events:AvatarEvent = this
}


/** A default base avatar that mixes [[AvatarSpace]] and [[AvatarRender]]. */
abstract class DefaultAvatarMixed(name:AvatarName, screen:Screen) extends DefaultAvatar(name, screen) with AvatarSpace with AvatarRender {

	def space:AvatarSpace = this

	def renderer:AvatarRender = this

	override def animate() { animateSpace; animateRender; animateSubs }

	override def change(state:AvatarState) {
		state match {
			case st:AvatarSpaceState => changeSpace(st)
			case st:AvatarRenderState => changeRender(st)
			case _ => throw NoSuchAvatarStateException(state)
		}
	}
}


/** A default base avatar that assumes the [[AvatarSpace]] and [[AvatarRender]]
  * are references to distinct interchangeable objects. */
abstract class DefaultAvatarComposed(name:AvatarName, screen:Screen) extends DefaultAvatar(name, screen) {

	def render() { renderer.render }
}