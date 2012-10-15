package org.sofa.math.test

import org.sofa.math.Triangle
import org.sofa.math.Point3
import org.junit._
import org.junit.Assert._
import scala.math._

object TestTriangle {
	def main(args:Array[String]):Unit = {
		(new TestTriangle).test
	}
}

class TestTriangle {
	@Test
	def test() {
		val t0 = Triangle(Point3(1,0,1), Point3(1,0,3), Point3(4,0,1))

		// Test distance from the three vertices.
		// Test is done so that the distance equals sqrt(3).
		
		val p0 = Point3(0,1,0)
		val (d0,pp0) = t0.distanceFrom(p0)
		
		assertEquals(sqrt(3), d0, 0.001)
		assertEquals(Point3(1,0,1), pp0)
		
		val p1 = Point3(5,1,0)
		val (d1,pp1) = t0.distanceFrom(p1)
		
		assertEquals(sqrt(3), d1, 0.001)
		assertEquals(Point3(4,0,1), pp1)
		
		val p2 = Point3(0,1,4)
		val (d2,pp2) = t0.distanceFrom(p2)
		
		assertEquals(sqrt(3), d2, 0.001)
		assertEquals(Point3(1,0,3),pp2)
		
		// Test the distance from the three edges.
		// Test is done so that the distance is sqrt(2).
		
		val p3 = Point3(2,1,0)
		val (d3,pp3) = t0.distanceFrom(p3)
		
		assertEquals(sqrt(2), d3, 0.001)
		assertEquals(Point3(2,0,1),pp3)
		
		val p4 = Point3(0,1,2)
		val (d4,pp4) = t0.distanceFrom(p4)

		assertEquals(sqrt(2), d4, 0.001)
		assertEquals(Point3(1,0,2),pp4)
		
		// Test the distance from the face.
		// Test is done so that the distance is 1.
		
		val p5 = Point3(2,1,2)
		val (d5,pp5) = t0.distanceFrom(p5)
		
		assertEquals(1, d5, 0.001)
		assertEquals(Point3(2,0,2),pp5)

		println("OK")
	}
 }