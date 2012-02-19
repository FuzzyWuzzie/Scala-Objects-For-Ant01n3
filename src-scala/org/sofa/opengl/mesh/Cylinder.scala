package org.sofa.opengl.mesh

import org.sofa.nio._
import org.sofa.opengl._
import scala.math._
import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._
import org.sofa.math.Vector3
import org.sofa.math.Vector2
import java.awt.Color

//     +-----+                     m = Sections = 2.
//    / \   / \                    n = Segments = 6.
//   +--- * ---+ ---- Disk 4       
//    \ /   \ /                    n = 6 points per disk.
//   + +-----+ + ---- Disk 3       disks 0 and 4 close the tube.
//   |\       /|                   disks 1 to 3 make up the cylinder.
//   | +-----+ |
//   + |     | + ---- Disk 2       n = 6 triangles per opening/closing disk.
//   |\|     |/|                   2n = 12 triangles per section. 
//   | +-----+ |
//   + |     | + ---- Disk 1       The vertex array contains the disks in order.
//    \|     |/                    The middle points of disk 0 and 4 are the two last points.
//   + +-----+ + ---- Disk 0
//    \       /                    
//     +-----+
//
// Triangles : CCW

class Cylinder(val radius:Float, height:Float, val segments:Int, val sections:Int = 1) extends Mesh 
			with TangentSurfaceMesh with ColorableMesh with IndexedMesh with TexturableMesh {
    
    if(segments < 3)
        throw new RuntimeException("Cannot create cylinder with less than 3 sides")
    
    protected lazy val V:FloatBuffer = allocateVertices
    
    protected lazy val C:FloatBuffer = allocateColors
    
    protected lazy val N:FloatBuffer = allocateNormals
    
    protected lazy val T:FloatBuffer = allocateTangents

    protected lazy val X:FloatBuffer = allocateTexCoords
    
    protected lazy val I:IntBuffer   = allocateIndices

    protected var textureRepeatS = 4
    
    protected var textureRepeatT = 4
    
    def vertices:FloatBuffer = V
    
    def colors:FloatBuffer = C
    
    def normals:FloatBuffer = N
    
    def tangents:FloatBuffer = T
    
    def texCoords:FloatBuffer = X
    
    def indices:IntBuffer = I
     
    override def hasColors = true
    
    override def hasIndices = true
    
    override def hasNormals = true
    
    override def hasTangents = true
    
    override def hasTexCoords = true
   
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
        val buf    = new FloatBuffer(n)
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
        val buf = new FloatBuffer(n)
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
        val buf = new FloatBuffer(n)
        
        for(i <- 0 until n) {
        	buf(i) = 1f
        }
        
        buf
    }

    protected def allocateNormals:FloatBuffer = {
        val n   = ((3+sections) * segments + 2) * 3 // (4 disks + (sections-1)) = (3 + sections)
        val buf = new FloatBuffer(n)
        
        for(s <- 0 until segments) {
        	val i = s * 3
        	
        	for(disk <- 1 until (2+sections)) {
        	    val start  = segments * 3 * disk
        		val normal = Vector2(vertices(start+i), vertices(start+i+2))
        	    
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
        val buf = new FloatBuffer(n)
        
        for(s <- 0 until segments) {
        	val i = s * 3
        	
        	for(disk <- 1 until (2+sections)) {
        	    val start  = segments * 3 * disk
        		val normal = Vector2(-vertices(start+i+2), vertices(start+i))
        	    
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
    
    def newVertexArray(gl:SGL) = new VertexArray(gl, indices, (3, vertices), (4, colors), (3, normals), (3, tangents), (2, texCoords))
    
    def drawAs():Int = GL_TRIANGLES
    
    def setTopDiskColor(color:Color) {
        // The disk color.
        
        val red   = color.getRed / 255f
        val green = color.getGreen / 255f
        val blue  = color.getBlue / 255f
        val alpha = color.getAlpha / 255f
        var start = segments * (2+sections) * 4
        
        for(s <- 0 until segments) {
            colors(start+0) = red
            colors(start+1) = green
            colors(start+2) = blue
            colors(start+3) = alpha
            
            start += 4
        }
        
        start = ((3+sections) * segments * 4) + 4

        // The central point color.
        
        colors(start+0) = red
        colors(start+1) = green
        colors(start+2) = blue
        colors(start+3) = alpha
    }
    
    def setDiskColor(disk:Int, color:Color) {
        // The disk color.
        
        val red   = color.getRed / 255f
        val green = color.getGreen / 255f
        val blue  = color.getBlue / 255f
        val alpha = color.getAlpha / 255f
        var start = segments * (disk) * 4
        
        for(s <- 0 until segments) {
            colors(start+0) = red
            colors(start+1) = green
            colors(start+2) = blue
            colors(start+3) = alpha
            
            start += 4
        }
        
    }
    
    def setBottomDiskColor(color:Color) {
        // The disk color.
        
        val red   = color.getRed / 255f
        val green = color.getGreen / 255f
        val blue  = color.getBlue / 255f
        val alpha = color.getAlpha / 255f
        var start = 0
        
        for(s <- 0 until segments) {
            colors(start+0) = red
            colors(start+1) = green
            colors(start+2) = blue
            colors(start+3) = alpha
            
            start += 4
        }
        
        start = ((3+sections) * segments * 4)

        // The central point color.
        
        colors(start+0) = red
        colors(start+1) = green
        colors(start+2) = blue
        colors(start+3) = alpha        
    }
    
    def setCylinderColor(color:Color) {
        var start = segments * 4;
        var end   = (2+sections) * segments * 4
        val red   = color.getRed / 255f
        val green = color.getGreen / 255f
        val blue  = color.getBlue / 255f
        val alpha = color.getAlpha / 255f
        
        for(i <- start until end by 4) {
            colors(i+0) = red
            colors(i+1) = green
            colors(i+2) = blue
            colors(i+3) = alpha
        }
    }
}
