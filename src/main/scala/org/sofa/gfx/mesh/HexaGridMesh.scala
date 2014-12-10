package org.sofa.gfx.mesh

import org.sofa.math.Rgba
import org.sofa.nio._
import org.sofa.gfx._
import scala.math._


object HexaGridMesh {
	def apply(
		width:Int,
	    height:Int,
		defaultColor:Rgba      = Rgba.White,
		ratio:Float            = 1f,
		perspectiveRatio:Float = 1f):HexaGridMesh = 
			new HexaGridMesh(width, height, defaultColor, ratio, perspectiveRatio)
}


/** A hexagonal 2D grid. 
  *
  * A mesh of lines representing a grid of hexagonal cells. The grid is
  * axonometric and isometric. Each point not on the border of the grid
  * divides in three edges. Each of these edge is 30° (Pi/6 radians) of
  * each other.
  *
  * By default the cell edges are all of length 1 unit. This means that
  * a cell is 2 units in height and sqrt(3) in width.
  *
  * Althought the grid cells are not aligned vertically, we map the grid
  * on a 2D square grid for indicing. The above-at-right cell of a cell is
  * considered above vertically. 
  *
  * The first cell is at (0,0) and each cell is indexed in 2D as usual.
  * The center of this first cell is also at (0,0) in the user space. This
  * allows to easily find cells centers.
  *
  * Some ratios allow to change the grid while preserving its isometric
  * properties. The ratio parameter allows to grow the the grid.
  *
  * To give an illusion of perspective one can squeeze the
  * height of the vertical segments of cells. This is the perspective ratio.
  *
  * This mesh only support color attributes.
  */
