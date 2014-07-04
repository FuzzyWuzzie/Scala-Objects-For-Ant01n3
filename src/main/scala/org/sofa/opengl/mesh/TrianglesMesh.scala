package org.sofa.opengl.mesh

import org.sofa.opengl.{SGL, VertexArray, ShaderProgram}
import org.sofa.math.{Rgba, Point3, Vector3, NumberSeq2, NumberSeq3, Triangle}
import org.sofa.nio.{IntBuffer}


/** A set of independant triangles that can be dynamically updated and
  * tries to send only changed informations to the GL.
  *
  * There are `size` triangles at max in the mesh, and `size`*3 vertices
  * will be allocated. You can share vertices if you which, not all allocated
  * vertices need to be used. The attributes Vertex, Color, Normal and TexCoord,
  * are faster to use than other generic attributes due to some caching.
  *
  * @param size The max number of triangles. */
class TrianglesMesh(val size:Int) extends Mesh {

	/** The mutable set of coordinates. */
	protected[this] lazy val V:MeshAttribute = addMeshAttribute(VertexAttribute.Vertex, 3)
	
	/** The mutable set of colors. */
	protected[this] lazy val C:MeshAttribute = addMeshAttribute(VertexAttribute.Color, 4)
	
	/** The mutable set of normals, changes with the triangles. */
	protected[this] lazy val N:MeshAttribute = addMeshAttribute(VertexAttribute.Normal, 3)

	/** The mutable set of texture coordinates, changes with the triangles. */
	protected[this] lazy val T:MeshAttribute = addMeshAttribute(VertexAttribute.TexCoord, 2)

	/** The mutable set of elements to draw. */
	protected[this] lazy val I:MeshIndex = new MeshIndex(size, 3)
    	
	// -- Mesh interface -----------------------------------------------------

	def vertexCount:Int = size * 3

	def drawAs(gl:SGL):Int = gl.TRIANGLES

	override def indices:IntBuffer = I.data

    override def hasIndices():Boolean = true

	// -- Constructive interface ---------------------------------------------------
	
	/** Set the `vertex` position (`x`, `y`, `z`). */
	def xyz(vertex:Int, x:Float, y:Float, z:Float):TrianglesMesh = setPoint(vertex,x,y,z)
	
	/** Set the `vertex` texture coordinates (`u`, `v`). */
	def uv(vertex:Int, u:Float, v:Float):TrianglesMesh = setPointTexCoord(vertex,u,v)
	
	/** Set the `vertex` color (`r`, `g`, `b`). */
	def rgb(vertex:Int, r:Float, g:Float, b:Float) = setPointColor(vertex,r,g,b,1f)
	
	/** Set the `vertex` color (`r`, `g`, `b`, `a`). */
	def rgba(vertex:Int, r:Float, g:Float, b:Float, a:Float) = setPointColor(vertex,r,g,b,a)
	
	/** Set the `vertex` normal (`x`, `y`, `z`). */
	def nnn(vertex:Int, x:Float, y:Float, z:Float) = setPointNormal(vertex,x,y,z)
	
	/** Set the `values` for `vertex` attribute `name`. */
	def user(name:String, vertex:Int, values:Float*) = setAttribute(name, vertex, values:_*)
	
	/** Set the `i`-th triangle as composed of vertices `v0`, `v1` and `v2` in this order. */
	def triangle(i:Int, v0:Int, v1:Int, v2:Int) = setTriangle(i, v0, v1, v2)

	/** The i-th point in the position vertex attribute. */
	def point(vertex:Int):Point3 = getPoint(vertex)

	/** The i-th point in the texture coordinates attribute. */
	def texCoord(vertex:Int):(Float,Float) = getPointTexCoord(vertex)
	
	/** The i-th triangle in the index array. The returned tuple contains the three indices of
	  * points in the position vertex array. See getPoint(Int)`. */
	def triangle(i:Int):(Int,Int,Int) = getTriangle(i)

	// -- Old Constructive interface -----------------------------------------------

	def setPoint(vertex:Int, p:NumberSeq3):TrianglesMesh = setPoint(vertex, p.x.toFloat, p.y.toFloat, p.z.toFloat)

