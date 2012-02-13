package org.sofa.math

import org.sofa.nio._
import scala.math._

/**
  * Simple sequence of numbers.
  * 
  * This is the basis for vectors or any size, matrices, points or any kind of set of
  * numbers.
  * 
  * ==A note on the design of the math library==
  * 
  * The choice in designing the math library was to always use double real numbers instead of
  * creating a version of each class for floats or doubles, or using type parameters. This
  * design choice  implies that sometimes one will have to copy an array in a given format to
  * another. 
  */
trait NumberSeq extends IndexedSeq[Double] {
    
// Attribute
    
    /** Type of content. */
    type ArrayLike <: { def apply(i:Int):Double; def update(i:Int, f:Double); def length:Int; }
    
    /** The return type of operations that generate a new NumberSeq.
      *
      * As +, -, * and / must return a NumberSeq as this trait is
      * specialized as a Vector or Point, such operations should instead
      * return a Vector or a Point not a NumberSeq. This type is therefore
      * specialized in the concrete classes that use it.
      */
    type ReturnType <: NumberSeq
    
    /** Real content. */
    val data:ArrayLike
 
// Access
    
    /** Number of elements. This is defined in SeqLike, do not confuse with norm !!. */
    def length:Int = data.length

    /** `i`-th element. */
    def apply(i:Int):Double = data(i)
	
    /** True if all components are zero. */
	def isZero:Boolean = {
        var ok = true
        var i  = 0
        val n  = size
        while(i<n) {
            if(data(i) != 0) {
                i = n
                ok = false
            }
        }
        ok
    }
	
	/** True if all components are zero. */
	def isOrigin:Boolean = isZero

	override def toString():String = {
	    val buf = new StringBuffer
	    
	    buf.append("(")
	    buf.append(mkString(", "))
	    buf.append(")")
	    buf.toString
	}
	
	/** New number sequence of the same size as this. */
	protected[math] def newInstance():ReturnType

// Conversion

    /** This sequence converted as an array of doubles.
      *
      * If the sequence is not backed by a double array, a conversion occurs.
      */
    def toDoubleArray:Array[Double] = {
        val n     = data.length
        var i     = 0
        val array = new Array[Double](n)
        while(i<n) {
            array(i) = data(i).toDouble
            i += 1
        }
        array
    }
    
    /** This sequence converted as an array of floats.
      *
      * If the sequence is not backed by a float array, a conversion occurs.
      */
    def toFloatArray:Array[Float] = {
        val n     = data.length
        var i     = 0
        val array = new Array[Float](n)
        while(i<n) {
            array(i) = data(i).toFloat
            i += 1
        }
        array
    }
    
    /** This sequence converted as a NIO buffer of doubles.
      *
      * If the sequence is not backed by a NIO buffer of doubles, a conversion occurs.
      */
    def toDoubleBuffer:DoubleBuffer = {
        val n   = data.length
        var i   = 0
        val buf = new DoubleBuffer(n)
        while(i<n) {
            buf(i) = data(i).toDouble
            i += 1
        }
        buf.rewind
        buf
    }
    
    /** This sequence converted  as a NIO buffer of floats.
      *
      * If the sequence is not backed by a NIO buffer of floats, a conversion occurs.
      */
    def toFloatBuffer:FloatBuffer = {
        val n   = data.length
        var i   = 0
        val buf = new FloatBuffer(n)
        while(i<n) {
            buf(i) = data(i).toFloat
            i += 1
        }
        buf.rewind
        buf
    }
    
// Modification

    /** Is the size of `other` the same as this ? If not throw a `RuntimeException`. */
    protected def checkSizes(other:NumberSeq) {
    	if(other.size != size) throw new RuntimeException("operation available on number sequences of same size only")
    }
    
    /** Assign `value` to the `i`-th element. */
    def update(i:Int, value:Double) = data(i) = value
    
    /** Copy the content of `data` in this.
      * 
      * The size of the smallest sequence determine the number of elements copied.
      */
    def copy(data:Traversable[Double]) {
        val n = scala.math.min(size, data.size) 
        var i = 0
        
        data.foreach { item =>
            if(i<n) {
            	this.data(i) = item
            }
            i += 1
        }
    }
    
