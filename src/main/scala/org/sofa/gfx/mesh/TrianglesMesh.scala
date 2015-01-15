package org.sofa.gfx.mesh

import org.sofa.gfx.{SGL, VertexArray, ShaderProgram}
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
class TrianglesMesh(val gl:SGL, val size:Int) extends Mesh {

	/** The mutable set of coordinates. */
	protected var V:MeshAttribute = addAttributeVertex
	
	/** The mutable set of colors. */
	protected var C:MeshAttribute = _ //addMeshAttribute(VertexAttribute.Color, 4)
	
	/** The mutable set of normals, changes with the triangles. */
	protected var N:MeshAttribute = _ //addMeshAttribute(VertexAttribute.Normal, 3)

	/** The mutable set of texture coordinates, changes with the triangles. */
	protected var T:MeshAttribute = _ //addMeshAttribute(VertexAttribute.TexCoord, 2)

	/** The mutable set of bone indices. */
	protected var B:MeshAttribute = _ 

	/** The mutable set of bone weights. */
	protected var W:MeshAttribute = _

	/** The mutable set of elements to draw. */
	protected var I:MeshElement = addIndices
    	
	// -- Mesh interface -----------------------------------------------------

	def vertexCount:Int = size * elementsPerPrimitive

    def elementsPerPrimitive:Int = 3

	def drawAs():Int = gl.TRIANGLES

	// -- Constructive interface ---------------------------------------------------

	final class Vx(var vertex:Int) {
		/** Set the `vertex` position (`x`, `y`, `z`). */
		def xyz(x:Float, y:Float, z:Float):Vx = { setPoint(vertex,x,y,z); this }
		
		/** Set the `vertex` texture coordinates (`u`, `v`). */
		def uv(u:Float, v:Float):Vx = { setPointTexCoord(vertex,u,v); this }
		
		/** Set the `vertex` color (`r`, `g`, `b`). */
		def rgb(r:Float, g:Float, b:Float):Vx = { setPointColor(vertex,r,g,b,1f); this }
		
		/** Set the `vertex` color (`r`, `g`, `b`, `a`). */
		def rgba(r:Float, g:Float, b:Float, a:Float):Vx = { setPointColor(vertex,r,g,b,a); this }

		/** Set the `vertex` `color`. */
		def rgba(color:Rgba):Vx = { setPointColor(vertex, color); this }
		
		/** Set the `vertex` normal (`x`, `y`, `z`). */
		def normal(x:Float, y:Float, z:Float):Vx = { setPointNormal(vertex,x,y,z); this }
		
		/**  Set the `vertex` bone indices. There can be up to four bones. */
		def bones(b0:Int, b1:Int = -1, b2:Int = -1, b3:Int = -1):Vx = { setPointBones(vertex,b0,b1,b2,b3); this }

		/** Set the `vertex` bone weights. There can be up to four bones. */
		def weights(w0:Float, w1:Float, w2:Float, w3:Float):Vx = { setPointWeights(vertex,w0,w1,w2,w3); this }

		/** Set the `values` for `vertex` attribute `name`. */
		def user(name:String, values:Float*):Vx = { 
			// Highly innefficient.
			meshAttribute(name).set(vertex, values:_*); this
		}
	}

	private[this] val vx = new Vx(-1)

	/** Access to a `vertex` by its index. You can then use the obtained vertex
	  * to change its position, color, tex coords, and any vertex attribute. Be
	  * careful, the returned vertex cannot be stored, its only a pointer in
	  * the mesh and its reference may change at each call to this method.
	  *
	  * Intended use, for example, is:
	  *
	  * mesh.vertex(232).xyz(1,2,3).uv(0,1).rgb(1,0,0)
	  */
	def vertex(vertex:Int):Vx = { vx.vertex = vertex; vx }

	/** Synonym of `vertex()`. */
	def v(vertex:Int):Vx = { vx.vertex = vertex; vx }
	
	/** Set the `i`-th triangle as composed of vertices `v0`, `v1` and `v2` in this order. */
	def triangle(i:Int, v0:Int, v1:Int, v2:Int) = setTriangle(i, v0, v1, v2)

	/** Synonym of `triangle()`. */
	def t(i:Int, v0:Int, v1:Int, v2:Int) = triangle(i, v0, v1, v2)

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
		val data = V.data

		data(i+0) = x
		data(i+1) = y
		data(i+2) = z
			
		V.range(vertex, vertex+1)

