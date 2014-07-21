package org.sofa.opengl.armature.behavior

import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet}

import org.sofa.FileLoader
import org.sofa.math.{Point2, Vector2}
import org.sofa.opengl.armature.{Armature, Joint, SifzArmatureBehaviorLoader, TimedKeys, TimedValue, TimedVector}

import org.sofa.behavior._


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


/** Change the visibility of a set of joints so that only one is visible at a given time.
  *
  * The duration is the time during which a joint is visible. Once this duration is
  * passed, the joint is made invisible and another is made visible. This in an infinite
  * loop. The joints visibility order it the one of the given parameter list. */
case class Switch(override val name:String, duration:Long, joints:Joint *) extends Behavior(name) {
	protected[this] var index = 0

	protected[this] var startTime = 0L

	def start(t:Long):Behavior = {
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
			var idx = (t - startTime) / duration

			if(idx >= joints.length) idx = joints.length-1
//printf("joints(%d) t=%d startTime=%d duration=%d, (t-startTime)=%d, (t-startTime)/duration=%d%n",
//			joints.length, t, startTime, duration, (t-startTime), (t-startTime)/duration)
			if(idx > index) {
				joints(index).visible = false
				index = idx.toInt
				joints(index).visible = true
			}
		}
	}

	def finished(t:Long):Boolean = (t > (startTime + (duration * joints.size)))
}


// -- Joint behaviors -----------------------------------------------------------------------------------------


/** Animate a rotation of the joint until it reaches a given `targetAngle` a the given start date plus the `duration`. 
  * This is an absolute rotation. The joint will at the end be oriented by the given `targetAngle`. */
case class LerpToAngle(override val name:String, joint:Joint, targetAngle:Double, override val duration:Long) extends LerpBehavior(name, duration) {
	protected[this] var startAngle:Double = 0.0

	override def start(t:Long):Behavior = {
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


/** Animate a displacement of the joint until it reaches a given `targetPosition` at the given start date plus the `duration`. 
  * This is an absolute displacement. The joint will at the end be at the given `targetPosition`. */
case class LerpToPosition(override val name:String, joint:Joint, targetPosition:(Double,Double), override val duration:Long) extends LerpBehavior(name, duration) {
	protected[this] var startPosition = new Point2(0,0)

	override def start(t:Long):Behavior = {
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


/** Animate a scale of the joint until it reaches a given `targetScale` at the given start date plus the `duration`. 
  * This is an absolute scale. The joint will at the end be at the given `targetScale`. */
case class LerpToScale(override val name:String, joint:Joint, targetScale:(Double,Double), override val duration:Long) extends LerpBehavior(name, duration) {
	protected[this] var startScale = new Point2(0,0)

	override def start(t:Long):Behavior = {
		startScale.copy(joint.transform.scale)
		super.start(t)
	}

	def animate(t:Long) {
		if(finished(t)) {
			joint.transform.scale.set(targetScale._1, targetScale._2)
		} else {
			val interp = interpolation(t)
			joint.transform.scale.set(
				startScale.x + ((targetScale._1 - startScale.x) * interp),
				startScale.y + ((targetScale._2 - startScale.y) * interp)
			)
		}
	}
}


/** Animate a displacement of the joint by a given `displacement` during a given `duration`.
  * This is a relative displacement. The joint will start at its current position and end at this
  * position plus the `displacement` after the given duration. */
case class LerpMove(override val name:String, joint:Joint, displacement:(Double,Double), override val duration:Long) extends LerpBehavior(name, duration) {
	protected[this] var startPosition = new Point2(0,0)

	override def start(t:Long):Behavior = {
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


// -- KeyInterp ------------------------------------------------------------------------------------------


/** Base interpolator for joints using a sequence of keys at specific times. */
abstract class LerpKeyJoint(name:String, val joint:Joint) extends Behavior(name) {
	/** Time at start (used to avoid time drift). */
	protected[this] var init = 0L

	/** The current key start time. */
	protected[this] var from = 0L

	/** The current key end time. */
	protected[this] var to = 0L

	def start(t:Long):Behavior = { init = t; from = t; to = t + next; this }

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
class LerpKeyMoveJoint(name:String, joint:Joint, val translate:Array[TimedVector]) extends LerpKeyJoint(name, joint) {
	protected[this] var index = -1

	protected[this] var startPosition = Vector2(0,0)

	protected[this] var fromPosition = Vector2(0,0)

	protected[this] var toPosition = Vector2(0,0)

	override def start(t:Long):Behavior = {
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
class LerpKeyRotateJoint(name:String, joint:Joint, val rotate:Array[TimedValue]) extends LerpKeyJoint(name, joint) {
	protected[this] var index = -1

	protected[this] var startAngle = 0.0

	protected[this] var fromAngle = 0.0

	protected[this] var toAngle = 0.0

	override def start(t:Long):Behavior = {
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


object  LerpKeyArmature {
	def loadFrom(fileName:String, armature:Armature, scal:Double = 1.0):Array[Behavior] = {
		val keys = ArmatureBehavior.loader.load(fileName)
		val behaviors = new ArrayBuffer[LerpKeyJoint]

		keys.foreach { keyset ⇒
			val name  = keyset._1
			val anim  = keyset._2
			val joint = (armature \\ name)

			if((anim.translate ne null) && anim.translate.size > 0)
				behaviors += new LerpKeyMoveJoint(name, joint, scale(scal, anim.translate.toArray))
			
			if((anim.rotate ne null) && anim.rotate.size > 0)
				behaviors += new LerpKeyRotateJoint(name, joint, anim.rotate.toArray)
		}

		behaviors.toArray
	}	

	private def scale(scale:Double, translate:Array[TimedVector]):Array[TimedVector] = {
		translate.foreach { tv ⇒
			tv.vector.set(tv.vector.x * scale, tv.vector.y * scale)
		}
		translate
	}

	def apply(name:String, armature:Armature, fileName:String, scale:Double = 1.0):LerpKeyArmature = new LerpKeyArmature(name, armature, fileName, scale)
	def apply(name:String, armature:Armature, behaviors:Behavior *):LerpKeyArmature = new LerpKeyArmature(name, armature, behaviors:_*)
}


/** A set of joint key interpolators. */
class LerpKeyArmature(name:String, armature:Armature, behaviors:Behavior *) extends InParallel(name, behaviors:_*) {
	import LerpKeyArmature._

	def this(name:String, armature:Armature, fileName:String, scale:Double = 1.0) {
		this(name, armature, LerpKeyArmature.loadFrom(fileName, armature, scale):_*)
	}
}