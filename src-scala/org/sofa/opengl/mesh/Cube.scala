package org.sofa.opengl.mesh

import org.sofa.nio._
import org.sofa.opengl._
import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._

/** A cube, centered around (0, 0, 0) whose `side` can be specified.
  * 
  * The vertex data must use indices. The cube is made of single triangles and therefore must be
  * drawn using "triangle" mode. You can use normals, and colors with this mesh.
  */
class Cube(val side:Float)
	extends Mesh with ColorableMesh with IndexedMesh
	with SurfaceMesh with TexturableMesh with TangentSurfaceMesh {
    
    protected lazy val V:FloatBuffer = allocateVertices
    protected lazy val C:FloatBuffer = allocateColors
    protected lazy val N:FloatBuffer = allocateNormals
    protected lazy val I:IntBuffer = allocateIndices
    protected lazy val X:FloatBuffer = allocateTexCoords
    protected lazy val T:FloatBuffer = allocateTangents

    protected var textureRepeatS:Int = 1
    protected var textureRepeatT:Int = 1

    def vertices:FloatBuffer = V
    def colors:FloatBuffer = C
    def normals:FloatBuffer = N
    def indices:IntBuffer = I
    def texCoords:FloatBuffer = X
    def tangents:FloatBuffer = T
        
    override def hasColors = true

    override def hasIndices = true
    
    override def hasNormals = true
    
    override def hasTexCoords = true
    
    override def hasTangents = true
    
    protected def allocateVertices:FloatBuffer = {
        val s = side / 2f
        
        FloatBuffer(
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

    }
    
    protected def allocateTexCoords:FloatBuffer = {
        val s = textureRepeatS
        val t = textureRepeatT
        
        FloatBuffer(
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
    }

    protected def allocateColors:FloatBuffer = {
        val n   = 6 * 4 * 4
        val buf = new FloatBuffer(n)
        
        for(i <- 0 until n) {
        	buf(i) = 1f
        }
        
        buf
    }

    protected def allocateNormals:FloatBuffer = {
        FloatBuffer(
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
    }

    protected def allocateTangents:FloatBuffer = {
        FloatBuffer(
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
    }

    protected def allocateIndices:IntBuffer = {
        IntBuffer(
        // Front
        0, 1, 2,
        0, 2, 3,
        // Right
        4, 5, 6,
        4, 6, 7,
        // Back
        8, 9, 10,
        8, 10, 11,
        // Left
        12, 13, 14,
        12, 14, 15,
        // Top
        16, 17, 18,
        16, 18, 19,
        // Bottom
        20, 21, 22,
        20, 22, 23)
    }
    
    def drawAs:Int = GL_TRIANGLES
    
    def newVertexArray(gl:SGL) = new VertexArray(gl, indices, (0, 3, vertices), (0, 4, colors), (0, 3, normals))
}
