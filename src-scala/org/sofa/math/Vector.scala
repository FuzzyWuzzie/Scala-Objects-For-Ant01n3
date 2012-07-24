package org.sofa.math

//===================================================

abstract class Vector extends NumberSeq

object Vector {
    def apply(values:Double*) = ArrayVector(values:_*)
    def apply(other:NumberSeq) = ArrayVector(other)
    def apply(size:Int) = ArrayVector(size)
}

object ArrayVector {
    def apply(values:Double*) = {
        val result = new ArrayVector(values.size)
        result.copy(values)
        result
    }
    def apply(other:NumberSeq) = {
        val result = new ArrayVector(other.size)
        result.copy(other)
        result
    }
    def apply(size:Int) = new ArrayVector(size)
}

object NioBufferVector {
    def apply(values:Double*) = {
        val result = new NioBufferVector(values.size)
        result.copy(values)
        result
    }
    def apply(other:NumberSeq) = {
        val result = new NioBufferVector(other.size)
        result.copy(other)
        result
    }
    def apply(size:Int) = new NioBufferVector(size)
}

class ArrayVector(size:Int) extends Vector {
    type ArrayLike = Array[Double]
    type ReturnType = ArrayVector
    
    val data = new Array[Double](size)
    def this(other:NumberSeq) = { this(other.size); copy(other) } 
    def newInstance = new ArrayVector(size)
    override def toDoubleArray = data
}

class NioBufferVector(size:Int) extends Vector {
    type ArrayLike = org.sofa.nio.DoubleBuffer
    type ReturnType = NioBufferVector
    
    val data = new org.sofa.nio.DoubleBuffer(size)
    def this(other:NumberSeq) = { this(other.size); copy(other) } 
    def newInstance = new NioBufferVector(size)
    override def toDoubleBuffer = { data.rewind; data }
}

//===================================================

object Vector2 {
    implicit def vector2ToTuple(v:Vector2):(Double, Double) = (v.x, v.y)
    def apply(x:Double, y:Double) = ArrayVector2(x, y)
    def apply() = ArrayVector2()
    def apply(from:Point2, to:Point2) = ArrayVector2(from, to)
    def apply(other:NumberSeq) = ArrayVector2(other)
    def apply(xy:(Double, Double)) = ArrayVector2(xy)
    def apply(fill:Double) = new ArrayVector2(fill, fill)
}

abstract class Vector2 extends NumberSeq2 {
    def set(x:Double, y:Double):Vector2 = {
        data(0) = x
        data(1) = y
        this
    }
}

object ArrayVector2 {
    def apply(x:Double, y:Double) = new ArrayVector2(x, y)
    def apply() = new ArrayVector2()
    def apply(from:Point2, to:Point2) = new ArrayVector2(to.x-from.x, to.y-from.y)
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new ArrayVector2()
        else if(other.size < 2)
        	 new ArrayVector2(other.data(0), 0)
        else new ArrayVector2(other.data(0), other.data(1))
    }
    def apply(xy:(Double, Double)) = new ArrayVector2(xy._1, xy._2)
    def apply(fill:Double) = new ArrayVector2(fill, fill)
}

object NioBufferVector2 {
    def apply(x:Double, y:Double) = new NioBufferVector2(x, y)
    def apply() = new NioBufferVector2()
    def apply(from:Point2, to:Point2) = new NioBufferVector2(to.x-from.x, to.y-from.y)
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new NioBufferVector2()
        else if(other.size < 2)
        	 new NioBufferVector2(other.data(0), 0)
        else new NioBufferVector2(other.data(0), other.data(1))
    }
    def apply(xy:(Double, Double)) = new NioBufferVector2(xy._1, xy._2)
    def apply(fill:Double) = new NioBufferVector2(fill, fill)
}

class ArrayVector2(xInit:Double, yInit:Double) extends Vector2 {
    type ArrayLike = Array[Double]
    type ReturnType = ArrayVector2
    
    val data = Array[Double](xInit, yInit)
    def this(other:Vector2) = this(other.x, other.y)
    def this() = this(0, 0)
    def newInstance = new ArrayVector2
    override def toDoubleArray = data
}

class NioBufferVector2(xInit:Double, yInit:Double) extends Vector2 {
    type ArrayLike = org.sofa.nio.DoubleBuffer
    type ReturnType = NioBufferVector2

    val data = org.sofa.nio.DoubleBuffer(xInit, yInit)
    def this(other:Vector2) = this(other.x, other.y)
    def this() = this(0, 0)
    def newInstance = new NioBufferVector2
    override def toDoubleBuffer = { data.rewind; data }
}

//===================================================

object Vector3 {
    implicit def vector3ToTuple(v:Vector3):(Double, Double, Double) = (v.x, v.y, v.z)
    def apply(x:Double, y:Double, z:Double) = ArrayVector3(x, y, z)
    def apply() = ArrayVector3()
    def apply(from:Point3, to:Point3) = ArrayVector3(from, to)
    def apply(other:NumberSeq) = ArrayVector3(other)
    def apply(xyz:(Double, Double, Double)) = ArrayVector3(xyz)
    def apply(xy:(Double, Double), z:Double) = ArrayVector3(xy, z)
    def apply(x:Double, yz:(Double, Double)) = ArrayVector3(x, yz)
    def apply(fill:Double) = ArrayVector3(fill, fill, fill)
}

