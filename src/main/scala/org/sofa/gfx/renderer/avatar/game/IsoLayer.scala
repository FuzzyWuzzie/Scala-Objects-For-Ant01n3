package org.sofa.gfx.renderer.avatar.game

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Box3Sized}
import org.sofa.gfx.{Texture, ShaderResource, ModelResource, TextureResource, TexParams}
import org.sofa.gfx.renderer.{Screen}
import org.sofa.gfx.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarRenderState, AvatarBaseStates}
import org.sofa.gfx.surface.event._
import org.sofa.gfx.renderer.{NoSuchAvatarException}

import org.sofa.gfx.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.gfx.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh}
import org.sofa.gfx.mesh.shapes.{HexaTilesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


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

	def consumeEvent(event:Event):Boolean = false
}