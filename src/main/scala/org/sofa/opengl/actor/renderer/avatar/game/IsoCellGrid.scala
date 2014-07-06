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


/** Configure one cell of a grid.
  *
  * @param relief Offset of the whole cell in Y axis (game coordinates).
  * @param texx index in X of a texture patch inside the texture config.
  * @param texy index in Y of a texture patch inside the texture config.
  * @param texOffsetMarker If true, vertices are marked as being modified by texture offset.
  */
case class IsoCellGridRelief(relief:Float, texx:Int, texy:Int, texOffsetMarker:Boolean=false)


/** Configure the shape of cells.
  *
  * Each cell is in fact represented by a rectangle whose height is variable
  * and width is always Sqrt(3). The rectangle origin can be offset from its
  * center. 
  *
  * @param offsetx X offset of the origin from the rectangle center.
  * @param offsety Y offset of the origin from the rectangle center.
  */
case class IsoCellGridShape(offsetx:Float, offsety:Float)


/** Configure the shader and textures to use for the cell.
  *
  * @param shader Identifier of the shader to use in the library.
  * @param color Identifier of the color texture in the library.
  * @param mask Identifier of the mask texture in the library.
  * @param w The width of a rectangle patch in the texture in UV coordinates.
  * @param h The height of a rectangle patch in the texture in UV coordinates.
  * @param x The set of u coordinates (in UV) for patches.
  * @param y The set of v coordinates (in UV) for patches.
  */
case class IsoCellGridShade(shader:String, color:String, mask:String,
				w:Float, h:Float, x:Array[Float], y:Array[Float])


/** Express how to create and render the iso-cell grid.
  *
  * @param shade The shader and textures to use.
  * @param shape The shape of a single cell.
  * @param relief A 2D array of [[IsoCellGridRelief]] that describes the elevation and texture to use for each cell, the
  *               size of the array gives the number of cells.
  */
case class IsoCellGridConfig(shade:IsoCellGridShade, shape:IsoCellGridShape,
				relief:Array[Array[IsoCellGridRelief]]) 
					extends AvatarRenderState with AvatarSpaceState {}


object IsoCellGrid {
	/** Shortcut to the value of sqrt(3). */
	final val Sqrt3 = math.sqrt(3).toFloat

	/** Name of user-defined attribute for the triangles mesh of the ground.
	  * This attribute allows to specify a displacement of the texture on some
	  * vertices only. If the x of this attribute is not zero, the vertice must
	  * move. The y and z are reserved for future usage. */
	final val UserAttr = "user"
}


class IsoCellGridRender(avatar:Avatar) extends IsoRender(avatar) with IsoRenderUtils {
	import IsoCellGrid._

	fillColor = Rgba.White

	lineColor = Rgba.Black

	protected[this] var mesh:TrianglesMesh = null

	protected[this] var color:Texture = null

	protected[this] var mask:Texture = null

	protected[this] var shader:ShaderProgram = null

	protected[this] var world:Avatar = null

	override def changeRender(state:AvatarRenderState) {
		import IsoCellGrid._
	 	state match {
	 		case IsoCellGridConfig(shade, shape, relief) => init(shade, shape, relief)
	 		case _ => super.changeRender(state)
	 	}
	}

