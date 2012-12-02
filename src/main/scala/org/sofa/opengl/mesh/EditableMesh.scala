package org.sofa.opengl.mesh

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import org.sofa.nio.FloatBuffer
import org.sofa.nio.IntBuffer

import javax.media.opengl.GL._
import javax.media.opengl.GL2._

case class BadlyNestedBeginEnd(msg:String) extends Throwable(msg) {
	def this() { this("Badly nested begin()/end()") }
}

object MeshDrawMode extends Enumeration {
	val TRIANGLES = Value(GL_TRIANGLES)
	val QUADS     = Value(GL_QUADS)
}

class EditableMesh extends Mesh {
	import MeshDrawMode._
	
	protected var drawMode = TRIANGLES
	
	protected var indexBuffer:ArrayBuffer[Int] = null
	
	protected val vertexBuffer = new MeshBuffer("vertex", 3)
	
	protected val otherBuffers:HashMap[String,MeshBuffer] = new HashMap[String,MeshBuffer]()
	
	protected var beganVertex = false
	
	protected var beganIndex = false
	
	// --------------------------------------------------------------
	// Access
	
	override def drawAs():Int = drawMode.id
	
	// --------------------------------------------------------------
	// Command, mesh building

	/** Start to build the primitive. 
	  * The draw mode for the primitive must be given here. Any previous values are deleted. */
	def begin(drawMode:MeshDrawMode.Value) {
		if(beganVertex) throw new BadlyNestedBeginEnd("cannot nest begin() calls");
		
		otherBuffers.clear
		vertexBuffer.clear
		nioCache.clear

		this.drawMode = drawMode
		
		beganVertex = true
	}

	/** Same as calling begin(MeshDrawMode), a code that calls vertex, color, normal, etc. and a final call to en(). */
	def buildAttributes(drawMode:MeshDrawMode.Value)(code: => Unit) {
		begin(drawMode)
		code
		end
	}
	
	/** Add a vertex to the primitive. If other attributes were declared but not changed
	  * the vertex take the last value specified for these attributes. */
	def vertex(x:Float, y:Float, z:Float) {
		if(!beganVertex) throw new BadlyNestedBeginEnd
		vertexBuffer.append(x, y, z)
		otherBuffers.foreach { _._2.sync(vertexBuffer) }
	}
	
	/** Specify the color for the next vertex or vertices. */
	def color(red:Float, green:Float, blue:Float, alpha:Float) { attribute("color", red, green, blue, alpha) }
	
	/** Specify the color for the next vertex or vertices. */
	def color(red:Float, green:Float, blue:Float) { color(red, green, blue, 1) }
	
	/** Specify the normal for the next vertex or vertices. */
	def normal(x:Float, y:Float, z:Float) { attribute("normal", x, y, z) }
	
	/** Specify the tangent for the next vertex or vertices. */
	def tangent(x:Float, y:Float, z:Float) { attribute("tangent", x, y, z) }
	
	/** Specify the texture coordinates for the next vertex or vertices. */
	def texCoord(u:Float, v:Float) { attribute("texcoord", u, v) }
	
	/** Specify the bone index for the next vertex or vertices. */
	def bone(b:Int) { attribute("bone", b) }
	
	/** Specify an arbitrary attribute for the next vertex or vertices. */
	def attribute(name:String, values:Float*) {
		if(!beganVertex) throw new BadlyNestedBeginEnd
		val buffer = otherBuffers.getOrElseUpdate(name, { new MeshBuffer(name, values.size, vertexBuffer, values:_*) })
		
		if(buffer.elements == vertexBuffer.elements) {
//Console.err.println("attribute(%s) appending %s".format(name, values));
			buffer.append(values:_*)
		}
	}
	
	/** End the building of the primitive. Any new call the begin() will
	  * erase the previous primitive. You can then call methods to create
	  * (or update) a vertex array from the primitives. */
	def end() {
		if(!beganVertex) throw new BadlyNestedBeginEnd
		beganVertex = false
	}
	
	def beginIndices() {
		if(beganIndex) throw new BadlyNestedBeginEnd("cannot nest beginIndices() calls");
		beganIndex = true
		indexNioCache = null
		indexBuffer = new ArrayBuffer[Int]()
	}

