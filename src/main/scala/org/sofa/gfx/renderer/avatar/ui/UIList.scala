package org.sofa.gfx.renderer.avatar.ui

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default}
import org.sofa.gfx.{ShaderResource}
import org.sofa.gfx.renderer.{Screen}
import org.sofa.gfx.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState}
import org.sofa.gfx.surface.event._
import org.sofa.gfx.renderer.{NoSuchAvatarException}

import org.sofa.gfx.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.gfx.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}

import org.sofa.Timer


object UIList {
	/** Message sent to the list to scale the height in centimeters of list items. */
	case class ItemsSize(sizeCm:Double) extends AvatarSpaceState {}

	/** Message sent to the list to scroll. Often used internally. */
	case class Offset(amount:Double) extends AvatarSpaceState {}
}


class UIList(name:AvatarName, screen:Screen) extends UIAvatar(name, screen) {

	var space = new UIAvatarSpaceList(this)

	var renderer = new UIAvatarRenderList(this)

	def consumeEvent(event:Event):Boolean = {
		event match {
			case e:ScrollEvent => {
				if(containsEvent(event)) {
					space.changeSpace(UIList.Offset(e.delta.y))
					true
				} else {
					false
				}
			}
			case _ => { false }
		}
	}

	override def animate() {
		space.animateSpace
		renderer.animateRender
		animateVisibleSubs
	}
}

class UIListItem(name:AvatarName, screen:Screen)
	extends UIAvatar(name, screen) {

	var space = new UIAvatarSpaceListItem(this)

	var renderer = new UIAvatarRenderListItem(this)

	def consumeEvent(event:Event):Boolean = false /* {
		event match {
			case e:SingleTapEvent => {
				if(containsEvent(event)) {
					println("%s tap event %s".format(name, event))
					true
				} else {
					false
				}
			}
			case e:DoubleTapEvent => {
				if(containsEvent(event)) {
					println("%s double-tap event %s".format(name, event))
					true
				} else {
					false
				}
			}
			case e:LongPressEvent => {
				if(containsEvent(event)) {
					println("%s long-press event %s".format(name, event))
					true
				} else {
					false
				}
			}
			case _ => {
				false
			}
		}
	}*/
}


// ----------------------------------------------------------------------------------------------


class UIAvatarRenderList(avatar:Avatar) extends UIAvatarRender(avatar) with UIrenderUtils {
	override def render() {
		self.space.pushSubSpace
		self.renderVisibleSubs
		self.space.popSubSpace
	}
}

class UIAvatarRenderListItem(avatar:Avatar) extends UIAvatarRender(avatar) with UIrenderUtils {

	color = Rgba.White

	lineColor = Rgba.Black

	override def render() {
		//println(s"* render ${self.name}")
		val space = self.space
		// val text  = screen.textLayer
		// val sizex = space.subSpace.sizex
		// val sizey = space.subSpace.sizey

//		if(color eq null) {
// //			color = Rgba.randomHue(0.7, 1.0)
//			color = Rgba.randomHueAndSaturation(1.0)
//		}

		space.pushSubSpace
//			fill
			//text.font("Ubuntu-M.ttf", 15)
//			text.font("LTe50136.ttf", cmToPoints(1).toInt)
//			text.color(Rgba.Black)
//			text.string("Hello", sizex*0.1, sizey*0.9, 0, screen.space)
			self.renderSubs
		space.popSubSpace		
	}
}

// ----------------------------------------------------------------------------------------------


class UIAvatarSpaceList(avatar:Avatar) extends UIAvatarSpace(avatar) {
 	var scale1cm = 1.0

 	var itemSize = 1.5	// cm

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
			case UIList.ItemsSize(sizeCm) => {
				itemSize = sizeCm
			}
			case UIList.Offset(amount) => {
				offsety += amount*0.01
				checkOffset
			}
			case _ => super.changeSpace(newState)
		}
	}

	protected def checkOffset() {
		var offsetMax = -(toSpace.sizey - fromSpace.sizey)

		if(offsetMax > 0) offsetMax = 0
		if(offsety > 0) offsety = 0
		else if(offsety < offsetMax) offsety = offsetMax
	}

 	override def animateSpace() {
 		scale1cm = self.parent.space.scale1cm
 		toSpace.to.set(1, scale1cm * itemSize * avatar.subCount, 1)
 		checkOffset

 		//println(s"# layout list available space (${fromSpace.size(0)}, ${fromSpace.size(1)})")
 		//println(s"#        list pos(${fromSpace.pos.x}, ${fromSpace.pos.y}) scale1cm=${scale1cm} items=${avatar.subCount} size=(${toSpace.to.x}, ${toSpace.to.y})")

 		layoutSubs
 	}

 	override def pushSubSpace() {
 		val space = avatar.screen.space

 		space.push
 		space.translate(thisSpace.posx, thisSpace.posy + offsety, 0)
 	}

 	override def popSubSpace() {
		self.screen.space.pop
 	}

 	protected def layoutSubs() {
 		var i = 0
 		self.foreachSub { sub =>
 			sub.space.thisSpace.setSize(1, itemSize * scale1cm, 1)
 			sub.space.thisSpace.setPosition(0, itemSize * scale1cm * i, 0)
 			//println("    | layout %d %s".format(i, sub))
 			i += 1
 		}
 	}

	override def isVisible(sub:Avatar):Boolean = {
		val up   = fromSpace.posy // + offsety
		val down = up + fromSpace.sizey
		val y    = sub.space.thisSpace.posy + offsety
		val h    = sub.space.thisSpace.sizey

		((y+h) >= up && y <= down)
	}
}


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
 		scale1cm = self.parent.space.scale1cm

		toSpace.from.set(0, 0, 0)
		toSpace.setSize(fromSpace.sizex, fromSpace.sizey, 1)
 		super.animateSpace
	}

	override def pushSubSpace() {
 		val space = self.screen.space

 		space.push
 		space.translate(thisSpace.posx, thisSpace.posy, 0)
	}

	override def popSubSpace() {
		self.screen.space.pop
	}
}