package org.sofa.opengl.mesh

import javax.media.opengl._
import org.sofa.nio._
import org.sofa.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._
import scala.math._
import org.sofa.math.Point3

class TriangleSet(count:Int) extends Mesh {
	protected lazy val V:FloatBuffer = allocateVertices

    protected def allocateVertices():FloatBuffer = FloatBuffer(count * 3 * 3)
	
	def setTriangle(i:Int, a:Point3, b:Point3, c:Point3) {
	    val pos = i*3*3
	    
	    V(pos+0) = a.x.toFloat
	    V(pos+1) = a.y.toFloat
	    V(pos+2) = a.z.toFloat

	    V(pos+3) = b.x.toFloat
	    V(pos+4) = b.y.toFloat
	    V(pos+5) = b.z.toFloat

	    V(pos+6) = c.x.toFloat
	    V(pos+7) = c.y.toFloat
	    V(pos+8) = c.z.toFloat
	} 
	
    def vertices:FloatBuffer = V

    def newVertexArray(gl:SGL):VertexArray = new VertexArray(gl, (0, 3, vertices))
    
    def drawAs():Int = GL_TRIANGLES
}

class ColoredTriangleSet(count:Int) extends TriangleSet(count) with ColorableMesh {
    protected lazy val C:FloatBuffer = FloatBuffer(count * 3 * 4)
    
    override def hasColors = true
    
    def colors:FloatBuffer = C
    
    def setColor(i:Int, ra:Float, ga:Float, ba:Float, aa:Float,
                        rb:Float, gb:Float, bb:Float, ab:Float,
                        rc:Float, gc:Float, bc:Float, ac:Float) {
        val pos = i*3*4
        
        C(pos+0) = ra
        C(pos+1) = ba
        C(pos+2) = ga
        C(pos+3) = aa
        
        C(pos+4) = rb
        C(pos+5) = bb
        C(pos+6) = gb
        C(pos+7) = ab

        C(pos+8)  = rc
        C(pos+9)  = bc
        C(pos+10) = gc
        C(pos+11) = ac
    }
    
    override def newVertexArray(gl:SGL):VertexArray = new VertexArray(gl, (0, 3, vertices), (1, 4, colors))
}