abstract class Vector3 extends NumberSeq3 {
    /** Set this to the cross product of this and vector (`x`, `y`, `z`).
      *
      * This operation works in place, modifying this vector.
      */
	def cross(x:Double, y:Double, z:Double) {
		var xx = 0.0
		var yy = 0.0

		xx      = (data(1) * z) - (data(2) * y);
		yy      = (data(2) * x) - (data(0) * z);
		data(2) = (data(0) * y) - (data(1) * x);
		data(0) = xx
		data(1) = yy
	}
    
    /** Set this to the cross product of this and `other`.
      *
      * This operation works in place, modifying this vector.
      */
	def cross(other:Vector3) {
		var xx = 0.0
		var yy = 0.0

		xx      = (data(1) * other.data(2)) - (data(2) * other.data(1));
		yy      = (data(2) * other.data(0)) - (data(0) * other.data(2));
		data(2) = (data(0) * other.data(1)) - (data(1) * other.data(0));
		data(0) = xx
		data(1) = yy
	}
	
	/** Result of the cross product between this and an `other` vector.
	  * 
	  * @return A new vector result of the cross product. 
	  */
	def X(other:Vector3):ReturnType = {
	    val result = newInstance.asInstanceOf[Vector3]
	    result.copy(this)
	    result.cross(other)
	    result.asInstanceOf[ReturnType]
	}
	
	def set(x:Double, y:Double, z:Double):Vector3 = {
        data(0) = x
        data(1) = y
        data(2) = z
        this
    }
}

object ArrayVector3 {
    def apply(x:Double, y:Double, z:Double) = new ArrayVector3(x, y, z)
    def apply() = new ArrayVector3()
    def apply(from:Point3, to:Point3) = new ArrayVector3(to.x-from.x, to.y-from.y, to.z-from.z)
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new ArrayVector3()
        else if(other.size < 2)
        	 new ArrayVector3(other.data(0), 0, 0)
        else if(other.size < 3)
        	 new ArrayVector3(other.data(0), other.data(1), 0)
        else new ArrayVector3(other.data(0), other.data(1), other.data(2))
    }
    def apply(xyz:(Double, Double, Double)) = new ArrayVector3(xyz._1, xyz._2, xyz._3)
    def apply(xy:(Double, Double), z:Double) = new ArrayVector3(xy._1, xy._2, z)
    def apply(x:Double, yz:(Double, Double)) = new ArrayVector3(x, yz._1, yz._2)
    def apply(fill:Double) = new ArrayVector3(fill, fill, fill)
}

object NioBufferVector3 {
    def apply(x:Double, y:Double, z:Double) = new NioBufferVector3(x, y, z)
    def apply() = new NioBufferVector3()
    def apply(from:Point3, to:Point3) = new NioBufferVector3(to.x-from.x, to.y-from.y, to.z-from.z)
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new NioBufferVector3()
        else if(other.size < 2)
        	 new NioBufferVector3(other.data(0), 0, 0)
        else if(other.size < 3)
        	 new NioBufferVector3(other.data(0), other.data(1), 0)
        else new NioBufferVector3(other.data(0), other.data(1), other.data(2))
    }
    def apply(xyz:(Double, Double, Double)) = new NioBufferVector3(xyz._1, xyz._2, xyz._3)
    def apply(xy:(Double, Double), z:Double) = new NioBufferVector3(xy._1, xy._2, z)
    def apply(x:Double, yz:(Double, Double)) = new NioBufferVector3(x, yz._1, yz._2)
    def apply(fill:Double) = new NioBufferVector3(fill, fill, fill)
}

class ArrayVector3(xInit:Double, yInit:Double, zInit:Double) extends Vector3 {
    type ArrayLike = Array[Double]
    type ReturnType = ArrayVector3
    
    val data = Array[Double](xInit, yInit, zInit)
    def this(other:Vector3) = this(other.x, other.y, other.z)
    def this() = this(0, 0, 0)
    def newInstance = new ArrayVector3
    override def toDoubleArray = data
}

class NioBufferVector3(xInit:Double, yInit:Double, zInit:Double) extends Vector3 {
    type ArrayLike = org.sofa.nio.DoubleBuffer
    type ReturnType = NioBufferVector3
    
    val data = org.sofa.nio.DoubleBuffer(xInit, yInit, zInit)
    def this(other:Vector3) = this(other.x, other.y, other.z)
    def this() = this(0, 0, 0)
    def newInstance = new NioBufferVector3
    override def toDoubleBuffer = { data.rewind; data }
}

//===================================================

