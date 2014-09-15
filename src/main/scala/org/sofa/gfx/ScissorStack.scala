package org.sofa.gfx

import org.sofa.math.Point4
import scala.math._


/** ScissorStack companion object. */
object ScissorStack {
	def apply():ScissorStack = new ScissorStack
}


/** A scissor box. */ 
case class Scissors(x:Int, y:Int, width:Int, height:Int) {}


/** A way to apply nested scissor tests, eventually from a given transformed space. */
class ScissorStack {

	/** The scissor stack. */
	protected[this] val stack = new scala.collection.mutable.ArrayBuffer[Scissors]()

	/** Avoid to reallocate a point at each push. */
	protected[this] val p0 = Point4(0, 0, 0, 1)

	/** Avoid to reallocate a point at each push. */
	protected[this] val p1 = Point4(0, 0, 0, 1)

	/** Push a scissor test around coordinates (`x0`, `y0`) and (`x1`, `y1`) within
	  * a transformed `space`. This will project the two points into pixel coordinates
	  * and then set the scissor test according to these projected coordinates. */
	def push(gl:SGL, x0:Double, y0:Double, x1:Double, y1:Double, space:Space):Scissors = {
		p0.set(x0, y0, 0, 1)
		p1.set(x1, y1, 0, 1)
		space.projectInPlace(p0)
		space.projectInPlace(p1)

		val w  = space.viewport(0)
		val h  = space.viewport(1)
		val X0 = ceil(p0.x / 2 * w + w / 2).toInt
		val Y1 = ceil(p0.y / 2 * h + h / 2).toInt
		val X1 = ceil(p1.x / 2 * w + w / 2).toInt
		val Y0 = ceil(p1.y / 2 * h + h / 2).toInt

		push(gl, X0, Y0, X1 - X0, Y1 + Y0)
	}

	/** Push a scissor test with box (`x`, `y`, `width`, `height`) in pixels.
	  * If there was no scissor test before, the scissor test is enabled. */
	def push(gl:SGL, x:Int, y:Int, width:Int, height:Int):Scissors = {
		push(gl, Scissors(x, y, width, height))
	}

	/** Push a scissor test with a given `scissor` box in pixels.
	  * If there was no scissor test before, the scissor test is enabled. */
	def push(gl:SGL, scissors:Scissors):Scissors = {
		if(stack.isEmpty)
			gl.enable(gl.SCISSOR_TEST)

		stack += scissors
		gl.scissor(scissors.x, scissors.y, scissors.width, scissors.height)
		gl.checkErrors

		scissors		
	}

	/** Pop the last pushed scissor test. If after this there is no
      * scissor box, the scissor test is disabled. */
	def pop(gl:SGL) {
		if(stack.isEmpty)
			throw new RuntimeException("cannot pop scissor stack, empty")

		stack.trimEnd(1)

		if(stack.isEmpty) {
			gl.disable(gl.SCISSOR_TEST)
		} else {
			val scissors = stack(stack.size - 1)
			gl.scissor(scissors.x, scissors.y, scissors.width, scissors.height)
		}
		gl.checkErrors
	}

	/** Push the given scissor box, run the code and pop the box. */
	def pushpop(gl:SGL, x:Int, y:Int, width:Int, height:Int)(code: =>Unit) {
		push(gl, x, y, width, height)
		code
		pop(gl)
	}

	/** Push the given scissor coordinates in the given space, run the code and pop the scissor test. */
	def pushpop(gl:SGL, x0:Double, y0:Double, x1:Double, y1:Double, space:Space)(code: => Unit) {
	}

	def foreach(code:(Scissors)=>Unit) {
		var i = 0
		val n = stack.size

		while(i < n) {
			code(stack(i))
			i += 1
		}
	}
}