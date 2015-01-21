package org.sofa.gfx.mesh

import scala.language.implicitConversions

import org.sofa.FileLoader
import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.gfx.io.collada.ColladaFile

import scala.collection.mutable.HashMap


/** Pluggable loader for mesh sources. */
trait MeshLoader extends FileLoader {
    /** Try to open a resource, and inside this resource a given
      * `geometry` part, or throw an IOException if not available. */
    def open(gl:SGL, resource:String, geometry:String):Mesh
}


/** Default loader for meshes, based on files and the include path, using
  * the Collada format, to read the geometry of the object.
  * This loader tries to open the given resource directly, then if not
  * found, tries to find it in each of the pathes provided by the include
  * path of [[org.sofa.gfx.io.collada.ColladaFile]]. If not found it throws an IOException. */
class ColladaMeshLoader extends MeshLoader {
    def open(gl:SGL, resource:String, geometry:String):Mesh = {
    	val file = new ColladaFile(resource)

    	file.library.geometry(geometry).get.mesh.toMesh(gl)
    }
}


/** Thrown if `bindShader` has not been called before draw. */
class NoVertexArrayException(msg:String) extends Exception(msg)

/** Thrown when a vertex attribute is not declared in a mesh. */
class NoSuchVertexAttributeException(msg:String) extends Exception(msg)

/** Thrown when a vertex index is out of bounds in a vertex attribute. */
class InvalidVertexException(msg:String) extends Exception(msg)

/** Thrown when a vertex component index is out of bounds. */
class InvalidVertexComponentException(msg:String) extends Exception(msg)

/** Thrown when a primitive index is out of bounds. */
class InvalidPrimitiveException(msg:String) extends Exception(msg)

/** Thrown when a primitive vertex index is out of bounds. */
class InvalidPrimitiveVertexException(msg:String) extends Exception(msg)

/** Thrown when drawing without having called `bindShader` or when habing called `begin`
  * but not `end`. */
class InvalidStateException(msg:String) extends Exception(msg)


object Mesh {
	var loader = new ColladaMeshLoader()
}


/** A mesh represents geometry and a way to draw it.
  *
  * The mesh trait is a very general one allowing all sorts of definition of
  * geometry. It comes as an abstraction above the handling of vertex attributes
  * and indices under the form of [[ArrayBuffer]], [[ElementBuffer]], [[VertexArray]]
  * and tied [[Shader]].
  *
  * The mesh trait is done to be inherited by concrete classes defining all kinds
  * of geometry, be it static or dynamic. For example there are three very general
  * mesh types [[PointsMesh]], [[LinesMesh]] and [[TrianglesMesh]] allowing to 
  * build geometry, but there are very specific meshes like [[CylinderMesh]] or
  * [[CubeMesh]] that represent already built elements.
  *
  * Excepted if you are a developper for specific kinds of geometry, you will not
  * have to use directly the mesh class.
  *
  * # Data
  *
  * Concerning the data stored, a mesh can be considered as a set of vertex attributes
  * and an optional set of indices into these attributes telling in which order
  * they are drawn.
  *
  * Vertex attributes is the data to draw, a vertex attribute contains for example
  * vertices positions, color, normals, texture coordinates, etc. The mesh is trait
  * is not limited in the number of vertex attribute that can be added nor in their
  * name or definition.
  *
  * There is zero or one set of indices inside these vertex attributes. Together with
  * the `drawAs` method this defines how to interpret vertex data and in which order.
  * For example if `drawAs` returns `GL_TRIANGLES` the indices are grouped by 3 and
  * define triangular faces. 
  *
  * The mesh stores all its data internally and handles the transmission of this
  * data to OpenGL. It provides draw methods to render the data. However these
  * methods are utilisable only after having bound the mesh with a shader. This
  * is done using the `bindShader` method that takes as arguments a [[ShaderProgram]] 
  * and an associative map of vertex attribute names to shader attribute names.
  * This operation makes the link between the various vertex attributes contained
  * in the mesh and the varying attributes used by the shader to render them
  * (Internally, this operation often creates a [[VertexArray]] to efficiently bind
  * and draw the data).
  *
  * # Dynamic updating
  *
  * Most mesh allow some sort of dynamic modification. This can be done using the
  * `begin()` and `end()` commands. The `begin()` method ensures the mesh will allow
  * modifications to its internal data. The `end()` method will ensure the data is
  * sent to the GL (there are several ways to handle data, by copy, by mapping, streamed,
  * etc, it depends on the sub-classes and the level of optimisation).
  *
  * You normally cannot call `draw` commands between a `begin` and `end` calls, while the data is
  * edited, it cannot be used by OpenGL. However some streaming meshes allow it.
  * There are no modification methods in this trait, they must be provided by the sub-classes
  * depending on the kind of primitive drawn.
  */
