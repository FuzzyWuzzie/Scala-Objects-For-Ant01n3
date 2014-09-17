package org.sofa.gfx.renderer.avatar.game

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Box3Sized}
import org.sofa.gfx.{Texture, ShaderResource, ModelResource, TextureResource, TexParams}
import org.sofa.gfx.renderer.{Screen}
import org.sofa.gfx.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarRenderState, AvatarBaseStates}
import org.sofa.gfx.surface.event._
import org.sofa.gfx.renderer.{NoSuchAvatarException}

import org.sofa.gfx.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.gfx.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh, HexaTilesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


object IsoCell {
	final val Sqrt3 = math.sqrt(3)
}


// == Renders ====================================================================


class IsoCellRender(avatar:Avatar) extends IsoRender(avatar) with IsoRenderUtils {

	fillColor = Rgba.White

	lineColor = Rgba.Black

	protected[this] var ground:TrianglesMesh = null

	protected[this] var underground:TrianglesMesh = null

	protected[this] var groundMask:Texture = null

	protected[this] var undergroundMask:Texture = null

	protected[this] var groundColor:Texture = null

	protected[this] var undergroundColor:Texture = null

	protected[this] var isoShader:ShaderProgram = null

	// override def changeRender(state:AvatarRenderState) {
	// 	state match { case _ => super.changeRender(state) }
	// }

	protected def init() {
		import VertexAttribute._
		import IsoCell._

		val gl = self.screen.gl

		if(ground ne null) ground.dispose
		if(underground ne null) underground.dispose
		
		ground = new TrianglesMesh(2)
		underground = new TrianglesMesh(2)
		
		isoShader = screen.libraries.shaders.getOrAdd(gl, "iso-shader", ShaderResource("iso-shader", "iso.vert.glsl", "iso.frag.glsl"))
		
		groundColor      = screen.libraries.textures.getOrAdd(gl, "ground-color-1",      TextureResource("ground-color-1",      "IsoTemplate_1024_Ground.png",           TexParams()))
		undergroundColor = screen.libraries.textures.getOrAdd(gl, "underground-color-1", TextureResource("underground-color-1", "IsoTemplate_1024_Underground.png",      TexParams()))
		groundMask       = screen.libraries.textures.getOrAdd(gl, "ground-mask-1",       TextureResource("ground-mask-1",       "IsoTemplate_1024_Ground-Mask.png",      TexParams()))
		undergroundMask  = screen.libraries.textures.getOrAdd(gl, "underground-mask-1",  TextureResource("underground-mask-1",  "IsoTemplate_1024_Underground-Mask.png", TexParams()))

		// Underground:
		//     Width:  4*sqrt(3)
		//     Height: 8
		// Ground:
		//     Width:  4*sqrt(3)
		//     Height: 4.5
		// Offset tiles:
		//     Width:  2*sqrt(3)
		//     Height: 2
		// Offset Underground:
		//     Width: 0
		//     Height: -0.5

		// 2+    5+----+4
		//  |\     \   |
		//  | \     \ B|
		//  | A\     \ |
		//  |   \     \|
		// 0+----+1    +3

		var w2 = (2 * Sqrt3).toFloat
		var h2 = 2f

		ground.setPoint(0, -w2, -(h2+0.5f), 0); ground.setPointTexCoord(0, 0.027f, 0.359f)
		ground.setPoint(1,  w2, -(h2+0.5f), 0); ground.setPointTexCoord(1, 0.460f, 0.359f)
		ground.setPoint(2, -w2,  h2,        0); ground.setPointTexCoord(2, 0.027f, 0.640f)
		ground.setPoint(3,  w2, -(h2+0.5f), 0); ground.setPointTexCoord(3, 0.460f, 0.359f)
		ground.setPoint(4,  w2,  h2,        0); ground.setPointTexCoord(4, 0.460f, 0.640f)
		ground.setPoint(5, -w2,  h2,        0); ground.setPointTexCoord(5, 0.027f, 0.640f)
		ground.setTriangle(0, 0, 1, 2)
		ground.setTriangle(1, 3, 4, 5)

		w2 = (2 * Sqrt3).toFloat
		h2 = 8f
		
		underground.setPoint(0, -w2, -(h2+0.5f), 0); underground.setPointTexCoord(0, 0.027f, 0.015f)
		underground.setPoint(1,  w2, -(h2+0.5f), 0); underground.setPointTexCoord(1, 0.460f, 0.015f)
		underground.setPoint(2, -w2, -(   0.5f), 0); underground.setPointTexCoord(2, 0.027f, 0.484f)
		underground.setPoint(3,  w2, -(h2+0.5f), 0); underground.setPointTexCoord(3, 0.460f, 0.015f)
		underground.setPoint(4,  w2, -(   0.5f), 0); underground.setPointTexCoord(4, 0.460f, 0.484f)
		underground.setPoint(5, -w2, -(   0.5f), 0); underground.setPointTexCoord(5, 0.027f, 0.484f)

		underground.setTriangle(0, 0, 1, 2)
		underground.setTriangle(1, 3, 4, 5)

		ground.newVertexArray(gl, isoShader, Vertex -> "position", TexCoord -> "texCoords")
		underground.newVertexArray(gl, isoShader, Vertex -> "position", TexCoord -> "texCoords")				
	}

