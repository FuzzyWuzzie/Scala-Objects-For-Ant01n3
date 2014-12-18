package org.sofa.gfx.renderer.avatar.ui

import scala.math._

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default}
import org.sofa.gfx.{ShaderResource}
import org.sofa.gfx.renderer.{Screen}
import org.sofa.gfx.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState}
import org.sofa.gfx.surface.event._
import org.sofa.gfx.renderer.{NoSuchAvatarException}

import org.sofa.gfx.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.gfx.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}

import org.sofa.Timer


/** A base toolbar avatar. */
class UIToolbar(name:AvatarName, screen:Screen) extends UIAvatar(name, screen) {

	var space = new UIAvatarSpaceToolbar(this)

	var renderer = new UIAvatarRenderToolbar(this)

	def consumeEvent(event:Event):Boolean = false
}


// ----------------------------------------------------------------------------------------------


class UIAvatarRenderToolbar(avatar:Avatar) extends UIAvatarRender(avatar) with UIRenderUtils {

	color = Rgba.fromHSV(toRadians(260.0), 1.0, 1.0)

	override def render() {
// if(self.spaceChanged)
// 	println("# %s space changed".format(self.name))

		val space = self.space

		space.pushSubSpace
			fill
			self.renderSubs
		space.popSubSpace		
		horizShadowUnder(0.3)

		self.spaceChanged = false
		self.renderChanged = false
	}
}


// ----------------------------------------------------------------------------------------------


class UIAvatarSpaceToolbar(avatar:Avatar) extends UIAvatarSpace(avatar) {
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

	// override def changeSpace(newState:AvatarSpaceState) {
	// 	super.changeSpace(newState)
	// }

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