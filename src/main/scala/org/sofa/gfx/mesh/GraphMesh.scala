// package org.sofa.gfx.mesh

// import org.sofa.gfx.{SGL, VertexArray, ShaderProgram}
// import org.sofa.math.{Rgba, Point3, Vector3, NumberSeq2, NumberSeq3, Triangle}
// import org.sofa.nio.{IntBuffer}


// /** A data structure suitable to represent a graph or part of a graph.
//   * @param nodeCount The max number of nodes.
//   * @param edgeCount The max numner of edges. */
// class GraphMesh(val nodeCount:Int, val edgeCount:Int) extends MultiMesh {

// 	/** The mutable set of coordinates. */
// 	protected[this] val V:MeshAttribute = addMeshAttribute(VertexAttribute.Vertex, 3)
	
// 	/** The mutable set of colors. */
// 	protected[this] lazy val C:MeshAttribute = addMeshAttribute(VertexAttribute.Color, 4)
	
// 	/** The mutable set of nodes. */
// 	protected[this] val nodes:MeshIndex = new MeshIndex(nodeCount, 1)

// 	/** The mutable set of edges. */
// 	protected[this] val edges:MeshIndex = new MeshIndex(edgeCount, 2)
    	
// 	// -- Mesh interface -----------------------------------------------------

// 	def vertexCount:Int = nodeCount * elementsPerPrimitive

//     def elementsPerPrimitive:Int = 1

// 	def drawAs(gl:SGL, indiceSet:Int):Int = gl.POINTS

// 	override def indices:IntBuffer = nodes.data

//     override def hasIndices():Boolean = true


//     // -- Constructive / Update interface --------------------------------------



// 	// -- Dynamic updating -----------------------------------------------------
	
//     override def beforeNewVertexArray() {
// 		if(has(VertexAttribute.Vertex))   V.resetMarkers
// 		if(has(VertexAttribute.Color))    C.resetMarkers
// 		nodes.resetMarkers
// 		edges.resetMarkers
//     }

// 	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
// 	  * avoid moving data between the CPU and GPU. */
//     def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true, true, true) }

// 	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
// 	  * avoid moving data between the CPU and GPU. You may give a boolean for each buffer in the vertex array
// 	  * that you want to update or not. */
// 	def updateVertexArray(gl:SGL, updateVertices:Boolean=false, updateColors:Boolean=false, updateNodes:Boolean=false, updateEdges:Boolean=false) {
// 		if(va ne null) {
// 			if(updateVertices) V.update(va)
// 			if(updateNormals)  N.update(va)
// 			if(updateNodes)    nodes.update(va)
// 			if(updateEdges)    edges.update(va)
// 		}
// 	}

// 	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
// 	  * avoid moving data between the CPU and GPU. You may give a boolean for each buffer in the vertex array
// 	  * that you want to update or not. */
// 	def updateVertexArray(gl:SGL, attributes:String*) {
// 		if(va ne null) {
// 			nodes.update(va)
// 			edges.update(va)
// 			attributes.foreach { meshAttribute(_).update(va) }
// 		}
// 	}
// }