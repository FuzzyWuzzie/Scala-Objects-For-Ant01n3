package org.sofa.gfx.armature

import java.io.{InputStream, FileInputStream}

import scala.io.Source
import scala.collection.mutable.HashMap

import org.sofa.Timer
import org.sofa.math.Point2


/** Raised for any error while loading an ARM file. */
class ARMArmatureIOException(msg:String) extends Exception(msg)


/** `ARMArmatureLoader` companion object. */
object ARMArmatureLoader {
	//                             |    name        |     pagew               pageh            |
	final val ArmatureDesc = """A\[\s*"([^"\s]+)"\s*,\s*\(\s*([^,\s]+)\s*,\s*([^\)\s]+)\s*\)\s*\]""".r
	//                          |name            |z              |         fromx           fromy           sizex           sizey            |         pivotx          pivoty           |         anchorx         anchory          |    visible    |        under        |         above       |
	final val JointDesc = """J\[\s*"([^"\s]+)"\s*,\s*([^,\s]+)\s*,\s*\(\s*([^,\s]+)\s*,\s*([^,\s]+)\s*,\s*([^,\s]+)\s*,\s*([^\)\s]+)\s*\)\s*,\s*\(\s*([^,\s]+)\s*,\s*([^\)\s]+)\s*\)\s*,\s*\(\s*([^,\s]+)\s*,\s*([^\s\)]+)\s*\)\s*,\s*([^,\s]+)\s*,\s*\{\s*([^\}]*)\}\s*,\s*\{\s*([^\}]*)\}\s*\]""".r
}


/** Armature loader based on the ARM format.
  *
  * The format is a simple text file. It always begins with "ARM001" this is the magic number
  * and also indicates the version of the format.
  *
  * Then follow a set of "joint" lines of the form:
  *
  *     J["<name>", <z>, (<fromx>, <fromy>, <sizex>, <sizey>), (<pivotx>, <pivoty>), (<anchorx>, <anchory>), <visibility>, {<under>}, {<above>}]
  *
  * Where :
  *  - <z> is the Z depth of the joint (could be used with the depth buffer, but not for joint ordering, see <above> and <under>).
  *  - (<fromx> ...) is the lower-left corner of the bounding box of the joint followed by its size in pixels.
  *  - (<pivotx> ...) is the pivot point position of the joint, in pixels.
  *  - (<anchorx> ...) is the position of the attach point of the joint in its parent, in pixels.
  *  - <visiblity> is either 0 or 1 meaning invisible or visible at start.
  *  - {<under>} is a list of joint names that compose the sub-hierarchy that is drawn before this joint.
  *  - {<abobe>} is a list of joint names that compose the sub-hierarchy that is drawn after this joint.
  *
  * All pixels values are absolute values inside the armature area (see under).
  *
  * The joints are always given in an order that gives joints lower in the hierarhy first. This 
  * could potentially allow to build the joints as soon as encoutered in the file, since sub-joints
  * will already have been declared. This is true even for armatures that reference the same joint
  * several times. The format support this and the shared joint is declared only once in the file,
  * before it is first needed. Be careful however that armatures do not support circular joint
  * references (cycles are not allowed).
  *
  * Finally the file ends with an "armature" line of the form:
  * 
  *     A["<name>", (<pagew>, <pageh>)]
  *
  * Where:
  *  - (<pagew>, <pageh>) is the armature area in the texture, in pixels.
  *
  * The fact the armature comes last allow to build the armature from all the previous joints when
  * encountered in the file, at the end. 
  */
class ARMArmatureLoader {
	import ARMArmatureLoader._

	/** Set of joints already encountered in the file. */
	protected[this] val joints = new HashMap[String, Joint]()

	/** Load an ARM file from `fileName`, an build an [[Armature]] at `scale` from it with the given
	  * `texture`  and `shader` from the [[Libraries]]. */
	def load(fileName:String, texture:String, shader:String, scale:Double):Armature =
		load(new FileInputStream(fileName), fileName, texture, shader, scale)

	/** Load an ARM file from an input `stream`, an build an [[Armature]] at `scale` from it with the given
	  * `texture`  and `shader` from the [[Libraries]]. `fileName` is only used in case of error to report the problem. */
	def load(stream:InputStream, fileName:String, texture:String, shader:String, scale:Double):Armature = {
		var lineNo = 0
		var armature:Armature = null

		Timer.timer.measure("ARMArmatureLoader.load()") {

			Source.fromInputStream(stream).getLines.foreach { line =>
				if(lineNo == 0) {
					if(line != "ARM001") throw new ARMArmatureIOException(s"invalid ARM file format, or wrong version of the file (${fileName})")
				} else {
					line.trim match {
						case ArmatureDesc(name, pagew, pageh) => {
							val page = Point2(pagew.toDouble, pageh.toDouble)
							val root = joints.get("root").getOrElse(throw new ARMArmatureIOException("no root joint in ARM file ?"))

							armature = Armature(name, scale, page, texture, shader, root)
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

	/** Parse a list of joint names and return an array of these joints.
	  *
	  * This method works since any ARM file is organized such that when a joint
	  * is encountered in the ARM file, its subjoints have already been declared. */
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