package org.sofa.gfx.mesh

import scala.collection.mutable.ArrayBuffer

import org.sofa.gfx.{SGL, VertexArray, ShaderProgram}
import org.sofa.math.{Rgba, Point2, Point3, Vector3, NumberSeq2, NumberSeq3, Triangle}
import org.sofa.nio.{IntBuffer}


// TODO
//   - Allow to add the indices if needed only.
//   - Add methods to ensure vertexToTriangles relations are remembered and automatically
//     updated if needed.



/** A set of independant triangles that can be dynamically updated and
  * tries to send only changed informations to the GL.
  *
  * There are `size` triangles at max in the mesh, and `size`*3 vertices
  * will be allocated by default. You can share vertices if you which,
  * not all allocated vertices need to be used. Therefore if you specify
  * a value > 0 for `vertices`, only this number of vertex attributes will
  * be allocated.
  */
class TrianglesMesh(val gl:SGL, val size:Int, val vertices:Int = -1) extends Mesh {

	/** The mutable set of coordinates. */
	protected var V:MeshAttribute = addAttributeVertex
	
	/** The mutable set of colors. */
	protected var C:MeshAttribute = _ 
	
	/** The mutable set of normals, changes with the triangles. */
	protected var N:MeshAttribute = _ 

	/** The mutable set of tangents. */
	protected var Nt:MeshAttribute = _

	/** The mutable set of bi-tangents. */
	protected var Nb:MeshAttribute = _

	/** The mutable set of texture coordinates, changes with the triangles. */
	protected var T:MeshAttribute = _ 

	/** The mutable set of bone indices. */
	protected var B:MeshAttribute = _ 

	/** The mutable set of bone weights. */
	protected var W:MeshAttribute = _

	/** The mutable set of elements to draw. */
	protected var I:MeshElement = addIndices

	/** Avoid to allocate a new [[Vx]] at each call to `vertex()`. This means that
	  * you cannot keep returned [[Vx]] values. */
	private[this] val vx = new Vx(-1)

	// -- Mesh interface -----------------------------------------------------

	def vertexCount:Int = if(vertices < 0) size * elementsPerPrimitive else vertices

    def elementsPerPrimitive:Int = 3

	def drawAs():Int = gl.TRIANGLES

	// -- Constructive interface ---------------------------------------------------

	/** Class representing one vertex, allowing easy modification of each attribute. */
	final class Vx(var vertex:Int) {
		/** Set the `vertex` position (`x`, `y`, `z`). */
		def pos(x:Float, y:Float, z:Float):Vx = { setVertexPosition(vertex,x,y,z); this }
		
		/** Set the `vertex` texture coordinates (`u`, `v`). */
		def tex(u:Float, v:Float):Vx = { setVertexTexCoords(vertex,u,v); this }

		/** Set the `vertex` color (`r`, `g`, `b`). */
		def clr(r:Float, g:Float, b:Float):Vx = { setVertexColor(vertex,r,g,b,1f); this }
		
		/** Set the `vertex` color (`r`, `g`, `b`, `a`). */
		def clr(r:Float, g:Float, b:Float, a:Float):Vx = { setVertexColor(vertex,r,g,b,a); this }

		/** Set the `vertex` `color`. */
		def clr(color:Rgba):Vx = { setVertexColor(vertex, color); this }
		
		/** Set the `vertex` normal (`x`, `y`, `z`). */
		def nrm(x:Float, y:Float, z:Float):Vx = { setVertexNormal(vertex,x,y,z); this }
		
		/** Set the `vertex` tangent (`x`, `y`, `z`). */
		def tan(x:Float, y:Float, z:Float):Vx = { setVertexTangent(vertex,x,y,z); this }

		/** Set the `vertex` bi-tangent (`x`, `y`, `z`). */
		def btn(x:Float, y:Float, z:Float):Vx = { setVertexBiTangent(vertex, x,y,z); this }

		/**  Set the `vertex` bone indices. There can be up to four bones. */
		def bon(b0:Int, b1:Int = -1, b2:Int = -1, b3:Int = -1):Vx = { setVertexBones(vertex,b0,b1,b2,b3); this }

		/** Set the `vertex` bone weights. There can be up to four bones. */
		def wei(w0:Float, w1:Float, w2:Float, w3:Float):Vx = { setVertexWeights(vertex,w0,w1,w2,w3); this }

		/** Set the `values` for `vertex` attribute `name`. */
		def usr(name:String, values:Float*):Vx = { 
			// Highly innefficient.
			meshAttribute(name).set(vertex, values:_*); this
		}
	}

