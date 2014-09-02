package org.sofa.gfx.renderer.avatar.ui

import scala.math.{min, max}

import org.sofa.math.{Point3, Point4, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default}
import org.sofa.gfx.{ShaderResource}
import org.sofa.gfx.renderer.{Screen}
import org.sofa.gfx.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState}
import org.sofa.gfx.surface.event._
import org.sofa.gfx.renderer.{NoSuchAvatarException}

import org.sofa.gfx.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.gfx.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


class UIAvatarFactory extends DefaultAvatarFactory {
	override def avatarFor(name:AvatarName, screen:Screen, kind:String):Avatar = {
		kind.toLowerCase match {
			case "ui.root" => new UIRoot(name, screen)
			case "ui.root-events" => new UIRootEvents(name, screen)
			case "ui.list" => new UIList(name, screen)
			case "ui.list-item" => new UIListItem(name, screen)
			case "ui.perspective" => new UIPerspective(name, screen)
			case "ui.panel" => new UIPanel(name, screen)
			case _ => chainAvatarFor(name, screen, kind)
		}
	}
}


object UIAvatar {
	/** Set the layout of an avatar. */
	case class SetLayout(layout:UILayout) extends AvatarSpaceState {}
}


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
			val w:Double = space.viewportPx(0)
			val h:Double = space.viewportPx(1)

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


// ----------------------------------------------------------------------------------------------


object UIrenderUtils {
	/** A quad mesh to fill a rectangular area. */
	var plainRect:TrianglesMesh = null

	/** A square line plainRect to stroke a rectangular area. */
	var strokeRect:LinesMesh = null

	/** A quad mesh with the two upper vertices black and the two lower vertices transparent. */
	var shadowUnderRect:TrianglesMesh = null

	/** A shader that takes an uniform color named `uniformColor` an apply it to each vertex. */
	var shaderUniform:ShaderProgram = null

	/** A shader that waits color information on each vertex, and allows a `transparency`
	  * uniform to make it more or less transparent. */
	var shaderColor:ShaderProgram = null
}


trait UIrenderUtils {
	var color = Rgba.Cyan

	var lineColor = Rgba.Red

	def self:Avatar

	def cmToPoints(cmValue:Double):Double = cmValue * 28.34

	/** Stroke the space of the avatar with an uniform color. */
	def fillAndStroke() {
		import VertexAttribute._
		import UIrenderUtils._

		val screen = self.screen
		val space  = screen.space
		val gl     = screen.gl

		if(shaderUniform eq null) {
			shaderUniform = screen.libraries.shaders.addAndGet(gl, "uniform-color-shader", ShaderResource("uniform-color-shader", "uniform_color.vert.glsl", "uniform_color.frag.glsl"))
		}

		if(plainRect eq null) {
			plainRect = new TrianglesMesh(2)
			plainRect.setPoint(0, 0, 0, 0)
			plainRect.setPoint(1, 1, 0, 0)
			plainRect.setPoint(2, 1, 1, 0)
			plainRect.setPoint(3, 0, 1, 0)
			plainRect.setTriangle(0, 0, 1, 2)
			plainRect.setTriangle(1, 0, 2, 3)
			plainRect.newVertexArray(gl, shaderUniform, Vertex -> "position")
		}

		if(strokeRect eq null) {
			strokeRect = new LinesMesh(4)
			strokeRect.setLine(0, 0,0,0, 1,0,0)
			strokeRect.setLine(1, 1,0,0, 1,1,0)
			strokeRect.setLine(2, 1,1,0, 0,1,0)
			strokeRect.setLine(3, 0,1,0, 0,0,0)
			strokeRect.newVertexArray(gl, shaderUniform, Vertex -> "position")
		}

		val subSpace = self.space.subSpace

		space.pushpop {
			shaderUniform.use
			//println(s"    | rendering ${self.name} scale(${subSpace.size(0)},${subSpace.size(1)})")
			shaderUniform.uniform("uniformColor", color)
			space.scale(subSpace.sizex, subSpace.sizey, 1)
			space.uniformMVP(shaderUniform)
			plainRect.lastva.draw(plainRect.drawAs(gl))
			shaderUniform.uniform("uniformColor", lineColor)
			strokeRect.lastva.draw(strokeRect.drawAs(gl))
		}
	}

