package org.sofa.opengl.mesh

import org.sofa.nio.FloatBuffer
import org.sofa.opengl.SGL
import org.sofa.opengl.VertexArray
import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._
import org.sofa.math.Rgba
import org.sofa.math.Point3

class DynPointsMesh(val size:Int) extends Mesh with ColorableMesh {
	
	/** The mutable set of coordinates. */
	protected lazy val V:FloatBuffer = allocateVertices
	
	/** The mutable set of colors. */
    protected lazy val C:FloatBuffer = allocateColors
    
    /** Start position of the last modification inside the coordinates array. */
    protected var vbeg = 0
    
    /** End position of the last modification inside the coordinates array. */
    protected var vend = size
    
    /** Start position of the last modification inside the color array. */
    protected var cbeg = 0
    
    /** End position of the last modification inside the color array. */
    protected var cend = size

    /** Last vertex array created using newVertexArray(). */
    protected var vertexArray:VertexArray = null
    
    protected def allocateVertices():FloatBuffer = new FloatBuffer(size*3)
	
	protected def allocateColors():FloatBuffer = new FloatBuffer(size*4)
    
	def vertices:FloatBuffer = V
	
	override def colors:FloatBuffer = C
	
	override def hasColors = true
	
	def setPoint(i:Int, p:Point3) { setPoint(i, p.x.toFloat, p.y.toFloat, p.z.toFloat) }
	
	def setPoint(i:Int, x:Float, y:Float, z:Float) {
		val p = i*3
		V(p)   = x
		V(p+1) = y
		V(p+2) = z
		
		if(i < vbeg) vbeg = i
		if(i+1 > vend) vend = i+1
	}
	
	def setColor(i:Int, c:Rgba) { setColor(i, c.red.toFloat, c.green.toFloat, c.blue.toFloat, c.alpha.toFloat) }
	
	def setColor(i:Int, red:Float, green:Float, blue:Float, alpha:Float) {
		val p = i*4
		C(p)   = red
		C(p+1) = green
		C(p+2) = blue
		C(p+3) = alpha

		if(i < cbeg) cbeg = i
		if(i+1 > cend) cend = i+1
	}
	
	def getPoint(i:Int):Point3 = { val p = i*3; Point3(V(p), V(p+1), V(p+2)) }
	
	def getColor(i:Int):Rgba = { val p = i*4; Rgba(C(p), C(p+1), C(p+2), C(p+3)) }
	
	def drawAs():Int = GL_POINTS
	
    override def newVertexArray(gl:SGL, locations:Tuple6[Int,Int,Int,Int,Int,Int]):VertexArray = {
		cbeg = size; cend = 0; vbeg = size; vend = 0
		vertexArray = super.newVertexArray(gl, locations)
		vertexArray
	}

    override def newVertexArray(gl:SGL, drawMode:Int, locations:Tuple6[Int,Int,Int,Int,Int,Int]):VertexArray = {
		cbeg = size; cend = 0; vbeg = size; vend = 0
		vertexArray = super.newVertexArray(gl, drawMode, locations)
		vertexArray
	}
    
	override def newVertexArray(gl:SGL, drawMode:Int, locations:Tuple2[String,Int]*):VertexArray = {
    	cbeg = size; cend = 0; vbeg = size; vend = 0
    	vertexArray = super.newVertexArray(gl, drawMode, locations:_*)
    	vertexArray
    }
    
    override def newVertexArray(gl:SGL, locations:Tuple2[String,Int]*):VertexArray = {
    	cbeg = size; cend = 0; vbeg = size; vend = 0
    	vertexArray = super.newVertexArray(gl, locations:_*)
    	vertexArray
    }
	
    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
	def updateVertexArray(gl:SGL, verticesName:String, colorsName:String) {
		if(vertexArray ne null) {
			if(vend > vbeg) {
				if(vbeg == 0 && vend == size)
					 vertexArray.buffer(verticesName).update(vertices)
				else vertexArray.buffer(verticesName).update(vbeg, vend, vertices)
				
				vbeg = size
				vend = 0
			}
			
			if(cend > cbeg) {
				if(cbeg == 0 && cend == size)
					 vertexArray.buffer(colorsName).update(colors)
				else vertexArray.buffer(colorsName).update(cbeg, cend, colors)
				
				cbeg = size
				cend = 0				
			}
		}
	}
}