object Vector4 {
    implicit def vector4ToTuple(v:Vector4):(Double, Double, Double, Double) = (v.x, v.y, v.z, v.w)
    def apply(x:Double, y:Double, z:Double, w:Double) = ArrayVector4(x, y, z, w)
    def apply() = ArrayVector4()
    def apply(other:NumberSeq) = ArrayVector4(other)
    def apply(xyzw:(Double, Double, Double, Double)) = ArrayVector4(xyzw)
    def apply(xyz:(Double, Double, Double), w:Double) = ArrayVector4(xyz, w)
    def apply(xy:(Double, Double), zw:(Double,Double)) = ArrayVector4(xy, zw)
    def apply(x:Double, yz:(Double, Double), w:Double) = ArrayVector4(x, yz, w)
    def apply(x:Double, yzw:(Double, Double, Double)) = ArrayVector4(x, yzw)
    def apply(fill:Double) = new ArrayVector4(fill, fill, fill, fill)
}

abstract class Vector4 extends NumberSeq4 {
	def set(x:Double, y:Double, z:Double, w:Double) {
        data(0) = x
        data(1) = y
        data(2) = z
        data(3) = w
    }
}

object ArrayVector4 {
    def apply(x:Double, y:Double, z:Double, w:Double) = new ArrayVector4(x, y, z, w)
    def apply() = new ArrayVector4()
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new ArrayVector4()
        else if(other.size < 2)
        	 new ArrayVector4(other.data(0), 0, 0, 0)
        else if(other.size < 3)
        	 new ArrayVector4(other.data(0), other.data(1), 0, 0)
        else if(other.size < 4)
        	 new ArrayVector4(other.data(0), other.data(1), other.data(2), 0)
        else new ArrayVector4(other.data(0), other.data(1), other.data(2), other.data(3))
    }
    def apply(xyzw:(Double, Double, Double, Double)) = new ArrayVector4(xyzw._1, xyzw._2, xyzw._3, xyzw._4)
    def apply(xyz:(Double, Double, Double), w:Double) = new ArrayVector4(xyz._1, xyz._2, xyz._3, w)
    def apply(xy:(Double, Double), zw:(Double,Double)) = new ArrayVector4(xy._1, xy._2, zw._1, zw._2)
    def apply(x:Double, yz:(Double, Double), w:Double) = new ArrayVector4(x, yz._1, yz._2, w)
    def apply(x:Double, yzw:(Double, Double, Double)) = new ArrayVector4(x, yzw._1, yzw._2, yzw._3)
    def apply(fill:Double) = new ArrayVector4(fill, fill, fill, fill)
}

object NioBufferVector4 {
    def apply(x:Double, y:Double, z:Double, w:Double) = new ArrayVector4(x, y, z, w)
    def apply() = new ArrayVector4()
    def apply(other:NumberSeq) = {
        if(other.size < 1) // Nooooo !!!
             new ArrayVector4()
        else if(other.size < 2)
        	 new ArrayVector4(other.data(0), 0, 0, 0)
        else if(other.size < 3)
        	 new ArrayVector4(other.data(0), other.data(1), 0, 0)
        else if(other.size < 4)
        	 new ArrayVector4(other.data(0), other.data(1), other.data(2), 0)
        else new ArrayVector4(other.data(0), other.data(1), other.data(2), other.data(3))
    }
    def apply(xyzw:(Double, Double, Double, Double)) = new NioBufferVector4(xyzw._1, xyzw._2, xyzw._3, xyzw._4)
    def apply(xyz:(Double, Double, Double), w:Double) = new NioBufferVector4(xyz._1, xyz._2, xyz._3, w)
    def apply(xy:(Double, Double), zw:(Double,Double)) = new NioBufferVector4(xy._1, xy._2, zw._1, zw._2)
    def apply(x:Double, yz:(Double, Double), w:Double) = new NioBufferVector4(x, yz._1, yz._2, w)
    def apply(x:Double, yzw:(Double, Double, Double)) = new NioBufferVector4(x, yzw._1, yzw._2, yzw._3)
    def apply(fill:Double) = new NioBufferVector4(fill, fill, fill, fill)
}

class ArrayVector4(xInit:Double, yInit:Double, zInit:Double, wInit:Double) extends Vector4 {
    type ArrayLike = Array[Double]
    type ReturnType = ArrayVector4
    
    val data = Array[Double](xInit, yInit, zInit, wInit)
    def this(other:Vector4) = this(other.x, other.y, other.z, other.w)
    def this() = this(0, 0, 0, 0)
    def newInstance = new ArrayVector4
    override def toDoubleArray = data
}

class NioBufferVector4(xInit:Double, yInit:Double, zInit:Double, wInit:Double) extends Vector4 {
    type ArrayLike = org.sofa.nio.DoubleBuffer
    type ReturnType = NioBufferVector4
    
    val data = org.sofa.nio.DoubleBuffer(xInit, yInit, zInit, wInit)
    def this(other:Vector4) = this(other.x, other.y, other.z, other.w)
    def this() = this(0, 0, 0, 0)
    def newInstance = new NioBufferVector4
    override def toDoubleBuffer = { data.rewind; data }
}
