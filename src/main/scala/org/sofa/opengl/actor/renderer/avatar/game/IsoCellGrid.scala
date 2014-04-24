package org.sofa.opengl.actor.renderer.avatar.game

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Box3Sized}
import org.sofa.opengl.{Texture, ShaderResource, ModelResource, TextureResource, TexParams}
import org.sofa.opengl.actor.renderer.{Screen}
import org.sofa.opengl.actor.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarRenderState}
import org.sofa.opengl.actor.renderer.{AvatarEvent, AvatarSpatialEvent, AvatarMotionEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarKeyEvent, AvatarZoomEvent}
import org.sofa.opengl.actor.renderer.{NoSuchAvatarException}

import org.sofa.opengl.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.opengl.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh, HexaTilesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


// == Renders ====================================================================


/** Number of cells in width and height of the hexa tile mesh. */
case class IsoCellGridSize(width:Int, height:Int) extends AvatarRenderState {}


class IsoCellGridRender(avatar:Avatar) extends IsoRender(avatar) with IsoRenderUtils {

	color = Rgba.White

	lineColor = Rgba.Black

	protected[this] var hexaTile:HexaTilesMesh = null

	protected[this] var hexaTex:Texture = null

	protected[this] var imageShader:ShaderProgram = null

	override def changeRender(state:AvatarRenderState) {
		import VertexAttribute._

		val gl = self.screen.gl

		state match {
			case IsoCellGridSize(width,height) => {
				if(hexaTile ne null) hexaTile.dispose
				hexaTile = new HexaTilesMesh(width, height, 1f, 1f, 1, 1)
				imageShader = screen.libraries.shaders.addAndGet(gl, "image-shader", ShaderResource("image-shader", "image_shader.vert.glsl", "image_shader.frag.glsl"))
				hexaTile.newVertexArray(gl, imageShader, Vertex -> "position", TexCoord -> "texCoords")
				hexaTex = screen.libraries.textures.addAndGet(gl, "one-hexa-tile", TextureResource("one-hexa-tile", "OneTile.png", TexParams()))
			}
			case _ => super.changeRender(state)
		}

	}

	override def render() {
		val gl    = self.screen.gl
		val space = self.space
		val text  = screen.textLayer


		space.pushSubSpace
		if(hexaTile eq null) {
			fillAvatarBox
			text.font("Ubuntu-L.ttf", 13)
			text.color(Rgba.Black)
			text.string("Hello", 0.2, 0.1, 0, screen.space)
		} else {
			screen.space.pushpop {
				gl.enable(gl.BLEND)
				screen.space.scale(space.subSpace.size(0), space.subSpace.size(1), 1)
				imageShader.use
				hexaTex.bindUniform(gl.TEXTURE0, imageShader, "texColor")
				screen.space.uniformMVP(imageShader)
				hexaTile.draw(gl)
		        gl.disable(gl.BLEND)
			}
		}
		self.renderSubs
		space.popSubSpace		
	}
}


// == Spaces =====================================================================


/** Space for a cell.
  * The cell does not resizes the space, it only translates to its position.
  * The while game works in the world coordinates. */
class IsoCellGridSpace(avatar:Avatar) extends IsoSpace(avatar) {
	var scale1cm = 1.0

	var fromSpace = new Box3Sized {
		pos.set(0, 0, 0)
		size.set(1, 1, 1)
	}

	var toSpace = new Box3Sized {
		pos.set(0, 0, 0)
		size.set(1, 1, 1)
	}

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def animateSpace() {
	}

	def pushSubSpace() {
 		scale1cm = self.parent.space.scale1cm
		
		val space = self.screen.space

 		space.push
 		space.translate(fromSpace.pos.x, fromSpace.pos.y, 0)
	}

	def popSubSpace() {
		self.screen.space.pop
	}
}


// == Avatars ====================================================================


class IsoCellGrid(name:AvatarName, screen:Screen) 
	extends IsoAvatar(name, screen) {

	var space = new IsoCellGridSpace(this)

	var renderer = new IsoCellGridRender(this)

	def consumeEvent(event:AvatarEvent):Boolean = {
		//println("%s ignore event %s".format(name, event))
		false
	}
}