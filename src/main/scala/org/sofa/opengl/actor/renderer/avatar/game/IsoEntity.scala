package org.sofa.opengl.actor.renderer.avatar.game

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Box3Sized}
import org.sofa.opengl.{Texture, ShaderResource, ModelResource, TextureResource, TexParams}
import org.sofa.opengl.armature.{Armature}
import org.sofa.opengl.armature.behavior.{ArmatureBehavior}
import org.sofa.opengl.actor.renderer.{Screen}
import org.sofa.opengl.actor.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarRenderState}
import org.sofa.opengl.actor.renderer.{AvatarEvent, AvatarSpatialEvent, AvatarMotionEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarKeyEvent, AvatarZoomEvent}
import org.sofa.opengl.actor.renderer.{NoSuchAvatarException}

import org.sofa.opengl.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.opengl.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh, HexaTilesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


// == Renders ====================================================================


case class IsoEntityConfig(armature:String, behavior:String) extends AvatarRenderState {}


class IsoEntityRender(avatar:Avatar) extends IsoRender(avatar) with IsoRenderUtils {

	color = Rgba.White

	lineColor = Rgba.Black

	protected[this] var armature:Armature = null

	protected[this] var behavior:ArmatureBehavior = null

	override def changeRender(state:AvatarRenderState) {
		state match {
			case IsoEntityConfig(armature, behavior) => init(armature, behavior)
			case _                                   => super.changeRender(state)
		}
	}

	protected def init(armature:String, behavior:String) {
		val gl   = self.screen.gl
		this.armature = screen.libraries.armatures.get(gl, armature)
		this.behavior = screen.libraries.behaviors.get(gl, behavior)
	}

	override def render() {
		val space = self.space
		val text  = screen.textLayer
		val gl    = self.screen.gl

		space.pushSubSpace
			if(armature eq null) {
//				fillAvatarBox

				gl.enable(gl.BLEND)
				gl.disable(gl.DEPTH_TEST)
				gl.disable(gl.CULL_FACE)
				armature.display(gl, screen.space)
				gl.disable(gl.BLEND)
			}

			self.renderSubs

		space.popSubSpace		
	}
}


// == Spaces =====================================================================


/** Space for an entity.
  * The entity does not resizes the space, it only translates to its position.
  * The while game works in the world coordinates. */
class IsoEntitySpace(avatar:Avatar) extends IsoSpace(avatar) {
	var scale1cm = 1.0

	var fromSpace = new Box3PosCentered {
		pos.set(0, 0, 0)
		from.set(-0.5, 0, -0.5)
		to.set(0.5, 1, 0.5)
	}

	var toSpace = fromSpace

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def animateSpace() {
	}

	def pushSubSpace() {
		val space  = self.screen.space

 		scale1cm = self.parent.space.scale1cm

 		space.push
 		space.translate(fromSpace.pos.x, fromSpace.pos.y, 0)
	}

	def popSubSpace() {
		self.screen.space.pop
	}
}


// == Avatars ====================================================================


class IsoEntity(name:AvatarName, screen:Screen)
	extends IsoAvatar(name, screen) {

	var space = new IsoEntitySpace(this) 

	var renderer = new IsoEntityRender(this)

	def consumeEvent(event:AvatarEvent):Boolean = {
		//println("%s ignore event %s".format(name, event))
		false
	}
}