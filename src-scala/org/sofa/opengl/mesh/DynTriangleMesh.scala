package org.sofa.opengl.mesh

import org.sofa.nio.FloatBuffer
import org.sofa.opengl.SGL
import org.sofa.opengl.VertexArray
import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._
import org.sofa.math.Rgba
import org.sofa.math.Point3
import org.sofa.math.Vector3

class DynTriangleMesh(val size:Int) extends Mesh with SurfaceMesh with ColorableMesh {
	
	/** The mutable set of coordinates. */
	protected lazy val V:FloatBuffer = allocateVertices
	
	/** The mutable set of colors. */
	protected lazy val C:FloatBuffer = allocateColors
	
	/** The mutable set of normals, changes with the triangles. */
	protected lazy val N:FloatBuffer = allocateNormals
	
    /** Start position of the last modification inside the coordinates array. */
    protected var vbeg = 0
    
    /** End position of the last modification inside the coordinates array. */
    protected var vend = size
    
    /** Start position of the last modification inside the color array. */
    protected var cbeg = 0
    
    /** End position of the last modification inside the color array. */
    protected var cend = size

    /** Last vertex array created using newVertexArray(). */
    protected var vertexArray:VertexArray = null
    
    protected def allocateVertices():FloatBuffer = new FloatBuffer(size*3*3)
	
	protected def allocateNormals():FloatBuffer = new FloatBuffer(size*3*3)
	
	protected def allocateColors():FloatBuffer = new FloatBuffer(size*4*3)
	
	def vertices:FloatBuffer = V
	
	override def normals:FloatBuffer = N
	
	override def colors:FloatBuffer = C
	
	override def hasNormals = true
	
	override def hasColors = true
	
	def setTriangle(i:Int, p0:Point3, p1:Point3, p2:Point3) {
		setTriangle(i, p0.x.toFloat, p0.y.toFloat, p0.z.toFloat,
		               p1.x.toFloat, p1.y.toFloat, p1.z.toFloat,
		               p2.x.toFloat, p2.y.toFloat, p2.z.toFloat)
	}	
	
	def setColor(i:Int, c:Rgba) { setColor(i, c, c, c) }
	
	def setColor(i:Int, c0:Rgba, c1:Rgba, c2:Rgba) {
		val p = i*4*3
		
		C(p   ) = c0.red.toFloat
		C(p+ 1) = c0.green.toFloat
		C(p+ 2) = c0.blue.toFloat
		C(p+ 3) = c0.alpha.toFloat
		
		C(p+ 4) = c1.red.toFloat
		C(p+ 5) = c1.green.toFloat
		C(p+ 6) = c1.blue.toFloat
		C(p+ 7) = c1.alpha.toFloat
		
		C(p+ 8) = c2.red.toFloat
		C(p+ 9) = c2.green.toFloat
		C(p+10) = c2.blue.toFloat
		C(p+11) = c2.alpha.toFloat

		if(i < cbeg) cbeg = i
		if(i+1 > cend) cend = i + 1
	}
	
	def setTriangle(i:Int, x0:Float, y0:Float, z0:Float, x1:Float, y1:Float, z1:Float, x2:Float, y2:Float, z2:Float) {
		val p = i*3*3
		
		V(p)   = x0
		V(p+1) = y0
		V(p+2) = z0
		
		V(p+3) = x1
		V(p+4) = y1
		V(p+5) = z1
		
		V(p+6) = x2
		V(p+7) = y2
		V(p+8) = z2		
		
		if(i < vbeg) vbeg = i
		if(i+1 > vend) vend = i+1
	}
	
	def autoComputeNormal(i:Int) {
		val (p0, p1, p2) = getTriangle(i)
		autoComputeNormal(i, p0, p1, p2)
	}
	
	def autoComputeNormal(i:Int, p0:Point3, p1:Point3, p2:Point3) {
		val v0 = Vector3(p0, p1)
		val v1 = Vector3(p0, p2)
		val n = v1 X v0
		n.normalize
		setNormal(i, n)
	}
	
	def setNormal(i:Int, n:Vector3) { setNormal(i, n.x.toFloat, n.y.toFloat, n.z.toFloat) }

	def setNormal(i:Int, x:Float, y:Float, z:Float) { setNormal(i, x, y, z, x, y, z, x, y, z) }
	
	def setNormal(i:Int, n0:Vector3, n1:Vector3, n2:Vector3) {
		setNormal(i,
			n0.x.toFloat, n0.y.toFloat, n0.z.toFloat,
			n1.x.toFloat, n1.y.toFloat, n1.z.toFloat,
			n2.x.toFloat, n2.y.toFloat, n2.z.toFloat)
	}
	
	def setNormal(i:Int, x0:Float, y0:Float, z0:Float, x1:Float, y1:Float, z1:Float, x2:Float, y2:Float, z2:Float) {
		val p = i*3*3
		
		N(p)   = x0
		N(p+1) = y0
		N(p+2) = z0
		
		N(p+3) = x1
		N(p+4) = y1
		N(p+5) = z1
		
		N(p+6) = x2
		N(p+7) = y2
		N(p+8) = z2		
		
		if(i < vbeg) vbeg = i
		if(i+1 > vend) vend = i+1		
	}
	
	def getTriangle(i:Int):(Point3,Point3,Point3) = {
		val p = i*3*3
		(Point3(V(p)  , V(p+1), V(p+2)),
		 Point3(V(p+3), V(p+4), V(p+5)),
		 Point3(V(p+6), V(p+7), V(p+8)))
	}
	
	def drawAs():Int = GL_TRIANGLES
	
    override def newVertexArray(gl:SGL, locations:Tuple6[Int,Int,Int,Int,Int,Int]):VertexArray = {
		cbeg = size; cend = 0; vbeg = size; vend = 0
		vertexArray = super.newVertexArray(gl, locations)
		vertexArray
	}

    override def newVertexArray(gl:SGL, drawMode:Int, locations:Tuple6[Int,Int,Int,Int,Int,Int]):VertexArray = {
		cbeg = size; cend = 0; vbeg = size; vend = 0
		vertexArray = super.newVertexArray(gl, drawMode, locations)
		vertexArray
	}
    
	override def newVertexArray(gl:SGL, drawMode:Int, locations:Tuple2[String,Int]*):VertexArray = {
		cbeg = size; cend = 0; vbeg = size; vend = 0
    	vertexArray = super.newVertexArray(gl, drawMode, locations:_*)
    	vertexArray
    }
    
    override def newVertexArray(gl:SGL, locations:Tuple2[String,Int]*):VertexArray = {
		cbeg = size; cend = 0; vbeg = size; vend = 0
    	vertexArray = super.newVertexArray(gl, locations:_*)
    	vertexArray
    }
	
	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
	def updateVertexArray(gl:SGL, verticesName:String, colorsName:String, normalsName:String) {
		if(vertexArray ne null) {
			if(vend > vbeg) {
				if(vbeg == 0 && vend == size) {
					vertexArray.buffer(verticesName).update(vertices)
					vertexArray.buffer(normalsName).update(normals)
				} else {
					vertexArray.buffer(verticesName).update(vbeg*3, vend*3, vertices)
					vertexArray.buffer(normalsName).update(vbeg*3, vend*3, normals)
				}
				
				vbeg = size
				vend = 0
			}
			if(cend > cbeg) {
				if(cbeg == 0 && cend == size) {
					vertexArray.buffer(colorsName).update(colors)
				} else {
					vertexArray.buffer(colorsName).update(cbeg*3, cend*3, colors)
				}
				
				
				cbeg = size
				cend = 0
			}
		}
	}
}