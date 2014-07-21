package org.sofa.behavior

import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet}

import org.sofa.FileLoader
import org.sofa.math.{Point2, Vector2}


// -- Base behavior -------------------------------------------------------------------------------------


/** Base abstract behavior class. */
abstract class Behavior(val name:String) {
	/** Start the behavior.
	  * @param t current time. */
	def start(t:Long):Behavior
	
	/** Animate the armature or a part of it.
	  * @param t current time. */
	def animate(t:Long)
	
	/** True if the behavior is finished. In this case, you can use
	  * start(Long)` to restart it. */
	def finished(t:Long):Boolean

	override def toString():String = "%s".format(name)
}


// -- Grouping behaviors ---------------------------------------------------------------------------------


/** Execute all agregated behaviors in parallel. The duration of this
  * behavior is the maximum duration of its agregated behaviors. In other words
  * as long as it remains an agregated behavior that is not finished this
  * behavior will not be finished. However be careful that behaviors are all
  * synchronized. This means that they always all start at the same time and
  * execute as many times as each other. */
case class InParallel(override val name:String, behaviors:Behavior *) extends Behavior(name) {
	def start(t:Long):Behavior = { behaviors.foreach { _.start(t) }; this }
	def animate(t:Long) { behaviors.foreach { _.animate(t) } }
	def finished(t:Long):Boolean = { behaviors.find { b ⇒ b.finished(t) == false } match {
			case None ⇒ true
			case _    ⇒ false
		}
	}
}


object InParallelDynamic {
	def apply(name:String, behaviors:Behavior*):InParallelDynamic = 
		new InParallelDynamic(name, behaviors:_*)
}


/** Execute all agregated behaviors in parallel. The difference with [[InParallel]]
  * is that you can add or remove new behaviors at any time, and that finished behaviors
  * are automatically removed. As long as it remains an agregated behavior that is not
  * finished, this behavior will not be finished. When finished this is empty. */
class InParallelDynamic(name:String, behaviorList:Behavior *) extends Behavior(name) {
	protected[this] val behaviors = new HashSet[Behavior]()

	behaviors ++= behaviorList

	def start(t:Long):Behavior = { 
		behaviors.foreach { _.start(t) }
		this 
	}

	/** Add an start the given `behavior` at time `t`. */
	def add(t:Long, behavior:Behavior) {
		behaviors += behavior
		behavior.start(t)
	}

	/** Remove the given `behavior`. */
	def remove(behavior:Behavior) {
		behaviors -= behavior
	}
	
	def animate(t:Long) {
		behaviors.foreach { behavior ⇒
			if(behavior.finished(t)) {
				behaviors -= behavior
			} else {
				behavior.animate(t)
			}
		} 
	}

	def finished(t:Long):Boolean = behaviors.isEmpty
}


/** Does nothing excepted consuming time until a given delay. */
case class Wait(override val name:String, duration:Long) extends Behavior(name) {
	protected[this] var startTime = 0L

	def start(t:Long):Behavior = { startTime = t; this }

	def animate(t:Long) {}

	def finished(t:Long) = t >= (startTime+duration) 
}


object InSequence {
	def apply(name:String, behaviors:Behavior*):InSequence = 
		new InSequence(name, behaviors:_*)
}


/** Execute all the agregated behaviors one after the other. The order is the
  * the one of the given behavior list. The duration of this behavior is the
  * sum of the duration of its agregated behaviors. */
class InSequence(name:String, b:Behavior *) extends Behavior(name) {
	protected[this] val behaviors = b.toArray
	
	protected[this] var index = 0

	def start(t:Long):Behavior = {
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


/** Repeats each behavior either a given number of times or infinitely.
  * 
  * Indicate a negative or zero `limit` to repeat indefinitely. Each behavior is repeated in
  * parallel. Each time a behavior is finished, it restarts (if it did not reached the limit).
  * When there is no limit a behavior twice faster than another will run twice more.
  * This behavior may never finish if the `limit` is zero or negative. */
case class Loop(override val name:String, limit:Int, behaviors:Behavior *) extends Behavior(name) {
	/** Number of repetitions for each behavior. */
	protected[this] val repeated = new Array[Int](behaviors.size)

	/** How many behaviors finished. */
	protected[this] var howManyFinished = 0

	def start(t:Long):Behavior = {
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
							if(limit > 0) {
								howManyFinished += 1
								// TODO remove the finished behavior !
							}	
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


/** Base implementation of something able to linearly interpolate values.
  *
  * The interpolation is time based, this means that knowing the start of the behavior and its
  * duration, when `animate() is called, the interpolated value depends linearly on the time. */
abstract class LerpBehavior(name:String, val duration:Long) extends Behavior(name) {

	protected[this] var from = 0L

	protected[this] var to = 0L
	
	def start(t:Long):Behavior = { from = t; to = t + duration; this }

	protected def interpolation(t:Long):Double = (t-from).toDouble / (to-from).toDouble

	def finished(t:Long):Boolean = (t >= to)
}