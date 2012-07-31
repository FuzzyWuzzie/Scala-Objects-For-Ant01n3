package org.sofa.opengl.mesh

import javax.media.opengl._
import org.sofa.nio._
import org.sofa.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._ 

import scala.math._

/** Create a circle centered at (0,0,0) with given `radius` and number of `sides` on the XZ plane.
  * 
  * The circle is made of lines. 
  * 
  * The data is usable to draw directly the vertices or to use indices. Vertices are given in
  * order, following the trigonometric direction. They must be drawn in "line loop" mode.
  */
class Circle(radius:Double, sides:Int) extends Mesh with IndexedMesh {
	protected lazy val V:FloatBuffer = allocateVertices
    protected lazy val I:IntBuffer = allocateIndices

    override def hasIndices = true
    
    protected def allocateVertices():FloatBuffer = {
	    var size = (sides) * 3
	    val buf = new FloatBuffer(size)
	    val kstep = (2*Pi) / sides
	    var k = 0.0
	    
	    for(i <- 0 until size by 3) {
	        buf(i)   = (cos(k) * radius).toFloat
	        buf(i+1) = 0f
	        buf(i+2) = (sin(k) * radius).toFloat
	        k += kstep
	    }
	    
	    buf
	}
	
	protected def allocateIndices():IntBuffer = {
	    val size = sides
	    val buf = IntBuffer(size)
	    
	    for(i <- 0 until size) {
	        buf(i) = i
	    }
	    
	    buf
	}
    
    def vertices:FloatBuffer = V
    
    override def indices:IntBuffer = I
    
    override def drawAs:Int = GL_LINE_LOOP
    
//    def newVertexArray(gl:SGL):VertexArray = new VertexArray(gl, indices, (0, 3, vertices))
}
