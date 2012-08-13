package org.sofa.opengl.io.collada

import scala.collection.mutable.HashMap
import scala.xml.Node
import org.sofa.opengl.mesh.Mesh
import org.sofa.opengl.mesh.EditableMesh
import org.sofa.opengl.mesh.MeshDrawMode
import scala.collection.mutable.ArrayBuffer
import scala.xml.NodeSeq


object Geometry {
	def apply(node:Node):Geometry = new Geometry(node)
}

/** A geometry descriptor in a Collada geometry library. */
class Geometry(node:Node) extends ColladaFeature {
	var name = ""
	
	val meshes = new HashMap[String,ColladaMesh]()

	parse(node)
	
	protected def parse(node:Node) {
		name = (node\"@name").text
		(node \\ "mesh").foreach { mesh => meshes += ((mesh.label, new ColladaMesh(mesh))) }
	}
	
	override def toString():String = "geometry(%s, (%s))".format(name, meshes.values.mkString(", "))
}

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
	
	protected def parse(node:Node) {
		id     = (node \ "@id").text
		name   = (node \ "@name").text; if(name.length == 0) name = "noname"
		stride = (node \ "technique_common" \ "accessor" \ "@stride").text.toInt
		data   = (node \ "float_array").text.split(" ").map(_.toFloat)
	}
	
	override def toString():String = "source(%s, stride %d, %d floats)".format(name, stride, data.length) 
}

/** Faces making up a mesh. */
abstract class Faces(node:Node, sources:HashMap[String,MeshSource]) {
	
	/** Number of faces. */
	var count = 0
	
	/** Face vertices reference several attributes in order (vertex, normal, tex-coords, etc.). */
	var inputs:Array[(Input.Value,String)] = null
	
	/** Reverse input types, give the type of input and get the offset. */
	var revInputs = new HashMap[Input.Value,Int]()
	
	/** References to vertex attributes stored in the sources. */
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
	
	def hasInput(input:Input.Value):Boolean = revInputs.contains(input)

	def offset(input:Input.Value):Int = revInputs.get(input).get
	
	def getData(i:Int, input:Input.Value):Int = if(hasInput(input)) data(i + offset(input)) else -1
	
	def getSource(input:Input.Value):String = inputs(revInputs.get(input).get)._2
	
	def getSourceData(input:Input.Value, index:Int):Array[Float] = getSourceData(getSource(input), index)
	
	def getSourceData(source:String, index:Int):Array[Float] = {
		val src = sources.get(source).get
		val n   = src.stride
		val res = Array[Float](n)
		var i   = 0
		var p   = index * n

		while(i < n) {
			res(i) = src.data(p+i)
		}
		
		res
	}
	
	protected def parse(node:Node) {
		count = (node \ "@count").text.toInt
		val in = (node \\ "input")
		inputs = new Array[(Input.Value,String)](in.length)
		in.foreach { input =>
			val offset = (input\"@offset").text.toInt
			inputs(offset) = (input\"@semantic").text match {
				case "VERTEX"   => { revInputs += ((Input.Vertex,   offset)); (Input.Vertex,   (input\"@source").text) }
				case "NORMAL"   => { revInputs += ((Input.Normal,   offset)); (Input.Normal,   (input\"@source").text) }
				case "TEXCOORD" => { revInputs += ((Input.TexCoord, offset)); (Input.TexCoord, (input\"@source").text) }
				case "TANGENT"  => { revInputs += ((Input.Tangent,  offset)); (Input.Tangent,  (input\"@source").text) }
				case "COLOR"    => { revInputs += ((Input.Color,    offset)); (Input.Color,    (input\"@source").text) }
				case "JOINT"    => { revInputs += ((Input.Bone,     offset)); (Input.Bone,     (input\"@source").text) }
				case _          => { revInputs += ((Input.User,     offset)); (Input.User,     (input\"@source").text) }
			}
		}
		data = (node\"p").text.split(" ").map(_.toInt)
	}
	
	def toMesh():Mesh

