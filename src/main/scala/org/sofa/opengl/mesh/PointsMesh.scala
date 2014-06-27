package org.sofa.opengl.mesh

import org.sofa.nio.FloatBuffer
import org.sofa.opengl.{SGL,VertexArray,ShaderProgram}
import org.sofa.math.{Rgba, Point3}


class PointsMesh(val size:Int) extends Mesh {
	
	/** The mutable set of coordinates. */
	protected lazy val V:FloatBuffer = FloatBuffer(size*3)
	
	/** The mutable set of colors. */
    protected lazy val C:FloatBuffer = FloatBuffer(size*4)
    
    /** Start position of the last modification inside the coordinates array. */
    protected var vbeg = 0
    
    /** End position of the last modification inside the coordinates array. */
    protected var vend = size
    
    /** Start position of the last modification inside the color array. */
    protected var cbeg = 0
    
    /** End position of the last modification inside the color array. */
    protected var cend = size    
    
	// -- Mesh interface

	def vertexCount:Int = size

	override def attribute(name:String):FloatBuffer = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex => V
			case VertexAttribute.Color  => C
			case _ => super.attribute(name)// throw new RuntimeException("this mesh as no %s attribute".format(name))
		}
	}

	override def attributeCount():Int = 2 + super.attributeCount

	override def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.Color.toString) ++ super.attributes

	override def components(name:String):Int = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex => 3
			case VertexAttribute.Color  => 4
			case _ => super.components(name) //throw new RuntimeException("this mesh as no %s attribute".format(name))
		}
	}

	override def has(name:String):Boolean = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex => true
			case VertexAttribute.Color  => true
			case _                      => super.has(name) //false
		}
	}
	
	def drawAs(gl:SGL):Int = gl.POINTS
	
	// -- Edition interface ------------------------------------

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

	// -- Dynamic edition ----------------------------------------------
	
    override def beforeNewVertexArray() {
    	cbeg = size
    	cend = 0
    	vbeg = size
    	vend = 0
	}

    def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true) }
	
    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
	def updateVertexArray(gl:SGL, updateVertices:Boolean, updateColors:Boolean) {
		if(va ne null) {
			if(updateVertices && vend > vbeg) {
				if(vbeg == 0 && vend == size)
					 va.buffer(VertexAttribute.Vertex.toString).update(V)
				else va.buffer(VertexAttribute.Vertex.toString).update(vbeg, vend, V)
				
				vbeg = size
				vend = 0
			}
			
			if(updateColors && cend > cbeg) {
				if(cbeg == 0 && cend == size)
					 va.buffer(VertexAttribute.Color.toString).update(C)
				else va.buffer(VertexAttribute.Color.toString).update(cbeg, cend, C)
				
				cbeg = size
				cend = 0				
			}
		}
	}
}