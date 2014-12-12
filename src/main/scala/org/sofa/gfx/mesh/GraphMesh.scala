package org.sofa.gfx.mesh

import org.sofa.gfx.{SGL, VertexArray, ShaderProgram}
import org.sofa.math.{Rgba, Point3, Vector3, NumberSeq2, NumberSeq3, Triangle}
import org.sofa.nio.{IntBuffer}


/** A data structure suitable to represent a graph or part of a graph.
  * @param maxNodes The max number of nodes.
  * @param maxEdges The max number of edges. */
class GraphMesh(val maxNodes:Int, val maxEdges:Int) extends MultiMesh {

	var nodeCount = 0

	var edgeCount = 0

	/** The mutable set of coordinates. */
	protected[this] val V:MeshAttribute = addMeshAttribute(VertexAttribute.Vertex, 3)
	
	/** The mutable set of colors. */
	protected[this] lazy val C:MeshAttribute = addMeshAttribute(VertexAttribute.Color, 4)
	
	/** The mutable set of nodes. */
	protected[this] val nodes:MeshElement = new MeshElement(maxNodes, elementsPerPrimitive(0))

	/** The mutable set of edges. */
	protected[this] val edges:MeshElement = new MeshElement(maxEdges, elementsPerPrimitive(1))
    	
	// -- Mesh interface -----------------------------------------------------

	def vertexCount:Int = maxNodes * elementsPerPrimitive(0)

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

	def addNode():Int = {
		val n = nodeCount
    	nodes.theData(n) = n  		// Yes actually the nodes array is pretty not useful...

    	if(n   < nodes.beg) nodes.beg = n
    	if(n+1 > nodes.end) nodes.end = n + 1
    
    	nodeCount += 1
    	n
    }

    def addEdge(from:Int, to:Int):Int = {
    	val e = edgeCount
    	val i = e * 2

    	edges.theData(i+0) = from
    	edges.theData(i+1) = to

    	if(e   < edges.beg) edges.beg = e
    	if(e+1 > edges.end) edges.end = e + 1
    
    	edgeCount += 1
    	e
    }

    def delNode(n:Int) {
    	nodeCount -= 1

    	if(n < nodeCount) {
    		// There is nothing to do with the nodes !
    		// They always stay in the same order, however
    		// the attribute data changes.
    		delVertex(n)
    		delColor(n)
    	}
    }

    protected def delVertex(n:Int) {
    	val d = V.theData
    	val l = nodeCount * 3
    	val i = n * 3

    	d(i+0) = d(l+0)
    	d(i+1) = d(l+1)
    	d(i+2) = d(l+2)

    	if(n   < V.beg) V.beg = n
    	if(n+1 > V.end) V.end = n + 1
    }

    protected def delColor(n:Int)  {
    	val d = C.theData
    	val l = nodeCount * 4
    	val i = n * 4

    	d(i+0) = d(l+0)
    	d(i+1) = d(l+1)
    	d(i+2) = d(l+2)
    	d(i+3) = d(l+3)

    	if(n   < C.beg) C.beg = n
    	if(n+1 > C.end) C.end = n + 1    	
    }

    def delEdge(e:Int) {
    	edgeCount -= 1

    	if(e < edgeCount) {
    		val src = edgeCount * 2
    		val dst = e * 2
    		val d   = edges.theData

    		d(dst+0) = d(src+0)
    		d(dst+1) = d(src+1)

    		if(dst   < edges.beg) edges.beg = dst
    		if(dst+1 > edges.end) edges.end = dst + 1
   		}
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
		if(hasva) {
			if(updateVertices) V.update(vas(0))
			if(updateColors)   C.update(vas(0))
		}
		if(hasva(0)  && updateNodes) nodes.update(vas(0))
		if(hasva(1) && updateEdges) edges.update(vas(1))
	}

	override def toString():String = {
		"mesh(%d, %d)%n  nodes(%s)%n  edges(%s)%n  vertex(%s)%n  colors(%s)%n".format(
			nodeCount, edgeCount,
			nodes.theData.mkString(", "),
			edges.theData.mkString(", "),
			V.theData.mkString(", "),
			C.theData.mkString(", ")
		)
	}
}
