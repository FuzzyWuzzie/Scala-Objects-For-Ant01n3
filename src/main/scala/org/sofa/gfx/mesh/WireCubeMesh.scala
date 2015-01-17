package org.sofa.gfx.mesh

import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.math.Rgba


/** A wire cube, centered around (0, 0, 0) whose `side` can be specified.
  * 
  * The vertex data must use indices. The cube is made of single lines and therefore must be
  * drawn using "line" mode. You can use colors with this mesh.
  */
class WireCubeMesh(val gl:SGL, val side:Float) extends Mesh  {
    
    protected var I:MeshElement = addIndex

    protected var V:MeshAttribute = addAttributeVertex

    protected var C:MeshAttribute = _

    // -- Mesh interface ------------------------------------

    def vertexCount:Int = 8

    def elementsPerPrimitive:Int = 2

    def drawAs():Int = gl.LINES    

	// -- Mesh building --------------------------------------------        
    
    protected def addAttributeVertex:MeshAttribute = {
        if(V eq null) {
	     	V = addMeshAttribute(VertexAttribute.Vertex, 3)
	        val s = side / 2f
	        
	        V.copy(
		        // Front
		        -s, -s,  s,			// 0
		         s, -s,  s,			// 1
		         s,  s,  s,			// 2
		        -s,  s,  s,			// 3
		        // Back
		        -s, -s, -s,			// 4
		         s, -s, -s,			// 5
		         s,  s, -s,		 	// 6
		        -s,  s, -s)			// 7
	    }

	    V
    }

    /** Add a color vertex attribute with all vertices having the same color (black). */
    def addAttributeColor:MeshAttribute = {
        if(C eq null) {
	        C = addMeshAttribute(VertexAttribute.Color, 4)
	        C.begin()

	        val n = 8 * 4
	        val d = C.data 
	        
	        for(i <- 0 until n) {
	        	d(i) = 1f
	        }    

	        C.range(0, vertexCount)
	        C.end
	    }

	    C
    }
    
    /** Set the color of each line. Allocate the color vertex attribute if needed. */
    def setColor(color:Rgba) {
    	if(C eq null) {
    		throw new NoSuchVertexAttributeException("no color attribute in wire cube mesh, add it first.")
    	}

    	val n = 8 * 4
    	val d = C.data

    	for(i <- 0 until n by 4) {
    		d(i+0) = color.red.toFloat
    		d(i+1) = color.green.toFloat
    		d(i+2) = color.blue.toFloat
    		d(i+3) = color.alpha.toFloat
    	}

    	C.range(0, vertexCount)
    }

    protected def addIndex:MeshElement = {
        if(I eq null) {
	        I = addMeshElement(12, 2)
	        I.begin
	        I.copy(
		        // Front
		        0, 1,
		        1, 2,
		        2, 3,
		        3, 0,
		        // Back
		        4, 5,
		        5, 6,
		        6, 7,
		        7, 4,
		        // Sides
		        0, 4,
		        1, 5,
		        2, 6,
		        3, 7)
	       	I.end
	    }

	    I
	}
}