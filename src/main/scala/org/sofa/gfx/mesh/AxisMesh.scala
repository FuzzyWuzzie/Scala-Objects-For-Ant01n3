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
    
    protected val V:MeshAttribute = allocateVertices

    protected val C:MeshAttribute = allocateColors

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

    protected def allocateVertices:MeshAttribute = {
    	val v = addMeshAttribute(VertexAttribute.Vertex, 3)
        val s = side / 2f
        
        v.set(0,  0,  0,  0)			// X+ 0
        v.set(1,  s,  0,  0)			// X+

        v.set(2,  0,  0,  0)			// X- 0
        v.set(3, -s,  0,  0)			// X-

        v.set(4,  0,  0,  0)			// Y+ 0
        v.set(5,  0,  s,  0)			// Y+

        v.set(6,  0,  0,  0)			// Y- 0
        v.set(7,  0, -s,  0)			// Y-

        v.set(8,  0,  0,  0)			// Z+ 0
        v.set(9,  0,  0,  s)			// Z+

        v.set(10,  0,  0,  0)			// Z- 0
        v.set(11,  0,  0, -s)			// Z-

        v
    }

    protected def allocateColors:MeshAttribute = {
    	val c = addMeshAttribute(VertexAttribute.Color, 4)

    	c.set(0, 1, 0, 0, 1)
    	c.set(1, 1, 0, 0, 1)
    		
    	c.set(2, 0.5f, 0, 0, 1)
    	c.set(3, 0.5f, 0, 0, 1)
    		
    	c.set(4, 0, 1, 0, 1)
    	c.set(5, 0, 1, 0, 1)
    		
    	c.set(6, 0, 0.5f, 0, 1)
    	c.set(7, 0, 0.5f, 0, 1)
    		
    	c.set(8, 0, 0, 1, 1)
    	c.set(9, 0, 0, 1, 1)
    		
    	c.set(10, 0, 0, 0.5f, 1)
    	c.set(11, 0, 0, 0.5f, 1)

    	c
    }
}