		this
	}
	
	def setPointColor(vertex:Int, c:Rgba):TrianglesMesh = setPointColor(vertex, c.red.toFloat, c.green.toFloat, c.blue.toFloat, c.alpha.toFloat)
	
	def setPointColor(vertex:Int, red:Float, green:Float, blue:Float, alpha:Float):TrianglesMesh = {
		val i = vertex * C.components
		val data = C.data
		
		data(i  ) = red
		data(i+1) = green
		data(i+2) = blue
		data(i+3) = alpha

		C.range(vertex, vertex+1)		

		this
	}
	
	def setPointNormal(vertex:Int, n:NumberSeq3):TrianglesMesh = setPointNormal(vertex, n.x.toFloat, n.y.toFloat, n.z.toFloat)
	
	def setPointNormal(vertex:Int, x:Float, y:Float, z:Float):TrianglesMesh = {
		val i = vertex * N.components
		val data = N.data
		
		data(i)   = x
		data(i+1) = y
		data(i+2) = z

		N.range(vertex, vertex+1)		

		this
	}

	def setPointTexCoord(vertex:Int, uv:NumberSeq2):TrianglesMesh = setPointTexCoord(vertex, uv.x.toFloat, uv.y.toFloat)

	def setPointTexCoord(vertex:Int, u:Float, v:Float):TrianglesMesh = {
		val i = vertex * T.components
		val data = T.data

		data(i)   = u
		data(i+1) = v

		T.range(vertex, vertex+1)

		this
	}

	def setPointBones(vertex:Int, b0:Int, b1:Int = -1, b2:Int = -1, b3:Int = -1):TrianglesMesh = {
		val i = vertex * B.components
		val data = B.data

		data(i+0) = b0
		data(i+1) = b1
		data(i+2) = b2
		data(i+3) = b3

		B.range(vertex, vertex+1)

		this
	}

	def setPointWeights(vertex:Int, w0:Float, w1:Float=0, w2:Float=0, w3:Float=0):TrianglesMesh = {
		val i = vertex * W.components
		val data = W.data

		data(i+0) = w0
		data(i+1) = w1
		data(i+2) = w2
		data(i+3) = w3

		W.range(vertex, vertex+1)

		this
	}
	
	/** Map the `t`-th triangle to the vertex attributes (`a`, `b`, `c`). */
	def setTriangle(t:Int, a:Int, b:Int, c:Int):TrianglesMesh = {
		val i = t * I.verticesPerPrim
		val data = I.data

		data(i)   = a
		data(i+1) = b
		data(i+2) = c
		
		I.range(t, t+1)

		this
	}

	/** Set individual index `i` to reference vertex attribute `a`. */
	def setIndex(i:Int, a:Int):TrianglesMesh = {
		val data = I.data
		
		data(i) = a
		
		I.range(i/3, i/3+1)

		this
	}
	
	/** The i-th point in the position vertex attribute. */
	def getPoint(vertex:Int):Point3 = {
		val i = vertex * V.components
		val data = V.data
		Point3(data(i), data(i+1), data(i+2))
	}

	/** The i-th point in the texture coordinates attribute. */
	def getPointTexCoord(vertex:Int):(Float,Float) = {
		val i = vertex * T.components
		val data = T.data
		(data(i), data(i+1))
	}
	
	/** The i-th triangle in the index array. The returned tuple contains the three indices of
	  * points in the position vertex array. See getPoint(Int)`. */
	def getTriangle(t:Int):(Int,Int,Int) = {
		val i = t * I.verticesPerPrim
		val data = I.data
		(data(i), data(i+1), data(i+2))
	}

	/** Recompute the tangents.
	  * The mesh must already have the triangles indices, the vertices position, normals, and texture coordinates, all
	  * four are needed to build the tangents. */
	def autoComputeTangents() { autoComputeTangents(false) }

	/** Recompute the tangents.
	  * The mesh must already have the triangles indices, the vertices position, normals, and texture coordinates, all
	  * four are needed to build the tangents. you can retrieve the bitangents
	  * using a cross product of the normal and tangent, however you need the handedness of the basis because
	  * your computed bitangent may point in the wrong direction. In this case the boolean argument asks to store
	  * tangents as 4-component elements where the fourth component is the handedness (1 or -1). You then multiply
	  * the result of this cross product by the value of this fourth component to have the correct handedness. */
	def autoComputeTangents(storeHandedness:Boolean) { autoComputeTangents(storeHandedness, false) }

	/** Recompute the tangents.
	  * The mesh must already have the triangles indices, the vertices position, normals, and texture coordinates, all
	  * four are needed to build the tangents. If you do not request to compute the bitangents, you can retrieve them
	  * using a cross product of the normal and tangent, however you need the handedness of the basis because
	  * your computed bitangent may point in the wrong direction. In this case the first argument asks to store
	  * tangents as 4-component elements where the fourth component is the handedness (1 or -1). You then multiply
	  * the result of this cross product by the value of this fourth component to have the correct handedness.
	  * The process involves to compute biTangents, they can be stored as a vertex attribute also by passing
	  * true as argument the second argument. */
	def autoComputeTangents(storeHandedness:Boolean, alsoComputeBiTangents:Boolean) {
		throw new RuntimeException("TrianglesMesh.autoComputeTangents() TODO")
	}

	/** Compute the relations between each vertex to each triangle. This allows, knowing a vertex,
	  * to iterate on the triangles it is connected to. Complexity O(n) with n triangles.
	  * Returns an array where each cell represents a vertex (in the same order as the vertex
	  * position attribute buffer), and contains an array of the triangles indices that uses
	  * this vertex. */
	def vertexToTriangle():Array[scala.collection.mutable.ArrayBuffer[Int]] = {
		throw new RuntimeException("TrianglesMesh.vertexToTriangle() TODO")
// 		// TODO we could do this at construction time ?! Memory vs. speed ...

// 		// Some verifications...
		
// 		if(beganIndex || beganVertex) throw new BadlyNestedBeginEnd("cannot call vertexToTriangle() inside begin/end")
// 		if(vertexBuffer.elements <= 0) throw new RuntimeException("vertexToTriangle() needs some vertices to operate upon")
// 		if(indexBuffer.size <= 0) throw new RuntimeException("vertexToTriangle() needs some indices to operate upon")

// 		// Create an array of int of the same number of elements as the vertexBuffer.

// 		val toTriangles = new Array[ArrayBuffer[Int]](vertexBuffer.elements)

// 		// For each triangle.

// 		val n = indexBuffer.size
// 		var i = 0

// 		while(i < n) {
// 			val p0 = indexBuffer(i)
// 			val p1 = indexBuffer(i+1)
// 			val p2 = indexBuffer(i+2)
// 			val t  = i / 3

// //			println("Triangle %d references points %d, %d and %d".format(t, p0, p1, p2))

// 			if(toTriangles(p0) eq null) toTriangles(p0) = new ArrayBuffer[Int]()
// 			if(toTriangles(p1) eq null) toTriangles(p1) = new ArrayBuffer[Int]()
// 			if(toTriangles(p2) eq null) toTriangles(p2) = new ArrayBuffer[Int]()

// 			// Reference this triangle.

// 			toTriangles(p0) += t
// 			toTriangles(p1) += t
// 			toTriangles(p2) += t

// 			i += 3
// 		}

// 		i = 0

// 		// toTriangles.foreach { vertex =>
// 		// 	if(vertex ne null)
// 		// 		println("vertex %d references { %s }".format(i, vertex.mkString(", ")))
// 		// 	else println("vertex %d is null !!".format(i))
// 		// 	i += 1
// 		// }

// 		toTriangles
	}

	/** Create a new mesh (a [[LinesMesh]]) that represents the normals (in red)
	  * and the tangents (in green). The mesh contains position, and color attributes
	  * only. */
	def newNormalsTangentsMesh():Mesh = newNormalsTangentsMesh(Rgba.Red, Rgba.Green)

	/** Create a new mesh (a [[LineMesh]]) that represents the normals (using
	  * the first given color) and the tangents (using the second given color).
	  * The mesh contains position and color attributes only. */
	def newNormalsTangentsMesh(normalsColor:Rgba, tangentsColor:Rgba):Mesh = {
		throw new RuntimeException("TrianglesMesh.newNormalsTangentsMesh() TODO")
// 		var point    = 0
// 		val vertices = vertexBuffer
// 		val normals  = getNormalMeshBuffer
// 		val tangents = getTangentMeshBuffer
// 		val n        = vertices.elements
// 		val ntMesh   = new LinesMesh(gl, n*2)

// 		while(point < n) {
// 			val P = Point3(vertices(point*3), vertices(point*3+1), vertices(point*3+2))
// 			val N = Vector3(normals(point*3), normals(point*3+1), normals(point*3+2))
// 			val T = Vector3(tangents(point*tangents.components), tangents(point*tangents.components+1), tangents(point*tangents.components+2))

// 			ntMesh.setColor(point, normalsColor)
// 			ntMesh.setLine(point, P, N)
// 			ntMesh.setColor(point+n, tangentsColor)
// 			ntMesh.setLine(point+n, P, T)

// 			point += 1
// 		}

// 		ntMesh
 	}

	// -- Building ---------------------------------------------------

	protected def addAttributeVertex:MeshAttribute = {
		if(V eq null) {
			V = addMeshAttribute(VertexAttribute.Vertex, 3)
		}
		V
	}

	def addAttributeColor:MeshAttribute = {
		if(C eq null) {
			C = addMeshAttribute(VertexAttribute.Color, 4)
		}
		C
	}

	def addAttributeNormal:MeshAttribute = {
		if(N eq null) {
			N = addMeshAttribute(VertexAttribute.Normal, 3)
		}
		N
	}

	def addAttributeTexCoord:MeshAttribute = {
		if(T eq null) {
			T = addMeshAttribute(VertexAttribute.TexCoord, 2)
		}
		T
	}

	def addAttributeBone:MeshAttribute = {
		if(B eq null) {
			B = addMeshAttribute(VertexAttribute.Bone, 4)
		}

		B
	}

	def addAttributeWeight:MeshAttribute = {
		if(W eq null) {
			W = addMeshAttribute(VertexAttribute.Weight, 4)
		}

		W
	}

	protected def addIndices():MeshElement = {
		 //new MeshElement(size, 3)
		 if(I eq null) {
		 	I = addMeshElement(size, 3)
		 }

		 I
	}
}