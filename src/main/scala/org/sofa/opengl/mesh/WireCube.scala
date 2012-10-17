package org.sofa.opengl.mesh

import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.math.Rgba
import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._

/** A wire cube, centered around (0, 0, 0) whose `side` can be specified.
  * 
  * The vertex data must use indices. The cube is made of single lines and therefore must be
  * drawn using "line" mode. You can use colors with this mesh.
  */
class WireCube(val side:Float)
	extends Mesh with ColorableMesh with IndexedMesh {
    
    protected lazy val V:FloatBuffer = allocateVertices
    protected lazy val C:FloatBuffer = allocateColors
    protected lazy val I:IntBuffer = allocateIndices

    def vertices:FloatBuffer = V
    override def colors:FloatBuffer = C
    override def indices:IntBuffer = I
        
    override def hasColors = true

    override def hasIndices = true
    
    protected def allocateVertices:FloatBuffer = {
        val s = side / 2f
        
        FloatBuffer(
        // Front
        -s, -s,  s,			// 0
         s, -s,  s,			// 1
         s,  s,  s,			// 2
        -s,  s,  s,			// 3
        // Back
        -s, -s, -s,			// 4
         s, -s, -s,			// 5
         s,  s, -s,		 	// 6
        -s,  s, -s)			// 7
    }

    protected def allocateColors:FloatBuffer = {
        val n   = 8 * 4
        val buf = new FloatBuffer(n)
        
        for(i <- 0 until n) {
        	buf(i) = 1f
        }
        
        buf
    }
    
    /** Set the color of each line. */
    def setColor(color:Rgba) {
    	val n = 8 * 4
    	
    	for(i <- 0 until n by 4) {
    		C(i+0) = color.red.toFloat
    		C(i+1) = color.green.toFloat
    		C(i+2) = color.blue.toFloat
    		C(i+3) = color.alpha.toFloat
    	}
    }

    protected def allocateIndices:IntBuffer = {
        IntBuffer(
        // Front
        0, 1,
        1, 2,
        2, 3,
        3, 0,
        // Back
        4, 5,
        5, 6,
        6, 7,
        7, 4,
        // Sides
        0, 4,
        1, 5,
        2, 6,
        3, 7)
    }
    
    def drawAs:Int = GL_LINES    
}