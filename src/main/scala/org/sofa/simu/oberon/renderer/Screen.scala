package org.sofa.simu.oberon.renderer

import scala.collection.mutable.HashMap

import org.sofa.math.{Rgba, Axes, AxisRange}
import org.sofa.opengl.{Camera, Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, VertexAttribute}

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

	/** Frame buffer. */
	val surface = renderer.surface

	/** User space in this screen. */
	var axes = Axes(AxisRange(-1,1), AxisRange(-1,1), AxisRange(-1,1))

	/** Allow to move the view in this screen. */
	val camera = new Camera()

	/** Set of child avatars. */
	protected val avatars = new HashMap[String,Avatar]()

	/** Set to true after begin() and reset to false after end(). */
	protected var rendering = false

	/** Something changed in the screen. */
	def change(axis:String, values:AnyRef*)

	/** Change the axes and therefore the size of the drawing space in the screen (not related
	  * with the real pixel width). */
	def changeAxes(newAxes:Axes) {
		axes = newAxes

		if(rendering) reshape
	}

	/** Something changed in an avatar of this screen. */
	def changeAvatar(name:String, axis:String, values:AnyRef*) {
		avatars.get(name).getOrElse(throw NoSuchAvatarException("screen %s does not contain avatar %s".format(name))).change(axis, values:_*)
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

		if(rendering) avatar.begin
	}

	/** Remove and avatar (and send it the end signal if the screen is rendering).
	  * Does nothing if the avatar does not exist. */
	def removeAvatar(name:String) { 
		val avatar = avatars.get(name).getOrElse(null)

		if(avatar ne null) {
			avatar.end
			avatars -= name
		}
	}

	/** Get an avatar by its name. */
	def avatar(name:String):Avatar = avatars.get(name).getOrElse(throw NoSuchAvatarException(name))

	// For implementers.

	/** Initialize the avatars. */
	protected def beginAvatars() { avatars.foreach { _._2.begin } }

	/** Render the avatars. */
	protected def renderAvatars() { avatars.foreach { _._2.render } }

	/** Animate the avatars. */
	protected def animateAvatars() { avatars.foreach { _._2.animate } }

	/** Finalize the avatars. */
	protected def endAvatars() { avatars.foreach { _._2.end } }
}

/** A screen where an image serve as the background and several button sprites
  * can be added.
  *
  * The screen dimensions are given by the axes (user units) and the screen viewport in pixels.
  * The height of the screen in user units, is the one given. The width of the screen in user
  * units is the one given times the ratio width/height in pixels of the screen. This ensures
  * The full height is always visible, and most of the time, more than the width is visible as
  * screens will be used un landscape mode. */
class MenuScreen(name:String, renderer:Renderer) extends Screen(name, renderer) {
	/** Color for parts not covered by the background image. */
	val clearColor = Rgba(1, 0, 0, 1)

	/** The background plane. */
	val background = new PlaneMesh(2, 2, 1, 1, true)

	/** The background shader. */
	var backgroundShader:ShaderProgram =null

	/** The background imagE. */
	var backgroundImage:Texture = null

	override def begin() {
		gl.clearColor(clearColor)
		gl.clearDepth(1f)
	    
	    gl.enable(gl.DEPTH_TEST)
	    
	    gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CW)
        
        gl.disable(gl.BLEND)
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)

        reshape

        beginShader
        beginGeometry

		super.begin
	}

	protected def beginShader() {
		backgroundShader = renderer.libraries.shaders.get(gl, "image-shader")
	}

	protected def beginGeometry() {
		import VertexAttribute._
        background.newVertexArray(gl, backgroundShader, Vertex -> "position", TexCoord -> "texCoords")
	}

	def change(axis:String, values:AnyRef*) {
		axis match {
			case "background-image" => {
				backgroundImage = renderer.libraries.textures.get(gl, values(0).asInstanceOf[String])
			}
			case _ => {
				throw NoSuchAxisException(axis)
			}
		}
	}

	override def render() {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		renderBackground
		super.render
	}

	override def reshape() {
		super.reshape
		val ratio = camera.viewportRatio
		camera.orthographic(axes.x.from*(ratio), axes.x.to*(ratio), axes.y.from, axes.y.to, axes.z.to, axes.z.from)
	}

	override def animate() {
		super.animate
	}

	override def end() {
		super.end
	}

	protected def renderBackground() {
		// Origin is in the middle of the screen and of the image.
		if(backgroundImage ne null) {
			backgroundShader.use
			backgroundImage.bindUniform(gl.TEXTURE0, backgroundShader, "texColor")
			camera.pushpop {
				camera.scaleModel(backgroundImage.ratio, 1, 1)
				camera.setUniformMVP(backgroundShader)
				background.lastVertexArray.draw(background.drawAs)
			}
		}
	}
}