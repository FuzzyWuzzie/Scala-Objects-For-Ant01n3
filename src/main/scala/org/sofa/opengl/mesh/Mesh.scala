package org.sofa.opengl.mesh

import scala.language.implicitConversions

import org.sofa.nio._
import org.sofa.opengl._

import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._ 
import scala.collection.mutable.HashMap


/** Predefined vertex attribute names. More are possible. */
object VertexAttribute extends Enumeration {
	type VertexAttribute = Value

	val Vertex    = Value("Vertex")
	val Normal    = Value("Normal")
	val Tangent   = Value("Tangent")
	val Bitangent = Value("Bitangent")
	val Color     = Value("Color")
	val TexCoord  = Value("TexCoord")
	val Bone      = Value("Bone")
	val Weight    = Value("Weight")

	/** Convert a pair (VertexAttribute,String) to a pair (String,String) as often used with Mesh.newVertexArray(). */
	implicit def VaStPair2StStPair(p:(VertexAttribute,String)):(String,String) = (p._1.toString, p._2)

	/** Convert a VertexAttribute to a String. */
	implicit def Va2St(v:VertexAttribute):String = v.toString
}


/** A mesh is a set of vertex data.
  * 
  * A mesh is a set of vertex attribute data. They are roughly composed of one or
  * more arrays of floats associated with an optionnal set of indices in these
  * attributes to tell how to draw the data.
  *
  * The mesh is not usable as is in an OpenGL program, you must transform it into a
  * [[VertexArray]]. The mesh acts as a factory to produce vertex arrays. You can create
  * as many vertex arrays as you need with one mesh. However dynamic meshes, that is
  * meshes that are able to update their attribute data in time, always remember the
  * last produced vertex array to allow to update it. */
trait Mesh {
	import VertexAttribute._

	/** Last produced vertex array. */
	protected var va:VertexArray = null 

	/** A vertex attribute by its name. */
	def attribute(name:String):FloatBuffer

	/** A vertex attribute by its enumeration name. */
	def attribute(name:VertexAttribute.Value):FloatBuffer = attribute(name.toString)
 
    /** Number of vertex attributes defined. */
    def attributeCount():Int

    /** Name and order of all the vertex attributes defined by this mesh. */
    def attributes():Array[String]

    /** Indices in the attributes array, draw order. */
    def indices:IntBuffer = throw new RuntimeException("no indices in this mesh")

    /** Number of components of the given vertex attribute. */
    def components(name:String):Int

    /** Number of components of the given vertex attribute. */
    def components(name:VertexAttribute.Value):Int = components(name.toString)

    /** True if the vertex attribute whose name is given is defined in this mesh. */
    def has(name:String):Boolean

    /** True if the vertex attribute whose name is given is defined in this mesh. */
    def has(name:VertexAttribute.Value):Boolean = has(name.toString)
    
    /** True if the mesh has indices in the vertex attributes to define primitives. */
    def hasIndices():Boolean = false

    /** How to draw the mesh (as lines, lines loops, triangles, quads, etc.).
      * This depends on the way the data is defined. */
    def drawAs:Int
    
    override def toString():String = {
    	val attrs = attributes.map { item => (item, components(item)) }

    	"mesh(%s, attributes(%d) { %s })".format(
    		if(hasIndices) "indexed" else "not indexed",
    		attributeCount,
    		attrs.mkString(", ")
    	)
    }

    /** The last created vertex array.
      *
      * Each time a vertex array is created with a mesh, it is remembered. Some
      * meshes allow to update the arrays when a change is made to the data in the
      * mesh. Such meshes are dynamic. */
    def lastVertexArray():VertexArray = va

    /** The last created vertex array. See [[lastVertexArray]]. */
    def lastva():VertexArray = va

    /** Always called before creating a new vertex array. Hook for descendants. */
    protected def beforeNewVertexArray() {}

    /** Always called after creating a new vertex array. Hook for descendants. */
    protected def afterNewVertexArray() {}

