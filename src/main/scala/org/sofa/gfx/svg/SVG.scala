package org.sofa.gfx.svg

import java.io.{PrintStream, FileOutputStream}
import org.sofa.math.{Point2, Vector2}
import scala.xml.{XML, Node, NodeSeq}


object SVG {
	final val InkscapeNS = "http://www.inkscape.org/namespaces/inkscape"

	/** Regex of the SVG pathes data understood by the parser. */
	final val CompatiblePath = """\s*([mM]\s*[-0-9\.,cClL ]+\s*z?)\s*""".r
	
	/** Set of commands accepted in SVG pathes data. */
	final val Commands = """([lLcCsS])""".r

	def load(fileName:String):NodeSeq = XML.loadFile(fileName)

	/** Extract an Inkscape SVG layer with id `layerId` from the `svg` data.
	  * Returns either a node sequence with one node or empty if not found. */
	def layerById(svg:NodeSeq, layerId:String):NodeSeq = {
		(svg \\ "g") find { g => (g \ "@id").text == layerId } match {
			case Some(x) => x
			case None => NodeSeq.Empty
		}
	}

	/** Extract an Inkscape SVG layer with label `layerName` from the `svg` data.
	  * Returns either a node sequence with one node or empty if not found. */
	def layer(svg:NodeSeq, layerName:String):NodeSeq = {
		val attr = "@{%s}label".format(InkscapeNS)
		(svg \\ "g") find { g => (g \ attr).text == layerName } match {
			case Some(x) => x
			case None => NodeSeq.Empty
		}
	}

	/** Extract a path with `id` from the `svg` data.
	  * Returns either a node sequence with one node or empty if not found. */
	def path(svg:NodeSeq, id:String):NodeSeq = {
		(svg \\ "path") find { p => (p \ "@id").text == id } match {
			case Some(x) => x
			case None => NodeSeq.Empty
		}
	}

	/** Extract all pathes from the `svg` data. */
	def pathes(svg:NodeSeq):NodeSeq = (svg \\ "path")

	/** Extract the data attributes of a set of `pathes`. */
	def pathesData(pathes:NodeSeq):List[String] = (pathes map { pathData(_) }).toList

	/** Extract the data attribute of a `path`. */
	def pathData(path:NodeSeq):String = (path \ "@d").text

	/** Convert SVG `path` data to a list of lines and curves. */
	def convertPath(path:String, lpt:Point2 = Point2(0,0)):List[PathItem] = {
		path match {
			case CompatiblePath(points) => {  
				val str = points.replace(",", " ").trim.split(" ").toList
				makePath(str, lpt)
			}
			case _ => {
				throw new RuntimeException("path is not parsable '%s'%n".format(path)) 
			}
		}
	}

	/** Convert a set of SVG `pathes` data into a list of pathes under the
	  * form of a list of lines and curves. This recursive method can take
	  * a last encountered point `lpt`, n. */
	def convertPathes(pathes:List[String], lpt:Point2 = Point2(0,0)):List[List[PathItem]] = {
		pathes match {
			case Nil => { List[List[PathItem]]() }
			case x :: tail => {
				val p = convertPath(x, lpt)
				p :: convertPathes(tail, p.last.to)	// <- innefficient, access to last element of list
			}
		}
	}

