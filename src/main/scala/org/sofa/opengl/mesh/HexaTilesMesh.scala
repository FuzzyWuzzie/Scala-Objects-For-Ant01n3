package org.sofa.opengl.mesh


import javax.media.opengl._
import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.math.{Point3, Rgba, Vector3}
import GL._
import GL2._
import GL2ES2._
import GL3._
import scala.math._


object HexaTilesMesh {
	def apply(
		width:Int,
	    height:Int,
		ratio:Float            = 1f,
		perspectiveRatio:Float = 1.0f,
		textureWidth:Int       = 1,
		textureHeight:Int      = 1
			):HexaTilesMesh = 
				new HexaTilesMesh(width, height, ratio, perspectiveRatio, textureWidth, textureHeight)
}


/** A a set of hexagonal tiles arranged on a 2D grid. 
  *
  * A mesh of independant hexagonal tiles representing a tesselation, arranged as
  * a 2D grid. The grid is axonometric and isometric. Each point not on the border
  * of the tile grid divides in three edges. Each of these edge is 30° (Pi/6 radians)
  * of each other.
  *
  * By default the tile edges are all of length 1 unit. This means that
  * a tile is 2 units in height and sqrt(3) in width.
  *
  * Althought the tiles are not aligned vertically, we map the grid
  * on a 2D square grid for indicing. The above-at-right cell of a cell is
  * considered above vertically. 
  *
  * The first cell is at (0,0) and each cell is indexed in 2D as usual.
  * The center of this first cell is also at (0,0) in the user space. This
  * allows to easily find cells centers.
  *
  * Some ratios allow to change the tiles while preserving their isometric
  * properties. The ratio parameter allows to grow the the tiles.
  *
  * To give an illusion of perspective one can squeeze the
  * height of the vertical segments of tiles. This is the perspective ratio.
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
		val width            :Int,			// Number of tiles along X
		val height           :Int,			// Number of tiles along Y
		val ratio            :Float = 1f,
		val perspectiveRatio :Float = 1f,
		val textureWidth     :Int   = 1,
		val textureHeight    :Int   = 1
	) extends Mesh {

	protected val vcount = height * width * 6

	protected val icount = 0

	/** The mutable set of coordinates. */
    protected lazy val V = allocateVertices

	/** The mutable set of colors. */
    protected lazy val T = allocateTexCoords

	/** The mutable set of elements to draw.
	  * A note on why we use indexing. It is better not to use it when points are
	  * never shared, a set of distinct triangles for example. But here, although
	  * the tiles are independant, a tile is made of several triangles, and inside
	  * this tile, the triangles share points. */
    protected lazy val I = allocateIndices

    /** Start position of the last modification inside the index array. */
	protected var ibeg = icount
	
	/** End position of the last modification inside the index array. */
	protected var iend = 0
	
    /** Start position of the last modification inside the coordinates array. */
    protected var vbeg = vcount
    
    /** End position of the last modification inside the coordinates array. */
    protected var vend = 0
        
    /** Start position of the last modification inside the color array. */
    protected var tbeg = vcount
    
    /** End position of the last modification inside the color array. */
    protected var tend = 0

    // -- Mesh creation ------------------------------------------------

    protected def setPoint(p:Int, x:Float, y:Float, data:FloatBuffer=V) {
    	data(p*3+0) = x
    	data(p*3+1) = y
    	data(p*3+2) = 0f
    }

    protected def setTexCoord(p:Int, u:Float, v:Float, data:FloatBuffer=V) {
    	data(p*2+0) = u
    	data(p*2+1) = v
    }

    protected def setHexagonPoints(p0:Int, xc:Float, yc:Float, xunit:Float, yunit:Float, data:FloatBuffer=V) {
   		setPoint(p0,   xc,          yc-yunit,    data)
   		setPoint(p0+1, xc+xunit/2f, yc-yunit/2f, data)
    	setPoint(p0+2, xc+xunit/2f, yc+yunit/2f, data)
    	setPoint(p0+3, xc,          yc+yunit,    data)
    	setPoint(p0+4, xc-xunit/2f, yc+yunit/2f, data)
    	setPoint(p0+5, xc-xunit/2f, yc-yunit/2f, data)
    }

    protected def setHexagonTexCoords(p0:Int, x:Float, y:Float, data:FloatBuffer=T) {
    	val xunit = sqrt(3).toFloat / 2f
    	val yunit = 1f

    	setTexCoord(p0,   xunit,    0,          data)
    	setTexCoord(p0+1, xunit*2f, yunit/4f,   data)
    	setTexCoord(p0+2, xunit*2f, yunit/4f*3, data)
    	setTexCoord(p0+3, xunit,    yunit,      data)
    	setTexCoord(p0+4, 0,        yunit/4f*3, data)
    	setTexCoord(p0+5, 0,        yunit/4f,   data)
    }

    protected def allocateVertices():FloatBuffer = {
    	// Tiles are allocated along a row first, then rows are
    	// stacked one on another. This means that the first tile (0,0)
    	// is the lower left one.

    	var x     = 0
    	var y     = 0
    	var yy    = 0f
    	val xunit = sqrt(3).toFloat * ratio
    	val yunit = ratio * 2f
    	val data  = FloatBuffer(width * height * 6 * 3)	// six points of 3 coordinates per cell, w x h cells.

    	vend = 0

    	while(y < height) {
    		x = 0

    		while(x < width) {
    			var xx = if(y%2==0) 0f else xunit/2f

    			setHexagonPoints(vend, xx, yy, xunit, yunit, data)

    			vend += 6
    			x    += 1
    		}

    		yy += yunit
    		y  += 1
    	}

		assert(vend == vcount)

    	data
    }

    protected def allocateTexCoords():FloatBuffer = {
    	var x     = 0
    	var y     = 0
    	val data  = FloatBuffer(width * height * 6 * 2)	// six points of 2 coordinates per cell, w x h cells.

    	tend = 0

    	while(y < height) {
    		x = 0

    		while(x < width) {
    			setHexagonTexCoords(tend, x, y, data)

    			tend += 6
    			x    += 1
    		}

    		y  += 1
    	}

		assert(tend == vcount)

    	data    	
    }

    protected def allocateIndices():IntBuffer = {
    	// val diag  = width * 2 + 1
    	// val diagc = height + 1
    	// val vert  = width + 1
    	// val vertc = height
    	// val data  = IntBuffer((diag*diagc + vert*vertc) * 2)

    	// // Allocate diagonals

    	// var lines = height + 1
    	// var line  = 0
    	// var col   = 0
    	// var pt    = 0

    	// ibeg = 0
    	// iend = 0

    	// while(line < lines) {
    	// 	col = 0
    		
    	// 	while(col <= width) {
    	// 		data(iend+0) = pt + width + 1
		   //  	data(iend+1) = pt

		   //  	if(line%2 == 0) {
		   //  		if(col < width) {
		   //  			iend += 2
		   //  			data(iend+0) = pt
		   //  			data(iend+1) = pt + width + 2
		   //  		}
		   //  	} else {
		   //  		if(col > 0) {
		   //  			iend += 2
		   //  			data(iend+0) = pt
		   //  			data(iend+1) = pt + width
		   //  		}
		   //  	}
			    
			  //   iend += 2

    	// 		col  += 1
		   //  	pt   += 1
    	// 	}

    	// 	line += 1
    	// 	pt   += width + 1
    	// }

    	// // Allocate verticals.

    	// lines = height
    	// line  = 0
    	// pt    = width + 1

    	// while(line < lines) {
    	// 	col = 0

    	// 	while(col <= width) {
    	// 		data(iend+0) = pt
    	// 		data(iend+1) = pt + width + 1

    	// 		iend += 2
    	// 		col  += 1
    	// 		pt   += 1
    	// 	}

    	// 	line += 1
    	// 	pt += width + 1
    	// }

    	// assert(iend == icount)

    	// data
    	null
    }

    // -- Mesh Interface -----------------------------------------------
    
    def attribute(name:String):FloatBuffer = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => V
    		case VertexAttribute.TexCoord => T
    		case _                        => throw new RuntimeException("no %s attribute in this mesh".format(name))
    	}
    }

    def attributeCount():Int = 2

    def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.TexCoord.toString)
        
	override def indices:IntBuffer = I
		
	override def hasIndices = true

    def components(name:String):Int = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => 3
    		case VertexAttribute.TexCoord => 2
    		case _                        => throw new RuntimeException("no %s attribute in this mesh".format(name))
    	}

    }

    def has(name:String):Boolean = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => true
    		case VertexAttribute.TexCoord => true
    		case _                        => false
    	}
    }

    def drawAs():Int = GL_TRIANGLES

    // -- Edition -----------------------------------------------------

    /** Change the color of all the points of a cell given by its X and Y coordinates.
      *
      * Be careful that changing a cell points colors will affect neightbor cells since
      * points are shared.
      */
	def setTileTexture(x:Int, y:Int, tx:Int, ty:Int) { 
		throw new RuntimeException("TODO setTileTexture")
	}

    // -- Dynamic mesh --------------------------------------------------

    override def beforeNewVertexArray() {
		vbeg = vcount; vend = 0; tbeg = vcount; tend = 0; ibeg = icount; iend = 0;
	}

    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
      * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true) }
     
    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
      * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL, updateVertices:Boolean, updateColors:Boolean) {
        if(va ne null) {
            if(vend > vbeg) {
                if(vbeg == 0 && vend == vcount)
                     va.buffer(VertexAttribute.Vertex.toString).update(V)
                else va.buffer(VertexAttribute.Vertex.toString).update(vbeg, vend, V)
                
                vbeg = vcount
                vend = 0
            }
            if(tend > tbeg) {
                if(tbeg == 0 && tend == vcount)
                     va.buffer(VertexAttribute.TexCoord.toString).update(T)
                else va.buffer(VertexAttribute.TexCoord.toString).update(tbeg, tend, T)
                
                tbeg = vcount
                tend = 0                
            }
			if(iend > ibeg) {
				if(ibeg == 0 && iend == icount)
				     va.indices.update(I)
				else va.indices.update(ibeg, iend, I)

				ibeg = icount
				iend = 0
        	}
    	}
    }
}