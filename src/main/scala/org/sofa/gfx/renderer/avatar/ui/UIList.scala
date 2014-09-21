package org.sofa.gfx.renderer.avatar.ui

import scala.math._

import org.sofa.math.{Point3, Vector3, Matrix4, Rgba, Box3, Box3From, Box3PosCentered, Box3Default}
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

	case class StartFling(velocity:Vector3) extends AvatarSpaceState {}

	final val FlingStart = 1.0

	final val FlingDecreaseRate = 0.95

	final val FlingOffsetBase = 0.05

	final val FlingStopUnder = 0.05
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
			case e:FlingEvent => {
				if(containsEvent(event)) {
					space.changeSpace(UIList.StartFling(e.velocity))
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

	hasLayer = true

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
// if(self.spaceChanged)
// 	println("# %s space changed".format(self.name))

		val s = self.space.thisSpace
		val scissors = self.screen.scissors.push(screen.gl, s.posx, s.posy, s.posx+s.sizex, s.posy+s.sizey, screen.space)
			//self.screen.textLayer.pushScissors(scissors)
			self.screen.textLayerDL.pushScissors(scissors)
				self.space.pushSubSpace
					self.renderVisibleSubs
				self.space.popSubSpace
			//self.screen.textLayer.popScissors()
			self.screen.textLayerDL.popScissors()
		self.screen.scissors.pop(screen.gl)
		renderScrollIndicator

		self.spaceChanged = false
		self.renderChanged = false
	}

	protected[this] var savedMVP = Matrix4()

	protected def renderScrollIndicator() {
		import UIAvatar._

		val space = self.space.asInstanceOf[UIAvatarSpaceList]
		val gl    = screen.gl

		if(self.spaceChanged) {
			if(space.knobVisible > 0) {
				val thisH = space.thisSpace.sizey
				val subH  = space.subSpace.sizey
				val offY  = space.offsety
				val ratio = thisH / subH
				val s1cm  = self.parent.space.scale1cm
				val sp    = screen.space

				val color = if(space.knobVisible > 0.5) KnobColor
			            else Rgba(KnobColor.red, KnobColor.green, KnobColor.blue, space.knobVisible * 2)

				sp.pushpop {
					// We are in the parent space.
					sp.translate(space.thisSpace.posx + space.thisSpace.sizex - (s1cm*KnobWidth),
				             space.thisSpace.posy - offY * ratio, 0)
					sp.scale(s1cm * KnobWidth, thisH * ratio, 1)
					savedMVP.copy(sp.top)
					shaderUniform.use
					shaderUniform.uniform("uniformColor", color)
					sp.uniformMVP(shaderUniform)
					plainRect.draw(gl)
				}

			}
		} else {
			val color = if(space.knobVisible > 0.5) KnobColor
			           else Rgba(KnobColor.red, KnobColor.green, KnobColor.blue, space.knobVisible * 2)
			
			shaderUniform.use
			shaderUniform.uniform("uniformColor", color)
			shaderUniform.uniformMatrix("MVP", savedMVP)
			plainRect.draw(gl)
		}
	}
}


class UIAvatarRenderListItem(avatar:Avatar) extends UIAvatarRender(avatar) with UIrenderUtils {

	color = null//Rgba.White

	lineColor = Rgba.Black

	override def render() {
		val space = self.space
		val gl = screen.gl
		if(color eq null) {
			color = Rgba.randomHue(0.7, 1.0)
		}

		if(self.hasLayer) {
			if(self.renderChanged) {
				pushLayer
					self.renderSubs
				popLayer
			}

			space.pushSubSpace
				renderLayer
			space.popSubSpace
		} else {
			space.pushSubSpace
				self.renderSubs
			space.popSubSpace
		}

		self.spaceChanged = false
		self.renderChanged = false
	}
}

// ----------------------------------------------------------------------------------------------


class UIAvatarSpaceList(avatar:Avatar) extends UIAvatarSpace(avatar) {
// 	var scale1cm = 1.0

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