	/** Copy `value` in each component. */
	def fill(value:Double) {
	    val n = size
	    var i = 0
	    while(i<n) {
	    	data(i) = value
	    	i += 1
	    }
	}

	/** Add each element of `other` to the corresponding element of this.
	  *
	  * This modifies in place this sequence. The size of the smallest sequence determines the
	  * number of elements to be added, starting at 0.
	  */
	def addBy(other:NumberSeq) {
	    val n = scala.math.min(size, other.size)
	    var i = 0
	    while(i<n) {
	    	data(i) += other(i)
	    	i += 1
	    }
	}

	/** Add `value` to each element of this.
	  *
	  * This modifies in place this sequence.
	  */
	def addBy(value:Double) {
	    val n = size
	    var i = 0
	    while(i<n) {
	    	data(i) += value
	    	i += 1
	    }
	}

	/** Add each element of `other` to the corresponding element of this.
	  *
	  * This modifies in place this sequence. The size of the smallest sequence determines the
	  * number of elements to be added, starting at 0.
	  */
	def +=(other:NumberSeq):ReturnType = { addBy(other); this.asInstanceOf[ReturnType] }

	/** Add `value` to each element of this.
	  *
	  * This modifies in place this sequence.
	  */
	def +=(value:Double):ReturnType = { addBy(value); this.asInstanceOf[ReturnType] }
	
	/** Result of the addition of each element of this by the corresponding element of
	  * `other`.
	  * 
	  * The two sequences must have the same size.
	  * 
	  * @return a new number sequence result of the addition.
	  */
    def +(other:NumberSeq):ReturnType = {
        checkSizes(other)
        val result = newInstance
        result.copy(this)
        result.addBy(other)
        result
    }
    
    /** Result of the addition of value to each element of this.
      * 
      * @return a new number sequence result of the addition. 
      */
    def +(value:Double):ReturnType = {
        val result = newInstance
        result.copy(this)
        result.addBy(value)
        result
    }

	/** Subtract each element of `other` to the corresponding element of this.
	  *
	  * This modifies in place this sequence. The size of the smallest sequence determines the
	  * number of elements to be added, starting at 0.
	  */
	def subBy(other:NumberSeq) {
	    val n = scala.math.min(size, other.size)
	    var i = 0
	    while(i<n) {
	    	data(i) -= other(i)
	    	i += 1
	    }
	}

	/** Subtract `value` to each element of this.
	  *
	  * This modifies in place this sequence.
	  */
	def subBy(value:Double) {
	    val n = size
	    var i = 0
	    while(i<n) {
	    	data(i) -= value
	    	i += 1
	    }
	}

	/** Subtract each element of `other` to the corresponding element of this.
	  *
	  * This modifies in place this sequence. The size of the smallest sequence determines the
	  * number of elements to be added, starting at 0.
	  */
	def -=(other:NumberSeq):ReturnType = { subBy(other); this.asInstanceOf[ReturnType] }

	/** Subtract `value` to each element of this.
	  *
	  * This modifies in place this sequence.
	  */
	def -=(value:Double):ReturnType = { subBy(value); this.asInstanceOf[ReturnType] }
	
	/** Result of the subtraction of each element `other` to the corresponding element of
	  * this.
	  * 
	  * The two sequences must have the same size.
	  * 
	  * @return a new number sequence result of the subtraction.
	  */
    def -(other:NumberSeq):ReturnType = {
        checkSizes(other)
        val result = newInstance
        result.copy(this)
        result.subBy(other)
        result
    }
    
    /** Result of the subtraction of value to each element of this.
      * 
      * @return a new number sequence result of the subtraction. 
      */
    def -(value:Double):ReturnType = {
        val result = newInstance
        result.copy(this)
        result.subBy(value)
        result
    }
	
