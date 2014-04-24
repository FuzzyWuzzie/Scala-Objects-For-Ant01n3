package org.sofa.opengl.actor.renderer.avatar.game

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Box3Sized}
import org.sofa.opengl.{Texture, ShaderResource, ModelResource, TextureResource, TexParams}
import org.sofa.opengl.actor.renderer.{Screen}
import org.sofa.opengl.actor.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarRenderState}
import org.sofa.opengl.actor.renderer.{AvatarEvent, AvatarSpatialEvent, AvatarMotionEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarKeyEvent, AvatarZoomEvent}
import org.sofa.opengl.actor.renderer.{NoSuchAvatarException}

import org.sofa.opengl.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.opengl.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh, HexaTilesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


// == Renders ====================================================================


class IsoEntityRender(avatar:Avatar) extends IsoRender(avatar) with IsoRenderUtils {

	color = Rgba.White

	lineColor = Rgba.Black

	override def render() {
		//println(s"* render ${self.name}")
		val space = self.space
		val text  = screen.textLayer

		space.pushSubSpace
		fillAvatarBox
		text.font("Ubuntu-L.ttf", 13)
		text.color(Rgba.Black)
		text.string("Hello", 0.2, 0.1, 0, screen.space)
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

	var fromSpace = new Box3Sized {
		pos.set(0, 0, 0)
		size.set(1, 1, 1)
	}

	var toSpace = new Box3Sized {
		pos.set(0, 0, 0)
		size.set(1, 1, 1)
	}

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