	/** Access to a `vertex` attribute by its index. You can then use the obtained vertex
	  * to change its position, color, tex coords, and any vertex attribute. Be careful,
	  * the returned vertex cannot be stored, its only a pointer in the mesh and its
	  * reference may change at each call to this method.
	  *
	  * Intended use, for example, is:
	  *
	  *     mesh.vertex(232).pos(1,2,3).uv(0,1).rgb(1,0,0)
      *
	  * Or, using syntax sugar:
	  *
	  *     mesh v(232) pos(1,2,3) uv(0,1) rgb(1,0,0)
	  */
	def vertex(vertex:Int):Vx = { vx.vertex = vertex; vx }

	/** Synonym of `vertex()`. */
	def v(vertex:Int):Vx = { vx.vertex = vertex; vx }
	
	/** Set the `t`-th triangle as composed of vertex attributes `v0`, `v1` and `v2` in this order. */
	def triangle(t:Int, v0:Int, v1:Int, v2:Int) = setTriangle(t, v0, v1, v2)

	/** Synonym of `triangle()`. */
	def t(i:Int, v0:Int, v1:Int, v2:Int) = triangle(i, v0, v1, v2)

	/** The position of `vertex`. */
	def position(vertex:Int):Point3 = getVertexPosition(vertex)

	/** The texture coordinates of `vertex`. */
	def texCoord(vertex:Int):Point2 = getVertexTexCoord(vertex)
	
	/** The normal of `vertex`. */
	def normal(vertex:Int):Vector3 = getVertexNormal(vertex)

	/** The tangent of `vertex`. */
	def tangent(vertex:Int):Vector3 = getVertexTangent(vertex)

	/** The bi-tangent of `vertex`. */
	def bitangent(vertex:Int):Vector3 = getVertexBiTangent(vertex)

	/** The `t`-th triangle in the index array. The returned tuple contains the three indices of
	  * points in the vertex attributes. */
	def triangle(t:Int):(Int,Int,Int) = getTriangle(t)

	// -- Old Constructive interface -----------------------------------------------

	/** Set the position `p` of `vertex`. */
	def setVertexPosition(vertex:Int, p:NumberSeq3):TrianglesMesh = setVertexPosition(vertex, p.x.toFloat, p.y.toFloat, p.z.toFloat)

	/** Set the position (`x`, `y`, `z`) of `vertex`. */
	def setVertexPosition(vertex:Int, x:Float, y:Float, z:Float):TrianglesMesh = {
		val i = vertex * V.components
		val data = V.data

		data(i+0) = x
		data(i+1) = y
		data(i+2) = z
			
		V.range(vertex, vertex+1)

		this
	}
	
