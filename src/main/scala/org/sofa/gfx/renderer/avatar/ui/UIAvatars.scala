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
			case _ => chainAvatarFor(name, screen, kind)
		}
	}
}


abstract class UIAvatar(name:AvatarName, screen:Screen) extends DefaultAvatarComposed(name, screen) {
	
	/** Another consumeOrPropagateEvent that looks if an avatar is visible to propagate the
	  * event to it. To call only if the event is a spatial one. */
	def consumeOrPropagateSpatialEventVisible(event:Event):Boolean = {
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
				res = consumeOrPropagateSpatialEventVisible(event)
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

			(( (posx >= min(fromx, tox)) && (posx <= max(fromx, tox)) ) &&
			 ( (posy >= min(fromy, toy)) && (posy <= max(fromy, toy)) ))
		} else {
			false
		}
	}
}


// ----------------------------------------------------------------------------------------------


object UIrenderUtils {
	var mesh:TrianglesMesh = null

	var lines:LinesMesh = null

	var shader:ShaderProgram = null


}


trait UIrenderUtils {
	var color = Rgba.Cyan

	var lineColor = Rgba.Red

	def self:Avatar

	def fillAndStroke() {
		import VertexAttribute._
		import UIrenderUtils._

		val screen = self.screen
		val space  = screen.space
		val gl     = screen.gl

		if(shader eq null) {
println("*** NEW SHADER")
			shader = screen.libraries.shaders.addAndGet(gl, "uniform-color-shader", ShaderResource("uniform-color-shader", "uniform_color.vert.glsl", "uniform_color.frag.glsl"))
		}

		if(mesh eq null) {
println("*** NEW MESH")
			mesh = new TrianglesMesh(2)
			mesh.setPoint(0, 0, 0, 0)
			mesh.setPoint(1, 1, 0, 0)
			mesh.setPoint(2, 1, 1, 0)
			mesh.setPoint(3, 0, 1, 0)
			mesh.setTriangle(0, 0, 1, 2)
			mesh.setTriangle(1, 0, 2, 3)
			mesh.newVertexArray(gl, shader, Vertex -> "position")
		}

		if(lines eq null) {
			lines = new LinesMesh(4)
			lines.setLine(0, 0,0,0, 1,0,0)
			lines.setLine(1, 1,0,0, 1,1,0)
			lines.setLine(2, 1,1,0, 0,1,0)
			lines.setLine(3, 0,1,0, 0,0,0)
			lines.newVertexArray(gl, shader, Vertex -> "position")
		}

		val subSpace = self.space.subSpace

		shader.use
		space.pushpop {
			//println(s"    | rendering ${self.name} scale(${subSpace.size(0)},${subSpace.size(1)})")
			shader.uniform("uniformColor", color)
			space.scale(subSpace.sizex, subSpace.sizey, 1)
			space.uniformMVP(shader)
			mesh.lastva.draw(mesh.drawAs(gl))
			shader.uniform("uniformColor", lineColor)
			lines.lastva.draw(lines.drawAs(gl))
		}
	}

	def fill() {
		import VertexAttribute._
		import UIrenderUtils._

		val screen = self.screen
		val space  = screen.space
		val gl     = screen.gl

		if(shader eq null) {
println("*** NEW SHADER")
			shader = screen.libraries.shaders.addAndGet(gl, "uniform-color-shader", ShaderResource("uniform-color-shader", "uniform_color.vert.glsl", "uniform_color.frag.glsl"))
		}

		if(mesh eq null) {
println("*** NEW MESH")
			mesh = new TrianglesMesh(2)
			mesh.setPoint(0, 0, 0, 0)
			mesh.setPoint(1, 1, 0, 0)
			mesh.setPoint(2, 1, 1, 0)
			mesh.setPoint(3, 0, 1, 0)
			mesh.setTriangle(0, 0, 1, 2)
			mesh.setTriangle(1, 0, 2, 3)
			mesh.newVertexArray(gl, shader, Vertex -> "position")
		}

		val subSpace = self.space.subSpace

		shader.use
		space.pushpop {
			//println(s"    | rendering ${self.name} scale(${subSpace.size(0)},${subSpace.size(1)})")
			shader.uniform("uniformColor", color)
			space.scale(subSpace.sizex, subSpace.sizey, 1)
			space.uniformMVP(shader)
			mesh.lastva.draw(mesh.drawAs(gl))
		}
	}

}


class UIAvatarRender(var self:Avatar) extends AvatarRender {
	def screen:Screen = self.screen
}


// ----------------------------------------------------------------------------------------------


abstract class UIAvatarSpace(var self:Avatar) extends AvatarSpace {
}
