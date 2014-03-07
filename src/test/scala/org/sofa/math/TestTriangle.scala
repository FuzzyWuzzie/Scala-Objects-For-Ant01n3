package org.sofa.math.test

import org.scalatest.{FlatSpec, Matchers}

import org.sofa.math.Triangle
import org.sofa.math.Point3
import scala.math._

class TestTriangle extends FlatSpec with Matchers {

	"A Triangle" should "allow to compute distance from it in 3D" in {
		val t0 = Triangle(Point3(1,0,1), Point3(1,0,3), Point3(4,0,1))

		// Test distance from the three vertices.
		// Test is done so that the distance equals sqrt(3).
		
		val p0 = Point3(0,1,0)
		val (d0,pp0) = t0.distanceFrom(p0)
		
		d0 should be (sqrt(3) +- 0.001)
		pp0 should be (Point3(1,0,1))
		
		val p1 = Point3(5,1,0)
		val (d1,pp1) = t0.distanceFrom(p1)
		
		d1 should be (sqrt(3) +- 0.001)
		pp1 should be (Point3(4,0,1))
		
		val p2 = Point3(0,1,4)
		val (d2,pp2) = t0.distanceFrom(p2)

		d2 should be (sqrt(3) +- 0.001)	
		pp2 should be (Point3(1,0,3))
		
		// Test the distance from the three edges.
		// Test is done so that the distance is sqrt(2).
		
		val p3 = Point3(2,1,0)
		val (d3,pp3) = t0.distanceFrom(p3)

		d3 should be (sqrt(2) +- 0.001)
		pp3 should be (Point3(2,0,1))		
		
		val p4 = Point3(0,1,2)
		val (d4,pp4) = t0.distanceFrom(p4)

		d4 should be (sqrt(2) +- 0.001)
		pp4 should be (Point3(1,0,2))
		
		// Test the distance from the face.
		// Test is done so that the distance is 1.
		
		val p5 = Point3(2,1,2)
		val (d5,pp5) = t0.distanceFrom(p5)

		d5 should be (1.0 +- 0.001)		
		pp5 should be (Point3(2,0,2))
	}
 }