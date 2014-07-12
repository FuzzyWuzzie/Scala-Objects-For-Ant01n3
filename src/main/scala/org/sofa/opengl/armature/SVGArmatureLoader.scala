package org.sofa.opengl.armature

import java.io.{File, InputStream, FileInputStream}

import scala.xml._
import scala.math._
import scala.collection.mutable.{ArrayBuffer, HashMap}

import org.sofa.Timer
import org.sofa.math.{Rgba, Point2, Point3, Matrix3}
//import org.sofa.simu.oberon.renderer._


// TODO : this thing is horribly slow. 
//
// Make it faster, and create an intermediary format for armatures:
//   -> Still read armatures from SVG since it is easy to edit by hand.
//   -> Still transform SVG to an armatures.
//   -> Allow the armature to write an intermediary format (say .arm).
//   -> Create a loader for intermediary formats.


// -- Exceptions ----------------------------------------------------------------------------------------------


/** Thrown during parsing of the SVG file when loading the armature. */
case class ArmatureParseException(message:String) extends Exception(message) 


// -- Utility classes -----------------------------------------------------------------------------------------


/** Represents an area in the SVG picture, as well as its pivot point, and eventually anchors for other areas. */
case class Area(val name:String, val x:Double, val y:Double, val w:Double, val h:Double, val pivot:Pivot, val anchors:Array[Anchor]) {
	
	override def toString() = "Area(%.2f, %.2f, %.2f, %.2f, %s, %d { %s })".format(x,y,w,h, pivot, anchors.size, anchors.mkString(", "))
	
	/** Find the first anchor with the given identifier. */
	def findAnchor(id:Int):Anchor = {
		anchors.find(_.id == id) match {
			case Some(x) => x
			case _ => throw ArmatureParseException("cannot find a joint with id '%d' in area '%s'".format(id, name))
		}
	}
}


/** Represents a pivot point. */
case class Pivot(xInit:Double, yInit:Double) extends Point2(xInit, yInit) {
	override def toString() = "Pivot(%.2f | %.2f)".format(x,y)
}


/** Represents an anchor point. */
case class Anchor(xInit:Double, yInit:Double, val id:Int) extends Point2(xInit, yInit) {
	override def toString() = "Anchor(%.2f | %.2f | [%d])".format(x,y,id)
}


// -- Utility transforms --------------------------------------------------------------------------------------


trait Transform {
	def transform(x:Double,y:Double):(Double,Double)
	def transform(p:Point2):Point2
	def transform(p:Point3):Point3
}


case class TranslateTransform(tx:Double,ty:Double) extends Transform {
	def transform(x:Double,y:Double) = (x+tx, y+ty)
	def transform(p:Point2) = new Point2(p.x+tx, p.y+ty)
	def transform(p:Point3) = new Point3(p.x+tx, p.y+ty, p.z)
}


case class MatrixTransform(a:Double,b:Double,c:Double,d:Double,e:Double,f:Double) extends Transform {
	val matrix = Matrix3((a,c,e), (b,d,f), (0,0,1))
	
	def transform(x:Double,y:Double) = {
		val rr = matrix * Point3(x, y, 1)
		(rr.x, rr.y)
	}
	def transform(p:Point2) = {
		val rr = matrix * Point3(p.x, p.y, 1)
		Point2(rr.x, rr.y)
	}
	def transform(p:Point3) = { matrix * p }
}


// -- Loader class --------------------------------------------------------------------------------------------


object SVGArmatureLoader {
	final val Sodipodi  = "http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd"
	
	final val Inkscape  = "http://www.inkscape.org/namespaces/inkscape" 

	final val JointDeclarationExp = """([\w\d]+)\s*(\([^\)]+\))?""".r

	final val SubJointDeclarationExp = """([^\\/]+)\s*([/\\])\s*(\d+)""".r

	final val SVGFillExp = """fill:#(\p{XDigit}\p{XDigit})(\p{XDigit}\p{XDigit})(\p{XDigit}\p{XDigit})""".r

	final val SVGTranslateExp = """translate\((-?\d+\.?\d*e?-?\d*),(-?\d+\.?\d*e?-?\d*)\)""".r

