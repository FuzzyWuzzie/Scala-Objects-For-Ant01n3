package org.sofa.gfx.actor.renderer.avatar.ui

import org.sofa.math.{ Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Box3Sized }
import org.sofa.gfx.{ ShaderResource, Space }
import org.sofa.gfx.actor.renderer.{ Screen }
import org.sofa.gfx.actor.renderer.{ Avatar, DefaultAvatar, DefaultAvatarComposed, DefaultAvatarMixed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarBaseStates }
import org.sofa.gfx.surface.event._
import org.sofa.gfx.actor.renderer.{ NoSuchAvatarException }

import org.sofa.gfx.{ SGL, ShaderProgram } //, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.gfx.mesh.{ TrianglesMesh, Mesh, VertexAttribute, LinesMesh } //, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


/** A generic avatar that you can inherit to override its `paint()` method. 
  *
  * You can subclass this avatar an override its `paint()` method to easily
  * render in an avatar. This avoids to create a Render */
abstract class UIPaint(name: AvatarName, screen: Screen) extends DefaultAvatarMixed(name, screen) {

	protected[this] val fromSpace = new Box3Sized {
		pos.set(0, 0, 0)
		size.set(1, 1, 1)
	}

	protected[this] val toSpace = fromSpace

	def scale1cm = self.parent.space.scale1cm

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def render() {
		val gl = self.screen.gl
		val space = self.space

		space.pushSubSpace
			paint(gl, screen.space)
			self.renderSubs
		space.popSubSpace
	}

	override def pushSubSpace() {
		val space = self.screen.space

		space.push
		space.translate(fromSpace.pos)
		space.scale(fromSpace.size)
	}

	override def popSubSpace() { self.screen.space.pop }

	override def changeSpace(newState:AvatarSpaceState) {
		newState match {
			case AvatarBaseStates.Move(offset) ⇒ fromSpace.pos += offset
			case AvatarBaseStates.MoveAt(position) ⇒ fromSpace.pos.copy(position)
			case AvatarBaseStates.Resize(size) ⇒ fromSpace.size.copy(size)
			case x ⇒ println(s"MeshAvatar changeSpace unknown message: ${x}")
		}
	}

	def consumeEvent(event:Event): Boolean = {
		false
	}

	/** Called when the avatar needs to be rendered, after the space has been set-up.
	  * @param gl The OpenGL context.
	  * @param space The current matrix stack, as setup by the avatar. */
	def paint(gl:SGL, space:Space)
}
