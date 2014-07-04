package org.sofa.opengl.mesh

import scala.language.implicitConversions

import org.sofa.FileLoader
import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.opengl.io.collada.ColladaFile

// import javax.media.opengl._
// import GL._
// import GL2._
// import GL2ES2._
// import GL3._ 
import scala.collection.mutable.HashMap



// TODO 
// - Generalize the MeshAttribute so that each set of data is stored the same way.
// - Make attribute(), attributeCount(), attributes(), has(), components() generic in mesh.
// - change EditableMesh to use these features (actually it duplicates them).
// - for dynamic meshes, use the VertexAttribute names or the name of the user attributes
//   to specify what to update.
// - Store in the MeshAttribute the part that have been updated (beg, end...).



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


/** Pluggable loader for mesh sources. */
trait MeshLoader extends FileLoader {
    /** Try to open a resource, and inside this resource a given
      * `geometry` part, or throw an IOException if not available. */
    def open(resource:String, geometry:String):Mesh
}


/** Default loader for meshes, based on files and the include path, using
  * the Collada format, to read the geometry of the object.
  * This loader tries to open the given resource directly, then if not
  * found, tries to find it in each of the pathes provided by the include
  * path of [[org.sofa.opengl.io.collada.ColladaFile]]. If not found it throws an IOException. */
class ColladaMeshLoader extends MeshLoader {
    def open(resource:String, geometry:String):Mesh = {
    	val file = new ColladaFile(resource)

    	file.library.geometry(geometry).get.mesh.toMesh 
    }
}


/** Thrown when the mesh should have a vertex array but have not. */
class NoVertexArrayException(msg:String) extends Exception(msg)


object Mesh {
	var loader = new ColladaMeshLoader()
}


/** A mesh is a set of vertex data.
  * 
  * A mesh is a set of vertex attribute data. They are roughly composed of one or
  * more arrays of floats associated with an optionnal set of indices in these
  * attributes to tell how to draw the data.
  *
  * The mesh is not usable as is in an OpenGL program, you must transform it into a
  * [[org.sofa.opengl.VertexArray]]. The mesh acts as a factory to produce vertex arrays. You can create
  * as many vertex arrays as you need with one mesh. However dynamic meshes, that is
  * meshes that are able to update their attribute data in time, always remember the
  * last produced vertex array to allow to update it. */
trait Mesh {
	import VertexAttribute._

	/** Representation of a user-defined attribute.
	  * 
	  * Such attributes are set of floats (1 to 4, depending on the number of components),
	  * each one being associated to a vertex. Individual meshes have to allocate these
	  * attribute by giving the number of vertices. This encapsulate a [[FloatBuffer]]
	  * to store data.
	  *
	  * This class has a generic `set()` method allowing to change an individual
	  * attribute values, but for efficiency reasons, you have plain access to
	  * the [[theData]] field wich is a buffer of [[vertexCount]]*[[components]]
	  * floats, and [[beg]] and [[end]] that mark the extent of modifications
	  * in the buffer. If you mess with these fields, you known the consequences. */
	class MeshAttribute(val name:String, val components:Int, val vertexCount:Int) {
		
		var theData = FloatBuffer(vertexCount * components)

		var beg:Int = vertexCount

		var end:Int = 0

		/** Data under the form a float buffer. */
		def data:FloatBuffer = theData

		/** Change [[components]] values at `vertex` in the buffer.
		  * @param values must contain at least [[components]] elements.
		  * @param vertex an index as a vertex number. */
		def set(vertex:Int, values:Float*) {
			val i = vertex *  components

			if(values.length >= components) {
				if(i >= 0 && i < theData.size) {
					if(beg > vertex) beg = vertex
					if(end < vertex) end = vertex

					var j = 0

					while(j < components) {
						theData(i+j) = values(j)
						j += 1
					}
				} else {
					throw new RuntimeException(s"invalid vertex ${vertex} out of attribute buffer (size=${vertexCount})")
				}
			} else {
				throw new RuntimeException(s"no enough values passed for attribute (${values.length}), needs ${components} components")
			}
		}

		/** Update the buffer (send it to OpenGL) with the same name as this attribute if
		  * some elements have been changed. */
		def update(va:VertexArray) {
			if(end > beg) {
				va.buffer(name).update(beg, end, theData)
				beg = vertexCount
				end = 0
			}
		}
	}

	/** Last produced vertex array. */
	protected[this] var va:VertexArray = _

	/** Set of user defined vertex attributes. Allocated on demand. */
	protected[this] var meshAttributes:HashMap[String, MeshAttribute] = null

	/** Release the resource of this mesh, the mesh is no more usable after this. */
	def dispose() { if(va ne null) va.dispose }

