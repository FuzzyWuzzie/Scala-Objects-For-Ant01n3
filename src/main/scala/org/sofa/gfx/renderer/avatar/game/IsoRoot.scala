package org.sofa.gfx.renderer.avatar.game

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Box3Sized}
import org.sofa.gfx.{Texture, ShaderResource, ModelResource, TextureResource, TexParams}
import org.sofa.gfx.renderer.{Screen}
import org.sofa.gfx.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarRenderState}
import org.sofa.gfx.surface.event._
import org.sofa.gfx.renderer.{NoSuchAvatarException}

import org.sofa.gfx.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.gfx.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh}
import org.sofa.gfx.mesh.shapes.{HexaTilesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


class IsoWorldAvatarFactory extends DefaultAvatarFactory {
	override def avatarFor(name:AvatarName, screen:Screen, kind:String):Avatar = {
		kind.toLowerCase match {
			case "iso-root"      => new IsoRoot(name, screen)
			case "iso-world"     => new IsoWorld(name, screen)
			case "iso-layer"     => new IsoLayer(name, screen)
			case "iso-cell-grid" => new IsoCellGrid(name, screen)
			case "iso-cell"      => new IsoCell(name, screen)
			case "iso-entity"    => new IsoEntity(name, screen)
			case _               => chainAvatarFor(name, screen, kind)
		}
	}
}


// == Renders ====================================================================


trait IsoRenderUtils {
	var unitSquare:Mesh = null

	var unitLineSquare:Mesh = null

	var uniformColorShader:ShaderProgram = null

	var fillColor = Rgba.Cyan

	var lineColor = Rgba.Red

	def self:Avatar

	def strokeAvatarBox() {
		import VertexAttribute._

		val screen = self.screen
		val space  = screen.space
		val gl     = screen.gl

		if(uniformColorShader eq null) {
			uniformColorShader = screen.libraries.shaders.getOrAdd(gl, "uniform-color-shader", ShaderResource("uniform-color-shader", "uniform_color.vert.glsl", "uniform_color.frag.glsl"))
		}

		if(unitSquare eq null) {
			unitSquare = screen.libraries.models.getOrAdd(gl, "unit-square") { (gl, name) =>
				val m = new TrianglesMesh(gl, 2)
				m.setVertexPosition(0, -0.5f, -0.5f, 0f)
				m.setVertexPosition(1,  0.5f, -0.5f, 0f)
				m.setVertexPosition(2,  0.5f,  0.5f, 0f)
				m.setVertexPosition(3, -0.5f,  0.5f, 0f)
				m.setTriangle(0, 0, 1, 2)
				m.setTriangle(1, 0, 2, 3)
				m.bindShader(uniformColorShader, Position -> "position")
				new ModelResource(name, m)
			}

			unitLineSquare = screen.libraries.models.getOrAdd(gl, "unit-line-square") { (gl, name) =>
				val l = new LinesMesh(gl, 4)
				l.setLine(0, -0.5f,-0.5f,0f,  0.5f,-0.5f,0f)
				l.setLine(1,  0.5f,-0.5f,0f,  0.5f, 0.5f,0f)
				l.setLine(2,  0.5f, 0.5f,0f, -0.5f, 0.5f,0f)
				l.setLine(3, -0.5f, 0.5f,0f, -0.5f,-0.5f,0f)
				l.bindShader(uniformColorShader, Position -> "position")
				new ModelResource(name, l)
			}
		}

		val subSpace = self.space.subSpace

		uniformColorShader.use
		space.pushpop {
			uniformColorShader.use
			space.scale(subSpace.size(0), subSpace.size(1), 1)
			space.uniformMVP(uniformColorShader)
			uniformColorShader.uniform("uniformColor", lineColor)
			unitLineSquare.draw
		}
	}

	def fillAvatarBox() {
		import VertexAttribute._

		val screen = self.screen
		val space  = screen.space
		val gl     = screen.gl

		if(uniformColorShader eq null) {
			uniformColorShader = screen.libraries.shaders.getOrAdd(gl, "uniform-color-shader", ShaderResource("uniform-color-shader", "uniform_color.vert.glsl", "uniform_color.frag.glsl"))
		}

		if(unitSquare eq null) {
			unitSquare = screen.libraries.models.getOrAdd(gl, "unit-square") { (gl, name) =>
				val m = new TrianglesMesh(gl, 2)
				m.setVertexPosition(0, -0.5f, -0.5f, 0f)
				m.setVertexPosition(1,  0.5f, -0.5f, 0f)
				m.setVertexPosition(2,  0.5f,  0.5f, 0f)
				m.setVertexPosition(3, -0.5f,  0.5f, 0f)
				m.setTriangle(0, 0, 1, 2)
				m.setTriangle(1, 0, 2, 3)
				m.bindShader(uniformColorShader, Position -> "position")
				new ModelResource(name, m)
			}

			unitLineSquare = screen.libraries.models.getOrAdd(gl, "unit-line-square") { (gl, name) =>
				val l = new LinesMesh(gl, 4)
				l.setLine(0, -0.5f,-0.5f,0f,  0.5f,-0.5f,0f)
				l.setLine(1,  0.5f,-0.5f,0f,  0.5f, 0.5f,0f)
				l.setLine(2,  0.5f, 0.5f,0f, -0.5f, 0.5f,0f)
				l.setLine(3, -0.5f, 0.5f,0f, -0.5f,-0.5f,0f)
				l.bindShader(uniformColorShader, Position -> "position")
				new ModelResource(name, l)
			}
		}

		val subSpace = self.space.subSpace

		uniformColorShader.use
		space.pushpop {
			uniformColorShader.use
			uniformColorShader.uniform("uniformColor", fillColor)
			space.scale(subSpace.size(0), subSpace.size(1), 1)
			space.uniformMVP(uniformColorShader)
			unitSquare.draw
			uniformColorShader.uniform("uniformColor", lineColor)
			unitLineSquare.draw
		}
	}
}


class IsoRender(var self:Avatar) extends AvatarRender {
	def screen:Screen = self.screen
}


class IsoRootRender(avatar:Avatar) extends IsoRender(avatar) {
	override def render() {
		val gl = screen.gl

		//gl.clearColor(Rgba.Grey50)
		gl.clearColor(Rgba.Black)
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)

		gl.lineWidth(1f)
		gl.disable(gl.DEPTH_TEST)
		gl.enable(gl.BLEND)
		//gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
		gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)		// Premultiplied alpha

		//println(s"* render ${self.name}")
		gl.checkErrors
		super.render
	}
}




