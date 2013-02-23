package org.sofa.opengl.io.collada

import scala.xml.{Node, NodeSeq}
import scala.collection.mutable.{HashMap, ArrayBuffer}
import org.sofa.opengl.mesh.{Mesh, EditableMesh}
import scala.math._

/** Geometry feature companion object. */
object Geometry {
	def apply(node:Node):Geometry = new Geometry(node)
}

/** A geometry descriptor in a Collada geometry library.
  *
  * The geometry feature is made of a name and a set of meshes. */
class Geometry(node:Node) extends ColladaFeature {
	/** Name of the geometry. */
	var name = ""

	/** Id of geometry. */
	var id = ""
	
	/** The mesh. */
	var mesh:ColladaMesh = null

	parse(node)
	
	protected def parse(node:Node) {
		id         = (node \ "@id").text
		name       = (node \ "@name").text
		val meshes = (node \\ "mesh")

		if(meshes.size > 0) 
			mesh = new ColladaMesh(id, meshes.head)
		else {
			// TODO conves_mesh or spline 
			throw new RuntimeException("convex_mesh or spline not yet supported, no mesh in geometry")
		}
	}
	
	override def toString():String = "geometry(%s, %s)".format(name, mesh)
}

//------------------------------------------------------------------------------------------------------

/** One source of data in a mesh (in OpenGL terms, a vertex attribute). */
class MeshSource() {
	/** Unique identifier of the source. */
	var id:String = ""
	/** Optional name of the source. */
	var name:String = "noname"
	/** Number of components per element (for example vertices are usually made of three components). */
	var stride = 0
	/** The raw data. */
	var data:Array[Float] = null

	def this(node:Node) { this(); parse(node) }

	def this(id:String, name:String, stride:Int, data:Array[Float]) {
		this()
		this.id     = id
		this.name   = name
		this.stride = stride
		this.data   = data
	}
	
	/** Data stride. */
	def getData(index:Int):Array[Float] = {
//Console.err.println("mesh %s get data stride = %d".format(name, stride))
		val res = new Array[Float](stride)
		var i   = 0
		var p   = index * stride

		while(i < stride) {
			res(i) = data(p+i)
			i += 1
		}
		
		res
	}

	/** Number of elements defined in the data (elements are made of `stride` components). */
	def elementCount():Int = data.length / stride
	
	protected def parse(node:Node) {
		id     = (node \ "@id").text
		name   = (node \ "@name").text; if(name.length == 0) name = "noname"
		stride = (node \ "technique_common" \ "accessor" \ "@stride").text.toInt
		data   = (node \ "float_array").text.trim.split("\\s+").map(_.toFloat)
	}
	
	override def toString():String = "source(%s, stride %d, %d floats)".format(name, stride, data.length) 
}

//------------------------------------------------------------------------------------------------------

/** Possible type of vertex attributes. */
object Input extends Enumeration {
	val Vertex   = Value
	val Normal   = Value
	val TexCoord = Value
	val Tangent  = Value
	val Color    = Value
	val Bone     = Value
	val Weight   = Value
	val User     = Value
}	

/** Faces making up a mesh.
  * 
  * The faces are polygons. They are arranged in Collada as a list of indices in various
  * sources of input data. The number of such inputs depends on the number of attributes
  * per vertex. The data is interleaved, this means that each vertex and its attribute
  * are a set of subsequent integers. This class will allow to know how many inputs
  * are available (in OpenGL terms how many attributes per vertex), their kind (normals,
  * colors, tangents, texture coordinates, etc.), and the identifier of the source where they
  * are stored. 
  * 
  * For example if the file defines that a vertex is made of three elements:
  *   - the vertex coordinates proper,
  *   - the vertex normal,
  *   - the vertex color,
  * In this case, there will be three inputs of type "Vertex", "Normal" and "Color". Lets say
  * the data defines a single triangle. In this case there will be three times three values in
  * the data, since each vertex is defined by three integers that are indices in the sources
  * meshes. 
  * 
  * For example, the data can be:
  *    0 0 0  1 0 1  2 0 2
  * 
  * The offset for vertices is 0, the offset for normals is 1 and the offset for color is 2.
  * This means that each bunch of three numbers define a single vertex, where the first number
  * in this bunch is the vertex index, the second number is the normal index and the third
  * number is the color index. Here all vertices share the same normal, but each has a different
  * color.
  * 
  * For each kind of input you can get the name of the source mesh where the data is defined.
  * */