	protected def init(shade:IsoCellGridShade, shape:IsoCellGridShape, relief:Array[Array[IsoCellGridRelief]]) {
		import VertexAttribute._

		val gl = self.screen.gl
		val gh = relief.length							// Number of cells Y
		val gw = if(gh > 0) relief(0).length else 0		// Number of cells X

		if(gh > 0 && gw > 0) {
			if(mesh ne null) mesh.dispose
		
			mesh = new TrianglesMesh(6 * gw * gh)		// 6 triangles per cell.

			mesh.addAttribute(UserAttr, 3)

			shader = screen.libraries.shaders.get(gl, shade.shader)
			color  = screen.libraries.textures.get(gl, shade.color)
			mask   = screen.libraries.textures.get(gl, shade.mask)
			
			//                      +---+        +---+  The 6 triangles,
			//     +---+---+  0.5   |  /   +  +   \  |  the 18 points.
			//     |4 /|\ 5|        | /   /|  |\   \ |     <--+
			//     | / | \ |        |/   / |  | \   \|        |  CCW
			//     |/  |  \|        +   /  |  |  \   +    |   | 
			//     + 0 | 1 +           +   |  |   +       +---+
			//     |\  |  /|        +   \  |  |  /   +
			//     | \ | / |        |\   \ |  | /   /|
			//     | 2\|/3 |        | \   \|  |/   / |
			//     +---+---+ -0.5   |  \   +  +   /  |
			// -Sqrt3/2  +Sqrt3/2   +---+        +---+

			var x  = 0  					// X coordinate in the iso-cells space.
			var y  = 0 						// Y coordinate in the iso-cells space.
			var p  = 0 						// Current point (8 per cell).
			var t  = 0 						// Current triangle (2 per cell).
			val uw = shade.w 				// Width in UV of a texture patch.
			val vh = shade.h 				// Height in UV of a texture patch.
			val w2 = Sqrt3 / 2f				// Half-width of a cell plus offset x.
			val h2 = 0.5f 					// Half-height of a cell plus offset y.

			while(y < gh) {
				x = 0
				while(x < gw) {
					val xx = shape.offsetx + offX(x, y)								// Real position X
					val yy = shape.offsety + offY(x, y) + relief(y)(x).relief		// Real position Y
					val uu = shade.x(relief(y)(x).texx)								// Lower left tex U
					val vv = shade.y(relief(y)(x).texy)								// Lower left tex V
					val texOff = if(relief(y)(x).texOffsetMarker) 1 else 0

					// Triangle 0
					mesh v(p+0) xyz(xx,    yy-h2, 0) uv(uu+uw/2, vv)       user(UserAttr, texOff, 0, 0)
					mesh v(p+1) xyz(xx,    yy+h2, 0) uv(uu+uw/2, vv+vh)    user(UserAttr, texOff, 0, 0)
					mesh v(p+2) xyz(xx-w2, yy,    0) uv(uu,      vv+vh/2)  user(UserAttr, texOff, 0, 0)
					mesh triangle(t+0, p+0, p+1, p+2)

					// Triangle 1
					mesh v(p+3) xyz(xx,    yy-h2, 0) uv(uu+uw/2, vv)       user(UserAttr, texOff, 0, 0)
					mesh v(p+4) xyz(xx,    yy+h2, 0) uv(uu+uw/2, vv+vh)    user(UserAttr, texOff, 0, 0)
					mesh v(p+5) xyz(xx+w2, yy,    0) uv(uu+uw,   vv+vh/2)  user(UserAttr, texOff, 0, 0)
					mesh triangle(t+1, p+3, p+4, p+5)

					// Triangle 2
					mesh v(p+6) xyz(xx-w2, yy-h2, 0)  uv(uu,       vv)       user(UserAttr, 0, 0, 0)
					mesh v(p+7) xyz(xx,    yy-h2, 0)  uv(uu+0.01f, vv)       user(UserAttr, 0, 0, 0)
					mesh v(p+8) xyz(xx-w2, yy,    0)  uv(uu,       vv+0.01f) user(UserAttr, 0, 0, 0)
					mesh triangle(t+2, p+6, p+7, p+8)

					// Triangle 3
					mesh v(p+ 9) xyz(xx,    yy-h2, 0) uv(uu,       vv)       user(UserAttr, 0, 0, 0)
					mesh v(p+10) xyz(xx+w2, yy-h2, 0) uv(uu+0.01f, vv)       user(UserAttr, 0, 0, 0)
					mesh v(p+11) xyz(xx+w2, yy,    0) uv(uu+0.01f, vv+0.01f) user(UserAttr, 0, 0, 0)
					mesh triangle(t+3, p+9, p+10, p+11)

					// Triangle 4
					mesh v(p+12) xyz(xx+w2, yy,    0) uv(uu,       vv)       user(UserAttr, 0, 0, 0)
					mesh v(p+13) xyz(xx+w2, yy+h2, 0) uv(uu+0.01f, vv+0.01f) user(UserAttr, 0, 0, 0)
					mesh v(p+14) xyz(xx,    yy+h2, 0) uv(uu,       vv+0.01f) user(UserAttr, 0, 0, 0)
					mesh triangle(t+4, p+12, p+13, p+14)

					// Triangle 5
					mesh v(p+15) xyz(xx-w2, yy,    0) uv(uu+0.01f, vv)       user(UserAttr, 0, 0, 0)
					mesh v(p+16) xyz(xx,    yy+h2, 0) uv(uu+0.01f, vv+0.01f) user(UserAttr, 0, 0, 0)
					mesh v(p+17) xyz(xx-w2, yy+h2, 0) uv(uu,       vv+0.01f) user(UserAttr, 0, 0, 0)
					mesh triangle(t+5, p+15, p+16, p+17)

					p += 18
					t += 6
					x += 1
				}

				y += 1
			}

			mesh.newVertexArray(gl, shader, Vertex -> "position", TexCoord -> "texCoords", UserAttr -> UserAttr)
	
			world = screen.avatar(AvatarName("root.world")).getOrElse(throw new RuntimeException("no world avatar ??"))
		}
	}

