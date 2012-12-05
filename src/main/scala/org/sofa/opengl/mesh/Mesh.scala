package org.sofa.opengl.mesh

import javax.media.opengl._
import org.sofa.nio._
import org.sofa.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._ 
import scala.collection.mutable.HashMap

/** A mesh is a set of vertex data. */
trait Mesh {
    /** The set of vertices making up the mesh. */
    def vertices:FloatBuffer

    /** The set of normal vectors associated with each vertex. */
    def normals:FloatBuffer = throw new RuntimeException("no normal attributes in this mesh")

    /** The set of tangent vectors associated with each vertex and normal. */
    def tangents:FloatBuffer = throw new RuntimeException("no tangent attributes in this mesh")

    /** The set of bitangnet vectors associated with each vertex and normal. */
    def bitangents:FloatBuffer = throw new RuntimeException("no bitangent attributes in this mesh")

    /** The set of colors for each vertex of the mesh. */
    def colors:FloatBuffer = throw new RuntimeException("no color attributes in this mesh")

    /** The set of texture coordinates of the mesh. */
    def texCoords:FloatBuffer = throw new RuntimeException("no texture coordinate attributes in this mesh")
 
    /** The set of bone indices. */
    def bones:IntBuffer = throw new RuntimeException("no bone attributes in this mesh")

    /** Indices in the attributes array, draw order. */
    def indices:IntBuffer = throw new RuntimeException("no indices in this mesh")

    /** Number of components per vertices. */
    def verticeComponents = 3
    
    /** Number of components per normal. */
    def normalComponents = 3
    
    /** Number of components per tangent. */
    def tangentComponents = 3

    /** Number of components per bitangent. */
    def bitangentComponents = 3
    
    /** Number of components per color. */
    def colorComponents = 4
    
    /** Number of components per texture coordinate. */
    def texCoordComponents = 2

    /** True if this mesh has a set of normals, associated with the vertices. */
    def hasNormals:Boolean = false
    
    /** True if this mesh in addition of normals, has tangents associated with the vertices. */
    def hasTangents:Boolean = false

    /** True if this mesh in addition of normals and tangents, has bitangents associated with the vertices. */
    def hasBitangents:Boolean = false
    
    /** True if this mesh has a set of colors, associated with the vertices. */
    def hasColors:Boolean = false
    
    /** True if this mesh has a set of texture coordinates, associated with the vertices. */
    def hasTexCoords:Boolean = false

    /** True if this mesh has bone indices for each vertex. */
    def hasBones:Boolean = false
    
    /** True if the mesh needs an index array to define which elements to draw. */
    def hasIndices:Boolean = false
    
    /** How to draw the mesh (as lines, lines loops, triangles, quads, etc.).
      * This depends on the way the data is defined. */
    def drawAs:Int
 
//        def newVertexArray(gl:SGL) = new VertexArray(gl, indices,
//    									(0, verticeComponents, vertices),
//    									(1, colorComponents, colors),
//    									(2, normalComponents, normals),
//    									(3, tangentComponents, tangents),
//    									(4, texCoordComponents, texCoords))
//    
//    def newVertexArray(gl:SGL, attributeIndices:Tuple6[Int,Int,Int,Int,Int, Int]) = {
//    	new VertexArray(gl, indices, (attributeIndices._1, verticeComponents, vertices),
//    	                             (attributeIndices._2, colorComponents, colors),
//    	                             (attributeIndices._3, normalComponents, normals),
//   	                                 (attributeIndices._4, tangentComponents, tangents),
//    	                             (attributeIndices._5, texCoordComponents, texCoords))
//    }

    /** Number of attributes defines (vertices, colors, normals, tangents, texCoords, bones). */
    def attributeCount():Int = {
    	var count = 1;	// Vertices are always present
    	
    	count += {if(hasColors) 1 else 0}
    	count += {if(hasNormals) 1 else 0}
    	count += {if(hasTangents) 1 else 0}
    	count += {if(hasBitangents) 1 else 0}
    	count += {if(hasTexCoords) 1 else 0}
    	count += {if(hasBones) 1 else 0}
    	
    	count
    }
    
