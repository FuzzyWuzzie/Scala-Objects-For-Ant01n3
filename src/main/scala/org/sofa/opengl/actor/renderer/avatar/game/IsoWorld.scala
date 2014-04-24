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


class IsoWorldRender(avatar:Avatar) extends IsoRender(avatar) with IsoRenderUtils {
	override def render() {
		//println(s"* render ${self.name}")
		self.space.pushSubSpace
		//fillAvatarBox
		self.renderSubs
		self.space.popSubSpace		
	}
}


// == Spaces =====================================================================


/** A 2D world in isometric projection. */
class IsoWorldSpace(avatar:Avatar) extends IsoSpace(avatar) {

 	var scale1cm = 1.0

// 	var itemHeight = 1.5	// cm

 	var fromSpace = new Box3From {
 		from.set(0,0,0)
 		to.set(1,1,1)
 	}

 	var toSpace = new Box3Sized {
 		pos.set(0,0,0)
 		size.set(2,2,2)
 	}

 	def thisSpace = fromSpace

 	def subSpace = toSpace

 	protected[this] var zoom = 1.0

 	protected[this] final val MoveFactor = 200.0

	override def changeSpace(newState:AvatarSpaceState) {
		newState match {
			case IsoWorldMove(direction) => {
				direction /= (MoveFactor/zoom)
				//toSpace.pos.set(toSpace.pos.x + direction.x, toSpace.pos.y - direction.y, toSpace.pos.z)
				toSpace.pos += direction
			}
			case IsoWorldZoom(amount) => {
				zoom += amount / MoveFactor
				if(zoom < 1) zoom = 1
			}
			case _ => super.changeSpace(newState)
		}
	}

 	override def animateSpace() {
 		val fsize   = fromSpace.size
		val ratiohw = fsize.x / fsize.y

		toSpace.size.set(2*zoom, 2*zoom*ratiohw, 2*zoom)

 		layoutSubs
 	}

 	def pushSubSpace() {
 		val space = avatar.screen.space
 		val fsize = fromSpace.size
 		val tsize = toSpace.size

		scale1cm = self.parent.space.scale1cm * 0.5 * tsize.x

 		space.push
 		space.translate(fsize.x/2, fsize.y/2, 0)
 		space.scale(fsize.x/tsize.x, -fsize.x/tsize.x, fsize.x/tsize.x)
 		space.translate(toSpace.pos.x, -toSpace.pos.y, toSpace.pos.z)
 	}

 	def popSubSpace() {
		self.screen.space.pop
 	}

 	protected def layoutSubs() {
 	}
}


// == Avatars ====================================================================


case class IsoWorldMove(direction:Vector3) extends AvatarSpaceState {}


case class IsoWorldZoom(amount:Double) extends AvatarSpaceState {}


class IsoWorld(name:AvatarName, screen:Screen)
	extends IsoAvatar(name, screen) {

	var space = new IsoWorldSpace(this)

	var renderer = new IsoWorldRender(this)

	protected[this] var prevMotionEvent:AvatarMotionEvent = null

	def consumeEvent(event:AvatarEvent):Boolean = {
		//println("%s ignore event %s".format(name, event))

		event match {
			case e:AvatarMotionEvent => {
				if(e.isEnd) {
					prevMotionEvent = null
				} else {
					if(prevMotionEvent ne null) {
						space.changeSpace(IsoWorldMove(prevMotionEvent.position --> e.position))
					}
					prevMotionEvent = e
				}
				true
			}
			case e:AvatarZoomEvent => {
				space.changeSpace(IsoWorldZoom(e.amount))
				true
			}
			case _ => {
				false
			}
		}
	}
}