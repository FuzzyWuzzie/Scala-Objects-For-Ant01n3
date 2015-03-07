package org.sofa.math

import scala.math._


/** An angle value in radians that interpolate linearly and smoothly when rotated.
  * This is an "animated" `angle` value that you can rotate positively or negatively
  * and that will adjust progressively at a given `speed`.
  */
class LerpAngle(var angle:Double, var speed:Double) {
	
	protected var dir = 0.0

	protected var target = 0.0

	angle = (angle % (2 * Pi))

	/** Rotate the angle `by` the given amount, the value will try to reach this
	  * after several calls to `step()`. At each `step()` the angle is rotated by
	  * (`by`*`speed`). */
	def rotate(by:Double) {
		var b = (by % (2 * Pi))
		dir = b * speed
		target += b
		target = target % (2*Pi)
	}

	/** Adjust the `angle` field to reach a target given by the next `rotate()` call. */
	def step() {
		angle += dir
		var da = target - (angle % (2 * Pi))

		if(abs(da) < 0.01) {
			angle = target
			dir = 0.0
		}		
	}

	override def toString():String = "%f".format(angle)
}