// package org.sofa.opengl.actor.renderer.avatar.game

// import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Box3Sized}
// import org.sofa.opengl.{Texture, ShaderResource, ModelResource, TextureResource, TexParams}
// import org.sofa.opengl.actor.renderer.{Screen}
// import org.sofa.opengl.actor.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarRenderState, AvatarBaseStates}
// import org.sofa.opengl.actor.renderer.{AvatarEvent, AvatarSpatialEvent, AvatarMotionEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarKeyEvent, AvatarZoomEvent}
// import org.sofa.opengl.actor.renderer.{NoSuchAvatarException}

// import org.sofa.opengl.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
// import org.sofa.opengl.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh, HexaTilesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


// // == Renders ====================================================================


// /** Configure one cell of a grid.
//   *
//   * @param relief Offset of the whole cell in Y axis (game coordinates).
//   * @param texx index in X of a texture patch inside the texture config.
//   * @param texy index in Y of a texture patch inside the texture config.
//   */
// case class IsoCellGridRelief(relief:Float, texx:Int, texy:Int)


// /** Configure the shape of cells.
//   *
//   * Each cell is in fact represented by a rectangle whose height is variable
//   * and width is always Sqrt(3). The rectangle origin can be offset from its
//   * center. 
//   *
//   * @param height Height of the rectangle.
//   * @param offsetx X offset of the origin from the rectangle center.
//   * @param offsety Y offset of the origin from the rectangle center.
//   */
// case class IsoCellGridShape(height:Float, offsetx:Float, offsety:Float)


// /** Configure the shader and textures to use for the cell.
//   *
//   * @param shader Identifier of the shader to use in the library.
//   * @param color Identifier of the color texture in the library.
//   * @param mask Identifier of the mask texture in the library.
//   * @param w The width of a rectangle patch in the texture in UV coordinates.
//   * @param h The height of a rectangle patch in the texture in UV coordinates.
//   * @param x The set of u coordinates (in UV) for patches.
//   * @param y The set of v coordinates (in UV) for patches.
//   */
// case class IsoCellGridShade(shader:String, color:String, mask:String,
// 				w:Float, h:Float, x:Array[Float], y:Array[Float])


// /** Express how to create and render the iso-cell grid.
//   *
//   * @param shade The shader and textures to use.
//   * @param shape The shape of a single cell.
//   * @param relief A 2D array of [[IsoCellGridRelief]] that describes the elevation and texture to use for each cell, the
//   *               size of the array gives the number of cells.
//   */
// case class IsoCellGridConfig(shade:IsoCellGridShade, shape:IsoCellGridShape,
// 				relief:Array[Array[IsoCellGridRelief]]) 
// 					extends AvatarRenderState with AvatarSpaceState {}


// object IsoCellGrid {
// 	/** Shortcut to the value of sqrt(3). */
// 	final val Sqrt3 = math.sqrt(3).toFloat
// }


// class IsoCellGridRender(avatar:Avatar) extends IsoRender(avatar) with IsoRenderUtils {

// 	fillColor = Rgba.White

// 	lineColor = Rgba.Black

// 	protected[this] var mesh:TrianglesMesh = null

// 	protected[this] var color:Texture = null

// 	protected[this] var mask:Texture = null

// 	protected[this] var shader:ShaderProgram = null

// 	protected[this] var world:Avatar = null

// 	override def changeRender(state:AvatarRenderState) {
// 		import IsoCellGrid._
// 	 	state match {
// 	 		case IsoCellGridConfig(shade, shape, relief) => init(shade, shape, relief)
// 	 		case _ => super.changeRender(state)
// 	 	}
// 	}

// 	protected def init(shade:IsoCellGridShade, shape:IsoCellGridShape, relief:Array[Array[IsoCellGridRelief]]) {
// 		import VertexAttribute._
// 		import IsoCellGrid._

// 		val gl = self.screen.gl
// 		val gh = relief.length
// 		val gw = if(gh > 0) relief(0).length else 0

// 		if(gh > 0 || gw > 0) {
// 			if(mesh ne null) mesh.dispose
		
// 			mesh = new TrianglesMesh(2 * gw * gh)

// 			shader = screen.libraries.shaders.get(gl, shade.shader)
// 			color  = screen.libraries.textures.get(gl, shade.color)
// 			mask   = screen.libraries.textures.get(gl, shade.mask)
			
// 			// 2+    2+----+3
// 			//  |\     \   |
// 			//  | \     \ B|
// 			//  | A\     \ |
// 			//  |   \     \|
// 			// 0+----+1    +1

// 			var x  = 0  					// Center of cell X.
// 			var y  = 0 						// Center of cell Y.
// 			var p  = 0 						// Current point (4 per cell).
// 			var t  = 0 						// Current triangle (2 per cell).
// 			val uw = shade.w 				// Width in UV of a texture patch.
// 			val vh = shade.h 				// Height in UV of a texture patch.
// 			val w2 = Sqrt3 / 2				// Half-width of a cell plus offset x.
// 			val h2 = shape.height / 2		// Half-height of a cell plus offset y.

