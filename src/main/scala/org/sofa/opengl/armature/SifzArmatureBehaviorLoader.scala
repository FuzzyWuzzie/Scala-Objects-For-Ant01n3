package org.sofa.opengl.armature

import java.io.{File, InputStream, FileInputStream}
import java.util.zip.{ZipFile, GZIPInputStream}

import scala.xml._
import scala.math._
import collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, HashMap}

import org.sofa.math.{Rgba, Point2, Point3, Matrix3, Vector2}
import org.sofa.opengl.armature.behavior._


// -- Timed keys --------------------------------------------------------------------------------

/** Set of translate and rotate values at distinct positions in time for a given joint. */
class TimedKeys(val name:String) {

	val translate = new ArrayBuffer[TimedVector]

	val rotate = new ArrayBuffer[TimedValue]
}

abstract class TimedThing(val timeMs:Long) {}

object TimedVector { def apply(t:Long, v:Vector2):TimedVector = new TimedVector(t, v) }
class TimedVector(timeMs:Long, val vector:Vector2) extends TimedThing(timeMs) {
	override def toString() = "tvec(%dms, %.3f, %.3f)".format(timeMs, vector.x, vector.y)
}

object TimedValue { def apply(t:Long, v:Double):TimedValue = new TimedValue(t, v) }
class TimedValue(timeMs:Long, val value:Double) extends TimedThing(timeMs) {
	override def toString() = "tval(%dms, %f)".format(timeMs, value)
}


// -- Sifz loader for Synfig ------------------------------------------------------------------------


case class SifzParseException(message:String) extends Exception(message)


object SifzArmatureBehaviorLoader {
	/** Matches a frame time. */
	final val FrameTimeExp  = """(\d+)f""".r

	/** Matches a time in seconds. */
	final val SecondTimeExp = """(\d+\.?\d*)s""".r
}

/** Parse a Sinfig Sifz file and extract PasteCanvas elements and their translate and
  * rotate sub-layers. Try to export a set of rotate and translate timed keys for each
  * PasteCanvas considering these PasteCanvas will match the various joints or an
  * armature. */
class SifzArmatureBehaviorLoader {
	import SifzArmatureBehaviorLoader._

	/** name of the behavior. */
	var name:String = ""

	/** Frames per second. */
	var fps = 0

	/** Start time in frames. */
	var start = 0L

	/** Ent time in frames. */
	var end = 0L

	/** Try to uncompress a Sifz file and parse it. Returns a set of
	  * time keys. */
	def load(fileName:String):HashMap[String,TimedKeys] = {
		parse(XML.load(new GZIPInputStream(new java.io.FileInputStream(fileName))))

	}

	/** Parse the contents of a Sif file. */
	protected def parse(root:Node):HashMap[String,TimedKeys] = {
		name  = (root \ "name").text
		fps   = (root \ "@fps").text.toDouble.toInt
		start = convertInFrames((root \ "@begin-time").text)
		end   = convertInFrames((root \ "@end-time").text)

		parsePasteCanvas(pasteCanvasOf(root \ "layer"), 0)
	}

	/** Try to convert a Sif time value in frames. */
	protected def convertInFrames(value:String):Long = {
		value match {
			case FrameTimeExp(t)  => { t.toInt }
			case SecondTimeExp(t) => { (t.toDouble * fps).toInt }
			case _                => { throw new SifzParseException("cannot understand time value '%s'".format(value)) }
		}
	}

	/** Try to convert a Sif time value in milliseconds. */
	protected def convertInMillis(value:String):Long = {
		value match {
			case FrameTimeExp(t)  => { (t.toLong * (1000 / fps)) }
			case SecondTimeExp(t) => { (t.toDouble * 1000).toLong }
			case _                => { throw new SifzParseException("cannot understand time value '%s'".format(value)) }
		}
	}

	/** Select all "PasteCanvas" in the given set of layers. */
	protected def pasteCanvasOf(nodes:NodeSeq):NodeSeq = nodes.filter(node => (node \ "@type").text == "PasteCanvas")