	/** Multiply each element of `other` with the corresponding element of this.
	  * 
	  * The two sequences must have the same size.
	  *
	  * This modifies in place this sequence. The size of the smallest sequence determines the
	  * number of elements to be multiplied, starting at 0.
	  */
	def multBy(other:NumberSeq) {
	    val n = scala.math.min(size, other.size)
	    var i = 0
	    while(i<n) {
	    	data(i) *= other(i)
	    	i += 1
	    }
	}

	/** Multiply each element of this by `value`.
	  * 
	  * This modifies in place this sequence.
	  */
	def multBy(value:Double) {
	    val n = size
	    var i = 0
	    while(i<n) {
	    	data(i) *= value
	    	i += 1
	    }
	}

	/** Multiply each element of `other` with the corresponding element of this.
	  * 
	  * The two sequences must have the same size.
	  *
	  * This modifies in place this sequence. The size of the smallest sequence determines the
	  * number of elements to be multiplied, starting at 0.
	  */
	def *=(other:NumberSeq):ReturnType = { multBy(other); this.asInstanceOf[ReturnType] }

	/** Multiply each element of this by `value`.
	  * 
	  * This modifies in place this sequence.
	  */
	def *=(value:Double):ReturnType = { multBy(value); this.asInstanceOf[ReturnType] }
	
	/** Result of the multiplication of each element of this by the corresponding element of
	  * `other`.
	  * 
	  * The two sequences must have the same size.
	  * 
	  * @return a new number sequence result of the multiplication.
	  */
	def *(other:NumberSeq):ReturnType = {
	    checkSizes(other)
	    val result = newInstance
	    result.copy(this)
	    result.multBy(other)
	    result
	}
	
	/** Result of the multiplication of each element of this by `value`.
	  * 
	  * @return a new number sequence result of the multiplication.
	  */
	def *(value:Double):ReturnType = {
	    val result = newInstance
	    result.copy(this)
	    result.multBy(value)
	    result
	}
	
	/** Divide each element of this by the corresponding element of `other`.
	  * 
	  * The two sequences must have the same size.
	  *
	  * This modifies in place this sequence. The size of the smallest sequence determines the
	  * number of elements to be divided, starting at 0.
	  */
	def divBy(other:NumberSeq) {
	    checkSizes(other)
	    val n = scala.math.min(size, other.size)
	    var i = 0
	    while(i<n) {
	    	data(i) /= other(i)
	    	i += 1
	    }
	}

	/** Divide each element of this by `value`.
	  * 
	  * This modifies in place this sequence.
	  */
	def divBy(value:Double) {
	    val n = size
	    var i = 0
	    while(i<n) {
	    	data(i) /= value
	    	i += 1
	    }
	}
	
	/** Divide each element of this by the corresponding element of `other`.
	  * 
	  * The two sequences must have the same size.
	  *
	  * This modifies in place this sequence. The size of the smallest sequence determines the
	  * number of elements to be divided, starting at 0.
	  */
	def /=(other:NumberSeq):ReturnType = { divBy(other); this.asInstanceOf[ReturnType] }

	/** Divide each element of this by `value`.
	  * 
	  * This modifies in place this sequence.
	  */
	def /=(value:Double):ReturnType = { divBy(value); this.asInstanceOf[ReturnType] }
	
	/** Result of the division of each element of this by the corresponding element of
	  * `other`.
	  * 
	  * The two sequences must have the same size.
	  * 
	  * @return a new number sequence result of the division.
	  */
	def /(other:NumberSeq):ReturnType = {
	    checkSizes(other)
	    val result = newInstance
	    result.copy(this)
	    result.divBy(other)
	    result
	}

	/** Result of the division of each element of this by `value`.
	  * 
	  * @return a new number sequence result of the division.
	  */
	def /(value:Double):ReturnType = {
	    val result = newInstance
	    result.copy(this)
	    result.divBy(value)
	    result
	}
	
	/** Dot product of this by the set of `values`.
	  * 
	  * The set of `values` must have at least the same number of components as this sequence,
	  * else the dot product is made on the minimum number of elements. 
	  */
	def dot(values:Double*):Double = {
	    val n =  scala.math.min(values.length, size)
	    var i = 0
	    var result = 0.0
	    while(i < n) {
	        result += data(i) * values(i)
	        i += 1
	    }
	    result
	}
	
