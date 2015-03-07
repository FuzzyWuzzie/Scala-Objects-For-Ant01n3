package org.sofa.gfx.mesh

import scala.language.implicitConversions

import org.sofa.Loader
import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.gfx.io.collada.ColladaFile

import scala.collection.mutable.HashMap


/** Representation of indices to draw primitives.
  *
  * Such indices are set of primitives constituted of one (points),
  * two (lines), three (triangles) indices in vertex attributes. 
  * A primitive contains `verticesPerPrim` indices.
  *
  * This encapsulates a 
  */
abstract class MeshElement(val gl:SGL, val primCount:Int, val verticesPerPrim:Int) {

	/** Release any GL memory. The object is no more usable after this. */
	def dispose()
	
	/** The OpenGL element buffer associated to the data. */
	def elementBuffer:ElementBuffer

	/** True between `begin` and `end` calls. */
	def modifable:Boolean

	/** Enter a mode where modifications is possible on the attribute. */
	def begin()

	/** End the modification mode, ensure data is sent to OpenGL. */
	def end()

	/** The data associated to this attribute. This is null outside of a `begin` - `end`
	  * pair of calls. Commands like `set` and `copy` are usable only inside `begin` and
	  * `end`. */
	def data:IntBuffer

	/** Change [[verticesPerPrim]] values at `prim` in the buffer. This
	  * is usable only between calls to `begin` and `end`.
	  * @param values must contain at least [[verticesPerPrim]] elements.
	  * @param prim an index as a primitive number. */
	def set(prim:Int, values:Int*)

	def set1(prim:Int, value:Int)

	def set2(prim:Int, a:Int, b:Int)

	def set3(prim:Int, a:Int, b:Int, c:Int)

	def set4(prim:Int, a:Int, b:Int, c:Int, d:Int)

	/** Copy the data from the set of `values` given. The number of
	  * values must match the size of the attribute. */
	def copy(values:Int*)

	/** Copy the data from the set of `values` given. The number of
	  * values must match the size of the attribute. */
	def copy(values:Array[Int])

	/** Copy the data from the set of `values` given. The number of
	  * values must match the size of the attribute. */
	def copy(values:scala.collection.mutable.ArrayBuffer[Int])

	/** Primitives in the given range changed, the indices are those
	  * of the primitves, not elements constituting the primitives (in
	  * other words, it does not matter if there are one element (points)
	  * two elements (lines) or three elements (triangles) in the primitive,
	  * you count in points, lines or triangles). */
	def range(from:Int, to:Int)

}


// -- By Copy -----------------------------------------------


/** A mesh element buffer that stores data in client memory and sent it to OpenGL
  * each time `end` is called.
  *
  * This is a basic way of working with data, but this allocates data twice, once
  * in client memory and once in OpenGL memory. Then after each sequence of change,
  * the data must be sent out to the GL by copy, hence the name. If you do not use
  * the data often, this may be easier to manage.
  *
  * To avoid sending all the data to the GL after each modification, a begin and end
  * index for modifications is stored.
  */
class MeshElementCopy(gl:SGL, primCount:Int, verticesPerPrim:Int) extends MeshElement(gl, primCount, verticesPerPrim) {
	
	/** The data ! */
	var theData = IntBuffer(primCount * verticesPerPrim)

	/** The associated element buffer. */
	var elementBuffer:ElementBuffer = null

	/** Start position (included) of the last modification. */
	var b:Int = primCount * verticesPerPrim

	/** End position (not included) of the last modification. */
	var e:Int = 0

	protected var updateOk = false

	def dispose() {
		if(elementBuffer ne null) elementBuffer.dispose()
		elementBuffer = null
	}

	def modifable:Boolean = updateOk

	def begin() {
		if(elementBuffer eq null) {
			elementBuffer = new ElementBuffer(gl, primCount * verticesPerPrim, gl.DYNAMIC_DRAW)
		}
		updateOk = true
	}

	def end() {
		if(e > b) {
			if(b == 0 && e == primCount)
				 elementBuffer.update(theData)
			else elementBuffer.update(b*verticesPerPrim, e*verticesPerPrim, theData)
			
			b = primCount
			e = 0
		}
		updateOk = false
	}

	/** Data under the form an int buffer. */
	def data:IntBuffer = theData

	def set(prim:Int, values:Int*) {
		val i = prim * verticesPerPrim

		if(values.length >= verticesPerPrim) {
			if(i >= 0 && i < theData.size) {
				if(b > prim)   b = prim
				if(e < prim+1) e = prim+1

				var j = 0

				while(j < verticesPerPrim) {
					theData(i+j) = values(j)
					j += 1
				}
			} else {
				throw new InvalidPrimitiveException(s"invalid primitive index ${prim} out of index buffer (size=${primCount})")
			}
		} else {
			throw new InvalidPrimitiveVertexException(s"no enough values passed for primitive (${values.length}), needs ${verticesPerPrim} vertices indices")
		}
	}

	def set1(prim:Int, value:Int) {
		if(verticesPerPrim != 1)
			throw new InvalidPrimitiveException(s"call set1 with elements buffer having 1 vertex per primitive")

		range(prim, prim+1)
		theData(prim) = value
	}
	
	def set2(prim:Int, x:Int, y:Int) {
		if(verticesPerPrim != 2)
			throw new InvalidPrimitiveException(s"call set2 with elements buffer having 2 vertices per primitive")

		range(prim, prim+1)

		val i = prim * verticesPerPrim

		theData(i+0) = x
		theData(i+1) = y
	}
	
	def set3(prim:Int, x:Int, y:Int, z:Int) {
		if(verticesPerPrim != 3)
			throw new InvalidPrimitiveException(s"call set3 with elements buffer having 2 vertices per primitive")

		range(prim, prim+1)

		val i = prim * verticesPerPrim

		theData(i+0) = x
		theData(i+1) = y
		theData(i+2) = z
	}
	
	def set4(prim:Int, x:Int, y:Int, z:Int, w:Int) {
		if(verticesPerPrim != 4)
			throw new InvalidPrimitiveException(s"call set4 with elements buffer having 2 vertices per primitive")

		range(prim, prim+1)

		val i = prim * verticesPerPrim

		theData(i+0) = x
		theData(i+1) = y
		theData(i+2) = z
		theData(i+3) = w
	}

	/** Copy the data from the set of `values` given. The number of values must match the size of the attribute. */
	def copy(values:Int*) {
		if(values.length >= primCount*verticesPerPrim)
			theData.copy(values.asInstanceOf[scala.collection.mutable.WrappedArray[Int]].array)
		else throw new RuntimeException("use copy with exactly the correct number of arguments")
		range(0, primCount)
	}

	def copy(values:Array[Int]) {
		if(values.length >= primCount*verticesPerPrim)
			theData.copy(values)
		else throw new RuntimeException("use copy with exactly the correct number of arguments")
		range(0, primCount)
	}

	/** Copy the data from the set of `values` given. The number of
	  * values must match the size of the attribute. */
	def copy(values:scala.collection.mutable.ArrayBuffer[Int]) {
		if(values.length >= primCount*verticesPerPrim)
			theData.copy(values)
		else throw new RuntimeException("use copy with exactly the correct number of arguments")
		range(0, primCount)
	}

	def range(from:Int, to:Int) {
		if(from < b) b = from
		if(to   > e) e = to
	}
}