package org.sofa.opengl.mesh

import javax.media.opengl._
import org.sofa.nio._
import org.sofa.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._
import scala.math._
import org.sofa.math.{Point3, Rgba}

class LineSet(val count:Int) extends Mesh {
    protected lazy val V:FloatBuffer = allocateVertices
    
    protected def allocateVertices():FloatBuffer = FloatBuffer(count * 3 * 2)

    protected var vbeg = 0

    protected var vend = count

    protected var vertexArray:VertexArray = null
    
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
    
    def vertices:FloatBuffer = V
    
//    def newVertexArray(gl:SGL):VertexArray = new VertexArray(gl, (0, 3, vertices))
    
    def drawAs():Int = GL_LINES

   override def newVertexArray(gl:SGL, locations:Tuple6[Int,Int,Int,Int,Int,Int]):VertexArray = {
        vbeg = count; vend = 0
        vertexArray = super.newVertexArray(gl, locations)
        vertexArray
    }

    override def newVertexArray(gl:SGL, drawMode:Int, locations:Tuple6[Int,Int,Int,Int,Int,Int]):VertexArray = {
        vbeg = count; vend = 0
        vertexArray = super.newVertexArray(gl, drawMode, locations)
        vertexArray
    }
    
    override def newVertexArray(gl:SGL, drawMode:Int, locations:Tuple2[String,Int]*):VertexArray = {
        vbeg = count; vend = 0
        vertexArray = super.newVertexArray(gl, drawMode, locations:_*)
        vertexArray
    }
    
    override def newVertexArray(gl:SGL, locations:Tuple2[String,Int]*):VertexArray = {
        vbeg = count; vend = 0
        vertexArray = super.newVertexArray(gl, locations:_*)
        vertexArray
    }
     
    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
      * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL, verticesName:String) {
        if(vertexArray ne null) {
            if(vend > vbeg) {
                if(vbeg == 0 && vend == count)
                     vertexArray.buffer(verticesName).update(vertices)
                else vertexArray.buffer(verticesName).update(vbeg*2, vend*2, vertices)
                
                vbeg = count
                vend = 0
            }
            
            // if(cend > cbeg) {
            //     if(cbeg == 0 && cend == size)
            //          vertexArray.buffer(colorsName).update(colors)
            //     else vertexArray.buffer(colorsName).update(cbeg*2, cend*2, colors)
                
            //     cbeg = count
            //     cend = 0                
            // }
        }
    }
}

class ColoredLineSet(count:Int) extends LineSet(count) with ColorableMesh {
    protected lazy val C:FloatBuffer = allocateColors

    protected def allocateColors():FloatBuffer = FloatBuffer(count * 4 * 2)

    protected var cbeg = 0

    protected var cend = count

    override def colors:FloatBuffer = C

    override def hasColors = true

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

    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
      * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL, verticesName:String, colorsName:String) {
        super.updateVertexArray(gl, verticesName)
        if(vertexArray ne null) {
            if(cend > cbeg) {
                if(cbeg == 0 && cend == count)
                     vertexArray.buffer(colorsName).update(colors)
                else vertexArray.buffer(colorsName).update(cbeg*2, cend*2, colors)
                
                cbeg = count
                cend = 0                
            }
        }
    }
}