    /** Create a vertex array for the mesh. This method will create the vertex array with
      * all the defined attribute arrays (colors, normals, tangents, texCoords, bones, etc.)
      * if they are available. It will associate each attribute with a location like this:
      * 
      *  - 0 = vertex,
      *  - 1 = color,
      *  - 2 = normals,
      *  - 3 = tangents,
      *  - 4 = texCoords,
      *  - 5 = bones,
      *  - 6 = bitangents.
      *  
      *  This is useful only for shaders having the possibility to associate this index
      *  with the attribute (having the 'layout' qualifier (e.g. layout(location=1)),
      *  that is under OpenGL 3). The draw mode for the array buffers is STATIC_DRAW. */
    def newVertexArray(gl:SGL):VertexArray = newVertexArray(gl, (0, 1, 2, 3, 4, 5, 6)) 

    /** Create a vertex array for the mesh. This method will create the vertex array with
      * all the defined attribute arrays (color, normals, tangents, texCoords, bones, bitangents,
      * etc.) if they are available (see the 'has*' methods). It will associate each
      * attribute with the attribute location given by the 'locations' argument in this
      * order: vertex, color, normals, tangents, texCoords, bones, bitangents. If the attribute
      * location given is negative, the attribute will not be associated. The draw mode
      * for the array buffers is STATIC_DRAW.
      * 
      * Examples:
      *    // Define all attributes, vertices at location 0, bones at location 3... no bitangents.
      *    val v = newVertexArray(gl, (0, 1, 2, 5, 4, 3, -1))
      *    // Define some attributes only, no colors, no tangents, no bones, no bitangents.
      *    val v = newVertexArray(gl, (0, -1, 6, -1, 4, -1, -1))  
      */
    def newVertexArray(gl:SGL,locations:Tuple7[Int,Int,Int,Int,Int,Int,Int]):VertexArray = newVertexArray(gl, gl.STATIC_DRAW, locations)
    
    /** Create a vertex array for the mesh. This method will create the vertex array with
      * all the defined attribute arrays (color, normals, tangents, texCoords, bones, bitangents,
      * etc.) if they are available (see the 'has*' methods). It will associate each
      * attribute with the attribute location given by the 'locations' argument in this
      * order: vertex, color, normals, tangents, texCoords, bones, bitangents. If the attribute
      * location given is negative, the attribute will not be associated. The draw mode
      * can be STATIC_DRAW, STREAM_DRAW, or DYNAMIC_DRAW.
      * 
      * Examples:
      *    // Define all attributes, vertices at location 0, bones at location 3... 
      *    val v = newVertexArray(gl, (0, 1, 2, 5, 4, 3, 6))
      *    // Define some attributes only, no colors, no tangents, no bones, no bitangents.
      *    val v = newVertexArray(gl, (0, -1, 6, -1, 4, -1, -1))  
      */
    def newVertexArray(gl:SGL, drawMode:Int, locations:Tuple7[Int,Int,Int,Int,Int,Int,Int]):VertexArray = {
    	val attribs = new Array[Tuple4[String,Int,Int,NioBuffer]](attributeCount)
    	var i = 0
    	
    	attribs(0) = ("vertices", locations._1, verticeComponents,  vertices);  i+=1
    	
    	if(hasColors     && locations._2 >= 0) { attribs(i) = ("colors",     locations._2, colorComponents,     colors);     i+=1 }
    	if(hasNormals    && locations._3 >= 0) { attribs(i) = ("normals",    locations._3, normalComponents,    normals);    i+=1 }
    	if(hasTangents   && locations._4 >= 0) { attribs(i) = ("tangents",   locations._4, tangentComponents,   tangents);   i+=1 }
    	if(hasTexCoords  && locations._5 >= 0) { attribs(i) = ("texcoords",  locations._5, texCoordComponents,  texCoords);  i+=1 }
    	if(hasBones      && locations._6 >= 0) { attribs(i) = ("bones",      locations._6, 1,                   bones);      i+=1 }
    	if(hasBitangents && locations._7 >= 0) { attribs(i) = ("bitangents", locations._7, bitangentComponents, bitangents); i+=1 }
    	
    	new VertexArray(gl, attribs:_*)
    }
    
