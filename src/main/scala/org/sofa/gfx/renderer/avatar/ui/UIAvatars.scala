package org.sofa.gfx.renderer.avatar.ui

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default}
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
}


// ----------------------------------------------------------------------------------------------


trait UIrenderUtils {
	var mesh:TrianglesMesh = null

	var lines:LinesMesh = null

	var shader:ShaderProgram = null

	var color = Rgba.Cyan

	var lineColor = Rgba.Red

	def self:Avatar

	def fillAndStroke() {
		import VertexAttribute._

		val screen = self.screen
		val space  = screen.space
		val gl     = screen.gl

		if(shader eq null) {
			shader = screen.libraries.shaders.addAndGet(gl, "uniform-color-shader", ShaderResource("uniform-color-shader", "uniform_color.vert.glsl", "uniform_color.frag.glsl"))
		}

		if(mesh eq null) {
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
			shader.use
			shader.uniform("uniformColor", color)
			space.scale(subSpace.size(0), subSpace.size(1), 1)
			space.uniformMVP(shader)
			mesh.lastva.draw(mesh.drawAs(gl))
			shader.uniform("uniformColor", lineColor)
			lines.lastva.draw(lines.drawAs(gl))
		}
	}

	def fill() {
		import VertexAttribute._

		val screen = self.screen
		val space  = screen.space
		val gl     = screen.gl

		if(shader eq null) {
			shader = screen.libraries.shaders.addAndGet(gl, "uniform-color-shader", ShaderResource("uniform-color-shader", "uniform_color.vert.glsl", "uniform_color.frag.glsl"))
		}

		if(mesh eq null) {
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
			shader.use
			shader.uniform("uniformColor", color)
			space.scale(subSpace.size(0), subSpace.size(1), 1)
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
