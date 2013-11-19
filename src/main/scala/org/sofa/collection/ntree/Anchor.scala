package org.sofa.collection.ntree


import org.sofa.math.ConstantNumberSeq3
import org.sofa.math.Point3


/** A constant point.
  * 
  * An anchor is guaranteed to remain at the same place, and therefore can be
  * passed between threads since once created, it is read-only.
  *
  * @author Antoine Dutot
  * @since 2007
  */
class Anchor(x:Double, y:Double, z:Double) extends Point3(x, y, z) with ConstantNumberSeq3 {}


/** Anchor class companion object. */
object Anchor {
	def apply(x:Double, y:Double, z:Double):Anchor = new Anchor(x, y, z)
}