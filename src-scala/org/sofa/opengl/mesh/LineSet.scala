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

class LineSet(val count:Int) extends Mesh {
    protected lazy val V:FloatBuffer = allocateVertices
    
    protected def allocateVertices():FloatBuffer = FloatBuffer(count * 3 * 2)
    
    def setLine(i:Int, a:Point3, b:Point3) {
        val pos = i*3*2
        
        V(pos+0) = a.x.toFloat
        V(pos+1) = a.y.toFloat
        V(pos+2) = a.z.toFloat
        V(pos+3) = b.x.toFloat
        V(pos+4) = b.y.toFloat
        V(pos+5) = b.z.toFloat
    }
    
    def vertices:FloatBuffer = V
    
    def newVertexArray(gl:SGL):VertexArray = new VertexArray(gl, (0, 3, vertices))
    
    def drawAs():Int = GL_LINES
}
