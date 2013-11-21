package org.sofa.opengl.mesh


import javax.media.opengl._
import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.math.{Point3, Rgba, Vector3}
import GL._
import GL2._
import GL2ES2._
import GL3._
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

    /** Start position of the last modification inside the coordinates array. */
    protected var vbeg = 0
    
    /** End position of the last modification inside the coordinates array. */
    protected var vend = 0
    
    /** Start position of the last modification inside the texcoords array. */
    protected var tbeg = 0
    
    /** End position of the last modification inside the texcoords array. */
    protected var tend = 0

    // -- Mesh interface ---------------------------------------

    def attribute(name:String):FloatBuffer = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => V
    		case VertexAttribute.TexCoord => T
    		case _                        => throw new RuntimeException("this mesh has no %s attribute".format(name))
    	}
    }

    def attributeCount():Int = 2

    def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.TexCoord.toString)
    
    def components(name:String):Int = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => 3
    		case VertexAttribute.TexCoord => 2
    		case _                        => throw new RuntimeException("this mesh has no %s attribute".format(name))
    	}

    }

    def has(name:String):Boolean = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex   => true
    		case VertexAttribute.TexCoord => true
    		case _                        => false
    	}    	
    }

	def drawAs():Int = GL_TRIANGLE_STRIP

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
		val data = new FloatBuffer(6*3)

		val xunit  = sqrt(3).toFloat * ratio
		val yunit  = ratio * 2f
    	val y4 = ((yunit / 4f) * perspectiveRatio)
    	val y2 = (y4 + (yunit / 4f))
    	val x2 = xunit / 2f

		setPoint(0,  0,  -y2, data)
		setPoint(1,  x2, -y4, data)
		setPoint(2,  x2,  y4, data)
		setPoint(3,  0,   y2, data)
		setPoint(4, -x2,  y4, data)
		setPoint(5, -x2, -y4, data)

		vend = 6*3

		data
	}

	protected def allocateTextures():FloatBuffer = {
		val data = new FloatBuffer(6*2)

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