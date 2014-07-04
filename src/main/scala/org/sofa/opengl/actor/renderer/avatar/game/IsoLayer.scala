package org.sofa.opengl.actor.renderer.avatar.game

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Box3Sized}
import org.sofa.opengl.{Texture, ShaderResource, ModelResource, TextureResource, TexParams}
import org.sofa.opengl.actor.renderer.{Screen}
import org.sofa.opengl.actor.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarRenderState, AvatarBaseStates}
import org.sofa.opengl.actor.renderer.{AvatarEvent, AvatarSpatialEvent, AvatarMotionEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarKeyEvent, AvatarZoomEvent}
import org.sofa.opengl.actor.renderer.{NoSuchAvatarException}

import org.sofa.opengl.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.opengl.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh, HexaTilesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


// == Renders ====================================================================


class IsoLayerRender(avatar:Avatar) extends IsoRender(avatar) with IsoRenderUtils {
	
	val lightDir = Vector3(1, 1.5, 0)
	
	protected[this] var dir = 0.01

	override def animateRender() {
	}

	override def render() {
		//println(s"* render ${self.name}")
		self.space.pushSubSpace
		// fillAvatarBox
		self.renderSubs
		self.space.popSubSpace		
	}
}


// == Spaces =====================================================================


/** A 2D isometric layer or rendering. */
class IsoLayerSpace(avatar:Avatar) extends IsoSpace(avatar) {

 	var scale1cm = 1.0

 	var fromSpace = new Box3Sized {
 		pos.set(0,0,0)
 		size.set(1,1,1)
 	}

 	var toSpace = fromSpace

 	def thisSpace = fromSpace

 	def subSpace = toSpace

	override def changeSpace(newState:AvatarSpaceState) {
		newState match {
			case AvatarBaseStates.Move(offset) => {
				println("iso layers move todo ?")
			}
			case AvatarBaseStates.MoveAt(offset) => {
				println("iso layer moveat todo ?")
			}
			case AvatarBaseStates.Resize(size) => {
				println("iso layer resize todo ?")
			}
			case _ => super.changeSpace(newState)
		}
	}

 	override def animateSpace() {}

 	override def pushSubSpace() {
 		scale1cm  = self.parent.space.scale1cm		
		val space = self.screen.space

 		space.push
 		space.translate(fromSpace.pos.x, fromSpace.pos.y, fromSpace.pos.z)
 	}

 	override def popSubSpace() {
		self.screen.space.pop
 	}
}


// == Avatars ====================================================================


class IsoLayer(name:AvatarName, screen:Screen)
	extends IsoAvatar(name, screen) {

	var space = new IsoLayerSpace(this)

	var renderer = new IsoLayerRender(this)

	protected[this] var prevMotionEvent:AvatarMotionEvent = null

	def consumeEvent(event:AvatarEvent):Boolean = {
		//println("%s ignore event %s".format(name, event))
		false
	}
}