	var knobVisible = 1.0

	var fling = 0.0

	var velocity = 0.0

 	def thisSpace = fromSpace

 	def subSpace = toSpace

	override def changeSpace(newState:AvatarSpaceState) {
		newState match {
			case UIList.ItemsSize(sizeCm) => {
				itemSize = sizeCm
				dirtyLayout = true
				self.screen.requestRender
				knobVisible = 1.0
				checkOffset
			}
			case UIList.Offset(amount) => {
				offsety += amount * scale1cm
				self.screen.requestRender
				knobVisible = 1.0
				checkOffset
			}
			case UIList.OffsetAt(percent) => {
				var p = max(0, min(1, percent))
				self.screen.requestRender
				var offsetMax = min(-(toSpace.sizey - fromSpace.sizey), 0)
				offsety = offsetMax * p
				knobVisible = 1.0
				checkOffset
			}
			case UIList.OffsetPages(pages) => {
				self.screen.requestRender
				offsety += fromSpace.sizey * pages
				knobVisible = 1.0
				checkOffset
			}
			case UIList.StartFling(velocity) => {
				this.velocity = velocity.y
				this.fling = UIList.FlingStart
			}
			case _ => super.changeSpace(newState)
		}
	}

	protected def checkOffset() {
		var offsetMax = min(-(toSpace.sizey - fromSpace.sizey), 0)

		if(offsety > 0) { offsety = 0; fling = 0 }
		else if(offsety < offsetMax) { offsety = offsetMax; fling = 0 }
		
		self.spaceChanged = true
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
		} else {
			if(self.parent.spaceChanged)
				self.spaceChanged = true
		}

		if(knobVisible > 0.05) {
			knobVisible -= 0.05
			self.screen.requestRender
		} else if(knobVisible > 0 && knobVisible <= 0.05) {
			knobVisible  = 0.0
			self.screen.requestRender
		}

		if(fling > 0) {
			fling *= UIList.FlingDecreaseRate
			
			if(fling < UIList.FlingStopUnder) {
				fling = 0.0
			} else {
				val off = UIList.FlingOffsetBase * velocity * scale1cm * fling
				offsety += off
				self.screen.requestRender
				knobVisible = 1.0
				checkOffset
			}
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
 			val subspace = sub.space.thisSpace
 			subspace.setPosition(0, itemSize * scale1cm * i, subspace.posz)
 			subspace.setSize(1, itemSize * scale1cm, 1)
 			sub.space.asInstanceOf[UIAvatarSpace].layoutRequest
			//println("    | layout %d %s size(%f, %f)".format(i, sub, sub.space.thisSpace.sizex, sub.space.thisSpace.sizey))
 			i += 1
 		}
 	}

	override def isVisible(sub:Avatar):Boolean = {
		if(!dirtyLayout) {
			val up   = toSpace.pos.y //fromSpace.posy // + offsety
			val down = up + fromSpace.sizey //fromSpace.sizey
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

//	var scale1cm = 1.0

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
			self.spaceChanged = true
		} else {
			if(self.parent.spaceChanged)
				self.spaceChanged = true
		}
		
		// DirtyLayout flag is reset in super.animateSpace
 		
 		super.animateSpace
	}

	override def pushSubSpace() {
 		val space = self.screen.space

 		space.push
 		space.translate(thisSpace.posx, thisSpace.posy, 0)
//println("%s.pushSubSpace(translate(%f, %f))".format(self.name, thisSpace.posx, thisSpace.posy))
	}

	override def popSubSpace() {
		self.screen.space.pop
	}

	override def subSpaceLayer() {
		val ratiohw = toSpace.sizey / toSpace.sizex
		
		self.screen.space.translate(-1, 1, 0)
		self.screen.space.scale(2, -2 / ratiohw, 1)
//println("%s.subSpaceLayer(%f, %f)".format(self.name, 1.0, ratiohw))
	}
}