package org.sofa.gfx.mesh.shapes

import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.gfx.mesh._
import org.sofa.math.Rgba


/** Draw the X, Y and Z axis as lines with colors.
  *
  * The `side` paramter allows to tell the length of each axis in the negative
  * and positive directions. The axis is centered at zero. By default the X
  * axis is red, the y axis is green, and the z axis is blue. Negative axis
  * parts are darker. */
class AxisMesh(val gl:SGL, val side:Float) extends Mesh {
    
    protected var V:MeshAttribute = addAttributeVertex

    protected var C:MeshAttribute = addAttributeColor

    def vertexCount:Int = 12

    def elementsPerPrimitive:Int = 6

    def drawAs():Int = gl.LINES    

    // -- Mesh parameters ---------------------------------------------------

    override def begin(name:String*) {
    	import VertexAttribute._
    	begin(Color)
    }

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
    	val c = C.data

    	c(x+0) = color.red.toFloat
    	c(x+1) = color.green.toFloat
    	c(x+2) = color.blue.toFloat
    	c(x+3) = color.alpha.toFloat
    	c(y+0) = (color.red/2).toFloat
    	c(y+1) = (color.green/2).toFloat
    	c(y+2) = (color.blue/2).toFloat
    	c(y+3) = (color.alpha/2).toFloat

    	C.range(i, i+1)
    }

    // -- Mesh building ---------------------------------------------------

    protected def addAttributeVertex:MeshAttribute = {
    	if(V eq null) {
	    	V = addMeshAttribute(VertexAttribute.Position, 3)
	        
	        V.begin

	        val s = side / 2f
	        
	        V.set3(0,  0,  0,  0)			// X+ 0
	        V.set3(1,  s,  0,  0)			// X+

	        V.set3(2,  0,  0,  0)			// X- 0
	        V.set3(3, -s,  0,  0)			// X-

	        V.set3(4,  0,  0,  0)			// Y+ 0
	        V.set3(5,  0,  s,  0)			// Y+

	        V.set3(6,  0,  0,  0)			// Y- 0
	        V.set3(7,  0, -s,  0)			// Y-

	        V.set3(8,  0,  0,  0)			// Z+ 0
	        V.set3(9,  0,  0,  s)			// Z+

	        V.set3(10,  0,  0,  0)			// Z- 0
	        V.set3(11,  0,  0, -s)			// Z-

	        V.end
	    }

        V
    }

    protected def addAttributeColor:MeshAttribute = {
    	if(C eq null) {
	    	C = addMeshAttribute(VertexAttribute.Color, 4)

	    	C.begin

	    	C.set4(0, 1, 0, 0, 1)
	    	C.set4(1, 1, 0, 0, 1)
	    		
	    	C.set4(2, 0.5f, 0, 0, 1)
	    	C.set4(3, 0.5f, 0, 0, 1)
	    		
	    	C.set4(4, 0, 1, 0, 1)
	    	C.set4(5, 0, 1, 0, 1)
	    		
	    	C.set4(6, 0, 0.5f, 0, 1)
	    	C.set4(7, 0, 0.5f, 0, 1)
	    		
	    	C.set4(8, 0, 0, 1, 1)
	    	C.set4(9, 0, 0, 1, 1)
	    		
	    	C.set4(10, 0, 0, 0.5f, 1)
	    	C.set4(11, 0, 0, 0.5f, 1)

	    	C.end
    	}
    	C
    }
}