package org.sofa.opengl.mesh

import org.sofa.opengl.{SGL, VertexArray, ShaderProgram}
import org.sofa.math.{Rgba, Point3, Vector3, NumberSeq2, NumberSeq3, Triangle}
import org.sofa.nio.{IntBuffer, FloatBuffer}
import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._

/** A dynamic set of quads that can be updated and tries to send only changed
  * informations to the GL.
  *
  * There are `size` quads at max in the mesh. */
class QuadsMesh(val size:Int) extends Mesh {
	
	/** The mutable set of coordinates. */
	protected lazy val V:FloatBuffer = new FloatBuffer(size*3*4)
	
	/** The mutable set of colors. */
	protected lazy val C:FloatBuffer = new FloatBuffer(size*4*4)
	
	/** The mutable set of normals, changes with the quads. */
	protected lazy val N:FloatBuffer = new FloatBuffer(size*3*4)

	/** The mutable set of texture coordinates, changes with the quads. */
	protected lazy val T:FloatBuffer = new FloatBuffer(size*2*4)
	
	/** The mutable set of elements to draw. */
	protected lazy val I:IntBuffer = new IntBuffer(size*4)
	
	/** Start position of the last modification inside the index array. */
	protected var ibeg = size
	
	/** End position of the last modification inside the index array. */
	protected var iend = 0
	
    /** Start position of the last modification inside the coordinates array. */
    protected var vbeg = size
    
    /** End position of the last modification inside the coordinates array. */
    protected var vend = 0
    
    /** Start position of the last modification inside the normal array. */
    protected var nbeg = size
    
    /** End position of the last modification inside the normal array. */
    protected var nend = 0
    
    /** Start position of the last modification inside the texcoords array. */
    protected var tbeg = size
    
    /** End position of the last modification inside the texcoords array. */
    protected var tend = 0
    
    /** Start position of the last modification inside the color array. */
    protected var cbeg = size
    
    /** End position of the last modification inside the color array. */
    protected var cend = 0
    	
	// -- Mesh interface -----------------------------------------------------

	def attribute(name:String):FloatBuffer = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex   => V
			case VertexAttribute.Normal   => N
			case VertexAttribute.TexCoord => T
			case VertexAttribute.Color    => C
			case _                        => throw new RuntimeException("this mesh has no attribute %s".format(name))
		}
	}

	override def indices:IntBuffer = I

	def attributeCount():Int = 4

	def attributes():Array[String] = Array[String](
				VertexAttribute.Vertex.toString,
				VertexAttribute.Normal.toString,
				VertexAttribute.TexCoord.toString,
				VertexAttribute.Color.toString)
	
	def components(name:String):Int = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex   => 3
			case VertexAttribute.Normal   => 3
			case VertexAttribute.TexCoord => 2
			case VertexAttribute.Color    => 4
			case _                        => throw new RuntimeException("this mesh has no attribute %s".format(name))
		}

	}

	def has(name:String):Boolean = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex   => true
			case VertexAttribute.Normal   => true
			case VertexAttribute.TexCoord => true
			case VertexAttribute.Color    => true
			case _                        => false
		}
	}

    override def hasIndices():Boolean = true

	def drawAs():Int = GL_QUADS

	// -- Constructive interface ---------------------------------------------------
	
	def setPoint(i:Int, p:NumberSeq3) { setPoint(i, p.x.toFloat, p.y.toFloat, p.z.toFloat) }

	def setPoint(i:Int, x:Float, y:Float, z:Float) {
		val p = i*3
		
		V(p)   = x
		V(p+1) = y
		V(p+2) = z
		
		if(i < vbeg) vbeg = i
		if(i+1 > vend) vend = i+1
	}
	
	def setPointColor(i:Int, c:Rgba) { setPointColor(i, c.red.toFloat, c.green.toFloat, c.blue.toFloat, c.alpha.toFloat) }
	
	def setPointColor(i:Int, red:Float, green:Float, blue:Float, alpha:Float) {
		val p = i*4
		
		C(p  ) = red
		C(p+1) = green
		C(p+2) = blue
		C(p+3) = alpha
		
		if(i < cbeg) cbeg = i
		if(i+1 > cend) cend = i + 1
	}
	
	def setPointNormal(i:Int, n:NumberSeq3) { setPointNormal(i, n.x.toFloat, n.y.toFloat, n.z.toFloat) }
	
	def setPointNormal(i:Int, x:Float, y:Float, z:Float) {
		val p = i*3
		
		N(p)   = x
		N(p+1) = y
		N(p+2) = z
		
		if(i < nbeg) nbeg = i
		if(i+1 > nend) nend = i+1
	}

	def setPointTexCoord(i:Int, uv:NumberSeq2) { setPointTexCoord(i, uv.x.toFloat, uv.y.toFloat) }

	def setPointTexCoord(i:Int, uv:(Float,Float)) { setPointTexCoord(i, uv._1, uv._2) }

	def setPointTexCoord(i:Int, u:Float, v:Float) {
		val p = i*2

		T(p)   = u
		T(p+1) = v

		if(i < tbeg) tbeg = i
		if(i+1 > tend) tend = i+1
	}
	
	/** Tell which vertex attribute to reference for the i-th triangle. */
	def setQuad(i:Int, a:Int, b:Int, c:Int, d:Int) {
		val p = i*4
		
		I(p)   = a
		I(p+1) = b
		I(p+2) = c
		I(p+3) = d
		
		if(p < ibeg) ibeg = p
		if(p+4 > iend) iend = p+4
	}
	
	/** The i-th point in the position vertex attribute. */
	def getPoint(i:Int):Point3 = {
		val p = i*3
		Point3(V(p), V(p+1), V(p+2))
	}
	
	def getPointTexCoords(i:Int):(Float,Float) = (T(i*2), T(i*2+1))

	/** The i-th triangle in the index array. The returned tuple contains the three indices of
	  * points in the position vertex array. See [[getPoint(Int)]]. */
	def getQuad(i:Int):(Int,Int,Int,Int) = {
		val p = i*4
		(I(p), I(p+1), I(p+2), I(p+3))
	}

	// -- Dynamic updating -------------------------------------------
	
    override def beforeNewVertexArray() {
		cbeg = size; cend = 0; vbeg = size; vend = 0; tbeg = size; tend = 0; ibeg = size; iend = 0
    }

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL) {
    	updateVertexArray(gl, true, true, true, true)
    }

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. You may give a boolean for each buffer in the vertex array
	  * that you want to update or not. */
	def updateVertexArray(gl:SGL, updateVertices:Boolean, updateColors:Boolean, updateNormals:Boolean, updateTexCoords:Boolean) {
		if(va ne null) {
			if(updateVertices && vend > vbeg) {
				va.buffer(VertexAttribute.Vertex.toString).update(vbeg, vend, V)
				vbeg = size
				vend = 0
			}
			if(updateNormals && (nend > nbeg)) {
				va.buffer(VertexAttribute.Normal.toString).update(nbeg, nend, N)
				nbeg = size
				nend = 0
			}
			if(updateColors && (cend > cbeg)) {
				va.buffer(VertexAttribute.Color.toString).update(cbeg, cend, C)
				cbeg = size
				cend = 0
			}
			if(updateTexCoords && (tend > tbeg)) {
				va.buffer(VertexAttribute.TexCoord.toString).update(tbeg, tend, T)
				tbeg = size
				tend = 0
			}
			if(iend > ibeg) {
				va.indices.update(ibeg, iend, I)
				ibeg = size
				iend = 0
			}
		}
	}
}