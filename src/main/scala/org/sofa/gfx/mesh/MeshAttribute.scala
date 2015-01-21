package org.sofa.gfx.mesh

import scala.language.implicitConversions

import org.sofa.FileLoader
import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.gfx.io.collada.ColladaFile

import scala.collection.mutable.HashMap


/** Representation of a vertex attribute.
  * 
  * Such attributes are set of floats (1 to 4, depending on the number of components),
  * each one being associated to a vertex. You can index the attribute by the
  * vertex number. Individual meshes have to allocate these
  * attribute by giving the number of vertices and the number of components per
  * vertice. This encapsulate a [[ArrayBuffer]] and gives access to a [[FloatBuffer]]
  * used to update the array buffer.
  *
  * This class is abstract and the float buffer
  * may be a specifically allocated memory area in client memory, but it may also
  * be memory mapped from OpenGL where you have direct access to the array buffer.
  * Therefore, before doing any modifications you must call `begin`, end before
  * using the attribute (and the mesh) do draw or do something with OpenGL you must
  * call `end`. */
abstract class MeshAttribute(val gl:SGL, val name:String, val components:Int, val vertexCount:Int, var divisor:Int = -1) {

	/** Release any GL memory. The object is no more usable after this. */
	def dispose()

	/** The OpenGL array buffer associated to the data. */
	def arrayBuffer:ArrayBuffer

	/** True between `begin` and `end` calls. */
	def modifiable:Boolean

	/** Enter a mode where modification is possible on the attribute. */
	def begin()

	/** End the modification mode, ensure data is sent to OpenGL. */
	def end()

	/** The data associated to this attribute. This is null outside of a `begin` - `end`
	  * pair of calls. Commands like `set` and `copy` are usable only inside `begin` and
	  * `end`. */
	def data:FloatBuffer

	/** Change [[components]] values at `vertex` in the buffer. This is usable only
	  * between calls to `begin` and `end`.
	  * @param values must contain at least [[components]] elements.
	  * @param vertex an index as a vertex number. */
	def set(vertex:Int, values:Float*)

	def set1(vertex:Int, value:Float)

	def set2(vertex:Int, a:Float, b:Float)

	def set3(vertex:Int, a:Float, b:Float, c:Float)

	def set4(vetex:Int, a:Float, b:Float, c:Float, d:Float)

	/** Copy the data from the set of `values` given. The number of values must match
	  * the size of the attribute. This is usable only between calls to `begin` and `end`. */
	def copy(values:Float*)

	/** Copy the data from the set of `values` given. The number of values must match
	  * the size of the attribute. This is usable only between calls to `begin` and `end`. */
	def copy(values:Array[Float])

	/** Copy the data from the set of `values` given. The number of values must match
	  * the size of the attribute. This is usable only between calls to `begin` and `end`. */
	def copy(values:scala.collection.mutable.ArrayBuffer[Float])

	/** Elements in the given range changed. The indices are thos of the vertices,
	  * not the individual components of the vertices (in other words, it does not
	  * matter if the attribute contains two components (UV), three (positions), or
	  * four (colors)). */
	def range(from:Int, to:Int)
}


// -- By Copy -----------------------------------------------------------------------------------------


