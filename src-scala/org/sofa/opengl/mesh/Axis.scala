package org.sofa.opengl.mesh

import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.math.Rgba
import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._

class Axis(val side:Float)
	extends Mesh with ColorableMesh {
    
    protected lazy val V:FloatBuffer = allocateVertices

    protected lazy val C:FloatBuffer = allocateColors

    def vertices:FloatBuffer = V

    override def colors:FloatBuffer = C
        
    override def hasColors = true

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

    def drawAs:Int = GL_LINES    
}