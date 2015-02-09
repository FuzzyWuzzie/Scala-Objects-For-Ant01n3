package org.sofa.gfx.mesh.shapes

import scala.math._

import org.sofa.gfx.mesh._
import org.sofa.gfx.{SGL, VertexArray, ShaderProgram}
import org.sofa.math.{Rgba, Point3, Vector3, NumberSeq2, NumberSeq3, Triangle}
import org.sofa.nio.{IntBuffer}


/** A pie chart or filled cirle in 2D.
  *
  * This always create a complete circle, but allows to draw as many segments as requested.
  * Use the `draw(gl,segments)` method to draw only a part of the segments.
  *
  * The circle has radius 1 by default (or pass a value to `radius` at build time) and lies in
  * the plane XY by default (set `XZ` to true at build time). The center is at (0,0) and the
  * first segment is at (radius,0) in cartesian coordinates. The segments go in trigonometric
  * direction.
  *
  * This mesh is not dynamic.
  *
  * @param the number of segments of the circle (parts of the pie). */
class PieMesh(val gl:SGL, val segments:Int, val radius:Double = 1.0, XZ:Boolean = false) extends Mesh {

	/** The mutable set of coordinates. Shortcut to the necessary mesh attribute. */
	protected var V:MeshAttribute = addAttributeVertex

	protected var C:MeshAttribute = _
    
    /** Create the circle with the given set of segments. */
	protected def addAttributeVertex():MeshAttribute = {
		if(V eq null) {
			V = addMeshAttribute(VertexAttribute.Position, 3)

			V.begin

			val data = V.data

			// Center

			data(0) = 0
			data(1) = 0
			data(2) = 0

			// Other points

			var v = 3
			var s = 0
			var a = 0f
			val angle = ((Pi * 2) / segments).toFloat
			val o1 = if(XZ) 2 else 1
			val o2 = if(XZ) 1 else 2

			while(s <= segments) {		// <= since the second and last points are superposed.
				data(v)    = (radius * cos(a)).toFloat
				data(v+o1) = (radius * sin(a)).toFloat
				data(v+o2) = 0f

				a += angle
				s += 1
				v += 3
			}

			V.range(0, vertexCount)
			V.end
		}

		V
	}

	/** Add a color attribute, all vertices are black. */
	def addAttributeColor():MeshAttribute = {
		if(C eq null) {
			C = addMeshAttribute(VertexAttribute.Color, 4)
		}

		C
	}

	/** Change the `color` of the whole mesh. */
	def setColor(color:Rgba) {
		if(C eq null)
			throw new NoSuchVertexAttributeException("no color attribute, add one before")

		var i = 0
		val n = vertexCount * 4
		val d = C.data

		while(i < n) {
			d(i+0) = color.red.toFloat
			d(i+1) = color.green.toFloat
			d(i+2) = color.blue.toFloat
			d(i+3) = color.alpha.toFloat
			i += 4
		}

		C.range(0, vertexCount)
	}

	/** Change the `color` of `vertex`. */
	def setVertexColor(vertex:Int, color:Rgba) {
		if(C eq null)
			throw new NoSuchVertexAttributeException("no color attribute, add one before")

		val i = vertex * C.components
		val d = C.data

		d(i+0) = color.red.toFloat
		d(i+1) = color.green.toFloat
		d(i+2) = color.blue.toFloat
		d(i+3) = color.alpha.toFloat

		C.range(vertex, vertex+1)
	}

	def setCenterColor(color:Rgba) {
		if(C eq null)
			throw new NoSuchVertexAttributeException("no color attribute, add one before")

		val d = C.data

		d(0) = color.red.toFloat
		d(1) = color.green.toFloat
		d(2) = color.blue.toFloat
		d(3) = color.alpha.toFloat

		C.range(0, 1)		
	}

	// -- Mesh interface -----------------------------------------------------

	def vertexCount:Int = segments + 2

	def elementsPerPrimitive:Int = 1

	def drawAs():Int = gl.TRIANGLE_FAN

    override def draw(count:Int) {
    	if(va ne null) {
    		val s = if(count <= this.segments) count else segments
    		va.draw(drawAs, 2 + s) 
    	} else {
    		throw new NoVertexArrayException("Mesh : create a vertex array first")
    	}
    }

    override def draw(start:Int, count:Int) {
    	if(va ne null) {
    		throw new RuntimeException("Due to the way the pie is drawn, you cannot offset the start.")
    		// val beg = if(start <= this.segments) start else segments
    		// val end = if(beg+count <= this.segments) count else segments - beg
    		// va.draw(drawAs(gl), 2 + beg, end)
    	} else {
    		throw new NoVertexArrayException("Mesh : create a vertex array first")
    	}
    }
}