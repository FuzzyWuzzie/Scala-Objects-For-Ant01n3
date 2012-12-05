package org.sofa.opengl.io.collada

import scala.xml.{Node, NodeSeq}
import scala.collection.mutable.{HashMap, ArrayBuffer}
import org.sofa.opengl.mesh.{Mesh, EditableMesh, MeshDrawMode}

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
	
	/** The mesh. */
	var mesh:ColladaMesh = null

	parse(node)
	
	protected def parse(node:Node) {
		val meshes = (node \\ "mesh")
		name = (node \ "@name").text

		if(meshes.size > 0) 
			mesh = new ColladaMesh(meshes.head)
		else {
			// TODO conves_mesh or spline 
			throw new RuntimeException("convex_mesh or spline not yet supported, no mesh in geometry")
		}
	}
	
	override def toString():String = "geometry(%s, %s)".format(name, mesh)
}

//------------------------------------------------------------------------------------------------------

/** One source of data in a mesh (in OpenGL terms, a vertex attribute). */
class MeshSource(node:Node) {
	/** Unique identifier of the source. */
	var id:String = ""
	/** Optional name of the source. */
	var name:String = "noname"
	/** Number of components per element (for example vertices are usually made of three components). */
	var stride = 0
	/** The raw data. */
	var data:Array[Float] = null

	parse(node)
	
	/** Data stride. */
	def getData(index:Int):Array[Float] = {
		val res = new Array[Float](stride)
		var i   = 0
		var p   = index * stride

		while(i < stride) {
			res(i) = data(p+i)
			i += 1
		}
		
		res
	}
	
	protected def parse(node:Node) {
		id     = (node \ "@id").text
		name   = (node \ "@name").text; if(name.length == 0) name = "noname"
		stride = (node \ "technique_common" \ "accessor" \ "@stride").text.toInt
		data   = (node \ "float_array").text.split(" ").map(_.toFloat)
	}
	
	override def toString():String = "source(%s, stride %d, %d floats)".format(name, stride, data.length) 
}

//------------------------------------------------------------------------------------------------------

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
abstract class Faces(node:Node, mesh:ColladaMesh) {
	
	/** Number of faces. */
	var count = 0
	
	/** Face vertices reference several attributes in order (vertex, normal, tex-coords, etc.). */
	var inputs:Array[(Input.Value,String)] = null
	
	/** Reverse input types, give the type of input and get the offset (or position in the data). */
	var revInputs = new HashMap[Input.Value,Int]()
	
	/** References to vertex attributes stored in the sources. This is an interleaved array,
	  * where each element is composed of several integers, the vertex index in the sources,
	  * then for example the color index and the normal index. In this example, there would be
	  * three inputs. */
	var data:Array[Int] = null

	/** Possible type of vertex attributes. */
	object Input extends Enumeration {
		val Vertex   = Value
		val Normal   = Value
		val TexCoord = Value
		val Tangent  = Value
		val Color    = Value
		val Bone     = Value
		val User     = Value
	}	
	
	/** Does the vertex attribute 'input' is provided for this face set ? */
	def hasInput(input:Input.Value):Boolean = revInputs.contains(input)

	/** Position of a given input type in the interleaved array. */
	def offset(input:Input.Value):Int = revInputs.get(input).get
	
	/** Get the i-th element in the array plus the offset of the given input type. */
	def getData(i:Int, input:Input.Value):Int = if(hasInput(input)) data(i + offset(input)) else -1
	
	/** Get the name of the source mesh for the given input. */
	def getSource(input:Input.Value):String = inputs(revInputs.get(input).get)._2
	
	/** Get the index-th vertex. */
	def getVertex(index:Int):Array[Float] = getAttribute(mesh.vertices, index)
	
	/** Get the index-th attributes value corresponding to the given input type. */
	def getAttribute(input:Input.Value, index:Int):Array[Float] = getAttribute(getSource(input), index)
	