	final val SVGMatrixExp = """matrix\((-?\d+\.?\d*e?-?\d*),(-?\d+\.?\d*e?-?\d*),(-?\d+\.?\d*e?-?\d*),(-?\d+\.?\d*e?-?\d*),(-?\d+\.?\d*e?-?\d*),(-?\d+\.?\d*e?-?\d*)\)""".r
}


/** Load an Armature from a prepared SVG file. 
  *
  * This class allows to transform a SVG file into an armature, with its hierachy,
  * joints, anchors, pivot points, Z-levels, etc.
  *
  * The SVG file must be prepared with Inkscape with the following particularities:
  *   - The document units must be PX.
  *   - The size of the page is the global size of the drawing.
  *   - The origin is considered at the lower-left corner with +X going right and +Y
  *     going up.
  *
  * Inside the document:
  *   - Each area must be in a layer with a name between square brackets.
  *   - Each area must contain only one rectangle that will delimit its area.
  *   - Each area must contain one circle, of fill color red (FF0000) wich will become
  *     its pivot point.
  *   - Each area can contain other circles, of fill color (FFxx00) with xx being the
  *     anchor identifier (between 1 and 255).
  *
  * The Z level and the hierachy or areas and how they are anchored one above the
  * other is expressed in a layer of the document whose name is passed as the paramater
  * `armatureId` (default is "Armature"). Other layers are ignored.
  *
  * In this layer one or more text elements define the arangement of Joints in an
  * Armature. We create each joint by giving a free name followed by an equal sign
  * that tell wich part of the drawing, wich area, is to be used. Then can follow
  * parenthesis, containing a description of the children, where they are anchored
  * (the name of the anchors in the parent part) and if they are above or under
  * (to compute the z levels). For example the text:
  *
  *		root=part1(b/1,c\2); b=part2(d/1); c=part3; d=part4;
  *
  * Means:
  *  - 'root' is a joint represented by part1, it contains two child joint, 'b' anchored above its anchor 1 and 'c' anchored under 2.
  *  - 'b' represented by part2 contains child 'd' above 1.
  *  - 'c' is reprsented by part3
  *  - 'd' is represented by part4
  *
  * There must always be a joint named 'root' that will be at the root of the armature hierarchy.
  *
  * Only the armature is extracted, the SVG does not need to contain the image of the texture that
  * will be used by the joints. The texture can be done with another software than Inkscape. We use
  * The Inkscape format since it uses layers, not plain SVG.
  */
class SVGArmatureLoader {
	import SVGArmatureLoader._

	/** The page width in pixels. */
	protected var pagew = 0.0

	/** The page height in pixels. */
	protected var pageh = 0.0

	/** Description of all the areas found in the SVG. */
	protected val areas = new HashMap[String,Area]()

	/** The armature hierarchy declaration. */
	protected var armatureDecl:String = null

	/** The set of [[Armature]] [[Joint]]s built fromt the armature declaration. */
	protected val joints = new HashMap[String,Joint]

	/** Load an [[Armature]] from a SVG file.
	  *
	  * @param name Name of the armature.
	  * @param texRes Name of a texture in the resource [[Libraries]].
	  * @param shaderRes Name of a shader in the resource [[Libraries]].
	  * @param fileName Name fo the SVG file to use. */
	def load(name:String, texRes:String, shaderRes:String, fileName:String, armatureId:String, scale:Double):Armature = {
		load(name, texRes, shaderRes, new FileInputStream(fileName), armatureId, scale)
	}

	/** Load an [[Armature]] from a SVG file.
	  *
	  * @param name Name of the armature.
	  * @param texRes Name of a texture in the resource [[Libraries]].
	  * @param shaderRes Name of a shader in the resource [[Libraries]].
	  * @param file A file descriptor on a SVG file to use. */
	def load(name:String, texRes:String, shaderRes:String, file:File, armatureId:String, scale:Double):Armature = {
		load(name, texRes, shaderRes, new FileInputStream(file), armatureId, scale)
	}

