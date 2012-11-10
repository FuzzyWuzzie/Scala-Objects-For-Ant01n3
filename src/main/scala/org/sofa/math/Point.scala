package org.sofa.math

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
    def apply(xyz:(Double, Double, Double)) = new Point3(xyz._1, xyz._2, xyz._3)
    def apply(xy:(Double, Double), z:Double) = new Point3(xy._1, xy._2, z)
    def apply(x:Double, yz:(Double, Double)) = new Point3(x, yz._1, yz._2)
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

    def +(other:Point3):ReturnType = {
        val result = new Point3(data(0), data(1), data(2))   // Faster than using apply
        result.addBy(other)
        result
    }
    
    override def +(value:Double):ReturnType = {
        val result = new Point3(data(0), data(1), data(2))   // Faster than using apply
        result.addBy(value)
        result
    }

    def -(other:Point3):ReturnType = {
        val result = new Point3(data(0), data(1), data(2))   // Faster than using apply
        result.subBy(other)
        result
    }
    
    override def -(value:Double):ReturnType = {
        val result = new Point3(data(0), data(1), data(2))   // Faster than using apply
        result.subBy(value)
        result
    }

    def *(other:Point3):ReturnType = {
        val result = new Point3(data(0), data(1), data(2))   // Faster than using apply
        result.multBy(other)
        result
    }
    
    override def *(value:Double):ReturnType = {
        val result = new Point3(data(0), data(1), data(2))   // Faster than using apply
        result.multBy(value)
        result
    }

    def /(other:Point3):ReturnType = {
        val result = new Point3(data(0), data(1), data(2))   // Faster than using apply
        result.divBy(other)
        result
    }
    
    override def /(value:Double):ReturnType = {
        val result = new Point3(data(0), data(1), data(2))   // Faster than using apply
        result.divBy(value)
        result
    }

    /** Vector between this and an `other` point. 
      *
      * The direction goes from this to `other`. The length of the vector is
      * the distance between the two points.
      */
    def -->(other:Point3):Vector3 = new Vector3(other.x-x, other.y-y, other.z-z)
}