package org.sofa.gfx.mesh.shapes


import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.gfx.mesh._
import org.sofa.math.{Point3, Rgba, Vector3}
import scala.math._


object HexaTilesMesh {
	def apply(
		gl:SGL,
		width:Int,
	    height:Int,
		ratio:Float            = 1f,
		perspectiveRatio:Float = 1f,
		textureWidth:Int       = 1,
		textureHeight:Int      = 1
			):HexaTilesMesh = 
				new HexaTilesMesh(gl, width, height, ratio, perspectiveRatio, textureWidth, textureHeight)
}


/** A a set of hexagonal tiles arranged on a 2D grid. 
  *
  * A mesh of independant hexagonal tiles representing a tesselation, arranged as
  * a 2D grid. The grid is axonometric and isometric. Each point not on the border
  * of the tile grid divides in three edges. Each of these edge is 120°
  * of each other.
  *
  * By default the tile edges are all of length 1 unit. This means that
  * a tile is 2 units in height and sqrt(3) in width.
  *
  * Althought the tiles are not aligned vertically, we map the grid
  * on a 2D square grid for indicing. The above-at-right cell of a cell is
  * considered above vertically in the square grid. 
  *
  * The first cell is at (0,0) and each cell is indexed in 2D grid as usual.
  * The center of this first cell is also at (0,0) in the user space. This
  * allows to easily find cells centers.
  *
  * Some ratios allow to change the tiles while preserving their isometric
  * properties. The ratio parameter allows to grow the tiles.
  *
  * To give an illusion of perspective one can squeeze the
  * height of the vertical segments of tiles. This is the perspective ratio.
  * Only the vertical segments are changed, to preserve the angles.
  *
  * Each tile owns its points, tiles do not share points. This means that they
  * have independant vertex attributes, like texture, etc.
  *
  * Texture coordinates consider by default that each tile has the same texture
  * occupying a rectangle of 1 unit height and sqrt(3)/2 units in length, inside
  * a square of side 1. You can give the considered texture a size in number of
  * tiles allong the U and V coordinates to create distinct tile textures.
  */
