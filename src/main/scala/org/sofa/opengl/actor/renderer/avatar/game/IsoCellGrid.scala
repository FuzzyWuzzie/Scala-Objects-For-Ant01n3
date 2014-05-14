package org.sofa.opengl.actor.renderer.avatar.game

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Box3Sized}
import org.sofa.opengl.{Texture, ShaderResource, ModelResource, TextureResource, TexParams}
import org.sofa.opengl.actor.renderer.{Screen}
import org.sofa.opengl.actor.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarRenderState, AvatarBaseStates}
import org.sofa.opengl.actor.renderer.{AvatarEvent, AvatarSpatialEvent, AvatarMotionEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarKeyEvent, AvatarZoomEvent}
import org.sofa.opengl.actor.renderer.{NoSuchAvatarException}

import org.sofa.opengl.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.opengl.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh, HexaTilesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


// == Renders ====================================================================


case class IsoCellConfig(relief:Float, ground:Int = 0, underground:Int = 0) {}


case class IsoCellGridRelief(relief:Array[Array[Float]]) extends AvatarRenderState {}


case class IsoCellGridConfig(config:Array[Array[IsoCellConfig]]) extends AvatarRenderState {}


object IsoCellGrid {
	/** Shortcut to the value of sqrt(3). */
	final val Sqrt3  = math.sqrt(3)
	/** Number of cells along X. */
	final val Width  = 10
	/** Number of cells along Z. */
	final val Height = 10

	/** Bottom-left U texture coordinate of the four underground patterns. */
	final val UGTexU = Array[Float](0.027f, 0.027f, 0.514f, 0.514f)
	/** Bottom-left V texture coordinate of the four underground patterns. */
	final val UGTexV = Array[Float](0.015f, 0.453f, 0.015f, 0.453f)

	/** Bottom-left U texture coordinate of the six ground patterns. */
	final val GTexU = Array[Float](0.027f, 0.027f, 0.027f, 0.514f, 0.514f, 0.514f)
	/** Bottom-left V texture coordinate of the six ground patterns. */
	final val GTexV = Array[Float](0.046f, 0.359f, 0.671f, 0.046f, 0.359f, 0.671f)

	/** Width (U) in texture coordinate of an underground pattern. */
	final val UGU = 0.433f
	/** Height (V) in texture coordinate of an underground pattern. */
	final val UGV = 0.468f

	/** Width (U) in texture coordinate of an ground pattern. */
	final val GU = 0.433f
	/** Height (V) in texture coordinate of an ground pattern. */
	final val GV = 0.281f
}


class IsoCellGridRender(avatar:Avatar) extends IsoRender(avatar) with IsoRenderUtils {

	color = Rgba.White

	lineColor = Rgba.Black

	protected[this] var ground:TrianglesMesh = null

	protected[this] var underground:TrianglesMesh = null

	protected[this] var groundMask:Texture = null

	protected[this] var undergroundMask:Texture = null

	protected[this] var groundColor:Texture = null

	protected[this] var undergroundColor:Texture = null

	protected[this] var isoShader:ShaderProgram = null

	override def changeRender(state:AvatarRenderState) {
		import IsoCellGrid._
	 	state match {
	 		case IsoCellGridConfig(config) => if(config.length >= Height && config(0).length >= Width) {
	 			init(config)
	 		} else {
	 			Console.err.println(s"bad config size for IsoCellGridRender, expected at least (${Width}x${Height}) got (${config.length}x${config(0).length})")
	 		}
	 		case IsoCellGridRelief(relief) => if(relief.length >= Height && relief(0).length >= Width) {
	 			changeRelief(relief)
	 		} else {
	 			Console.err.println(s"bad relief size for IsoCellGridRender, expected at least (${Width}x${Height}) got (${relief.length}x${relief(0).length})")
	 		}
	 		case _ => super.changeRender(state)
	 	}
	}

