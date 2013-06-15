package org.sofa.opengl.avatar.renderer

import scala.collection.mutable.{HashMap, HashSet}
import akka.actor.{ActorRef}

import org.sofa.math.{Rgba, Axes, AxisRange, Point3, Vector3, NumberSeq3, SpatialHash, SpatialObject, SpatialPoint}
import org.sofa.opengl.{Camera, Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, LinesMesh, VertexAttribute}
import org.sofa.opengl.surface.{MotionEvent}


/** Sent when a screen is not found. */
case class NoSuchScreenException(msg:String) extends Exception(msg)


/** A screen in the game.
  *
  * A screen is necessarily (actually) occupying the whole display.
  * Only one screen is active and rendered at a time.
  */
abstract class Screen(val name:String, val renderer:Renderer) extends Renderable {
	/** OpenGL. */
	val gl = renderer.gl

	/** The spatial index used in screens to retrieve indexed avatars. */
	type SpatialIndex = SpatialHash[SpatialObject, SpatialPoint, AvatarIndex]

	/** Frame buffer. */
	val surface = renderer.surface

	/** User space in this screen. */
	var axes = Axes(AxisRange(-1,1), AxisRange(-1,1), AxisRange(-1,1))

	/** Allow to move the view in this screen. */
	val camera = new Camera()

	/** Set of child avatars. */
	protected val avatars = new HashMap[String,Avatar]()

	/** Allow to quicly find objects at a given position, and detect collisions. */
	protected var spash:SpatialIndex = new SpatialIndex(1)

	/** Set to true after begin() and reset to false after end(). */
	protected var rendering = false

	// Acccess

	/** Width of the screen in game units. This is the maximum visible space, independant of any camera zoom. */
	def width:Double = axes.x.length

	/** Height of the screen in game units. This is the maximum visible space, independant of any camera zoom. */
	def height:Double = axes.y.length

	// Modification

	/** Something changed in the screen. */
	def change(axis:String, values:AnyRef*)

	/** Change the axes and therefore the size of the drawing space in the screen (not related
	  * with the real pixel width). */
	def changeAxes(newAxes:Axes, spashUnit:Double) {
		axes = newAxes
		changeSpashUnits(spashUnit)
		if(rendering) reshape
	}

	/** If the size of each bucket in the space index is changed, we need to create a new one,
	  * register each indexed avatar anew and replace it. */
	protected def changeSpashUnits(spashUnit:Double) {
		if(spashUnit != spash.bucketSize) {
			val newSpash = new SpatialIndex(spashUnit)

			if(avatars.size > 0) {
				avatars.foreach { item =>
					if(item._2.isIndexed) {
						item._2.index.spash = newSpash
						newSpash += item._2.index
					}
				}
			}

			spash = newSpash
		}
	}

	// Interaction Events

	/** The screen as been pinched or the mouse scroll wheel used. */
	def pinch(amount:Int) {	
	}

	/** The screen has been touched or the pointer moved. Return true if the touch event was sent to an avatar,
	  * else the touch event must be processed by the screen itself. */
	def motion(e:MotionEvent):Boolean = {
		val things = new HashSet[AvatarIndex]()
		val (xx, yy) = positionPX2GU(e.x, e.y)
		var touch = false

		spash.getThings(null, things, xx - 0.001, yy - 0.001, xx + 0.001, yy + 0.001)
		
		things.filter(thing ⇒ thing.contains(xx, yy, 0)).foreach { thing ⇒
			thing.touched(xx, yy, 0, e.isStart, e.isEnd)
			touch = true
		}

		touch
	}

	// Renderable

	/** Set the `rendering` flag to true and send a begin signal to all child avatars. */
	def begin() {
		rendering = true
		beginAvatars
	}	

	/** By default renders all the child avatars. */
	def render() {
		renderAvatars
	}

	/** By default sets the size of the viewport to the size in pixels of the surface. */
	def reshape() {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
	}

	/** By default animate each avatar. */
	def animate() {
		animateAvatars
	}

	/** By default send the end signal to all child avatars. */
	def end() {
		endAvatars
		rendering = false
	}

	// Avatars.

	/** Add an avatar (and send it the begin signal if the screen is rendering). */
	def addAvatar(name:String, avatar:Avatar) {
		avatars += (name -> avatar)

		if(avatar.isIndexed) {
			avatar.index.spash = spash
			spash += avatar.index
		}

		if(rendering) avatar.begin
	}

	/** Remove and avatar (and send it the end signal if the screen is rendering).
	  * Does nothing if the avatar does not exist. */
	def removeAvatar(name:String) { 
		avatars.get(name).foreach { avatar ⇒
			avatar.end
			
			if(avatar.isIndexed) {
				spash -= avatar.index
				avatar.index.spash = null
			}
			
			avatars -= name
		}
	}

	/** An avatar changed position. */
	def changeAvatarPosition(name:String, newPos:NumberSeq3) {
		avatar(name).changePosition(newPos)
	}

	/** An avatar changed size. */
	def changeAvatarSize(name:String, newSize:NumberSeq3) {
		avatar(name).changeSize(newSize)
	}

	/** Something changed in an avatar of this screen. */
	def changeAvatar(name:String, state:AvatarState) {
		avatar(name).change(state)
	}

	/** Ask the avatar `name` to send events to `acquaintance`. */
	def addAvatarAcquaintance(name:String, acquaintance:ActorRef) {
		avatar(name).addAcquaintance(acquaintance)
	}

	/** Get an avatar by its name. */
	def avatar(name:String):Avatar = avatars.get(name).getOrElse(throw NoSuchAvatarException("screen %s does not contain avatar %s".format(this.name,name)))

	// For implementers.

	/** Initialize the avatars. */
	protected def beginAvatars() { avatars.foreach { _._2.begin } }

	/** Render the avatars. */
	protected def renderAvatars() { avatars.foreach { _._2.render } }

	/** Animate the avatars. */
	protected def animateAvatars() { avatars.foreach { _._2.animate } }

	/** Finalize the avatars. */
	protected def endAvatars() { avatars.foreach { _._2.end } }

	// Utility

	/** Transform a position from pixels to game units. */
	protected def positionPX2GU(x:Double, y:Double):(Double, Double) = {
		(axes.x.from + (axes.x.length * (x / camera.viewportPx(0))),
		 axes.y.from + (axes.y.length * (1-(y / camera.viewportPx(1))))

		 )
	}

	/** Transform a length from pixels to game units. */
	protected def lengthPX2GU(length:Double):Double = {
		// By default we map lengths on the Y (the X can be larger since we use a rectangular aspect ratio in landscape).
		length * (axes.y.length / camera.viewportPx.y)
	}
}