	/** Load an [[Armature]] from a SVG file.
	  *
	  * @param name Name of the armature.
	  * @param texRes Name of a texture in the resource [[Libraries]].
	  * @param shaderRes Name of a shader in the resource [[Libraries]].
	  * @param stream A stream pointing at the SVG data to load and use. */
	def load(name:String, texRes:String, shaderRes:String, stream:InputStream, armatureId:String, scale:Double):Armature = {
var armature:Armature = null
Timer.timer.measure("SVGArmatureLoader.load()") {
		val root      = XML.load(stream)
		pagew         = (root \ "@width").text.toDouble
		pageh         = (root \ "@height").text.toDouble
		val namedview = (root \ "namedview")

		if(namedview.size < 1) 
			throw ArmatureParseException("no 'namedview' element, this program works only with Inkscape.")

		val units = namedview.head.attribute(Inkscape,"document-units").getOrElse(throw ArmatureParseException("no 'document-units' attribute in element namedview ?")).text

		if(units.toLowerCase != "px") {
			throw ArmatureParseException("bad units '%s', please specify px in document preferences".format(units))
		}

		root \ "g" foreach { group =>
			if(group.attribute(Inkscape, "groupmode").get.text == "layer") {
				val label = group.attribute(Inkscape, "label").get.text.trim
				
				if(label.startsWith("[") && label.endsWith("]")) {
					val name = label.substring(1,label.length-1)

					areas += (name -> parseArea(name, group))
				} else if(label == armatureId) {
					armatureDecl = (group \ "text").text
				}
			}
		}

		if(armatureDecl eq null)
			throw new ArmatureParseException(s"no armature layer named `${armatureId}` found")

//println("found %d areas =====".format(areas.size))
//areas.foreach { area => println("area %s".format(area)) }

		armature = buildArmature(name, texRes, shaderRes, scale)

		areas.clear
		joints.clear

		armatureDecl = null
		pagew        = 0.0
		pageh        = 0.0
}
		armature
	}

	/** Parse an area (rectangle) of the SVG file and its pivot and anchor points. */
	def parseArea(name:String, area:Node):Area = {
		val globalTr = getTransform(area)
		val rect     = (area \ "rect")

		if(rect.size < 1) {
			throw ArmatureParseException("Area '%s' without SVG 'rect' delimiter. Draw a rectangle to delimit the area.".format(name))
		}

		// I do not know why, but inskape origin in the GUI seems to be at bottom-left
		// and in the file at top-left, hence the inversion compared to the pageh.

		val tr = getTransform(rect.head)
		val w  = (rect \ "@width").text.toDouble
		val h  = (rect \ "@height").text.toDouble
		var p  = Point2((rect \ "@x").text.toDouble, (rect \ "@y").text.toDouble)
		
		p   = globalTr.transform(tr.transform(p))
		p.y = (pageh - p.y) - h

		var pivot:Pivot = null
		var anchors = new ArrayBuffer[Anchor]()

		(area \ "path") foreach { path =>
			path.attribute(Sodipodi, "type") match {
				case Some(x) => x.text match {
					case "arc" => parseArc(path, globalTr) match {
						case p:Pivot  => pivot = p
						case a:Anchor => anchors += a
						case _ => println("unknown pivot or anchor type... ignored")
					}
					case _ =>
				}
				case _ => {}
			}
		}

		if(pivot eq null)
			throw ArmatureParseException("Area '%s' without pivot point !".format(name))

		Area(name,p.x,p.y,w,h,pivot,anchors.toArray)
	}

	/** Parse an arc (a SVG path) that define a pivot or anchor point. */
	def parseArc(arc:Node, globalTr:Transform):Point2 = {
		var c  = Point2(doubleAttr(Sodipodi, arc, "cx"), doubleAttr(Sodipodi, arc, "cy"))
		var tr = getTransform(arc)

		c = globalTr.transform(tr.transform(c))
		c.y = (pageh - c.y)

		getFill(arc) match {
			case (255,0) => new Pivot(c.x,c.y)
			case (255,x) => new Anchor(c.x,c.y,x)
			case _       => null
		}
	}

	/** Retrieve a double value from an attribute in a node of the SVG file. */
	def doubleAttr(url:String, node:Node, attribute:String):Double = {
		node.attribute(url, attribute) match { case Some(x) => x.text.toDouble; case _ => 0.0 }
	}

