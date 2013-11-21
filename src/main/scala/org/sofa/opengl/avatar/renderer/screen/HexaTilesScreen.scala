package org.sofa.opengl.avatar.renderer.screen

import scala.math._
import scala.collection.mutable.{HashMap, HashSet, ArrayBuffer}
import akka.actor.{ActorRef}

import org.sofa.math.{Rgba, Axes, AxisRange, Point2, Point3, Vector3, NumberSeq3}
import org.sofa.collection.{SpatialHash, SpatialObject, SpatialPoint, SpatialCube}
import org.sofa.opengl.{Camera, Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, LinesMesh, HexaGridMesh, HexaTilesMesh, HexaTileMesh, VertexAttribute}
import org.sofa.opengl.surface.{MotionEvent}
import org.sofa.opengl.avatar.renderer.{Screen, ScreenState, Renderer, NoSuchScreenStateException}


object HexaTilesScreen {
	case class AddTileLayer(z:Int, texture:String)
	case class AddEntityLayer(z:Int, texture:String)
	case class RemoveLayer(z:Int)
	case class AddLayerKind(z:Int, kind:String, x:Int, y:Int)
	case class AddTileToLayer(z:Int, x:Int, y:Int, kind:String)
	case class RemoveTileFromLayer(z:Int, x:Int, y:Int)
	case class AddEntityToLayer(z:Int, name:String, x:Double, y:Double, kind:String)
	case class RemoveEntityFromLayer(z:Int, name:String)
}


/** A screen made of a set of 2D tiles of hexagonal shape on an axonometric grid.
  *
  * Although the idea is to represent a pseudo perspective, it is in fact isometric,
  * and the screen is 2D aligned with the screen.
  *
  * The screen number of cells is given by the axes X and Y (user units). The Z axis
  * is not used. The screen auto-adapts to the vieable area ratio.
  * 
  * The tiles are hexagonal, with two vertical sides and four diagonal sides,
  * all have a fixed size :
  *
  * - The cell vertical side is 1 unit.
  * - The cell diagonal side is 2 units.
  * - The cell overall height is 3 units.
  * - The cell overall width is 2 * sqrt(3) units.
  *
  * The zoom of the view units are cells. A zoom of 1 (cannot go under) means we see an
  * entire cell.
  *
  * The view has a position, whose units are cells. When on this position, the cell is
  * at the center of the view.
  *
  * The cells are numbered on a 2D square grid. The cell above at right of another is considered
  * above in the grid. The cell above at left is above at left in the grid.
  *
  * In the grid, cells have only six possible neighbors (in a 2D square grid, height are possible).
  * This means that the 2D square grid cells do not consider the above-at-right and under-at-right
  * cells as neighbors.
  */
class HexaTilesScreen(name:String, renderer:Renderer) extends Screen(name, renderer) {
	import HexaTilesScreen._

	/** Color for parts not covered by the background image. */
	val clearColor = Rgba(0, 0, 0, 1)

	// == View ============================

	/** Number of tiles along X. */
	var w = 1.0

	/** Number of tiles along Y. */
	var h = 1.0

	/** A zoom of 1 means we see a cell entirely. The units are cells. */
	var zoom = 2.0

	/** A position of (0, 0) means the lowest-left cell in the 2D plane. The
	  * Units are cells. */
	var center = Point2(0, 0)

	// == Cells =============================

	// Represented directly as a Triangle mesh ?

	// == Debug ==============================

	var debug = true	

	// == Grid ===============================

	var gridShader:ShaderProgram = null
	
	var tilesShader:ShaderProgram = null

	var grid:HexaGridMesh = null

	var tiles:HexaTilesMesh = null

	var tilesTex:Texture = null

	// == Access ============================

	override def width:Double = w

	override def height:Double = h

	// == Avatar ============================

	override def begin() {
		gl.clearColor(clearColor)
		gl.clearDepth(1f)
	    
	    gl.disable(gl.DEPTH_TEST)
	    
//		gl.enable(gl.CULL_FACE)
//		gl.cullFace(gl.BACK)
        gl.frontFace(gl.CW)
        
        gl.disable(gl.BLEND)
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)

        beginShader
        beginTexture
        beginGeometry

		super.begin

