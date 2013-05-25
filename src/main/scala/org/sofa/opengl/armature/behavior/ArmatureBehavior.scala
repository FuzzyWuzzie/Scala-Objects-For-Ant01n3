package org.sofa.opengl.armature.behavior

import scala.collection.mutable.{ArrayBuffer, HashMap}

import org.sofa.FileLoader
import org.sofa.math.{Point2}
import org.sofa.opengl.armature.{Armature, Joint, SifzArmatureBehaviorLoader, TimedKeys, TimedValue, TimedVector}


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

// -- KeyInterp ------------------------------------------------------------------------------------------

class KeyInterp(name:String, armature:Armature) extends ArmatureBehavior(name) {

	def loadFrom(fileName:String) {
		val keys = ArmatureBehavior.loader.load(fileName)

println("*** keys size %d".format(keys.size))
		keys foreach { item =>
			println("have %d translate and %d rotate for %s".format(item._2.translate.size, item._2.rotate.size, item._1))
		}
	}

	def start(t:Long):ArmatureBehavior = {
		null
	}

	def animate(t:Long) {

	}

	def finished(t:Long):Boolean = {
		true
	}
	
}