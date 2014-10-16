package org.sofa.gfx.renderer

import scala.collection.mutable.{ArrayBuffer, HashMap}
import akka.actor.{ActorRef}

import org.sofa.math.{Vector3, Point3, NumberSeq3, Rgba}
import org.sofa.collection.{SpatialCube, SpatialHash, SpatialHashException}
import org.sofa.gfx.{Space, TextureFramebuffer}
import org.sofa.gfx.transforms.{SpaceTransform}


/** All specific avatar messages */
trait AvatarState {}


/** When an avatar is not found. */
case class NoSuchAvatarException(msg:String) extends Exception(msg)


/** When a state does not exist in an avatar or screen. */
case class NoSuchAvatarStateException(state:AvatarState) extends Exception(state.toString)


/** Represents the layer framebuffer / bitmap where the avatar may be rendered 
  * so that the layer is used instead of redrawing the avatar constantly.
  *
  * Keep in mind that avatars have a tri-dimensional size in real numbers.
  * The layer is a frame-buffer in two dimensions with integer (pixel)
  * size.
  *
  * Often the X and Y axis are used to find the size of the framebuffer using
  * dpc. Most of the time the size is rounded to the closest larger integer value (ceil),
  * meaning that the size of the layer is alway the same or larger. The scaling
  * factors to pass from the original size to the size of the frame-buffer are
  * stored in `scalex` and `scaley`. These scales are not directly factors from
  * the real size (in whatever unit it is) toward pixels, it is a ratio from the size
  * to another size (in the same unknown units) that represent the enlarged version of
  * the avatar to fit the new frame-buffer.
  *
  * @param fb The framebuffer object.
  * @param scalex The scaling factor to pass from the avatar width and
           the integer width in pixels of the framebuffer.
  * @param scaley Same as scalex for the avatar and framebuffer height. */
case class AvatarLayer(fb:TextureFramebuffer, scalex:Double, scaley:Double) {}


/** Graphical representation of an actor in the renderer.
  *
  * Avatars have a name (a path) that identify them uniquely in the hierarchy of
  * avatars. They also know the screen they are attached to actually.
  *
  * Avatars are containers for other avatars. This forms a hierarchy of
  * nested graphical elements.
  *
  * Avatars also maintain a set of "acquaintances", actor refs where
  * they can directly send events when interacted upon.
  *
  * Avatars also define their position and size as an [[AvatarSpace]] that can
  * be specific to each avatar. This space both defines the size and position
  * of the avatar inside its parent, and a "sub-space" for sub-avatars. Both
  * spaces can be the same. It can receive "animate()" calls, and is therefore
  * able to layout sub-avatars.
  *
  * Finally, avatars also define an [[AvatarRender]] object that is in charge of
  * representing the avatar on screen.
  *
  * This modularity allows to decouple the various tasks an avatar has to handle:
  *
  * - [[AvatarContainer]]: Hierarchy (mixed).
  * - [[AvatarInteraction]]: Events (contained or mixed).
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
	protected[this] var above:Avatar = null

	/** True after a call to [begin] and before [end]. */
	protected[this] var rendering = false

	/** Utility flag set to true by `AvatarRender.changeRender()` if something changed. Reset
	  * to false at the end of the render phase (therefore usable by sub-avatars to know
	  * if their parent render changed). */
	var renderChanged = true

	/** Utility flag set to true by `AvatarSpace.changeSpace()` if something changed. Reset
	  * to false at the end of the render phase (therefore usable by sub-avatars to know
	  * if their parent render changed). */
	var spaceChanged = true

	/** Some avatars draw inside their parent layer (recursively), others have their
	  * own rendering layer that can be composited onto the parent one. Layers allow
	  * to store the rendering in an offscreen buffer that can conveniently be reused
	  * as long as the sub-avatar hierarchy is not invalidated. See [[AvatarRender]]
	  * `pushLayer()`, `popLayer()` and `disposeLayer()`. */
	var hasLayer:Boolean = false

	/** The optionnal render layer. See `hasLayer`. */
	var layer:AvatarLayer = null

