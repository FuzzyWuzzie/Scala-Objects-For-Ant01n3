package org.sofa.opengl.io.collada

import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeSeq
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import org.sofa.math.Rgba
import org.sofa.math.Vector3

class File(root:NodeSeq) {
	var asset:Assets = new Assets((root \\ "asset").head)
	val library = new Library(root)
}

object Units extends Enumeration {
	val Meters = Value(1)
	val Centimeters = Value(100)
}

class Unit {
	var units = Units.Meters
	var meter = 1.0
	def this(xml:Node) {
		this()
		meter = (xml\"@meter").text.toDouble
		units = (xml\"@name").text match {
			case "meter" => Units.Meters
			case "centimeter" => Units.Centimeters
			case _ => Units.Meters
		}
	}
	override def toString():String = "%s(%f meters)".format(units, meter)
}

object Axis extends Enumeration {
	val X = Value(0)
	val Y = Value(1)
	val Z = Value(2)
}

class Contributor(val name:String, val tool:String) {
	override def toString():String = "contrib(%s, %s)".format(name, tool)
}

class Assets {
	var contributors = new ArrayBuffer[Contributor]()
	var created:String = ""
	var modified:String = ""
	var units = new Unit()
	var upAxis = Axis.Y
	
	def this(xml:Node) {
		this()
		
		(xml \ "contributor").foreach { contributor =>
			contributors += new Contributor((contributor \ "author").text, (contributor \ "authoring_tool").text)
		}
		
		created  = (xml \ "created").text
		modified = (xml \ "modified").text
		units    = new Unit((xml\"unit").head)
		upAxis   = (xml \ "up_axis").text match {
			case "Z_UP" => Axis.Z
			case "X_UP" => Axis.X
			case _      => Axis.Y
		}
	} 
	
	override def toString():String = "asset((%s), created %s, modified %s, %s up, %s)".format(contributors.mkString(", "), created, modified, upAxis, units)
}

class Library(root:NodeSeq) {
	val cameras      = new HashMap[String,Camera]()
	val lights       = new HashMap[String,Light]()
	val geometries   = new HashMap[String,Geometry]()
	val visualScenes = new HashMap[String,VisualScene]()

	parse(root)
	
	def parse(root:NodeSeq) {
		// Cameras
		(root \\ "library_cameras" \ "camera").foreach { node =>
			cameras += ((node.label, Camera(node)))
		}  
		// Lights
		(root \\ "library_lights" \ "light").foreach { node =>
			lights += ((node.label, Light(node)))
		}
		// Geometries
		(root \\ "library_geometries" \ "geometry").foreach { node =>
			geometries += ((node.label, Geometry(node)))
		}
	}
	
	override def toString():String = {
		val builder = new StringBuilder()
		if(!cameras.isEmpty) {
			cameras.foreach { item => builder.append(item._2.toString); builder.append("%n".format()) }
		}
		if(!lights.isEmpty) {
			lights.foreach { item => builder.append(item._2.toString); builder.append("%n".format()) }
		}
		if(!geometries.isEmpty) {
			geometries.foreach { item => builder.append(item._2.toString); builder.append("%n".format()) }
		}
		builder.toString
	}
}

class ColladaFeature {}

class Camera extends ColladaFeature {}

class CameraPerspective(val fovAxis:Axis.Value, val fov:Double, val aspectRatio:Double, val znear:Double, val zfar:Double) extends Camera {
	override def toString():String = "camera(fov %f(%s), ratio %f, znear %f, zfar %f)".format(fov, fovAxis, aspectRatio, znear, zfar)
}

object Camera {
	def apply(xml:Node):Camera = {
		val cam   = (xml \\ "optics" \\ "technique_common").head
		val persp = cam \ "perspective"
		
		if(!persp.isEmpty) {
			val xfov = (persp \ "xfov")
			val yfov = (persp \ "yfov")
			var fov  = 1.0
			var axis = Axis.X
			
			if(!xfov.isEmpty) {
				fov = xfov.text.toDouble
			} else {
				fov = yfov.text.toDouble
				axis = Axis.Y
			}
			
			new CameraPerspective(
					axis, fov,
					(persp \ "aspect_ratio").text.toDouble,
					(persp \ "znear").text.toDouble,
					(persp \ "zfar").text.toDouble)
		} else {
			throw new RuntimeException("Collada: ortho camera TODO")
		}
	}
}

class Light(val color:Rgba) extends ColladaFeature {
}

class PointLight(color:Rgba, val constant_att:Double, linear_att:Double, quad_att:Double) extends Light(color) {
	override def toString():String = "pointLight(%s, cstAtt %f, linAtt %f, quadAtt %f)".format(color, constant_att, linear_att, quad_att)
}

