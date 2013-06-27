package org.sofa.opengl.avatar.renderer.sprite

import scala.math._
import scala.collection.mutable.{HashMap}

import org.sofa.math.Axis._
import org.sofa.math.{Vector3, NumberSeq3, SpatialCube, Axis}
import org.sofa.opengl.{Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, VertexAttribute}
import org.sofa.opengl.avatar.renderer.{Sprite, Screen, Avatar, AvatarIndex, AvatarState, AvatarIndex2D, NoSuchStateException}

/** ImageSprite companion object defining the messages that can be received (change()), and
  * the [[ImageSprite.Animator]] class that can be used to tell how the sprite moves and
  * reshapes itself. */
object ImageSprite {

	/** Declare a new state for the sprite, and associate it an image in the resources library.
	  * If the `change` field is true, the current state is changed with the new declared
	  * state as if a [[ChangeState]] message was received. */
	case class AddState(resourceName:String, state:String, change:Boolean) extends AvatarState

	/** Declare a new state for the sprite, and associate both an image from the resources library,
	  * and an animator that tells how to change the position and size of the sprite at each frame
	  * according to the current time. If the `change` field is true, the current state is
	  * changed with the new declared state as if a [[ChangeState]] message was received. */
	case class AddAnimatedState(resourceName:String, animator:Animator, state:String, change:Boolean) extends AvatarState

	/** Change the current state of the sprite. */
	case class ChangeState(state:String) extends AvatarState

	/** Change the orientation of the image, by giving the axis it is perpendicular to. By default the image is perpendicular to the Z axis. */
	case class ChangeOrientation(orientation:Axis) extends AvatarState

	/** An image sprite animator specifies  */
	trait Animator {
		/** True if the animator can define a next position. */
		def hasNextPosition:Boolean = true
		/** True if the animator can define a next size. */
		def hasNextSize:Boolean = true
		/** Provide the next position of the sprite at `time` by copying the position into the given number sequence. */
		def nextPosition(time:Long):NumberSeq3
		/** Provide the next dimension of the sprite at `time` by copying the size into the given number sequence. */
		def nextSize(time:Long):NumberSeq3
	}

	/** A state of an image sprite. */
	case class State(val animator:Animator, val texture:Texture, val avatar:Avatar) {
		def hasAnimator:Boolean = (animator ne null)
		def nextPosition(time:Long) {
			if(hasAnimator && animator.hasNextPosition) 
				avatar.screen.changeAvatarPosition(avatar.name, animator.nextPosition(time))
		}
		def nextSize(time:Long) {
		 	if(hasAnimator && animator.hasNextSize)
		 		avatar.screen.changeAvatarSize(avatar.name, animator.nextSize(time))
		}
	}
}

/** A sprite that displays one image at a time.
  *
  * The image sprite defines a set of images. Each image is associated to a state. Switching
  * the sprite state switches the image.
  * 
  * The size and position of the sprite can be animated separately for each sprite. */
class ImageSprite(name:String, screen:Screen, override val isIndexed:Boolean = false) extends Sprite(name, screen) {
	import ImageSprite._

	/** Shortcut to the GL. */
	protected val gl = screen.renderer.gl

	/** All the states. */
	protected val states = new HashMap[String,State]()

	/** Current state. */
	protected var state:State = null

	/** Shader for the image. */
	protected var imageShader:ShaderProgram = null

	/** Geometry to display the image. */
	protected var imageMesh = new PlaneMesh(2, 2, 1, 1, true)

	/** Orientation of the image along one of the three Cartesian axes. */
	protected var orientation = Axis.Z

	/** The spatial index anchor. */
	protected val idx:AvatarIndex = if(isIndexed) new AvatarIndex2D(this) else null

	override def index = idx

	/** True if there is actually a current state (that is the sprite is initialized). */
	def hasState:Boolean = (state ne null)

	override def begin() {
		super.begin
		import VertexAttribute._
		imageShader = screen.renderer.libraries.shaders.get(gl, "image-shader")
        imageMesh.newVertexArray(gl, imageShader, Vertex -> "position", TexCoord -> "texCoords")
	}

	override def changePosition(newPos:NumberSeq3) {
		super.changePosition(newPos)
	}

	override def changeSize(newSize:NumberSeq3) {
		super.changeSize(newSize)
	}

	override def change(st:AvatarState) {
		st match {
			case AddState(res:String, state:String, change:Boolean) ⇒ {
				val tex = screen.renderer.libraries.textures.get(gl, res)
				
				states += (state -> State(null, tex, this))
				
				if(change) changeState(state)
			}
			case AddAnimatedState(res:String, an:Animator, state:String, change:Boolean) ⇒ {
				val tex = screen.renderer.libraries.textures.get(gl, res)
				
				states += (state -> State(an, tex, this))

				if(change) changeState(state)
			}
			case ChangeState(state:String) ⇒ {
				changeState(state)
			}
			case ChangeOrientation(axis:Axis) ⇒ {
				changeOrientation(axis)
			}
			case _ ⇒ { super.change(st) }
		}
	}

	protected def changeState(st:String) {
		state = states.get(st).getOrElse(throw NoSuchStateException("avatar %s has no state %s".format(name, state)))
	}

	protected def changeOrientation(axis:Axis) {
		orientation = axis
	}

	def render() {
		val camera = screen.camera

		if(hasState) {
			gl.enable(gl.BLEND)
			imageShader.use
			state.texture.bindUniform(gl.TEXTURE0, imageShader, "texColor")
			camera.pushpop {
				camera.translate(pos.x, pos.y, pos.z)
				orientation match {
					case Axis.X ⇒ { camera.rotate(-Pi/2.0, Axis.Y) }
					case Axis.Y ⇒ { camera.rotate(-Pi/2.0, Axis.X) }
					case _      ⇒ { /* NOP */ }
				}
				camera.scale(size.x, size.y, size.z)
				camera.uniformMVP(imageShader)
				imageMesh.lastVertexArray.draw(imageMesh.drawAs)
			}
			gl.disable(gl.BLEND)
		}
	}

	def animate() {
		if(hasState) {
			state.nextPosition(0)
			state.nextSize(0)
		}
	}
}