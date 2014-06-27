package org.sofa.opengl.mesh

import org.sofa.nio._
import org.sofa.opengl._
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
  * and down directions respectively, hence the reason to have two more disks at
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
  */
class CylinderMesh(val radius:Float, height:Float, val segments:Int, val sections:Int = 1) extends Mesh {
    
    if(segments < 3)
        throw new RuntimeException("Cannot create cylinder with less than 3 sides")
    
    protected lazy val V:FloatBuffer = allocateVertices
    
    protected lazy val C:FloatBuffer = allocateColors
    
    protected lazy val N:FloatBuffer = allocateNormals
    
    protected lazy val T:FloatBuffer = allocateTangents

    protected lazy val X:FloatBuffer = allocateTexCoords
    
    protected lazy val I:IntBuffer   = allocateIndices
    
    protected lazy val B:FloatBuffer = allocateBones

    protected var textureRepeatS = 1

    protected var textureRepeatT = 1

    // -- Mesh edition -------------------------------------------------
    
    /** Set the top disk color, this must be done before the vertex array is produced. */
    def setTopDiskColor(color:Rgba) {
        // The disk color.
        
        var start = segments * (2+sections) * 4
        
        for(s <- 0 until segments) {
            C(start+0) = color.red.toFloat
            C(start+1) = color.green.toFloat
            C(start+2) = color.blue.toFloat
            C(start+3) = color.alpha.toFloat
            
            start += 4
        }
        
        start = ((3+sections) * segments * 4) + 4

        // The central point color.
        
        C(start+0) = color.red.toFloat
        C(start+1) = color.green.toFloat
        C(start+2) = color.blue.toFloat
        C(start+3) = color.alpha.toFloat
    }
    
    /** Set the `disk` color, this must be done before the vertex array is produced. */
    def setDiskColor(disk:Int, color:Rgba) {
        // The disk color.
        
        var start = segments * (disk) * 4
        
        for(s <- 0 until segments) {
            C(start+0) = color.red.toFloat
            C(start+1) = color.green.toFloat
            C(start+2) = color.blue.toFloat
            C(start+3) = color.alpha.toFloat
            
            start += 4
        }
        
    }
    
    /** Set the botom disk color, this must be done before the vertex array is produced. */
    def setBottomDiskColor(color:Rgba) {
        // The disk color.
        
        var start = 0
        
        for(s <- 0 until segments) {
            C(start+0) = color.red.toFloat
            C(start+1) = color.green.toFloat
            C(start+2) = color.blue.toFloat
            C(start+3) = color.alpha.toFloat
            
            start += 4
        }
        
        start = ((3+sections) * segments * 4)

        // The central point color.
        
        C(start+0) = color.red.toFloat
        C(start+1) = color.green.toFloat
        C(start+2) = color.blue.toFloat
        C(start+3) = color.alpha.toFloat 
    }
    
    /** Set the cylinder color, this must be done before a vertex array is produced. */
    def setCylinderColor(color:Rgba) {
        var start = segments * 4;
        var end   = (2+sections) * segments * 4
        
        for(i <- start until end by 4) {
            C(i+0) = color.red.toFloat
            C(i+1) = color.green.toFloat
            C(i+2) = color.blue.toFloat
            C(i+3) = color.alpha.toFloat
        }
    }
    
    // -- Mesh interface -----------------------------------------

    def vertexCount:Int = (6 * (sections + 1)) + 2

    override def attribute(name:String):FloatBuffer = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => V
    		case VertexAttribute.Color    => C
    		case VertexAttribute.Normal   => N
    		case VertexAttribute.Tangent  => T
    		case VertexAttribute.TexCoord => X
    		case VertexAttribute.Bone     => B
    		case _                        => super.attribute(name) //throw new RuntimeException("mesh has no %s attribute".format(name))
    	}
    }
    
    override def indices:IntBuffer = I

    override def attributeCount():Int = 6 + super.attributeCount

    override def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString,
    											   VertexAttribute.Color.toString,
    											   VertexAttribute.Normal.toString,
    											   VertexAttribute.Tangent.toString,
    											   VertexAttribute.TexCoord.toString,
    											   VertexAttribute.Bone.toString) ++ super.attributes
     
    override def components(name:String):Int = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => 3
    		case VertexAttribute.Color    => 4
    		case VertexAttribute.Normal   => 3
    		case VertexAttribute.Tangent  => 3
    		case VertexAttribute.TexCoord => 2
    		case VertexAttribute.Bone     => 1
    		case _                        => super.components(name) //throw new RuntimeException("mesh has no %s attribute".format(name))
    	}
    }

    override def has(name:String):Boolean = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => true
    		case VertexAttribute.Color    => true
    		case VertexAttribute.Normal   => true
    		case VertexAttribute.Tangent  => true
    		case VertexAttribute.TexCoord => true
    		case VertexAttribute.Bone     => true
    		case _                        => super.has(name) //false
    	}
    }
    
    override def hasIndices = true

    def drawAs(gl:SGL):Int = gl.TRIANGLES

    // -- Mesh building --------------------------------------------------
   
    protected def allocateVertices:FloatBuffer = {
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

        val n      = ((3+sections) * segments + 2) * 3 // (4 disks + (sections-1)) = (3 + sections)
        val buf    = FloatBuffer(n)
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
        
        buf
    }
    
    protected def allocateTexCoords:FloatBuffer = {
        val n   = ((3+sections) * segments + 2) * 2
        val buf = FloatBuffer(n)
        val nx  = textureRepeatS.toFloat / segments
        var xx  = 0.0f
        
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
        
        buf
    }

    protected def allocateColors:FloatBuffer = {
        val n   = ((3 + sections) * segments + 2) * 4
        val buf = FloatBuffer(n)
        
        for(i <- 0 until n) {
        	buf(i) = 1f
        }
        
        buf
    }

    protected def allocateNormals:FloatBuffer = {
        val n   = ((3+sections) * segments + 2) * 3 // (4 disks + (sections-1)) = (3 + sections)
        val buf = FloatBuffer(n)
        
        for(s <- 0 until segments) {
        	val i = s * 3
        	
        	for(disk <- 1 until (2+sections)) {
        	    val start  = segments * 3 * disk
        		val normal = Vector2(V(start+i), V(start+i+2))
        	    
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
        
        buf
    }

    protected def allocateTangents:FloatBuffer = {
        val n   = ((3+sections) * segments + 2) * 3 // (4 disks + (sections-1)) = (3 + sections)
        val buf = FloatBuffer(n)
        
        for(s <- 0 until segments) {
        	val i = s * 3
        	
        	for(disk <- 1 until (2+sections)) {
        	    val start  = segments * 3 * disk
        		val normal = Vector2(-V(start+i+2), V(start+i))
        	    
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
        
        buf
    }
    
    protected def allocateBones:FloatBuffer = {
        val n = segments * (3+sections)
        val buf = FloatBuffer(n+2)

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

        buf
    }

    protected def allocateIndices:IntBuffer = {
        // There are segments triangles per opening and closing disk.
        // There are 2*segments triangles for each section of the tube.
        // Times 3 since each triangle is made of 3 points
        
        val n   = ((2 + (2 * sections)) * segments) * 3
        val buf = IntBuffer(n)
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
        
        assert(i == n)
        
        buf
    }    
}