package org.sofa.gfx.mesh

import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.math.Rgba

/** A cube, centered around (0, 0, 0) whose `side` can be specified.
  * 
  * The vertex data must use indices. The cube is made of single triangles and therefore must be
  * drawn using "triangle" mode. Triangles are in CW order. 
  *
  * As with each [[Mesh]], you can use any vertex attribute you desire and add them using
  * `addAttribute()`. However there are helper methods to allocate colors, normals,
  * tex-coords and tangents so that they are already filled correctly:
  *    - addAttributeTexCoord()
  *    - addAttributeNormal()
  *    - addAttributeColor()
  *    - addAttributeTangent()
  *
  * There is also a facility to use instanced rendering and draw multiples cubes with one call.
  */
class CubeMesh(val gl:SGL, val side:Float) extends Mesh {
    
    protected var I:MeshElement = addIndex
    protected var V:MeshAttribute = addAttributeVertex
    protected var C:MeshAttribute = _ 
    protected var N:MeshAttribute = _ 
    protected var X:MeshAttribute = _ 
    protected var T:MeshAttribute = _ 
    protected var P:MeshAttribute = _

    protected var textureRepeatS:Int = 1
    
    protected var textureRepeatT:Int = 1

	/** Define how many times the texture repeats along the S and T coordinates. This must be
	  * done before the plane is transformed to a mesh. */    
    def setTextureRepeat(S:Int, T:Int) {
        textureRepeatS = S
        textureRepeatT = T
    }

    /** Set the color of the whole cube. Automatically allocate a color vertex
      * attribute if needed. */
    def setColor(color:Rgba) {
    	if(C eq null)
    		throw new NoSuchVertexAttributeException("no color vertex attribute, add one first")

    	val n = 6 * 4 * 4
    	val d = C.data
    	
    	for(i <- 0 until n by 4) {
    		d(i+0) = color.red.toFloat
    		d(i+1) = color.green.toFloat
    		d(i+2) = color.blue.toFloat
    		d(i+3) = color.alpha.toFloat
    	}

    	C.range(0, vertexCount)
    }

    // -- Mesh interface ---------------------------------------------

    def vertexCount:Int = 24

    def elementsPerPrimitive:Int = 3
    
    def drawAs():Int = gl.TRIANGLES    

	// -- Building ---------------------------------------------------
    
    protected def addAttributeVertex:MeshAttribute = {
    	if(V eq null) {
	    	V = addMeshAttribute(VertexAttribute.Vertex, 3)
	        val s = side / 2f

	        V.begin
	        V.copy(
	        // Front
	        	-s, -s,  s,			// 0
	        	 s, -s,  s,			// 1
	        	 s,  s,  s,			// 2
	        	-s,  s,  s,			// 3
	        // Right
	        	 s, -s,  s,			// 4
	        	 s, -s, -s,			// 5
	        	 s,  s, -s,			// 6
	        	 s,  s,  s,			// 7
	        // Back
	        	 s, -s, -s,			// 8
	        	-s, -s, -s,			// 9
	        	-s,  s, -s,		 	// 10
	        	 s,  s, -s,			// 11
	        // Left
	        	-s, -s, -s,			// 12
	        	-s, -s,  s,			// 13
	        	-s,  s,  s,			// 14
	        	-s,  s, -s,			// 15
	        // Top
	        	-s,  s,  s,			// 16
	        	 s,  s,  s,			// 17
	        	 s,  s, -s,			// 18
	        	-s,  s, -s,			// 19
	        // Bottom
	        	-s, -s, -s,			// 20
	        	 s, -s, -s,			// 21
	        	 s, -s,  s,			// 22
	        	-s, -s,  s)			// 23
	        V.end
		}    
        V
    }
    
