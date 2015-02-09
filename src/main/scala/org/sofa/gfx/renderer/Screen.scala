package org.sofa.gfx.renderer

import scala.collection.mutable.{HashMap, HashSet}
import akka.actor.{ActorRef}

import org.sofa.math.{Rgba, Axes, AxisRange, Point3, Vector3, NumberSeq3}
import org.sofa.collection.{SpatialHash, SpatialObject, SpatialPoint}
import org.sofa.gfx.{Space, ShaderResource, ScissorStack, TypeFaceResource}
import org.sofa.gfx.mesh.{LinesMesh, VertexAttribute}
import org.sofa.gfx.surface.event.{Event, MotionEvent}
import org.sofa.gfx.text.{TextLayer, GLFont, GLTypeFace}

import org.sofa.Timer


/** A message sent to a screen. */
trait ScreenState {}


/** Sent when a screen is not found. */
case class NoSuchScreenException(msg:String) extends Exception(msg)


/** When a state does not exist in a screen. */
case class NoSuchScreenStateException(state:ScreenState) extends Exception("unknown screen state %s (%s)".format(state, state.getClass.getName))


/** When the screen is used out of its rendering cycle (the screen is not current, its begin() method 
  * has not yet been called, or its end() method has been called). */
case class ScreenNotCurrentException(msg:String) extends Exception(msg)


/** The description of the space (model-view and projection matrix stacks). */
class ScreenSpace extends Space {}


/** Screen companion object. */
object Screen {
	/** Creates a [[DefaultScreen]]. */
	def apply(name:String, renderer:Renderer):Screen = new DefaultScreen(name, renderer)

	/** Change the screen global ratio. By default all given measures are respected
	  * in the metric system independently of the screen resolution. This ratio
	  * scales all. */
	case class ScreenRatio(ratio:Double) extends ScreenState {}
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

	/** The space where avatars are rendered, this is a way to tell were is the origin and how
	  * avatar coordinates are considered. */
	val space = new ScreenSpace()

	/** A special space where integer coordinates directly map to pixels. */
	val pixelSpace = new ScreenSpace()

	/** Set of avatars in the active selection. */
	val selection = new AvatarSelection

	/** Eventually scissors to select area of the screen to draw. */
	val scissors = ScissorStack()

// Hiden variable fields

	/** Layer of text above the screen. */
	protected[this] var text:TextLayer = null 					// TODO

	protected[this] var text2:org.sofa.gfx.dl.TextLayer = null	// TODO

	/** Set to true after begin() and reset to false after end(). */
	protected[this] var rendering = false

	/** The global rendering ratio, scales all measure in centimeters. */
	protected[this] var ratio = 1.0

// Modification

	/** Ask the renderer to re-render. Only needed if the renderer is not in continuous rendering mode. */
	def requestRender() { if(rendering) renderer.requestRender }

	/** Something changed in the screen. */
	def change(state:ScreenState) {
		state match {
			case Screen.ScreenRatio(ratio) => this.ratio = ratio
			case _ => throw new NoSuchScreenStateException(state)
		}
	}

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

	def textLayerDL:org.sofa.gfx.dl.TextLayer = {	// TODO
		if(!rendering) throw new ScreenNotCurrentException("cannot use the text layer if the screen is not current")

		if(text2 eq null) {
			libraries.shaders += ShaderResource("color-text-shader", "colortext.vert.glsl", "colortext.frag.glsl")
			text2 = new org.sofa.gfx.dl.TextLayer(gl, libraries.shaders.get(gl, "color-text-shader"))
			text2.reshape(surface.width, surface.height)
		}
		text2
	}

	/** Convert a value in millimeters to a font size suitable for the actual screen and system. */
	def mmToFontSize(value:Int):Int = surface.mmToFontSize(value, ratio)

	/** Dots per centimeters (how many device pixels per centimeters). */
	def dpc:Double = surface.dpc * ratio

