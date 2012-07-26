package org.sofa.opengl.mesh

import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.math.Rgba
import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._

/** A single plane of several quads (themselves made of two triangles) in the XZ plane, centered
  * around the (0, 0, 0) point.
  * 
  * The number of rows and columns of quads can be specified with `nVertX` and `nVertZ` and the
  * overall size of the plane can be given with `width` and `depth`.
  * 
  * The plane is made of triangles and must be drawn in this mode. Indices must be used. You can
  * use a normal, color, tangent, and tex-coords arrays with this mesh.
  * 
  * Z
  * ^ 
  * |
  * +--+--+  nVertX = 3
  * | /| /|  nVertZ = 3
  * |/���|/ |  The origin is at the center.
  * +--0--+
  * | /| /|
  * |/���|/ |
  * +--+--+-->X
  * 
  * Triangles are in CW order.
  */
class Plane(val nVertX:Int, val nVertZ:Int, val width:Int, val depth:Int)
	extends Mesh with ColorableMesh with TangentSurfaceMesh
		with IndexedMesh with TexturableMesh {
    
    protected lazy val V:FloatBuffer = allocateVertices
    protected lazy val C:FloatBuffer = allocateColors
    protected lazy val N:FloatBuffer = allocateNormals
    protected lazy val T:FloatBuffer = allocateTangents
    protected lazy val X:FloatBuffer = allocateTexCoords
    protected lazy val I:IntBuffer = allocateIndices

    def vertices:FloatBuffer = V
    def colors:FloatBuffer = C
    def normals:FloatBuffer = N
    def tangents:FloatBuffer = T
    def texCoords:FloatBuffer = X
    def indices:IntBuffer = I
    
    protected var textureRepeatS:Int = 4
    protected var textureRepeatT:Int = 4
    
    override def hasColors = true
    
    override def hasIndices = true
    
    override def hasNormals = true
    
    override def hasTangents = true
    
    override def hasTexCoords = true
    
    protected def allocateVertices:FloatBuffer = {
        val buf = new FloatBuffer(nVertX * nVertZ * 3)
        val nw  = width.toFloat / (nVertX-1).toFloat
        val nd  = depth.toFloat / (nVertZ-1).toFloat 
        var xx  = -width/2f
        var zz  = -depth/2f
        var i   = 0

        for(d <- 0 until nVertZ) {
        	for(x <- 0 until nVertX) {
        	    buf(i+0) = xx
        	    buf(i+1) = 0
        	    buf(i+2) = zz
        	    xx += nw
        	    i  += 3
        	}
        	xx  = -width/2f
        	zz += nd
        }
        
        buf
    }
    
    def setTextureRepeat(S:Int, T:Int) {
        textureRepeatS = S
        textureRepeatT = T
    }
    
    protected def allocateTexCoords:FloatBuffer = {
        val n   = nVertX * nVertZ * 2 
        val buf = new FloatBuffer(n)
        var nw  = textureRepeatS / (nVertX-1).toFloat
        var nd  = textureRepeatT / (nVertZ-1).toFloat
        var xx  = 0f
        var zz  = 0f
        var i   = 0
        
        for(d <- 0 until nVertZ) {
            for(x <- 0 until nVertX) {
                buf(i+0) = xx
                buf(i+1) = zz
                xx += nw
                i  += 2
            }
            xx  = 0
            zz += nd
        }
        
        buf
    }

    protected def allocateColors:FloatBuffer = {
        val n   = nVertX * nVertZ * 4
        val buf = new FloatBuffer(n)
        
        for(i <- 0 until n) {
        	buf(i) = 1f
        }
        
        buf
    }

    protected def allocateNormals:FloatBuffer = {
        val n   = nVertX * nVertZ * 3
        val buf = new FloatBuffer(n)
        
        for(i <- 0 until n by 3) {
        	buf(i+0) = 0f
        	buf(i+1) = 1f
        	buf(i+2) = 0f
        }
        
        buf
    }
    
    protected def allocateTangents:FloatBuffer = {
        val n   = nVertX * nVertZ * 3
        val buf = new FloatBuffer(n)
        
        for(i <- 0 until n by 3) {
           buf(i+0) = 1f
           buf(i+1) = 0f
           buf(i+2) = 0f
        }
            
        buf
    }

    protected def allocateIndices:IntBuffer = {
        val n   = ((nVertX-1) * (nVertZ-1))	// n Squares
        val buf = new IntBuffer(n*2*3)		// 2 triangles per square
        var i   = 0
        
        for(z <- 0 until nVertZ-1) {
        	for(x <- 0 until nVertX-1) {
        	    buf(i+0) = z*nVertX + x 
        	    buf(i+1) = z*nVertX + x + 1
        	    buf(i+2) = z*nVertX + x + nVertX + 1 
        	    buf(i+3) = z*nVertX + x 
        	    buf(i+4) = z*nVertX + x + nVertX + 1
        	    buf(i+5) = z*nVertX + x + nVertX 
        	    i += 6
        	}
        }

        buf
    }
    
    def setColor(color:Rgba) {
        val n     = nVertX * nVertZ * 4
        
        for(i <- 0 until n by 4) {
            colors(i+0) = color.red.toFloat
            colors(i+1) = color.green.toFloat
            colors(i+2) = color.blue.toFloat
            colors(i+3) = color.alpha.toFloat
        }
    }
    
    def newVertexArray(gl:SGL) = new VertexArray(gl, indices, (0, 3, vertices), (1, 4, colors),
    			(2, 3, normals), (3, 3, tangents), (4, 2, texCoords))
    
    def newVertexArray(gl:SGL, attributeIndices:Tuple5[Int,Int,Int,Int,Int]) = {
    	new VertexArray(gl, indices, (attributeIndices._1, 3, vertices),
    	                             (attributeIndices._2, 4, colors),
    	                             (attributeIndices._3, 3, normals),
    	                             (attributeIndices._4, 3, tangents),
    	                             (attributeIndices._5, 2, texCoords))
    }
    
    def drawAs():Int = GL_TRIANGLES
}