    def addAttributeTexCoord:MeshAttribute = {
    	if(X eq null) {
	        X = addMeshAttribute(VertexAttribute.TexCoord, 2)
	        val s = textureRepeatS
	        val t = textureRepeatT

	        X.begin
	        X.copy(
	        	// Front
	            0, 0,
	        	s, 0,
	        	s, t,
	        	0, t,
	        	// Right
	        	0, 0,
	        	s, 0,
	        	s, t,
	        	0, t,
	        	// Back
	        	0, 0,
	        	s, 0,
	        	s, t,
	        	0, t,
	        	// Left
	        	0, 0,
	        	s, 0,
	        	s, t,
	        	0, t,
	        	// Top
	        	0, 0,
	        	s, 0,
	        	s, t,
	        	0, t,
	        	// Bottom
	        	0, 0,
	        	s, 0,
	        	s, t,
	        	0, t
	        )
	        X.end
		}
        
        X
    }

    def addAttributeColor:MeshAttribute = {
        if(C eq null) {
	        var i = 0
	        val n = 6 * 4 * 4
	        C = addMeshAttribute(VertexAttribute.Color, 4)// FloatBuffer(n)

	        C.begin

	        val d = C.data
	        
	        while(i < n) {
	        	d(i) = 1f
	        	i += 1
	        }

	        C.range(0, vertexCount)
	        C.end
	    }

        C
    }

    def addAttributeNormal:MeshAttribute = {
    	if(N eq null) {
	        N = addMeshAttribute(VertexAttribute.Normal, 3)

	        N.begin
	       	N.copy(
		    	// Front
		         0,  0,  1,
		         0,  0,  1,
		         0,  0,  1,
		         0,  0,  1,
		    	// Right
		         1,  0,  0,
		         1,  0,  0,
		         1,  0,  0,
		         1,  0,  0,
		    	// Back
		         0,  0, -1,
		         0,  0, -1,
		         0,  0, -1,
		         0,  0, -1,
		    	// Left
		        -1,  0,  0,
		        -1,  0,  0,
		        -1,  0,  0,
		        -1,  0,  0,
		    	// Top
		         0,  1,  0,
		         0,  1,  0,
		         0,  1,  0,
		         0,  1,  0,
		    	// Bottom
		         0, -1,  0,
		         0, -1,  0,
		         0, -1,  0,
		         0, -1,  0)

	       	N.end
	    }

       	N
    }

    def addAttributeTangent:MeshAttribute = {
    	if(T eq null) {
	        T = addMeshAttribute(VertexAttribute.Tangent, 3)

	        T.begin
	        T.copy(
		    	// Front
		         1,  0,  0,
		         1,  0,  0,
		         1,  0,  0,
		         1,  0,  0,
		    	// Right
		         0,  0, -1,
		         0,  0, -1,
		         0,  0, -1,
		         0,  0, -1,
		    	// Back
		        -1,  0,  0,
		        -1,  0,  0,
		        -1,  0,  0,
		        -1,  0,  0,
		    	// Left
		         0,  0,  1,
		         0,  0,  1,
		         0,  0,  1,
		         0,  0,  1,
		    	// Top
		         1,  0,  0,
		         1,  0,  0,
		         1,  0,  0,
		         1,  0,  0,
		    	// Bottom
		        -1,  0,  0,
		        -1,  0,  0,
		        -1,  0,  0,
		        -1,  0,  0)

	        T.end
		}
        
        T
    }

    protected def addIndex:MeshElement = {
        if(I eq null) {
	        I = addMeshElement(12, 3)
	        I.begin

	        I.copy(
		        // Front
		        0, 2, 1,
		        0, 3, 2,
		        // Right
		        4, 6, 5,
		        4, 7, 6,
		        // Back
		        8, 10, 9,
		        8, 11, 10,
		        // Left
		        12, 14, 13,
		        12, 15, 14,
		        // Top
		        16, 18, 17,
		        16, 19, 18,
		        // Bottom
		        20, 22, 21,
		        20, 23, 22)

	        I.end
	    }

        I
    }

    def addAttributeInstancedOffset(count:Int, divisor:Int=1):MeshAttribute = {
		if(P eq null)
			P = addMeshAttribute(VertexAttribute.Offset, 3, count, divisor)

		P
    }

    def offset(i:Int, x:Float, y:Float, z:Float) {
    	val d = P.data
    	val v = i * 3
    	d(v+0) = x
    	d(v+1) = y
    	d(v+2) = z
    	P.range(i, i+1)
    }
}