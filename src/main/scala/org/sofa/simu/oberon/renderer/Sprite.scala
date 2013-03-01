package org.sofa.simu.oberon.renderer

import scala.collection.mutable.{HashMap}

import org.sofa.math.{Vector3, NumberSeq3, SpatialCube}
import org.sofa.opengl.{Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, VertexAttribute}

/** Specific avatar that implements a clickable element. */
abstract class Sprite(name:String, val screen:Screen) extends Avatar(name) {}

object ImageSprite {
	/** Declare a new state for the sprite, and associate it an image in the resources library.
	  * If the `change` field is true, the current state is changed with the new declared
	  * state as if a [[ChangeState]] message was received. */
	case class AddState(resourceName:String, state:String, change:Boolean) extends AvatarState {}

	/** Change the current state of the sprite. */
	case class ChangeState(state:String) extends AvatarState {}
}

/** A sprite that display an unchanging image. */
class ImageSprite(name:String, screen:Screen, override val isIndexed:Boolean = false) extends Sprite(name, screen) {
	/** Shortcut to the GL. */
	protected val gl = screen.renderer.gl

	/** The sprite images. */
	protected val images = new HashMap[String,Texture]()

	/** Current state. */
	protected var state:Texture = null

	/** Shader for the image. */
	protected var imageShader:ShaderProgram = null

	/** Geometry to display the image. */
	protected var imageMesh = new PlaneMesh(2, 2, 1, 1, true)

	/** The spatial index anchor. */
	protected val idx:AvatarIndex = if(isIndexed) new AvatarIndex(this) else null

	override def index = idx

	override def begin() {
		super.begin
		import VertexAttribute._
		imageShader = screen.renderer.libraries.shaders.get(gl, "image-shader")
        imageMesh.newVertexArray(gl, imageShader, Vertex -> "position", TexCoord -> "texCoords")
	}

	override def changeSize(newSize:NumberSeq3) {
		size.copy(newSize)	
		if(isIndexed) index.changedSize
	}

	override def change(st:AvatarState) {
		import ImageSprite._
		st match {
			case AddState(res:String, state:String, change:Boolean) ⇒ {
				images += (state -> screen.renderer.libraries.textures.get(gl, res))
				
				if(change)
					changeState(state)
			}
			case ChangeState(state:String) ⇒ {
				changeState(state)
			}
			case _ ⇒ { super.change(st) }
		}
	}

	protected def changeState(st:String) {
		state = images.get(st).getOrElse(throw NoSuchStateException("avatar %s has no state %s".format(name, state)))
	}

	def render() {
		val camera = screen.camera

		if(state ne null) {
			gl.enable(gl.BLEND)
			imageShader.use
			state.bindUniform(gl.TEXTURE0, imageShader, "texColor")
			camera.pushpop {
				camera.translateModel(pos.x, pos.y, pos.z)
				camera.scaleModel(size.x, size.y, size.z)
				camera.setUniformMVP(imageShader)
				imageMesh.lastVertexArray.draw(imageMesh.drawAs)
			}
			gl.disable(gl.BLEND)
		}
	}

	def animate() {
	}

	override def touched(e:TouchEvent) {
		super.touched(e)
	}

	override def end() {
		super.end
	}
}

/** A sprite that may display several images, depending on its state. */
class AnimationSprite(name:String, screen:Screen) extends Sprite(name, screen) {
	def render() {}
	def animate() {}
}

class SkeletonSprite(name:String, screen:Screen) extends Sprite(name, screen) {
	def render() {}
	def animate() {}
}