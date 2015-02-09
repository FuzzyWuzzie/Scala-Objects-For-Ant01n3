package org.sofa.math 


/** Line companion object. */
object Line {
	/** New line from a point `p` and a vector `dir`. */
	def apply(p:Point3, dir:Vector3):Line =  new Line(p, dir)

	/** New line from a point (`px`, `py`, `pz`) and a vector (`dx`, `dy`, `dz`). */
	def apply(px:Double, py:Double, pz:Double, dx:Double, dy:Double, dz:Double):Line = new Line(Point3(px,py,pz), new Vector3(dx, dy, dz))
}


/** A line defined by a point and a direction vector. */
class Line(val p:Point3, val dir:Vector3) {}