// == Spaces =====================================================================


abstract class IsoSpace(var self:Avatar) extends AvatarSpace {
}


/** A basic space that goes from the root screen space (-1,-1,-1)->(1,1,1)
  * right handed, toward a (0,0,0)->(1,1,1) space where origin is at the
  * top-left corner with X positive right and Y positive down (occidental
  * reading direction).
  *
  * This is ideal for UIs, and indeed is used as the root to display
  * User interfaces above the game display. */
class IsoRootSpace(avatar:Avatar) extends IsoSpace(avatar) {
	/** Ratiohw height / width. */
	var ratiohw = 1.0

	var scale1cm = 1.0 

	protected val fromSpace = new Box3Default {
		pos.set(0, 0, 0)
		from.set(-1, -1, -1)
		to.set(1, 1, 1)
		size.set(2, 2, 2)
	}

	protected val toSpace = new Box3Default {
		pos.set(0, 0, 0)
		from.set(0, 0, 0)
		to.set(1, 1, 1)
		size.set(1, 1, 1)
	}

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def animateSpace() {
		val screen  = avatar.screen
		val surface = screen.surface
		ratiohw     = surface.height.toDouble / surface.width.toDouble 		
		val dpc     = 37.0	// 37 dots-pixels per centimeter
		scale1cm    = 1.0 / (screen.surface.width / dpc)	// One centimeter in the sub-space equals this

		toSpace.to.set(1, 1*ratiohw, 1)
		toSpace.size.set(1, 1*ratiohw, 1)

		layoutSubs
	}

	override def pushSubSpace() {
		val space = self.screen.space

		// we pass from x [-1..1] positive right and y [-1..1] positive top to
		// x [0..1] positive right and y [0..ratio] positive down.

		space.push
		space.translate(-1, 1, 0)
		space.scale(2, -2/ratiohw, 1)
	}

	override def popSubSpace() { self.screen.space.pop }

	def layoutSubs() {
		self.foreachSub { sub =>
			sub.space.thisSpace.setPosition(0, 0, 0)
			sub.space.thisSpace.setSize(toSpace.size.x, toSpace.size.y, 1)
		}
	}
}


// == Avatars ====================================================================


abstract class IsoAvatar(name:AvatarName, screen:Screen) extends DefaultAvatarComposed(name, screen) {}


/** Iso Root is a container for the game, it has a
  * space of 1 unit width and 1*ratio unit height.
  * with an origin at the lower left corner. It allows
  * to add a world that will define the game 2D coordinates,
  * but also to add an UI on top of it.
  *
  * We use three coordinate systems:
  *   - Root
  *   - World 2D coordinate system of the game representation
  *   - Game 3D coordinate system where Y is up, X is right, and Z front.
  */
class IsoRoot(name:AvatarName, screen:Screen)
	extends IsoAvatar(name, screen) {

	var space = new IsoRootSpace(this)
	
	var renderer = new IsoRootRender(this)	

	def consumeEvent(event:Event):Boolean = false

	// def consumeEvent(event:Event):Boolean = {
	// 	println("IsoRoot event: %s".format(event))
	// 	true
	// }
}