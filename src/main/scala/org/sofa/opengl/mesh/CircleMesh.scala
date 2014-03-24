package org.sofa.opengl.mesh

import javax.media.opengl._
import org.sofa.nio._
import org.sofa.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._ 

import scala.math._

/** Create a circle centered at (0,0,0) with given `radius` and number of `sides` on the XZ plane.
  * 
  * The circle is made of lines. 
  * 
  * The data is usable to draw directly the vertices or to use indices. Vertices are given in
  * order, following the trigonometric direction. They must be drawn in "line loop" mode.
  */
class CircleMesh(radius:Double, sides:Int) extends Mesh {
	
	protected lazy val V:FloatBuffer = allocateVertices
    
    protected lazy val I:IntBuffer = allocateIndices

    // -- Mesh interface -------------------------------

	def attribute(name:String):FloatBuffer = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex => V
			case _                      => throw new RuntimeException("mesh has no %s attribute".format(name))
		}
	}

	def attributeCount():Int = 1

	def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString)
    
    override def indices:IntBuffer = I

    override def hasIndices = true

	def components(name:String):Int = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex => 3
			case _                      => throw new RuntimeException("mesh has no %s attribute".format(name))
		}
	}

	def has(name:String):Boolean = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex => true
			case _                      => false
		}
	}
    
    override def drawAs:Int = GL_LINE_LOOP
    
    // -- Building -------------------------------------

    protected def allocateVertices():FloatBuffer = {
	    var size = (sides) * 3
	    val buf = FloatBuffer(size)
	    val kstep = (2*Pi) / sides
	    var k = 0.0
	    
	    for(i <- 0 until size by 3) {
	        buf(i)   = (cos(k) * radius).toFloat
	        buf(i+1) = 0f
	        buf(i+2) = (sin(k) * radius).toFloat
	        k += kstep
	    }
	    
	    buf
	}
	
	protected def allocateIndices():IntBuffer = {
	    val size = sides
	    val buf = IntBuffer(size)
	    
	    for(i <- 0 until size) {
	        buf(i) = i
	    }
	    
	    buf
	}
}