abstract class Faces(node:Node, val mesh:ColladaMesh) {
	// Some explanations of the (awefull) organisation of this class.
	//
	// First: THIS THING IS A GIANT SPAGHETTI BOWL.
	//
	// More seriously:
	//
	//  - The data array is a set of integer indices in various data sources in each MeshSource of the tied mesh.
	//    If the mesh has 2 vertex attributes the data length will be twice the number of vertices. With an
	//    index for the first and an index for the second vertex attribute : data is interleaved.
	//
	//  - Each input describes a kind of vertex attribute, It contains the enumeration value of this kind
	//    of vertex attribute and the name of the source in order to find it in the Mesh sources.
	//    The input thing is an array and vertex attributes are indexed in it using the same offset value
	//    as the one given in the Collada file in the set of inputs of the polylist... excepted for added
	//    sources (one that are generated), for example bones and weights that are merely appended to the
	//    inputs but are not real inputs in the collada geometry and come from the collada controller.
	//
	//  - Each revInput describes the same vertex attributes, they are referenced by the constant describing
	//    the vertex attribute and contains two ints. The first one is the offset if the data array of the
	//    index pointing in one of the mesh sources. The second is the index of the vertex attribute in
	//    the input array. They are both the same for data comming from collada geometry, but may differ in
	//    data generated like bones and weights. For these two last ones, in fact the offset is the one of
	//    the vertex data since they are the same. This means that data may contain less indices than there
	//    are vertex attributes since some indices are shared. Pfew..
	//
	// Now you understand the spaghetti reference ? Ok, why doing such a complicated thing ? In oder to
	// keep the collada data untouched. Since the goal of this part of the library is to allow generic
	// collada import.

	/** Number of faces. */
	var count = 0
	
	/** Number of components that identify a vertex in data. */
	var dataStride = 0

	/** Face vertices reference several attributes in order (vertex, normal, tex-coords, etc.). */
	var inputs:ArrayBuffer[(Input.Value,String)] = null
	
	/** Reverse input types, give the type of input and get the offset (or position in the data). */
	var revInputs = new HashMap[Input.Value,(Int,Int)]()
	
	/** References to vertex attributes stored in the sources. This is an interleaved array,
	  * where each element is composed of several integers, the vertex index in the sources,
	  * then for example the color index and the normal index. In this example, there would be
	  * three inputs. */
	var data:Array[Int] = null
	
	/** Does the vertex attribute 'input' is provided for this face set ? */
	def hasInput(input:Input.Value):Boolean = revInputs.contains(input)

	/** Position of a given input type in the interleaved data array. */
	def dataOffset(input:Input.Value):Int = revInputs.get(input).get._1
	
	/** Position of the given input in the input array (This may differ from the dataOffset to share indices
	  * in this data offset). */
	def inputIndex(input:Input.Value):Int = revInputs.get(input).get._2

	/** Get the i-th element in the array plus the offset of the given input type. */
	def getData(i:Int, input:Input.Value):Int = if(hasInput(input)) data(i + dataOffset(input)) else -1
	
	/** Get the name of the source mesh for the given input. */
	def getSource(input:Input.Value):String = inputs(inputIndex(input))._2
	
	/** Get the index-th vertex. */
	def getVertex(index:Int):Array[Float] = getAttribute(mesh.vertices, index)
	
	/** Get the index-th attributes value corresponding to the given input type. */
	def getAttribute(input:Input.Value, index:Int):Array[Float] = getAttribute(getSource(input), index)

	protected def getAttribute(source:String, index:Int):Array[Float] = mesh.sources.get(source).get.getData(index)
	
