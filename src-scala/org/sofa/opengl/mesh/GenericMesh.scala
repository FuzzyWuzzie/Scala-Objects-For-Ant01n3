package org.sofa.opengl.mesh

import org.sofa.opengl.SGL
import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import org.sofa.nio.FloatBuffer
import org.sofa.nio.IntBuffer
import scala.collection.mutable.ArrayBuffer

/** Various mesh draw modes.
  * 
  * These mode identifiers map to the corresponding OpenGL constant. */
object MeshDrawMode extends Enumeration {
	val LINES = Value(GL_LINES)
	val LINE_LOOP = Value(GL_LINE_LOOP)
	val LINE_STRIP = Value(GL_LINE_STRIP)
	val TRIANGLES = Value(GL_TRIANGLES)
	val TRIANGLE_STRIP = Value(GL_TRIANGLE_STRIP)
	val QUADS = Value(GL_QUADS)
}

class GenericMesh extends Mesh {
	import MeshDrawMode._
	
	// ----------------------------------------------------------------
	// Attribute
	
	protected val drawMode:MeshDrawMode.Value = TRIANGLES

	protected var vertexBuffer:ArrayBuffer[Float] = null
	
	protected var colorBuffer:ArrayBuffer[Float] = null
	
	protected var normalBuffer:ArrayBuffer[Float] = null
	
	protected var tangentBuffer:ArrayBuffer[Float] = null
	
	protected var texCoordBuffer:ArrayBuffer[Float] = null
	
	protected var boneBuffer:ArrayBuffer[Int] = null
	
	protected var indexBuffer:ArrayBuffer[Int] = null

	protected var began = false
	
	// ----------------------------------------------------------------
	// Access
	
	override def drawAs():Int = drawMode.id
	
	// ----------------------------------------------------------------
	// Command, mesh building
	
	def begin() {
		if(began) throw new RuntimeException("canno nest begin() calls") 
		
		vertexBuffer = null
		colorBuffer = null
		normalBuffer = null
		tangentBuffer = null
		texCoordBuffer = null
		boneBuffer = null
		
		began = true
	}
	
	def vertex(x:Float, y:Float, z:Float) {
		if(! began) throw new RuntimeException("use begin() before issuing attributes")
		if(vertexBuffer eq null) vertexBuffer = new ArrayBuffer[Float]()
		
		vertexBuffer += x
		vertexBuffer += y
		vertexBuffer += z
	}
	
	def color(red:Float, green:Float, blue:Float, alpha:Float) {
		if(! began) throw new RuntimeException("use begin() before issuing attributes")
		if(colorBuffer eq null) colorBuffer = new ArrayBuffer[Float]()
		
		colorBuffer += red
		colorBuffer += green
		colorBuffer += blue
		colorBuffer += alpha
	}
	
	def color(red:Float, green:Float, blue:Float) { color(red, green, blue, 1) }
	
	def texCoord(u:Float, v:Float) {
		if(! began) throw new RuntimeException("use begin() before issuing attributes")
		if(texCoordBuffer eq null) texCoordBuffer = new ArrayBuffer[Float]()
		
		texCoordBuffer += u
		texCoordBuffer += v
	}
	
	def normal(x:Float, y:Float, z:Float) {
		if(! began) throw new RuntimeException("use begin() before issuing attributes")
		if(normalBuffer eq null) normalBuffer = new ArrayBuffer[Float]()
		
		normalBuffer += x
		normalBuffer += y
		normalBuffer += z
	}
	
	def tangent(x:Float, y:Float, z:Float) {
		if(! began) throw new RuntimeException("use begin() before issuing attributes")
		if(tangentBuffer eq null) tangentBuffer = new ArrayBuffer[Float]()
		
		tangentBuffer += x
		tangentBuffer += y
		tangentBuffer += z
	}
	
	def bone(b:Int) {
		if(! began) throw new RuntimeException("use begin() before issuing attributes")
		if(boneBuffer eq null) boneBuffer = new ArrayBuffer[Int]()
		
		boneBuffer += b
	}
	
	def end() {
		if(! began) throw new RuntimeException("end() call must be preceded by a begin() call");
		began = false
	}
	
	def beginIndices() {
		indexBuffer = null		
	}
	
	def indice(i:Int) {
		if(indexBuffer eq null) indexBuffer = new ArrayBuffer[Int]()
		
		indexBuffer += i
	}
	
	def endIndices() {
	}
	
	// ------------------------------------------------------------------------
	// Mesh
	
	override def hasNormals:Boolean = ((normalBuffer ne null) && (normalBuffer.size > 0))
	
	override def hasTangents:Boolean = ((tangentBuffer ne null) && (tangentBuffer.size > 0))
	
	override def hasColors:Boolean = ((colorBuffer ne null) && (colorBuffer.size > 0))
	
	override def hasTexCoords:Boolean = ((texCoordBuffer ne null) && (texCoordBuffer.size > 0))
	
	override def hasBones:Boolean = ((boneBuffer ne null) && (boneBuffer.size > 0))

	def vertices:FloatBuffer = if((vertexBuffer ne null) && (vertexBuffer.size > 0)) new FloatBuffer(vertexBuffer) else throw new RuntimeException("no vertices defined in this mesh")
	
	override def normals:FloatBuffer = if(hasNormals) new FloatBuffer(normalBuffer) else throw new RuntimeException("no normals defined in this mesh")
	
	override def tangents:FloatBuffer = if(hasTangents) new FloatBuffer(tangentBuffer) else throw new RuntimeException("no tangents defined in this mesh")
	 
	override def colors:FloatBuffer = if(hasColors) new FloatBuffer(colorBuffer) else throw new RuntimeException("no colors defined in this mesh")
	
	override def texCoords:FloatBuffer = if(hasTexCoords) new FloatBuffer(texCoordBuffer) else throw new RuntimeException("no texture coordinates defined in this mesh")
	
	override def bones:IntBuffer = if(hasBones) new IntBuffer(boneBuffer) else throw new RuntimeException("no bones defined in this mesh")
	
	override def indices:IntBuffer = if((indices ne null) && (indices.size > 0)) new IntBuffer(indexBuffer) else throw new RuntimeException("no indices defined in this mesh")
}