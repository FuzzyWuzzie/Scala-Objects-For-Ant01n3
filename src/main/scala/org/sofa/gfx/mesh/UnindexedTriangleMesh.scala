package org.sofa.gfx.mesh

import org.sofa.gfx.{SGL, VertexArray, ShaderProgram}
import org.sofa.math.{Rgba, Point3, Vector3, NumberSeq2, NumberSeq3, Triangle}
import org.sofa.nio.{IntBuffer, FloatBuffer}


/** Like a [[TriangleMesh]] but without an index.
  *
  * Should disappear as soon as TrinalgesMesh will allow not having an index.
  */
class UnindexedTrianglesMesh(val gl:SGL, val size:Int) extends Mesh {

	/** The mutable set of coordinates. */
	protected var V:MeshAttribute = addAttributeVertex()

	/** The mutable set of colors. */
	protected var C:MeshAttribute = _

	/** The mutable set of normals, changes with the triangles. */
	protected var N:MeshAttribute = _

    // -- Mesh interface ---------------------------------------

    def vertexCount:Int = size * 3

    def elementsPerPrimitive:Int = 3

	def drawAs():Int = gl.TRIANGLES

	protected def addAttributeVertex():MeshAttribute = { if(V eq null) { V = addMeshAttribute(VertexAttribute.Vertex, 3) }; V }

	def addAttributeColor():MeshAttribute = { if(C eq null) { C = addMeshAttribute(VertexAttribute.Color, 4) }; C }

	def addAttributeNormal():MeshAttribute = { if(N eq null) { N = addMeshAttribute(VertexAttribute.Normal, 3) }; N }

    // -- Editing ----------------------------------------------------------------

	def setTriangle(i:Int, p0:Point3, p1:Point3, p2:Point3) {
		setTriangle(i, p0.x.toFloat, p0.y.toFloat, p0.z.toFloat,
		               p1.x.toFloat, p1.y.toFloat, p1.z.toFloat,
		               p2.x.toFloat, p2.y.toFloat, p2.z.toFloat)
	}	

	def setTriangle(i:Int, t:Triangle) { setTriangle(i, t.p0, t.p1, t.p2) }

	def setTriangle(i:Int, x0:Float, y0:Float, z0:Float, x1:Float, y1:Float, z1:Float, x2:Float, y2:Float, z2:Float) {
		val p = i*3*3
		val v = V.data

		v(p)   = x0
		v(p+1) = y0
		v(p+2) = z0

		v(p+3) = x1
		v(p+4) = y1
		v(p+5) = z1

		v(p+6) = x2
		v(p+7) = y2
		v(p+8) = z2		

		V.range(i*3, i*3+3)
	}

	def setColor(i:Int, c:Rgba) { setColor(i, c, c, c) }

	def setColor(i:Int, c0:Rgba, c1:Rgba, c2:Rgba) {
		val p = i*4*3
		val c = C.data

		c(p   ) = c0.red.toFloat
		c(p+ 1) = c0.green.toFloat
		c(p+ 2) = c0.blue.toFloat
		c(p+ 3) = c0.alpha.toFloat

		c(p+ 4) = c1.red.toFloat
		c(p+ 5) = c1.green.toFloat
		c(p+ 6) = c1.blue.toFloat
		c(p+ 7) = c1.alpha.toFloat

		c(p+ 8) = c2.red.toFloat
		c(p+ 9) = c2.green.toFloat
		c(p+10) = c2.blue.toFloat
		c(p+11) = c2.alpha.toFloat

		C.range(i*3, i*3+3)
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
		val n = N.data

		n(p)   = x0
		n(p+1) = y0
		n(p+2) = z0

		n(p+3) = x1
		n(p+4) = y1
		n(p+5) = z1

		n(p+6) = x2
		n(p+7) = y2
		n(p+8) = z2		

		N.range(i*3, i*3+3)
	}

	def getTriangle(i:Int):(Point3,Point3,Point3) = {
		val p = i*3*3
		val v = V.data
		(Point3(v(p)  , v(p+1), v(p+2)),
		 Point3(v(p+3), v(p+4), v(p+5)),
		 Point3(v(p+6), v(p+7), v(p+8)))
	}
}