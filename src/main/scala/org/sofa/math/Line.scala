package org.sofa.math 


/** Line companion object. */
object Line {
	/** New line from a point `p` and a vector `dir`. */
	def apply(p:Point3, dir:Vector3):Line =  new Line(p, dir)

	/** New line from a point (`px`, `py`, `pz`) and a vector (`dx`, `dy`, `dz`). */
	def apply(px:Double, py:Double, pz:Double, dx:Double, dy:Double, dz:Double):Line = new Line(Point3(px,py,pz), new Vector3(dx, dy, dz))
}


/** A line defined by a point and a direction vector. */
class Line(val p:Point3, val dir:Vector3) {

	/** Compute the intersection of two lines in 3D, returns `None` if 
	  * the two lines do not intersect. Else returns and `Option[Point3]` of
	  * the intersection point. Globally if you two lines are coplanar and
	  * not parallel, they intersect. */
	def intersection(other:Line):Option[Point3] = {
		// see http://stackoverflow.com/questions/2316490/the-algorithm-to-find-the-point-of-intersection-of-two-3d-line-segment

		val da = dir
		val db = other.dir
		val dc = Vector3(p, other.p)
		val daXdb = da X db

		if(dc.dot(daXdb) == 0) {
			val s = (dc X db).dot(daXdb) / daXdb.norm2
			
			if(s >= 0.0 && s <= 1.0) {
				val ip = (p + da) * s
				Some(ip)
			} else {
				None
			}
		} else {
			None
		}
	}
}