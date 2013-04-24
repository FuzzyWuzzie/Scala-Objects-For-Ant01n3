package org.sofa.simu.oberon.renderer.sprite

import scala.collection.mutable.{HashMap}

import org.sofa.math.{Vector3, NumberSeq3, SpatialCube}
import org.sofa.opengl.{SGL, Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, VertexAttribute, QuadsMesh}

import org.sofa.simu.oberon.renderer.{Sprite,Screen,Avatar,AvatarIndex,AvatarState,AvatarIndex2D,NoSuchStateException}

/** TileSprite companion object defining the messages that can be received (change()). */
object TilesSprite {
	case class GridInit(width:Int, height:Int, resourceName:String) extends AvatarState

	class StateChangeAction extends AvatarState

	/** Declare a new state for the sprite, and associate it an image in the resources library. The tile is
	  * taken between coordinates (u0,v0) and (u1,v1), in [0..1]. */
	case class AddState(state:String, u0:Double, v0:Double, u1:Double, v1:Double) extends StateChangeAction

	case class ChangeState(st:TileState) extends StateChangeAction

	case class ChangeStates(xy:Seq[TileState]) extends StateChangeAction

	case class FillState(x0:Int, y0:Int, x1:Int, y1:Int, st:String) extends StateChangeAction

	case class TileState(x:Int, y:Int, state:String)

	/** A state of a tile sprite. A state describe the UV coordinates in the sprite texture for a kind of tile. */
	case class State(val id:String, val u0:Float, val v0:Float, val u1:Float, val v1:Float)

	/** A state index maps a state to a position in the state grid buffer. */
	case class StateIndex(var state:State, var index:Int)

	/** Grid of states. The grid lower-left cell lower-left point is at (0,0) (with size (1,1)) and
	  * the grid higher-right cell lower-left point is at (w,h) (with its higher-right point at (w+1,h+1)). 
	  * 
	  * The state grid both remembers the cell states and their positions, but also the manage
	  * a quads mesh that is updated with only the visible cells and their texture coordinates. */
	class StateGrid(var w:Int, var h:Int) {
		/** Grid to retrieve the tiles per coordinates. */
		protected var grid = new Array[StateIndex](w*h)

		/** Number of tiles. */
		protected var sz = 0

		/** Geometry to display the tiles. */
		protected var tilesMesh = new QuadsMesh(w*h)

		def init(gl:SGL, shader:ShaderProgram) {
			import VertexAttribute._
			// Init the vertex array with non initialized arrays, not important, we
			// draw only sz elements and sz starts at zero an grows with initialized tiles.
	        tilesMesh.newVertexArray(gl, shader, Vertex -> "position", TexCoord -> "texCoords")
		}

		/** Number of tiles in the grid (the grid starts empty with each cell with no state). */
		def size:Int = sz

		def resize(width:Int, height:Int) {
			throw new RuntimeException("TODO StateGrid.resize")
		} 

		/** True if the grid contains a tile at (x,y). */
		def has(x:Int, y:Int):Boolean = (grid((y*w)+x) ne null)

		/** Ensure the cell at (x,y) has the given state. If the cell already had a state, it is
		  * changed, else it is added. */
		def set(x:Int, y:Int, state:State) {
			if(x < 0 || x >= w) throw new RuntimeException("invalid grid tile x = %d".format(x))
			if(y < 0 || y >= h) throw new RuntimeException("invalid grid tile y = %d".format(y))

			val index = if(this(x,y) ne null) this(x,y).index else sz
			val p = index * 4

			tilesMesh.setPointTexCoord(p,   state.u0, state.v1)
			tilesMesh.setPointTexCoord(p+1, state.u1, state.v1)
			tilesMesh.setPointTexCoord(p+2, state.u1, state.v0)
			tilesMesh.setPointTexCoord(p+3, state.u0, state.v0)

			if(this(x,y) eq null) {
				tilesMesh.setPoint(p,   x,   y,   0)
				tilesMesh.setPoint(p+1, x+1, y,   0)
				tilesMesh.setPoint(p+2, x+1, y+1, 0)
				tilesMesh.setPoint(p+3, x,   y+1, 0)

				tilesMesh.setQuad(index, p, p+1, p+2, p+3)
				sz += 1
Console.err.println("adding tile(%d,%d) sz=%d index=%d (%f %f %f %f)".format(x,y,sz,index,state.u0,state.v0,state.u1,state.v1))
			}
else Console.err.println("changing tile(%d,%d), sz=%d index=%d (%f %f %f %f)".format(x,y,sz,index,state.u0,state.v0,state.u1,state.v1))
			
			grid((y*w)+x) = StateIndex(state, index)
		}

		/** Remove a state from the cell at (x,y), ensuring it will not be displayed. */
		def unset(x:Int, y:Int) {
			if(x < 0 || x >= w) throw new RuntimeException("invalid grid tile x = %d".format(x))
			if(y < 0 || y >= h) throw new RuntimeException("invalid grid tile y = %d".format(y))

			// If there are two or more cells and we do not remove the last index,
			// we merely swap the given cell (index) with the last one (index), and decrease the
			// sz by one.
			//
			// If this is the last index (with one or more cells) we only decrease the size.

			if(this(x,y) ne null) {
				if(this(x,y).index < sz-1) {
					val index = this(x,y).index
					val p     = index  * 4
					val last  = (sz-1) * 4

					tilesMesh.setPoint(p,   tilesMesh.getPoint(last))
					tilesMesh.setPoint(p+1, tilesMesh.getPoint(last+1))
					tilesMesh.setPoint(p+2, tilesMesh.getPoint(last+2))
					tilesMesh.setPoint(p+3, tilesMesh.getPoint(last+3))

					tilesMesh.setPointTexCoord(p,   tilesMesh.getPointTexCoords(last))
					tilesMesh.setPointTexCoord(p+1, tilesMesh.getPointTexCoords(last+1))
					tilesMesh.setPointTexCoord(p+2, tilesMesh.getPointTexCoords(last+2))
					tilesMesh.setPointTexCoord(p+3, tilesMesh.getPointTexCoords(last+3))

					sz -= 1
				} else if(sz > 0) {
					sz -= 1
				}
			}
		}