	/** Fill the space of the avatar with an uniform color. */
	def fill() {
		import VertexAttribute._
		import UIrenderUtils._

		val screen = self.screen
		val space  = screen.space
		val gl     = screen.gl

		if(shaderUniform eq null) {
			shaderUniform = screen.libraries.shaders.addAndGet(gl, "uniform-color-shader", ShaderResource("uniform-color-shader", "uniform_color.vert.glsl", "uniform_color.frag.glsl"))
		}

		if(plainRect eq null) {
			plainRect = new TrianglesMesh(2)
			plainRect.setPoint(0, 0, 0, 0)
			plainRect.setPoint(1, 1, 0, 0)
			plainRect.setPoint(2, 1, 1, 0)
			plainRect.setPoint(3, 0, 1, 0)
			plainRect.setTriangle(0, 0, 1, 2)
			plainRect.setTriangle(1, 0, 2, 3)
			plainRect.newVertexArray(gl, shaderUniform, Vertex -> "position")
		}

		val subSpace = self.space.subSpace

		shaderUniform.use
		space.pushpop {
			//println(s"    | rendering ${self.name} scale(${subSpace.size(0)},${subSpace.size(1)})")
			shaderUniform.uniform("uniformColor", color)
			space.scale(subSpace.sizex, subSpace.sizey, 1)
			space.uniformMVP(shaderUniform)
			plainRect.lastva.draw(plainRect.drawAs(gl))
		}
	}

	def shadowAbove(alpha:Double) {
		import VertexAttribute._
		import UIrenderUtils._

		val screen = self.screen
		val space  = screen.space
		val gl     = screen.gl

		if(shaderColor eq null) {
			shaderColor = screen.libraries.shaders.addAndGet(gl, "color-shader", ShaderResource("color-shader", "plain_shader.vert.glsl", "plain_shader.frag.glsl"))
		}

		if(shadowUnderRect eq null) {
			shadowUnderRect = new TrianglesMesh(2)
			shadowUnderRect v(0) xyz(0, 0, 0) rgba(0, 0, 0, 0.05f)
			shadowUnderRect v(1) xyz(1, 0, 0) rgba(0, 0, 0, 0.05f)
			shadowUnderRect v(2) xyz(1, 1, 0) rgba(0, 0, 0, 0)
			shadowUnderRect v(3) xyz(0, 1, 0) rgba(0, 0, 0, 0)
			shadowUnderRect t(0, 0, 1, 2)
			shadowUnderRect t(1, 0, 2, 3)
			shadowUnderRect.newVertexArray(gl, shaderColor, Vertex -> "position", Color -> "color")
		}

		val subSpace = self.space.subSpace
		val s1cm     = self.space.scale1cm

		shaderColor.use
		space.pushpop {
			space.scale(subSpace.sizex, s1cm*0.3, 1)
			space.uniformMVP(shaderColor)
			shadowUnderRect.lastva.draw(shadowUnderRect.drawAs(gl))
		}
	}
}


class UIAvatarRender(var self:Avatar) extends AvatarRender {
	def screen:Screen = self.screen
}


// ----------------------------------------------------------------------------------------------


abstract class UIAvatarSpace(var self:Avatar) extends AvatarSpace {

	/** An independant layout algorithm, eventually shared between instances, if
	  * the space does not already acts as a layout. */
	protected[this] var layout:UILayout = null
	
	/** A flag indicating the layout must be recomputed.
	  * This flag is set to true when (this list is exaustive
	  * and should be kept up to date):
	  *  - the avatar is created.
	  *  - this avatar space changes.
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
			
			if(layout ne null)
				layout.layout(self, scale1cm)
		}

		//super.animateSpace	// super is a trait with an abstract animateSpace() method.
	}
}