	/** Recursive parsing of a SVG path data from the description `str`. 
	  *
	  * The `str` parameter is a list of strings, each element containing
	  * a token of the SVG path (numbers or commands 'M', 'l', 'z'...). 
	  *
	  * At start, we only need to know a previous point `lpt`. If no path were
	  * present in the SVG group before, this point must be `Point2(0,0)`.
	  * Commands in the path data may start relative to a previous path.
	  *
	  * During recursivity, we need the previous point `lpt`, as well as the last 
	  * command encountered `lastCmd`. In addition to implement the 's' and 'S'
	  * curves, we need the point before the last point encountered, `llpt`. 
	  * To implement the 'z' and 'Z' commands we also need the first
	  * encountered point `fpt`. The `fpt` and `llpt` points can be null
	  * at start of the recursivity, they will be set in the recursive process.
	  * The `lastCmd` can be an empty string at start.*/
	def makePath(str:List[String], lpt:Point2, llpt:Point2=null, fpt:Point2=null, lastCmd:String=""):List[PathItem] = {
		str match {
			case Nil => { List[PathItem]() }
			case "M" :: x :: y :: tail => {
				val a = Point2(x.toDouble, y.toDouble)
				if(fpt == null)
				     makePath(tail, a, null, a, "M")
				else makePath(tail, a, null, fpt, "M")
			}
			case "m" :: x :: y :: tail => {
//				val a = Point2(x.toDouble + lpt.x, y.toDouble + lpt.y)
				val a = Point2(x.toDouble, y.toDouble)
				if(fpt == null)
				     makePath(tail, a, null, a, "m")
				else makePath(tail, a, null, fpt, "m")
			}
			case "C" :: xb :: yb :: xc :: yc :: xd :: yd :: tail => {
				val a = lpt												// pt1
				val b = Point2(xb.toDouble, yb.toDouble)				// ctrl pt1
				val c = Point2(xc.toDouble, yc.toDouble)				// ctrl pt2
				val d = Point2(xd.toDouble, yd.toDouble)				// pt2
				PathCurve(a, b, c, d) :: makePath(tail, d, c, fpt, "C")
			} 
			case "c" :: xb :: yb :: xc :: yc :: xd :: yd :: tail => {
				val a = lpt												// pt1
				val b = Point2(xb.toDouble + a.x, yb.toDouble + a.y)	// ctrl pt1
				val c = Point2(xc.toDouble + a.x, yc.toDouble + a.y)	// ctrl pt2
				val d = Point2(xd.toDouble + a.x, yd.toDouble + a.y)	// pt2
				PathCurve(a, b, c, d) :: makePath(tail, d, c, fpt, "c")
			}
			case "S" :: xc :: yc :: xd :: yd :: tail => {
				val a = lpt 											// pt1
				val b = lpt + Vector2(llpt, lpt)						// ctrl pt1 (mirror of llpt by lpt)
				val c = Point2(xc.toDouble, yc.toDouble)				// ctrl pt2
				val d = Point2(xd.toDouble, yd.toDouble)				// pt2
				PathCurve(a, b, c, d) :: makePath(tail, d, c, fpt, "S")
			}
			case "s" :: xc :: yc :: xd :: yd :: tail => {
				val a = lpt 											// pt1
				val b = lpt + Vector2(llpt, lpt)						// ctrl pt1 (mirror of llpt by lpt)
				val c = Point2(xc.toDouble + b.x, yc.toDouble + b.y)	// ctrl pt2
				val d = Point2(xd.toDouble + c.x, yd.toDouble + c.y)	// pt2
				PathCurve(a, b, c, d) :: makePath(tail, d, c, fpt, "s")
			}
			case "L" :: x :: y :: tail => {
				val a = lpt
				val b = Point2(x.toDouble, y.toDouble)
				PathLine(a, b) :: makePath(tail, b, a, fpt, "L")
			}
			case "l" :: x :: y :: tail => {
				val a = lpt
				val b = Point2(x.toDouble + a.x, y.toDouble + a.y)
				PathLine(a, b) :: makePath(tail, b, a, fpt, "l")
			}
			case "z" :: tail => {
				val a = lpt
				val b = fpt
				PathLine(a, b) :: makePath(tail, b, a, fpt, "z")
			}
			case x :: tail => {
				lastCmd match {
					case "m" => makePath("l" :: x :: tail, lpt, llpt, fpt, "m")
					case "M" => makePath("L" :: x :: tail, lpt, llpt, fpt, "M")
					case Commands(c) => makePath(c :: x :: tail, lpt, llpt, fpt, c)
					case _ => throw new RuntimeException("cannot parse path, unknown command %s.".format(lastCmd.toString))	
				}
			}
		}
	}
	
	/** Generic element of SVG path data. */
	trait PathItem {
		/** The first point of the path element. */
		def from:Point2
		/** The last point of the path element. */
		def to:Point2
	}

	/** Line in SVG path data. */
	case class PathLine(from:Point2, to:Point2) extends PathItem {
		override def toString:String = "L %s %s".format(from, to)
	}

	/** Cubic Bézier curve in SVG path data. */
	case class PathCurve(from:Point2, ctrlfrom:Point2, ctrlto:Point2, to:Point2) extends PathItem {
		override def toString:String = "C %s %s %s %s".format(from, ctrlfrom, ctrlto, to)
	}
}