	protected def parse(node:Node) {
		count = (node \ "@count").text.toInt
		val in = (node \\ "input")
		dataStride = in.length
		inputs = new ArrayBuffer[(Input.Value,String)](dataStride+10)
		in.foreach { input => inputs += null }	// populate array, we will work with indices not in order, (horrible...)
		in.foreach { input =>
			val offset = (input \ "@offset").text.toInt
			inputs(offset) = (input \ "@semantic").text match {
				case "VERTEX"   => { revInputs += ((Input.Vertex,   (offset, offset))); (Input.Vertex,   (input \ "@source").text.substring(1)) }
				case "NORMAL"   => { revInputs += ((Input.Normal,   (offset, offset))); (Input.Normal,   (input \ "@source").text.substring(1)) }
				case "TEXCOORD" => { revInputs += ((Input.TexCoord, (offset, offset))); (Input.TexCoord, (input \ "@source").text.substring(1)) }
				case "TANGENT"  => { revInputs += ((Input.Tangent,  (offset, offset))); (Input.Tangent,  (input \ "@source").text.substring(1)) }
				case "COLOR"    => { revInputs += ((Input.Color,    (offset, offset))); (Input.Color,    (input \ "@source").text.substring(1)) }
				case _          => { revInputs += ((Input.User,     (offset, offset))); (Input.User,     (input \ "@source").text.substring(1)) }
			}
		}
		data = (node \ "p").text.trim.split("\\s+").map(_.toInt)
	}
	
	/** Transform this into a SOFA [[Mesh]], usable to draw in an OpenGL scene.
	  * Some transformations may be applyed to the original data accoding to the settings in
	  * The Collada Mesh. */
	def toMesh():Mesh

	/** Generate a list of vertices from the data given in the Collada file such that each vertex
	  * owns its own set of attributes (as needed by OpenGL). If mergeVertices is true, the procedure
	  * will try to ensure that two vertices having exactly the same attributes (all of them) will
	  * not be duplicated. The order remains the same. */
	def toVertexList():(ArrayBuffer[Int],ArrayBuffer[Vertex]) = {
		val vertices = new ArrayBuffer[Vertex]()		// The element buffer.
		val elements = new ArrayBuffer[Int]()

		if(mesh.mergeVerticesMesh) {
			val unicity  = new HashMap[Vertex,Int]()		// To test unicity, and retrieve the original.	
			var i = 0

			while(i < data.length) {
				var vertex = new Vertex(this, getData(i, Input.Vertex), getData(i, Input.Normal), getData(i, Input.TexCoord),
					getData(i, Input.Tangent), getData(i, Input.Color), getData(i, Input.Bone), getData(i, Input.Weight))
	
//Console.err.print("%s".format(vertex))

				if(!unicity.contains(vertex)) {
					val index = vertices.length
					vertices += vertex
					unicity  += ((vertex,index))
					elements += index//unicity.get(vertex).get
//Console.err.println("  -> new %d".format(index))
				} else {
					val index = unicity.get(vertex).get
					elements += index
	//				vertex = vertices(unicity.get(vertex).get)
//Console.err.println("  -> old %d".format(index))
				}
			
				i += dataStride
			}
			
			val expected = data.length / dataStride
			val obtained = unicity.size
println("Collada Faces %d original elements %d unique elements (saved %d compressed %.2f%%)".format(expected, obtained, expected-obtained, (1-(obtained.toDouble/expected.toDouble))*100))
		} else {		
			var i = 0
			while(i < data.length) {
				var vertex = new Vertex(this, getData(i, Input.Vertex), getData(i, Input.Normal), getData(i, Input.TexCoord),
					getData(i, Input.Tangent), getData(i, Input.Color), getData(i, Input.Bone), getData(i, Input.Weight))
	
				val index = vertices.length
				vertices += vertex
				elements += index			
				i += dataStride
			}
		}

// var i = 0
// vertices.foreach { vertex =>
// 	println("vertex %d -> %s".format(i,vertex))
// 	i+= 1
// }

		(elements, vertices)
	}