	/** Set the `color` of `vertex`. */
	def setVertexColor(vertex:Int, color:Rgba):TrianglesMesh = setVertexColor(vertex, color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
	
	/** Set the color (`red`, `green`, `blue`, `alpha`) of `vertex`. */
	def setVertexColor(vertex:Int, red:Float, green:Float, blue:Float, alpha:Float):TrianglesMesh = {
		val i = vertex * C.components
		val data = C.data
		
		data(i  ) = red
		data(i+1) = green
		data(i+2) = blue
		data(i+3) = alpha

		C.range(vertex, vertex+1)		

		this
	}
	
	/** Set the `normal` of `vertex`. */
	def setVertexNormal(vertex:Int, normal:NumberSeq3):TrianglesMesh = setVertexNormal(vertex, normal.x.toFloat, normal.y.toFloat, normal.z.toFloat)
	
	/** Set the normal (`x`, `y`, `z`) of `vertex`. */
	def setVertexNormal(vertex:Int, x:Float, y:Float, z:Float):TrianglesMesh = {
		val i = vertex * N.components
		val data = N.data

		data(i)   = x
		data(i+1) = y
		data(i+2) = z

		N.range(vertex, vertex+1)		

		this
	}

	/** Set the `tangent` of `vertex`. */
	def setVertexTangent(vertex:Int, tangent:NumberSeq3):TrianglesMesh = setVertexTangent(vertex, tangent.x.toFloat, tangent.y.toFloat, tangent.z.toFloat)
	
	/** Set the tangent (`x`, `y`, `z`) of `vertex`. */
	def setVertexTangent(vertex:Int, x:Float, y:Float, z:Float):TrianglesMesh = {
		val i = vertex * Nt.components
		val data = Nt.data
		
		data(i)   = x
		data(i+1) = y
		data(i+2) = z

		Nt.range(vertex, vertex+1)		

		this
	}

	/** Set the `bitangent` of `vertex`. */
	def setVertexBiTangent(vertex:Int, bitangent:NumberSeq3):TrianglesMesh = setVertexBiTangent(vertex, bitangent.x.toFloat, bitangent.y.toFloat, bitangent.z.toFloat)
	
	/** Set the bi-tangent (`x`, `y`, `z`) of `vertex`. */
	def setVertexBiTangent(vertex:Int, x:Float, y:Float, z:Float):TrianglesMesh = {
		val i = vertex * Nb.components
		val data = Nb.data
		
		data(i)   = x
		data(i+1) = y
		data(i+2) = z

		Nb.range(vertex, vertex+1)		

		this
	}

	/** Set the texture coordinates `uv` of `vertex`. */
	def setVertexTexCoords(vertex:Int, uv:NumberSeq2):TrianglesMesh = setVertexTexCoords(vertex, uv.x.toFloat, uv.y.toFloat)

	/** Set the texture coordinates (`u`, `v`) of `vertex`. */
	def setVertexTexCoords(vertex:Int, u:Float, v:Float):TrianglesMesh = {
		val i = vertex * T.components
		val data = T.data

		data(i)   = u
		data(i+1) = v

		T.range(vertex, vertex+1)

		this
	}

	/** Set the bones references (`b0`, `b1`, `b2`, `b3`) (up to four bone indices) of `vertex. */
	def setVertexBones(vertex:Int, b0:Int, b1:Int = -1, b2:Int = -1, b3:Int = -1):TrianglesMesh = {
		val i = vertex * B.components
		val data = B.data

		data(i+0) = b0
		data(i+1) = b1
		data(i+2) = b2
		data(i+3) = b3

		B.range(vertex, vertex+1)

		this
	}

	/** Set the influence of each bone (`w0`, `w1`, `w2`, `w3`) (up to four bones) of `vertex`. */
	def setVertexWeights(vertex:Int, w0:Float, w1:Float=0, w2:Float=0, w3:Float=0):TrianglesMesh = {
		val i = vertex * W.components
		val data = W.data

		data(i+0) = w0
		data(i+1) = w1
		data(i+2) = w2
		data(i+3) = w3

		W.range(vertex, vertex+1)

		this
	}
	
	/** Map the `t`-th triangle to the vertex attributes indices (`a`, `b`, `c`). */
	def setTriangle(triangle:Int, a:Int, b:Int, c:Int):TrianglesMesh = {
		val i = triangle * I.verticesPerPrim
		val data = I.data

		data(i)   = a
		data(i+1) = b
		data(i+2) = c
		
		I.range(triangle, triangle+1)

		this
	}

	/** Change the coordinates of vertices already identified by `triangle`. This method
	  * assumes the indices of the three vertices of `triangle` are already set. The corresponding
	  * position vertex attribute is then modified. */
	def setTrianglePosition(triangle:Int,
		x0:Float, y0:Float, z0:Float,
		x1:Float, y1:Float, z1:Float,
		x2:Float, y2:Float, z2:Float) {
		val (i0, i1, i2) = getTriangle(triangle)
		setVertexPosition(i0, x0, y0, z0)
		setVertexPosition(i1, x1, y1, z1)
		setVertexPosition(i2, x2, y2, z2)
	}

	/** Change the coordinates of vertices already identified by `triangle`. This method
	  * assumes the indices of the three vertices of `triangle` are already set. The corresponding
	  * position vertex attribute is then modified. */
	def setTrianglePosition(triangle:Int, p0:Point3, p1:Point3, p2:Point3) {
		setTrianglePosition(triangle,
			p0.x.toFloat, p0.y.toFloat, p0.z.toFloat,
			p1.x.toFloat, p1.y.toFloat, p1.z.toFloat,
			p2.x.toFloat, p2.y.toFloat, p2.z.toFloat)
	}

	/** Change the normal of vertices already identified by `triangle`. This method
	  * assumes the indices of the three vertices of `triangle` are already set. The corresponding
	  * normal vertex attribute is then modified. */
	def setTriangleNormal(triangle:Int, x:Float, y:Float, z:Float) {
		val (i0, i1, i2) = getTriangle(triangle)
		setVertexNormal(i0, x, y, z)
		setVertexNormal(i1, x, y, z)
		setVertexNormal(i2, x, y, z)
	}

	/** Change the `normal` of vertices already identified by `triangle`. This method
	  * assumes the indices of the three vertices of `triangle` are already set. The corresponding
	  * `normal` vertex attribute is then modified. */
	def setTriangleNormal(triangle:Int, normal:Vector3) { setTriangleNormal(triangle, normal.x.toFloat, normal.y.toFloat, normal.z.toFloat) }

	/** Change the color of vertices already identified by `triangle`. This method
	  * assumes the indices of the three vertices of `triangle` are already set. The corresponding
	  * color vertex attribute is then modified. */
	def setTriangleColor(triangle:Int, r:Float, g:Float, b:Float, a:Float) {
		val (i0, i1, i2) = getTriangle(triangle)
		setVertexColor(i0, r, g, b, a)
		setVertexColor(i1, r, g, b, a)
		setVertexColor(i2, r, g, b, a)
	}

	/** Change the `color` of vertices already identified by `triangle`. This method
	  * assumes the indices of the three vertices of `triangle` are already set. The corresponding
	  * `color` vertex attribute is then modified. */
	def setTriangleColor(triangle:Int, color:Rgba) { setTriangleColor(triangle, color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat) }

	/** Set individual index `i` to reference vertex attribute `a`, independantly of triangles. */
	def setIndex(i:Int, a:Int):TrianglesMesh = {
		val data = I.data
		
		data(i) = a
		
		I.range(i/3, i/3+1)

		this
	}

	/** Compute the normals of a `triangle` whose indices and position vertex attribute are already set.
	  * The computed normals are all perpendicular to the `triangle` face. The points are taken 
	  * in order (first to second, then first to third) to create two vectors and compute a third perpendicular
	  * vector), use parameter `opposite` to change the orientation of the resulting normal. */
	def computeTriangleNormals(triangle:Int, opposite:Boolean=false) {
		val (i0, i1, i2) = getTriangle(triangle)
		//autoComputeTriangleNormals(triangle, getVertexPosition(i0), getVertexPosition(i1), getVertexPosition(i2), opposite)
		val p0 = getVertexPosition(i0)
		val p1 = getVertexPosition(i1)
		val p2 = getVertexPosition(i2)
		val v0 = Vector3(p0, p1)
		val v1 = Vector3(p0, p2)
		val n  = v1 X v0
		
		n.normalize
		
		if(opposite)
		     setTriangleNormal(triangle, n.oppose)
		else setTriangleNormal(triangle, n)
	}
	
	/** The position of `vertex`. */
	def getVertexPosition(vertex:Int):Point3 = {
		val i = vertex * V.components
		val data = V.data
		Point3(data(i), data(i+1), data(i+2))
	}

	/** The texture coordinates of `vertex`. */
	def getVertexTexCoord(vertex:Int):Point2 = {
		val i = vertex * T.components
		val data = T.data
		Point2(data(i), data(i+1))
	}

	/** The normal of `vertex`. */
	def getVertexNormal(vertex:Int):Vector3 = {
		val i = vertex * N.components
		val data = N.data
		Vector3(data(i), data(i+1), data(i+2))
	}

	/** The tangent of `vertex`. */
	def getVertexTangent(vertex:Int):Vector3 = {
		val i = vertex * Nt.components
		val data = Nt.data
		Vector3(data(i), data(i+1), data(i+2))
	}

	/** The bi-tangent of `vertex`. */
	def getVertexBiTangent(vertex:Int):Vector3 = {
		val i = vertex * Nb.components
		val data = Nb.data
		Vector3(data(i), data(i+1), data(i+2))
	}
	
	/** The `t`-th triangle in the index array. The returned tuple contains the three indices of
	  * points in the vertex attributes. */
	def getTriangle(t:Int):(Int,Int,Int) = {
		val i = t * I.verticesPerPrim
		val data = I.data
		(data(i), data(i+1), data(i+2))
	}

	/** Compute the tangents and optionnaly bi-tangents of the whole mesh.
	  *
	  * The mesh must already have the triangles indices, the vertices position, normals, and texture coordinates, all
	  * four are needed to build the tangents.
	  *
	  * Actually this procedure assumes that each vertex is owned by only one triangle. TODO: If several triangles share
	  * a vertex, we need to average values on all triangles using this vertex.
	  */
	def autoComputeTangents(alsoComputeBiTangents:Boolean=true) {
		// Do some verifications.

		if(modifiable) throw new RuntimeException("you mus compute tangents outside of begin()/end()")
		if(!hasElements) throw new RuntimeException("TODO autoComputeTangents with ordered triangles")
		if(T eq null) throw new RuntimeException("autoComputeTangents() needs texture coordinates to produce tangents")
		if(N eq null) throw new RuntimeException("autoComputeTangents() needs normals to produce tangents")

		// Add (or replace) a tangent buffer.

		if(Nt ne null) { Nt.dispose; Nt = null; meshAttributes.remove(VertexAttribute.Tangent) }
		if(Nb ne null) { Nb.dispose; Nb = null; meshAttributes.remove(VertexAttribute.Bitangent) }

		Nt = addMeshAttribute(VertexAttribute.Tangent, 3)

		if(alsoComputeBiTangents)
			Nb = addMeshAttribute(VertexAttribute.Bitangent, 3)

		begin()

		// Compute the relations between triangles and points.

		val toTriangles = vertexToTriangle

		// Now we can build the tangents ...

		var i = 0
		val n = V.vertexCount ; assert(n == toTriangles.size)

		while(i < n) {	// for each vertex
			if(toTriangles(i) ne null) {
				var T  = toTriangles(i)(0)		// TODO : Actually assume the vertex is shared by no other triangle than the first one !!
				var (p0, p1, p2) = triangle(T)	//        we do not "average" all the triangles...

				assert(p0 == i || p1 == i || p2 == i)

				val P0 = position(p0)
				val P1 = position(p1)
				val P2 = position(p2)

				val UV0 = texCoord(p0)
				val UV1 = texCoord(p1)
				val UV2 = texCoord(p2)

				val x1 = P1.x - P0.x
				val y1 = P1.y - P0.y
				val z1 = P1.z - P0.z
				
				val x2 = P2.x - P0.x
				val y2 = P2.y - P0.y
				val z2 = P2.z - P0.z
				
				val s1 = UV1.x - UV0.x
				val t1 = UV1.y - UV0.y
				
				val s2 = UV2.x - UV0.x
				val t2 = UV2.y - UV0.y

				var d    = (s1 * t2 - s2 * t1); assert(d != 0)
				val r    = 1.0f / d
	        	val sdir = Vector3((t2 * x1 - t1 * x2) * r, (t2 * y1 - t1 * y2) * r, (t2 * z1 - t1 * z2) * r)
	        	val tdir = Vector3((s1 * x2 - s2 * x1) * r, (s1 * y2 - s2 * y1) * r, (s1 * z2 - s2 * z1) * r)

	        	// Orthogonalize the tangent with the normal (Gram-Schmidt).
	        	// tangent[a] = (t - n * Dot(n, t)).Normalize();
	        
	        	var N = normal(i) // Vector3(normalBuffer(i*3), normalBuffer(i*3+1), normalBuffer(i*3+2))
	        	N.multBy(N.dot(sdir))
	        	sdir.subBy(N)
	        	sdir.normalize

	        	// Finally add the tangent, one for each vertex.

	        	setVertexTangent(i, sdir.x.toFloat, sdir.y.toFloat, sdir.z.toFloat)

	        	if(alsoComputeBiTangents)
	 				setVertexBiTangent(i, tdir.x.toFloat, tdir.y.toFloat, tdir.z.toFloat) //biTangentBuffer.append(tdir.x.toFloat, tdir.y.toFloat, tdir.z.toFloat)       	
	 		} else {
	 			setVertexTangent(i, 0, 1, 0)
	 			if(alsoComputeBiTangents) setVertexBiTangent(i, 0, 0, 1)
	 		}


        	i += 1
		}

		end()

		assert(Nt.vertexCount == V.vertexCount) // tangentBuffer.elements == vertexBuffer.elements)
		assert(Nt.vertexCount == N.vertexCount) //tangentBuffer.elements == normalBuffer.elements)
	}

	/** Compute the relations between each vertex and each triangle. This allows, knowing a vertex,
	  * to iterate on the triangles it is connected to (triangles can share vertices). Complexity
	  * is O(n) with n triangles.
	  *
	  * Returns an array where each cell represents a vertex in the same order as the vertex
	  * position attribute buffer, and contains an array of the triangles indices that uses
	  * this vertex. It is possible that some vertice do not reference any triangle.
	  * 
	  * The returned data is not stored
	  */
	def vertexToTriangle():Array[scala.collection.mutable.ArrayBuffer[Int]] = {
		if(! hasElements)
			throw new RuntimeException("TODO compute vertex to triangle with ordered triangles")

		// Create an array of int of the same number of elements as the vertexBuffer.

		val toTriangles = new Array[ArrayBuffer[Int]](V.vertexCount)

		// For each triangle.

		val n = I.primCount
		val d = I.data
		var t = 0

		while(t < n) {
			val (p0, p1, p2) = getTriangle(t)

			if(toTriangles(p0) eq null) toTriangles(p0) = new ArrayBuffer[Int]()
			if(toTriangles(p1) eq null) toTriangles(p1) = new ArrayBuffer[Int]()
			if(toTriangles(p2) eq null) toTriangles(p2) = new ArrayBuffer[Int]()

			toTriangles(p0) += t
			toTriangles(p1) += t
			toTriangles(p2) += t

			t += 1
		}

		// t = 0
		// while(t < V.vertexCount) {	// for each vertex
		// 	val tt = toTriangles(t)
		// 	val tc = if(tt ne null) tt.length else 0
		// 	val ts = if(tt ne null) tt.mkString(",") else ""
		// 	printf("vertex(%d) has %d triangles {%s}%n", t, tc, ts)
		// 	t += 1
		// }

		toTriangles
	}

	/** Create a new [[LineMesh]] that represents the normals (using `normalsColor`, which
	  * by default is red). The mesh contains "position" and "color" attributes only. The
	  * returned mesh is not bound to any shader. */
	def newNormalsMesh(normalsColor:Rgba=Rgba.Red):LinesMesh = {
		var i     = 0
		val n     = V.vertexCount
		val nMesh = new LinesMesh(gl, n)

		nMesh.addAttributeColor
		nMesh.begin()

		while(i < n) {	// For each vertex
			val P = position(i)
			val N = normal(i)

			nMesh line(i)   ab(P, N) rgba(normalsColor)

			i += 1
		}

		nMesh.end()
		nMesh
 	}

	/** Create a new [[LineMesh]] that represents the normals (using `normalsColor`, which
	  * by default is red) and the tangents (using `tangentsColor`, which by default is
	  * green). The mesh contains "position" and "color" attributes only. The returned
	  * mesh is not bound to any shader. */
	def newNormalsTangentsMesh(normalsColor:Rgba=Rgba.Red, tangentsColor:Rgba=Rgba.Green):LinesMesh = {
		var i      = 0
		val n      = V.vertexCount // vertices.elements
		val ntMesh = new LinesMesh(gl, n * 2)

		ntMesh.addAttributeColor
		ntMesh.begin()

		while(i < n) {	// For each vertex
			val P = position(i)
			val N = normal(i)
			val T = tangent(i)

			ntMesh line(i)   ab(P, N) rgba(normalsColor)
			ntMesh line(i+n) ab(P, T) rgba(tangentsColor)

			i += 1
		}

		ntMesh.end()
		ntMesh
 	}

	// -- Attributes ---------------------------------------------------

	protected def addIndices():MeshElement = { if(I eq null) { I = addMeshElement(size, 3) }; I }

	protected def addAttributeVertex:MeshAttribute = { if(V eq null) { V = addMeshAttribute(VertexAttribute.Position, 3) }; V }

	/** Add a color vertex attribute to this mesh. */
	def addAttributeColor:MeshAttribute = { if(C eq null) { C = addMeshAttribute(VertexAttribute.Color, 4) }; C }

	/** Add a normal vertex attribute to this mesh. */
	def addAttributeNormal:MeshAttribute = { if(N eq null) { N = addMeshAttribute(VertexAttribute.Normal, 3) }; N }

	/** Add a tangent vertex attribute to this mesh. */
	def addAttributeTangent:MeshAttribute = { if(N eq null) { N = addMeshAttribute(VertexAttribute.Tangent, 3) }; N }

	/** Add a bi-tangent vertex attribute to this mesh. */
	def addAttributeBiTangent:MeshAttribute = { if(N eq null) { N = addMeshAttribute(VertexAttribute.Bitangent, 3) }; N }

	/** Add a tex-coords vertex attribute to this mesh. */
	def addAttributeTexCoord:MeshAttribute = { if(T eq null) { T = addMeshAttribute(VertexAttribute.TexCoord, 2) }; T }

	/** Add a bones index vertex attribute to this mesh. */
	def addAttributeBone:MeshAttribute = { if(B eq null) { B = addMeshAttribute(VertexAttribute.Bone, 4) }; B }

	/** Add a bones weight vertex attribute to this mesh. */
	def addAttributeWeight:MeshAttribute = { if(W eq null) { W = addMeshAttribute(VertexAttribute.Weight, 4) }; W }
}