trait Mesh {
	import VertexAttribute._

	/** Tie to the GL, provided by sub-classes. */
	val gl:SGL

	/** Associated vertex array, created when `bindShader` is called, all vertex attributes
	  * must have been added. */
	protected var va:VertexArray = _

	/** Shader bound to this mesh. */
	protected var sh:ShaderProgram = _

	/** Set of user defined vertex attributes. Allocated before `bindShader`. */
	protected var meshAttributes:HashMap[String, MeshAttribute] = null

	/** The optional elements index. */
	protected var meshElements:MeshElement = null

	// XXX TODO dispose on the VA does not dispose the ArrayBuffers. But as they
	// are shared, we should probably count the references on them (in MeshAttribute ?).

	/** Release the resource of this mesh, the mesh is no more usable after this. */
	def dispose() { if(va ne null) va.dispose }

	/** Number of vertices in the mesh. */
	def vertexCount:Int

	/** Number of elements (vertices) for one primitive. For
	  * example lines uses 2 elements, triangles 3, etc. */
	def elementsPerPrimitive:Int

	/** Declare a vertex attribute `name` for the mesh.
	  *
	  * The attribute is made of the given number of `components` per vertex. For example,
	  * if this is a point in 3D there are 3 components. If this is a 2D texture UV
	  * coordinates, there are 2 components. The components are floats.
	  *
	  * If a shader was bound with a mapping of attributes to shader attribute names, you
	  * will have to call `bindShader()` anew. */
	def addAttribute(name:VertexAttribute, components:Int) { addAttribute(name.toString, components) }

	/** Declare a vertex attribute `name` for the mesh.
	  *
	  * The attribute is made of the given number of `components` per vertex. For example,
	  * if this is a point in 3D there are 3 components. If this is a 2D texture UV
	  * coordinates, there are 2 components. The components are floats.
	  *
	  * If a shader was bound with a mapping of attributes to shader attribute names, you
	  * will have to call `bindShader()` anew. */
	def addAttribute(name:String, components:Int) { addMeshAttribute(name, components) }

	/** Declare a vertex attribute `name`, shared with another `mesh`. The given `name`
	  * is also the name of the attribute in the other `mesh`.
	  *
	  * If a shader was bound with a mapping of attributes to shader attribute names, you
	  * will have to call `bindShader()` anew. */
	def addAttribute(name:String, mesh:Mesh) {
		addMeshAttribute(name, mesh.meshAttribute(name))
	}

	/** Internal method used to declare a new attribute, its `name` and the
	  * number of `components`, that is the number of values per vertex.
	  * The size of the attribute is know using `vertexCount`. There is no
	  * divisor, that is this attribute is not used for instanced rendering.
	  * The returned [[MeshAttribute]] is stored. Values are 32 bit floats. */
	protected def addMeshAttribute(name:String, components:Int):MeshAttribute = {
		addMeshAttribute(name, components, vertexCount)
	}

	/** Internal method used to declare a new attribute, its `name` and the
	  * number of `components`, that is the number of values per vertex.
	  * The returned [[MeshAttribute]] is stored. Values are 32 bit floats.
	  * `nVertices` gives the number of attributes (therefore there will
	  * be `nVertices` times `components` values in the attribute). The
	  * `divisor` is used to configure instanced attributes, that is
	  * attributes passed to each instance for instanced rendering. */
	protected def addMeshAttribute(name:String, components:Int, nVertices:Int, divisor:Int=0):MeshAttribute = {
		addMeshAttribute(name, newMeshAttribute(name, components, nVertices, divisor))
	}

	/** Internal method used to declare a new `attribute` associated to `name` in the mesh,
	  * but this attribute is already created or pertains to another mesh. */
	protected def addMeshAttribute(name:String, attribute:MeshAttribute):MeshAttribute = {
		if(va ne null) {
			va.dispose
			va = null
		}

		if(meshAttributes eq null)
			meshAttributes = new HashMap[String, MeshAttribute]()
		
		meshAttributes += name -> attribute

		attribute
	}

