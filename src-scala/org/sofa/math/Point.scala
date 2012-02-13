package org.sofa.math

import scala.math._

object Point2 {
    implicit def point2ToTuple(v:Point2):(Double, Double) = (v.x, v.y)
    def apply(x:Double, y:Double):Point2 = ArrayPoint2(x, y)
}

abstract class Point2 extends NumberSeq2 {
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
	def -->(other:Point2):Vector2
}

object ArrayPoint2 {
    def apply(x:Double, y:Double) = new ArrayPoint2(x, y)
    def apply() = new ArrayPoint2()
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new ArrayPoint2()
        else if(other.size < 2)
        	 new ArrayPoint2(other.data(0), 0)
        else new ArrayPoint2(other.data(0), other.data(1))
    }
    def apply(xy:(Double, Double)) = new ArrayPoint2(xy._1, xy._2)
}

object NioBufferPoint2 {
    def apply(x:Double, y:Double) = new NioBufferPoint2(x, y)
    def apply() = new NioBufferPoint2()
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new NioBufferPoint2()
        else if(other.size < 2)
        	 new NioBufferPoint2(other.data(0), 0)
        else new NioBufferPoint2(other.data(0), other.data(1))
    }
    def apply(xy:(Double, Double)) = new NioBufferPoint2(xy._1, xy._2)
}

class ArrayPoint2(xInit:Double, yInit:Double) extends Point2 {
    type ArrayLike = Array[Double]
    type ReturnType = ArrayPoint2
    
    val data = Array[Double](xInit, yInit)
    def this(other:Point2) = this(other.x, other.y)
    def this() = this(0, 0)
    def newInstance = new ArrayPoint2
	def -->(other:Point2):Vector2 = new ArrayVector2(other.x-x, other.y-y)
    override def toDoubleArray = data
}

class NioBufferPoint2(xInit:Double, yInit:Double) extends Point2 {
    type ArrayLike = org.sofa.nio.DoubleBuffer
    type ReturnType = NioBufferPoint2
    
    val data = org.sofa.nio.DoubleBuffer(xInit, yInit)
    def this(other:Point2) = this(other.x, other.y)
    def this() = this(0, 0)
    def newInstance = new NioBufferPoint2
	def -->(other:Point2):Vector2 = new NioBufferVector2(other.x-x, other.y-y)
    override def toDoubleBuffer = { data.rewind; data }
}

//===================================================

object Point3 {
    implicit def point3ToTuple(v:Point3):(Double, Double, Double) = (v.x, v.y, v.z)
    def apply(x:Double, y:Double, z:Double):Point3 = ArrayPoint3(x, y, z)
}

abstract class Point3 extends NumberSeq3 {
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

	/** Vector between this and an `other` point. 
	  *
	  * The direction goes from this to `other`. The length of the vector is
	  * the distance between the two points.
	  */
	def -->(other:Point3):Vector3
}

object ArrayPoint3 {
    def apply(x:Double, y:Double, z:Double) = new ArrayPoint3(x, y, z)
    def apply() = new ArrayPoint3()
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new ArrayPoint3()
        else if(other.size < 2)
        	 new ArrayPoint3(other.data(0), 0, 0)
        else if(other.size < 3)
        	 new ArrayPoint3(other.data(0), other.data(1), 0)
        else new ArrayPoint3(other.data(0), other.data(1), other.data(2))
    }
    def apply(xyz:(Double, Double, Double)) = new ArrayPoint3(xyz._1, xyz._2, xyz._3)
    def apply(xy:(Double, Double), z:Double) = new ArrayPoint3(xy._1, xy._2, z)
    def apply(x:Double, yz:(Double, Double)) = new ArrayPoint3(x, yz._1, yz._2)
}

object NioBufferPoint3 {
    def apply(x:Double, y:Double, z:Double) = new NioBufferPoint3(x, y, z)
    def apply() = new NioBufferPoint3()
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new NioBufferPoint3()
        else if(other.size < 2)
        	 new NioBufferPoint3(other.data(0), 0, 0)
        else if(other.size < 3)
        	 new NioBufferPoint3(other.data(0), other.data(1), 0)
        else new NioBufferPoint3(other.data(0), other.data(1), other.data(2))
    }
    def apply(xyz:(Double, Double, Double)) = new NioBufferPoint3(xyz._1, xyz._2, xyz._3)
    def apply(xy:(Double, Double), z:Double) = new NioBufferPoint3(xy._1, xy._2, z)
    def apply(x:Double, yz:(Double, Double)) = new NioBufferPoint3(x, yz._1, yz._2)
}

class ArrayPoint3(xInit:Double, yInit:Double, zInit:Double) extends Point3 {
    type ArrayLike = Array[Double]
    type ReturnType = ArrayPoint3
    
    val data = Array[Double](xInit, yInit, zInit)
    def this(other:Point3) = this(other.x, other.y, other.z)
    def this() = this(0, 0, 0)
    def newInstance = new ArrayPoint3
	def -->(other:Point3):Vector3 = new ArrayVector3(other.x-x, other.y-y, other.z-z)
    override def toDoubleArray = data
}

class NioBufferPoint3(xInit:Double, yInit:Double, zInit:Double) extends Point3 {
    type ArrayLike = org.sofa.nio.DoubleBuffer
    type ReturnType = NioBufferPoint3
    
    val data = org.sofa.nio.DoubleBuffer(xInit, yInit, zInit)
    def this(other:Point3) = this(other.x, other.y, other.z)
    def this() = this(0, 0, 0)
    def newInstance = new NioBufferPoint3
	def -->(other:Point3):Vector3 = new NioBufferVector3(other.x-x, other.y-y, other.z-z)
    override def toDoubleBuffer = { data.rewind; data }
}
