package org.sofa.math

import org.scalatest.{FlatSpec, Matchers}

import org.sofa.math._
import scala.math._


class TestRgba extends FlatSpec with Matchers {    
	"Rgba" should "convert fromHSV pure red" in {
		val red = Rgba.fromHSV(0, 1, 1)	// Pure red, 0°.

		red.red   should be (1.0 +- 0.001)
		red.green should be (0.0 +- 0.001)
		red.blue  should be (0.0 +- 0.001)
	}

	it should "convert fromHSV pure green" in {
		val green = Rgba.fromHSV(2*(Pi/3.0), 1, 1)	// Pure green, 120°

		green.red   should be (0.0 +- 0.001)
		green.green should be (1.0 +- 0.001)
		green.blue  should be (0.0 +- 0.001)
	}

	it should "convert fromHSV pure blue" in {
		val blue = Rgba.fromHSV(4*(Pi/3.0), 1, 1)	// Pure blue, 240°

		blue.red   should be (0.0 +- 0.001)
		blue.green should be (0.0 +- 0.001)
		blue.blue  should be (1.0 +- 0.001)
    }   

	it should "convert fromHSV pure yellow" in {
		val yellow = Rgba.fromHSV((Pi/3.0), 1, 1)	// Pure yellow, 60°

		yellow.red   should be (1.0 +- 0.001)
		yellow.green should be (1.0 +- 0.001)
		yellow.blue  should be (0.0 +- 0.001)
    }   

	it should "convert fromHSV pure cyan" in {
		val cyan = Rgba.fromHSV(Pi, 1, 1)	// Pure cyan, 180°

		cyan.red   should be (0.0 +- 0.001)
		cyan.green should be (1.0 +- 0.001)
		cyan.blue  should be (1.0 +- 0.001)
    }   

	it should "convert fromHSV pure magenta" in {
		val magenta = Rgba.fromHSV(5*(Pi/3.0), 1, 1)	// Pure magenta, 300°

		magenta.red   should be (1.0 +- 0.001)
		magenta.green should be (0.0 +- 0.001)
		magenta.blue  should be (1.0 +- 0.001)
    }   

	it should "convert fromHSV dark magenta" in {
		val magenta = Rgba.fromHSV(5*(Pi/3.0), 1, 0.5)	// Dark magenta, 300°

		magenta.red   should be (0.5 +- 0.001)
		magenta.green should be (0.0 +- 0.001)
		magenta.blue  should be (0.5 +- 0.001)
    }   
}