package org.sofa.gfx.mesh

import org.sofa.nio._
import org.sofa.gfx._
import scala.math._
import org.sofa.math.{Vector2, Vector3, Rgba}


/** Representation of a cylinder.
  * 
  * The cylinder is closed by two top and bottom sections. It approximates a circle
  * using a given number of segments.
  * 
  * The cylinder is by default using a single section of tube, but can be configured
  * to have more sections.
  * 
  * The normals are organized so that the top and bottom disks points to the up
  * and down directions respectively, hence the reason to have two more disks of points at
  * the top and bottom. Normals along the tube points toward the exterior following
  * the radius of the tube. 
  *
  *     +-----+                     m = Sections = 2, therefore m+3 = 5 disks.
  *    / \   / \                    n = Segments = 6.
  *   +--- * ---+ ---- Disk 4       
  *    \ /   \ /                    n = 6 points per disk.
  *   + +-----+ + ---- Disk 3       disks 0 and 4 close the tube.
  *   |\       /|                   disks 1 to 3 make up the cylinder.
  *   | +-----+ |
  *   + |     | + ---- Disk 2       n = 6 triangles per opening/closing disk.
  *   |\|     |/|                   2n = 12 triangles per section. 
  *   | +-----+ |
  *   + |     | + ---- Disk 1       The vertices are organized by disk, plus two last vertices
  *    \|     |/                    The first to close the bottom disk, the second to close the
  *   + +-----+ + ---- Disk 0       top disk.
  *    \       /                    
  *     +-----+                     
  *                                 
  * The triangles are given in CW order.
  *
  * The only dynamic part of the mesh concerns colors.
  */
class CylinderMesh(val gl:SGL, val radius:Float, height:Float, val segments:Int, val sections:Int = 1) extends Mesh {
    
    if(segments < 3)
        throw new RuntimeException("Cannot create cylinder with less than 3 sides")

    protected var I:MeshElement = addIndices    
    
    protected var V:MeshAttribute = addAttributeVertex
    
    protected var C:MeshAttribute = _ //allocateColors
    
    protected var N:MeshAttribute = _ //allocateNormals
    
    protected var T:MeshAttribute = _ //allocateTangents

    protected var X:MeshAttribute = _ //allocateTexCoords
    
    protected var B:MeshAttribute = _ //allocateBones

    protected var textureRepeatS = 1

    protected var textureRepeatT = 1

    // -- Mesh edition -------------------------------------------------
    
    override def begin(attributes:String*) {
    	begin(VertexAttribute.Color)
    }

    /** Set the top disk color, this must be done before the vertex array is produced. */
    def setTopDiskColor(color:Rgba) {
    	if(C eq null)
    		throw new NoSuchVertexAttributeException("no color vertex attribute, add one before")

        // The disk color.
        
        val data  = C.data
        var start = segments * (2+sections) * 4
        
        for(s <- 0 until segments) {
            data(start+0) = color.red.toFloat
            data(start+1) = color.green.toFloat
            data(start+2) = color.blue.toFloat
            data(start+3) = color.alpha.toFloat
            
            start += 4
        }
        
        start = ((3+sections) * segments * 4) + 4

        // The central point color.
        
        data(start+0) = color.red.toFloat
        data(start+1) = color.green.toFloat
        data(start+2) = color.blue.toFloat
        data(start+3) = color.alpha.toFloat

        C.range(0, vertexCount)
    }
    
    /** Set the `disk` color, this must be done before the vertex array is produced. */
    def setDiskColor(disk:Int, color:Rgba) {
    	if(C eq null)
    		throw new NoSuchVertexAttributeException("no color vertex attribute, add one before")

        // The disk color.
        
        val data  = C.data
        var start = segments * (disk) * 4
        
        for(s <- 0 until segments) {
            data(start+0) = color.red.toFloat
            data(start+1) = color.green.toFloat
            data(start+2) = color.blue.toFloat
            data(start+3) = color.alpha.toFloat
            
            start += 4
        }
        
        C.range(0, vertexCount)
    }
    
    /** Set the botom disk color, this must be done before the vertex array is produced. */
    def setBottomDiskColor(color:Rgba) {
    	if(C eq null)
    		throw new NoSuchVertexAttributeException("no color vertex attribute, add one before")

        // The disk color.
        
        val data  = C.data
        var start = 0
        
        for(s <- 0 until segments) {
            data(start+0) = color.red.toFloat
            data(start+1) = color.green.toFloat
            data(start+2) = color.blue.toFloat
            data(start+3) = color.alpha.toFloat
            
            start += 4
        }
        
        start = ((3+sections) * segments * 4)

        // The central point color.
        
        data(start+0) = color.red.toFloat
        data(start+1) = color.green.toFloat
        data(start+2) = color.blue.toFloat
        data(start+3) = color.alpha.toFloat 

        C.range(0, vertexCount)
    }
    