    /** Create a vertex array for the mesh. This method will create the vertex array with
      * all the vertex attributes present in the mesh. Each one will have a location starting
      * from 0. The order follows the one given by the list of attributes given by the
      * [[attributes()]] method.
      * 
      * This is useful only for shaders having the possibility to associate locations
      * with a vertex attribute (having the 'layout' qualifier (e.g. layout(location=1)),
      * that is under OpenGL 3). The draw mode for the array buffers is STATIC_DRAW. 
	  *
      * The last created vertex array is remembered by the mesh and can be accessed later,
      * and for some meshes updated from new data if the mesh is dynamic. */
    def newVertexArray(gl:SGL):VertexArray = {
    	var locs = new Array[(String,Int)](attributeCount)
    	var i    = 0
    	
    	attributes.foreach { name => locs(i) = (name, i); i+= 1 }
    	newVertexArray(gl, locs:_*)
    }
    
    /** Create a vertex array from the given map of attribute names / locations.
      * The draw mode for the array buffers is STATIC_DRAW.
      * 
      * Example usage: newVertexArray(gl, ("vertices", 0), ("normals", 1))
      * 
      * Attribute names are case insensitive.
	  *
      * The last created vertex array is remembered by the mesh and can be accessed later,
      * and for some meshes updated from new data if the mesh is dynamic. */
    def newVertexArray(gl:SGL, locations:Tuple2[String,Int]*):VertexArray = newVertexArray(gl, gl.STATIC_DRAW, locations:_*)

    /** Create a vertex array from the given map of attribute name / shader attribute names.
      * The given `shader` is directly used to query the position of attribute names.
      * The draw mode for the array buffers is STATIC_DRAW.
      * 
      * Example usage: newVertexArray(gl, gl.DYNAMIC_DRAW, myShader, ("vertices", "V"), ("normals", "N"))
      * If the shader contains input attribute named V and N.
	  *
      * The last created vertex array is remembered by the mesh and can be accessed later,
      * and for some meshes updated from new data if the mesh is dynamic. */
    def newVertexArray(gl:SGL, shader:ShaderProgram, locations:Tuple2[String,String]*):VertexArray = newVertexArray(gl, gl.STATIC_DRAW, shader, locations:_*)

    /** Create a vertex array from the given map of attribute name / shader attribute names.
      * The given `shader` is directly used to query the position of attribute names.
      * You can specify the draw mode for the array buffers, either STATIC_DRAW, STREAM_DRAW or DYNAMIC_DRAW.
      * 
      * Example usage: newVertexArray(gl, gl.DYNAMIC_DRAW, myShader, ("vertices", "V"), ("normals", "N"))
      * If the shader contains input attribute named V and N.
	  *
      * The last created vertex array is remembered by the mesh and can be accessed later,
      * and for some meshes updated from new data if the mesh is dynamic. */
    def newVertexArray(gl:SGL, drawMode:Int, shader:ShaderProgram, locations:Tuple2[String,String]*):VertexArray = {
    	val locs = new Array[Tuple2[String,Int]](locations.length)
    	var i = 0
    	while(i < locations.length) {
    		locs(i) = (locations(i)._1, shader.getAttribLocation(locations(i)._2))
    		i += 1
    	}
    	newVertexArray(gl, drawMode, locs:_*)
    }

    /** Create a vertex array from the given map of attribute names / locations.
      * You can specify the draw mode for the array buffers, either STATIC_DRAW, STREAM_DRAW or DYNAMIC_DRAW.
      * 
      * Example usage: newVertexArray(gl, gl.DYNAMIC_DRAW, ("vertices", 0), ("normals", 1))
	  *
      * The last created vertex array is remembered by the mesh and can be accessed later,
      * and for some meshes updated from new data if the mesh is dynamic. */
    def newVertexArray(gl:SGL, drawMode:Int, locations:Tuple2[String,Int]*):VertexArray = {
    	beforeNewVertexArray

    	val locs = new Array[Tuple4[String,Int,Int,NioBuffer]](locations.size)
    	var pos = 0
    	
    	locations.foreach { value => 
    		locs(pos) = (value._1, value._2, components(value._1), attribute(value._1))
    		pos += 1
    	}
    	
    	if(hasIndices)
    	     va = new VertexArray(gl, indices, drawMode, locs:_*)
    	else va = new VertexArray(gl, drawMode, locs:_*)

    	afterNewVertexArray

    	va
    }
}