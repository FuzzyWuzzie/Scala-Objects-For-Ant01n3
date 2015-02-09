package org.sofa.gfx.mesh.shapes

import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.gfx.mesh._
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
class PlaneMesh(val gl:SGL, val nVertX:Int, val nVertZ:Int, val width:Float, val depth:Float, var isXY:Boolean = false) extends Mesh {
    import VertexAttribute._
    
    protected var I:MeshElement = addIndex

    protected var V:MeshAttribute = addAttributeVertex
    
    protected var C:MeshAttribute = _ //addAttributeColor
    
    protected var N:MeshAttribute = _ //addAttributeNormal
    
    protected var T:MeshAttribute = _ //addAttributeTangent
    
    protected var X:MeshAttribute = _ //addAttributeTexCoord

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
    	if(C eq null)
    		throw new NoSuchVertexAttributeException("no color vertex attribute, add one first")

        val n = nVertX * nVertZ * 4
        val d = C.data
        
        for(i <- 0 until n by 4) {
            d(i+0) = color.red.toFloat
            d(i+1) = color.green.toFloat
            d(i+2) = color.blue.toFloat
            d(i+3) = color.alpha.toFloat
        }

        C.range(0, n/4)
    }

    // -- Mesh interface --------------------------------------------------------

    def vertexCount:Int = nVertX * nVertZ

    def elementsPerPrimitive:Int = 3

    def drawAs():Int = gl.TRIANGLES

    // -- Building ----------------------------------------------------------
    
    protected def addAttributeVertex:MeshAttribute = {
    	if(V eq null) {
    		V = addMeshAttribute(VertexAttribute.Position, 3)

	        V.begin

    		val v   = V.data
	        val nw  = width / (nVertX-1).toFloat
	        val nd  = depth / (nVertZ-1).toFloat 
	        var xx  = -width/2f
	        var zz  = -depth/2f
	        var i   = 0

	        if(isXY) {
	            for(d <- 0 until nVertZ) {
	                for(x <- 0 until nVertX) {
	                    v(i+0) = xx
	                    v(i+1) = zz	// **
	                    v(i+2) = 0
	                    xx += nw
	                    i  += 3
	                }
	                xx  = -width/2f
	            	zz += nd
	            }
	        } else {
	            for(d <- 0 until nVertZ) {
	                for(x <- 0 until nVertX) {
	                    v(i+0) = xx
	                    v(i+1) = 0
	                    v(i+2) = zz	// **
	                    xx += nw
	                    i  += 3
	                }
	                xx  = -width/2f
	                zz += nd
	            }
	        }
	        V.range(0, vertexCount)
	        V.end
		}        
        
        V
    }
    
    def addAttributeTexCoord:MeshAttribute = {
    	if(X eq null) {
			X = addMeshAttribute(VertexAttribute.TexCoord, 2)

	        X.begin

//	        val n   = nVertX * nVertZ * 2 
	        val v   = X.data
	        var nw  = textureRepeatS / (nVertX-1).toFloat
	        var nd  = textureRepeatT / (nVertZ-1).toFloat
	        var xx  = 0f
	        var zz  = 0f
	        var i   = 0
	        
	        for(d <- 0 until nVertZ) {
	            for(x <- 0 until nVertX) {
	                v(i+0) = xx
	                v(i+1) = zz
	                xx += nw
	                i  += 2
	            }
	            xx  = 0
	            zz += nd
	        }

	        X.range(0, vertexCount)
	       	X.end
        }

        X
    }

    def addAttributeColor:MeshAttribute = {
    	if(C eq null) {
    		C = addMeshAttribute(VertexAttribute.Color, 4)

    		C.begin

    		val d = C.data
	        val n = nVertX * nVertZ * 4
	        
	        for(i <- 0 until n) {
	        	d(i) = 1f
	        }

	        C.range(0, vertexCount)
	        C.end
		}

        C
    }

    def addAttributeNormal:MeshAttribute = {
    	if(N eq null) {
    		N = addMeshAttribute(VertexAttribute.Normal, 3)

    		N.begin

    		val d = N.data
	        val n = nVertX * nVertZ * 3
	        
	        if(isXY) {
	            for(i <- 0 until n by 3) {
	               d(i+0) = 0f
	               d(i+1) = 0f
	               d(i+2) = 1f
	            }
	        } else {
	            for(i <- 0 until n by 3) {
	        	   d(i+0) = 0f
	        	   d(i+1) = 1f
	        	   d(i+2) = 0f
	            }
	        }

	        N.range(0, vertexCount)
	        N.end
		}        
        
        N
    }
    
    def addAttributeTangent:MeshAttribute = {
    	if(T eq null) {
    		T = addMeshAttribute(VertexAttribute.Tangent, 3)

    		T.begin

    		val d = T.data
	        val n = nVertX * nVertZ * 3
	        
	        for(i <- 0 until n by 3) {
	            d(i+0) = 1f
	            d(i+1) = 0f
	            d(i+2) = 0f
	        }

	        T.range(0, vertexCount)
	        T.end
    	}
            
        T
    }

    protected def addIndex:MeshElement = {
    	if(I eq null) {
	        val n = ((nVertX-1) * (nVertZ-1))	// n Squares
	        var i = 0

    		I = addMeshElement(n*2, 3)	// 2 triangles per square, 3 vertices per triangle
    		I.begin
	        
	        val d = I.data

	        for(z <- 0 until nVertZ-1) {
	        	for(x <- 0 until nVertX-1) {
	        	    d(i+0) = z*nVertX + x 
	        	    d(i+1) = z*nVertX + x + 1
	        	    d(i+2) = z*nVertX + x + nVertX + 1 
	        	    d(i+3) = z*nVertX + x 
	        	    d(i+4) = z*nVertX + x + nVertX + 1
	        	    d(i+5) = z*nVertX + x + nVertX 
	        	    i += 6
	        	}
	        }

	        I.range(0, n*2)
	        I.end
    	}

    	I
    }
}