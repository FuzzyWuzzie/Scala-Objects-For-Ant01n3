package org.sofa.gfx.mesh

import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.math.Rgba

/** A single plane of several quads (themselves made of two triangles) in the XZ plane, centered
  * around the (0, 0, 0) point.
  *
  * The number of rows and columns of quads can be specified with `nVertX` and `nVertZ` and the
  * overall size of the plane can be given with `width` and `depth`.
  *
  * The last argument of the constructor `isXY` allows to build a plane in the XY plane instead of
  * the XZ plane. In this case the `nVertZ` argument must naturally be read `nVertY`.
  * 
  * The plane is made of triangles and must be drawn in this mode. Indices must be used. You can
  * use a normal, color, tangent, and tex-coords arrays with this mesh.
  * 
  * Z
  * ^ 
  * |
  * +--+--+  nVertX = 3
  * | /| /|  nVertZ = 3
  * |/ |/ |  The origin is at the center.
  * +--0--+
  * | /| /|
  * |/ |/ |
  * +--+--+-->X
  * 
  * Triangles are in CW order.
  */
class PlaneMesh(val nVertX:Int, val nVertZ:Int, val width:Float, val depth:Float, var isXY:Boolean = false) extends Mesh {
    import VertexAttribute._

    protected lazy val V:FloatBuffer = allocateVertices
    
    protected lazy val C:FloatBuffer = allocateColors
    
    protected lazy val N:FloatBuffer = allocateNormals
    
    protected lazy val T:FloatBuffer = allocateTangents
    
    protected lazy val X:FloatBuffer = allocateTexCoords
    
    protected lazy val I:IntBuffer = allocateIndices

    protected var textureRepeatS:Int = 1

    protected var textureRepeatT:Int = 1

	/** Define how many times the texture repeats along the S and T coordinates. This must be
	  * done before the plane is transformed to a mesh. */    
    def setTextureRepeat(S:Int, T:Int) {
        textureRepeatS = S
        textureRepeatT = T
    }

	/** Set the color of the whole plane. This must be done before the plane is transformed to a mesh. */    
    def setColor(color:Rgba) {
        val n     = nVertX * nVertZ * 4
        
        for(i <- 0 until n by 4) {
            C(i+0) = color.red.toFloat
            C(i+1) = color.green.toFloat
            C(i+2) = color.blue.toFloat
            C(i+3) = color.alpha.toFloat
        }
    }

    // -- Mesh interface --------------------------------------------------------

    def vertexCount:Int = nVertX * nVertZ

    def elementsPerPrimitive:Int = 3

    override def attribute(name:String):FloatBuffer = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => V
    		case VertexAttribute.Color    => C
    		case VertexAttribute.Normal   => N
    		case VertexAttribute.Tangent  => T
    		case VertexAttribute.TexCoord => X
    		case _                        => super.attribute(name) //throw new RuntimeException("This mesh does not have a vertex attribute %s".format(name))
    	}
    }

    override def attributeCount:Int = 5 + super.attributeCount

    override def attributes() = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.Normal.toString,
    		VertexAttribute.Tangent.toString, VertexAttribute.TexCoord.toString, VertexAttribute.Color.toString) ++ super.attributes

    override def components(name:String):Int = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => 3
    		case VertexAttribute.Color    => 4
    		case VertexAttribute.Normal   => 3
    		case VertexAttribute.Tangent  => 3
    		case VertexAttribute.TexCoord => 2
    		case _                        => super.components(name) //throw new RuntimeException("This mesh does not have a vertex attribute %s".format(name))
    	}
    }

    override def has(name:String):Boolean = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => true
    		case VertexAttribute.Color    => true
    		case VertexAttribute.Normal   => true
    		case VertexAttribute.Tangent  => true
    		case VertexAttribute.TexCoord => true
    		case _                        => super.has(name) //false
    	}    	
    }

    override def hasIndices():Boolean = true
    
    override def indices:IntBuffer = I
    
    def drawAs(gl:SGL):Int = gl.TRIANGLES

    // -- Building ----------------------------------------------------------
    
    protected def allocateVertices:FloatBuffer = {
        val buf = FloatBuffer(nVertX * nVertZ * 3)
        val nw  = width / (nVertX-1).toFloat
        val nd  = depth / (nVertZ-1).toFloat 
        var xx  = -width/2f
        var zz  = -depth/2f
        var i   = 0

        if(isXY) {
            for(d <- 0 until nVertZ) {
                for(x <- 0 until nVertX) {
                    buf(i+0) = xx
                    buf(i+1) = zz	// **
                    buf(i+2) = 0
                    xx += nw
                    i  += 3
                }
                xx  = -width/2f
            	zz += nd
            }
        } else {
            for(d <- 0 until nVertZ) {
                for(x <- 0 until nVertX) {
                    buf(i+0) = xx
                    buf(i+1) = 0
                    buf(i+2) = zz	// **
                    xx += nw
                    i  += 3
                }
                xx  = -width/2f
                zz += nd
            }
        }
        
        buf
    }
    
    protected def allocateTexCoords:FloatBuffer = {
        val n   = nVertX * nVertZ * 2 
        val buf = FloatBuffer(n)
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
        val buf = FloatBuffer(n)
        
        for(i <- 0 until n) {
        	buf(i) = 1f
        }
        
        buf
    }

    protected def allocateNormals:FloatBuffer = {
        val n   = nVertX * nVertZ * 3
        val buf = FloatBuffer(n)
        
        if(isXY) {
            for(i <- 0 until n by 3) {
               buf(i+0) = 0f
               buf(i+1) = 0f
               buf(i+2) = 1f
            }
        } else {
            for(i <- 0 until n by 3) {
        	   buf(i+0) = 0f
        	   buf(i+1) = 1f
        	   buf(i+2) = 0f
            }
        }
        
        buf
    }
    
    protected def allocateTangents:FloatBuffer = {
        val n   = nVertX * nVertZ * 3
        val buf = FloatBuffer(n)
        
        for(i <- 0 until n by 3) {
            buf(i+0) = 1f
            buf(i+1) = 0f
            buf(i+2) = 0f
        }
            
        buf
    }

    protected def allocateIndices:IntBuffer = {
        val n   = ((nVertX-1) * (nVertZ-1))	// n Squares
        val buf = IntBuffer(n*2*3)		// 2 triangles per square
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
}