// Access to components

	/** The component of the avatar that handles position. */
	def space:AvatarSpace

	/** The component of the avatar that handles rendering. */
	def renderer:AvatarRender

	/** The component of the avatar that handles interactions. */
	def events:AvatarInteraction

// Interaction

	/** By default handles avatar space and render states otherwise throw a NoSuchAvatarStateException. */
	def change(state:AvatarState) {
		var used = false

		// Cannot use a match, since the state can be sent several times.
		if(state.isInstanceOf[AvatarSpaceState])  { used = true; space.changeSpace(state.asInstanceOf[AvatarSpaceState]) }
		if(state.isInstanceOf[AvatarRenderState]) { used = true; renderer.changeRender(state.asInstanceOf[AvatarRenderState]) }

		if(! used) {
			state match {
				case AvatarBaseStates.RenderFilter(filter) => renderFilter(filter)
				case AvatarBaseStates.RenderFilterRequest() => renderFilterRequest()
				case _ => throw new NoSuchAvatarStateException(state)
			}
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

	/** Parent avatar (read-only). */
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

	/** Render only the sub-avatars for which the space.isVisible method returns true.
	  * @return the number of visible sub-avatars rendered. */
	def renderVisibleSubs():Int

	/** Animate the space, the renderer and call `animateSubs()`. */
	def animate() { 
		renderFilterSubs
		space.animateSpace
		renderer.animateRender
		animateSubs
	}

	/** Animate the sub-avatars. */
	def animateSubs()

	/** Animate only the sub-avatars for which the space.isVisible method returns true.
	  * @return the number of visible sub-avatars animated. */
	def animateVisibleSubs():Int 

	/** Apply the given `code` to each sub-avatar. */
	def foreachSub(code:(Avatar)=>Unit)

	/** Apply the given `code` to each sub-avatar in the render filter. If there is
	  * no render filter this is applied to each sub-avatar. */
	def foreachFilteredSub(code:(Avatar)=>Unit)

	/** Find the first sub-avatar that matches the given `predicate`. */
	def findSub(predicate:(Avatar)=>Boolean):Option[Avatar]

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
	with AvatarContainerArray with AvatarInteraction {

	def events:AvatarInteraction = this
}


/** A default base avatar that mixes [[AvatarSpace]] and [[AvatarRender]].
  * 
  * This means that avatars inheriting this class cannot dynamically change their
  * space transformation and renderer. You have to override:
  *  - `render()`.
  * An eventually:
  *  - `animateRender()`,
  *  - `changeRender()`,
  *  - `pushSubSpace()` and `popSubSpace()`,
  *  - `animateSpace()`,
  *  - `changeSpace()`.
  */
abstract class DefaultAvatarMixed(name:AvatarName, screen:Screen) extends DefaultAvatar(name, screen) with AvatarSpace with AvatarRender {

	def space:AvatarSpace = this

	def renderer:AvatarRender = this

	override def animate() { animateSpace; animateRender; animateSubs }

	override def render() {
		super.render
		spaceChanged  = false
		renderChanged = false
	}

// Not needed and bugged (state can be sent to sapce AND render)
// 	override def change(state:AvatarState) {
// 		state match {
// 			case st:AvatarSpaceState => changeSpace(st)
// 			case st:AvatarRenderState => changeRender(st)
// 			case _ => throw NoSuchAvatarStateException(state)
// 		}
// 	}
}


/** A default base avatar that assumes the [[AvatarSpace]] and [[AvatarRender]]
  * are references to distinct interchangeable objects. */
abstract class DefaultAvatarComposed(name:AvatarName, screen:Screen) extends DefaultAvatar(name, screen) {

	def render() {
		renderer.render 
		spaceChanged  = false
		renderChanged = false
	}
}