	/** Dot product of this by `other`.
	  * 
	  * The two sequences must have the same size.
	  */
	def dot(other:NumberSeq):Double = {
	    checkSizes(other)
	    val n = size
	    var i = 0
		var result = 0.0
		while(i<n) {
		    result += data(i) * other.data(i)
		    i += 1
		}
		result
	}
	
	/** Dot product of `this` and `other`.
	  * 
	  * The two sequences must have the same size.
	  */
	def **(other:NumberSeq):Double = dot(other)
	
	/** Magnitude of this (length in terms of distance). */
	def norm:Double = {
	    var result = 0.0
	    foreach { item => result += item * item }
	    scala.math.sqrt(result)
	}
	
	/** Multiply each element of this by the norm of this.
	  * 
	  * Changes are applied to this in place. 
	  */
	def normalize():Double = {
	    val len = norm
	    var i   = 0
	    val n   = size
	    while(i<n) {
	        data(i) /= len
	        i += 1
	    }
	    len
	}
	
	/** Result of the normalization of this. 
	  * 
	  * @return a new number sequence normalization of this.
	  * @see [[normalize]]
	  */
	def normalized():ReturnType = {
	    val result = newInstance
	    result.copy(this)
	    result.normalize
	    result
	}
}

//===================================================

trait NumberSeq2 extends NumberSeq {
    def x:Double = data(0)
    def y:Double = data(1)
    def xy:(Double, Double) = (data(0), data(1))
    
    def x_=(value:Double) = data(0) = value
    def y_=(value:Double) = data(1) = value
    def xy_=(value:(Double, Double)) = { data(0) = value._1; data(1) = value._2 }
}

//===================================================

trait NumberSeq3 extends NumberSeq2 {
	def z:Double = data(2)
    def yz:(Double, Double) = (data(1), data(2))
    def xz:(Double, Double) = (data(0), data(2))
    def xyz:(Double, Double, Double) = (data(0), data(1), data(2))
    
    def z_=(value:Double) = data(2) = value
    def yz_=(value:(Double, Double)) = { data(1) = value._1; data(2) = value._2 }
    def xz_=(value:(Double, Double)) = { data(0) = value._1; data(2) = value._2 }
    def xyz_=(value:(Double, Double, Double)) = { data(0) = value._1; data(1) = value._2; data(2) = value._3 }
}

//===================================================

trait NumberSeq4 extends NumberSeq3 {
	def w:Double = data(3)
    def xw:(Double, Double) = (data(0), data(3))
    def yw:(Double, Double) = (data(1), data(3))
    def zw:(Double, Double) = (data(2), data(3))
    def xyw:(Double, Double, Double) = (data(0), data(1), data(3))
    def xzw:(Double, Double, Double) = (data(0), data(2), data(3))
    def yzw:(Double, Double, Double) = (data(1), data(2), data(3))
    def xyzw:(Double, Double, Double, Double) = (data(0), data(1), data(2), data(3))
    
    def w_=(value:Double) = data(3) = value
    def xw_=(value:(Double, Double)) = { data(0) = value._1; data(3) = value._2 }
    def yw_=(value:(Double, Double)) = { data(1) = value._1; data(3) = value._2 }
    def zw_=(value:(Double, Double)) = { data(2) = value._1; data(3) = value._2 }
    def xyw_=(value:(Double, Double, Double)) = { data(0) = value._1; data(1) = value._2; data(3) = value._3 }
    def xzw_=(value:(Double, Double, Double)) = { data(0) = value._1; data(2) = value._2; data(3) = value._3 }
    def yzw_=(value:(Double, Double, Double)) = { data(1) = value._1; data(2) = value._2; data(3) = value._3 }
    def xyzw_=(value:(Double, Double, Double, Double)) = { data(0) = value._1; data(1) = value._2; data(2) = value._3; data(3) = value._4 }

}
