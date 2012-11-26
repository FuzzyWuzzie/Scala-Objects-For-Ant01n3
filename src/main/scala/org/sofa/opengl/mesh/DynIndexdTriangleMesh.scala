package org.sofa.opengl.mesh

import org.sofa.opengl.{SGL, VertexArray}
import org.sofa.math.{Rgba, Point3, Vector3, NumberSeq2, NumberSeq3}
import org.sofa.nio.{IntBuffer, FloatBuffer}
import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._

/** A dynamic set of triangles that can be updated and tries to send only changed
  * informations to the GL. */
class DynIndexedTriangleMesh(val size:Int) extends Mesh with IndexedMesh with SurfaceMesh with ColorableMesh {
	
	/** The mutable set of coordinates. */
	protected lazy val V:FloatBuffer = allocateVertices
	
	/** The mutable set of colors. */
	protected lazy val C:FloatBuffer = allocateColors
	
	/** The mutable set of normals, changes with the triangles. */
	protected lazy val N:FloatBuffer = allocateNormals

	/** The mutable set of texture coordinates, changes with the triangles. */
	protected lazy val T:FloatBuffer = allocateTexCoords
	
	/** The mutable set of elements to draw. */
	protected lazy val I:IntBuffer = allocateIndices
	
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

    /** Last vertex array created using newVertexArray(). */
    protected var vertexArray:VertexArray = null
    
    protected def allocateVertices():FloatBuffer = new FloatBuffer(size*3*3)	// There may be less points.
	
	protected def allocateNormals():FloatBuffer = new FloatBuffer(size*3*3)		// There may be less normals.
	
	protected def allocateColors():FloatBuffer = new FloatBuffer(size*4*3)		// There may be less colors.
	
	protected def allocateTexCoords():FloatBuffer = new FloatBuffer(size*2*3)	// There may be less texcoords.

	protected def allocateIndices():IntBuffer = new IntBuffer(size*3)
	
	def vertices:FloatBuffer = V
	
	override def normals:FloatBuffer = N
	
	override def colors:FloatBuffer = C
	
	override def texCoords:FloatBuffer = T

	override def indices:IntBuffer = I
	
	override def hasNormals = true
	
	override def hasColors = true
	
	override def hasTexCoords = true

	override def hasIndices = true
	
	def setPoint(i:Int, p:NumberSeq3) { setPoint(i, p.x.toFloat, p.y.toFloat, p.z.toFloat) }

	def setPoint(i:Int, x:Float, y:Float, z:Float) {
		val p = i*3
		
		V(p)   = x
		V(p+1) = y
		V(p+2) = z
		
		if(i < vbeg) vbeg = i
		if(i+1 > vend) vend = i+1
//Console.err.println("setPoint(%d -> %d)".format(vbeg, vend))
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

	def setPointTexCoord(i:Int, u:Float, v:Float) {
		val p = i*2

		T(p)   = u
		T(p+1) = v

		if(i < tbeg) tbeg = i
		if(i+1 > tend) tend = i+1
	}
	
	def setTriangle(i:Int, a:Int, b:Int, c:Int) {
		val p = i*3
		
		I(p)   = a
		I(p+1) = b
		I(p+2) = c
		
		if(p < ibeg) ibeg = p
		if(p+3 > iend) iend = p+3
//Console.err.println("#setTriangle(%d -> %d)".format(ibeg, iend))
	}
	
	def getPoint(i:Int):Point3 = {
		val p = i*3
		Point3(V(p), V(p+1), V(p+2))
	}
	
	def getTriangle(i:Int):(Int,Int,Int) = {
		val p = i*3
		(I(p), I(p+1), I(p+2))
	}
	
	def drawAs():Int = GL_TRIANGLES
	
    override def newVertexArray(gl:SGL, locations:Tuple6[Int,Int,Int,Int,Int,Int]):VertexArray = {
		cbeg = size; cend = 0; vbeg = size; vend = 0; tbeg = size; tend = 0; ibeg = size; iend = 0
		vertexArray = super.newVertexArray(gl, locations)
		vertexArray
	}

    override def newVertexArray(gl:SGL, drawMode:Int, locations:Tuple6[Int,Int,Int,Int,Int,Int]):VertexArray = {
		cbeg = size; cend = 0; vbeg = size; vend = 0; tbeg = size; tend = 0; ibeg = size; iend = 0
		vertexArray = super.newVertexArray(gl, drawMode, locations)
		vertexArray
	}
    
	override def newVertexArray(gl:SGL, drawMode:Int, locations:Tuple2[String,Int]*):VertexArray = {
		cbeg = size; cend = 0; vbeg = size; vend = 0; tbeg = size; tend = 0; ibeg = size; iend = 0
    	vertexArray = super.newVertexArray(gl, drawMode, locations:_*)
    	vertexArray
    }
    
    override def newVertexArray(gl:SGL, locations:Tuple2[String,Int]*):VertexArray = {
		cbeg = size; cend = 0; vbeg = size; vend = 0; tbeg = size; tend = 0; ibeg = size; iend = 0
    	vertexArray = super.newVertexArray(gl, locations:_*)
    	vertexArray
    }
	
	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. You must give the name of each buffer in the vertex array
	  * that you want to update. The name can be null if your vertex array does not use it. */
	def updateVertexArray(gl:SGL, verticesName:String, colorsName:String, normalsName:String, texCoordsName:String) {
		if(vertexArray ne null) {
			if(vend > vbeg) {
				vertexArray.buffer(verticesName).update(vbeg, vend, vertices)
				
				vbeg = size
				vend = 0
			}
			if((normalsName ne null) && (nend > nbeg)) {
				vertexArray.buffer(normalsName).update(nbeg, nend, normals)
				
				nbeg = size
				nend = 0
			}
			if((colorsName ne null) && (cend > cbeg)) {
				vertexArray.buffer(colorsName).update(cbeg, cend, colors)
				
				cbeg = size
				cend = 0
			}
			if((texCoordsName ne null) && (tend > tbeg)) {
				vertexArray.buffer(texCoordsName).update(tbeg, tend, texCoords)

				tbeg = size
				tend = 0
			}
			if(iend > ibeg) {
				vertexArray.indices.update(ibeg, iend, indices)
				
				ibeg = size
				iend = 0
			}
		}
	}
}