class HexaTilesMesh(
		val gl:SGL,
		val width            :Int,			// Number of tiles along X
		val height           :Int,			// Number of tiles along Y
		val ratio            :Float = 1f,
		val perspectiveRatio :Float = 1f,
		val textureWidth     :Int   = 1,
		val textureHeight    :Int   = 1
	) extends Mesh {

	protected val vcount = height * width * 6

	protected val icount = height * width * 12

	/** The mutable set of coordinates. */
    protected var V:MeshAttribute = addAttributeVertex

	/** The mutable set of colors. */
    protected var T:MeshAttribute = addAttributeTexCoord

	/** The mutable set of elements to draw.
	  * A note on why we use indexing. It is better not to use it when points are
	  * never shared, a set of distinct triangles for example. But here, although
	  * the tiles are independant, a tile is made of several triangles, and inside
	  * this tile, the triangles share points. */
    protected var I:MeshElement = allocateIndices
	
    def tileHeight = 2.0

    def tileWidth = scala.math.sqrt(3)

    def tileOffsetX = tileWidth

    def tileOffsetY = 2.0 * (3.0/4.0)

    def vertexCount = vcount

    def elementsPerPrimitive:Int = 3

    // -- Mesh creation ------------------------------------------------

    protected def setPoint(p:Int, x:Float, y:Float, data:FloatBuffer) {
    	data(p*3+0) = x
    	data(p*3+1) = y
    	data(p*3+2) = 0f
    }

    protected def setTexCoord(p:Int, u:Float, v:Float, data:FloatBuffer) {
    	data(p*2+0) = u
    	data(p*2+1) = v
    }

    protected def setTriangle(t:Int, p0:Int, p1:Int, p2:Int, data:IntBuffer) {
    	data(t)   = p0
    	data(t+1) = p1
    	data(t+2) = p2    	
    }

    protected def setHexagonPoints(p0:Int, xc:Float, yc:Float, xunit:Float, yunit:Float, data:FloatBuffer) {
    	val y4 = ((yunit / 4f) * perspectiveRatio)
    	val y2 = (y4 + (yunit / 4f))
    	val x2 = xunit / 2f

   		setPoint(p0,   xc,    yc-y2, data)
   		setPoint(p0+1, xc+x2, yc-y4, data)
    	setPoint(p0+2, xc+x2, yc+y4, data)
    	setPoint(p0+3, xc,    yc+y2, data)
    	setPoint(p0+4, xc-x2, yc+y4, data)
    	setPoint(p0+5, xc-x2, yc-y4, data)
    }

    protected def setHexagonTexCoords(p0:Int, x:Float, y:Float, data:FloatBuffer) {
    	val xunit = sqrt(3).toFloat / 4f
    	val yunit = 1f

    	setTexCoord(p0,   xunit,    0,          data)
    	setTexCoord(p0+1, xunit*2f, yunit/4f,   data)
    	setTexCoord(p0+2, xunit*2f, yunit/4f*3, data)
    	setTexCoord(p0+3, xunit,    yunit,      data)
    	setTexCoord(p0+4, 0,        yunit/4f*3, data)
    	setTexCoord(p0+5, 0,        yunit/4f,   data)
    }

    protected def setHexagonTriangles(t:Int, p0:Int, data:IntBuffer) {
    	setTriangle(t,   p0,   p0+1, p0+5, data)
    	setTriangle(t+3, p0+1, p0+4, p0+5, data)
    	setTriangle(t+6, p0+1, p0+2, p0+4, data)
    	setTriangle(t+9, p0+2, p0+3, p0+4, data)
    }

    protected def addAttributeVertex():MeshAttribute = {
    	if(V eq null) {
    		V = addMeshAttribute(VertexAttribute.Position, 3)

    		V.begin

	    	// Tiles are allocated along a row first, then rows are
	    	// stacked one on another. This means that the first tile (0,0)
	    	// is the lower left one.

	    	var x     = 0
	    	var y     = 0
	    	var yy    = 0f
	    	val xunit = sqrt(3).toFloat * ratio
	    	val yunit = ratio * 2f
	    	val data  = V.data //FloatBuffer(width * height * 6 * 3)	// six points of 3 coordinates per cell, w x h cells.
	    	var vend  = 0

	    	while(y < height) {
	    		x = 0

	    		var xx = if(y%2==0) 0f else xunit/2f

	    		while(x < width) {
	    			setHexagonPoints(vend, xx, yy, xunit, yunit, data)

	    			vend += 6
	    			xx   += xunit
	    			x    += 1
	    		}

	    		yy += ((yunit/2f) * perspectiveRatio) + (yunit/4f)
	    		y  += 1
	    	}

    		V.range(0, vertexCount)
    		V.end
    	}

    	V
    }

    protected def addAttributeTexCoord():MeshAttribute = {
    	if(T eq null) {
    		T = addMeshAttribute(VertexAttribute.TexCoord, 2)

    		T.begin
	
	       	var x     = 0
	    	var y     = 0
	    	var tend  = 0
	    	val data  = T.data //FloatBuffer(width * height * 6 * 2)	// six points of 2 coordinates per cell, w x h cells.

	    	while(y < height) {
	    		x = 0

	    		while(x < width) {
	    			setHexagonTexCoords(tend, x, y, data)

	    			tend += 6
	    			x    += 1
	    		}

	    		y  += 1
	    	}

			T.range(0, vcount)
			T.end
	 	}

    	T
    }

    protected def allocateIndices():MeshElement = {
    	if(I eq null) {
	    	// We would need multidrawarrays to draw a lot of triangle fans/strips but
	    	// this does not exist in ES2.0. I do not know the difference in performance
	    	// between drawing a lot of hexatiles using triangle strip (6 indices) or
	    	// using a whole set of hexatiles made of several 4 triangles (12 indices).
	    	//
	    	// TODO make a test....
	    	//
	    	// Things to consider : we may have a lot of tiles to draw when unzooming,
	    	// however the detail level will decrease. Most of the time we draw only
	    	// few tiles. Furthermore, when zoomed in, instead of drawing the whole tiles
	    	// mesh, and letting OpenGL decide which part is visible or not by testing each
	    	// part, we decide to draw only the visible tiles. It may be far more efficient
	    	// ...

	    	I = addMeshElement(width * height * 4, 3)
	    	I.begin

//	    	val data = IntBuffer(width * height * 12)	// each cell is 4 triangles, so 12 points.
			val data = I.data

	    	var i = 0
	    	var p = 0
	    	var n = width * height
	    	var iend = 0

	    	while(i < n) {
	    		setHexagonTriangles(iend, p, data)

	    		iend += 12
	    		i    += 1
	    		p    += 6
	    	}

	    	I.range(0, width * height * 4)
	    	I.end
	    }

	    I
    }

    // -- Mesh Interface -----------------------------------------------
            
    def drawAs():Int = gl.TRIANGLES

    // -- Edition -----------------------------------------------------

    /** Change the texture of a given tile.
      *
      * The tile and the texture are indicated by their coordinates in the 2D grid.
      */
	def setTileTexture(x:Int, y:Int, tx:Int, ty:Int) { 
		throw new RuntimeException("TODO setTileTexture")
	}

    // -- Dynamic mesh --------------------------------------------------

 //    override def beforeNewVertexArray() {
	// 	vbeg = vcount; vend = 0; tbeg = vcount; tend = 0; ibeg = icount; iend = 0;
	// }

 //    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
 //      * avoid moving data between the CPU and GPU. */
 //    def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true) }
     
 //    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
 //      * avoid moving data between the CPU and GPU. */
 //    def updateVertexArray(gl:SGL, updateVertices:Boolean, updateColors:Boolean) {
 //        if(va ne null) {
 //            if(vend > vbeg) {
 //                if(vbeg == 0 && vend == vcount)
 //                     va.buffer(VertexAttribute.Vertex.toString).update(V)
 //                else va.buffer(VertexAttribute.Vertex.toString).update(vbeg, vend, V)
                
 //                vbeg = vcount
 //                vend = 0
 //            }
 //            if(tend > tbeg) {
 //                if(tbeg == 0 && tend == vcount)
 //                     va.buffer(VertexAttribute.TexCoord.toString).update(T)
 //                else va.buffer(VertexAttribute.TexCoord.toString).update(tbeg, tend, T)
                
 //                tbeg = vcount
 //                tend = 0                
 //            }
	// 		if(iend > ibeg) {
	// 			if(ibeg == 0 && iend == icount)
	// 			     va.indices.update(I)
	// 			else va.indices.update(ibeg, iend, I)

	// 			ibeg = icount
	// 			iend = 0
 //        	}
 //    	}
 //    }
}