        reshape
	}

	protected def beginShader() {
		gridShader  = renderer.libraries.shaders.get(gl, "plain-shader")
		tilesShader = renderer.libraries.shaders.get(gl, "image-shader")
	}

	protected def beginTexture() {
		tilesTex = renderer.libraries.textures.get(gl, "tiles-image")
	}

	protected def beginGeometry() {
        setGrid
        setTiles
	}

	def change(state:ScreenState) {
		state match {
			case _ ⇒ {
				throw NoSuchScreenStateException(state)
			}
		}
	}

	override def changeAxes(newAxes:Axes, spashUnit:Double) {
		super.changeAxes(newAxes, spashUnit)

		h = axes.y.length
		w = axes.x.length

		setGrid
		setTiles
		reshape
	}

  	protected def setGrid() {
		import VertexAttribute._
		if((grid ne null) && (grid.lastVertexArray ne null)) {
			grid.lastVertexArray.dispose
		}

		grid = HexaGridMesh(w.toInt, h.toInt, defaultColor=Rgba.Red, perspectiveRatio=0.5f)
		grid.newVertexArray(gl, gridShader, Vertex -> "position", Color -> "color")
	}

	protected def setTiles() {
		import VertexAttribute._
		if((tiles ne null) && (tiles.lastVertexArray ne null)) {
			tiles.lastVertexArray.dispose
		}

		tiles = HexaTilesMesh(w.toInt, h.toInt, perspectiveRatio=0.5f)
		tiles.newVertexArray(gl, tilesShader, Vertex -> "position", TexCoord -> "texCoords")
	}

	override def render() {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		super.render
		// // In order to handle all the transparencies we have
		// // to sort elements. Therefore, we can disable the
		// // depth test.
		// val sorted = avatars.toArray.sortWith(_._2.pos.z < _._2.pos.z)
		// sorted.foreach { _._2.render }
		renderBackground
	}

	override def reshape() {
		super.reshape
		val ratio = camera.viewportRatio
		val hh    = (2.0 * h * zoom) / 2
		val ww    = (sqrt(3) * w * zoom) / 2
println("ortho(%f - %f, %f - %f".format(-ww*ratio, ww*ratio, -hh, hh))
		camera.orthographic(-ww*ratio, ww*(ratio), -hh, hh, -1, 1)
	}

	override def animate() {
		super.animate
	}

	override def end() {
		super.end
	}

	/** Render the background image (if any). */
	protected def renderBackground() {
		// Origin is in the middle of the screen and of the image.
		if(debug) {
		}
		renderTiles
		renderGrid
	}

	protected def renderTiles() {
        gl.enable(gl.BLEND)
		tilesShader.use
		tilesTex.bindUniform(gl.TEXTURE0, tilesShader, "texColor")
		camera.uniformMVP(tilesShader)
		tiles.lastVertexArray.draw(tiles.drawAs)
        gl.disable(gl.BLEND)
	}

	/** Render a grid alligned with the spash. */
	protected def renderGrid() {
		gridShader.use
		camera.uniformMVP(gridShader)
		grid.lastVertexArray.draw(grid.drawAs)
	}
	
	/** Pass from pixels to game units. */
	override protected def positionPX2GU(x:Double, y:Double):(Double, Double) = {
		val ratio   = camera.viewportRatio
		val xfrom   = axes.x.from * ratio
		val xlength = ((axes.x.to * ratio) - xfrom)

		(xfrom       + (xlength       * (   x / camera.viewportPx(0))),
		 axes.y.from + (axes.y.length * (1-(y / camera.viewportPx(1)))))
	}
}


// TODO a list of layers.
//
// This is the underlying model of this screen. A hexa tile screen is a set
// of layers. Some layers contain dynamic movable objects in a continuous space.
// Some layers contains non movable static tiles in a discrete space of 2D positions.
// The user describes the number of layers, their types and Z order.
//
//		- A layer describe both movable or static elements.
//		- Layers are drawn in Z order.
//		- Specific layers are created for dynamic elements.
//		- Specific layers are created for tiles.
//		- A layer contains only elements of one kind. Elements of the
//		  same kind may have several representations inside the same texture.
//		  goal: allow to push graphic state (textures) only once for several
//		  tiles.
//		- Layers are sets of rectangular patches that divide the space in areas
//		  goal: to avoid redrawing the whole scene, but only visible patches.
//		- Each patch is made of a list of tiles or dynamic elements. The list
//		  is a chained list.
//		  goal: avoid to store a 2D array with a lot of holes.


/** A dynamic set of layers of tiles. */
class HexaLayers(val width:Int, val height:Int) {

	/** The set of layers in order. We can deal with an array
	  * since there will be very few addition and removal. */
	protected val layers = new ArrayBuffer[HexaLayer]()

	def apply(i:Int):HexaLayer = layers(i)

	def addTiles(z:Int, texture:Texture) { layers += new HexaTilesLayer(z, texture) }

	def addEntities() { /* TODO */ }

	def remove(layer:HexaLayer) {  layers -= layer }

	def +=(layer:HexaLayer) { layers += layer }

	def -=(layer:HexaLayer) { layers -= layer }

	def insert(i:Int, layer:HexaLayer) { layers.insert(i, layer) }

	def draw() { layers.foreach { layer => layer.draw } }

	def visibility(from:Point3, to:Point3) { layers.foreach { layer => layer.visibility(from, to) } }
}


trait HexaLayer {
	val z:Int 

	def draw()

	def visibility(from:Point3, to:Point3)
}


/** A layer of tiles. */
class HexaTilesLayer(
	val z:Int,
	val texture:Texture,
	val ratio:Float            = 1f,
	val perspectiveRatio:Float = 1f,
	val textureWidth:Int       = 1,
	val textureHeight:Int      = 1,
	val bucketSize:Int         = 10)
		extends HexaLayer {
	
	protected val hexaTile = HexaTileMesh(ratio, perspectiveRatio, textureWidth, textureHeight)

	protected val spacehash = new SpatialHash[SpatialObject,SpatialPoint,HexaTile](bucketSize)

	def add(x:Int, y:Int, texX:Int=0, texY:Int=0) {
		// Add a tile, update the current visible list of HexaTiles.
	}

	def remove(x:Int, y:Int) {
		// Remove a tile, update the current visible list of HexaTiles.
	}

	def draw() {
		// Draw the list of visible HexaTiles.
	}

	def visibility(from:Point3, to:Point3) {
		// Create a list of visible buckets.
		// From it, create a list of visible HexaTiles.
		// Better to create the list once and keep it, since
		// we will not always move the view and therefore we
		// will not often change the visible area.
	}

	class HexaTile(x:Int, y:Int) extends SpatialCube {
		val from:Point3 = Point3(0, 0, 0)

		val to:Point3 = Point3(0, 0, 0)

		protected def init(x:Int, y:Int) {
			val xunit  = sqrt(3) * ratio
			val yunit  = ratio * 2.0
    		val y2 = (((yunit / 4.0) * perspectiveRatio) + (yunit / 4.0))
    		val x2 = xunit / 2.0

			from.set(-x2, -y2, 0)
			to.set(x2, y2, 0)
		}

		init(x, y)
	}
}
