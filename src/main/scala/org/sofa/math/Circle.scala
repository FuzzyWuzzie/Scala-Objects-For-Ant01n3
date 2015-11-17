package org.sofa.math


object Circle {
	def apply(center:Point2, radius:Double):Circle = VarCircle(center, radius)
}


/** A basic 2D circle definition. */
trait Circle {
	/** Center of the circle. */
	def center:Point2

	/** Radius of the circle. */
	def radius:Double

	/** Diameter of the cirlce. */
	def diameter:Double = radius * 2

	/** Is point `p` inside the circle ? */
	def isInside(p:Point2):Boolean = {
		//p.distance(center) <= radius
		// More efficient:
		val xx = center.x-p.x
		val yy = center.x-p.y
		(xx*xx+yy*yy) <= radius*radius
	}

	/** Is point `p` inside the circle ? */
	def isInside(x:Double, y:Double):Boolean = {
		//Point2(x,y).distance(center) <= radius
		// More efficient:
		val xx = center.x-x
		val yy = center.y-y
		(xx*xx+yy*yy) <= radius*radius
	}

	override def toString():String = "Circle[%s, radius %f]".format(center, radius)
}


case class VarCircle(center:Point2, radius:Double) extends Circle {
}


case class ConstCircle(center:Point2, radius:Double) extends Circle {
	private val d = radius * 2

	override def diameter = d
}