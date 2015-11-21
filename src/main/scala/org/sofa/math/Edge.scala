package org.sofa.math

import scala.math._


/** A segment of line between two points. */
trait Edge {
	/** Start point. */
	def p0:Point3
	/** End point. */
	def p1:Point3
}


/** Edge whose points can be changed. */
class VarEdge(var p0:Point3, var p1:Point3) extends Edge {}
object VarEdge { def apply(p0:Point3, p1:Point3):VarEdge = new VarEdge(p0, p1) }

/** Edge whose points cannot be modified. */
case class ConstEdge(p0:Point3, p1:Point3) extends Edge {}

/** Edge defined whose point can be changed, and are identified by indices in a sequence of points. */
case class IndexedEdge(i0:Int, i1:Int, points:IndexedSeq[Point3]) extends Edge {
	def p0 = points(i0)
	def p1 = points(i1)
}
