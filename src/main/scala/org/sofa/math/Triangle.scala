package org.sofa.math

import scala.math._


object Triangle {
	def apply(p0:Point3, p1:Point3, p2:Point3):Triangle = new VarTriangle(p0, p1, p2)
	def unapply(t:Triangle):(Point3,Point3,Point3) = (Point3(t.p0),Point3(t.p1),Point3(t.p2))
}


object VarTriangle {
	def apply(p0:Point3, p1:Point3, p2:Point3):VarTriangle = new VarTriangle(p0, p1, p2)
	def unapply(t:VarTriangle):(Point3,Point3,Point3) = (Point3(t.p0),Point3(t.p1),Point3(t.p2))
}


object ConstTriangle {
	def apply(p0:Point3, p1:Point3, p2:Point3):ConstTriangle = new ConstTriangle(p0, p1, p2)
	def unapply(t:ConstTriangle):(Point3,Point3,Point3) = (Point3(t.p0),Point3(t.p1),Point3(t.p2))	
}


/** A simple triangle trait containing or referencing three points in 3 dimensions.
  *
  * The two implementations given [[VarTriangle]] and [[ConstTriangle]] contain the
  * points. Other implementations could contain only point indices inside a larger
  * mesh.
  *
  * This class provides various utilities to handle triangles and geometry on
  * on triangles :
  *   - compute a normal form,
  *   - compute a normal,
  *   - compute a distance of a point from the triangle, with the calculus of
  *     the nearest point on the triangle surface.
  *   - compute the center and radius of a circumcircle (a cricle that pass by
  *     the three points of the triangle).
  */
trait Triangle {
	def p0:Point3
	def p1:Point3
	def p2:Point3

	/** Computes the normal form of a triangle, that is a base point,
	  * and two vectors to locate the two other points from the base point. */
	def normalForm():(Point3,Vector3,Vector3) = { (Point3(p0),Vector3(p0, p1),Vector3(p0, p2)) }

	/** Compute the triangle normal. */
	def normal():Vector3 = {
		val v0 = Vector3(p0, p1)
		val v1 = Vector3(p0, p2)
			
		val normal = v1 X v0
		normal.normalize
		normal
	}

	/** Plane of the triangle. */
	def plane():Plane = Plane(p0, normal)

	/** Circumcircle of the triangle using only the absissa and ordinates (x and y) of points. */
// TODO NOT TESTED
	def circumcircleXY():Circle = {
		var d = 2 * ((p0.x * (p1.y - p2.y)) + (p1.x * (p2.y - p0.y)) + (p2.x * (p0.y - p1.y)))
		var x = 0.0
		var y = 0.0
		if(d != 0) {
			x = ((p0.x*p0.x + p0.y*p0.y)*(p1.y - p2.y) + (p1.x*p1.x + p1.y*p1.y)*(p2.y - p0.y) + (p2.x*p2.x + p2.y*p2.y)*(p0.y - p1.y)) / d
			y = ((p0.x*p0.x + p0.y*p0.y)*(p2.x - p1.x) + (p1.x*p1.x + p1.y*p1.y)*(p0.x - p2.x) + (p2.x*p2.x + p2.y*p2.y)*(p1.x - p0.x)) / d
		}
		val r = sqrt((p0.x-x)*(p0.x-x) + (p0.y-y)*(p0.y-y))
		
		Circle(Point2(x, y), r)
	}


	/** Circumcircle of the triangle using only the absissa and depth (x and z) of points. */
// TODO NOT TESTED
	def circumcircleXZ():Circle = {
		var d = 2 * ((p0.x * (p1.z - p2.z)) + (p1.x * (p2.z - p0.z)) + (p2.x * (p0.z - p1.z)))
		var x = 0.0
		var y = 0.0
		if(d != 0) {
			x = ((p0.x*p0.x + p0.z*p0.z)*(p1.z - p2.z) + (p1.x*p1.x + p1.z*p1.z)*(p2.z - p0.z) + (p2.x*p2.x + p2.z*p2.z)*(p0.z - p1.z)) / d
			y = ((p0.x*p0.x + p0.z*p0.z)*(p2.x - p1.x) + (p1.x*p1.x + p1.z*p1.z)*(p0.x - p2.x) + (p2.x*p2.x + p2.z*p2.z)*(p1.x - p0.x)) / d
		}
		val r = sqrt((p0.x-x)*(p0.x-x) + (p0.z-y)*(p0.z-y))
		
		Circle(Point2(x, y), r)
	}

	/** Sphere deduced of the circumcircle of the triangle considering the points in 3D. */
// TODO NOT TESTED
	def circumsphere():Sphere = {
		val (p0,v0,v1) = normalForm

		// Perpendicular bisectors
		val pb0 = Line(p0 + (v0*0.5), v0 X normal)
		val pb1 = Line(p0 + (v1*0.5), v1 X normal)

		// Intersection point
		val ip = pb0.intersection(pb1) match {
			case Some(p) => p
			case None => throw new RuntimeException("WTF ?")
		}

		// Radius
		val r = p0.distance(ip)

		Sphere(ip, r)
	}