	protected def toMesh(elements:ArrayBuffer[Int], vertices:ArrayBuffer[Vertex]):Mesh = {
		val mesh = new EditableMesh()
	
		mesh.buildAttributes {
			vertices.foreach { vertex =>
				if(vertex.normal >= 0) {
					val norm = getAttribute(Input.Normal, vertex.normal)
					
					if(this.mesh.blenderToOpenGLCoos)
					     mesh.normal(norm(1), norm(2), norm(0))
					else mesh.normal(norm(0), norm(1), norm(2))
				}
				if(vertex.texcoord >= 0) {
					val uv = getAttribute(Input.TexCoord, vertex.texcoord)
					mesh.texCoord(uv(0), uv(1))
				}
				if(vertex.bone >= 0) {
					val bone = getAttribute(Input.Bone, vertex.bone)
					mesh.bone(bone(0),bone(1),bone(2),bone(3))
				}
				if(vertex.weight >= 0) {
					val weight = getAttribute(Input.Weight, vertex.weight)
					mesh.weight(weight(0),weight(1),weight(2),weight(3))
				}
				if(vertex.index >= 0) {
					val vert = getVertex(vertex.index)

					if(this.mesh.blenderToOpenGLCoos)
					     mesh.vertex(vert(1), vert(2), vert(0))
					else mesh.vertex(vert(0), vert(1), vert(2))
				}
			}
		}
		
		mesh.buildIndices {
			elements.foreach { mesh.index(_) }
		}
		
		mesh
	}	
}

/** Represents internally a vertex.
  *
  * This class allows :
  *  - To represent the integer indices in the various vertex attribute arrays.
  *  - To easily hash and compare it in order to implement a the vertex merging
  *    to remove multiple versions of the same vertex. */
class Vertex(val faces:Faces, val index:Int, val normal:Int, val texcoord:Int, val tangent:Int, val color:Int, val bone:Int, val weight:Int) {
	/** Linear array of all the values associated with the vertex, the x, y, z positions, the normal, the color, tex coords,
	  * etc.. */
	protected var repr:Array[Float] = null

	/** Obtain the flat representation of this vertex. */
	def getRepr():Array[Float] = { computeRepr; repr }

	/** Compute a representation in an array where each value for each attribute of a single vertex are stored, this
	  * allows fast (somewhat...) comparison, and hash value obtension. */
	protected def computeRepr() {
		if(repr eq null) {
			repr = new Array[Float](24)

			if(index >=0) {
				val vert = faces.getVertex(index)
				repr(0) = vert(0).toFloat
				repr(1) = vert(1).toFloat
				repr(2) = vert(2).toFloat
			}
			if(normal >= 0) {
				val norm = faces.getAttribute(Input.Normal, normal) 
				repr(3) = norm(0).toFloat
				repr(4) = norm(1).toFloat
				repr(5) = norm(2).toFloat
			}
			if(texcoord >= 0) {
				val uv = faces.getAttribute(Input.TexCoord, texcoord)
				repr(6) = uv(0).toFloat
				repr(7) = uv(1).toFloat
			}
			if(tangent >= 0) {
				val tan = faces.getAttribute(Input.Tangent, tangent)
				repr(8)  = tan(0).toFloat
				repr(9)  = tan(1).toFloat
				repr(10) = tan(2).toFloat
				repr(11) = tan(3).toFloat
			} 
			if(color >= 0) {
				val clr = faces.getAttribute(Input.Color, color)
				repr(12) = clr(0).toFloat
				repr(13) = clr(1).toFloat
				repr(14) = clr(2).toFloat
				repr(15) = clr(3).toFloat
			}
			if(bone >= 0) {
				val boneSource = faces.mesh.sources.get(faces.getSource(Input.Bone)).get
				val stride     = boneSource.stride

				if(stride > 4) {
					Console.err.println("WARNING : SOFA meshes do not support vertices that reference more than 4 bones.")
				}

				val bon = faces.getAttribute(Input.Bone, bone)

				repr(16) = bon(0).toFloat
				repr(17) = bon(1).toFloat	// We know the bone stride is 4
				repr(18) = bon(2).toFloat
				repr(19) = bon(3).toFloat
			}
			if(weight >= 0) {	
				val weightSource = faces.mesh.sources.get(faces.getSource(Input.Weight)).get
				val stride       = weightSource.stride

				if(stride > 4) {
					Console.err.println("WARNING : SOFA meshes do not support vertices that reference more than 4 weights.")
				}

				val wei = faces.getAttribute(Input.Weight, weight)

				repr(20) = wei(0).toFloat
				repr(21) = wei(1).toFloat	// We know the weight stride is 4
				repr(22) = wei(2).toFloat
				repr(23) = wei(3).toFloat
			}
		}
	}