    /** Set the cylinder color, this must be done before a vertex array is produced. */
    def setCylinderColor(color:Rgba) {
    	if(C eq null)
    		throw new NoSuchVertexAttributeException("no color vertex attribute, add one before")

    	val data  = C.data
        var start = segments * 4;
        var end   = (2+sections) * segments * 4
        
        for(i <- start until end by 4) {
            data(i+0) = color.red.toFloat
            data(i+1) = color.green.toFloat
            data(i+2) = color.blue.toFloat
            data(i+3) = color.alpha.toFloat
        }

        C.range(0, vertexCount)
    }
    
    // -- Mesh interface -----------------------------------------

    def vertexCount:Int = ((3+sections) * segments + 2) // (6 * (sections + 1)) + 2

    def elementsPerPrimitive:Int = 3
    
    def drawAs():Int = gl.TRIANGLES

    // -- Mesh building --------------------------------------------------
   
    protected def addAttributeVertex():MeshAttribute = {
    	if(V eq null) {
	        // There are 'segments' points per disc.
	        // There are two discs for the tube and two discs to close the tube
	        //    and three values per point.
	        // + (sections-1) discs if section > 1
	        // + 2 points at the center of the opening and closing disks.
	        // The first and last disks close the tube.
	        // The second and ante-last disk are the tube basis.
	        // In between disks, if any, are sections.
	        // The two points that close disk 1 and disk n-1 (first and last) are at the end of
	        // the array, in this order. Disk 1 is at the bottom.

    		V = addMeshAttribute(VertexAttribute.Vertex, 3)
	        V.begin

	        val n      = vertexCount * 3 // (4 disks + (sections-1)) = (3 + sections)
	        val buf    = V.data //FloatBuffer(n)
	        val hstep  = height / sections
	        val pstep  = (2*Pi) / segments
	        var angle  = 0.0
	        
	        for(s <- 0 until segments) {
	        	val i = s * 3
	        	val x = (cos(angle) * radius).toFloat
	        	val y = (sin(angle) * radius).toFloat
	        	
	        	for(disk <- 0 until (3+sections)) {
	        	    val start = segments * 3 * disk
	        	    val z     = if(disk <= 1) 0f else if(disk >= (1+sections)) height.toFloat else (hstep * (disk-1)).toFloat
	        		
	        	    buf(start+i+0) = x
	        	    buf(start+i+1) = z
	        	    buf(start+i+2) = y
	        	}
	        	
	        	angle += pstep
	        }

	        buf(n-4) = 0 
	        buf(n-5) = 0
	        buf(n-6) = 0

	        buf(n-1) = 0
	        buf(n-2) = height
	        buf(n-3) = 0
	        
	        V.range(0, vertexCount)
	        V.end
		}
        V
    }
    
    def addAttributeTexCoords():MeshAttribute = {
    	if(X eq null) {
    		X = addMeshAttribute(VertexAttribute.TexCoord, 2)
    		X.begin()

	        val n    = vertexCount * 2
	        val buf  = X.data //FloatBuffer(n)
	        val nx   = textureRepeatS.toFloat / segments
	        var xx   = 0.0f
	        
	        for(s <- 0 until segments) {
	            val i = s * 2
	            for(disk <- 0 until (3+sections)) {
	                val start = segments * 2 * disk
	        	    val zz    = if(disk <= 1) 0f else if(disk >= (1+sections)) 1 else (textureRepeatT.toFloat/sections * (disk-1)).toFloat
	                
	                buf(start+i+0) = xx
	                buf(start+i+1) = zz
	            }
	            
	            xx += nx
	        }
	        
	        // ??
	        
	        buf(n-3) = 0
	        buf(n-4) = 0
	        
	        buf(n-1) = 0
	        buf(n-2) = 0

	        X.range(0, vertexCount)
	        X.end()
    	}
        
        X
    }

    def addAttributeColor():MeshAttribute = {
    	if(C eq null) {
    		C = addMeshAttribute(VertexAttribute.Color, 4)
    		C.begin()

	        val n   = vertexCount * 4
    	    val buf = C.data //FloatBuffer(n)
        
        	for(i <- 0 until n) {
        		buf(i) = 1f
        	}

    		C.range(0, vertexCount)
    		C.end()
    	}
        
        C
    }

    def addAttributeNormal():MeshAttribute = {
    	if(N eq null) {
    		N = addMeshAttribute(VertexAttribute.Normal, 3)
    		N.begin
    		V.begin

	        val n   = vertexCount * 3 // (4 disks + (sections-1)) = (3 + sections)
	        val buf = N.data
	        val v   = V.data
	        
	        for(s <- 0 until segments) {
	        	val i = s * 3
	        	
	        	for(disk <- 1 until (2+sections)) {
	        	    val start  = segments * 3 * disk
	        		val normal = Vector2(v(start+i), v(start+i+2))
	        	    
	        		normal.normalize
	        		
	        	    buf(start+i+0) = normal.x.toFloat 
	        	    buf(start+i+1) = 0
	        	    buf(start+i+2) = normal.y.toFloat
	        	}
	        }

	        var start1 = 0
	        var start2 = (2+sections) * segments * 3 
	        
	        for(s <- 0 until segments) {
	            buf(start1+0) =  0
	            buf(start1+1) = -1
	            buf(start1+2) =  0
	            
	            buf(start2+0) =  0
	            buf(start2+1) =  1
	            buf(start2+2) =  0

	            start1 += 3
	            start2 += 3
	        }

	        buf(n-4) =  0 
	        buf(n-5) = -1
	        buf(n-6) =  0

	        buf(n-1) =  0
	        buf(n-2) =  1
	        buf(n-3) =  0


	        V.end
    		N.range(0, vertexCount)
    		N.end
    	}
        
        N
    }

