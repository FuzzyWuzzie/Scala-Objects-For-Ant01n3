package org.sofa.gfx.io.collada

import scala.xml.Node
import scala.collection.mutable.ArrayBuffer

/** Unit types used inside a Collada file. */
object Units extends Enumeration {
	val Meters = Value(1)
	val Centimeters = Value(100)
}

/** Units of a Collada file. */
class Unit {
	/** The unit. */
	var units = Units.Meters
	/** How many meters does the units resolves to. */
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

/** Axis types. */
object Axis extends Enumeration {
	val X = Value(0)
	val Y = Value(1)
	val Z = Value(2)
}

/** A contributor in a Collada file. */
class Contributor(val name:String, val tool:String) {
	override def toString():String = "contrib(%s, %s)".format(name, tool)
}

/** Assets of a Collada file. */
class Assets {
	/** Contributors to the file. */
	var contributors = new ArrayBuffer[Contributor]()
	/** File creation. */
	var created:String = ""
	/** Last modification date. */
	var modified:String = ""
	/** Units of the file. */
	var units = new Unit()
	/** What is the up axis ? */
	var upAxis = Axis.Y
	
	def this(xml:Node) {
		this()
		
		(xml \ "contributor").foreach { contributor =>
			contributors += new Contributor((contributor \ "author").text, (contributor \ "authoring_tool").text)
		}
		
		created  = (xml \ "created").text
		modified = (xml \ "modified").text
		units    = new Unit((xml \ "unit").head)
		upAxis   = (xml \ "up_axis").text match {
			case "Z_UP" => Axis.Z
			case "X_UP" => Axis.X
			case _      => Axis.Y
		}
	} 
	
	override def toString():String = "asset((%s), created %s, modified %s, %s up, %s)".format(contributors.mkString(", "), created, modified, upAxis, units)
}

/** An element of a Collada library. */
class ColladaFeature {}