	override def equals(other:Any):Boolean = other match {
		case v:Vertex => {
			computeRepr
			val otherRepr = v.getRepr
			var equals = true
			var i = 0
			while(i < repr.length && equals) {
				if(repr(i) != otherRepr(i)) {
					equals = false
				}
				i += 1
			}

			equals
		}
		case _ => false
	}
	
	override def hashCode():Int = {
		// yeah !
		computeRepr

		var i = 1
		var sum = 41 + java.lang.Float.floatToIntBits(repr(0))
		while(i < repr.length) {
			sum = (41 * sum + java.lang.Float.floatToIntBits(repr(i)))
			i += 1
		}

		sum

		//(41*(41*(41*(41*(41*(41*(41*(41*(41*(41*(41*(41*(41*(41*(41*(41*(41+x)+y)+z)+nx)+ny)+nz)+u)+v)+tx)+ty)+tz)+tw)+r)+g)+b)+a)+bo)
	}

	override def toString():String = {
		computeRepr
		"vertex(%d, %d, %d, %d, %d, %d, %d { %s })".format(index, normal, texcoord, tangent, color, bone, weight, repr.mkString(", "))
	}
}

//------------------------------------------------------------------------------------------------------

/** Faces only made of triangles. */
class Triangles(node:Node, mesh:ColladaMesh) extends Faces(node, mesh) {
	parse(node)
	override def toString():String = "triangles(%d, [%s], data %d(%d))".format(count, inputs.mkString(","), data.length, (data.length/inputs.length)/3)
	def toMesh():Mesh = { val (elements,vertices) = toVertexList; toMesh(elements, vertices) }
}

//------------------------------------------------------------------------------------------------------

/** Faces made of polygons with an arbitrary number of vertices. */
class Polygons(node:Node, mesh:ColladaMesh) extends Faces(node, mesh) {
	/** Number of vertex per face. */
	var vcount:Array[Int] = null
	
	parse(node)
	
	protected override def parse(node:Node) {
		super.parse(node)
		vcount = (node \ "vcount").text.trim.split("\\s+").map(_.toInt)
	}
	
	override def toString():String = "polys(%d, [%s], vcount %d, data %d)".format(count, inputs.mkString(","), vcount.length, data.length)
	
	/** */
	def toMesh():Mesh = {
		var (elements,vertices) = triangulate
		toMesh(elements,vertices)
	}
	
	def triangulate():(ArrayBuffer[Int],ArrayBuffer[Vertex]) = {
		var (elements,vertices) = toVertexList
		val elems = new ArrayBuffer[Int]
		var i = 0
		var v = 0

		while(i < vcount.length) {
			val vn = vcount(i)
			
			if(vn == 4) {
				// Emit two triangles.
				elems += elements(v)
				elems += elements(v+1)
				elems += elements(v+3)
				elems += elements(v+1)
				elems += elements(v+2)
				elems += elements(v+3)
				v += 4
			} else if(vn == 3) {
				// Emit the triangle as is.
				elems += elements(v)
				elems += elements(v+1)
				elems += elements(v+2)
				v += 3
			} else {
				throw new RuntimeException("only quads and triangles are possible actually :(")
			}
			
			i += 1
		}
		
		(elems,vertices)
	}
}

//------------------------------------------------------------------------------------------------------

/** Describe a mesh (source (vertex attributes), and indices in the source under the form of faces.
  * This mesh format offer a convertion toward SOFA meshes */
class ColladaMesh(val id:String, node:Node) {
	
	/** Try to merge vertices with exactly the same values (for position, but also color, normals, etc.) */
	var mergeVerticesMesh:Boolean = false

	/** Invert x with y, y with z and z with x. Useful to pass from Blender coordinates to OpenGL ones. */
	var blenderToOpenGLCoos:Boolean = false

	/** All the vertex attributes. */
	val sources = new HashMap[String,MeshSource]()
	
