package org.sofa.gfx.mesh

import org.sofa.nio.FloatBuffer
import org.sofa.gfx.{SGL,VertexArray,ShaderProgram}
import org.sofa.math.{Rgba, Point3}


class PointsMesh(val size:Int) extends Mesh {
	
	/** The mutable set of coordinates. */
	protected var V = addAttributeVertex() // new MeshAttribute(size*3)
	
	/** The mutable set of colors. */
    protected var C:MeshAttribute =  _ // new MeshAttribute(size*4)
    
	// -- Mesh interface

	def vertexCount:Int = size

	def elementsPerPrimitive:Int = 1
	
	def drawAs(gl:SGL):Int = gl.POINTS
	
	protected def addAttributeVertex():MeshAttribute = {
		if(V eq null) {
			V = addMeshAttribute(VertexAttribute.Vertex, 3)
		}

		V
	}

	def addAttributeColor():MeshAttribute = {
		if(C eq null) {
			C = addMeshAttribute(VertexAttribute.Color, 4)
		}

		C
	}

	// -- Edition interface ------------------------------------

	def setPoint(i:Int, p:Point3) { setPoint(i, p.x.toFloat, p.y.toFloat, p.z.toFloat) }
	
	def setPoint(i:Int, x:Float, y:Float, z:Float) {
		val p = i*3
		val v = V.theData

		v(p)   = x
		v(p+1) = y
		v(p+2) = z
		
		if(i   < V.beg) V.beg = i
		if(i+1 > V.end) V.end = i+1
	}
	
	def setColor(i:Int, c:Rgba) { setColor(i, c.red.toFloat, c.green.toFloat, c.blue.toFloat, c.alpha.toFloat) }
	
	def setColor(i:Int, red:Float, green:Float, blue:Float, alpha:Float) {
		if(C eq null)
			addAttributeColor


		val p = i*4
		val c = C.theData

		c(p)   = red
		c(p+1) = green
		c(p+2) = blue
		c(p+3) = alpha

		if(i   < C.beg) C.beg = i
		if(i+1 > C.end) C.end = i+1
	}
	
	def getPoint(i:Int):Point3 = { val p = i*3; val v = V.theData; Point3(v(p), v(p+1), v(p+2)) }
	
	def getColor(i:Int):Rgba = { val p = i*4; val c = V.theData; Rgba(c(p), c(p+1), c(p+2), c(p+3)) }

	// -- Dynamic edition ----------------------------------------------
	
    override def beforeNewVertexArray() {
    	V.resetMarkers
    	if(C ne null) C.resetMarkers
	}

    def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true) }
	
    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
	def updateVertexArray(gl:SGL, updateVertices:Boolean, updateColors:Boolean) {
		if(va ne null) {
			if(updateVertices) V.update(va)
			if(updateColors)   C.update(va)
		}
	}
}