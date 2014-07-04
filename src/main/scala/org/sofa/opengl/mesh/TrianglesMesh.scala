package org.sofa.opengl.mesh

import org.sofa.opengl.{SGL, VertexArray, ShaderProgram}
import org.sofa.math.{Rgba, Point3, Vector3, NumberSeq2, NumberSeq3, Triangle}
import org.sofa.nio.{IntBuffer, FloatBuffer}


/** A dynamic set of independant triangles that can be dynamically updated and
  * tries to send only changed informations to the GL.
  *
  * There are `size` triangles at max in the mesh. */
class TrianglesMesh(val size:Int) extends Mesh {
	
	// TODO remove this awful list of attributes using the MeshAttribute interface in Mesh.

	/** The mutable set of coordinates. */
	protected[this] lazy val V:FloatBuffer = FloatBuffer(size*3*3)
	
	/** The mutable set of colors. */
	protected[this] lazy val C:FloatBuffer = FloatBuffer(size*4*3)
	
	/** The mutable set of normals, changes with the triangles. */
	protected[this] lazy val N:FloatBuffer = FloatBuffer(size*3*3)

	/** The mutable set of texture coordinates, changes with the triangles. */
	protected[this] lazy val T:FloatBuffer = FloatBuffer(size*2*3)
	
	/** The mutable set of elements to draw. */
	protected[this] lazy val I:IntBuffer = IntBuffer(size*3)
	
	/** Start position of the last modification inside the index array. */
	protected[this] var ibeg = size
	
	/** End position of the last modification inside the index array. */
	protected[this] var iend = 0
	
    /** Start position of the last modification inside the coordinates array. */
    protected[this] var vbeg = size
    
    /** End position of the last modification inside the coordinates array. */
    protected[this] var vend = 0
    
    /** Start position of the last modification inside the normal array. */
    protected[this] var nbeg = size
    
    /** End position of the last modification inside the normal array. */
    protected[this] var nend = 0
    
    /** Start position of the last modification inside the texcoords array. */
    protected[this] var tbeg = size
    
    /** End position of the last modification inside the texcoords array. */
    protected[this] var tend = 0
    
    /** Start position of the last modification inside the color array. */
    protected[this] var cbeg = size
    
    /** End position of the last modification inside the color array. */
    protected[this] var cend = 0
    	
	// -- Mesh interface -----------------------------------------------------

	def vertexCount:Int = size * 3

