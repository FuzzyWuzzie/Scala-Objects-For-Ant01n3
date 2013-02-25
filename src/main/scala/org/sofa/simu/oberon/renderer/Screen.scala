package org.sofa.simu.oberon.renderer

import scala.collection.mutable.HashMap

import org.sofa.math.{Rgba, Axes, AxisRange}
import org.sofa.opengl.{Camera, Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, VertexAttribute}

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

		if(rendering) {
			reshape
		}
Console.err.println("screen(%s) new axes = %s".format(name, axes))
	}

	/** Something changed in an avatar of this screen. */
	def changeAvatar(name:String, axis:String, values:AnyRef*) {
		avatars.get(name).getOrElse(throw NoSuchAvatarException("screen %s does not contain avatar %s".format(name))).change(axis, values:_*)
	}

	// Renderable

	def begin() {
		rendering = true
		beginAvatars
	}	

	def render() {
		renderAvatars
	}

	def reshape() {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
	}

	def animate() {
		animateAvatars
	}

	def end() {
		endAvatars
		rendering = false
	}

	// Avatars.

	/** Add an avatar. */
	def addAvatar(name:String, avatar:Avatar) { avatars += (name -> avatar) }

	/** Remove and avatar. */
	def removeAvatar(name:String) { avatars -= name }

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

class MenuScreen(name:String, renderer:Renderer) extends Screen(name, renderer) {
	val clearColor = Rgba(1, 0, 0, 1)

	val background = new PlaneMesh(2, 2, 1, 1, true)

	var backgroundShader:ShaderProgram =null

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
Console.err.println("Screen(%s) -> new background %s -> %s".format(name, values(0), backgroundImage))
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
Console.err.println("Screen(%s) surface (%d x %d)".format(name, surface.width, surface.height))
		super.reshape
		camera.orthographic(axes)
	}

	override def animate() {
		super.animate
	}

	override def end() {
		super.end
	}

	protected def renderBackground() {
		if(backgroundImage ne null) {
			backgroundShader.use
			backgroundImage.bindUniform(gl.TEXTURE0, backgroundShader, "texColor")
			camera.pushpop {
				camera.translateModel(0.5, 0.5, 0.5)
				camera.setUniformMVP(backgroundShader)
				background.lastVertexArray.draw(background.drawAs)
			}
		}
	}
}