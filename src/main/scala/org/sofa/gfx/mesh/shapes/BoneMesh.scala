package org.sofa.gfx.mesh.shapes

import org.sofa.nio._
import org.sofa.gfx._
import scala.math._
import org.sofa.gfx.mesh._
import org.sofa.math.Vector3
import org.sofa.math.Vector2
import java.awt.Color


/** A model of Bone as often used to represent deformation skeletons.
  *
  * The bone model has only position attribute, and indices by default, you
  * muse allocate a color attribute if needed (`addAttributeColor()`). 
  */
class BoneMesh(val gl:SGL) extends Mesh {
	
	protected var V:MeshAttribute = addAttributeVertex
	
	protected var C:MeshAttribute = _
	
	protected var I:MeshElement = addIndex

	// -- Edition ------------------------------------------
	
	override def begin(names:String*) {
		// Only color is editable.
		import VertexAttribute._
		begin(VertexAttribute.Color)
	}

	/** Set the whole color of the model. */
	def setColor(color:Color) {
		if(C eq null)
			throw new NoSuchVertexAttributeException("No color vertex attribute, add one first. See addAttributeColor().")

	    val n = 6 * 4
	    val red   = color.getRed   / 255f
        val green = color.getGreen / 255f
        val blue  = color.getBlue  / 255f
        val alpha = color.getAlpha / 255f
        val c     = C.data

	    for(i <- 0 until n by 4) {
	        c(i+0) = red
	        c(i+1) = green
	        c(i+2) = blue
	        c(i+3) = alpha
	    }

	    C.range(0, n)
	}

	// -- Mesh interface -----------------------------------

	def vertexCount:Int = 6

    def elementsPerPrimitive:Int = 3

	def drawAs():Int = gl.TRIANGLES
	
	// -- Building ------------------------------------------

	protected def addAttributeVertex:MeshAttribute = {
		if(V eq null) {
			V = addMeshAttribute(VertexAttribute.Position, 3)

	    	val n = 6 * 3
	    
		    // Four middle points 0 1 2 3
		    V.begin
		    V.copy(
			    // Right 0
		    	0.25f, 0.25f, 0,
		    	// Front 1
		    	0, 0.25f, 0.25f,
		    	// Left 2
		    	-0.25f, 0.25f, 0,
		    	// Back 3
		        0, 0.25f, -0.25f,
		    	// Bottom point 4
		     	0, 0, 0,
		    	// Top point 5
		     	0, 1, 0 )
		   	V.end
		}
	    
	    V
	}
	
	/** Add a "color" vertex attribute, and color the whole model white. */
	def addAttributeColor:MeshAttribute = {
		if(C eq null) {
			C = addMeshAttribute(VertexAttribute.Color, 4)
	    	
	    	//val n = 6 * 4
	    
	    	C.begin
	    	C.copy(
	    		1,1,1,1,
	    		1,1,1,1,
	    		1,1,1,1,
	    		1,1,1,1,
	    		1,1,1,1,
	    		1,1,1,1)
	    	C.end
		}
	    
	    C
	}
	
	protected def addIndex:MeshElement = {
		if(I eq null) {
			I = addMeshElement(8, 3)
	    	I.begin

	    	val n = 8 * 3
	    	//val buf = IntBuffer(n)
	    	val buf = I.data
	    
	    	var t=0
	    
	    	for(i <- 0 until 4) {
	       		buf(t+0) = (i)%4
	        	buf(t+1) = 4
	        	buf(t+2) = (i+1)%4

	        	buf(t+3) = 5
	        	buf(t+4) = (i)%4
	        	buf(t+5) = (i+1)%4
	        
	        	t += 6
	    	}

	    	I.range(0, 8)
	    	I.end
	    }
	    
	    I
	}
}


/** Mofidy the [[BoneMesh]] class to draw lines instead of plain faces for the bone model. */
class BoneLineMesh(gl:SGL) extends BoneMesh(gl) {

	override def drawAs():Int = gl.LINES
		
	override protected def addIndex:MeshElement = {
		if(I eq null) {
			I = addMeshElement(12, 2)
			I.begin

		    val n = 6 * 4
		    //val buf = IntBuffer(n)
		    val buf = I.data
		    
		    var t=0
		    
		    for(i <- 0 until 4) {
		        buf(t+0) = 4 // (i)%4
		        buf(t+1) = i%4
		        buf(t+2) = i%4
		        buf(t+3) = 5
		        buf(t+4) = i%4
		        buf(t+5) = (i+1)%4
		        
		        t += 6
		    }

		    I.range(0, 12)
		    I.end
		} 
	    
	    I
	}
}