	protected def init(config:Array[Array[IsoCellConfig]]) {
		import VertexAttribute._
		import IsoCellGrid._

		val gl = self.screen.gl

		if(ground      ne null) ground.dispose
		if(underground ne null) underground.dispose
		
		ground      = new TrianglesMesh(2 * Width * Height)
		underground = new TrianglesMesh(2 * Width * Height)
		
		isoShader = screen.libraries.shaders.addAndGet(gl, "iso-shader", ShaderResource("iso-shader", "iso.vert.glsl", "iso.frag.glsl"))
		
		groundColor      = screen.libraries.textures.addAndGet(gl, "ground-color-1",      TextureResource("ground-color-1",      "IsoTemplate_1024_Ground.png",           TexParams()))
		undergroundColor = screen.libraries.textures.addAndGet(gl, "underground-color-1", TextureResource("underground-color-1", "IsoTemplate_1024_Underground.png",      TexParams()))
		groundMask       = screen.libraries.textures.addAndGet(gl, "ground-mask-1",       TextureResource("ground-mask-1",       "IsoTemplate_1024_Ground-Mask.png",      TexParams()))
		undergroundMask  = screen.libraries.textures.addAndGet(gl, "underground-mask-1",  TextureResource("underground-mask-1",  "IsoTemplate_1024_Underground-Mask.png", TexParams()))

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

		var x  = 0
		var y  = 0
		var p  = 0
		var t  = 0
		
		val w2 = (2 * Sqrt3).toFloat
		var h2 = 2f


		val u0 = 0.027f
		val v0 = 0.0
		val uu = 0.433f
		val vv = 0.468f

		while(y < Height) {
			x = 0
			while(x < Width) {
				h2 = 2f

				val re = if(savedRelief ne null) savedRelief(y)(x) else config(y)(x).relief 			// Relief (offset y)
				val xx = (if(y % 2 == 0) (x * 4 * Sqrt3) else (x * 4 * Sqrt3 + 2 * Sqrt3)).toFloat		// x center of cell
				val yy = y * 2f																			// y center of cell
				val zz = y * 0.1f																		// depth
				var gu = GTexU(config(y)(x).ground)														// left-bottom U of tex
				var gv = GTexV(config(y)(x).ground)														// left-bottom V of tex

				ground.setPoint(p+0, xx-w2, (yy-(h2+0.5f))+re, zz); ground.setPointTexCoord(p+0, gu,      gv)
				ground.setPoint(p+1, xx+w2, (yy-(h2+0.5f))+re, zz); ground.setPointTexCoord(p+1, gu + GU, gv)
				ground.setPoint(p+2, xx-w2, (yy+h2       )+re, zz); ground.setPointTexCoord(p+2, gu,      gv + GV)
				ground.setPoint(p+3, xx+w2, (yy-(h2+0.5f))+re, zz); ground.setPointTexCoord(p+3, gu + GU, gv)
				ground.setPoint(p+4, xx+w2, (yy+h2       )+re, zz); ground.setPointTexCoord(p+4, gu + GU, gv + GV)
				ground.setPoint(p+5, xx-w2, (yy+h2       )+re, zz); ground.setPointTexCoord(p+5, gu,      gv + GV)
				
				ground.setTriangle(t+0, p+0, p+1, p+2)
				ground.setTriangle(t+1, p+3, p+4, p+5)

				h2 = 8f
				gu = UGTexU(config(y)(x).underground)
				gv = UGTexV(config(y)(x).underground)

				underground.setPoint(p+0, xx-w2, yy-(h2+0.5f), zz); underground.setPointTexCoord(p+0, gu,       gv)
				underground.setPoint(p+1, xx+w2, yy-(h2+0.5f), zz); underground.setPointTexCoord(p+1, gu + UGU, gv)
				underground.setPoint(p+2, xx-w2, yy-(   0.5f), zz); underground.setPointTexCoord(p+2, gu,       gv + UGV)
				underground.setPoint(p+3, xx+w2, yy-(h2+0.5f), zz); underground.setPointTexCoord(p+3, gu + UGU, gv)
				underground.setPoint(p+4, xx+w2, yy-(   0.5f), zz); underground.setPointTexCoord(p+4, gu + UGU, gv + UGV)
				underground.setPoint(p+5, xx-w2, yy-(   0.5f), zz); underground.setPointTexCoord(p+5, gu,       gv + UGV)

				underground.setTriangle(t+0, p+0, p+1, p+2)
				underground.setTriangle(t+1, p+3, p+4, p+5)

				p += 6
				t += 2
				x += 1
			}

			y += 1
		}

		ground.newVertexArray(gl, isoShader, Vertex -> "position", TexCoord -> "texCoords")
		underground.newVertexArray(gl, isoShader, Vertex -> "position", TexCoord -> "texCoords")				
		changedRelief = false
		savedRelief = null
	}

