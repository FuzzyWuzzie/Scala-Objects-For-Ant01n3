package org.sofa.gfx.mesh


import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.math.{Point3, Rgba, Vector3}
import scala.math._


object HexaTileMesh {
	def apply(
		gl:SGL,
		ratio:Float            = 1f,
		perspectiveRatio:Float = 1f,
		textureWidth:Int       = 1,
		textureHeight:Int      = 1
			):HexaTileMesh = 
				new HexaTileMesh(gl, ratio, perspectiveRatio, textureWidth, textureHeight)
}


/** A set of independent tiles.
  * 
  * This class creates a set of tiles each centered around (0,0) with the
  * same size. Each tile only differs by its texture coordinates. The mesh
  * allows to choose which tile to draw.
  *
  * You select the number of tiles using the `textureWidth` and `textureHeight`.
  * These size indicate a grid of texture elements
  */
class HexaTileMesh(
	val gl:SGL,
	val ratio:Float            = 1f,
	val perspectiveRatio:Float = 1f,
	val textureWidth:Int       = 1,
	val textureHeight:Int      = 1
		) extends Mesh {

	/** The mutable set of coordinates. */
	protected var V:MeshAttribute = addAttributeVertex

	/** The mutable set of texture coordinates. */
	protected var T:MeshAttribute = addAttributeTexCoord

	/** Number of vertices stored in the vertex array. */
	protected val vCount = 6 * textureWidth * textureHeight

    // -- Mesh interface ---------------------------------------

    def vertexCount:Int = vCount

	def drawAs():Int = gl.TRIANGLE_STRIP

    def elementsPerPrimitive:Int = 1

    override def draw(count:Int) {
    	// Must be redefined to count elements well
    	if(va ne null)
    		va.draw(drawAs, 2 + count)
    	else throw new NoVertexArrayException("Mesh: create a vertex array before draw")
    }

    // -- Mesh creation ------------------------------------------------

    protected def setPoint(p:Int, x:Float, y:Float, data:FloatBuffer) {
    	data(p*3  ) = x
    	data(p*3+1) = y
    	data(p*3+2) = 0
    }

    protected def setTexCoord(p:Int, u:Float, v:Float, data:FloatBuffer) {
    	data(p*2  ) = u
    	data(p*2+1) = v
    }

	protected def addAttributeVertex():MeshAttribute = {
		if(V eq null) {
			V = addMeshAttribute(VertexAttribute.Vertex, 3)

			V.begin

			val data   = V.data
			val xunit  = sqrt(3).toFloat * ratio
			val yunit  = ratio * 2f
	    	val y4     = ((yunit / 4f) * perspectiveRatio)
	    	val y2     = (y4 + (yunit / 4f))
	    	val x2     = xunit / 2f

	    	for(i <- 0 until textureWidth * textureHeight) {
				setPoint(i*6,    0,  -y2, data)
				setPoint(i*6+1,  x2, -y4, data)
				setPoint(i*6+2,  x2,  y4, data)
				setPoint(i*6+3,  0,   y2, data)
				setPoint(i*6+4, -x2,  y4, data)
				setPoint(i*6+5, -x2, -y4, data)
	    	}

			V.range(0, vCount)
			V.end
		} 

		V
	}

	protected def addAttributeTexCoord():MeshAttribute = {
		if(T eq null) {
			T = addMeshAttribute(VertexAttribute.TexCoord, 2)
			T.begin
	
			val data  = T.data
			val xunit = sqrt(3).toFloat / 4f
	    	val yunit = 1f

	    	setTexCoord(0, xunit,    0,          data)
	    	setTexCoord(1, xunit*2f, yunit/4f,   data)
	    	setTexCoord(2, xunit*2f, yunit/4f*3, data)
	    	setTexCoord(3, xunit,    yunit,      data)
	    	setTexCoord(4, 0,        yunit/4f*3, data)
	    	setTexCoord(5, 0,        yunit/4f,   data)

			T.range(0, vCount)
			T.end
		}

		T
	}

	// -- Dynamic -----------------------------------------------

 //    override def beforeNewVertexArray() {
	// 	tbeg = 6*2; tend = 0; vbeg = 6*3; vend = 0
	// }

	// /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	//   * avoid moving data between the CPU and GPU. */
	// def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true) }

	// /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	//   * avoid moving data between the CPU and GPU. */
	// def updateVertexArray(gl:SGL, updateVertices:Boolean, updateTexCoords:Boolean) {
	// 	if(va ne null) {
	// 		if(updateVertices && vend > vbeg) {
	// 			if(vbeg == 0 && vend == 6*3) {
	// 				va.buffer(VertexAttribute.Vertex.toString).update(V)
	// 			} else {
	// 				va.buffer(VertexAttribute.Vertex.toString).update(vbeg, vend, V)
	// 			}

	// 			vbeg = 6*3
	// 			vend = 0
	// 		}
	// 		if(updateTexCoords && tend > tbeg) {
	// 			if(tbeg == 0 && tend == 6*2) {
	// 				va.buffer(VertexAttribute.TexCoord.toString).update(T)
	// 			} else {
	// 				va.buffer(VertexAttribute.TexCoord.toString).update(tbeg, tend, T)
	// 			}


	// 			tbeg = 6*2
	// 			tend = 0
	// 		}
	// 	}
	// }
}