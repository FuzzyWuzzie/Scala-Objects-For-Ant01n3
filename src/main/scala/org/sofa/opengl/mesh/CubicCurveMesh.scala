package org.sofa.opengl.mesh

import javax.media.opengl._
import org.sofa.nio._
import org.sofa.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._
import scala.math._
import org.sofa.math._
import org.sofa.math.CubicCurve._

// class CubicCurve(val segments:Int, val A:Point3, val B:Point3, val C:Point3, val D:Point3) extends Mesh {
// 	protected val V:FloatBuffer = new FloatBuffer((segments+1)*3)
	
// 	def this(segments:Int) {
// 	    this(segments, Point3(0,0,0), Point3(0,0,0), Point3(0,0,0), Point3(0,0,0))
// 	}
	
// 	if(segments < 1) 
// 	    throw new RuntimeException("number of segments in CubicCurve must at least be 1")
	
// 	def changeCurve(a:Point3, b:Point3, c:Point3, d:Point3) {
// 	    A.copy(a)
// 	    B.copy(b)
// 	    C.copy(c)
// 	    D.copy(d)
	    
// 	    val size = (segments + 1) * 3
// 	    var step = 1.0 / segments
// 	    var t    = 0.0
	    
// 	    for(i <- 0 until size by 3) {
// 	       V(i+0) = evalCubic(A.x, B.x, C.x, D.x, t).toFloat
// 	       V(i+1) = evalCubic(A.y, B.y, C.y, D.y, t).toFloat
// 	       V(i+2) = evalCubic(A.z, B.z, C.z, D.z, t).toFloat
// 	       t       += step
// 	    }
// 	}
	
// 	def vertices:FloatBuffer = V
	
// //	def newVertexArray(gl:SGL):VertexArray = new VertexArray(gl, (0, 3, vertices))
	
// 	def drawAs():Int = GL_LINE_STRIP
// }

class CubicCurveMesh(countCurves:Int, val segments:Int) extends Mesh {
    protected val V:FloatBuffer = new FloatBuffer((segments+1)*count*3)
    
    protected val F:IntBuffer = new IntBuffer(count)
    
    protected val C:IntBuffer = new IntBuffer(count)
    
    init()
    
    protected def init() { 
    	if(segments < 1)
    		throw new RuntimeException("number of segments in CubicCurves must at least be 1")
    
    	val size = (segments + 1)
    	
    	for(i <- 0 until count) {
    		F(i) = i * size
    		C(i) = size
    	}
    }
    
    def setCurve(i:Int, a:Point3, b:Point3, c:Point3, d:Point3) {
	    val size  = (segments + 1) * 3
        val start = i * size
	    var step  = 1.0 / segments
	    var t     = 0.0
        
	    for(j <- start until (start+size) by 3) {
	       V(j+0) = evalCubic(a.x, b.x, c.x, d.x, t).toFloat
	       V(j+1) = evalCubic(a.y, b.y, c.y, d.y, t).toFloat
	       V(j+2) = evalCubic(a.z, b.z, c.z, d.z, t).toFloat
	       t       += step
	    }
    }

    def attribute(name:String):FloatBuffer = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => V
    		case _                      => throw new RuntimeException("mesh has no %s attribute".format(name))
    	}
    }

    def attributeCount():Int = 1

    def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString)

    def components(name:String):Int = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => 1
    		case _                      => throw new RuntimeException("mesh has no %s attribute".format(name))
    	}
    }

    def has(name:String):Boolean = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => true
    		case _                      => false
    	}    	
    }
        
    def firsts:IntBuffer = F
    
    def counts:IntBuffer = C
    
    def count:Int = countCurves
    
    def drawAs():Int = GL_LINE_STRIP
}