	protected[this] var changedRelief = false
	protected[this] val lightDir = Vector3(1, 1.5, 0)
	protected[this] var dir = 0.01

	protected def changeRelief(relief:Array[Array[Float]]) {
		import IsoCellGrid._

		if(ground ne null) {
			var x = 0
			var y = 0

			while(y < Height) {
				x = 0
				while(x < Width) {
					if(relief(y)(x) != 0)
						changeReliefAt(x, y, relief(y)(x))

					x += 1
				}

				y += 1
			}

		} else {
			savedRelief = relief
		}
	}

	protected[this] var savedRelief:Array[Array[Float]] = null

	protected def changeReliefAt(x:Int, y:Int, delta:Float) {
		if(ground ne null) {
			import IsoCellGrid._

			val xx = (if(y % 2 == 0) (x * 4 * Sqrt3) else (x * 4 * Sqrt3 + 2 * Sqrt3)).toFloat
			val yy = y * 2f
			val zz = y * 0.1f
			val p  = (y * Width + x) * 6
			val t  = (y * Width + x) * 2

			val w2 = (2 * Sqrt3).toFloat
			var h2 = 2f

			ground.setPoint(p+0, xx-w2, (yy-(h2+0.5f))+delta, zz)
			ground.setPoint(p+1, xx+w2, (yy-(h2+0.5f))+delta, zz)
			ground.setPoint(p+2, xx-w2, (yy+ h2      )+delta, zz)
			ground.setPoint(p+3, xx+w2, (yy-(h2+0.5f))+delta, zz)
			ground.setPoint(p+4, xx+w2, (yy- h2      )+delta, zz)
			ground.setPoint(p+5, xx-w2, (yy- h2      )+delta, zz)

			h2 = 8f

			underground.setPoint(p+0, xx-w2, (yy-(h2+0.5f))+delta, zz); underground.setPointTexCoord(p+0, 0.027f, 0.015f)
			underground.setPoint(p+1, xx+w2, (yy-(h2+0.5f))+delta, zz); underground.setPointTexCoord(p+1, 0.460f, 0.015f)
			underground.setPoint(p+2, xx-w2, (yy-(   0.5f))+delta, zz); underground.setPointTexCoord(p+2, 0.027f, 0.484f)
			underground.setPoint(p+3, xx+w2, (yy-(h2+0.5f))+delta, zz); underground.setPointTexCoord(p+3, 0.460f, 0.015f)
			underground.setPoint(p+4, xx+w2, (yy-(   0.5f))+delta, zz); underground.setPointTexCoord(p+4, 0.460f, 0.484f)
			underground.setPoint(p+5, xx-w2, (yy-(   0.5f))+delta, zz); underground.setPointTexCoord(p+5, 0.027f, 0.484f)

			changedRelief = true
		}
	}

	protected def updateRelief() {
		if(changedRelief) {
			val gl = self.screen.gl
			
			ground.updateVertexArray(gl, updateVertices=true)
			underground.updateVertexArray(gl, updateVertices=true)
			changedRelief = false
		}
	}

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
		
			if(ground ne null) {
				if(changedRelief) updateRelief

				screen.space.pushpop {
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
class IsoCellGridSpace(avatar:Avatar) extends IsoSpace(avatar) {
	var scale1cm = 1.0

	var fromSpace = new Box3Sized {
		pos.set(0, 0, 0)
		size.set(1, 1, 1)
	}

	var toSpace = fromSpace

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def changeSpace(newState:AvatarSpaceState) {
		import IsoCellGrid._

		newState match {
			case AvatarBaseStates.Move(offset) => {
				println("Cannot move an iso cell, use MoveAt")
			}
			case AvatarBaseStates.MoveAt(position:Point3) => {
					fromSpace.pos.x = position.x * 4 * Sqrt3 * Width
					fromSpace.pos.y = position.y * 2 * Height
					fromSpace.pos.z = position.y
			}
			case AvatarBaseStates.Resize(size) => {
				println("Cannot resize an iso cell")
			}
			case _ => super.changeSpace(newState)
		}
	}

	override def animateSpace() {}

	def pushSubSpace() {
 		scale1cm  = self.parent.space.scale1cm		
		val space = self.screen.space

 		space.push
 		space.translate(fromSpace.pos.x, fromSpace.pos.y, fromSpace.pos.z)
	}

	def popSubSpace() { self.screen.space.pop }
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