	def edge0:Edge = VarEdge(p0, p1)
	def edge1:Edge = VarEdge(p1, p2)
	def edge2:Edge = VarEdge(p2, p0)

	/** Return `p` if it is shared with the `other` triangle, else null.
	  * The `butNot` argument allows to return `p` only if it is not `butNot`. */
	def commonPoint(other:Triangle, p:Point3, butNot:Point3 = null):Point3 = {
		if((butNot eq null) || (butNot != p)) {
			if(p == other.p0 || p == other.p1 || p == other.p2) p else null
		} else {
			null
		}
	}

	/** The shared edge between this and the `other` triangle or null if no edge is shared. */
	def sharedEdge(other:Triangle):Edge = {
		var sp0 = commonPoint(other, p0)
		if(sp0 eq null) sp0 = commonPoint(other, p1)
		if(sp0 eq null) sp0 = commonPoint(other, p2)
		if(sp0 ne null) {
			var sp1 = commonPoint(other, p0, sp0)
			if(sp1 eq null) sp1 = commonPoint(other, p1, sp0)
			if(sp1 eq null) sp1 = commonPoint(other, p2, sp0)
			if(sp1 ne null) {
				VarEdge(sp0, sp1)
			} else {
				null
			}
		} else {
			null
		}
	}

	/** True if the given `edge` is shared with the `other` triangle. */
	def isShared(edge:Edge, other:Triangle):Boolean = {
		var sp0 = commonPoint(other, edge.p0)
		var sp1 = if(sp0 ne null) commonPoint(other, edge.p1, sp0) else null
		((sp0 ne null) && (sp1 ne null))
	} 
	
	/** Compute the distance from the given point `p` to this triangle.
	  * Return the distance, and a point on the triangle where the distance
	  * is minimum to `p`.
	  * Based on David Eberly algorithm (http:\\www.geometrictools.com/Documentation/DistancePoint3Triangle3.pdf)
	  * and Gwendolyn Fisher mathlab implementation. */
	def distanceFrom(pp:Point3):(Double,Point3) = {
		val (bb,e0,e1) = normalForm()
		var dist = 0.0
		//var pp0:Point3 = null
		
		val dd = Vector3(pp, bb)
		val a = e0 dot e0
		val b = e0 dot e1
		val c = e1 dot e1
		val d = e0 dot dd
		val e = e1 dot dd
		val f = dd dot dd
		
		val det = a*c - b*b
		var s   = b*e - c*d
		var t   = b*d - a*e
		
		var sqrDistance = 0.0

		if((s+t) <= det) {
			if(s < 0) {
				if(t < 0) {
					// region4
					if(d < 0)  {
						t = 0
						if (-d >= a) {
							s = 1
							sqrDistance = a + 2*d + f
						} else {
							s = -d/a
							sqrDistance = d*s + f
						}
					} else {
						s = 0
						if (e >= 0) {
							t = 0
							sqrDistance = f
						} else {
							if (-e >= c) {
								t = 1
								sqrDistance = c + 2*e + f
							} else {
								t = -e/c
								sqrDistance = e*t + f
							}
						}
					} // of region 4
				} else {
					// region 3
					s = 0
					if(e >= 0) {
						t = 0
						sqrDistance = f
					} else {
						if(-e >= c) {
							t = 1
							sqrDistance = c + 2*e +f
						} else {
							t = -e/c
							sqrDistance = e*t + f
						}
					}
				} // of region 3 
			} else {
				if(t < 0) {
					// region 5
					t = 0
					if(d >= 0) {
						s = 0
						sqrDistance = f
					} else{
						if(-d >= a) {
							s = 1
							sqrDistance = a + 2*d + f // GF 20101013 fixed typo d*s ->2*d
						} else {
							s = -d/a
							sqrDistance = d*s + f
						}
					}
				} else {
					// region 0
					val invDet = 1/det
					s = s*invDet
					t = t*invDet
					sqrDistance = s*(a*s + b*t + 2*d) + t*(b*s + c*t + 2*e) + f
				} 
			}
		} else {
			if(s < 0) {
				// region 2
				val tmp0 = b + d
				val tmp1 = c + e
				if(tmp1 > tmp0) { // minimum on edge s+t=1
					val numer = tmp1 - tmp0;
					val denom = a - 2*b + c
					if(numer >= denom) {
						s = 1;
						t = 0;
						sqrDistance = a + 2*d + f // GF 20101014 fixed typo 2*b -> 2*d
					} else {
						s = numer/denom
						t = 1-s;
						sqrDistance = s*(a*s + b*t + 2*d) + t*(b*s + c*t + 2*e) + f
					} 
				} else {          // minimum on edge s=0
					s = 0
					if(tmp1 <= 0) {
						t = 1
						sqrDistance = c + 2*e + f
					}else{
						if(e >= 0){
							t = 0
							sqrDistance = f
						}else{
							t = -e/c
							sqrDistance = e*t + f
						}
					}
				} // of region 2
			} else {
				if(t < 0) {
					//region6 
					val tmp0 = b + e
					val tmp1 = a + d
					if (tmp1 > tmp0) {
						val numer = tmp1 - tmp0
						val denom = a-2*b+c
						if (numer >= denom) {
							t = 1
							s = 0
							sqrDistance = c + 2*e + f
						} else {
							t = numer/denom
							s = 1 - t
							sqrDistance = s*(a*s + b*t + 2*d) + t*(b*s + c*t + 2*e) + f
						}
					} else {  
						t = 0;
						if (tmp1 <= 0) {
							s = 1
							sqrDistance = a + 2*d + f
						} else {
							if (d >= 0) {
								s = 0
								sqrDistance = f
							} else {
								s = -d/a
								sqrDistance = d*s + f
							}
						}
					}
					//end region 6
				} else {
					// region 1
					val numer = c + e - b - d
					if(numer <= 0) {
						s = 0
						t = 1
						sqrDistance = c + 2*e + f
					}else {
						val denom = a - 2*b + c
						if(numer >= denom) {
							s = 1
							t = 0
							sqrDistance = a + 2*d + f
						} else {
							s = numer/denom
							t = 1-s
							sqrDistance = s*(a*s + b*t + 2*d) + t*(b*s + c*t + 2*e) + f
						}
					} // of region 1
				}
			}
		}

		// account for numerical round-off error
		if (sqrDistance < 0) {
			sqrDistance = 0
		}

		dist = math.sqrt(sqrDistance)

		// pp0 = (bb + e0*s + e1*t).asInstanceOf[Point3]
		// Optimization to avoid two point3 creation.
		var pp0 = Point3(bb)

		e0  *= s
		e1  *= t
		pp0 += e0
		pp0 += e1

		(dist,pp0)
	}	
}


