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

    def node(n:Int, x:Float, y:Float, z:Float) {
    	val v = n * 3
    	val data = V.theData

    	data(v+0) = x
    	data(v+1) = y
    	data(v+2) = z

    	nodes.theData(n) = n

    }

    def nodeColor(n:Int, rgba:Rgba) {
    	val v = n * 3
    	val data = C.theData

    	data(v+0) = rgba.red.toFloat
    	data(v+1) = rgba.green.toFloat
    	data(v+2) = rgba.blue.toFloat
    	data(v+3) = rgba.alpha.toFloat
    }

    def edge(e:Int, from:Int, to:Int) {
    	val i = e * 2

    	edges.theData(i+0) = from
    	edges.theData(i+1) = to
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