	def setPoint(vertex:Int, x:Float, y:Float, z:Float):TrianglesMesh = {
		val i = vertex * V.components
		val data = V.theData

		data(i+0) = x
		data(i+1) = y
		data(i+2) = z
		
		if(vertex   < V.beg) V.beg = vertex
		if(vertex+1 > V.end) V.end = vertex + 1

		this
	}
	
	def setPointColor(vertex:Int, c:Rgba):TrianglesMesh = setPointColor(vertex, c.red.toFloat, c.green.toFloat, c.blue.toFloat, c.alpha.toFloat)
	
	def setPointColor(vertex:Int, red:Float, green:Float, blue:Float, alpha:Float):TrianglesMesh = {
		val i = vertex * C.components
		val data = C.theData
		
		data(i  ) = red
		data(i+1) = green
		data(i+2) = blue
		data(i+3) = alpha
		
		if(vertex   < C.beg) C.beg = vertex
		if(vertex+1 > C.end) C.end = vertex + 1

		this
	}
	
	def setPointNormal(vertex:Int, n:NumberSeq3):TrianglesMesh = setPointNormal(vertex, n.x.toFloat, n.y.toFloat, n.z.toFloat)
	
	def setPointNormal(vertex:Int, x:Float, y:Float, z:Float):TrianglesMesh = {
		val i = vertex * N.components
		val data = N.theData
		
		data(i)   = x
		data(i+1) = y
		data(i+2) = z
		
		if(vertex   < N.beg) N.beg = vertex
		if(vertex+1 > N.end) N.end = vertex + 1

		this
	}

	def setPointTexCoord(vertex:Int, uv:NumberSeq2):TrianglesMesh = setPointTexCoord(vertex, uv.x.toFloat, uv.y.toFloat)

	def setPointTexCoord(vertex:Int, u:Float, v:Float):TrianglesMesh = {
		val i = vertex * T.components
		val data = T.theData

		data(i)   = u
		data(i+1) = v

		if(vertex   < T.beg) T.beg = vertex
		if(vertex+1 > T.end) T.end = vertex+1

		this
	}
	
	/** Tell which vertex attribute to reference for the i-th triangle. */
	def setTriangle(t:Int, a:Int, b:Int, c:Int):TrianglesMesh = {
		val i = t * I.verticesPerPrim
		val data = I.theData

		data(i)   = a
		data(i+1) = b
		data(i+2) = c
		
		if(i < I.beg) I.beg = i
		if(i+I.verticesPerPrim > I.end) I.end = i + I.verticesPerPrim

		this
	}
	
	/** The i-th point in the position vertex attribute. */
	def getPoint(vertex:Int):Point3 = {
		val i = vertex * V.components
		val data = V.theData
		Point3(data(i), data(i+1), data(i+2))
	}

	/** The i-th point in the texture coordinates attribute. */
	def getPointTexCoord(vertex:Int):(Float,Float) = {
		val i = vertex * T.components
		val data = T.theData
		(data(i), data(i+1))
	}
	
	/** The i-th triangle in the index array. The returned tuple contains the three indices of
	  * points in the position vertex array. See getPoint(Int)`. */
	def getTriangle(t:Int):(Int,Int,Int) = {
		val i = t * I.verticesPerPrim
		val data = I.theData
		(data(i), data(i+1), data(i+2))
	}

	// -- Dynamic updating -------------------------------------------
	
    override def beforeNewVertexArray() {
		if(has(VertexAttribute.Color))    C.resetMarkers
		if(has(VertexAttribute.Vertex))   V.resetMarkers
		if(has(VertexAttribute.TexCoord)) T.resetMarkers
		if(has(VertexAttribute.Normal))   N.resetMarkers
		I.resetMarkers
    }

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true, true, true) }

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. You may give a boolean for each buffer in the vertex array
	  * that you want to update or not. */
	def updateVertexArray(gl:SGL, updateVertices:Boolean=false, updateColors:Boolean=false, updateNormals:Boolean=false, updateTexCoords:Boolean=false) {
		if(va ne null) {
			if(updateVertices) V.update(va)
			if(updateNormals) N.update(va)
			if(updateColors) C.update(va)
			if(updateTexCoords) T.update(va)
			I.update(va)
		}
	}

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. You may give a boolean for each buffer in the vertex array
	  * that you want to update or not. */
	def updateVertexArray(gl:SGL, attributes:String*) {
		if(va ne null) {
			I.update(va)
			attributes.foreach { meshAttribute(_).update(va) }
		}
	}
}