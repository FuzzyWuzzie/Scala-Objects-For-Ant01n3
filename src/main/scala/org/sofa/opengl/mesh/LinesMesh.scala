package org.sofa.opengl.mesh

import javax.media.opengl._
import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.math.{Point3, Rgba, Vector3}
import GL._
import GL2._
import GL2ES2._
import GL3._
import scala.math._

class LinesMesh(val count:Int) extends Mesh {
    protected lazy val V:FloatBuffer = FloatBuffer(count * 3 * 2)

    protected lazy val C:FloatBuffer = FloatBuffer(count * 4 * 2)

    protected var cbeg = 0

    protected var cend = count

    protected var vbeg = 0

    protected var vend = count

    // -- Mesh Interface -----------------------------------------------
    
    def attribute(name:String):FloatBuffer = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => V
    		case VertexAttribute.Color  => C
    		case _                      => throw new RuntimeException("no %s attribute in this mesh".format(name))
    	}
    }

    def attributeCount():Int = 2

    def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.Color.toString)
        
    def components(name:String):Int = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => 3
    		case VertexAttribute.Color  => 4
    		case _                      => throw new RuntimeException("no %s attribute in this mesh".format(name))
    	}

    }

    def has(name:String):Boolean = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => true
    		case VertexAttribute.Color  => true
    		case _                      => false
    	}
    }

    def drawAs():Int = GL_LINES

    // -- Edition -----------------------------------------------------

	/** Set the i-th line as the line between points `a` and `b`. */    
    def setLine(i:Int, a:Point3, b:Point3) {
        val pos = i*3*2
        
        V(pos+0) = a.x.toFloat
        V(pos+1) = a.y.toFloat
        V(pos+2) = a.z.toFloat
        V(pos+3) = b.x.toFloat
        V(pos+4) = b.y.toFloat
        V(pos+5) = b.z.toFloat

        if(i < vbeg) vbeg = i
        if(i+1 > vend) vend = i+1
    }

    /** Set the i-th line as the line between points `p` and `p+v`. */
    def setLine(i:Int, p:Point3, v:Vector3) {
    	val pos = i*3*2

    	V(pos+0) = p.x.toFloat
    	V(pos+1) = p.y.toFloat
    	V(pos+2) = p.z.toFloat
    	V(pos+3) = (p.x+v.x).toFloat
    	V(pos+4) = (p.y+v.y).toFloat
    	V(pos+5) = (p.z+v.z).toFloat

        if(i < vbeg) vbeg = i
        if(i+1 > vend) vend = i+1
    }

    def setColor(i:Int, c:Rgba) { setColor(i, c, c) }

    def setColor(i:Int, c0:Rgba, c1:Rgba) {
        setColor(i, c0.red.toFloat, c0.green.toFloat, c0.blue.toFloat, c0.alpha.toFloat,
                    c1.red.toFloat, c1.green.toFloat, c1.blue.toFloat, c1.alpha.toFloat)
    }

    def setColor(i:Int, r:Float, g:Float, b:Float, a:Float) {
        setColor(i, r,g,b,a, r,g,b,a)
    }

    def setColor(i:Int, ra:Float, ga:Float, ba:Float, aa:Float,
                        rb:Float, gb:Float, bb:Float, ab:Float) {
        val pos = i*4*2

        C(pos+0) = ra
        C(pos+1) = ga
        C(pos+2) = ba
        C(pos+3) = aa

        C(pos+4) = rb
        C(pos+5) = gb
        C(pos+6) = bb
        C(pos+7) = ab

        if(i < cbeg) cbeg = i
        if(i+1 > cend) cend = i+1
    }

    // -- Dynamic mesh --------------------------------------------------

    override def beforeNewVertexArray() {
        vbeg = count; vend = 0
	}

    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
      * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true) }
     
    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
      * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL, updateVertices:Boolean, updateColors:Boolean) {
        if(va ne null) {
            if(vend > vbeg) {
                if(vbeg == 0 && vend == count)
                     va.buffer(VertexAttribute.Vertex.toString).update(V)
                else va.buffer(VertexAttribute.Vertex.toString).update(vbeg*2, vend*2, V)
                
                vbeg = count
                vend = 0
            }
            if(cend > cbeg) {
                if(cbeg == 0 && cend == count)
                     va.buffer(VertexAttribute.Color.toString).update(C)
                else va.buffer(VertexAttribute.Color.toString).update(cbeg*2, cend*2, C)
                
                cbeg = count
                cend = 0                
            }
        }
    }
}