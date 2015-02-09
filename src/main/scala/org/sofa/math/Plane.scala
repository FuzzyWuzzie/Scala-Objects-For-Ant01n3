package org.sofa.math


/** Plane companion object. */
object Plane {
	/** Intersection of a line with a plane. The line is described by a point `linePt` and 
	  * a vector `lineDir`. The plane is described by a point `planePt` and a normal `planeNrm`.
	  * Returns a point at the position of intersection or null if the line does not intersect
	  * the plane or is on the plane. */
	def intersect(linePt:Point3, lineDir:Vector3, planePt:Point3, planeNrm:Vector3):Point3 = {
		//      (planePt - linePt) . planeNrm
		// d = --------------------------------
		//          lineDir . planeNrm
		//
		// res = d * lineDir + linePt

		val dotLN = lineDir ** planeNrm	// dotprod

		if(dotLN != 0) {
			val PL    = Vector3(planePt - linePt)
			val dotPN = PL ** planeNrm 	// dotprod
			val d     = dotPN / dotLN
			
			linePt + (lineDir * d)
		} else {
			null
		}
	}	

	/** New plane from a point `p` and a normal `n`. */
	def apply(p:Point3, n:Vector3):Plane = new Plane(p, n)

	/** New plane from a point (`px`, `py`, `pz`) and a normal (`nx`, `ny`, `nz`). */
	def apply(px:Double, py:Double, pz:Double, nx:Double, ny:Double, nz:Double):Plane = new Plane(Point3(px,py,pz), Vector3(nx,ny,nz))
}


/** A plane defined by a point and a normal. */
class Plane(val p:Point3, val n:Vector3) {
	/** Intersection of a given `line` with this plane. Returns a point at
	  * the position of intersection, or null if the line does not intersect the
	  * plane or is on it. */
	def intersect(l:Line):Point3 = Plane.intersect(l.p, l.dir, p, n)
}