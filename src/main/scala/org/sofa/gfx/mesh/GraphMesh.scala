package org.sofa.gfx.mesh

import org.sofa.gfx.{SGL, VertexArray, ShaderProgram}
import org.sofa.math.{Rgba, Point3, Vector3, NumberSeq2, NumberSeq3, Triangle}
import org.sofa.nio.{IntBuffer}


/** A data structure suitable to represent a graph or part of a graph.
  * @param nodeCount The max number of nodes.
  * @param edgeCount The max number of edges. */
class GraphMesh(val nodeCount:Int, val edgeCount:Int) extends MultiMesh {

	/** The mutable set of coordinates. */
	protected[this] val V:MeshAttribute = addMeshAttribute(VertexAttribute.Vertex, 3)
	
	/** The mutable set of colors. */
	protected[this] lazy val C:MeshAttribute = addMeshAttribute(VertexAttribute.Color, 4)
	
	/** The mutable set of nodes. */
	protected[this] val nodes:MeshElement = new MeshElement(nodeCount, elementsPerPrimitive(0))

	/** The mutable set of edges. */
	protected[this] val edges:MeshElement = new MeshElement(edgeCount, elementsPerPrimitive(1))
    	
	// -- Mesh interface -----------------------------------------------------

	def vertexCount:Int = nodeCount * elementsPerPrimitive(0)

    def elementsPerPrimitive(subMesh:Int):Int = subMesh match {
    	case 0 => 1
    	case 1 => 2
    	case _ => throw new ArrayIndexOutOfBoundsException(subMesh)
    }

    override def elements(subMesh:Int):IntBuffer = subMesh match {
    	case 0 => nodes.data
    	case 1 => edges.data
    	case _ => throw new ArrayIndexOutOfBoundsException(subMesh)
	} 

	def drawAs(gl:SGL, subMesh:Int):Int = subMesh match {
		case 0 => gl.POINTS
		case 1 => gl.LINES
		case _ => throw new ArrayIndexOutOfBoundsException(subMesh)
	}

    override def hasElements():Boolean = true

    def subMeshCount = 2

    // -- Constructive / Update interface --------------------------------------

    def vertex(v:Int, x:Float, y:Float, z:Float) {
    	val i = v * 3
    	val data = V.theData

    	data(i+0) = x
    	data(i+1) = y
    	data(i+2) = z

		if(v   < V.beg) V.beg = v
		if(v+1 > V.end) V.end = v + 1
	}

    def color(c:Int, rgba:Rgba) {
    	val i = c * 3
    	val data = C.theData

    	data(i+0) = rgba.red.toFloat
    	data(i+1) = rgba.green.toFloat
    	data(i+2) = rgba.blue.toFloat
    	data(i+3) = rgba.alpha.toFloat

    	if(c   < C.beg) C.beg = c
    	if(c+1 > C.end) C.end = c + 1
    }

	def node(n:Int, vertex:Int) {
    	nodes.theData(n) = vertex

    	if(n   < nodes.beg) nodes.beg = n
    	if(n+1 > nodes.end) nodes.end = n + 1
    }

    def edge(e:Int, vertexFrom:Int, vertexTo:Int) {
    	val i = e * 2

    	edges.theData(i+0) = vertexFrom
    	edges.theData(i+1) = vertexTo

    	if(e   < edges.beg) edges.beg = e
    	if(e+1 > edges.end) edges.end = e + 1
    }

	// -- Dynamic updating -----------------------------------------------------
	
  //   override def beforeNewVertexArray(subMesh:Int) {
		// if(has(VertexAttribute.Vertex))   V.resetMarkers
		// if(has(VertexAttribute.Color))    C.resetMarkers
		// nodes.resetMarkers
		// edges.resetMarkers
  //   }

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
    def update(gl:SGL) { update(gl, true, true, true, true) }

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. You may give a boolean for each buffer in the vertex array
	  * that you want to update or not. */
	def update(gl:SGL, updateVertices:Boolean=false, updateColors:Boolean=false, updateNodes:Boolean=false, updateEdges:Boolean=false) {
		if(va ne null) {
			if(updateVertices) V.update(va)
			if(updateColors)   C.update(va)
			if(updateNodes)    nodes.update(va)
			if(updateEdges)    edges.update(va)
		}
	}
}