package org.sofa.math

import scala.language.implicitConversions
import scala.util.Random
import scala.math._


object Point2 {
    implicit def point2ToTuple(v:Point2):(Double, Double) = (v.x, v.y)

    def apply(x:Double, y:Double) = new Point2(x, y)
  
    def apply() = new Point2()
  
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new Point2()
        else if(other.size < 2)
             new Point2(other.data(0), 0)
        else new Point2(other.data(0), other.data(1))
    }
  
    def apply(xy:(Double, Double)) = new Point2(xy._1, xy._2)

    /** A random point with coordinates within [0..1[. */
    def random():Point2 = random(0, 1)

    /** A random point with coordinates within [min..max[. */
    def random(min:Double, max:Double):Point2 = random(min, max, min, max)

    /** A random point with absissa coordinates within [minx..maxx[ and
      * ordinates within [miny..maxy[. */
    def random(minx:Double, maxx:Double, miny:Double, maxy:Double):Point2 =
    	Point2(Random.nextDouble*(maxx-minx)-minx, Random.nextDouble*(maxy-miny)-miny)

    /** A point whose components are the minimum of the corresponding components in the given sequence of points. */
    def min(pp:Point2 *):Point2 = {
    	val p = new Point2(Double.PositiveInfinity,Double.PositiveInfinity)
    	var i = 0
    	val n = p.size 
    	while(i < n) {
    		if(pp(i).x < p.x) p.x = pp(i).x
    		if(pp(i).y < p.y) p.y = pp(i).y
    		i += 1
    	}
    	p
    }

    /** A point whose components are the maximum of the corresponding components in the given sequence of points. */
    def max(pp:Point2 *):Point2 = {
    	val p = new Point2(Double.NegativeInfinity,Double.NegativeInfinity)
    	var i = 0
    	val n = p.size 
    	while(i < n) {
    		if(pp(i).x > p.x) p.x = pp(i).x
    		if(pp(i).y > p.y) p.y = pp(i).y
    		i += 1
    	}
    	p
    }
}


class Point2(xInit:Double, yInit:Double) extends NumberSeq2 {

    type ReturnType = Point2
    
    protected[math] final val data = Array[Double](xInit, yInit)
    
    def this(other:Point2) = this(other.x, other.y)
    
    def this() = this(0, 0)
    
    def newInstance = new Point2
    
    override final def size:Int = 2

    /** Create a new point linear interpolation of this and `other`.
      * 
	  * The new point is located between this and `other` if
	  * `factor` is between 0 and 1 (0 yields this point, 1 yields
	  * the `other` point). 
	  */
	def interpolate(other:Point2, factor:Double):Point2 = {
	    val result = newInstance.asInstanceOf[Point2]
		result.data(0) = data(0) + ((other.data(0) - data(0)) * factor);
		result.data(1) = data(1) + ((other.data(1) - data(1)) * factor);
		result
	}

    /** Distance between this and `other`. */
    def distance(other:NumberSeq):Double = {
        if(other.data.length > 1) {
            val xx = other.data(0) - data(0)
            val yy = other.data(1) - data(1)
            sqrt(xx*xx + yy*yy)
        } else {
            0
        }
    }

	/** Distance between this and `other`. */
	def distance(other:Point2):Double = {
		val xx = other.data(0) - data(0)
		val yy = other.data(1) - data(1)
		abs(sqrt((xx * xx) + (yy * yy)))
	}
	
	/** Distance between this and point (`x`,`y`,`z`). */
	def distance(x:Double, y:Double, z:Double):Double = {
		val xx = x - data(0)
		val yy = y - data(1)
		abs(sqrt((xx * xx) + (yy * yy)))
	}

    /** Vector between this and an `other` point. 
      *
      * The direction goes from this to `other`. The length of the vector is
      * the distance between the two points.
      */
    def -->(other:Point2):Vector2 = new Vector2(other.x-x, other.y-y)

    override def canEqual(a:Any) = a.isInstanceOf[Point2]

  	override def equals(that:Any):Boolean = that match {
		case that:Point2 => this.x == that.x && this.y == that.y && that.hashCode == this.hashCode
		case _ => false
	}