	protected[this] val lightDir = Vector3(1, 1.5, 0)
	protected[this] var dir = 0.01

	override def render() {
		val gl    = self.screen.gl
		val space = self.space
		val text  = screen.textLayer

		lightDir.x = lightDir.x + dir
		lightDir.z = lightDir.z - dir

		if(lightDir.x > 1) { lightDir.x = 1; dir = -dir }
		else if(lightDir.x < 0) { lightDir.x = 0; dir = -dir }
		if(lightDir.z > 1) { lightDir.z = 1 }
		else if(lightDir.z < 0) { lightDir.z = 0 }

		space.pushSubSpace
		
			if(ground eq null)
				init

			screen.space.pushpop {
				//fillAvatarBox
				gl.enable(gl.BLEND)
				gl.enable(gl.DEPTH_TEST)
				isoShader.use
				undergroundColor.bindUniform(gl.TEXTURE0, isoShader, "texColor")
				undergroundMask.bindUniform(gl.TEXTURE1, isoShader, "texMask")
				isoShader.uniform("lightDir", lightDir)
				screen.space.uniformMVP(isoShader)
				underground.draw(gl)
				screen.space.translate(0,0,-0.1)
				screen.space.uniformMVP(isoShader)
				groundColor.bindUniform(gl.TEXTURE0, isoShader, "texColor")
				groundMask.bindUniform(gl.TEXTURE1, isoShader, "texMask")
				ground.draw(gl)
				gl.disable(gl.DEPTH_TEST)
		        gl.disable(gl.BLEND)
			}
	
			self.renderSubs
		
		space.popSubSpace		
	}
}


// == Spaces =====================================================================


/** Space for a cell.
  * The cell does not resizes the space, it only translates to its position, therefore
  * super and sub spaces are the same.
  * The while game works in the world coordinates. */
class IsoCellSpace(avatar:Avatar) extends IsoSpace(avatar) {
	var scale1cm = 1.0

	var fromSpace = new Box3Sized {
		pos.set(0, 0, 0)
		size.set(1, 1, 1)
	}

	var toSpace = fromSpace

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def changeSpace(newState:AvatarSpaceState) {
		import IsoCell._

		newState match {
			case AvatarBaseStates.Move(offset) => {
				//fromSpace.pos.x += offset.x * scala.math.sqrt(3)
				//fromSpace.pos.y += offset.y * 2 * (3.0/4.0)
				println("Cannot move an iso cell, use MoveAt")
			}
			case AvatarBaseStates.MoveAt(position:Point3) => {
				if(position.y.toInt % 2 == 0) {
					fromSpace.pos.x = position.x * 4 * Sqrt3
					fromSpace.pos.y = position.y * 2 + (position.z * 0.5)
					fromSpace.pos.z = position.y
				} else {
					fromSpace.pos.x = position.x * 4 * Sqrt3 + 2 * Sqrt3
					fromSpace.pos.y = position.y * 2 + (position.z * 0.5)					
					fromSpace.pos.z = position.y
				}
			}
			case AvatarBaseStates.Resize(size) => {
				//self.renderer.changeRender(IsoCellGridSize(size))
				//fromSpace.size.copy(size)
				println("Cannot resize an iso cell")
			}
			case _ => super.changeSpace(newState)
		}
	}

	override def animateSpace() {}

	override def pushSubSpace() {
 		scale1cm  = self.parent.space.scale1cm		
		val space = self.screen.space

 		space.push
 		space.translate(fromSpace.pos.x, fromSpace.pos.y, fromSpace.pos.z)
	}

	override def popSubSpace() { self.screen.space.pop }
}


// == Avatars ====================================================================


class IsoCell(name:AvatarName, screen:Screen) 
	extends IsoAvatar(name, screen) {

	var space = new IsoCellSpace(this)

	var renderer = new IsoCellRender(this)

	def consumeEvent(event:Event):Boolean = false
}