	def addAttributeTangent():MeshAttribute = {
		if(T eq null) {
			T = addMeshAttribute(VertexAttribute.Tangent, 3)
			T.begin
			V.begin

	        val n   = vertexCount * 3 // (4 disks + (sections-1)) = (3 + sections)
	        val buf = T.data //FloatBuffer(n)
	        val v   = V.data
	        
	        for(s <- 0 until segments) {
	        	val i = s * 3
	        	
	        	for(disk <- 1 until (2+sections)) {
	        	    val start  = segments * 3 * disk
	        		val normal = Vector2(-v(start+i+2), v(start+i))
	        	    
	        		normal.normalize
	        		
	        	    buf(start+i+0) = normal.x.toFloat 
	        	    buf(start+i+1) = 0
	        	    buf(start+i+2) = normal.y.toFloat
	        	}
	        }

	        var start1 = 0
	        var start2 = (2+sections) * segments * 3 
	        
	        for(s <- 0 until segments) {
	            buf(start1+0) =  1
	            buf(start1+1) =  0
	            buf(start1+2) =  0
	            
	            buf(start2+0) = -1
	            buf(start2+1) =  0
	            buf(start2+2) =  0

	            start1 += 3
	            start2 += 3
	        }

	        buf(n-4) =  1 
	        buf(n-5) =  0
	        buf(n-6) =  0

	        buf(n-1) = -1
	        buf(n-2) =  0
	        buf(n-3) =  0

	        V.end
	        T.range(0, vertexCount)
	        T.end
        }

        T
    }
    
	def addAttributeBones():MeshAttribute = {
		if(B eq null) {
			B = addMeshAttribute(VertexAttribute.Bone, 1)
			B.begin

	        val n = segments * (3+sections)
	        val buf = B.data //FloatBuffer(n+2)

	        // There are by default 'segments' bones
	        
	        if(segments == 1) {
	            // Only one bone.
	            for(s <- 0 until n) {
	                buf(s) = 0
	            }
	        } else if(segments == 2) {
	            // The bone 0 is for the two bottom disks and the third disk.
	            for(s <- 0 until segments * 2) {
	                buf(s) = 0
	            }
	            // The bone 1 is for the two top disks.
	            for(s <- segments * 2 until n) {
	                buf(s) = 1
	            }
	        } else {
	            // The two bottom disks and first disk and one more are for bone 0 (first section).
	            for(s <- 0 until segments * 3) {
	                buf(s) = 0
	            }
	            // Other disks until the end are for each bone.
				var start = 3 * segments
				var end   = start + segments
	            for(b <- 1 until sections) {
	            	for(s<- start until end) {
	            	    buf(s) = b
	            	}
	                start += segments
	                end   += segments
	            }
	            // The top disks is also for the last bone.
	            for(s <- (n-segments) until n) {
	                buf(s) = sections - 1
	            }
	        }
	        
	        buf(n) = 0
	        buf(n+1) = sections - 1


			B.range(0, vertexCount)
			B.end
		}

        B
    }

    protected def addIndices():MeshElement = {
        // There are segments triangles per opening and closing disk.
        // There are 2*segments triangles for each section of the tube.
        // Times 3 since each triangle is made of 3 points
        
        if(I eq null) {
	        val n   = ((2 + (2 * sections)) * segments)
        	
        	I = addMeshElement(n, 3)
        	I.begin

	        val buf = I.data
	        var i   = 0
	        
	        // The tube sections
	        
	        for(disk <- 0 until sections) {
	        	val start1 = (disk+1) * segments
	            val start2 = (disk+2) * segments
	                
	            for(s <- 0 until segments) {
	                buf(i+0) = start1 + s
	                buf(i+1) = start2 + ((s + 1)%segments)
	                buf(i+2) = start2 + s
	                
	                buf(i+3) = start2 + ((s + 1)%segments)
	                buf(i+4) = start1 + s
	                buf(i+5) = start1 + ((s + 1)%segments)
	                
	                i += 6
	            }
	        }
	        
	        // The two closing discs
	        
	        val start1 = 0 
	        val start2 = (2 + sections) * segments 
	        val c1 = segments * (sections + 3)	// The closing points
	        val c2 = c1 + 1
	        
	        for(s <- 0 until segments) {
	        	buf(i+0) = start1 + s 
	        	buf(i+1) = c1
	        	buf(i+2) = start1 + ((s + 1)%segments)
	            
	        	buf(i+3) = start2 + s
	        	buf(i+4) = start2 + ((s + 1)%segments)
	        	buf(i+5) = c2
	        	
	            i += 6
	        }
	        
	        assert(i == n*3)

	        I.range(0, n)
	        I.end
        }

        I
    }    
}