package org.sofa.simu.oberon.renderer

import scala.collection.mutable.HashMap

import org.sofa.math.{Rgba, Axes, AxisRange}
import org.sofa.opengl.Camera

/** A screen in the game.
  *
  * A screen is necessarily (actually) occupying the whole display.
  * Only one screen is active and rendered at a time.
  */
abstract class Screen(val renderer:Renderer) extends Renderable {
	/** OpenGL. */
	val gl = renderer.gl

	/** Frame buffer. */
	val surface = renderer.surface

	/** User space in this screen. */
	var axes = Axes(AxisRange(-1,1), AxisRange(-1,1), AxisRange(-1,1))

	/** Allow to move the view in this screen. */
	val camera = new Camera()

	/** Set of child avatars. */
	protected val avatars = new HashMap[String,Avatar]()

	/** The display changed appearance. */
	def reshape()

	/** Add an avatar. */
	def addAvatar(name:String, avatar:Avatar) { avatars += (name -> avatar) }

	/** Remove and avatar. */
	def removeAvatar(name:String) { avatars -= name }

	/** Get an avatar by its name. */
	def avatar(name:String):Avatar = avatars.get(name).getOrElse(throw NoSuchAvatar(name))

	/** Initialize the avatars. */
	protected def beginAvatars() { avatars.foreach { _._2.begin } }

	/** Render the avatars. */
	protected def renderAvatars() { avatars.foreach { _._2.render } }

	/** Animate the avatars. */
	protected def animateAvatars() { avatars.foreach { _._2.animate } }

	/** Finalize the avatars. */
	protected def endAvatars() { avatars.foreach { _._2.end } }
}

class MenuScreen(renderer:Renderer) extends Screen(renderer) {
	val clearColor = Rgba(1, 0, 0, 1)

	def begin() {
		gl.clearColor(clearColor)
		gl.clearDepth(1f)
	    
	    gl.enable(gl.DEPTH_TEST)
	    
	    gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CW)
        
        gl.disable(gl.BLEND)
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)

        beginAvatars
	}	

	def render() {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		renderAvatars
	}

	def reshape() {
	}

	def animate() {
		animateAvatars
	}

	def end() {
		endAvatars
	}
}