	/** What source is the main vertex position attribute ? */
	var vertices:String = ""
	
	/** Indices into the source to build the real geometry. */
	var faces:Faces = null

	/** Optionnal controller. */
	var controller:Controller = null
		
	parse(node)
	
	/** Try to merge vertices with exactly the same values (for position, but also color, normals, etc.)
	  * This setting is applyed when the mesh is transformed to a SOFA [[Mesh]], when calling [[toMesh()]]. */	
	def mergeVertices(on:Boolean) { mergeVerticesMesh = on }

	/** Try to swap axis considering the source uses Blender axis and pass them to OpenGL axis.
	  * This means that the x becomes y, the y becomes z and the z becomes x. This setting is applyed
	  * when the mesh is transformed to a SOFA [[Mesh]] when calling [[toMesh()]]. */
	def blenderToOpenGL(on:Boolean) { blenderToOpenGLCoos = on }
	
	/** Set the optionnal controller on this geometry. */
	def setController(c:Controller) {
		if(id != c.skin.source)
			throw new RuntimeException("controller tied with the wrong mesh, ids do not match (controller(%s) != mesh(%s))".format(id, c.skin.source))

		controller = c

		// Form a bone array and a weight array from the controller.

		val vertexAttr = sources.get(vertices).getOrElse(throw new RuntimeException("no vertex source??"))

		if(controller.skin.vertexCount != vertexAttr.elementCount)
			throw new RuntimeException("controller defines bones/weights for %d vertices, but mesh has %d vertices!".format(controller.skin.vertexCount, vertexAttr.elementCount))

		val (bones, weights) = controller.skin.computeBonesAndWeights(4, controller.skin.stride)	// Max stride of 4		
		val stride = 4
		val n      = controller.skin.vertexCount
		// Console.err.println("--stide = %d--------------".format(stride))
		// for(i <- 0 until n) {
		// 	Console.err.print("vertex %d -> bone( ".format(i))
		// 	for(j <- 0 until stride) {
		// 		Console.err.print("%.0f ".format(bones(i*stride+j)))
		// 	}
		// 	Console.err.print("  weight( ")
		// 	for(j <- 0 until stride) {
		// 		Console.err.print("%.3f ".format(weights(i*stride+j)))
		// 	}
		// 	Console.err.println(")")
		// }

		// Add them as sources.

		sources += (("GeneratedBone",   new MeshSource("GeneratedBone",   "GeneratedBone",   stride, bones)))
		sources += (("GeneratedWeight", new MeshSource("GeneratedWeight", "GeneratedWeight", stride, weights)))

		// And tie them in the faces list.

		val voffset = faces.dataOffset(Input.Vertex)
		val offset  = faces.inputs.length

		faces.inputs += ((Input.Bone, "GeneratedBone"))
		faces.inputs += ((Input.Weight, "GeneratedWeight"))
		faces.revInputs += ((Input.Bone, (voffset, offset)))		// Offset is the one of the vertex array.
		faces.revInputs += ((Input.Weight, (voffset, offset+1)))	// Idem.
	}

	protected def parse(node:Node) {
		processSources(node \\ "source")
		processVertices((node \ "vertices").head)
		processFaces(node)
	}
	
	protected def processSources(nodes:NodeSeq) {
		nodes.foreach { source => val src = new MeshSource(source); sources += ((src.id, src)) }
	}
	
	protected def processVertices(vert:Node) {
		vertices = (vert \ "input" \ "@source").text.substring(1)
	}
	
	protected def processFaces(node:Node) {
		val triangles = (node \\ "triangles")
		val polylist = (node \\ "polylist")
		
		if(!triangles.isEmpty) {
			faces = new Triangles(triangles.head, this)
		} else if(!polylist.isEmpty) {
			faces = new Polygons(polylist.head, this)
		}
	}

	/** Transform the Collada data to a SOFA Mesh object.
	  * See [[mergeVertices(Boolean)]] and [[blenderToOpenGL(Boolean)]]. */
	def toMesh():Mesh = faces.toMesh
	
	override def toString():String = "mesh(%s(%s), %s)".format(sources(vertices).name, sources.values.mkString(","), faces)
}