	/** Internal method used to declare that the mesh has an elements
	  * buffer. */
	protected def addMeshElement(primCount:Int, verticesPerPrim:Int):MeshElement = {
		if(hasElements)
			throw new InvalidStateException("The mesh already has elements")

		meshElements = newMeshElement(primCount, verticesPerPrim)

		meshElements
	}

	/** Create a new mesh attribute (but does not insert it). By default this
	  * method creates a [[MeshAtttributeCopy]] instance, override in sub-classes
	  * to change the kind of [[MeshAttribute]] created. */
	protected def newMeshAttribute(name:String, components:Int, nVertices:Int, divisor:Int):MeshAttribute = {
		new MeshAttributeCopy(gl, name, components, nVertices, divisor)
	}

	/** Create a new mesh element (but does not store it). By default this
	  * method creates a [[MeshElementCopy]] instance, override in sub-classes
	  * to change the kind of [[MeshElement]] created. */
	protected def newMeshElement(primCount:Int, verticesPerPrim:Int):MeshElement = {
		new MeshElementCopy(gl, primCount, verticesPerPrim)
	}

	/** Access a vertex attribute by its name  under the form of a [[MeshAttribute]] handling it.
	  * The [[MeshAttribute]] class provides access to the real data, depending on the implementation.
	  * You must use its `begin()` and `end()` method before changing or accessing the data it
	  * contains. */
	def meshAttribute(name:String):MeshAttribute = {
		if(meshAttributes ne null) meshAttributes.get(name).getOrElse { 
			throw new NoSuchVertexAttributeException(s"mesh has no attribute named ${name}")
		} else {
			null
		}
	}

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

    /** True if this mesh has an attribute with the given `name`. */
    def hasAttribute(name:String):Boolean = if(meshAttributes ne null) meshAttributes.contains(name) else true
    // TODO remove the test, when all mesh will be compatible.

    /** Number of components of the given vertex attribute. */
    def components(name:String):Int = {
    	if(meshAttributes ne null) {
    		meshAttributes.get(name) match {
    			case Some(x) => x.components
    			case None => throw new NoSuchVertexAttributeException("mesh has no attribute named %s".format(name))
    		}
    	} else {
			throw new NoSuchVertexAttributeException("mesh has no attribute named %s".format(name))
    	}
    }

    /** Number of components of the given vertex attribute. */
    def components(name:VertexAttribute.Value):Int = components(name.toString)

    /** For some vertex attributes, when doing instanced drawing, the values are different for
      * each instance of the base drawing. Such attributes use the divisor to tell when the
      * values must change for the given instance. */
    def divisor(name:String):Int = {
    	if(meshAttributes ne null) {
    		meshAttributes.get(name) match {
    			case Some(x) => x.divisor
    			case None => throw new NoSuchVertexAttributeException("mesh has no attribute named %s".format(name))
    		}
    	} else {
    		throw new NoSuchVertexAttributeException("mesh has no attribute named %s".format(name))
    	}
    }

    /** True if the vertex attribute whose name is given is defined in this mesh. */
    def has(name:String):Boolean = {
    	if(meshAttributes ne null)
    		 meshAttributes.contains(name)
    	else false
    }

    /** True if the vertex attribute whose name is given is defined in this mesh. */
    def has(name:VertexAttribute.Value):Boolean = has(name.toString)
    
    /** True if the mesh has elements indices in the vertex attributes to define primitives. */
    def hasElements():Boolean = (meshElements ne null)

// Attribute and element change commands
// These methods require that begin be called

// XXX modifications should be done according to a specific mesh class.

	/** Set of attributes actually "mapped", that is for which `begin()` has been called. */
	protected var mapped =  new HashMap[String, MeshAttribute]()

	protected var elementsMapped:Boolean = false

	/** This method must be called before any change is made to the data in the mesh.
	  * If no `names` are given, all attributes will allow modification, as
	  * well as the optional elements, else only the attributes whose name has
	  * been given will allow it. The spatial name "Elements" allows modifications
	  * on the elements buffer.
	  *
	  * Begining modifications on attributes or elements can be costly (since they
	  * must be sent to the GL), therefore if you know you will only change one or
	  * a part of the attributes and not the others, prefer passing their names.
	  *
	  * Once `begin()` has been called, `end()` must be called after modification to
	  * the mesh data so that informations are sent to the GL and before any GL command
	  * that could use the mesh data. This means that you must not start any GL command
	  * inside the `begin()` and `end()` calls that could touch the data of this mesh. */
	def begin(names:String*) {
		if(mapped.size > 0) 
			throw new RuntimeException("cannot call begin() twice, call end() first.")

		if(names.length == 0) {
			meshAttributes.foreach { item =>
				mapped += item
				item._2.begin
			}
			beginElements
		} else {
			names.foreach { name =>
				if(name == "Elements") {
					beginElements
				} else {
					val att = meshAttribute(name)
					att.begin
					mapped += name -> att
				}
			}			
		}

	}

