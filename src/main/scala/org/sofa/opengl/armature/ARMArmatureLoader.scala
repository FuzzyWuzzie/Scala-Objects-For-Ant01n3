package org.sofa.opengl.armature

import scala.io.Source
import scala.collection.mutable.HashMap

import org.sofa.Timer
import org.sofa.math.Point2


// TODO remove scale from the format, not needed !


class ARMArmatureIOException(msg:String) extends Exception(msg)


object ARMArmatureLoader {
	//                             |    name        |     pagew               pageh            |    scale       |
	final val ArmatureDesc = """A\[\s*"([^"\s]+)"\s*,\s*\(\s*([^,\s]+)\s*,\s*([^\)\s]+)\s*\)\s*,\s*([^\]\s]+)\s*\]""".r
	//                          |name            |z              |         fromx           fromy           sizex           sizey            |         pivotx          pivoty           |         anchorx         anchory          |    visible    |        under        |         above
	final val JointDesc = """J\[\s*"([^"\s]+)"\s*,\s*([^,\s]+)\s*,\s*\(\s*([^,\s]+)\s*,\s*([^,\s]+)\s*,\s*([^,\s]+)\s*,\s*([^\)\s]+)\s*\)\s*,\s*\(\s*([^,\s]+)\s*,\s*([^\)\s]+)\s*\)\s*,\s*\(\s*([^,\s]+)\s*,\s*([^\s\)]+)\s*\)\s*,\s*([^,\s]+)\s*,\s*\{\s*([^\}]*)\}\s*,\s*\{\s*([^\}]*)\}\s*\]""".r
}


class ARMArmatureLoader {
	import ARMArmatureLoader._

	protected[this] val joints = new HashMap[String, Joint]()

	def load(name:String, texRes:String, shaderRes:String, fileName:String, armatureId:String, scale:Double):Armature = {
		var lineNo = 0
		var armature:Armature = null
Timer.timer.measure("ARMArmatureLoader.load()") {
		Source.fromFile(fileName).getLines.foreach { line =>
			if(lineNo == 0) {
				if(line != "ARM001") throw new ARMArmatureIOException(s"invalid ARM file format, or wrong version of the file (${fileName})")
			} else {
				line.trim match {
					case ArmatureDesc(name, pagew, pageh, sc) => {
						val page = Point2(pagew.toDouble, pageh.toDouble)
						val scal = sc.toDouble
						val root = joints.get("root").getOrElse(throw new ARMArmatureIOException("no root joint in ARM file ?"))

						// println("A[%s, (%f, %f), %f]".format(name, page.x, page.y, scal))

						armature = Armature(name, scale, page, texRes, shaderRes, root)
					}
					case JointDesc(name, sz, fromx, fromy, sizex, sizey,
								   pivotx, pivoty, anchorx, anchory,
								   visible, sunder, sabove) => {

						val z      = sz.toDouble
						val from   = Point2(fromx.toDouble, fromy.toDouble)
						val size   = Point2(sizex.toDouble, sizey.toDouble)
						val pivot  = Point2(pivotx.toDouble, pivoty.toDouble)
						val anchor = Point2(anchorx.toDouble, anchory.toDouble)
						val vis    = visible == "1"
						val under  = findJoints(name, sunder)
						val above  = findJoints(name, sabove)

						// println("J[%s, %f, (%f, %f, %f, %f), (%f, %f), (%f, %f), %b, {%s}, {%s}]".format(
						// 	name, z, from.x, from.y, size.x, size.y, pivot.x, pivot.y, anchor.x, anchor.y,
						// 	vis,
						// 	if(under ne null) under.map(_.name).mkString(",") else "",
						// 	if(above ne null) above.map(_.name).mkString(",") else ""))

						joints += (name -> new Joint(name, z, from, size, pivot, anchor, vis, under, above))
					}
					case _ => {
						throw new ARMArmatureIOException(s"unrecognized phrase in ARM file '${fileName}':${lineNo}: '${line}'")
					}
				}
			}

			lineNo += 1
		}
}
		armature
	}

	protected def findJoints(src:String, list:String):Array[Joint] = {
		val l = list.trim
		if(l.length > 0) {
			l.split(",").map { name =>
				joints.get(name.trim).getOrElse(
					throw new ARMArmatureIOException(s"a joint ${src} reference another ${name} unknown, malformed ARM file ?")
				)
			}
		} else {
			null
		}
	}
}