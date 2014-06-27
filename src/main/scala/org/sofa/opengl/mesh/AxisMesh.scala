package org.sofa.opengl.mesh

import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.math.Rgba

/** Draw the X, Y and Z axis as lines with colors.
  *
  * The `side` paramter allows to tell the length of each axis in the negative and positive directions. */
class AxisMesh(val side:Float) extends Mesh {
    
    protected lazy val V:FloatBuffer = allocateVertices

    protected lazy val C:FloatBuffer = allocateColors

    def vertexCount:Int = 12

    override def attribute(name:String):FloatBuffer = {
    	VertexAttribute.withName(name) match { 
    		case VertexAttribute.Vertex => V
    		case VertexAttribute.Color  => C
    		case _                      => super.attribute(name) //throw new RuntimeException("mesh has no attribute %s".format(name))
    	}
    }

    override def attributeCount():Int = 2 + super.attributeCount

    override def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.Color.toString) ++ super.attributes

    override def components(name:String):Int = {
    	VertexAttribute.withName(name) match { 
    		case VertexAttribute.Vertex => 3
    		case VertexAttribute.Color  => 4
    		case _                      => super.components(name) //throw new RuntimeException("mesh has no attribute %s".format(name))
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
    	C(x+0) = color.red.toFloat
    	C(x+1) = color.green.toFloat
    	C(x+2) = color.blue.toFloat
    	C(x+3) = color.alpha.toFloat
    	C(y+0) = (color.red/2).toFloat
    	C(y+1) = (color.green/2).toFloat
    	C(y+2) = (color.blue/2).toFloat
    	C(y+3) = (color.alpha/2).toFloat
    }

    // -- Mesh building ---------------------------------------------------

    protected def allocateVertices:FloatBuffer = {
        val s = side / 2f
        
        FloatBuffer(
         0,  0,  0,			// X+ 0
         s,  0,  0,			// X+

         0,  0,  0,			// X- 0
        -s,  0,  0,			// X-

         0,  0,  0,			// Y+ 0
         0,  s,  0,			// Y+

         0,  0,  0,			// Y- 0
         0, -s,  0,			// Y-

         0,  0,  0,			// Z+ 0
         0,  0,  s,			// Z+

         0,  0,  0,			// Z- 0
         0,  0, -s)			// Z-
    }

    protected def allocateColors:FloatBuffer = {
        FloatBuffer(
        	1, 0, 0, 1,
        	1, 0, 0, 1,
        		
        	0.5f, 0, 0, 1,
        	0.5f, 0, 0, 1,
        		
        	0, 1, 0, 1,
        	0, 1, 0, 1,
        		
        	0, 0.5f, 0, 1,
        	0, 0.5f, 0, 1,
        		
        	0, 0, 1, 1,
        	0, 0, 1, 1,
        		
        	0, 0, 0.5f, 1,
        	0, 0, 0.5f, 1)
    }
}