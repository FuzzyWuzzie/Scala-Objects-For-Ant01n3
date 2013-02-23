package org.sofa.math

object AxisRange { def apply(r:(Double,Double)):AxisRange = AxisRange(r._1, r._2) }

/** Range along an axis, the from part is smaller thant the to part.
  * Usually the from part is negative and the to part is positive. */	
case class AxisRange(from:Double, to:Double) {
	assert(from <= to)

	/** Center on the axis according to the range. */
	def origin:Double = ((to - from) / 2.0)
}

object Axes { def apply(x:(Double,Double), y:(Double,Double), z:(Double,Double)):Axes = Axes(AxisRange(x), AxisRange(y), AxisRange(z)) }

/** Represent an immutable cubic/rectangular area in space delimited along
  * each of the x, y, and z axes by ranges of values. */
case class Axes(x:AxisRange, y:AxisRange, z:AxisRange) {

	/** Origin point, center of the delimited space. */
	val origin = Point3(x.origin, y.origin, z.origin)
}