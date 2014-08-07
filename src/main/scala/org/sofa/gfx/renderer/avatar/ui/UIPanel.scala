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


/** A very simple avatar that does not render anything but support 
  * a customisable renderer and a space that is able to receive a 
  * layout algorithm. */
class UIPanel(name:AvatarName, screen:Screen) extends UIAvatar(name, screen) {

	var space = new UIAvatarSpacePanel(this)

	var renderer = new UIAvatarRenderPanel(this)

	def consumeEvent(event:Event):Boolean = false
}


// ----------------------------------------------------------------------------------------------


class UIAvatarRenderPanel(avatar:Avatar) extends UIAvatarRender(avatar) with UIrenderUtils {

	color = null

	override def render() {
		val space = self.space
		// val text  = screen.textLayer
		// val sizex = space.subSpace.sizex
		// val sizey = space.subSpace.sizey

		if(color eq null) {
			color = Rgba.randomHueAndSaturation(1.0)
		}

		space.pushSubSpace
			fill
//			text.font("LTe50136.ttf", cmToPoints(1).toInt)
//			text.color(Rgba.Black)
//			text.string("Hello", sizex*0.1, sizey*0.9, 0, screen.space)
			self.renderSubs
		space.popSubSpace		
	}
}


// ----------------------------------------------------------------------------------------------


class UIAvatarSpacePanel(avatar:Avatar) extends UIAvatarSpace(avatar) {
	var scale1cm = 1.0

	var fromSpace = new Box3From {
		from.set(0,0,0)
		to.set(1,1,1)
	}

	var toSpace = new Box3From {
		from.set(0,0,0)
		to.set(1,1,1)
	}

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def changeSpace(newState:AvatarSpaceState) {
		super.changeSpace(newState)
	}

	override def animateSpace() {
		scale1cm = self.parent.space.scale1cm
		toSpace.from.set(0, 0, 0)
		toSpace.setSize(fromSpace.sizex, fromSpace.sizey, 1)
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