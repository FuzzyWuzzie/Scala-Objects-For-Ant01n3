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


object UIPanel {
	/** Change the default panel renderer by a specialized one. */
	case class ChangeRender(creator:(Avatar)=>UIAvatarRenderPanel) extends AvatarState
}


/** A very simple avatar that does not render anything but supports
  * a customisable renderer. */
class UIPanel(name:AvatarName, screen:Screen) extends UIAvatar(name, screen) {

	var space = new UIAvatarSpacePanel(this)

	var renderer = new UIAvatarRenderPanel(this)

	override def change(state:AvatarState) {
		state match {
			case UIPanel.ChangeRender(creator) => { renderer = creator(self) }
			case s => super.change(s)
		}
	}

	def consumeEvent(event:Event):Boolean = false
}


// ----------------------------------------------------------------------------------------------


class UIAvatarRenderPanel(avatar:Avatar) extends UIAvatarRender(avatar) with UIrenderUtils {

	color = null

	override def render() {
// if(self.spaceChanged)
// 	println("# %s space changed".format(self.name))
		val space = self.space

		if(color eq null) {
			color = Rgba.randomHue(0.1, 1.0)
		}

		space.pushSubSpace
			fill
			self.renderSubs
		space.popSubSpace

		self.spaceChanged = false
		self.renderChanged = false
	}
}


// ----------------------------------------------------------------------------------------------


class UIAvatarSpacePanel(avatar:Avatar) extends UIAvatarSpace(avatar) {
//	var scale1cm = 1.0

	var fromSpace = new Box3From {
		from.set(0,0,0)
		size.set(1,1,1)
	}

	var toSpace = new Box3From {
		from.set(0,0,0)
		size.set(1,1,1)
	}

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def changeSpace(newState:AvatarSpaceState) {
		super.changeSpace(newState)
	}

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

		// dirtyLayout flag is reset in super.animateSpace.

		super.animateSpace
	}

	override def pushSubSpace() {
		val space = avatar.screen.space
		
		space.push
		space.translate(thisSpace.posx, thisSpace.posy, 0)
	}

	override def popSubSpace() {
		avatar.screen.space.pop
	}

	override def isVisible(sub:Avatar):Boolean = {
		true
	}
}