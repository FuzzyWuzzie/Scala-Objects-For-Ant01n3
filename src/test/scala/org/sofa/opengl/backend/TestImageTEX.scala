package org.sofa.gfx.backend

import org.scalatest.FlatSpec

import org.sofa._
import org.sofa.gfx._
import org.sofa.gfx.backend._


class TestImageTEX extends FlatSpec {
	"An ImageTEX" should "load and save properly with no transparency" in {
		val src = ImageTEX.loadFrom("/Users/antoine/Documents/Art/Images/TestTEXplain.png")
		ImageTEX.save("/Users/antoine/Desktop/testTEX.tex", src)
		val dst = ImageTEX.load("/Users/antoine/Desktop/testTEX.tex")

		assert(dst ne null, "loaded TEX should not be null")
		assertResult(src.width)  { dst.width }
		assertResult(src.height) { dst.height }
		assertResult(true) { compare(src, dst) }
	}

	it should "load and save properly with transparency" in {
		val src = ImageTEX.loadFrom("/Users/antoine/Documents/Art/Images/TestTEXalpha.png")
		ImageTEX.save("/Users/antoine/Desktop/testTEX.tex", src)
		val dst = ImageTEX.load("/Users/antoine/Desktop/testTEX.tex")

		assert(dst ne null, "loaded TEX should not be null")
		assertResult(src.width)  { dst.width }
		assertResult(src.height) { dst.height }
		assertResult(true) { (compare(src, dst)) }
	}

	def compare(src:ImageTEX, dst:ImageTEX):Boolean = {
		val length = src.data.capacity
		var i = 0
		var ok = true

		while(ok && i < length) {
			ok = src.data(i) == dst.data(i)
			i += 1
		}

		ok
	}
}