	override def attribute(name:String):FloatBuffer = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex   => V
			case VertexAttribute.Normal   => N
			case VertexAttribute.TexCoord => T
			case VertexAttribute.Color    => C
			case _                        => super.attribute(name) //throw new RuntimeException("this mesh has no attribute %s".format(name))
		}
	}

	override def indices:IntBuffer = I

	override def attributeCount():Int = super.attributeCount + 4

	override def attributes():Array[String] = Array[String](
				VertexAttribute.Vertex.toString,
				VertexAttribute.Normal.toString,
				VertexAttribute.TexCoord.toString,
				VertexAttribute.Color.toString) ++ super.attributes
	
	override def components(name:String):Int = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex   => 3
			case VertexAttribute.Normal   => 3
			case VertexAttribute.TexCoord => 2
			case VertexAttribute.Color    => 4
			case _                        => super.components(name) //throw new RuntimeException("this mesh has no attribute %s".format(name))
		}

	}

	override def has(name:String):Boolean = {
		VertexAttribute.withName(name) match {
			case VertexAttribute.Vertex   => true
			case VertexAttribute.Normal   => true
			case VertexAttribute.TexCoord => true
			case VertexAttribute.Color    => true
			case _                        => super.has(name)
		}
	}

    override def hasIndices():Boolean = true

	def drawAs(gl:SGL):Int = gl.TRIANGLES

	// -- Constructive interface ---------------------------------------------------
	
	def setPoint(i:Int, p:NumberSeq3) { setPoint(i, p.x.toFloat, p.y.toFloat, p.z.toFloat) }

	def setPoint(i:Int, x:Float, y:Float, z:Float) {
		val p = i*3
		
		V(p)   = x
		V(p+1) = y
		V(p+2) = z
		
		if(i < vbeg) vbeg = i
		if(i+1 > vend) vend = i+1
//Console.err.println("setPoint(%d -> %d)".format(vbeg, vend))
	}
	
	def setPointColor(i:Int, c:Rgba) { setPointColor(i, c.red.toFloat, c.green.toFloat, c.blue.toFloat, c.alpha.toFloat) }
	
	def setPointColor(i:Int, red:Float, green:Float, blue:Float, alpha:Float) {
		val p = i*4
		
		C(p  ) = red
		C(p+1) = green
		C(p+2) = blue
		C(p+3) = alpha
		
		if(i < cbeg) cbeg = i
		if(i+1 > cend) cend = i + 1
	}
	
	def setPointNormal(i:Int, n:NumberSeq3) { setPointNormal(i, n.x.toFloat, n.y.toFloat, n.z.toFloat) }
	
	def setPointNormal(i:Int, x:Float, y:Float, z:Float) {
		val p = i*3
		
		N(p)   = x
		N(p+1) = y
		N(p+2) = z
		
		if(i < nbeg) nbeg = i
		if(i+1 > nend) nend = i+1
	}

	def setPointTexCoord(i:Int, uv:NumberSeq2) { setPointTexCoord(i, uv.x.toFloat, uv.y.toFloat) }

	def setPointTexCoord(i:Int, u:Float, v:Float) {
		val p = i*2

		T(p)   = u
		T(p+1) = v

		if(i < tbeg) tbeg = i
		if(i+1 > tend) tend = i+1
	}
	
	/** Tell which vertex attribute to reference for the i-th triangle. */
	def setTriangle(i:Int, a:Int, b:Int, c:Int) {
		val p = i*3
		
		I(p)   = a
		I(p+1) = b
		I(p+2) = c
		
		if(p < ibeg) ibeg = p
		if(p+3 > iend) iend = p+3
	}
	
	/** The i-th point in the position vertex attribute. */
	def getPoint(i:Int):Point3 = {
		val p = i*3
		Point3(V(p), V(p+1), V(p+2))
	}

	/** The i-th point in the texture coordinates attribute. */
	def getPointTexCoord(i:Int):(Float,Float) = {
		val p = i*2
		(T(p), T(p+1))
	}
	
	/** The i-th triangle in the index array. The returned tuple contains the three indices of
	  * points in the position vertex array. See getPoint(Int)`. */
	def getTriangle(i:Int):(Int,Int,Int) = {
		val p = i*3
		(I(p), I(p+1), I(p+2))
	}

	// -- Dynamic updating -------------------------------------------
	
    override def beforeNewVertexArray() {
		cbeg = size; cend = 0; vbeg = size; vend = 0; tbeg = size; tend = 0; ibeg = size; iend = 0
    }

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL) {
    	updateVertexArray(gl, true, true, true, true)
    }

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. You may give a boolean for each buffer in the vertex array
	  * that you want to update or not. */
	def updateVertexArray(gl:SGL, updateVertices:Boolean=false, updateColors:Boolean=false, updateNormals:Boolean=false, updateTexCoords:Boolean=false) {
		if(va ne null) {
			if(updateVertices && vend > vbeg) {
				va.buffer(VertexAttribute.Vertex.toString).update(vbeg, vend, V)
				vbeg = size
				vend = 0
			}
			if(updateNormals && (nend > nbeg)) {
				va.buffer(VertexAttribute.Normal.toString).update(nbeg, nend, N)
				nbeg = size
				nend = 0
			}
			if(updateColors && (cend > cbeg)) {
				va.buffer(VertexAttribute.Color.toString).update(cbeg, cend, C)
				cbeg = size
				cend = 0
			}
			if(updateTexCoords && (tend > tbeg)) {
				va.buffer(VertexAttribute.TexCoord.toString).update(tbeg, tend, T)
				tbeg = size
				tend = 0
			}
			if(iend > ibeg) {
				va.indices.update(ibeg, iend, I)
				ibeg = size
				iend = 0
			}
		}
	}
}


// -------------------------------------------------------------------------------


/** Like a [[TriangleMesh]] but without an index. */
class UnindexedTrianglesMesh(val size:Int) extends Mesh {

	/** The mutable set of coordinates. */
	protected lazy val V:FloatBuffer = FloatBuffer(size*3*3)

	/** The mutable set of colors. */
	protected lazy val C:FloatBuffer = FloatBuffer(size*4*3)

	/** The mutable set of normals, changes with the triangles. */
	protected lazy val N:FloatBuffer = FloatBuffer(size*3*3)

    /** Start position of the last modification inside the coordinates array. */
    protected var vbeg = 0
    
    /** End position of the last modification inside the coordinates array. */
    protected var vend = size
    
    /** Start position of the last modification inside the color array. */
    protected var cbeg = 0
    
    /** End position of the last modification inside the color array. */
    protected var cend = size

    // -- Mesh interface ---------------------------------------

    def vertexCount:Int = size * 3