// 			while(y < gh) {
// 				x = 0
// 				while(x < gw) {
// 					val xx = shape.offsetx -(y * Sqrt3 * 0.5f) + (x * Sqrt3 * 0.5f)
// 					val yy = shape.offsety -(y * 0.5f) - (x * 0.5f) + relief(y)(x).relief
// 					val uu = shade.x(relief(y)(x).texx)
// 					val vv = shade.y(relief(y)(x).texy)

// 					mesh xyz(p+0, xx-w2, yy-h2, 0) uv(p+0, uu,    vv)
// 					mesh xyz(p+1, xx+w2, yy-h2, 0) uv(p+1, uu+uw, vv)
// 					mesh xyz(p+2, xx-w2, yy+h2, 0) uv(p+2, uu,    vv+vh)
// 					mesh xyz(p+3, xx+w2, yy+h2, 0) uv(p+3, uu+uw, vv+vh)
// 					mesh triangle(t+0, p+0, p+1, p+2)
// 					mesh triangle(t+1, p+1, p+3, p+2)

// 					p += 4
// 					t += 2
// 					x += 1
// 				}

// 				y += 1
// 			}

// 			mesh.newVertexArray(gl, shader, Vertex -> "position", TexCoord -> "texCoords")
	
// 			world = screen.avatar(AvatarName("root.world")).getOrElse(throw new RuntimeException("no world avatar ??"))
// 		}
// 	}

// 	override def render() {
// 		val gl    = self.screen.gl
// 		val space = self.space
// 		val text  = screen.textLayer

// 		space.pushSubSpace
		
// 			if(mesh ne null) {
// 				val lightDir = world.renderer.asInstanceOf[IsoWorldRender].lightDir

// 				screen.space.pushpop {
// 					gl.enable(gl.BLEND)
// 					gl.disable(gl.DEPTH_TEST)
// 					shader.use
// 					color.bindUniform(gl.TEXTURE0, shader, "texColor")
// 					mask.bindUniform(gl.TEXTURE1, shader, "texMask")
// 					shader.uniform("lightDir", lightDir)
// 					screen.space.uniformMVP(shader)
// 					mesh.draw(gl)
// 		   	    	gl.disable(gl.BLEND)
// 		    	}
// 		    }
// 		    //strokeAvatarBox

// 			self.renderSubs
		
// 		space.popSubSpace		
// 	}
// }


// // == Spaces =====================================================================


// /** Space for a cell.
//   * The cell does not resizes the space, it only translates to its position, therefore
//   * super and sub spaces are the same.
//   * The while game works in the world coordinates. */
// class IsoCellGridSpace(avatar:Avatar) extends IsoSpace(avatar) {

// 	var scale1cm = 1.0

// 	var fromSpace = new Box3Sized {
// 		pos.set(0, 0, 0)
// 		size.set(1, 1, 1)
// 	}

// 	var toSpace = fromSpace

// 	def thisSpace = fromSpace

// 	def subSpace = toSpace

// 	override def changeSpace(newState:AvatarSpaceState) {
// 		import IsoCellGrid._

// 		newState match {
// 			case AvatarBaseStates.Move(offset) => {
// 				println("Cannot move an iso cell, use MoveAt")
// 			}
// 			case AvatarBaseStates.MoveAt(position:Point3) => {
// 					fromSpace.pos.x = (position.y * Sqrt3 * fromSpace.size.x * 0.5) + (position.x * Sqrt3 * fromSpace.size.x * 0.5)
// 					fromSpace.pos.y = (position.y * 0.5 * fromSpace.size.y) - (position.x * 0.5 * fromSpace.size.y)
// 					fromSpace.pos.z = 0
// 			}
// 			case AvatarBaseStates.Resize(size) => {
// 				println("Cannot resize an iso cell")
// 			}
// 			case IsoCellGridConfig(shade, shape, relief) => {
// 				val h = relief.length
// 				val w = if(h > 0) relief(0).length else 0

// 				if(h > 0 && w > 0)
// 					fromSpace.size.set(w, h, 1)
// 				else println("A*******AH?????")

// 				println(s"w ${w} h ${h} size ${fromSpace.size}")
// 			}
// 			case _ => super.changeSpace(newState)
// 		}
// 	}

// 	override def animateSpace() {}

// 	override def pushSubSpace() {
//  		scale1cm  = self.parent.space.scale1cm		
// 		val space = self.screen.space

//  		space.push
//  		space.translate(fromSpace.pos.x, fromSpace.pos.y, fromSpace.pos.z)
// 	}

// 	override def popSubSpace() { self.screen.space.pop }
// }


// // == Avatars ====================================================================


// /** An IsoCell grid is a grid of tiles used to build a regular array of isometric
//   * cells.
//   *
//   * Each cell is 4*Sqrt(3) width and at least 4 in height. However most of the time
//   * cells are higher. A typical ground cell is 4.5 and a typical underground cell
//   * is 8.
//   */
// class IsoCellGrid(name:AvatarName, screen:Screen) 
// 	extends IsoAvatar(name, screen) {

// 	var space = new IsoCellGridSpace(this)

// 	var renderer = new IsoCellGridRender(this)

// 	def consumeEvent(event:AvatarEvent):Boolean = {
// 		//println("%s ignore event %s".format(name, event))
// 		false
// 	}
// }