/** A mesh attribute that stores data in client memory and send it to OpenGL
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
class MeshAttributeCopy(gl:SGL, name:String, components:Int, vertexCount:Int, divisor:Int = -1)
	extends MeshAttribute(gl, name, components, vertexCount, divisor) {
	
	/** The data ! */
	val theData = FloatBuffer(vertexCount * components)

	/** The associated array buffer. */
	var arrayBuffer:ArrayBuffer = null

	/** Start position (included) of the last modification. */
	var b:Int = vertexCount

	/** End position (not included) of the last modification. */
	var e:Int = 0

	protected var updateOk = false

	def dispose() {
		if(arrayBuffer ne null) arrayBuffer.dispose()
		arrayBuffer = null
	}

	def modifiable:Boolean = updateOk

	def begin() {
		if(arrayBuffer eq null) {
			arrayBuffer = new ArrayBuffer(gl, components, components * vertexCount * 4, gl.FLOAT, gl.DYNAMIC_DRAW)
		}
		updateOk = true
	}

	def end() {
		if(e > b) {
			if(b == 0 && e == vertexCount)
			     arrayBuffer.update(theData)
			else arrayBuffer.update(b, e, theData)

			b = vertexCount
			e = 0
		}
		updateOk = false
	}

	/** Data under the form a float buffer. */
	def data:FloatBuffer = theData

	/** Change [[components]] values at `vertex` in the buffer. The `setX` variants
	  * are faster. You should use them if possible.
	  * @param values must contain at least [[components]] elements.
	  * @param vertex an index as a vertex number. */
	def set(vertex:Int, values:Float*) {
		val i = vertex * components

		if(values.length >= components) {
			if(i >= 0 && i < theData.size) {
				if(b > vertex)   b = vertex
				if(e < vertex+1) e = vertex+1

				var j = 0

				while(j < components) {
					theData(i+j) = values(j)
					j += 1
				}
			} else {
				throw new InvalidVertexException(s"invalid vertex ${vertex} out of attribute buffer (size=${vertexCount})")
			}
		} else {
			throw new InvalidVertexComponentException(s"no enough values passed for attribute (${values.length}), needs ${components} components")
		}
	}

	def set1(vertex:Int, value:Float) {
		if(components != 1)
			throw new InvalidVertexException(s"call set1 with mesh attributes having 1 component per vertex")

		range(vertex, vertex+1)
		theData(vertex) = value
	}

	def set2(vertex:Int, x:Float, y:Float) {
		if(components != 2)
			throw new InvalidVertexException(s"call set2 with mesh attributes having 2 components per vertex")

		range(vertex, vertex+1)

		val i = vertex * components

		theData(i+0) = x
		theData(i+1) = y
	}

	def set3(vertex:Int, x:Float, y:Float, z:Float) {
		if(components != 3)
			throw new InvalidVertexException(s"call set3 with mesh attributes having 3 components per vertex")

		range(vertex, vertex+1)

		val i = vertex * components

		theData(i+0) = x
		theData(i+1) = y
		theData(i+2) = z
	}

	def set4(vertex:Int, x:Float, y:Float, z:Float, w:Float) {
		if(components != 4)
			throw new InvalidVertexException(s"call set4 with mesh attributes having 4 components per vertex")

		range(vertex, vertex+1)

		val i = vertex * components

		theData(i+0) = x
		theData(i+1) = y
		theData(i+2) = z
		theData(i+3) = w
	}

	/** Copy the data from the set of `values` given. The number of values must match the size of the attribute. */
	def copy(values:Float*) {
		if(values.length >= vertexCount*components) {
			theData.copy(values.asInstanceOf[scala.collection.mutable.WrappedArray[Float]].array)
		}
		else throw new RuntimeException("use copy with exactly the correct number of arguments")
		range(0, vertexCount)
	}

	def copy(values:Array[Float]) {
		if(values.length >= vertexCount*components) {
			theData.copy(values)
		}
		else throw new RuntimeException("use copy with exactly the correct number of arguments")
		range(0, vertexCount)
	}

	def copy(values:scala.collection.mutable.ArrayBuffer[Float]) {
		if(values.length >= vertexCount*components) {
			theData.copy(values)
		}
		else throw new RuntimeException("use copy with exactly the correct number of arguments")
		range(0, vertexCount)
	}

	/** Indicate the range of changed elements if the data is accessed freely throught the
	  * pointer returned by `data()`. If you do not call this, the data may not be sent
	  * to OpenGL. Using `copy()`, `set()`, `setX()` does not require to use this method.
	  * You can call this method several times with several ranges. The data will be sent
	  * to the GL only when `end()` is called. */
	def range(from:Int, to:Int) {
		if(from < b) b = from
		if(to   > e) e = to
	}
}


// -- By Mapping -------------------------------------------------------------------------------------


/** TODO */