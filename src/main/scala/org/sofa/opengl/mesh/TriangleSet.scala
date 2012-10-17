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
import org.sofa.math.Rgba
import org.sofa.math.Triangle
import org.sofa.math.Vector3

class TriangleSet(count:Int) extends Mesh {
	protected lazy val V:FloatBuffer = allocateVertices

    protected def allocateVertices():FloatBuffer = FloatBuffer(count * 3 * 3)
	
    def setTriangle(i:Int, t:Triangle) { setTriangle(i, t.p0, t.p1, t.p2) }
    
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

//    def newVertexArray(gl:SGL):VertexArray = new VertexArray(gl, (0, 3, vertices))
    
    def drawAs():Int = GL_TRIANGLES
}

class ColoredTriangleSet(count:Int) extends TriangleSet(count) with ColorableMesh {
    protected lazy val C:FloatBuffer = FloatBuffer(count * 3 * 4)
    
    override def hasColors = true
    
    override def colors:FloatBuffer = C
    
    def setColor(i:Int, c:Rgba) { setColor(i, c.red.toFloat, c.green.toFloat, c.blue.toFloat, c.alpha.toFloat) }
    
    def setColor(i:Int, a:Rgba, b:Rgba, c:Rgba) {
    	setColor(i, a.red.toFloat, a.green.toFloat, a.blue.toFloat, a.alpha.toFloat,
    			    b.red.toFloat, b.green.toFloat, b.blue.toFloat, b.alpha.toFloat,
    			    c.red.toFloat, c.green.toFloat, c.blue.toFloat, c.alpha.toFloat)
    }
    
    def setColor(i:Int, r:Float, g:Float, b:Float, a:Float) { setColor(i, r,g,b,a, r,g,b,a, r,g,b,a) }
    
    def setColor(i:Int, ra:Float, ga:Float, ba:Float, aa:Float,
                        rb:Float, gb:Float, bb:Float, ab:Float,
                        rc:Float, gc:Float, bc:Float, ac:Float) {
        val pos = i*3*4
        
        C(pos+0) = ra
        C(pos+1) = ga
        C(pos+2) = ba
        C(pos+3) = aa
        
        C(pos+4) = rb
        C(pos+5) = gb
        C(pos+6) = bb
        C(pos+7) = ab

        C(pos+8)  = rc
        C(pos+9)  = gc
        C(pos+10) = bc
        C(pos+11) = ac
    }
}

class ColoredSurfaceTriangleSet(count:Int) extends ColoredTriangleSet(count) with SurfaceMesh {
    protected lazy val N:FloatBuffer = FloatBuffer(count * 3 * 3)
    
    override def hasNormals = true
    
    override def normals:FloatBuffer = N
    
    def setNormal(i:Int, n:Vector3) { setNormal(i, n.x.toFloat, n.y.toFloat, n.z.toFloat) }
    
    def setNormal(i:Int, a:Vector3, b:Vector3, c:Vector3) {
    	setNormal(i, a.x.toFloat, a.y.toFloat, a.z.toFloat,
    			     b.x.toFloat, b.y.toFloat, b.z.toFloat,
    			     c.x.toFloat, c.y.toFloat, c.z.toFloat)
    }
    
    def setNormal(i:Int, x:Float, y:Float, z:Float) { setNormal(i, x,y,z, x,y,z, x,y,z) }
    
    def setNormal(i:Int, xa:Float, ya:Float, za:Float,
    		             xb:Float, yb:Float, zb:Float,
    		             xc:Float, yc:Float, zc:Float) {  
        val pos = i*3*3
        
        N(pos+0) = xa
        N(pos+1) = ya
        N(pos+2) = za
        
        N(pos+3) = xb
        N(pos+4) = yb
        N(pos+5) = zb

        N(pos+6) = xc
        N(pos+7) = yc
        N(pos+8) = zc
    }	
}