	/** Retrieve a font of the given `typeFace` at a `size` given in millimeters. The actual size of
	  * the returned font in pixels may vary for the same `size` argument in millimeters if the 
	  * screen surface changed from one physical screen to another that have difference dots-per-centimeter. */
	def font(typeFace:String, size:Int):GLFont = {
		val sz = mmToFontSize(size)
		val tf = libraries.typeFaces.getOrAdd(gl, typeFace) { (gl, name) => 
				val shader = screen.libraries.shaders.getOrAdd(gl, "color-text-shader") { (gl, name) => 
					ShaderResource(name, "colortext.vert.glsl", "colortext.frag.glsl")
				}
				TypeFaceResource(name, "%s.ttf".format(name), shader) 
			}
		tf.font(sz)
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
		if(rendering) {
			if(hasSubs) {
Timer.timer.measure("screen.render") {
				renderAvatars 
}

				if(text2 ne null) {
Timer.timer.measure("screen.renderText2") {
					text2.render
}
				}
				if(text ne null) {
Timer.timer.measure("screen.renderText") {
					text.render(space)
}
				}
			} else {
				gl.clearColor(Rgba.Blue)
				gl.clear(gl.COLOR_BUFFER_BIT)
			}
		}
	}

	/** By default sets the size of the viewport to the size in pixels of the surface. */
	def reshape() {
		if(rendering) {
			space.viewport(surface.width, surface.height)
			pixelSpace.viewport(surface.width, surface.height)
			pixelSpace.viewIdentity
			pixelSpace.orthographicPixels()
			gl.viewport(0, 0, surface.width, surface.height) 
			if(text2 ne null) text2.reshape(surface.width, surface.height)
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
	def addAvatar(path:AvatarName, avatarType:String):Avatar = {
		val avatar = addSub(path, avatarType)

		if(rendering)
			avatar.begin

		avatar
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
Timer.timer.measure("changeAvatars") {
		var i = 0
		val n = pathes.length
		while(i < n) {
			changeAvatar(pathes(i), state)
			i += 1
		}
}
	}

	/** Change multiple avatars each with a distinct state. */
	def changesAvatars(changes:Array[RendererActor.ChangeAvatar]) {
Timer.timer.measure("changesAvatars") {
		var i = 0
		val n = changes.length
		while(i < n) {
			changeAvatar(changes(i).name, changes(i).state)
			i += 1
		}
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


/** A default screen class with an avatar container implemented by an array and a cache of avatar names to
  * speed-up avatar lookup. */
class DefaultScreen(name:String, renderer:Renderer) extends Screen(name, renderer) with AvatarContainerArray {
	// In tests the base AvatarContainer.avatar lookup is four to five times slower than using the hash-map.
	// The overrided methods are the one of screen and this works as long as only the screen is concerned as
	// an entry point (which should awlays be the case). This does not mean you cannot add avatars out of the
	// screen, but they will not be searchable via the screen.

	/** Cache of avatar-name -> avatar. */
	protected[this] val avatarNameCache = new HashMap[String, Avatar]()

	/** Override the base `addAvatar()` method to use an avatar name cache to speed-up avatar lookups. */
	override def addAvatar(path:AvatarName, avatarType:String):Avatar = {
		val avatar = super.addAvatar(path, avatarType)
		avatarNameCache += (path.toString -> avatar)
		avatar
	}

	/** Override the base `removeAvatar()` method to use an avatar name cache to speed-up avatar lookups. */
	override def removeAvatar(path:AvatarName) {
		removeSub(path)
		avatarNameCache -= path.toString
	}

	/** Override the base `avatar()` accessor to add an avatar name cache to speed-up avatar lookups. */
	override def avatar(path:AvatarName, prefix:Int = -1):Option[Avatar] = {
		if(prefix < 0) {
			avatarNameCache.get(path.toString)
		} else {
			super.avatar(path, prefix)
		}
	}
}