class DirectionalLight(color:Rgba) extends Light(color) {
	override def toString():String = "dirLight(%s)".format(color)
}

object Light {
	def apply(xml:Node):Light = {
		val light = (xml \\ "technique_common").head
		val point = light \\ "point"
		val dir   = light \\ "directional"
		if(!point.isEmpty) {
			new PointLight(parseColor((point\"color").text),
					(point\"constant_attenuation").text.toDouble,
					(point\"linear_attenuation").text.toDouble,
					(point\"quadratic_attenuation").text.toDouble)
		} else if(!dir.isEmpty) {
			new DirectionalLight(parseColor((dir\"color").text))
		} else {
			null
		}
	}
	
	protected def parseColor(text:String):Rgba = {
		val colors = text.split(" ");
		var red    = 0.0
		var green  = 0.0
		var blue   = 0.0
		
		if(colors.length>0) { red   = colors(0).toDouble }
		if(colors.length>1) { green = colors(1).toDouble }
		if(colors.length>2) { blue  = colors(2).toDouble }
		
		new Rgba(red, green, blue, 1)
	}
}

object Geometry {
	def apply(node:Node):Geometry = new Geometry(node)
}

class Geometry(node:Node) extends ColladaFeature {
	var name = ""
	
	protected val meshes = new HashMap[String,ColladaMesh]()

	parse(node)
	
	protected def parse(node:Node) {
		name = (node\"@name").text
		(node \\ "mesh").foreach { mesh => meshes += ((mesh.label, new ColladaMesh(mesh))) }
	}
	
	override def toString():String = "geometry(%s, (%s))".format(name, meshes.values.mkString(", "))
}

class MeshSource(node:Node) {
	var id:String = ""
	var name:String = "noname"
	var stride = 0
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
class Faces(node:Node, sources:HashMap[String,MeshSource]) {
	/** Number of faces. */
	var count = 0
	
	/** Face vertices reference several attributes in order (vertex, normal, tex-coords, etc.). */
	var inputs:Array[InputType.Value] = null
	
	/** References to vertex attributes stored in the sources. */
	var data:Array[Int] = null

	/** Possible type of vertex attributes. */
	object InputType extends Enumeration {
		val Vertex = Value
		val Normal = Value
		val TexCoord = Value
		val User = Value
	}	

	protected def parse(node:Node) {
		count = (node \ "@count").text.toInt
		val in = (node \\ "input")
		inputs = new Array[InputType.Value](in.length)
		in.foreach { input =>
			val offset = (input\"@offset").text.toInt
			inputs(offset) = (input\"@semantic").text match {
				case "VERTEX"   => InputType.Vertex 
				case "NORMAL"   => InputType.Normal
				case "TEXCOORD" => InputType.TexCoord
				case _          => InputType.User  
			}
		}
		data = (node\"p").text.split(" ").map(_.toInt)
	}
}

class Triangles(node:Node, sources:HashMap[String,MeshSource]) extends Faces(node, sources) {
	
	protected override def parse(node:Node) {
		super.parse(node)
	}

	parse(node)
	
	override def toString():String = "triangles(%d, [%s], data %d(%d))".format(count, inputs.mkString(","), data.length, (data.length/inputs.length)/3)
}

class Polygons(node:Node, sources:HashMap[String,MeshSource]) extends Faces(node, sources) {
	/** Number of vertex per face. */
	var vcount:Array[Int] = null
	
	parse(node)
	
	protected override def parse(node:Node) {
		super.parse(node)
		vcount = (node\"vcount").text.split(" ").map(_.toInt)
	}
	
	override def toString():String = "polys(%d, [%s], vcount %d, data %d)".format(count, inputs.mkString(","), vcount.length, data.length)
}

class ColladaMesh(node:Node) {
	
	protected val sources = new HashMap[String,MeshSource]()
	
	protected var vertices:String = ""
	
	protected var faces:Faces = null
		
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

class VisualScene extends ColladaFeature {
	
}

object ColladaInput { def main(args:Array[String]) { (new ColladaInput).test } }

class ColladaInput {
	def test() {
		process(scala.xml.XML.loadFile("/Users/antoine/Desktop/Suzanne.dae").child)
		process(scala.xml.XML.loadFile("/Users/antoine/Desktop/duck_triangulate.dae").child)
	}
	
	def process(root:NodeSeq) {
		val file = new File(root)
		
		println(file.asset)
		println(file.library)
		
		println("----------------")
		println("File Contents:")
		root.foreach { e =>
			if(e.label != "#PCDATA")
				println("element %s".format(e.label))
		}
	}
}