	override def hashCode:Int = (x*73856093).toInt^(y*19349663).toInt
}


//===================================================


object Point3 {
    implicit def point3ToTuple(v:Point3):(Double, Double, Double) = (v.x, v.y, v.z)

    def apply(x:Double, y:Double, z:Double) = new Point3(x, y, z)
    
    def apply() = new Point3()
    
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new Point3()
        else if(other.size < 2)
             new Point3(other.data(0), 0, 0)
        else if(other.size < 3)
             new Point3(other.data(0), other.data(1), 0)
        else new Point3(other.data(0), other.data(1), other.data(2))
    }

    def apply(other:NumberSeq2, z:Double) = new Point3(other.data(0), other.data(1), z)
    
    def apply(xyz:(Double, Double, Double)) = new Point3(xyz._1, xyz._2, xyz._3)
    
    def apply(xy:(Double, Double), z:Double) = new Point3(xy._1, xy._2, z)
    
    def apply(x:Double, yz:(Double, Double)) = new Point3(x, yz._1, yz._2)

     /** A random point with coordinates within [0..1[. */
   def random():Point3 = random(0, 1)

   /** A random point with coordinates within [min..max[. */
     def random(min:Double, max:Double):Point3 = random(min, max, min, max, min, max)

    /** A random point with absissa coordinates within [minx..maxx[,
      * ordinates within [miny..maxy[ and depth within [minz..maxz[. */
    def random(minx:Double, maxx:Double, miny:Double, maxy:Double, minz:Double, maxz:Double):Point3 =
    	Point3(Random.nextDouble*(maxx-minx)-minx,
    		   Random.nextDouble*(maxy-miny)-miny,
    		   Random.nextDouble*(maxz-minz)-minz)

    /** A point whose components are the minimum of the corresponding components in the given sequence of points. */
    def min(pp:Point3*):Point3 = {
    	val p = new Point3(Double.PositiveInfinity,Double.PositiveInfinity,Double.PositiveInfinity)
    	var i = 0
    	val n = p.size 
    	while(i < n) {
    		if(pp(i).x < p.x) p.x = pp(i).x
    		if(pp(i).y < p.y) p.y = pp(i).y
    		if(pp(i).z < p.z) p.z = pp(i).z
    		i += 1
    	}
    	p
    }

    /** A point whose components are the maximum of the corresponding components in the given sequence of points. */
    def max(pp:Point3*):Point3 = {
    	val p = new Point3(Double.NegativeInfinity,Double.NegativeInfinity,Double.NegativeInfinity)
    	var i = 0
    	val n = p.size 
    	while(i < n) {
    		if(pp(i).x > p.x) p.x = pp(i).x
    		if(pp(i).y > p.y) p.y = pp(i).y
    		if(pp(i).z > p.z) p.z = pp(i).z
    		i += 1
    	}
    	p
    }
}


class Point3(xInit:Double, yInit:Double, zInit:Double) extends NumberSeq3 {

    type ReturnType = Point3
    
    protected[math] final val data = Array[Double](xInit, yInit, zInit)

    def this(other:Point3) = this(other.x, other.y, other.z)

    def this() = this(0, 0, 0)

    def newInstance = new Point3

    override final def size:Int = 3

    /** Create a new point linear interpolation of this and `other`.
      * 
	  * The new point is located between this and `other` if
	  * `factor` is between 0 and 1 (0 yields this point, 1 yields
	  * the `other` point). 
	  */
	def interpolate(other:Point3, factor:Double):Point3 = {
	    val result = newInstance.asInstanceOf[Point3]
		result.data(0) = data(0) + ((other.data(0) - data(0)) * factor);
		result.data(1) = data(1) + ((other.data(1) - data(1)) * factor);
		result.data(2) = data(2) + ((other.data(2) - data(2)) * factor);
		result
	}

    /** Distance between this and `other`. */
    def distance(other:NumberSeq):Double = {
        if(other.data.length > 2) {
            val xx = other.data(0) - data(0)
            val yy = other.data(1) - data(1)
            val zz = other.data(2) - data(2)
            sqrt(xx*xx + yy*yy + zz*zz)
        } else {
            0
        }
    }