	/** Convert a position (x,y) in cell space to real space absissa. */
	protected def offX(x:Int, y:Int):Float = -(y * Sqrt3 * 0.5f) + (x * Sqrt3 * 0.5f)

	/** Convert a position (x,y) in cell space to real space ordinate. */
	protected def offY(x:Int, y:Int):Float = -(y * 0.5f) - (x * 0.5f)

	override def render() {
		val gl    = self.screen.gl
		val space = self.space
		val text  = screen.textLayer

		space.pushSubSpace
		
			if(mesh ne null) {
				val worldr = world.renderer.asInstanceOf[IsoWorldRender]
				val sunDir = worldr.sunDir
				val seaOff = worldr.seaOffset

				screen.space.pushpop {
					gl.enable(gl.BLEND)
					gl.disable(gl.DEPTH_TEST)
					shader.use
					color.bindUniform(gl.TEXTURE0, shader, "texColor")
					mask.bindUniform(gl.TEXTURE1, shader, "texMask")
					shader.uniform(IsoWorld.SunDir, sunDir)
					shader.uniform(IsoWorld.TexOffset, seaOff)
					screen.space.uniformMVP(shader)
					mesh.draw(gl)
		   	    	gl.disable(gl.BLEND)
		    	}
		    }
		    //strokeAvatarBox

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
					fromSpace.pos.x = (position.y * Sqrt3 * fromSpace.size.x * 0.5) + (position.x * Sqrt3 * fromSpace.size.x * 0.5)
					fromSpace.pos.y = (position.y * 0.5 * fromSpace.size.y) - (position.x * 0.5 * fromSpace.size.y)
					fromSpace.pos.z = 0
			}
			case AvatarBaseStates.Resize(size) => {
				println("Cannot resize an iso cell")
			}
			case IsoCellGridConfig(shade, shape, relief) => {
				val h = relief.length
				val w = if(h > 0) relief(0).length else 0

				if(h > 0 && w > 0)
					fromSpace.size.set(w, h, 1)
				else println("A*******AH?????")

				println(s"w ${w} h ${h} size ${fromSpace.size}")
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


/** An IsoCell grid is a grid of tiles used to build a regular array of isometric
  * cells.
  *
  * Each cell is 4*Sqrt(3) width and at least 4 in height. However most of the time
  * cells are higher. A typical ground cell is 4.5 and a typical underground cell
  * is 8.
  */
class IsoCellGrid(name:AvatarName, screen:Screen) 
	extends IsoAvatar(name, screen) {

	var space = new IsoCellGridSpace(this)

	var renderer = new IsoCellGridRender(this)

	def consumeEvent(event:AvatarEvent):Boolean = {
		//println("%s ignore event %s".format(name, event))
		false
	}
}