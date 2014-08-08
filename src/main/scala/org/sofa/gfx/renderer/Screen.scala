package org.sofa.gfx.renderer

import scala.collection.mutable.{HashMap, HashSet}
import akka.actor.{ActorRef}

import org.sofa.math.{Rgba, Axes, AxisRange, Point3, Vector3, NumberSeq3}
import org.sofa.collection.{SpatialHash, SpatialObject, SpatialPoint}
import org.sofa.gfx.{Space, ShaderResource}
import org.sofa.gfx.mesh.{PlaneMesh, LinesMesh, VertexAttribute}
import org.sofa.gfx.surface.event.{Event, MotionEvent}
import org.sofa.gfx.text.TextLayer

import org.sofa.Timer


/** A message sent to a screen. */
trait ScreenState {}


/** Sent when a screen is not found. */
case class NoSuchScreenException(msg:String) extends Exception(msg)


/** When a state does not exist in a screen. */
case class NoSuchScreenStateException(state:ScreenState) extends Exception(state.toString)


/** When the screen is used out of its rendering cycle (the screen is not current, its begin() method 
  * has not yet been called, or its end() method has been called). */
case class ScreenNotCurrentException(msg:String) extends Exception(msg)


/** The description of the space (model-view and projection matrix stacks). */
class ScreenSpace extends Space {}


/** Screen companion object. */
object Screen {
	/** Creates a [[DefaultScreen]]. */
	def apply(name:String, renderer:Renderer):Screen = new DefaultScreen(name, renderer)
}


/** A screen.
  *
  * The role of a screen is to define a surface where avatars can be placed
  * and interacted upon.
  *
  * A screen is necessarily (actually) occupying the whole display.
  *
  * Only one screen is active and rendered at a time.
  *
  * A screen is :
  *
  * - An OpenGL surface.
  * - A space where by default origin is at the center, in X, Y inside [-1, 1] and Z (infinite).
  * - The root of a hierarchical set of avatars, each avatar being able to contain other avatars
  *   and modify the space.
  * - An entry point for the renderer to send and control avatars.
  * - An easy way to activate and deactivate a whole group of avatars.
  */
abstract class Screen(val name:String, val renderer:Renderer) extends Renderable with AvatarContainer {

// Visible constant fields

	val screen:Screen = this 	// Needed by AvatarContainer. Yes it allows to write screen.screen.screen.screen... :)

	/** Open Graphics Library. */
	val gl = renderer.gl

	/** Resources. */
	val libraries = renderer.libraries

	/** Frame buffer. */
	val surface = renderer.surface

	/** The space where things are rendered, this is a way to tell were is the origin and how
	  * avatar coordinates are considered. */
	val space = new ScreenSpace()

	/** Set of avatars in the active selection. */
	val selection = new AvatarSelection

	/** Layer of text above the screen. */
	protected[this] var text:TextLayer = null

// Hiden variable fields

	/** Set to true after begin() and reset to false after end(). */
	protected[this] var rendering = false

// Modification

	/** Something changed in the screen. */
	def change(state:ScreenState) {}

// Access

	/** Access to the text layer, above all screen rendering.
	  *
	  * The text layer role is to memorize text items and render them at the end,
	  * in an efficient way. */
	def textLayer:TextLayer = {
		if(!rendering) throw new ScreenNotCurrentException("cannot use the text layer if the screen is not current")

		if(text eq null) {
			libraries.shaders += ShaderResource("text-shader", "text.vert.glsl", "text.frag.glsl")
			text = new TextLayer(gl, libraries.shaders.get(gl, "text-shader"))
		}
		text
	}

// Interaction Events

	/** Propagate an event to sub avatars. 
	  * If there is a selection, the event is send to each member of the selection.
	  * Else the event is propagated to each sub-avatar, down the hierarchy, until one consumes it. */
	def propagateEvent(event:Event):Boolean = {
		if(selection.isEmpty) {
			findSub { sub => sub.events.consumeOrPropagateEvent(event) } match {
				case Some(a) => true
				case _       => false
			}
		} else {
			selection.broadcastEvent(event)
			true
		}
	}

	// Renderable -- Override these methods in subscreens.

	/** Set the [[rendering]] flag to true and send a begin signal to all child avatars using [[beginAvatars]]. */
	def begin() {
		rendering = true
		reshape
		beginAvatars
	}	

	/** By default renders all the child avatars using [[renderAvatars]].
	  * If the screen is empty, a blue background is drawn. */
	def render() {
Timer.timer.measure("screen.render") {
		if(rendering) {
			if(hasSubs) {
				renderAvatars 

				if(text ne null)
					text.render(space)
			} else {
				gl.clearColor(Rgba.Blue)
				gl.clear(gl.COLOR_BUFFER_BIT)
			}
		}
}
	}

