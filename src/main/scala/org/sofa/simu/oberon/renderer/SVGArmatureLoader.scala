package org.sofa.opengl.test

import scala.xml._
import scala.math._
import scala.collection.mutable.{ArrayBuffer, HashMap}

import org.sofa.math.{Rgba, Point2}
import org.sofa.simu.oberon.renderer._

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

// -- Loader class --------------------------------------------------------------------------------------------

object SVGArmatureLoader {
	final val Sodipodi  = "http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd"
	
	final val Inkscape  = "http://www.inkscape.org/namespaces/inkscape" 

	final val JointDeclarationExp = """(\w+)\s*(\([^\)]+\))?""".r

	final val SubJointDeclarationExp = """(\w+)\s*([/\\])\s*(\d+)""".r

	final val SVGFillExp = """fill:#(\p{XDigit}\p{XDigit})(\p{XDigit}\p{XDigit})(\p{XDigit}\p{XDigit})""".r

	final val SVGTranslateExp = """translate\((-?\d+\.?\d*e?-?\d*),(-?\d+\.?\d*e?-?\d*)\)""".r
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
  *   - Each area must contain one circle, of color red (FF0000) wich will become
  *     its pivot point.
  *   - Each area can contain other circles, of color (FFxx00) with xx being the
  *     anchor identifier (between 0 and 255).
  *
  * The Z level and the hierachy or areas and how they are anchored one above the
  * other is expressed in a layer of the document that must be named "Armature".
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

	/** The set of [[Armature]] [[Joints]] built fromt the armature declaration. */
	protected val joints = new HashMap[String,Joint]

	/** Load an [[Armature]] from a SVG file and store it in the armatures field of the Armature object. */
	def cache(name:String, texRes:String, shaderRes:String, fileName:String) {
		Armature.armatures += (name -> load(name, texRes, shaderRes, fileName))
	}

	/** Load an [[Armature]] from a SVG file.
	  *
	  * @param name Name of the armature.
	  * @param texRes Name of a texture in the resource [[Libraries]].
	  * @param shaderRes Name of a shader in the resource [[Libraries]]. */
	def load(name:String, texRes:String, shaderRes:String, fileName:String):Armature = {
		val root      = XML.loadFile(fileName)
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
				} else if(label == "Armature") {
					armatureDecl = (group \ "text").text
				}
			}
		}

		val armature = buildArmature(name, texRes, shaderRes)

		areas.clear
		joints.clear

		armatureDecl = null
		pagew        = 0.0
		pageh        = 0.0

		armature
	}

	/** Parse an area (rectangle) of the SVG file and its pivot and anchor points. */
	def parseArea(name:String, area:Node):Area = {
		val globalTr = getTranslation(area)
		val rect     = (area \ "rect")

		if(rect.size < 1) {
			throw ArmatureParseException("Area without SVG 'rect' delimiter. Draw a rectangle to delimit the area.")
		}

		// I do not know why, but inskape origin in the GUI seems to be at bottom-left
		// and in the file at top-left, hence the inversion compared to the pageh.

		val tr = getTranslation(rect.head)
		val w  = (rect \ "@width").text.toDouble
		val h  = (rect \ "@height").text.toDouble
		val x  = (rect \ "@x").text.toDouble + (globalTr._1 + tr._1)
		val y  = (pageh - ((rect \ "@y").text.toDouble + (globalTr._2 + tr._2))) - h

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
			throw ArmatureParseException("Area without pivot point !")

		Area(name,x,y,w,h,pivot,anchors.toArray)
	}

	/** Parse an arc (a SVG path) that define a pivot or anchor point. */
	def parseArc(arc:Node, globalTr:(Double,Double)):Point2 = {
		var cx = doubleAttr(Sodipodi, arc, "cx")
		var cy = doubleAttr(Sodipodi, arc, "cy")
		var tr = getTranslation(arc)

		cx += (tr._1 + globalTr._1)
		cy  = (pageh - (cy + (tr._2 + globalTr._2)))

		getFill(arc) match {
			case (255,0) => new Pivot(cx,cy)
			case (255,x) => new Anchor(cx,cy,x)
			case _       => null
		}
	}

	/** Retrieve a double value from an attribute in a node of the SVG file. */
	def doubleAttr(url:String, node:Node, attribute:String):Double = {
		node.attribute(url, attribute) match { case Some(x) => x.text.toDouble; case _ => 0.0 }
	}

	/** Retrieve the translation of a SVG node. */
	def getTranslation(node:Node):(Double,Double) = {
		node.attribute("transform") match {
			case Some(x) => {
				x.text match {
					case SVGTranslateExp(tx, ty) => (tx.toDouble, ty.toDouble)
					case x => { throw ArmatureParseException("transform of node is not a translation '%s'".format(x)) }
				}
			}
			case None => { (0.0,0.0) }
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
	def buildArmature(name:String, texRes:String, shaderRes:String):Armature = {
		armatureDecl.split(';').foreach { areaDecl =>
			val parts = areaDecl.trim.split('=')

			if(parts.size != 2) throw ArmatureParseException("invalid part description '%s'".format(areaDecl.trim))

			val name  = parts(0).trim
			val decl  = parts(1).trim
			val joint = findJoint(name)

			parseJointDeclaration(joint, decl)
		}

		val root = joints.get("root").getOrElse(throw new ArmatureParseException("no 'root' joint found"))

		Armature(name, 1.0/max(pagew,pageh), Point2(pagew, pageh), texRes, shaderRes, root)
	}

	/** Parse a joint declaration and fill the joint with the corresponding area information.
	  * Setup the children joints if indicated. */
	def parseJointDeclaration(joint:Joint, declaration:String) {
		declaration match {
			case JointDeclarationExp(part, subs) => {
				var sub = if(subs ne null) subs.substring(1,subs.length-1).split(",") else Array[String]()
				
				val area = areas.get(part).getOrElse(throw ArmatureParseException("cannot configure joint '%s' area '%' does not exist".format(joint.name, part)))
				 
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
					val joint      = findJoint(name)
					joint.anchorGU = Point2(area.findAnchor(anchor.toInt))

					if(z == "/") under += joint else above += joint
					println("---> %s".format(z))
				}
				case _ => throw ArmatureParseException("cannot parse subjoint declaration '%s' in joint '%s'".format(subs, joint.name))
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