	/** Retrieve the translation of a SVG node. */
	def getTransform(node:Node):Transform = {
		node.attribute("transform") match {
			case Some(x) => {
				x.text match {
					case SVGTranslateExp(tx, ty) => TranslateTransform(tx.toDouble, ty.toDouble)
					case SVGMatrixExp(a, b, c, d, e, f) => MatrixTransform(a.toDouble, b.toDouble, c.toDouble, d.toDouble, e.toDouble, f.toDouble)
					case x => { throw ArmatureParseException("transform of node is not a translation or a matrix '%s'".format(x)) }
				}
			}
			case None => { TranslateTransform(0.0,0.0) }
		}
	}

	/** Retrieve the fill value of the 'style' attribute of a SVG node and return the values for red and green,
	  * used to give a type and identifier of a pivot or anchor point. */
	def getFill(node:Node):(Int,Int) = {
		val style   = (node \ "@style").text
		val value   = SVGFillExp.findFirstIn(style).getOrElse(throw ArmatureParseException("no fill on marker, need color to know marker type"))

		value match {
			case SVGFillExp(r,g,b) => (Integer.parseInt(r,16),Integer.parseInt(g,16))
			case _ => (0,0)
		}
	}

	/** Build an Armature from the parsed [[areas]] and the [[armatureDecl]]. */
	def buildArmature(name:String, texRes:String, shaderRes:String, scale:Double):Armature = {
		armatureDecl.split(';').foreach { areaDecl =>
			val parts = areaDecl.trim.split('=')

			if(parts.size != 2) throw ArmatureParseException("invalid part description '%s'".format(areaDecl.trim))

			val name  = parts(0).trim
			val decl  = parts(1).trim
			val joint = findJoint(name)

			parseJointDeclaration(joint, decl)
		}

		joints.foreach { joint =>
			if(joint._2.fromUV eq null) // not initialized ?
				throw new ArmatureParseException(s"a joint `${joint._2.name} is used in the armature but never defined")
		}

		val root = joints.get("root").getOrElse(throw new ArmatureParseException("no 'root' joint found"))

		//Armature(name, 1.0/max(pagew,pageh), Point2(pagew, pageh), texRes, shaderRes, root)
		Armature(name, scale, Point2(pagew, pageh), texRes, shaderRes, root)
	}

	/** Parse a joint declaration and fill the joint with the corresponding area information.
	  * Setup the children joints if indicated. */
	def parseJointDeclaration(joint:Joint, declaration:String) {
		declaration match {
			case JointDeclarationExp(part, subs) => {
				var sub = if(subs ne null) subs.substring(1,subs.length-1).split(",") else Array[String]()
				
				val area = areas.get(part).getOrElse(throw ArmatureParseException("cannot configure joint '%s' area '%s' does not exist".format(joint.name, part)))

				joint.fromUV  = Point2(area.x,area.y)
				joint.sizeUV  = Point2(area.w,area.h)
				joint.pivotGU = Point2(area.pivot)

				if(sub.size > 0) {
					val (under,above) = parseJointSubs(joint, sub, area)
					joint.subUnder = under
					joint.subAbove = above
				}
			}
			case _ => throw ArmatureParseException("cannot parse declaration '%s' for joint '%s'".format(declaration, joint.name))
		}
	}

	/** Parse each anchored joint and return two arrays of sub joints, the first are to drawn under and the other above. */
	protected def parseJointSubs(joint:Joint, subs:Array[String], area:Area):(Array[Joint],Array[Joint]) = {
		val under = new ArrayBuffer[Joint]
		val above = new ArrayBuffer[Joint]

		subs.foreach { sub =>
			sub match {
				case SubJointDeclarationExp(name, z, anchor) => {
					val joint      = findJoint(name.trim)
					joint.anchorGU = Point2(area.findAnchor(anchor.trim.toInt))

					if(z == "/") under += joint else above += joint
				}
				case _ => throw ArmatureParseException("cannot parse subjoint declaration '%s' in joint '%s'".format(sub, joint.name))
			}
		}

		(under.toArray, above.toArray)
	}

	/** Find a joint with the given name or create it. */
	protected def findJoint(name:String):Joint = {
		joints.get(name).getOrElse {
			val j = Joint(name)
			
			j.anchorGU = Point2(0,0)
			joints += (name -> j)
			
			j
		}
	}
}