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


/** A simple 2D layout that position only two sub-avatars. 
  *
  * @param horizontal if true, the layout follows the X axis, else the Y axis.
  * @param separation the value used to position and size the two sub avatars.
  * @param separationMode how to interpret the `separation` value.
  * @param useRenderFilter if true, the avatars are taken from the eventually filtered list of subs, else the full list of subs. */
class UILayoutTwoPanes(val horizontal:Boolean, var separation:Double, val separationMode:UILayoutSeparationMode.Value, useRenderFilter:Boolean=true) extends UILayout {
	import UILayoutSeparationMode._

	override def isShareable = true

	def layout(parent:Avatar, scale1cm:Double):Boolean = {
		if(parent.subCount >= 2) {
			val pspace = parent.space.subSpace
			val fromx  = pspace.fromx
			val fromy  = pspace.fromy
			val sizex  = pspace.sizex
			val sizey  = pspace.sizey
			val subs   = if(useRenderFilter) parent.filteredIterator else parent.iterator
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
		val s0 = sub0.space.thisSpace
		val s1 = sub1.space.thisSpace
		
		if(horizontal) {
			s0.setPosition(fromx, fromy, s0.posz)
			s0.setSize(sizex * separation, sizey, 1)
			s1.setPosition(fromx + sizex * separation, fromy, s1.posz)
			s1.setSize(sizex - (sizex * separation), sizey, 1)
		} else {
			s0.setPosition(fromx, fromy, s0.posz)
			s0.setSize(sizex, sizey * separation, 1)
			s1.setPosition(fromx, fromy + sizey * separation, s1.posz)
			s1.setSize(sizex, sizey - (sizey * separation), 1)
		}

		sub0.space.asInstanceOf[UIAvatarSpace].layoutRequest
		sub1.space.asInstanceOf[UIAvatarSpace].layoutRequest
	}

	protected def layoutFirstPaneFixedSizeCm(parent:Avatar, scale1cm:Double, fromx:Double, fromy:Double, sizex:Double, sizey:Double, sub0:Avatar, sub1:Avatar) {
		val sep = separation * scale1cm
		val s0  = sub0.space.thisSpace
		val s1  = sub1.space.thisSpace

		if(horizontal) {
			s0.setPosition(fromx, fromy, s0.posz)
			s0.setSize(sep, sizey, 1)
			s1.setPosition(fromx + sep, fromy, s1.posz)
			s1.setSize(sizex - sep, sizey, 1)
		} else {
			s0.setPosition(fromx, fromy, s0.posz)
			s0.setSize(sizex, sep, 1)
			s1.setPosition(fromx, fromy + sep, s1.posz)
			s1.setSize(sizex, sizey - sep, 1)
		}

		sub0.space.asInstanceOf[UIAvatarSpace].layoutRequest
		sub1.space.asInstanceOf[UIAvatarSpace].layoutRequest
	}

	protected def layoutSecondPaneFixedSizeCm(parent:Avatar, scale1cm:Double, fromx:Double, fromy:Double, sizex:Double, sizey:Double, sub0:Avatar, sub1:Avatar) {
		val sep = separation * scale1cm
		val s0  = sub0.space.thisSpace
		val s1  = sub1.space.thisSpace

		if(horizontal) {
			s0.setPosition(fromx, fromy, s0.posz)
			s0.setSize(sizex - sep, sizey, 1)
			s1.setPosition(fromx + sizex - sep, fromy, s1.posz)
			s1.setSize(sep, sizey, 1)
		} else {
			s0.setPosition(fromx, fromy, s0.posz)
			s0.setSize(sizex, sizey - sep, 1)
			s1.setPosition(fromx, fromy + sizey - sep, s1.posz)
			s1.setSize(sizex, sep, 1)
		}

		sub0.space.asInstanceOf[UIAvatarSpace].layoutRequest
		sub1.space.asInstanceOf[UIAvatarSpace].layoutRequest
	}
}