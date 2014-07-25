package org.sofa.gfx.transforms

import org.sofa.gfx.Space


/** Generic way to transform a [[Space]]. */
abstract class SpaceTransform(val space:Space) {
	/** Push the new transfrom. By default only call push on the modelview matrix of the space. */
	def push() { space.push }

	/** Pop the transform and return in the space state before the call to `push()`. By default this
	  * only call pop on the modelview matrix of the space. */
	def pop() { space.pop }

	/** Call `push()`, execute the given `code`, then call `pop()`. */
	def pushpop(code:()=>Unit) { push; code(); pop }
}