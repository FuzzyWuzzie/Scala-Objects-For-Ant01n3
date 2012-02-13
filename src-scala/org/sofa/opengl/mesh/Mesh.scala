package org.sofa.opengl.mesh

import javax.media.opengl._
import org.sofa.nio._
import org.sofa.opengl._

import GL._
import GL2._
import GL2ES2._
import GL3._ 


/** A mesh is a set of vertex data. */
trait Mesh {
    /** The set of vertices making up the mesh. */
    def vertices:FloatBuffer

    def verticesComponents = 3
    
    /** True if this mesh has a set of normals, associated with the vertices. */
    def hasNormals:Boolean = false
    
    /** True if this mesh in addition of normals, has tangents associated with the vertices. */
    def hasTangents:Boolean = false
    
    /** True if this mesh has a set of colors, associated with the vertices. */
    def hasColors:Boolean = false
    
    /** True if this mesh has a set of texture coordinates, associated with the vertices. */
    def hasTexCoords:Boolean = false
    
    /** True if the mesh needs an indice array to define which elements to draw. */
    def hasIndices:Boolean = false
    
    /** How to draw the mesh (as lines, lines loops, triangles, quads, etc.). */
    def drawAs:Int
    
    /** Create a vertex array for the mesh. */
    def newVertexArray(gl:SGL):VertexArray
}

/** Trait for meshes that have normal data. */
trait SurfaceMesh {
    /** The set of normal vectors associated with each vertex. */
    def normals:FloatBuffer
    
    def normalComponents = 3
    
    def hasNormals:Boolean = true
}

/** Trait for meshes that have normal data and tangent data. The Bi-tangents can be obtained
  * from the normal and tangents. */
trait TangentSurfaceMesh extends SurfaceMesh {
    /** The set of tangent vectors associated with each vertex and normal. */
    def tangents:FloatBuffer
    
    def tangentsComponents = 3
    
    def hasTangents:Boolean = true
}

/** Trait for meshes that have color data. */
trait ColorableMesh {
    /** The set of colors for each vertex of the mesh. */
    def colors:FloatBuffer    
    
    def colorComponents = 4
    
    def hasColors:Boolean = true
}

/** Trait for meshes that have texture coordinates. */
trait TexturableMesh {
    def texCoords:FloatBuffer
    
    def texCoordCompoenents = 2
    
    def hasColors:Boolean = true
}

/** Trait for meshes that have indices in the vertex arrays. */
trait IndexedMesh {
    def indices:IntBuffer
    
    def hasIndices:Boolean = true
}

trait MultiArrayMesh {
    def firsts:IntBuffer
    
    def counts:IntBuffer
    
    def count:Int
}

trait MultiElementMesh {
    // TODO
}