	protected def getAttribute(source:String, index:Int):Array[Float] = mesh.sources.get(source).get.getData(index)
	
	protected def parse(node:Node) {
		count = (node \ "@count").text.toInt
		val in = (node \\ "input")
		inputs = new Array[(Input.Value,String)](in.length)
		in.foreach { input =>
			val offset = (input \ "@offset").text.toInt
			inputs(offset) = (input \ "@semantic").text match {
				case "VERTEX"   => { revInputs += ((Input.Vertex,   offset)); (Input.Vertex,   (input \ "@source").text.substring(1)) }
				case "NORMAL"   => { revInputs += ((Input.Normal,   offset)); (Input.Normal,   (input \ "@source").text.substring(1)) }
				case "TEXCOORD" => { revInputs += ((Input.TexCoord, offset)); (Input.TexCoord, (input \ "@source").text.substring(1)) }
				case "TANGENT"  => { revInputs += ((Input.Tangent,  offset)); (Input.Tangent,  (input \ "@source").text.substring(1)) }
				case "COLOR"    => { revInputs += ((Input.Color,    offset)); (Input.Color,    (input \ "@source").text.substring(1)) }
				case "JOINT"    => { revInputs += ((Input.Bone,     offset)); (Input.Bone,     (input \ "@source").text.substring(1)) }
				case _          => { revInputs += ((Input.User,     offset)); (Input.User,     (input \ "@source").text.substring(1)) }
			}
		}
		data = (node \ "p").text.split(" ").map(_.toInt)
	}
	
	def toMesh():Mesh = toMesh(false)

	/** Transform this into a SOFA mesh, usable to draw in an OpenGL scene. */
	def toMesh(xyz2yzx:Boolean):Mesh

	/** Generate a list of vertices from the data given in the Collada file such that each vertex
	  * owns its own set of attributes (as needed by OpenGL). If mergeVertices is true, the procedure
	  * will try to ensure that two vertices having exactly the same attributes (all of them) will
	  * not be duplicated. The order remains the same. */
	def toVertexList(mergeVertices:Boolean):(ArrayBuffer[Int],ArrayBuffer[Vertex]) = {
		val vertices = new ArrayBuffer[Vertex]()		// The element buffer.
		val elements = new ArrayBuffer[Int]()

		if(mergeVertices) {
			val unicity  = new HashMap[Vertex,Int]()		// To test unicity, and retrieve the original.	
			var i = 0

			while(i < data.length) {
				var vertex = new Vertex(getData(i, Input.Vertex), getData(i, Input.Normal), getData(i, Input.TexCoord),
					getData(i, Input.Tangent), getData(i, Input.Color), getData(i, Input.Bone))
	
				if(!unicity.contains(vertex)) {
					val index = vertices.length
					vertices += vertex
					unicity  += ((vertex,index))
					elements += index//unicity.get(vertex).get
				} else {
					elements += unicity.get(vertex).get
	//				vertex = vertices(unicity.get(vertex).get)
				}
			
				i += inputs.length
			}
			
Console.err.println("Collada Faces %d original elements %d unique elements".format(data.length/inputs.length, elements.size))
		} else {		
			var i = 0
			while(i < data.length) {
				var vertex = new Vertex(getData(i, Input.Vertex), getData(i, Input.Normal), getData(i, Input.TexCoord),
					getData(i, Input.Tangent), getData(i, Input.Color), getData(i, Input.Bone))
	
				val index = vertices.length
				vertices += vertex
				elements += index			
				i += inputs.length
			}
		}

		(elements, vertices)
	}