		/** Give the tile at (x,y). */
		def apply(x:Int, y:Int):StateIndex = grid((y*w)+x)

		/** Draw the vertex array of the quad mesh. */
		def draw(gl:SGL) {
			if(size > 0) {
				tilesMesh.updateVertexArray(gl, true, false, false, true)
				tilesMesh.lastVertexArray.draw(tilesMesh.drawAs, size * 4)
			}	
		}
	}
}

/** A sprite that displays one or more tiles in a grid. Each grid cell can have a distinct
  * tile type called a state.
  *
  * This sprite works by indexing tiles in a grid with a given maximum width and height. 
  * The grid is indexed at integer positions and each cell is (1,1) in size. You can rescale
  * this with the changeSize and changePosition methods. Each cell of the grid starts empty.
  * You then can add new tiles by setting the state of a tile or group of tiles. First you
  * have to declare all possible states (states can be added at any time). A state defines
  * a name that maps to a UV coordinates in the texture of the sprite. Then you change the
  * state of a cell by giving the name of a state and the position of the cell in integer
  * coordinates. */
class TilesSprite(name:String, screen:Screen, override val isIndexed:Boolean = false) extends Sprite(name, screen) {
	import TilesSprite._

	/** Shortcut to the GL. */
	protected val gl = screen.renderer.gl

	/** All the possible states. A state is the texture of a tile and a name for this texture. */
	protected val states = new HashMap[String,State]()

	/** Current state. */
	protected var state:StateGrid = null

	/** Shader for the tiles. */
	protected var tilesShader:ShaderProgram = null

	/** The spatial index anchor. */
	protected val idx:AvatarIndex = if(isIndexed) new AvatarIndex2D(this) else null

	/** Texture containing the tiles. */
	protected var texture:Texture = null

	override def index = idx

	/** True if there is actually a current state (that is the sprite is initialized). */
	def hasState:Boolean = (state ne null)

	override def begin() {
		super.begin
		tilesShader = screen.renderer.libraries.shaders.get(gl, "image-shader")
	}

	override def changePosition(newPos:NumberSeq3) {
		super.changePosition(newPos)
	}

	override def changeSize(newSize:NumberSeq3) {
		super.changeSize(newSize)
	}

	override def change(st:AvatarState) {
		st match {
			case GridInit(width, height, texResName) ⇒ {
				texture = screen.renderer.libraries.textures.get(gl, texResName)

				if(state ne null) {
					state.resize(width, height)
				} else {
					state = new StateGrid(width, height)
					state.init(gl, tilesShader)
				}
			}
			case AddState(state, u0, v0, u1, v1) ⇒ {				
				states += (state -> State(state, u0.toFloat, v0.toFloat, u1.toFloat, v1.toFloat))
			}
			case ChangeState(state) ⇒ { changeState(state) }
			case ChangeStates(states:Seq[TileState]) ⇒ { changeStates(states) }
			case FillState(x0, y0, x1, y1, state) ⇒ { fillState(x0,y0,x1,y1,state) }
			case _ ⇒ { 
				Console.err.println("unknown message %s, passing to parent".format(st))
				super.change(st) }
		}
	}

	protected def changeState(st:TileState) {
		if(st.state eq null) {
			state.unset(st.x, st.y)
		} else {
			val s = states.get(st.state).getOrElse(throw NoSuchStateException("avatar %s has no state %s".format(name, st.state)))
			state.set(st.x, st.y, s)
		}
	}

	protected def changeStates(st:Seq[TileState]) { st.foreach { changeState(_) } }

	protected def fillState(x0:Int, y0:Int, x1:Int, y1:Int, st:String) {
		if(st ne null) {
			val s = states.get(st).getOrElse(throw NoSuchStateException("avatar %S has no state %s".format(name, st)))

			var x = x0
			var y = y0

			while(y < y1) {
				while(x < x1) {
					state.set(x,y,s)
					x += 1
				}
				x  = x0
				y += 1
			}
		} else {
			var x = x0
			var y = y0

			while(y < y1) {
				while(x < x1) {
					state.unset(x,y)
					x += 1
				}
				x  = x0
				y += 1
			}
		}
	}

	def render() {
		val camera = screen.camera

		if(hasState) {
			gl.disable(gl.BLEND)
        	gl.frontFace(gl.CCW)

			tilesShader.use
// XXX ah ah ah ONLY one TEXTURE !!! XXX
			texture.bindUniform(gl.TEXTURE0, tilesShader, "texColor")
			camera.pushpop {
				camera.translateModel(pos.x, pos.y, pos.z)
				camera.scaleModel(size.x, size.y, size.z)
				camera.setUniformMVP(tilesShader)
				state.draw(gl)
				//tilesMesh.lastVertexArray.draw(tilesMesh.drawAs)
			}
			//gl.disable(gl.BLEND)
			gl.frontFace(gl.CW)
		}
	}

	def animate() {
		// if(hasState) {
		// 	state.nextPosition(0)
		// 	state.nextSize(0)
		// }
	}
}