	/** Declare a vertex attribute `name` for the mesh.
	  *
	  * The attribute is made of the given number of `components`. For example,
	  * if this is a point in 3D there are 3 components. If this is a 2D texture UV
	  * coordinates, there are 2 components. */
	def addAttribute(name:VertexAttribute, components:Int) { addAttribute(name.toString, components) }

	/** Declare a vertex attribute `name` for the mesh.
	  *
	  * The attribute is made of the given number of `components`. For example,
	  * if this is a point in 3D there are 3 components. If this is a 2D texture UV
	  * coordinates, there are 2 components. */
	def addAttribute(name:String, components:Int) {
		if(meshAttributes eq null)
			meshAttributes = new HashMap[String, MeshAttribute]()

		meshAttributes += ((name, new MeshAttribute(name, components, vertexCount)))
	}

	/** Change the value of an attribute at the given `vertex`. The `values` must
	  * have as many elements as the attribute has components.
	  * @param name The attribute name.
	  * @param vertex The vertex tied to this attribute values.
	  * @param values a set of floats one for each component. */
	def setAttribute(name:String, vertex:Int, values:Float*) {
		val data:MeshAttribute = if(meshAttributes ne null) meshAttributes.get(name).getOrElse { 
			throw new RuntimeException(s"mesh has no attribute named ${name}")
			null
		} else null
		
		data.set(vertex, values:_*)
	}

	/** Number of vertices in the mesh. */
	def vertexCount:Int

	/** A vertex attribute by its name. */
	def attribute(name:String):FloatBuffer = {
		if(meshAttributes ne null) {
			meshAttributes.get(name) match {
				case Some(x) => x.data
				case None => null
			}
		} else {
			null
		}
	}

	/** A vertex attribute by its enumeration name. */
	def attribute(name:VertexAttribute.Value):FloatBuffer = attribute(name.toString)
 
    /** Number of vertex attributes defined. */
    def attributeCount():Int = {
    	if(meshAttributes ne null)
    		 meshAttributes.size
    	else 0
    }

    /** Name and order of all the vertex attributes defined by this mesh. */
    def attributes():Array[String] = {
    	if(meshAttributes ne null)
    		(meshAttributes.map { item => item._1 }).toArray
    	else {
     		return new Array(0)   		
    	}
    }

    /** Indices in the attributes array, draw order. */
    def indices:IntBuffer = throw new RuntimeException("no indices in this mesh")

    /** Number of components of the given vertex attribute. */
    def components(name:String):Int = {
    	if(meshAttributes ne null) {
    		meshAttributes.get(name) match {
    			case Some(x) => x.components
    			case None => throw new RuntimeException("mesh has no attribute named %s".format(name))
    		}
    	} else {
			throw new RuntimeException("mesh has no attribute named %s".format(name))
    	}
    }

    /** Number of components of the given vertex attribute. */
    def components(name:VertexAttribute.Value):Int = components(name.toString)

    /** True if the vertex attribute whose name is given is defined in this mesh. */
    def has(name:String):Boolean = {
    	if(meshAttributes ne null)
    		 meshAttributes.contains(name)
    	else false
    }

    /** True if the vertex attribute whose name is given is defined in this mesh. */
    def has(name:VertexAttribute.Value):Boolean = has(name.toString)
    
    /** True if the mesh has indices in the vertex attributes to define primitives. */
    def hasIndices():Boolean = false

    /** How to draw the mesh (as points, lines, lines loops, triangles, etc.).
      * This depends on the way the data is defined. */
    def drawAs(gl:SGL):Int

    /** Draw the last vertex array created. If no vertex array has been created 
      * a NoVertexArrayException is thrown. Use the `drawAs()` method to select
      * how to draw the mesh (triangles, points, etc.). */
    def draw(gl:SGL) {
    	if(va ne null)
    		va.draw(drawAs(gl)) 
    	else throw new NoVertexArrayException("Mesh : create a vertex array first")
    }
    
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

    /** The last created vertex array. Synonym of `lastVertexArray`. */
    def lastva():VertexArray = va

    /** True if at least one vertex array was created. You can access it using `lastva()`. */
    def hasva:Boolean = (va ne null)

    /** Always called before creating a new vertex array. Hook for sub-classes. */
    protected def beforeNewVertexArray() {}

    /** Always called after creating a new vertex array. Hook for sub-classes. */
    protected def afterNewVertexArray() {}

    /** Create a vertex array for the mesh. This method will create the vertex array with
      * all the vertex attributes present in the mesh. Each one will have a location starting
      * from 0. The order follows the one given by the list of attributes given by the
      * `attributes()` method.
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