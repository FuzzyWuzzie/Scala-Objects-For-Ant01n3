package org.sofa.gfx.mesh.shapes

import org.sofa.gfx.SGL
import org.sofa.nio._
import org.sofa.gfx.mesh._

import scala.math._


/** Create a circle centered at (0,0,0) with given number of `sides` on the XZ plane.
  *
  * By default the `radius` is 1.
  * 
  * The circle is made of lines. 
  * 
  * The data is usable to draw directly the vertices or to use indices. Vertices are given in
  * order, following the trigonometric direction. They must be drawn in "line loop" mode.
  */
class CircleMesh(val gl:SGL, val sides:Int, val radius:Double = 1.0) extends Mesh {
	
    protected var I:MeshElement = addIndices

	protected var V:MeshAttribute = addAttributeVertex    

    // -- Mesh interface -------------------------------

    def vertexCount:Int = sides

    def elementsPerPrimitive:Int = sides
        
    override def drawAs():Int = gl.LINE_LOOP
    
    // -- Building -------------------------------------

    protected def addAttributeVertex():MeshAttribute = {
    	if(V eq null) {
    		V = addMeshAttribute(VertexAttribute.Position, 3)

	    	V.begin

		    val buf = V.data
		    val kstep = (2 * Pi) / sides
		    var k = 0.0
		    var i = 0
		    val n = sides * 3

		    while(i < n) {
		        buf(i+0) = (cos(k) * radius).toFloat
		        buf(i+1) = 0f
		        buf(i+2) = (sin(k) * radius).toFloat
		        
		        k += kstep
		    	i += 3
		    }

		    V.range(0, vertexCount)
		    V.end
		}

	    V
	}
	
	protected def addIndices():MeshElement = {
		if(I eq null) {
			I = addMeshElement(sides, 3)

			I.begin

		    val d = I.data
		    var i = 0

		    while(i < sides) {
		    	d(i) = i
		    	i += 1
		    }
		    
		    I.range(0, sides)
		    I.end
		}

	    I
	}
}