	/** This method must be called for any change made to the data in the mesh to be
	  * sent to the GL. */
	def end() {
		mapped.foreach { _._2.end }
		mapped.clear
		endElements
	}

	/** True if between calls to `begin()` and `end()`. */
	def modifiable():Boolean = (mapped.size > 0 || elementsMapped)

	/** Same as calling `begin()`, `code()` and `end()`. */
	def modify(names:String*)(code: =>Unit) {
		begin(names:_*)
		code
		end()
	}

	def beginElements() {
		if(!elementsMapped && hasElements) {
			meshElements.begin
			elementsMapped = true
		}
	}

	def endElements() {
		if(elementsMapped) {
			meshElements.end
			elementsMapped = false
		}
	}

	// /** Change the value of an attribute for the given `vertex`. The `values` must
	//   * have as many elements as the attribute has components. This will fail if 
	//   * `begin()` has not been called.
	//   * @param name The attribute name.
	//   * @param vertex The vertex tied to this attribute values.
	//   * @param values a set of floats one for each component. */
	// def setAttribute(name:String, vertex:Int, values:Float*) {
	// 	meshAttribute(name).set(vertex, values:_*)
	// }

	// /** A vertex attribute by its name. Null if `begin()` has not been called. */
	// def attribute(name:String):FloatBuffer = {
	// 	if(meshAttributes ne null) {
	// 		meshAttributes.get(name) match {
	// 			case Some(x) => x.data
	// 			case None => null
	// 		}
	// 	} else {
	// 		null
	// 	}
	// }

	// /** A vertex attribute by its enumeration name. Null if `begin()` has not
	//   * been called. */
	// def attribute(name:VertexAttribute.Value):FloatBuffer = attribute(name.toString)

    /** Indices of the elements to draw in the attributes array, in draw order.
      * The indices points at elements in each attribute array. */
    def elements:MeshElement = if(meshElements eq null) throw new InvalidPrimitiveException("no elements in this mesh") else meshElements

// Drawing commands

    /** How to draw the mesh (as points, lines, lines loops, triangles, etc.).
      * This depends on the way the data is defined. */
    def drawAs():Int

    /** Draw the last vertex array created. If no vertex array has been created 
      * a `NoVertexArrayException` is thrown. This uses the `drawAs()` method to select
      * how to draw the mesh (triangles, points, etc.). */
    def draw() {
    	if(! mapped.isEmpty)
    		throw new InvalidStateException("Mesh.end() must be called before draw().")
    	if(va ne null)
    		va.draw(drawAs) 
    	else throw new NoVertexArrayException("Call bindShader() before draw().")
    }

    /** Draw the `count` first primitives the last vertex array created. A
      * primitive is a line or triangle for example, it depends on the kind of mesh.
      * If no vertex array has been created a `NoVertexArrayException` is thrown.
      * This uses the `drawAs()` method to select
      * how to draw the mesh (triangles, points, etc.), and the `elementsPerPrimitive`
      * to know how many elements (vertices, colors) makes up a primitive. */
    def draw(count:Int) {
    	if(! mapped.isEmpty)
    		throw new InvalidStateException("Mesh.end() must be called before draw().")
    	if(va ne null)
    		va.draw(drawAs, count * elementsPerPrimitive)
    	else throw new NoVertexArrayException("Call bindShader() before draw().")
    }

    /** Draw `count` primitives of the last vertex array created starting at `start`. A
      * primitive is a line or triangle for example, it depends on the kind of mesh.
      * If no vertex array has been created a `NoVertexArrayException` is thrown.
      * This uses the `drawAs()` method to select
      * how to draw the mesh (triangles, points, etc.), and the `elementsPerPrimitive`
      * to know how many elements (vertices, colors) makes up a primitive. */
    def draw(start:Int, count:Int) {
    	if(! mapped.isEmpty)
    		throw new InvalidStateException("Mesh.end() must be called before draw().")
    	if(va ne null) {
    		val epp = elementsPerPrimitive
    		va.draw(drawAs, start * epp, count * epp)
    	} else {
    		throw new NoVertexArrayException("Call bindShader() before draw().")
    	}
    }