	/** Like a call to beginIndices(), calls to index(Int) inside the given code and a call to endIndices(). */
	def buildIndices(code: => Unit) {
		beginIndices
		code
		endIndices
	}
	
	def index(i:Int) {
		if(!beganIndex) throw new BadlyNestedBeginEnd
		indexBuffer += i
	}
	
	def endIndices() {
		if(!beganIndex) throw new BadlyNestedBeginEnd
		beganIndex = false
	}

	// --------------------------------------------------------------
	// Access, Mesh interface

	/** Cache of NIO float buffers. */
	protected val nioCache:HashMap[String,FloatBuffer] = new HashMap[String,FloatBuffer]()
    
	protected var indexNioCache:IntBuffer = null
	
    protected def getCache(name:String):FloatBuffer = nioCache.getOrElseUpdate(name,
    		{ new FloatBuffer(otherBuffers.get(name).getOrElse(throw new RuntimeException("no %s attributes in this mesh".format(name))).buffer) } )
	
	def vertices:FloatBuffer = nioCache.getOrElseUpdate("vertex", { new FloatBuffer(vertexBuffer.buffer) } )

    override def normals:FloatBuffer = getCache("normal")

    override def tangents:FloatBuffer = getCache("tangent")

    override def colors:FloatBuffer = getCache("color")

    override def texCoords:FloatBuffer = getCache("texcoord")
 
    override def bones:IntBuffer = throw new RuntimeException("TODO")//getCache("bone")

    override def indices:IntBuffer = {
		if(indexBuffer ne null) {
			if(indexNioCache eq null)
				indexNioCache = new IntBuffer(indexBuffer)
			indexNioCache 
		} else {
			throw new RuntimeException("no indices for this mesh") 
		}
    }

    override def hasNormals:Boolean = otherBuffers.contains("normal")
    
    override def hasTangents:Boolean = otherBuffers.contains("tangent")
    
    override def hasColors:Boolean = otherBuffers.contains("color")
    
    override def hasTexCoords:Boolean = otherBuffers.contains("texcoord")

    override def hasBones:Boolean = otherBuffers.contains("bone")
    
    override def hasIndices:Boolean = (indexBuffer ne null)
}

/** A buffer of elements each made of `components` float values.
  * 
  * Therefore there are components*elements float values in the buffer. */
protected class MeshBuffer(val name:String, val components:Int, other:MeshBuffer, values:Float*) {
	// --------------------------------------------------------------
	// Attributes
	
	/** The buffer. */
	val buffer = new ArrayBuffer[Float]
	
	/** Number of elements in the buffer (tuples of components values). */
	var elements = 0
	
	/** Temporary set of values (one element). */
	val temp = new Array[Float](components)
	
	// --------------------------------------------------------------
	// Construction

	/** New empty buffer. */
	def this(name:String, comps:Int) { this(name, comps, null)  }
	
	syncWithValues(other, values:_*)
	
	/** Fill the buffer by appending the given values (see `append()`), until this
	  * buffer has as many elements as the `other` buffer. */
	protected def syncWithValues(other:MeshBuffer, values:Float*) {
//Console.err.println("new buffer %s other=%s values=%s".format(name, other, values));
		if(other ne null) {
			while(elements < other.elements) {
				append(values:_*)
				elements += 1
			}
		}
	}
	
	// --------------------------------------------------------------
	// Commands

	/** Remove all the elements. */
	def clear() {
		elements = 0
		buffer.clear
	}
	
	/** Fill the buffer with the last components added until it has a number of elements
	  * identical with the `other`.  */
	def sync(other:MeshBuffer) {
		val n = ((elements-1) * components)
		var i = 0
		
		while(i < components) {
			temp(i) = buffer(n+i); i += 1
		}
		
		while(elements < other.elements) {
			i = 0
			while(i < components) {
				buffer += temp(i); i += 1
			}
			elements += 1
		}
	}
	
	/** Append a set of values. This always appends exactly the number of values
	  * given by `components`. If there are no enough `values`, zeroes are appended.
	  * If there are too many `values` they are ignored. */
	def append(values:Float*) {
		var i = 0
		while(i < components) {
			buffer += (if(values.size > i) values(i) else 0)
			i += 1
		}
		elements += 1
	}
	
	override def toString():String = "Buffer(%s: %d elt, %d comps)".format(name, elements, components)
}