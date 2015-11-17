package org.sofa.math


object Sphere {
	def apply(center:Point3, radius:Double):Sphere = VarSphere(center, radius)
}


/** A basic sphere definition. */
trait Sphere {
	/** Center of the circle. */
	def center:Point3

	/** Radius of the circle. */
	def radius:Double

	/** Diameter of the cirlce. */
	def diameter:Double = radius * 2

	/** Is point `p` inside the circle ? */
	def inside(p:Point3):Boolean = p.distance(center) <= radius
}


case class VarSphere(center:Point3, radius:Double) extends Sphere {
}


case class ConstSphere(center:Point3, radius:Double) extends Sphere {
	private val d = radius * 2

	override def diameter = d
}