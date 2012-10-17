package org.sofa.math

/**  Utility methods to deal with Bézier cubic curves. */
object CubicCurve {
	/** Evaluate a cubic Bézier curve according to control points `x0`, `x1`, `x2` and `x3` and 
	 * return the position at parametric position `t` of the curve.
	 * @return The coordinate at parametric position `t` on the curve. */
	def evalCubic(x0:Double, x1:Double, x2:Double, x3:Double, t:Double):Double = {
		val tt = (1f - t)
		
		x0 * (tt*tt*tt) + 3f * x1 * t * (tt*tt) + 3f * x2 * (t*t) * tt + x3 * (t*t*t)
	}

	/** Evaluate a cubic Bézier curve according to control points `p0`, `p1`, `p2` and `p3` and 
	 * return the position at parametric position `t` of the curve.
	 * @return The point at parametric position `t` on the curve. */
	def evalCubic(p0:Point2, p1:Point2, p2:Point2, p3:Point2, t:Double):Point2 = {
		Point2(evalCubic( p0.x, p1.x, p2.x, p3.x, t ),
		       evalCubic( p0.y, p1.y, p2.y, p3.y, t ))
	}
	
	/** Evaluate a cubic Bézier curve according to control points `p0`, `p1`, `p2` and `p3` and 
	 * return the position at parametric position `t` of the curve.
	 * @return The point at parametric position `t` on the curve. */
	def evalCubic(p0:Point3, p1:Point3, p2:Point3, p3:Point3, t:Double):Point3 = {
	    Point3(evalCubic(p0.x, p1.x, p2.x, p3.x, t),
	           evalCubic(p0.y, p1.y, p2.y, p3.y, t),
	           evalCubic(p0.z, p1.z, p2.z, p3.z, t))
	}

	/** Evaluate a cubic Bézier curve according to control points `p0`, `p1`, `p2` and `p3` and 
	 * store the position at parametric position `t` of the curve in `result`.
	 * @return the given reference to `result`. */
	def evalCubic(p0:Point2, p1:Point2, p2:Point2, p3:Point2, t:Double, result:Point2):Point2 = {
		result.x = evalCubic(p0.x, p1.x, p2.x, p3.x, t)
		result.y = evalCubic(p0.y, p1.y, p2.y, p3.y, t)
		result
	}
	
	/** Evaluate a cubic Bézier curve according to control points `p0`, `p1`, `p2` and `p3` and 
	 * store the position at parametric position `t` of the curve in `result`.
	 * @return the given reference to `result`. */
	def evalCubic(p0:Point3, p1:Point3, p2:Point3, p3:Point3, t:Double, result:Point3):Point3 = {
		result.x = evalCubic(p0.x, p1.x, p2.x, p3.x, t)
		result.y = evalCubic(p0.y, p1.y, p2.y, p3.y, t)
		result.z = evalCubic(p0.z, p1.z, p2.z, p3.z, t)
		result
	}
	
	/** Derivative of a cubic Bézier curve according to control points `x0`, `x1`, `x2` and `x3` 
	 * at parametric position `t` of the curve.
	 * @return The derivative at parametric position `t` on the curve. */
	def derivativeCubic(x0:Double, x1:Double, x2:Double, x3:Double, t:Double):Double = {
			//A = x3 - 3 * x2 + 3 * x1 - x0
			//B = 3 * x2 - 6 * x1 + 3 * x0
			//C = 3 * x1 - 3 * x0
			//D = x0
			//Vx = 3At2 + 2Bt + C 
		3 * ( x3 - 3 * x2 + 3 * x1 - x0 ) * t*t +
		  2 * ( 3 * x2 - 6 * x1 + 3 * x0 ) * t +
		    ( 3 * x1 - 3 * x0 )
	}
	
	/** Derivative point of a cubic Bézier curve according to control points `x0`, `x1`, `x2` and
	 * `x3` at parametric position `t` of the curve.
	 * @return The derivative point at parametric position `t` on the curve. */
	def derivativeCubic(p0:Point2, p1:Point2, p2:Point2, p3:Point2, t:Double):Point2 = {
		Point2(derivativeCubic(p0.x, p1.x, p2.x, p3.x, t),
		       derivativeCubic(p0.y, p1.y, p2.y, p3.y, t))
	}
	
	/** Derivative point of a cubic Bézier curve according to control points `x0`, `x1`, `x2` and
	 * `x3` at parametric position `t` of the curve.
	 * @return The derivative point at parametric position `t` on the curve. */
	def derivativeCubic(p0:Point3, p1:Point3, p2:Point3, p3:Point3, t:Double):Point3 = {
		Point3(derivativeCubic(p0.x, p1.x, p2.x, p3.x, t),
		       derivativeCubic(p0.y, p1.y, p2.y, p3.y, t),
		       derivativeCubic(p0.z, p1.z, p2.z, p3.z, t))
	}

	/** Store in `result` the derivative point of a cubic Bézier curve according to control points
	 * `x0`, `x1`, `x2` and `x3` at parametric position `t` of the curve.
	 * @return the given reference to `result`. */
	def derivativeCubic(p0:Point2, p1:Point2, p2:Point2, p3:Point3, t:Double, result:Point2):Point2 = {
		result.x = derivativeCubic(p0.x, p1.x, p2.x, p3.x, t)
		result.y = derivativeCubic(p0.y, p1.y, p2.y, p3.y, t)
		result
	}
	
	/** Store in `result` the derivative point of a cubic Bézier curve according to control points
	 * `x0`, `x1`, `x2` and `x3` at parametric position `t` of the curve.
	 * @return the given reference to `result`. */
	def derivativeCubic(p0:Point3, p1:Point3, p2:Point3, p3:Point3, t:Double, result:Point3):Point3 = {
		result.x = derivativeCubic(p0.x, p1.x, p2.x, p3.x, t)
		result.y = derivativeCubic(p0.y, p1.y, p2.y, p3.y, t)
		result.z = derivativeCubic(p0.z, p1.z, p2.z, p3.z, t)
		result
	}

	/** The perpendicular vector to the curve defined by control points `p0`, `p1`, `p2` and `p3`
	 * at parametric position `t`.
	 * @return A vector perpendicular to the curve at position `t`. */
	def perpendicularCubic(p0:Point2, p1:Point2, p2:Point2, p3:Point2, t:Double):Vector2 = {
		Vector2(derivativeCubic(p0.y, p1.y, p2.y, p3.y, t), -derivativeCubic(p0.x, p1.x, p2.x, p3.x, t))
	}

	/** Store in `result` the perpendicular vector to the curve defined by control points `p0`,
	 * `p1`, `p2` and `p3`  at parametric position `t`.
	 * @return the given reference to `result`. */
	def perpendicularCubic(p0:Point2, p1:Point2, p2:Point2, p3:Point2, t:Double, result:Vector2):Vector2 = {
		result.x =  derivativeCubic(p0.y, p1.y, p2.y, p3.y, t)
		result.y = -derivativeCubic(p0.x, p1.x, p2.x, p3.x, t)
		result
	}
	
	/** Evaluate the length of a B�zier curve by taking n points on the curve and summing the lengths of
	 * the n+1 segments thus defined. */
	def approxLengthOfCurve(p0:Point3, p1:Point3, p2:Point3, p3:Point3):Double = {
		val inc = 0.1
		var i   = inc
		var len = 0.0
		var from = p0
		var to   = Point3(0, 0, 0)
		
		while( i < 1f ) {
			to    = evalCubic(p0, p1, p2, p3, i)
			i    += inc
			len  += from.distance(to)
			from  = to
		}
		
		len += from.distance(p3)
		
		len
	}
}