	/** By default sets the size of the viewport to the size in pixels of the surface. */
	def reshape() {
		if(rendering) {
			space.viewportPx(surface.width, surface.height)
			gl.viewport(0, 0, surface.width, surface.height) 
		}
	}

	/** By default animate each avatar using [[animateAvatars]]. */
	def animate() { if(rendering) animateAvatars }

	/** By default send the end signal to all child avatars using [[endAvatars]], then
	  * set the [[rendering]] flag to false. Release any resource you hold here. */
	def end() {
		endAvatars
		rendering = false
	
		if(text ne null) {
			text.dispose
			text = null
		}
	}

// Avatars.

	/** Add an avatar (and send it the begin signal if the screen is rendering).
	  * The avatar `name` can ressemble a path to tell where to put the avatar
	  * inside the avatar hierarchy. The components of such pathes are separated
	  * by dots. Names without a dot are put as direct child of this screen. A
	  * name like "foo.bar" for examples creates an avatar "bar" as sub-avatar of
	  * "foo" which is child of the screen. Similarly a name "foo.bar.zub" creates
	  * an avatar "zub" as sub avatar of avatar "foo.bar". See [[AvatarName]].
	  * The `avatarType` will be passed to the avatar factory chain to create
	  * the appropriate kind of avatar. */
	def addAvatar(path:AvatarName, avatarType:String) {
		val avatar = addSub(path, avatarType)

		if(rendering)
			avatar.begin			
	}

	/** Like the regular `addAvatar` method but add as many avatars as there
	  * are entries in the given `pathes`. All the avatars will have the same
	  * `avatarType`. */
	def addAvatars(pathes:Array[AvatarName], avatarType:String) {
		var i = 0
		val n = pathes.length
		while(i < n) {
			addAvatar(pathes(i), avatarType)
			i += 1
		}
	}

	/** Remove an avatar (and send it the end signal if the screen is rendering).
	  * Does nothing if the avatar does not exist. */
	def removeAvatar(path:AvatarName) { removeSub(path) }

	/** Like the regular `removeAvatar` but removes all the avatars named in `pathes`. */
	def removeAvatars(pathes:Array[AvatarName]) {
		var i = 0
		val n = pathes.length
		while(i < n) {
			removeAvatar(pathes(i))
			i += 1
		}
	}

	/** Change an avatar state. */
	def changeAvatar(path:AvatarName, state:AvatarState) {
		avatar(path) match {
			case Some(avatar) => avatar.change(state) 
			case None => System.err.println("changeAvatar(%s) no such avatar".format(path.toString))
		}
	}

	/** Change multiple avatars with the same state. */
	def changeAvatars(pathes:Array[AvatarName], state:AvatarState) {
		var i = 0
		val n = pathes.length
		while(i < n) {
			changeAvatar(pathes(i), state)
			i += 1
		}
	}

	/** Change multiple avatars each with a distinct state. */
	def changesAvatars(changes:Array[RendererActor.ChangeAvatar]) {
		var i = 0
		val n = changes.length
		while(i < n) {
			changeAvatar(changes(i).name, changes(i).state)
			i += 1
		}
	}

	/** Ask the avatar `name` to send events to `acquaintance`. */
	def addAvatarAcquaintance(path:AvatarName, acquaintance:ActorRef) {
		avatar(path) match {
			case Some(avatar) => avatar.events.addAcquaintance(acquaintance) 
			case None => System.err.println("addAvatarAcquaintance(%s) no such avatar".format(path.toString))
		}
	}

	/** Get an avatar by its name. */
	def avatar(path:String):Option[Avatar] = avatar(AvatarName(path))

	/** Access to the given avatar. */
	def avatar(path:AvatarName, prefix:Int = -1):Option[Avatar]

	/** Factory for avatars. Used when adding an avatar. This does not add it, it only,
	  * creates it. */
	def createAvatar(path:AvatarName, kind:String):Avatar = renderer.factory.avatarFor(path, this, kind)

	/** To conform to [[AvatarContainer]], always null. */
	def self:Avatar = null

// For implementers.

	/** Initialize the avatars. */
	protected def beginAvatars() { foreachSub { _.begin } }

	/** Finalize the avatars. */
	protected def endAvatars() { foreachSub { _.end } }

	/** Render the avatars. */
	protected def renderAvatars() { renderSubs }

	/** Animate the avatars. */
	protected def animateAvatars() { animateSubs }
}


/** A default screen class with an avatar container implemented by an array. */
class DefaultScreen(name:String, renderer:Renderer) extends Screen(name, renderer) with AvatarContainerArray {}