	/** Distance between this and `other`. */
	def distance(other:Point3):Double = {
		val xx = other.data(0) - data(0)
		val yy = other.data(1) - data(1)
		val zz = other.data(2) - data(2)
		abs(sqrt((xx * xx) + (yy * yy) + (zz *zz)))
	}
	
	/** Distance between this and point (`x`,`y`,`z`). */
	def distance(x:Double, y:Double, z:Double):Double = {
		val xx = x - data(0)
		val yy = y - data(1)
		val zz = z - data(2)
		abs(sqrt((xx * xx) + (yy * yy) + (zz * zz)))
	}

    def +(other:Point3):ReturnType = (new Point3(data(0), data(1), data(2))).addBy(other)   // Faster than using apply
    
    override def +(value:Double):ReturnType = (new Point3(data(0), data(1), data(2))).addBy(value)   // Faster than using apply

    def -(other:Point3):ReturnType = (new Point3(data(0), data(1), data(2))).subBy(other)   // Faster than using apply
    
    override def -(value:Double):ReturnType = (new Point3(data(0), data(1), data(2))).subBy(value)   // Faster than using apply

    def *(other:Point3):ReturnType = (new Point3(data(0), data(1), data(2))).multBy(other)   // Faster than using apply
    
    override def *(value:Double):ReturnType = (new Point3(data(0), data(1), data(2))).multBy(value)   // Faster than using apply

    def /(other:Point3):ReturnType = (new Point3(data(0), data(1), data(2))).divBy(other)   // Faster than using apply
    
    override def /(value:Double):ReturnType = (new Point3(data(0), data(1), data(2))).divBy(value)   // Faster than using apply

    /** Vector between this and an `other` point. 
      *
      * The direction goes from this to `other`. The length of the vector is
      * the distance between the two points.
      */
    def -->(other:Point3):Vector3 = new Vector3(other.x-x, other.y-y, other.z-z)

    override def canEqual(a:Any) = a.isInstanceOf[Point3]

  	override def equals(that:Any):Boolean = that match {
		case that:Point3 => this.x == that.x && this.y == that.y && this.z == that.z && that.hashCode == this.hashCode
		case _ => false
	}

	override def hashCode:Int = (x*73856093).toInt^(y*19349663).toInt^(z*83492791).toInt
}


//===================================================


object Point4 {
    implicit def point4ToTuple(v:Point4):(Double, Double, Double, Double) = (v.x, v.y, v.z, v.w)

    def apply(x:Double, y:Double, z:Double, w:Double) = new Point4(x, y, z, w)
    def apply() = new Point4()
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new Point4()
        else if(other.size < 2)
             new Point4(other.data(0), 0, 0, 0)
        else if(other.size < 3)
             new Point4(other.data(0), other.data(1), 0, 0)
        else if(other.size < 4)
             new Point4(other.data(0), other.data(1), other.data(2), 0)
        else new Point4(other.data(0), other.data(1), other.data(2), other.data(3))
    }

    def apply(other:NumberSeq2, z:Double, w:Double) = new Point4(other.data(0), other.data(1), z, w)
    def apply(other:NumberSeq3, w:Double) = new Point4(other.data(0), other.data(1), other.data(2), w)

    def apply(xyzw:(Double, Double, Double, Double))    = new Point4(xyzw._1, xyzw._2, xyzw._3, xyzw._4)
    def apply(xyz:(Double, Double, Double), w:Double)   = new Point4(xyz._1, xyz._2, xyz._3, w)
    def apply(x:Double, yzw:(Double, Double, Double))   = new Point4(x, yzw._1, yzw._2, yzw._3)
    def apply(xy:(Double, Double), zw:(Double, Double)) = new Point4(xy._1, xy._2, zw._1, zw._2)
    def apply(xy:(Double,Double), z:Double, w:Double)   = new Point4(xy._1, xy._2, z, w)
    def apply(x:Double, yz:(Double,Double), w:Double)   = new Point4(x, yz._1, yz._2, w)
    def apply(x:Double, y:Double, zw:(Double,Double))   = new Point4(x, y, zw._1, zw._2)
}


class Point4(xInit:Double, yInit:Double, zInit:Double, wInit:Double) extends NumberSeq4 {

