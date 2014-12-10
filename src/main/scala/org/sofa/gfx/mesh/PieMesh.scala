package org.sofa.gfx.mesh

import scala.math._

import org.sofa.gfx.{SGL, VertexArray, ShaderProgram}
import org.sofa.math.{Rgba, Point3, Vector3, NumberSeq2, NumberSeq3, Triangle}
import org.sofa.nio.{IntBuffer}


/** A pie chart or cirle in 2D.
  *
  * This always create a complete circle, but allows to draw as many segments as requested.
  * Use the `draw(gl,segments)` method to draw only a part of the segments.
  *
  * The circle has radius 1 and lies in the plane XY. The center is at (0,0) and the first
  * segment is at (0.5,0) in cartesian coordinates. The segments goes up, in trigonometric
  * direction.
  *
  * This mesh is not dynamic.
  *
  * @param the number of segments of the circle (parts of the pie). */
class PieMesh(val segments:Int) extends Mesh {

	/** The mutable set of coordinates. Shortcut to the necessary mesh attribute. */
	protected[this] val V:MeshAttribute = createMesh
    
    /** Create the circle with the given set of segments. */
	protected def createMesh():MeshAttribute = {
		val attrib = addMeshAttribute(VertexAttribute.Vertex, 3)
		val data   = attrib.theData

		// Center

		data(0) = 0
		data(1) = 0
		data(2) = 0

		// Other points

		var v = 3
		var s = 0
		var a = 0f
		var angle = ((Pi*2) / segments).toFloat

		while(s <= segments) {		// <= since the second and last points are superposed.
			data(v+0) = cos(a).toFloat
			data(v+1) = sin(a).toFloat
			data(v+2) = 0f

			a += angle
			s += 1
			v += 3
		}

		attrib
	}

	// -- Mesh interface -----------------------------------------------------

	def vertexCount:Int = segments + 2

	def elementsPerPrimitive:Int = 1

	def drawAs(gl:SGL):Int = gl.TRIANGLE_FAN

    override def draw(gl:SGL, count:Int) {
    	if(va ne null) {
    		val s = if(count <= this.segments) count else segments
    		va.draw(drawAs(gl), 2 + s) 
    	} else {
    		throw new NoVertexArrayException("Mesh : create a vertex array first")
    	}
    }

    override def draw(gl:SGL, start:Int, count:Int) {
    	if(va ne null) {
    		throw new RuntimeException("Due to the way the pie is drawn, you cannot offset the start.")
    		// val beg = if(start <= this.segments) start else segments
    		// val end = if(beg+count <= this.segments) count else segments - beg
    		// va.draw(drawAs(gl), 2 + beg, end)
    	} else {
    		throw new NoVertexArrayException("Mesh : create a vertex array first")
    	}
    }

    override def hasElements():Boolean = false
}