    def drawInstanced(instances:Int) {
    	if(! mapped.isEmpty)
    		throw new InvalidStateException("Mesh.end() must be called before draw().")
    	if(va ne null) {
    		val epp = elementsPerPrimitive
    		va.drawInstanced(drawAs, instances)
    	} else {
    		throw new NoVertexArrayException("Call bindShader() before draw().")
    	}
    }

    def drawInstanced(count:Int, instances:Int) {
    	if(! mapped.isEmpty)
    		throw new InvalidStateException("Mesh.end() must be called before draw().")
    	if(va ne null) {
    		val epp = elementsPerPrimitive
    		va.drawInstanced(drawAs, count * epp, instances)
    	} else {
    		throw new NoVertexArrayException("Call bindShader() before draw().")
    	}
    }

    def drawInstanced(start:Int, count:Int, instances:Int) {
    	if(! mapped.isEmpty)
    		throw new InvalidStateException("Mesh.end() must be called before draw().")
    	if(va ne null) {
    		val epp = elementsPerPrimitive
    		va.drawInstanced(drawAs, start * epp, count * epp, instances)
    	} else {
    		throw new NoVertexArrayException("Call bindShader() before draw().")
    	}
    }

// Associated vertex array, XXX this API whill change

    /** The associated vertex array.
      *
      * This is created when `bindShader` is called. */
    def vertexArray():VertexArray = va

    /** True as soon as `bindShader` has been called. After this a vertex array is created
      * and draw commands can be used. */
    def boundToShader:Boolean = (va ne null)

    /** The shader used to draw the mesh, to allocate the vertex array. Non null as soon
      * as `bindShader` has been called. */
    def shader:ShaderProgram = sh

    /** Always called before creating the vertex array. Hook for sub-classes. */
    protected def beforeBindShader() {}

    /** Always called after creating the vertex array. Hook for sub-classes. */
    protected def afterBindShader() {}

    /** Associate this mesh to the given `shader`, create a vertex array from all the vertex
      * attributes and the eventual element indices, bind the vertex attribute names to name of
      * attributes in the shader. The given `locations` map tells how to associate a vertex attribute
      * name to a shader attribute name. The given `shader` is directly used to query the position
      * of attribute names.
      * 
      * Example usage: 
      *
      *    myMesh.bindShader(myShader, "vertices" -> "V", "normals" -> "N")
      * 
      * This bind the shader `myShader` to this mesh. This shader uses two attributes `V` and `N`
      * to retrieve vertices positions and normals. The command assumes the mesh has also two vertex
      * attributes named `vertices` and `normals`. The command establish a link between these attribute
      * name in the mesh and in the shader.
      *
      * The [[VertexAttribute]] object defines common vertex attribute names, therefore you can also
      * use:
      *    
      *    import VertexAttribute._
      *    myMesh.bindShader(myShader, Vertex -> "V", Normal -> "N")
	  *
      * The vertex array is then remembered. It will be disposed only if a new vertex attribute
      * is added.
      */
    def bindShader(shader:ShaderProgram, locations:Tuple2[String,String]*):VertexArray = {
    	if(! mapped.isEmpty)
    		throw new InvalidStateException("Mesh.end() must be called before bindShader().")

    	beforeBindShader

    	val locs = new Array[Tuple4[String,Int,ArrayBuffer,Int]](locations.size)
    	var pos  = 0

    	locations.foreach { value => 
    		val attName = value._1
    		val varName = value._2
    		
    		if(!hasAttribute(attName))
    			throw new NoSuchVertexAttributeException("mesh has no attribute named '%s' (mapped to '%s')".format(attName, varName))
 
 	  		val att = meshAttribute(attName)
 
 	  		if(att.arrayBuffer eq null)
 	  			throw new InvalidStateException("mesh attribute %s -> no data, use begin/end on the attribute or the mesh to upload data.".format(attName))

    		locs(pos) = (attName, shader.getAttribLocation(varName), att.arrayBuffer, att.divisor)
    		pos += 1
    	}

    	sh = shader
    	
    	if(hasElements)
    	     va = new VertexArray(gl, meshElements.elementBuffer, locs:_*)
    	else va = new VertexArray(gl, null, locs:_*)

    	afterBindShader

    	va
    }

// Utility
    
    override def toString():String = {
    	val attrs = attributes.map { item => (item, components(item)) }

    	"mesh(%s, attributes(%d) { %s })".format(
    		if(hasElements) "elements array" else "no elements array",
    		attributeCount,
    		attrs.mkString(", ")
    	)
    }
}