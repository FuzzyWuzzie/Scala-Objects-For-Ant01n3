package org.sofa.opengl.armature.behavior

import scala.collection.mutable.{ArrayBuffer, HashMap}

import org.sofa.FileLoader
import org.sofa.math.{Point2, Vector2}
import org.sofa.opengl.armature.{Armature, Joint, SifzArmatureBehaviorLoader, TimedKeys, TimedValue, TimedVector}



// A set of "behaviors" for armatures.
//
// This is still a prototype of a pseudo DSL of composable behaviors.



// -- Loaders -------------------------------------------------------------------------------------


/** Pluggable loader for armature behavior sources. */
trait ArmatureBehaviorLoader extends FileLoader {
    /** Try to open a resource, or throw an IOException if not available. */
    def load(resource:String):HashMap[String,TimedKeys]
}


/** Default loader for armature behaviors, based on files and the include path.
  * This loader tries to open the given resource directly, then if not
  * found, tries to find it in each of the pathes provided by the include
  * path. If not found it throws an IOException. */
class DefaultArmatureBehaviorLoader extends ArmatureBehaviorLoader {
	private[this] val loader = new SifzArmatureBehaviorLoader()

    def load(resource:String):HashMap[String,TimedKeys] = {
        loader.load(findPath(resource, ArmatureBehavior.path))
    }
}


// -- Base behavior -------------------------------------------------------------------------------------


object ArmatureBehavior {
	val path = new ArrayBuffer[String]	

	var loader:ArmatureBehaviorLoader = new DefaultArmatureBehaviorLoader()
}

/** Base abstract behavior class. */
abstract class ArmatureBehavior(val name:String) {
	def start(t:Long):ArmatureBehavior
	def animate(t:Long)
	def finished(t:Long):Boolean

	override def toString():String = "%s".format(name)
}


// -- Grouping behaviors ---------------------------------------------------------------------------------


class DoInParallel(name:String, val behaviors:ArmatureBehavior *) extends ArmatureBehavior(name) {
	def start(t:Long):ArmatureBehavior = { behaviors.foreach { _.start(t) }; this }
	def animate(t:Long) { behaviors.foreach { _.animate(t) } }
	def finished(t:Long):Boolean = { behaviors.find { b => b.finished(t) == false } match {
			case None => true
			case _    => false
		}
	}
}


class DoInSequence(name:String, b:ArmatureBehavior *) extends ArmatureBehavior(name) {
	val behaviors = b.toArray
	
	var index = 0

	def start(t:Long):ArmatureBehavior = {
		index = 0
		behaviors(index).start(t)
		this
	}
	
	def animate(t:Long) { 
		if(index < behaviors.length) {
			if(behaviors(index).finished(t)) {
				index += 1
				if(index < behaviors.length)
					behaviors(index).start(t)
			}
			if(index < behaviors.length) {
				behaviors(index).animate(t)
			}
		}
	}

	def finished(t:Long):Boolean = (index >= behaviors.length)
}


// -- Joint behaviors -----------------------------------------------------------------------------------------


abstract class JointBehavior(name:String, val joint:Joint) extends ArmatureBehavior(name) {}


abstract class InterpolateBehavior(name:String, joint:Joint, val duration:Long) extends JointBehavior(name, joint) {
	var from = 0L

	var to = 0L
	
	def start(t:Long):ArmatureBehavior = { from = t; to = t + duration; this }

	protected def interpolation(t:Long):Double = (t-from).toDouble / (to-from).toDouble

	def finished(t:Long):Boolean = (t >= to)
}


class InterpToAngle(name:String, joint:Joint, val targetAngle:Double, duration:Long) extends InterpolateBehavior(name, joint, duration) {
	var startAngle:Double = 0.0

	override def start(t:Long):ArmatureBehavior = {
		startAngle = joint.transform.angle
		super.start(t)
	}

	def animate(t:Long) {
		if(finished(t)) {
			joint.transform.angle = targetAngle
		} else {
			joint.transform.angle = startAngle + ((targetAngle - startAngle) * interpolation(t))
//println("joint %s angle %f (%% == %f)".format(joint.name, joint.angle, percent))
		}
	}
}


/** Absolute displacement. */
class InterpToPosition(name:String, joint:Joint, val targetPosition:(Double,Double), duration:Long) extends InterpolateBehavior(name, joint, duration) {
	var startPosition = new Point2(0,0)

	override def start(t:Long):ArmatureBehavior = {
		startPosition.copy(joint.transform.translation)
		super.start(t)
	}