	/** Select all "translate" in the given set of layers. */
	protected def translateOf(nodes:NodeSeq):NodeSeq = nodes.filter(node => (node \ "@type").text == "translate")

	/** Select all "rotate" in the given set of layers. */
	protected def rotateOf(nodes:NodeSeq):NodeSeq = nodes.filter(node => (node \ "@type").text == "rotate")

	/** Select all the "canvas" of a set of params. */
	protected def canvasParamOf(nodes:NodeSeq):NodeSeq = nodes.filter(node => (node \ "@name").text == "canvas")

	/** Parse a "PasteCanvas", a kind of group of joints, and produce a set of `TimedKeys`,
	  * recursively for the whole sub-hierarchy. */
	protected def parsePasteCanvas(pasteCanvas:NodeSeq, level:Int):HashMap[String,TimedKeys] = {
		val keys = new HashMap[String,TimedKeys]

		pasteCanvas foreach { pc => 
			val name = (pc \ "@desc").text
			val key  = new TimedKeys(name)

			// println("%s[%s]".format("    " * level, name))

			keys += (name -> key)

			canvasParamOf(pc \ "param") foreach { canvasParam =>
				val canvas      = (canvasParam \ "canvas")
				val layers      = (canvas \ "layer")
				val translate   = parseTranslate(translateOf(layers))
				val rotate      = parseRotate(rotateOf(layers))
				key.rotate    ++= rotate
				key.translate ++= translate

				// println("%s   -> translate [%s]".format("    " * level, translate.mkString(", ")))
				// println("%s   -> rotate    [%s]".format("    " * level, rotate.mkString(", ")))

				keys ++= parsePasteCanvas(pasteCanvasOf(layers), level+1) 
			}
		}

		keys
	}

	/** Parse either a set of animated translation or a single translation. */
	protected def parseTranslate(layers:NodeSeq):ArrayBuffer[TimedVector] = {
		val vectors = new ArrayBuffer[TimedVector]

		(layers \ "param").filter(param => (param \ "@name").text == "origin").foreach { param =>
			param.child.foreach { child => child.label match {
					case "animated" => vectors ++= parseAnimatedVectors(child)
					case "vector"   => {} //vectors += TimedVector(0, parseVector(child))
					case _          => {}
				}
			}
		}

		vectors
	}

	/** Parse a set of animated translation. */
	protected def parseAnimatedVectors(animated:Node):ArrayBuffer[TimedVector] = {
		val vectors = new ArrayBuffer[TimedVector]

		(animated \ "waypoint") foreach { waypoint =>
			val timeMs = convertInMillis((waypoint \ "@time").text)
			val vector = parseVector((waypoint \ "vector").head)

			vectors += TimedVector(timeMs, vector)
		}

		vectors
	}

	/** Parse a single vector. */
	protected def parseVector(vector:Node):Vector2 = Vector2((vector \ "x").text.toDouble, (vector \ "y").text.toDouble)

	/** Parse either a set of animated angles or a single angle. */
	protected def parseRotate(layers:NodeSeq):ArrayBuffer[TimedValue] = {
		val values = new ArrayBuffer[TimedValue]

		(layers \ "param").filter(param => (param \ "@name").text == "amount").foreach { param =>
			param.child.foreach { child => child.label match {
					case "animated" => values ++= parseAnimatedAngles(child)
					case "angle"    => {} //values += TimedValue(0, parseAngle(child))
					case _          => {}
				}
			}
		}

		values
	}

	/** Parse a set of animated angles. */
	protected def parseAnimatedAngles(animated:Node):ArrayBuffer[TimedValue] = {
		val angles = new ArrayBuffer[TimedValue]

		(animated \ "waypoint") foreach { waypoint =>
			val timeMs = convertInMillis((waypoint \ "@time").text)
			val angle  = parseAngle((waypoint \ "angle").head)

			angles += TimedValue(timeMs, angle)
		}

		angles
	}

	/** Parse a single angle. */
	protected def parseAngle(angle:Node):Double = (angle \ "@value").text.toDouble
}