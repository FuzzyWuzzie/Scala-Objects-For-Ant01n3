package org.sofa.opengl.actor.renderer.avatar.ui

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default}
import org.sofa.opengl.{ShaderResource}
import org.sofa.opengl.actor.renderer.{Screen}
import org.sofa.opengl.actor.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState}
import org.sofa.opengl.actor.renderer.{AvatarEvent, AvatarSpatialEvent, AvatarMotionEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarKeyEvent}
import org.sofa.opengl.actor.renderer.{NoSuchAvatarException}

import org.sofa.opengl.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.opengl.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


class UIAvatarRenderList(avatar:Avatar) extends UIAvatarRender(avatar) with UIrenderUtils {
	override def render() {
		//println(s"* render ${self.name}")
		self.space.pushSubSpace
		fill
		self.renderSubs
		self.space.popSubSpace		
	}
}

class UIAvatarRenderListItem(avatar:Avatar) extends UIAvatarRender(avatar) with UIrenderUtils {

	color = Rgba.White

	lineColor = Rgba.Black

	override def render() {
		//println(s"* render ${self.name}")
		val space = self.space
		val text  = screen.textLayer

		space.pushSubSpace
		fill
		text.font("Ubuntu-L.ttf", 13)
		text.color(Rgba.Black)
		text.string("Hello", 0.2, 0.1, 0, screen.space)
		self.renderSubs
		space.popSubSpace		
	}
}

// ----------------------------------------------------------------------------------------------


class UIAvatarSpaceList(avatar:Avatar) extends UIAvatarSpace(avatar) {
 	var scale1cm = 1.0

 	var itemHeight = 1.5	// cm

 	var fromSpace = new Box3From {
 		from.set(0,0,0)
 		to.set(1,1,1)
 	}

 	var toSpace = new Box3From {
 		from.set(0,0,0)
 		to.set(1,1,1)
 	}

	var offsety = 0.0

 	def thisSpace = fromSpace

 	def subSpace = toSpace

	override def changeSpace(newState:AvatarSpaceState) {
		newState match {
			case AvatarOffsetState(amount) => {
				offsety += amount*0.01

				var offsetMax = -(toSpace.size.height - fromSpace.size.height)
				
				if(offsetMax > 0) offsetMax = 0

				if(offsety > 0) offsety = 0
				else if(offsety < offsetMax) offsety = offsetMax
			}
			case _ => super.changeSpace(newState)
		}
	}
 	override def animateSpace() {
 		scale1cm = self.parent.space.scale1cm

 		toSpace.to.set(1, scale1cm * itemHeight * avatar.subCount, 1)

 		//println(s"# layout list available space (${fromSpace.size(0)}, ${fromSpace.size(1)})")
 		//println(s"#        list pos(${fromSpace.pos.x}, ${fromSpace.pos.y}) scale1cm=${scale1cm} items=${avatar.subCount} size=(${toSpace.to.x}, ${toSpace.to.y})")

 		layoutSubs
 	}

 	override def pushSubSpace() {
 		val space = avatar.screen.space

 		space.push
 		space.translate(thisSpace.pos.x, thisSpace.pos.y + offsety, 0)
 	}

 	override def popSubSpace() {
		self.screen.space.pop
 	}

 	protected def layoutSubs() {
 		var i = 0
 		self.foreachSub { sub =>
 			sub.space.thisSpace.setSize(1, itemHeight*scale1cm, 1)
 			sub.space.thisSpace.setPosition(0, itemHeight*scale1cm*i, 0)
 			//println("    | layout %d %s".format(i, sub))
 			i += 1
 		}
 	}
}


case class AvatarOffsetState(amount:Double) extends AvatarSpaceState {}


class UIAvatarSpaceListItem(avatar:Avatar) extends UIAvatarSpace(avatar) {
	var scale1cm = 1.0

	var fromSpace = new Box3From {
		from.set(0, 0, 0)
		to.set(1, 1, 1)
	}

	var toSpace = new Box3From {
		from.set(0, 0, 0)
		to.set(1, 1, 1)
	}

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def animateSpace() {
		toSpace.from.set(0, 0, 0)
		toSpace.setSize(fromSpace.size(0), fromSpace.size(1), 1)

 		scale1cm = self.parent.space.scale1cm

 		//println(s"# layout ListItem pos=${fromSpace.pos} size=${fromSpace.from} sub -> pos=${toSpace.pos} size=${toSpace.size}")
	}

	override def pushSubSpace() {
 		val space = self.screen.space

 		space.push
 		space.translate(thisSpace.pos.x, thisSpace.pos.y, 0)
	}

	override def popSubSpace() {
		self.screen.space.pop
	}
}


// ----------------------------------------------------------------------------------------------


class UIList(name:AvatarName, screen:Screen) extends UIAvatar(name, screen) {

	var space = new UIAvatarSpaceList(this)

	var renderer = new UIAvatarRenderList(this)

	var prevMotionEvent:AvatarMotionEvent = null

	def consumeEvent(event:AvatarEvent):Boolean = {
		event match {
			case e:AvatarMotionEvent => {
				if(e.isEnd) {
					println("%s consume event END".format(name))
					prevMotionEvent = null
				} else {
					if(prevMotionEvent ne null) {
						println("%s consume event move".format(name))
						space.changeSpace(AvatarOffsetState(e.position.y - prevMotionEvent.position.y))
					} else println("%s consume event no move".format(name))
					prevMotionEvent = e
				}
				//println("%s consume event %s".format(name, event))

				true
			}
			case _ => {
				println("%s ignore event %s".format(name, event))
				false
			}
		}
	}
}

class UIListItem(name:AvatarName, screen:Screen)
	extends UIAvatar(name, screen) {

	var space = new UIAvatarSpaceListItem(this)

	var renderer = new UIAvatarRenderListItem(this)

	def consumeEvent(event:AvatarEvent):Boolean = {
		event match {
			case e:AvatarClickEvent => {
				println("%s received event %s".format(name, event))
				false
			}
			case e:AvatarLongClickEvent => {
				println("%s received event %s".format(name, event))
				false
			}
			case _ => {
				println("%s received event %s".format(name, event))
				false
			}
		}
	}
}