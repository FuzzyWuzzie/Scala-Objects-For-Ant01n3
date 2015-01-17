package org.sofa.gfx.mesh

import org.sofa.nio.FloatBuffer
import org.sofa.gfx.{SGL,VertexArray,ShaderProgram}
import org.sofa.math.{Rgba, Point3}


/** A general mesh allowing to draw points.
  *
  * By default the mesh contains a Vertex position atttribute.
  * You can easily add a color vertex attribute and indices in
  * the vertex attributes.
  *
  * @param size The number of points. */
class PointsMesh(val gl:SGL, val size:Int) extends Mesh {
	
	/** The mutable set of coordinates. */
	protected var V = addAttributeVertex()
	
	/** The mutable set of colors. */
    protected var C:MeshAttribute =  _

	/** A set of indices. */
	protected var I:MeshElement = _
    
	// -- Mesh interface

	def vertexCount:Int = size

	def elementsPerPrimitive:Int = 1

	def drawAs():Int = gl.POINTS
	
	protected def addAttributeVertex():MeshAttribute = {
		if(V eq null) {
			V = addMeshAttribute(VertexAttribute.Vertex, 3)
		}

		V
	}

	/** Add a color vertex attribute to the mesh. */
	def addAttributeColor():MeshAttribute = {
		if(C eq null) {
			C = addMeshAttribute(VertexAttribute.Color, 4)
		}

		C
	}

	/** Add a set of indices into the vertex attributes. */
	def addIndices():MeshElement = {
		if(I eq null) {
			I = addMeshElement(size, 1)
		}

		I
	}

	// -- Edition interface ------------------------------------

	def setIndex(i:Int, p:Int) {
		val v = I.data

		v(i) = p

		I.range(i, i+1)
	}

	/** Move the `i`-th point at `p`. */
	def setPoint(i:Int, p:Point3) { setPoint(i, p.x.toFloat, p.y.toFloat, p.z.toFloat) }
	
	/** Move the `i`-th point at (`x`, `y`, `z`). */
	def setPoint(i:Int, x:Float, y:Float, z:Float) {
		val p = i*3
		val v = V.data

		v(p)   = x
		v(p+1) = y
		v(p+2) = z

		V.range(i, i+1)		
	}

	/** Change the `color` of the `i`-th point. */	
	def setColor(i:Int, color:Rgba) { 
		setColor(i, color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
	}
	
	/** Set the color (`red`, `green`, `blue`, `alpha`) of the `i`-th point. */
	def setColor(i:Int, red:Float, green:Float, blue:Float, alpha:Float) {
		if(C eq null) {
			throw new NoSuchVertexAttributeException("no color vertex attribute in points mesh, add it first")
		}

		val p = i*4
		val c = C.data

		c(p)   = red
		c(p+1) = green
		c(p+2) = blue
		c(p+3) = alpha

		C.range(i, i+1)
	}
	
	/** Get the coordinates of the `i`-th point. */
	def getPoint(i:Int):Point3 = { val p = i*3; val v = V.data; Point3(v(p), v(p+1), v(p+2)) }
	
	/** Get the color of th `i`-th point. */
	def getColor(i:Int):Rgba = { val p = i*4; val c = V.data; Rgba(c(p), c(p+1), c(p+2), c(p+3)) }
}