    /** Create a vertex array from the given map of location / attributes and only
      * with the specified attributes. The draw mode for the array buffers is
      * STATIC_DRAW.
      * 
      * Example usage: newVertexArray(gl, ("vertices", 0), ("normals", 1))
      * 
      * Possible attributes are:
      *  - vertices
      *  - colors
      *  - normals
      *  - tangents
      *  - texcoords
      *  - bones
      *  - bitangents
      *
      * Attribute names are case indenpentant.
      */
    def newVertexArray(gl:SGL, locations:Tuple2[String,Int]*):VertexArray = newVertexArray(gl, gl.STATIC_DRAW, locations:_*)
    
    /** Create a vertex array from the given map of location / attributes and only
      * with the specified attributes. You can specify the draw mode for the array buffers,
      * either STATIC_DRAW, STREAM_DRAW or DYNAMIC_DRAW.
      * 
      * Example usage: newVertexArray(gl, ("vertices", 0), ("normals, 1))
      * 
      * Possible attributes are:
      *  - vertices
      *  - colors
      *  - normals
      *  - tangents
      *  - texcoords
      *  - bones
      *  - bitangents
      *
      * Attribute names are case indenpentant.
      */
    def newVertexArray(gl:SGL, drawMode:Int, locations:Tuple2[String,Int]*):VertexArray = {
    	val locs = new Array[Tuple4[String,Int,Int,NioBuffer]](locations.size)
    	var pos = 0
    	locations.foreach { value =>
    		locs(pos) = value._1.toLowerCase match {
    			case "vertices"   => ("vertices",   value._2, verticeComponents, vertices)
    			case "colors"     => ("colors",     value._2, colorComponents, colors)
    			case "normals"    => ("normals",    value._2, normalComponents, normals)
    			case "tangents"   => ("tangents",   value._2, tangentComponents, tangents)
    			case "texcoords"  => ("texcoords",  value._2, texCoordComponents, texCoords)
    			case "bones"      => ("bones",      value._2, 1, bones)
    			case "bitangents" => ("bitangents", value._2, bitangentComponents, bitangents)
    			case _ => throw new RuntimeException("Unknown key %s (available: vertices, colors, normals, tangents, texCoords or texcoords, bones, bitangents or biTangents)".format(value._1))
    		}
    		pos += 1
    	}
    	
    	if(hasIndices)
    	     new VertexArray(gl, indices, drawMode, locs:_*)
    	else new VertexArray(gl, drawMode, locs:_*)
    }
}

/** Trait for meshes that have normal data. */
trait SurfaceMesh {    
    def hasNormals:Boolean = true
}

/** Trait for meshes that have normal data and tangent data. The Bi-tangents can be obtained
  * from the normal and tangents. */
trait TangentSurfaceMesh extends SurfaceMesh {    
    def hasTangents:Boolean = true
}

trait BitangentSurfaceMesh extends SurfaceMesh {
	def hasBitangents:Boolean = true
}

/** Trait for meshes that have color data. */
trait ColorableMesh {    
    def hasColors:Boolean = true
}

/** Trait for meshes that have texture coordinates. */
trait TexturableMesh {
    def hasColors:Boolean = true
}

/** Trait for meshes that have bone indices. */
trait AnimableMesh {
    def hasBones:Boolean = true
}

/** Trait for meshes that have indices in the vertex arrays. */
trait IndexedMesh {    
    def hasIndices:Boolean = true
}

trait MultiArrayMesh {
    def firsts:IntBuffer
    
    def counts:IntBuffer
    
    def count:Int
}