	def animate(t:Long) {
		if(finished(t)) {
			joint.transform.translation.set(targetPosition._1, targetPosition._2)
		} else {
			val interp =  interpolation(t)
			joint.transform.translation.set(
				startPosition.x + ((targetPosition._1 - startPosition.x) * interp),
				startPosition.y + ((targetPosition._2 - startPosition.y) * interp)
			)
		}
	}
}


/** Relative displacement. */
class InterpMove(name:String, joint:Joint, val displacement:(Double,Double), duration:Long) extends InterpolateBehavior(name, joint, duration) {
	var startPosition = new Point2(0,0)

	override def start(t:Long):ArmatureBehavior = {
		startPosition.copy(joint.transform.translation)
		super.start(t)
	}

	def animate(t:Long) {
		if(finished(t)) {
			joint.transform.translation.set(startPosition.x + displacement._1, startPosition.y + displacement._2)
		} else {
			val interp = interpolation(t)
			joint.transform.translation.set(
				startPosition.x + (displacement._1 * interp),
				startPosition.y + (displacement._2 * interp)
			)
		}
	}
}

// -- Switches -------------------------------------------------------------------------------------------

object JointVisibilitySwitch {
	def apply(name:String, duration:Long, joints:Joint *):JointVisibilitySwitch = new JointVisibilitySwitch(name, duration, joints:_*)
}

class JointVisibilitySwitch(name:String, val duration:Long, val joints:Joint *) extends ArmatureBehavior(name) {
	protected var index = 0

	protected var startTime = 0L

	def start(t:Long):ArmatureBehavior = {
		index = 0
		joints.foreach { _.visible = false }
		joints(index).visible = true
		startTime = t
		this
	}

	def animate(t:Long) {
		if(finished(t)) {
			joints(index).visible = false
			index = joints.size - 1
			joints(index).visible = true
		} else {
			val idx = (t - startTime) / duration

			if(idx > index) {
				joints(index).visible = false
				index = idx.toInt
				joints(index).visible = true
			}
		}
	}

	def finished(t:Long):Boolean = (t > (startTime + (duration * joints.size)))
}

// -- Loops ----------------------------------------------------------------------------------------------

object BehaviorLoop {
	def apply(name:String, limit:Int, behaviors:ArmatureBehavior *):BehaviorLoop = new BehaviorLoop(name, limit, behaviors:_*)
}

/** Repeats each behavior either a given number of times or infinitely.
  * 
  * Indicate a negative or zero `limit` to repeat indefinitely. */
class BehaviorLoop(name:String, val limit:Int, val behaviors:ArmatureBehavior *) extends ArmatureBehavior(name) {
	/** Number of repetitions for each behavior. */
	protected val repeated = new Array[Int](behaviors.size)

	/** How many behaviors finished. */
	protected var howManyFinished = 0

	def start(t:Long):ArmatureBehavior = {
		var i = 0
		while(i < repeated.size) { repeated(i) = 0; i += 1 }
		howManyFinished = 0
		this
	}

	def animate(t:Long) {
		if(finished(t)) {
			// Nop
		} else {
			var i = 0
			val n = behaviors.size
			val l = if(limit > 0) limit else Int.MaxValue

			while(i < n) {
				if(repeated(i) < l) {
					if(behaviors(i).finished(t)) {
						if(limit > 0)
							repeated(i) += 1
						
						if(repeated(i) < l) {
							behaviors(i).start(t)
						} else {
							if(limit > 0)
								howManyFinished += 1
						}
					} else {
						behaviors(i).animate(t)
					}
				}	

				i += 1
			}
		}
	}

	def finished(t:Long):Boolean = (howManyFinished == behaviors.size)
}

// -- KeyInterp ------------------------------------------------------------------------------------------

/** Base interpolator for joints using a sequence of keys at specific times. */
abstract class JointKeyInterp(name:String, joint:Joint) extends JointBehavior(name, joint) {
	/** Time at start (used to avoid time drift). */
	var init = 0L

	/** The current key start time. */
	var from = 0L

	/** The current key end time. */
	var to = 0L

	def start(t:Long):ArmatureBehavior = { init = t; from = t; to = t + next; this }

	def finished(t:Long):Boolean = (t >= to && (!hasNext))

	protected def interpolation(t:Long):Double = (t-from).toDouble / (to-from).toDouble

	/** Switch to the next key (if possible) and return the end-time of the next key (relative to the start not to the previous key). */
	protected def next():Long

	/** True if there is a next key. */
	protected def hasNext():Boolean
}