	/** Generate a list of vertices from the data given in the Collada file such that each vertex
	  * owns its own set of attributes (as needed by OpenGL). The procedure try to ensure that
	  * two vertices having exactly the same attributes will not be duplicated. Naturally the order
	  * remains the same. */
	def toVertexList():ArrayBuffer[Vertex] = {
		val unicity  = new HashMap[Vertex,Int]()		// To test unicity, and retrieve the original.
		val vertices = new ArrayBuffer[Vertex]()		// The element buffer.
		val vertexList = new ArrayBuffer[Vertex]()
		
		var i = 0
		while(i < data.length) {
			var vertex = new Vertex(getData(i, Input.Vertex), getData(i, Input.Normal), getData(i, Input.TexCoord),
					getData(i, Input.Tangent), getData(i, Input.Color), getData(i, Input.Bone))
	
			if(!unicity.contains(vertex)) {
				val index = vertices.length
				vertices += vertex
				unicity += ((vertex,index))
			} else {
				vertex = vertices(unicity.get(vertex).get)
			}
			
			vertexList += vertex
			
			i += inputs.length
		}

Console.err.println("Generated a list of vertices (%d vertices, for %d inputs, data size %d), with %d unique vertices".format(vertexList.size, inputs.length, data.size, vertices.size))
		
		vertexList
	}
	
	class Vertex(val index:Int, val normal:Int, val texcoord:Int, val tangent:Int, val color:Int, val bone:Int) {
		override def equals(other:Any):Boolean = other match {
			case v:Vertex => (index==v.index && normal==v.normal && texcoord==v.texcoord && tangent==v.tangent && color==v.color && bone==v.bone)
			case _ => false
		}
		override def hashCode():Int = 41 * (41 * (41 * (41 * (41 * (41 + index) + normal ) + texcoord ) + tangent ) + color ) + bone
	}
}

/** Faces only made of triangles. */
class Triangles(node:Node, sources:HashMap[String,MeshSource]) extends Faces(node, sources) {
	parse(node)
	override def toString():String = "triangles(%d, [%s], data %d(%d))".format(count, inputs.mkString(","), data.length, (data.length/inputs.length)/3)
	
	def toMesh():Mesh = {
		val vertices = toVertexList
		val mesh = new EditableMesh()
	
		mesh.begin(MeshDrawMode.TRIANGLES)
			vertices.foreach { vertex =>
				if(vertex.normal >= 0) {
					val norm = getSourceData(Input.Normal, vertex.normal)
					mesh.normal(norm(0), norm(1), norm(2))
				}
				if(vertex.index >= 0) {
					val vert = getSourceData(Input.Vertex, vertex.index)
					mesh.vertex(vert(0), vert(1), vert(2))
				}
			}
		mesh.end
		
		mesh
	}
}

/** Faces made of polygons with an arbitrary number of vertices. */
class Polygons(node:Node, sources:HashMap[String,MeshSource]) extends Faces(node, sources) {
	/** Number of vertex per face. */
	var vcount:Array[Int] = null
	
	parse(node)
	
	protected override def parse(node:Node) {
		super.parse(node)
		vcount = (node\"vcount").text.split(" ").map(_.toInt)
	}
	
	override def toString():String = "polys(%d, [%s], vcount %d, data %d)".format(count, inputs.mkString(","), vcount.length, data.length)
	
	/** */
	def toMesh():Mesh = {
		val vertices = toVertexList
		val mesh = new EditableMesh()
	
//		mesh.begin(MeshDrawMode.TRIANGLES)
//			vertices.
//		mesh.end
		
		mesh
	}
}

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
		vertices = (vert\"input"\"@source").text.substring(1)
	}
	
	protected def processFaces(node:Node) {
		val triangles = (node\\"triangles")
		val polylist = (node\\"polylist")
		
		if(!triangles.isEmpty) {
			faces = new Triangles(triangles.head, sources)
		} else if(!polylist.isEmpty) {
			faces = new Polygons(polylist.head, sources)
		}
	}
	
	override def toString():String = "mesh(%s(%s), %s)".format(sources(vertices).name, sources.values.mkString(","), faces)
}