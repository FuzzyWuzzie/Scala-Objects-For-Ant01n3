package org.sofa.math


/** A double value oscillating linearly between two values.
  *
  * This is an "animated" double value, the `value` oscillates between a `min` and 
  * a `max`, by ((`max`-'min')*`speed`) increments. The `dir` direction inverts
  * regularly when `max` or `min` is reached. */
class BouncingDouble(var value:Double, val min:Double, val max:Double, val speed:Double, var dir:Double = 1) {
	/** Move the value toward `max` or `min`. */
	def step() {
		value += (dir * (speed * (max-min)))

		if(value > max) { value = max; dir = -dir }
		else if(value < min) { value = min; dir = -dir }
	}

	override def toString():String = "%f".format(value)
}