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

class BoneMesh extends Mesh {
	protected lazy val V:FloatBuffer = allocateVertices
	
	protected lazy val C:FloatBuffer = allocateColors
	
	protected lazy val I:IntBuffer = allocateIndices

	// -- Edition ------------------------------------------
	
	def setColor(color:Color) {
	    val n = 6 * 4
	    val red   = color.getRed   / 255f
        val green = color.getGreen / 255f
        val blue  = color.getBlue  / 255f
        val alpha = color.getAlpha / 255f

	    for(i <- 0 until n by 4) {
	        C(i+0) = red
	        C(i+1) = green
	        C(i+2) = blue
	        C(i+3) = alpha
	    }
	}

	// -- Mesh interface -----------------------------------

	def attribute(name:String):FloatBuffer = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex => V
			case VertexAttribute.Color  => C
			case _                      => throw new RuntimeException("mesh has no %s attribute".format(name))
		}
	}	

	def attributeCount():Int = 2

	def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.Color.toString)
	
	override def indices:IntBuffer = I
		
	override def hasIndices = true

	def components(name:String):Int = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex => 3
			case VertexAttribute.Color  => 4
			case _                      => throw new RuntimeException("mesh has no %s attribute".format(name))
		}
	}	

	def has(name:String):Boolean = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex => true
			case VertexAttribute.Color  => true
			case _                      => false
		}
	}	

	def drawAs():Int = GL_TRIANGLES
	
	// -- Building ------------------------------------------

	protected def allocateVertices:FloatBuffer = {
	    val n = 6 * 3
	    val buf = new FloatBuffer(n)
	    
	    // Four middle points 0 1 2 3
	    // Right 0
	    buf(0) = 0.25f
	    buf(1) = 0.25f
	    buf(2) = 0

	    // Front 1
	    buf(3) = 0
	    buf(4) = 0.25f
	    buf(5) = 0.25f

	    // Left 2
	    buf(6) = -0.25f
	    buf(7) =  0.25f
	    buf(8) =  0

	    // Back 3
	    buf( 9) =  0
	    buf(10) =  0.25f
	    buf(11) = -0.25f

	    // Bottom point 4
	    buf(12) = 0
	    buf(13) = 0
	    buf(14) = 0
	    
	    // Top point 5
	    buf(15) = 0 
	    buf(16) = 1
	    buf(17) = 0
	    
	    buf
	}
	
	protected def allocateColors:FloatBuffer = {
	    val n = 6 * 4
	    val buf = new FloatBuffer(n)
	    
	    for(i <- 0 until n) {
	        buf(i) = 1
	    }
	    
	    buf
	}
	
	protected def allocateIndices:IntBuffer = {
	    val n = 8 * 3
	    val buf = new IntBuffer(n)
	    
	    var t=0
	    
	    for(i <- 0 until 4) {
	        buf(t+0) = (i)%4
	        buf(t+1) = 4
	        buf(t+2) = (i+1)%4

	        buf(t+3) = 5
	        buf(t+4) = (i)%4
	        buf(t+5) = (i+1)%4
	        
	        t += 6
	    }
	    
	    buf
	}
}

class BoneLineMesh extends BoneMesh {

	override def drawAs():Int = GL_LINES
		
	override protected def allocateIndices:IntBuffer = {
	    val n = 6 * 4
	    val buf = new IntBuffer(n)
	    
	    var t=0
	    
	    for(i <- 0 until 4) {
	        buf(t+0) = 4 // (i)%4
	        buf(t+1) = i%4
	        buf(t+2) = i%4
	        buf(t+3) = 5
	        buf(t+4) = i%4
	        buf(t+5) = (i+1)%4
	        
	        t += 6
	    }
	    
	    buf
	}
}