    override def attribute(name:String):FloatBuffer = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => V
    		case VertexAttribute.Normal => N
    		case VertexAttribute.Color  => C
    		case _                      => super.attribute(name) //throw new RuntimeException("this mesh has no %s attribute".format(name))
    	}
    }

    override def attributeCount():Int = 3 + super.attributeCount

    override def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.Normal.toString, VertexAttribute.Color.toString) ++ super.attributes
    
    override def components(name:String):Int = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => 3
    		case VertexAttribute.Normal => 3
    		case VertexAttribute.Color  => 4
    		case _                      => super.components(name) //throw new RuntimeException("this mesh has no %s attribute".format(name))
    	}

    }

    override def has(name:String):Boolean = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => true
    		case VertexAttribute.Normal => true
    		case VertexAttribute.Color  => true
    		case _                      =>  super.has(name) //false
    	}    	
    }

	def drawAs(gl:SGL):Int = gl.TRIANGLES

    // -- Editing ----------------------------------------------------------------

	def setTriangle(i:Int, p0:Point3, p1:Point3, p2:Point3) {
		setTriangle(i, p0.x.toFloat, p0.y.toFloat, p0.z.toFloat,
		               p1.x.toFloat, p1.y.toFloat, p1.z.toFloat,
		               p2.x.toFloat, p2.y.toFloat, p2.z.toFloat)
	}	

	def setTriangle(i:Int, t:Triangle) { setTriangle(i, t.p0, t.p1, t.p2) }

	def setTriangle(i:Int, x0:Float, y0:Float, z0:Float, x1:Float, y1:Float, z1:Float, x2:Float, y2:Float, z2:Float) {
		val p = i*3*3

		V(p)   = x0
		V(p+1) = y0
		V(p+2) = z0

		V(p+3) = x1
		V(p+4) = y1
		V(p+5) = z1

		V(p+6) = x2
		V(p+7) = y2
		V(p+8) = z2		

		if(i < vbeg) vbeg = i
		if(i+1 > vend) vend = i+1
	}

	def setColor(i:Int, c:Rgba) { setColor(i, c, c, c) }

	def setColor(i:Int, c0:Rgba, c1:Rgba, c2:Rgba) {
		val p = i*4*3

		C(p   ) = c0.red.toFloat
		C(p+ 1) = c0.green.toFloat
		C(p+ 2) = c0.blue.toFloat
		C(p+ 3) = c0.alpha.toFloat

		C(p+ 4) = c1.red.toFloat
		C(p+ 5) = c1.green.toFloat
		C(p+ 6) = c1.blue.toFloat
		C(p+ 7) = c1.alpha.toFloat

		C(p+ 8) = c2.red.toFloat
		C(p+ 9) = c2.green.toFloat
		C(p+10) = c2.blue.toFloat
		C(p+11) = c2.alpha.toFloat

		if(i < cbeg) cbeg = i
		if(i+1 > cend) cend = i + 1
	}

	def autoComputeNormal(i:Int) {
		val (p0, p1, p2) = getTriangle(i)
		autoComputeNormal(i, p0, p1, p2)
	}

	def autoComputeNormal(i:Int, p0:Point3, p1:Point3, p2:Point3) {
		val v0 = Vector3(p0, p1)
		val v1 = Vector3(p0, p2)
		val n = v1 X v0
		n.normalize
		setNormal(i, n)
	}

	def setNormal(i:Int, n:Vector3) { setNormal(i, n.x.toFloat, n.y.toFloat, n.z.toFloat) }

	def setNormal(i:Int, x:Float, y:Float, z:Float) { setNormal(i, x, y, z, x, y, z, x, y, z) }

	def setNormal(i:Int, n0:Vector3, n1:Vector3, n2:Vector3) {
		setNormal(i,
			n0.x.toFloat, n0.y.toFloat, n0.z.toFloat,
			n1.x.toFloat, n1.y.toFloat, n1.z.toFloat,
			n2.x.toFloat, n2.y.toFloat, n2.z.toFloat)
	}

	def setNormal(i:Int, x0:Float, y0:Float, z0:Float, x1:Float, y1:Float, z1:Float, x2:Float, y2:Float, z2:Float) {
		val p = i*3*3

		N(p)   = x0
		N(p+1) = y0
		N(p+2) = z0

		N(p+3) = x1
		N(p+4) = y1
		N(p+5) = z1

		N(p+6) = x2
		N(p+7) = y2
		N(p+8) = z2		

		if(i < vbeg) vbeg = i
		if(i+1 > vend) vend = i+1		
	}

	def getTriangle(i:Int):(Point3,Point3,Point3) = {
		val p = i*3*3
		(Point3(V(p)  , V(p+1), V(p+2)),
		 Point3(V(p+3), V(p+4), V(p+5)),
		 Point3(V(p+6), V(p+7), V(p+8)))
	}

	// -- Dynamic -----------------------------------------------

    override def beforeNewVertexArray() {
		cbeg = size; cend = 0; vbeg = size; vend = 0
	}

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
	def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true, true) }

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. */
	def updateVertexArray(gl:SGL, updateVertices:Boolean, updateColors:Boolean, updateNormals:Boolean) {
		if(va ne null) {
			if(updateVertices && vend > vbeg) {
				if(vbeg == 0 && vend == size) {
					va.buffer(VertexAttribute.Vertex.toString).update(V)
					if(updateNormals)
						va.buffer(VertexAttribute.Normal.toString).update(N)
				} else {
					va.buffer(VertexAttribute.Vertex.toString).update(vbeg*3, vend*3, V)
					if(updateNormals)
						va.buffer(VertexAttribute.Normal.toString).update(vbeg*3, vend*3, N)
				}

				vbeg = size
				vend = 0
			}
			if(updateColors && cend > cbeg) {
				if(cbeg == 0 && cend == size) {
					va.buffer(VertexAttribute.Color.toString).update(C)
				} else {
					va.buffer(VertexAttribute.Color.toString).update(cbeg*3, cend*3, C)
				}


				cbeg = size
				cend = 0
			}
		}
	}
}