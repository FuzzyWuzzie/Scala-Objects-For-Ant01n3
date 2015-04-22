package org.sofa.gfx.renderer.avatar.ui

import scala.math.{min, max}

import org.sofa.math.{Point3, Point4, Vector3, Matrix4, Rgba, Box3, Box3From, Box3PosCentered, Box3Default}
import org.sofa.gfx.{ShaderResource, TextureFramebuffer}
import org.sofa.gfx.text.{GLFont}
import org.sofa.gfx.renderer.{Screen}
import org.sofa.gfx.renderer.{Avatar, AvatarLayer, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState}
import org.sofa.gfx.surface.event._
import org.sofa.gfx.renderer.{NoSuchAvatarException}

import org.sofa.gfx.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.gfx.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}
import org.sofa.gfx.dl.DisplayList


class UIAvatarFactory extends DefaultAvatarFactory {
	override def avatarFor(name:AvatarName, screen:Screen, kind:String):Avatar = {
		kind.toLowerCase match {
			case "ui.root"        => new UIRoot(name, screen)
			case "ui.root-events" => new UIRootEvents(name, screen)
			case "ui.list"        => new UIList(name, screen)
			case "ui.list-item"   => new UIListItem(name, screen)
			case "ui.perspective" => new UIPerspective(name, screen)
			case "ui.panel"       => new UIPanel(name, screen)
			case "ui.toolbar"     => new UIToolbar(name, screen)
			case _ => chainAvatarFor(name, screen, kind)
		}
	}
}


object UIAvatar {
	/** Width of a knob, by default 1.5 mm. TODO CSS. */
	final val KnobWidth = 0.1

	/** Color of a knob. TODO CSS. */
	final val KnobColor = Rgba.Grey40

	/** Set the layout of an avatar. */
	case class SetLayout(layout:UILayout) extends AvatarSpaceState {}
}


/** Base class for a hierarchy of avatar allowing to build user interfaces. */
abstract class UIAvatar(name:AvatarName, screen:Screen) extends DefaultAvatarComposed(name, screen) {

	/** Another consumeOrPropagateEvent that looks if an avatar is visible to propagate the
	  * event to it. To call only if the event is a spatial one. */
	def consumeOrPropagateEventVisible(event:SpatialEvent):Boolean = {
		if(! consumeEvent(event)) {
			self.findSub { sub =>
				if(space.isVisible(sub))
					 sub.events.consumeOrPropagateEvent(event)
				else false
			} match {
				case Some(a) => true
				case _       => false
			}
		} else {
			true
		}
	}

	/** Overriding of the consume-event operation to push/pop the avatar space.
	  * This allows to be able to test if a spatial event is inside an avatar or
	  * not. */
	override def consumeOrPropagateEvent(event:Event):Boolean = {
		var res:Boolean = false

		event match {
			case e:SpatialEvent => {
				space.pushSubSpace
				res = consumeOrPropagateEventVisible(e)
				space.popSubSpace
			}
			case _ => super.consumeOrPropagateEvent(event)
		}

		res
	}

	def containsEvent(event:Event):Boolean = {
		if(event.isInstanceOf[SpatialEvent]) {
			val space    = screen.space
			val sevent   = event.asInstanceOf[SpatialEvent] 
			val from     = space.project(Point4(0, 0, 0, 1))
			val to       = space.project(Point4(this.space.subSpace.sizex, this.space.subSpace.sizey, 0, 1))
			val origPos  = sevent.position()
			val w:Double = space.viewport(0)
			val h:Double = space.viewport(1)

			// // project yield elements in [-1:1] along X and Y, convert to pixels:

			val posx  = origPos.x
			val posy  = h - origPos.y
			val fromx = from.x / 2 * w + w / 2
			val fromy = from.y / 2 * h + h / 2
			val tox   = to.x   / 2 * w + w / 2
			val toy   = to.y   / 2 * h + h / 2

			// Test inclusion in pixels:

			(( (posx >= scala.math.min(fromx, tox)) && (posx <= scala.math.max(fromx, tox)) ) &&
			 ( (posy >= scala.math.min(fromy, toy)) && (posy <= scala.math.max(fromy, toy)) ))
		} else {
			false
		}
	}

	override def renderFilterRequest() {
		space match {
			case s:UIAvatarSpace => s.layoutRequest
			case _ => {}
		}
		super.renderFilterRequest
		self.screen.requestRender		
	}
}


// ----------------------------------------------------------------------


class UIAvatarRender(var self:Avatar) extends AvatarRender {
	def screen:Screen = self.screen
}


class UIAvatarRenderBase(avatar:Avatar) extends UIAvatarRender(avatar) {

	def this() { this(null) }

	def setAvatar(avatar:UIPerspective) { this.self = avatar }

	override def render() { super.render }

	def consumeEvent(event:Event):Boolean = {
		false
	}
}


// ----------------------------------------------------------------------------------------------


abstract class UIAvatarSpace(var self:Avatar) extends AvatarSpace {

 	var scale1cm = 1.0

	/** An independant layout algorithm, eventually shared between instances, if
	  * the space does not already acts as a layout. */
	protected[this] var layout:UILayout = null
	
	/** A flag indicating the layout must be recomputed.
	  * This flag is set to true when (this list is exaustive
	  * and should be kept up to date):
	  *  - the avatar is created.
	  *  - this avatar space changes (Avatar.spaceChanged).
	  *  - a sub-avatar is added or removed.
	  *  - the layoutRequest is issued by the sub-avatars. */
	protected[this] var dirtyLayout = true

	/** Tell this avatar that one of its sub-avatar changed its size or disposition and this
	  * avatar must relayout. If this avatar changes its size or disposition in return, it
	  * can propagate the layout request to its parent avatar. */
	def layoutRequest() { dirtyLayout = true }

	/** True if the layout needs to be recomputed at next animation step. */
	def needRelayout:Boolean = dirtyLayout

	override def subCountChanged(delta:Int) {
		layoutRequest
		self.screen.requestRender
	}

	override def changeSpace(newState:AvatarSpaceState) {
		newState match {
			case UIAvatar.SetLayout(newLayout) => layout = newLayout
			case state => super.changeSpace(state)
		}
	}

	override def animateSpace() {
		if(dirtyLayout) {
			self.screen.requestRender
			dirtyLayout = false
			self.spaceChanged = true
			
			if(layout ne null)
				layout.layout(self, scale1cm)
		}

		//super.animateSpace	// super is a trait with an abstract animateSpace() method.
	}
}