	protected def toMesh(elements:ArrayBuffer[Int], vertices:ArrayBuffer[Vertex], xyz2yzx:Boolean):Mesh = {
		val mesh = new EditableMesh()
	
		mesh.buildAttributes {
			vertices.foreach { vertex =>
				if(vertex.normal >= 0) {
					val norm = getAttribute(Input.Normal, vertex.normal)
					
					if(xyz2yzx)
					     mesh.normal(norm(1), norm(2), norm(0))
					else mesh.normal(norm(0), norm(1), norm(2))
				}
				if(vertex.texcoord >= 0) {
					val uv = getAttribute(Input.TexCoord, vertex.texcoord)
					mesh.texCoord(uv(0), uv(1))
				}
				if(vertex.index >= 0) {
					val vert = getVertex(vertex.index)

					if(xyz2yzx)
					     mesh.vertex(vert(1), vert(2), vert(0))
					else mesh.vertex(vert(0), vert(1), vert(2))
				}
			}
		}
		
		mesh.buildIndices(MeshDrawMode.TRIANGLES) {
			elements.foreach { mesh.index(_) }
		}
		
		mesh
	}	
}

/** Represents internally a vertex. */
class Vertex(val index:Int, val normal:Int, val texcoord:Int, val tangent:Int, val color:Int, val bone:Int) {
	override def equals(other:Any):Boolean = other match {
		case v:Vertex => (index==v.index && normal==v.normal && texcoord==v.texcoord && tangent==v.tangent && color==v.color && bone==v.bone)
		case _ => false
	}
	override def hashCode():Int = 41 * (41 * (41 * (41 * (41 * (41 + index) + normal ) + texcoord ) + tangent ) + color ) + bone

	override def toString():String = "vertex(pos=%d, norm=%d, texc=%d, tang=%d, clr=%d, bone=%d)".format(index, normal, texcoord, tangent, color, bone)
}


//------------------------------------------------------------------------------------------------------

/** Faces only made of triangles. */
class Triangles(node:Node, mesh:ColladaMesh) extends Faces(node, mesh) {
	parse(node)
	override def toString():String = "triangles(%d, [%s], data %d(%d))".format(count, inputs.mkString(","), data.length, (data.length/inputs.length)/3)
	def toMesh(xyz2yzx:Boolean):Mesh = { val (elements,vertices) = toVertexList(false); toMesh(elements, vertices, xyz2yzx) }
}

//------------------------------------------------------------------------------------------------------

/** Faces made of polygons with an arbitrary number of vertices. */
class Polygons(node:Node, mesh:ColladaMesh) extends Faces(node, mesh) {
	/** Number of vertex per face. */
	var vcount:Array[Int] = null
	
	parse(node)
	
	protected override def parse(node:Node) {
		super.parse(node)
		vcount = (node \ "vcount").text.split(" ").map(_.toInt)
	}
	
	override def toString():String = "polys(%d, [%s], vcount %d, data %d)".format(count, inputs.mkString(","), vcount.length, data.length)
	
	/** */
	def toMesh(xyz2yzx:Boolean):Mesh = {
		var (elements,vertices) = triangulate
		toMesh(elements,vertices,xyz2yzx)
	}
	
	def triangulate():(ArrayBuffer[Int],ArrayBuffer[Vertex]) = {
		var (elements,vertices) = toVertexList(false)
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

/** Describe a mesh (source (vertex attributes), and indices in the source under the form of faces. */
class ColladaMesh(node:Node) {
	
	/** All the vertex attributes. */
	val sources = new HashMap[String,MeshSource]()
	
	/** What source is the main vertex position attribute ? */
	var vertices:String = ""
	
	/** Indices into the source to build the real geometry. */
	var faces:Faces = null
		
	parse(node)
	
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

	/** Transform the Collada data to a SOFA Mesh object. */
	def toMesh():Mesh = toMesh(false)
	
	/** Transform the Collada data to a SOFA Mesh object.
	  * If the argument xyz2yzx is true, vertices and normals components
	  * are interverted so that:
	  * Blender x becomes OpenGL z,
	  * Blender y becomes OpenGL x,
	  * Blender z becomes OpenGL y. */
	def toMesh(xyz2yzx:Boolean):Mesh = faces.toMesh(xyz2yzx)
	
	override def toString():String = "mesh(%s(%s), %s)".format(sources(vertices).name, sources.values.mkString(","), faces)
}