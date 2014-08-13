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


object UILayoutSeparationMode extends Enumeration {
	val Percentage = Value
	val FirstPaneFixedSizeCm = Value
	val SecondPaneFixedSizeCm = Value
	type SeparationMode = Value
}


/** A base layout algorithm trait.
  *
  * If possible, layouts should not store any state so that they can be shared between
  * avatar instances. */
trait UILayout {
	/** Lay out the sub-avatars of the given `parent`. `scale1cm` represents 1 centimeter on screen.
	  * @return true if the layout of the sub-avatars changed the space of this avatar. In this case
	  *              the avatar may issue a layout request to its parent. */
	def layout(parent:Avatar, scale1cm:Double):Boolean

	/** If true, the `layout()` method has no side effects and the layout can
	  * be shared by multiple avatar instances. */
	def isShareable:Boolean = false
}


/** A simple layout that position only two sub-avatars. */
class UILayoutTwoPanes(val horizontal:Boolean, var separation:Double, val separationMode:UILayoutSeparationMode.Value) extends UILayout {
	import UILayoutSeparationMode._

	override def isShareable = true

	def layout(parent:Avatar, scale1cm:Double):Boolean = {
		if(parent.subCount >= 2) {
			val pspace = parent.space.subSpace
			val fromx  = pspace.fromx
			val fromy  = pspace.fromy
			val sizex  = pspace.sizex
			val sizey  = pspace.sizey
			val subs   = parent.iterator
			val sub0   = subs.next
			val sub1   = subs.next

			separationMode match {
				case Percentage => layoutPercentage(parent, scale1cm, fromx, fromy, sizex, sizey, sub0, sub1)
				case FirstPaneFixedSizeCm => layoutFirstPaneFixedSizeCm(parent, scale1cm, fromx, fromy, sizex, sizey, sub0, sub1)
				case SecondPaneFixedSizeCm => layoutSecondPaneFixedSizeCm(parent, scale1cm, fromx, fromy, sizex, sizey, sub0, sub1)
				case m => throw new RuntimeException("unknown layout mode %s".format(m))
			}
		}
		false
	}

	protected def layoutPercentage(parent:Avatar, scale1cm:Double, fromx:Double, fromy:Double, sizex:Double, sizey:Double, sub0:Avatar, sub1:Avatar) {
		if(horizontal) {
			sub0.space.thisSpace.setPosition(fromx, fromy, 0)
			sub0.space.thisSpace.setSize(sizex * separation, sizey, 1)
			sub1.space.thisSpace.setPosition(fromx + sizex * separation, fromy, 0)
			sub1.space.thisSpace.setSize(sizex - (sizex * separation), sizey, 1)
		} else {
			sub0.space.thisSpace.setPosition(fromx, fromy, 0)
			sub0.space.thisSpace.setSize(sizex, sizey * separation, 1)
			sub1.space.thisSpace.setPosition(fromx, fromy + sizey * separation, 0)
			sub1.space.thisSpace.setSize(sizex, sizey - (sizey * separation), 1)
		}

		sub0.space.asInstanceOf[UIAvatarSpace].layoutRequest
		sub1.space.asInstanceOf[UIAvatarSpace].layoutRequest
	}

	protected def layoutFirstPaneFixedSizeCm(parent:Avatar, scale1cm:Double, fromx:Double, fromy:Double, sizex:Double, sizey:Double, sub0:Avatar, sub1:Avatar) {
		val sep = separation * scale1cm

		if(horizontal) {
			sub0.space.thisSpace.setPosition(fromx, fromy, 0)
			sub0.space.thisSpace.setSize(sep, sizey, 1)
			sub1.space.thisSpace.setPosition(fromx + sep, fromy, 0)
			sub1.space.thisSpace.setSize(sizex - sep, sizey, 1)
		} else {
			sub0.space.thisSpace.setPosition(fromx, fromy, 0)
			sub0.space.thisSpace.setSize(sizex, sep, 1)
			sub1.space.thisSpace.setPosition(fromx, fromy + sep, 0)
			sub1.space.thisSpace.setSize(sizex, sizey - sep, 1)
		}

		sub0.space.asInstanceOf[UIAvatarSpace].layoutRequest
		sub1.space.asInstanceOf[UIAvatarSpace].layoutRequest
	}

	protected def layoutSecondPaneFixedSizeCm(parent:Avatar, scale1cm:Double, fromx:Double, fromy:Double, sizex:Double, sizey:Double, sub0:Avatar, sub1:Avatar) {
		val sep = separation * scale1cm

		if(horizontal) {
			sub0.space.thisSpace.setPosition(fromx, fromy, 0)
			sub0.space.thisSpace.setSize(sizex - sep, sizey, 1)
			sub1.space.thisSpace.setPosition(fromx + sizex - sep, fromy, 0)
			sub1.space.thisSpace.setSize(sep, sizey, 1)
		} else {
			sub0.space.thisSpace.setPosition(fromx, fromy, 0)
			sub0.space.thisSpace.setSize(sizex, sizey - sep, 1)
			sub1.space.thisSpace.setPosition(fromx, fromy + sizey - sep, 0)
			sub1.space.thisSpace.setSize(sizex, sep, 1)
		}

		sub0.space.asInstanceOf[UIAvatarSpace].layoutRequest
		sub1.space.asInstanceOf[UIAvatarSpace].layoutRequest
	}
}