    type ReturnType = Point4
    
    protected[math] final val data = Array[Double](xInit, yInit, zInit, wInit)

    def this(other:Point4) = this(other.x, other.y, other.z, other.w)

    def this() = this(0, 0, 0, 0)

    def newInstance = new Point4

    override final def size:Int = 4

    /** Create a new point linear interpolation of this and `other`.
      * 
	  * The new point is located between this and `other` if
	  * `factor` is between 0 and 1 (0 yields this point, 1 yields
	  * the `other` point). 
	  */
	def interpolate(other:Point4, factor:Double):Point4 = {
	    val result = newInstance.asInstanceOf[Point4]
		result.data(0) = data(0) + ((other.data(0) - data(0)) * factor);
		result.data(1) = data(1) + ((other.data(1) - data(1)) * factor);
		result.data(2) = data(2) + ((other.data(2) - data(2)) * factor);
		result.data(3) = data(3) + ((other.data(3) - data(3)) * factor);
		result
	}

    /** Distance between this and `other`. */
    def distance(other:NumberSeq):Double = {
        if(other.data.length > 3) {
            val xx = other.data(0) - data(0)
            val yy = other.data(1) - data(1)
            val zz = other.data(2) - data(2)
            val ww = other.data(3) - data(3)
            sqrt(xx*xx + yy*yy + zz*zz + ww*ww)
        } else {
            0
        }
    }

	/** Distance between this and `other`. */
	def distance(other:Point4):Double = {
		val xx = other.data(0) - data(0)
		val yy = other.data(1) - data(1)
		val zz = other.data(2) - data(2)
		val ww = other.data(3) - data(3)
		abs(sqrt((xx * xx) + (yy * yy) + (zz * zz) + (ww * ww)))
	}
	
	/** Distance between this and point (`x`,`y`,`z`,`w`). */
	def distance(x:Double, y:Double, z:Double, w:Double):Double = {
		val xx = x - data(0)
		val yy = y - data(1)
		val zz = z - data(2)
		val ww = w - data(3)
		abs(sqrt((xx * xx) + (yy * yy) + (zz * zz) + (ww * ww)))
	}

    def +(other:Point4):ReturnType = (new Point4(data(0), data(1), data(2), data(3))).addBy(other)   // Faster than using apply
    
    override def +(value:Double):ReturnType = (new Point4(data(0), data(1), data(2), data(3))).addBy(value)   // Faster than using apply

    def -(other:Point4):ReturnType = (new Point4(data(0), data(1), data(2), data(3))).subBy(other)   // Faster than using apply
    
    override def -(value:Double):ReturnType = (new Point4(data(0), data(1), data(2), data(3))).subBy(value)   // Faster than using apply

    def *(other:Point4):ReturnType = (new Point4(data(0), data(1), data(2), data(3))).multBy(other)   // Faster than using apply
    
    override def *(value:Double):ReturnType = (new Point4(data(0), data(1), data(2), data(3))).multBy(value)   // Faster than using apply

    def /(other:Point4):ReturnType = (new Point4(data(0), data(1), data(2), data(3))).divBy(other)   // Faster than using apply
    
    override def /(value:Double):ReturnType = (new Point4(data(0), data(1), data(2), data(3))).divBy(value)   // Faster than using apply

    def perspectiveDivide() {
    	data(0) /= data(3)
    	data(1) /= data(3)
    	data(2) /= data(3)
    	data(3) = 0
    }

    /** Vector between this and an `other` point. 
      *
      * The direction goes from this to `other`. The length of the vector is
      * the distance between the two points.
      */
    def -->(other:Point4):Vector4 = new Vector4(other.x-x, other.y-y, other.z-z, other.w-w)

    override def canEqual(a:Any) = a.isInstanceOf[Point4]

  	override def equals(that:Any):Boolean = that match {
		case that:Point4 => this.x == that.x && this.y == that.y && this.z == that.z && this.w == that.w && that.hashCode == this.hashCode
		case _ => false
	}

	override def hashCode:Int = (x*73856093).toInt^(y*19349663).toInt^(z*83492791).toInt^(w*19349663).toInt
}