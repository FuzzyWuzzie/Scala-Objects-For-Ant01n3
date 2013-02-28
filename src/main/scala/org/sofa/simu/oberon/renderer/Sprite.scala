package org.sofa.simu.oberon.renderer

import org.sofa.math.{Vector3, NumberSeq3, SpatialCube}
import org.sofa.opengl.{Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, VertexAttribute}

/** Specific avatar that implements a clickable element. */
abstract class Sprite(name:String, val screen:Screen) extends Avatar(name) {}

/** A sprite that display an unchanging image. */
class ImageSprite(name:String, screen:Screen, override val isIndexed:Boolean = false) extends Sprite(name, screen) {
	/** Shortcut to the GL. */
	protected val gl = screen.renderer.gl

	/** The sprite image. */
	protected var image:Texture = null

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

	override def change(axis:String, values:AnyRef*) {
		axis match {
			case "image" ⇒ {
				if(values(0).isInstanceOf[String]) {
					image = screen.renderer.libraries.textures.get(gl, values(0).asInstanceOf[String])
				}
			}
			case _ ⇒ super.change(axis, values)
		}
	}

	def render() {
		val camera = screen.camera

		if(image ne null) {
			gl.enable(gl.BLEND)
			imageShader.use
			image.bindUniform(gl.TEXTURE0, imageShader, "texColor")
			camera.pushpop {
				camera.scaleModel(size.x, size.y, size.z)
				camera.setUniformMVP(imageShader)
				imageMesh.lastVertexArray.draw(imageMesh.drawAs)
			}
			gl.disable(gl.BLEND)
		}
	}

	def animate() {

	}

	override def end() {
		super.end
	}
}

/** A sprite that may display several images, depending on its state. */
class AnimationSprite(name:String, screen:Screen) extends Sprite(name, screen) {
	override def begin() { super.begin }

	// override def change(axis:String, values:AnyRef*) {

	// }

	def render() {}

	def animate() {}

	override def end() { super.end }
}

class SkeletonSprite(name:String, screen:Screen) extends Sprite(name, screen) {
	override def begin() { super.begin }

	// override def change(axis:String, values:AnyRef*) {

	// }

	def render() {}

	def animate() {}

	override def end() { super.end }
}