/** Relative displacement according to a set of key positions in time. 
  *
  * The set of translation vectors are absolute positions according to the start position, not relative one
  * with another. Each vector has a time that is relative to the start not to the previous key. */
class JointKeyMoveInterp(name:String, joint:Joint, val translate:Array[TimedVector]) extends JointKeyInterp(name, joint) {
	var index = -1

	var startPosition = Vector2(0,0)

	var fromPosition = Vector2(0,0)

	var toPosition = Vector2(0,0)

	override def start(t:Long):ArmatureBehavior = {
		index = -1
		
		startPosition.copy(joint.transform.translation)
		fromPosition.copy(startPosition)
		super.start(t)
	}

	def animate(t:Long) {
		if(finished(t)) {
			joint.transform.translation.set(startPosition.x + toPosition.x, startPosition.y + toPosition.y)
		} else {
			if(t > to) {
				from = to
				to   = init + next
			}
			
			val interp = interpolation(t)
			joint.transform.translation.set(
				startPosition.x + fromPosition.x + ((toPosition.x - fromPosition.x) * interp),
				startPosition.y + fromPosition.y + ((toPosition.y - fromPosition.y) * interp)
			)
		}
	}

	protected def next():Long = {		
		index += 1

		if(index < translate.length) {
			fromPosition.copy(toPosition)
			toPosition.copy(translate(index).vector)
			translate(index).timeMs
		} else {
			0L
		}
	}

	protected def hasNext():Boolean = (index+1 < translate.length)
}

/** Absolute rotation, according to a set of key angles in time. 
  *
  * The set of angles are absolute values, not relative one with another.
  * Each rotation has a time that is relative to the start not to the previous key. */
class JointKeyRotateInterp(name:String, joint:Joint, val rotate:Array[TimedValue]) extends JointKeyInterp(name, joint) {
	var index = -1

	var startAngle = 0.0

	var fromAngle = 0.0

	var toAngle = 0.0

	override def start(t:Long):ArmatureBehavior = {
		index     = -1
		fromAngle = joint.transform.angle
		
		super.start(t)
	}

	def animate(t:Long) {
		if(finished(t)) {
			joint.transform.angle = toAngle
		} else {
			if(t > to) {
				from = to
				to   = init + next
			}

			val interp = interpolation(t)
			joint.transform.angle = fromAngle + ((toAngle - fromAngle) * interp)
		}
	}

	protected def next():Long = {
		index += 1

		if(index < rotate.length) {
			fromAngle = toAngle
			toAngle   = rotate(index).value
			rotate(index).timeMs
		} else {
			0L
		}
	}

	protected def hasNext():Boolean = (index+1 < rotate.length)
}

object  ArmatureKeyInterp {
	def loadFrom(fileName:String, armature:Armature, scal:Double = 1.0):Array[ArmatureBehavior] = {
		val keys = ArmatureBehavior.loader.load(fileName)
		val behaviors = new ArrayBuffer[JointKeyInterp]

		keys.foreach { keyset =>
			val name  = keyset._1
			val anim  = keyset._2
			val joint = (armature \\ name)

			if((anim.translate ne null) && anim.translate.size > 0)
				behaviors += new JointKeyMoveInterp(name, joint, scale(scal, anim.translate.toArray))
			
			if((anim.rotate ne null) && anim.rotate.size > 0)
				behaviors += new JointKeyRotateInterp(name, joint, anim.rotate.toArray)
		}

		behaviors.toArray
	}	

	private def scale(scale:Double, translate:Array[TimedVector]):Array[TimedVector] = {
		translate.foreach { tv =>
			tv.vector.set(tv.vector.x * scale, tv.vector.y * scale)
		}
		translate
	}

	def apply(name:String, armature:Armature, fileName:String, scale:Double = 1.0):ArmatureKeyInterp = new ArmatureKeyInterp(name, armature, fileName, scale)
	def apply(name:String, armature:Armature, behaviors:ArmatureBehavior *):ArmatureKeyInterp = new ArmatureKeyInterp(name, armature, behaviors:_*)
}

/** A set of joint key interpolators. */
class ArmatureKeyInterp(name:String, armature:Armature, behaviors:ArmatureBehavior *) extends DoInParallel(name, behaviors:_*) {
	import ArmatureKeyInterp._

	def this(name:String, armature:Armature, fileName:String, scale:Double = 1.0) {
		this(name, armature, ArmatureKeyInterp.loadFrom(fileName, armature, scale):_*)
	}
}