class HexaGridMesh(
		val width            :Int,
		val height           :Int,
		val defaultColor     :Rgba  = Rgba.White,
		val ratio            :Float = 1f,
		val perspectiveRatio :Float = 1f
	) extends Mesh {

	protected val vcount = (((height * 2) + 2) * (width + 1))

	protected val icount = ((((width * 2) + 1) * (height + 1)) + ((width + 1) * height)) * 2

	/** The mutable set of coordinates. */
    protected lazy val V = allocateVertices

	/** The mutable set of colors. */
    protected lazy val C = allocateColors

	/** The mutable set of elements to draw. */
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
    protected var cbeg = vcount
    
    /** End position of the last modification inside the color array. */
    protected var cend = 0

    // -- Mesh creation ------------------------------------------------

    def vertexCount:Int = vcount

    def elementsPerPrimitive:Int = 2

    protected def allocateVertices():FloatBuffer = {
    	// generate a set of points, organized first in rows (X) then
    	// in columns (Y).
    	val xunit = sqrt(3).toFloat * ratio 	// size of a cell along X.
    	val yunit = ratio * 2f 					// size of a cell along Y.
    	val cols  = width + 1					// Number of columns of points.
    	val rows  = (height * 2) + 2 			// Number of rows of points.
    	val data  = FloatBuffer(rows * cols * 3)

    	var row = 0  							// Current row.
    	var col = 0 							// Current column.
    	var x   = 0f 							// Current x position for current point.
    	var y   = -((yunit / 4f) + ((yunit / 4f) * perspectiveRatio.toFloat)) 					// Current y position for current point.

    	vbeg = 0
    	vend = 0

    	while(row < rows) {
    		val r4 = row % 4

    		if(r4 == 0 || r4 == 3) x = 0f else x = -xunit/2f

    		col = 0

    		while(col < cols) {
    			data(vend*3+0) = x
    			data(vend*3+1) = y
    			data(vend*3+2) = 0f

    			x    += xunit
    			col  += 1
    			vend += 1
    		}

    		y   += (if(r4 == 0 || r4 == 2) yunit/4f else ((yunit/2f)*perspectiveRatio.toFloat))
    		row += 1
    	}

		assert(vend == vcount)

    	data
    }

    protected def allocateColors():FloatBuffer = {
    	val cols = width + 1
    	val rows = (height * 2) + 2
    	val size = rows * cols
		val data = FloatBuffer(size * 4)

		cend = 0

		while(cend < size) {
			data(cend*4+0) = defaultColor.red.toFloat
			data(cend*4+1) = defaultColor.green.toFloat
			data(cend*4+2) = defaultColor.blue.toFloat
			data(cend*4+3) = defaultColor.alpha.toFloat

			cend += 1
		}

		assert(cend == vcount)

		data
    }

    protected def allocateIndices():IntBuffer = {
    	val diag  = width * 2 + 1
    	val diagc = height + 1
    	val vert  = width + 1
    	val vertc = height
    	val data  = IntBuffer((diag*diagc + vert*vertc) * 2)

    	// Allocate diagonals

    	var lines = height + 1
    	var line  = 0
    	var col   = 0
    	var pt    = 0

    	ibeg = 0
    	iend = 0

    	while(line < lines) {
    		col = 0
    		
    		while(col <= width) {
    			data(iend+0) = pt + width + 1
		    	data(iend+1) = pt

		    	if(line%2 == 0) {
		    		if(col < width) {
		    			iend += 2
		    			data(iend+0) = pt
		    			data(iend+1) = pt + width + 2
		    		}
		    	} else {
		    		if(col > 0) {
		    			iend += 2
		    			data(iend+0) = pt
		    			data(iend+1) = pt + width
		    		}
		    	}
			    
			    iend += 2

    			col  += 1
		    	pt   += 1
    		}

    		line += 1
    		pt   += width + 1
    	}

    	// Allocate verticals.

    	lines = height
    	line  = 0
    	pt    = width + 1

    	while(line < lines) {
    		col = 0

    		while(col <= width) {
    			data(iend+0) = pt
    			data(iend+1) = pt + width + 1

    			iend += 2
    			col  += 1
    			pt   += 1
    		}

    		line += 1
    		pt += width + 1
    	}

    	assert(iend == icount)

    	data
    }

    // -- Mesh Interface -----------------------------------------------
    
    override def attribute(name:String):FloatBuffer = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => V
    		case VertexAttribute.Color  => C
    		case _                      => super.attribute(name) //throw new RuntimeException("no %s attribute in this mesh".format(name))
    	}
    }

    override def attributeCount():Int = 2

    override def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.Color.toString) ++ super.attributes
        
	override def elements:IntBuffer = I
		
	override def hasElements = true

    override def components(name:String):Int = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => 3
    		case VertexAttribute.Color  => 4
    		case _                      => super.components(name) //throw new RuntimeException("no %s attribute in this mesh".format(name))
    	}

    }

    override def has(name:String):Boolean = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => true
    		case VertexAttribute.Color  => true
    		case _                      => super.has(name) //false
    	}
    }

    def drawAs(gl:SGL):Int = gl.LINES

    // -- Edition -----------------------------------------------------

    /** Change the color of all the points of a cell given by its X and Y coordinates.
      *
      * Be careful that changing a cell points colors will affect neightbor cells since
      * points are shared.
      */
	def setCellColor(x:Int, y:Int, c:Rgba) { setCellColor(x, y, c.red.toFloat, c.green.toFloat, c.blue.toFloat, c.alpha.toFloat) }

    /** Change the color of all the points of a cell given by its X and Y coordinates.
      *
      * Be careful that changing a cell points colors will affect neightbor cells since
      * points are shared.
      */
	def setCellColor(x:Int, y:Int, r:Float, g:Float, b:Float, a:Float) {
		throw new RuntimeException("TODO setCellColor")
	}

    // -- Dynamic mesh --------------------------------------------------

    override def beforeNewVertexArray() {
        vbeg = vcount; vend = 0; cbeg = vcount; cend = 0; ibeg = icount; iend = 0;
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
            if(cend > cbeg) {
                if(cbeg == 0 && cend == vcount)
                     va.buffer(VertexAttribute.Color.toString).update(C)
                else va.buffer(VertexAttribute.Color.toString).update(cbeg, cend, C)
                
                cbeg = vcount
                cend = 0                
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