/** A triangle class that can be moved.
  *
  * This class is made for triangles whose points can be changed at any time. If
  * the triangle is not to be modified, you can improve performance by using the
  * [[ConstTriangle]] class that will pre-compute the normal and the normal form, and
  * avoid calculus during collision and distance tests. */
class VarTriangle(val p0:Point3, val p1:Point3, val p2:Point3) extends Triangle {
}


/** A triangle that will not be moved.
  * 
  * The use of such a triangle allows to ensure we can compute the normal to the triangle and the
  * normal form of the triangle once and forall. Then the computation of distanceFrom is far more
  * efficient. As such triangles are often used in collision tests, this can greatly improve things.
  * Edges are not memorized. */
class ConstTriangle(val p0:Point3, val p1:Point3, val p2:Point3) extends Triangle {

	/** Vector between point 0 and 1. */
	val v0 = Vector3(p0, p1)

	/** Vector between point 0 and 2. */
	val v1 = Vector3(p0, p2)

	/** The normal to the triangle face. */
	protected[this] val v2 = (v1 X v0)

	v2.normalize

	/** The normal form of a triangle, that is a base point,
	  * and two vectors to locate the two other points from the base point. */
	override def normalForm():(Point3,Vector3,Vector3) = { (Point3(p0),Vector3(v0),Vector3(v1)) }

	/** Triangle normal. */
	override def normal():Vector3 = v2

	override def edge0:Edge = ConstEdge(p0, p1)
	override def edge1:Edge = ConstEdge(p1, p2)
	override def edge2:Edge = ConstEdge(p2, p0)
}


/** A triangle class where points pertain to a pool of shared points. */
case class IndexedTriangle(i0:Int, i1:Int, i2:Int, points:IndexedSeq[Point3]) extends Triangle {
	def p0:Point3 = points(i0)
	def p1:Point3 = points(i1)
	def p2:Point3 = points(i2)
	override def edge0:Edge = IndexedEdge(i0, i1, points)
	override def edge1:Edge = IndexedEdge(i1, i2, points)
	override def edge2:Edge = IndexedEdge(i2, i0, points)

	def commonPointi(other:Triangle, p:Int, butNot:Int = -1):Int = other match {
		case that:IndexedTriangle => {
			if((butNot < 0) || (butNot != p)) {
				if(p == that.i0 || p == that.i1 || p == that.i2) p else -1
			} else {
				-1
			}
		} 
		case _ => -1
	}

	override def sharedEdge(other:Triangle):Edge = {
		var sp0 = commonPointi(other, i0)
		if(sp0 < 0) sp0 = commonPointi(other, i1)
		if(sp0 < 0) sp0 = commonPointi(other, i2)
		if(sp0 >= 0) {
			var sp1 = commonPointi(other, i0, sp0)
			if(sp1 < 0) sp1 = commonPointi(other, i1, sp0)
			if(sp1 < 0) sp1 = commonPointi(other, i2, sp0)
			if(sp1 >= 0) {
				IndexedEdge(sp0, sp1, points)
			} else {
				null
			}
		} else {
			null
		}
	}

	override def isShared(edge:Edge, other:Triangle):Boolean = edge match {
		case that:IndexedEdge => {
			var sp0 = commonPointi(other, that.i0)
			var sp1 = if(sp0 >= 0) commonPointi(other, that.i1, sp0) else -1
			(sp0 >= 0 && sp1 >= 0)
		} 
		case _ => false
	}

	override def toString():String = "ITri[%s -> %s -> %s]".format(points(i0), points(i1), points(i2))
}
