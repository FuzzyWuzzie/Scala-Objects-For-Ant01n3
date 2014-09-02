package org.sofa.gfx.renderer.avatar.ui

import scala.math._

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default}
import org.sofa.gfx.{ShaderResource}
import org.sofa.gfx.renderer.{Screen}
import org.sofa.gfx.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName,
                              AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer,
                              AvatarFactory, DefaultAvatarFactory, AvatarSpaceState,
                              AvatarRenderState, AvatarState}
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

	/** Message sent to the list to scroll at a given percent (0 to 1) in the list. Often used 
	  *internally. */
	case class OffsetAt(percent:Double) extends AvatarSpaceState {}

	/** Message sent to the list to scroll of one or more complete visible page (negative numbers
	  * are possible). */
	case class OffsetPages(pages:Int) extends AvatarSpaceState {}
}


class UIList(name:AvatarName, screen:Screen) extends UIAvatar(name, screen) {

	var space = new UIAvatarSpaceList(this)

	var renderer = new UIAvatarRenderList(this)

	def consumeEvent(event:Event):Boolean = {
		import ActionKey._

		event match {
			case e:ScrollEvent => {
				if(containsEvent(event)) {
					space.changeSpace(UIList.Offset(e.delta.y))
					true
				} else {
					false
				}
			}
			case e:ActionKeyEvent => {
				e.key match {
					case PageUp   => { space.changeSpace(UIList.OffsetPages(1)); true }
					case PageDown => { space.changeSpace(UIList.OffsetPages(-1)); true }
					case Begin    => { space.changeSpace(UIList.OffsetAt(0.0)); true }
					case End      => { space.changeSpace(UIList.OffsetAt(1.0)); true }
					case Up       => { space.changeSpace(UIList.Offset(1)); true }
					case Down     => { space.changeSpace(UIList.Offset(-1)); true }
					case _        => { false }
				}
			}
			case _ => { false }
		}
	}

	override def animate() {
		renderFilterSubs
		space.animateSpace
		renderer.animateRender
		animateVisibleSubs
		//animateSubs
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
 		size.set(1,1,1)
 	}

 	var toSpace = new Box3From {
 		from.set(0,0,0)
 		size.set(1,1,1)
 	}

	var offsety = 0.0

 	def thisSpace = fromSpace

 	def subSpace = toSpace

	override def changeSpace(newState:AvatarSpaceState) {
		newState match {
			case UIList.ItemsSize(sizeCm) => {
				itemSize = sizeCm
				dirtyLayout = true
				self.screen.requestRender
			}
			case UIList.Offset(amount) => {
				offsety += amount * scale1cm
				self.screen.requestRender
				checkOffset
			}
			case UIList.OffsetAt(percent) => {
				var p = max(0, min(1, percent))
				self.screen.requestRender
				var offsetMax = min(-(toSpace.sizey - fromSpace.sizey), 0)
				offsety = offsetMax * p
				checkOffset
			}
			case UIList.OffsetPages(pages) => {
				self.screen.requestRender
				offsety += fromSpace.sizey * pages
				checkOffset
			}
			case _ => super.changeSpace(newState)
		}
	}

	protected def checkOffset() {
		var offsetMax = min(-(toSpace.sizey - fromSpace.sizey), 0)

		if(offsety > 0) offsety = 0
		else if(offsety < offsetMax) offsety = offsetMax
	}

 	override def animateSpace() {
  		if(dirtyLayout) {
  			val oldsizey = toSpace.to.y
 			scale1cm = self.parent.space.scale1cm
 			toSpace.setSize(1, scale1cm * itemSize * avatar.filteredSubCount, 1)
 			
 			// If the sizey changed, this means the dpc or scale1cm changed,
 			// we must ask the parent to relayout anew since we changed size.
 			
			self.screen.requestRender

 			if(oldsizey == toSpace.to.y)
 		 	     dirtyLayout = false	// Before subs, as they can ask a relayout.
 		 	else self.parent.space.asInstanceOf[UIAvatarSpace].layoutRequest

 			layoutSubs
 			checkOffset
		}
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
 		self.foreachFilteredSub { sub =>
 			sub.space.thisSpace.setPosition(0, itemSize * scale1cm * i, 0)
 			sub.space.thisSpace.setSize(1, itemSize * scale1cm, 1)
 			sub.space.asInstanceOf[UIAvatarSpace].layoutRequest
			//println("    | layout %d %s size(%f, %f)".format(i, sub, sub.space.thisSpace.sizex, sub.space.thisSpace.sizey))
 			i += 1
 		}
 	}

	override def isVisible(sub:Avatar):Boolean = {
		if(!dirtyLayout) {
			val up   = fromSpace.posy // + offsety
			val down = up + fromSpace.sizey
			val y    = sub.space.thisSpace.posy + offsety
			val h    = sub.space.thisSpace.sizey

			((y+h) >= up && y <= down)
		} else {
			true
		}
	}

	override def subCountChanged(delta:Int) {
		dirtyLayout = true
		self.screen.requestRender

		// The size of the list changed. Ask the parent to relayout.

		if(self.parent ne null)
			self.parent.space.asInstanceOf[UIAvatarSpace].layoutRequest
	}
}


class UIAvatarSpaceListItem(avatar:Avatar) extends UIAvatarSpace(avatar) {

	var scale1cm = 1.0

	var fromSpace = new Box3From {
		from.set(0, 0, 0)
		size.set(1, 1, 1)
	}

	var toSpace = new Box3From {
		from.set(0, 0, 0)
		size.set(1, 1, 1)
	}

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def animateSpace() {
 		scale1cm = self.parent.space.scale1cm

 		if(dirtyLayout) {
			toSpace.from.set(0, 0, 0)
			toSpace.setSize(fromSpace.sizex, fromSpace.sizey, 1)
			self.screen.requestRender
		}
		
		// DirtyLayout flag is reset in super.animateSpace
 		
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