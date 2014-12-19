package org.sofa.gfx.mesh

import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.math.Rgba


/** Draw the X, Y and Z axis as lines with colors.
  *
  * The `side` paramter allows to tell the length of each axis in the negative
  * and positive directions. The axis is centered at zero. By default the X
  * axis is red, the y axis is green, and the z axis is blue. Negative axis
  * parts are darker. */
class AxisMesh(val side:Float) extends Mesh {
    
    protected var V:MeshAttribute = addAttributeVertex

    protected var C:MeshAttribute = addAttributeColor

    def vertexCount:Int = 12

    def elementsPerPrimitive:Int = 6

    def drawAs(gl:SGL):Int = gl.LINES    

    // -- Mesh parameters ---------------------------------------------------
    
    /** Set the color of each axis line. The negative part of the axis will be the color divided by two. */
    def setColor(x:Rgba, y:Rgba, z:Rgba) {
    	setXColor(x)
    	setYColor(y)
    	setZColor(z)
    }
    
    /** Set the color of the X axis. The negative part of the axis will be the color divided by two. */
    def setXColor(x:Rgba) { setAxisColor(0, x) }

    /** Set the color of the Y axis. The negative part of the axis will be the color divided by two. */
    def setYColor(y:Rgba) { setAxisColor(1, y) }

    /** Set the color of the Z axis. The negative part of the axis will be the color divided by two. */
    def setZColor(z:Rgba) { setAxisColor(2, z) }
    
    protected def setAxisColor(i:Int, color:Rgba) {
    	val x = i * 8
    	val y = x + 4
    	val c = C.theData

    	c(x+0) = color.red.toFloat
    	c(x+1) = color.green.toFloat
    	c(x+2) = color.blue.toFloat
    	c(x+3) = color.alpha.toFloat
    	c(y+0) = (color.red/2).toFloat
    	c(y+1) = (color.green/2).toFloat
    	c(y+2) = (color.blue/2).toFloat
    	c(y+3) = (color.alpha/2).toFloat
    }

    // -- Mesh building ---------------------------------------------------

    protected def addAttributeVertex:MeshAttribute = {
    	if(V eq null) {
	    	V = addMeshAttribute(VertexAttribute.Vertex, 3)
	        
	        val s = side / 2f
	        
	        V.set(0,  0,  0,  0)			// X+ 0
	        V.set(1,  s,  0,  0)			// X+

	        V.set(2,  0,  0,  0)			// X- 0
	        V.set(3, -s,  0,  0)			// X-

	        V.set(4,  0,  0,  0)			// Y+ 0
	        V.set(5,  0,  s,  0)			// Y+

	        V.set(6,  0,  0,  0)			// Y- 0
	        V.set(7,  0, -s,  0)			// Y-

	        V.set(8,  0,  0,  0)			// Z+ 0
	        V.set(9,  0,  0,  s)			// Z+

	        V.set(10,  0,  0,  0)			// Z- 0
	        V.set(11,  0,  0, -s)			// Z-
	    }

        V
    }

    protected def addAttributeColor:MeshAttribute = {
    	if(C eq null) {
	    	C = addMeshAttribute(VertexAttribute.Color, 4)

	    	C.set(0, 1, 0, 0, 1)
	    	C.set(1, 1, 0, 0, 1)
	    		
	    	C.set(2, 0.5f, 0, 0, 1)
	    	C.set(3, 0.5f, 0, 0, 1)
	    		
	    	C.set(4, 0, 1, 0, 1)
	    	C.set(5, 0, 1, 0, 1)
	    		
	    	C.set(6, 0, 0.5f, 0, 1)
	    	C.set(7, 0, 0.5f, 0, 1)
	    		
	    	C.set(8, 0, 0, 1, 1)
	    	C.set(9, 0, 0, 1, 1)
	    		
	    	C.set(10, 0, 0, 0.5f, 1)
	    	C.set(11, 0, 0, 0.5f, 1)
    	}
    	C
    }
}