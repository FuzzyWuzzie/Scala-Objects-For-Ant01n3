package org.sofa.gfx.mesh


import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.math.{Point3, Rgba, Vector3}
import scala.math._


object HexaTileMesh {
	def apply(
		ratio:Float            = 1f,
		perspectiveRatio:Float = 1f,
		textureWidth:Int       = 1,
		textureHeight:Int      = 1
			):HexaTileMesh = 
				new HexaTileMesh(ratio, perspectiveRatio, textureWidth, textureHeight)
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
	val ratio:Float            = 1f,
	val perspectiveRatio:Float = 1f,
	val textureWidth:Int       = 1,
	val textureHeight:Int      = 1
		) extends Mesh {

	/** The mutable set of coordinates. */
	protected lazy val V:FloatBuffer = allocateVertices

	/** The mutable set of texture coordinates. */
	protected lazy val T:FloatBuffer = allocateTextures

	/** Number of vertices stored in the vertex array. */
	protected val vCount = 6 * textureWidth * textureHeight

    /** Start position of the last modification inside the coordinates array. */
    protected var vbeg = 0
    
    /** End position of the last modification inside the coordinates array. */
    protected var vend = 0
    
    /** Start position of the last modification inside the texcoords array. */
    protected var tbeg = 0
    
    /** End position of the last modification inside the texcoords array. */
    protected var tend = 0

    // -- Mesh interface ---------------------------------------

    def vertexCount:Int = vCount

    override def attribute(name:String):FloatBuffer = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => V
    		case VertexAttribute.TexCoord => T
    		case _                        => super.attribute(name) //throw new RuntimeException("this mesh has no %s attribute".format(name))
    	}
    }

    override def attributeCount():Int = 2 + super.attributeCount

    override def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.TexCoord.toString) ++ super.attributes
    
    override def components(name:String):Int = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => 3
    		case VertexAttribute.TexCoord => 2
    		case _                        => super.components(name) //throw new RuntimeException("this mesh has no %s attribute".format(name))
    	}

    }

    override def has(name:String):Boolean = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => true
    		case VertexAttribute.TexCoord => true
    		case _                        => super.has(name)// false
    	}    	
    }

	def drawAs(gl:SGL):Int = gl.TRIANGLE_STRIP

    def elementsPerPrimitive:Int = 1

    override def draw(gl:SGL, count:Int) {
    	// Must be redefined to count elements well
    	if(va ne null)
    		va.draw(drawAs(gl), 2 + count)
    	else throw new NoVertexArrayException("Mesh: create a vertex array before draw")
    }

    // -- Mesh creation ------------------------------------------------

    protected def setPoint(p:Int, x:Float, y:Float, data:FloatBuffer=V) {
    	data(p*3  ) = x
    	data(p*3+1) = y
    	data(p*3+2) = 0
    }

    protected def setTexCoord(p:Int, u:Float, v:Float, data:FloatBuffer=T) {
    	data(p*2  ) = u
    	data(p*2+1) = v
    }

	protected def allocateVertices():FloatBuffer = {
		val data = FloatBuffer(vCount * 3)

		val xunit  = sqrt(3).toFloat * ratio
		val yunit  = ratio * 2f
    	val y4 = ((yunit / 4f) * perspectiveRatio)
    	val y2 = (y4 + (yunit / 4f))
    	val x2 = xunit / 2f

    	for(i <- 0 until textureWidth * textureHeight) {
			setPoint(i*6,    0,  -y2, data)
			setPoint(i*6+1,  x2, -y4, data)
			setPoint(i*6+2,  x2,  y4, data)
			setPoint(i*6+3,  0,   y2, data)
			setPoint(i*6+4, -x2,  y4, data)
			setPoint(i*6+5, -x2, -y4, data)
    	}

		vend = vCount * 3

		data
	}

	protected def allocateTextures():FloatBuffer = {
		val data = FloatBuffer(6*2)

		val xunit = sqrt(3).toFloat / 4f
    	val yunit = 1f

    	setTexCoord(0, xunit,    0,          data)
    	setTexCoord(1, xunit*2f, yunit/4f,   data)
    	setTexCoord(2, xunit*2f, yunit/4f*3, data)
    	setTexCoord(3, xunit,    yunit,      data)
    	setTexCoord(4, 0,        yunit/4f*3, data)
    	setTexCoord(5, 0,        yunit/4f,   data)

    	tend = 6*2

		data
	}

    // -- Editing ----------------------------------------------------------------


	// -- Dynamic -----------------------------------------------

    override def beforeNewVertexArray() {
		tbeg = 6*2; tend = 0; vbeg = 6*3; vend = 0
	}

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
	def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true) }

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
	def updateVertexArray(gl:SGL, updateVertices:Boolean, updateTexCoords:Boolean) {
		if(va ne null) {
			if(updateVertices && vend > vbeg) {
				if(vbeg == 0 && vend == 6*3) {
					va.buffer(VertexAttribute.Vertex.toString).update(V)
				} else {
					va.buffer(VertexAttribute.Vertex.toString).update(vbeg, vend, V)
				}

				vbeg = 6*3
				vend = 0
			}
			if(updateTexCoords && tend > tbeg) {
				if(tbeg == 0 && tend == 6*2) {
					va.buffer(VertexAttribute.TexCoord.toString).update(T)
				} else {
					va.buffer(VertexAttribute.TexCoord.toString).update(tbeg, tend, T)
				}


				tbeg = 6*2
				tend = 0
			}
		}
	}
}