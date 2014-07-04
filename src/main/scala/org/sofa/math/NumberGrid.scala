package org.sofa.math

import org.sofa.nio._
import scala.math._
import scala.compat.Platform
import org.sofa.math.Axis._

/** A 2D grid of numbers.
  * 
  * ==Ordering==
  * 
  * This grid is organized as a linear sequence of numbers. This sequence must be
  * considered in column major order, this means elements are organized by example
  * with a 4x4 grid as follows:
  * {{{
  *      | 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 |
  * 
  *      =   | 0  4  8 12 |
  *          | 1  5  9 13 |
  *          | 2  6 10 14 |
  *          | 3  7 11 15 |
  * }}}
  * This is not common in math, but has been chosen to be directly compatible with
  * graphics library like OpenGL for example.
  */
trait NumberGrid extends IndexedSeq[Double] {
// Attribute
	
	/** The return type of +, -, * and / operations for example. */
	type ReturnType <: NumberGrid
	
	/** The real storage. */
	protected[math] val data:Array[Double]

	/** A reference to a temporary array, to avoid reallocate it constantly. */
    protected var tmpFltArr:Array[Float] = null

    protected var tmpFltBuf:FloatBuffer = null

// Access
	
	/** Number of numbers in the grid. */
	def length:Int = data.length
	
	/** Number of columns. */
	def width:Int
	
	/** Number of rows. */
	def height:Int

	/** Access to the `i`-th element of the grid seen as a linear sequence of numbers in
	  * column major order. */
	def apply(i:Int):Double = data(i)
	
	/** Access to element at (`row`, `col`). */
	def apply(row:Int, col:Int):Double = data(row+col*height)
	
	override def toString():String = {
		val buf = new StringBuffer

		for(row <- 0 until height) {
		    buf.append("| ")
		    for(col <- 0 until width) {
		        buf.append("%10.3f".format(data(row+col*height)))
		    }
		    buf.append(" |%n".format())
		}
		
		buf.toString
	}

	/** String representation of the matrix on one line. */
	def toCompactString():String = {
		val buf = new StringBuffer

		buf.append("matrix( ")
		for(row <- 0 until height) {
			for(col <- 0 until width) {
				buf.append("%.2f ".format(data(row+col*height)))
			}
			buf.append("| ")
		}
		buf.append(")")
		buf.toString
	}
	
	/** New number grid of the same size as this. */
	protected def newInstance(width:Int, height:Int):ReturnType

	/** New copy of this. */
	def newClone:ReturnType = {
	    val result = newInstance(width, height)
	    result.copy(this)
	    result
	}

// Conversion
	
    /** This grid converted of numbers as an array of doubles.
      *
      * If the sequence is not backed by a double array, a conversion occurs.
      */
    def toDoubleArray:Array[Double] = data
    
    /** This sequence converted of numbers as an array of floats.
      *
      * If the sequence is not backed by a float array, a conversion occurs.
      */
    def toFloatArray:Array[Float] = {
        val n     = data.length
        var i     = 0

        if((tmpFltArr eq null) || tmpFltArr.length < n)
        	tmpFltArr = new Array[Float](n)

        while(i < n) {
            tmpFltArr(i) = data(i).toFloat
            i += 1
        }

        tmpFltArr
    }
    
    /** This sequence converted of numbers as a NIO buffer of doubles.
      *
      * If the sequence is not backed by a NIO buffer of doubles, a conversion occurs.
      */
    def toDoubleBuffer:DoubleBuffer = {
        val buf = DoubleBuffer(data)
        buf
    }
    
    /** This sequence converted of numbers as a NIO buffer of floats.
      *
      * If the sequence is not backed by a NIO buffer of floats, a conversion occurs.
      */
    def toFloatBuffer:FloatBuffer = {
        val n   = data.length
        var i   = 0

        if((tmpFltBuf eq null) || tmpFltBuf.size < n)
        	tmpFltBuf = FloatBuffer(n)

        while(i < n) {
            tmpFltBuf(i) = data(i).toFloat
            i += 1
        }
        
        tmpFltBuf
    }

// Modification
    
    /** Assign `value` to the i-th element.*/
    def update(i:Int, value:Double) = data(i) = value
    
    /** Assign `value` to the element at (`row`,`col`). */
    def update(row:Int, col:Int, value:Double) = data(row+col*height) = value

    /** Copy the content of `other` in this.
      * 
      * The size of the smallest grid determine the number of elements copied.
      * The contents are not copied linearly. The elements are copied by their
      * (row,col) coordinates. For example if you copy a 3x3 grid in a 4x4 grid
      * only the 3x3 upper part of the 4x4 grid will be changed.0
      */
    def copy(other:NumberGrid) {
    	Platform.arraycopy(other.data, 0, data, 0, math.min(data.length, other.data.length))
    }

    def copy(floats:Array[Float], offset:Int) {
    	if(floats.length-offset >= 16) {
    		var i = 0
    		while(i < 16) {
    			data(i) = floats(i+offset)
    			i += 1
    		}
    	}
    }

    def copy(doubles:Array[Double], offset:Int) {
    	if(doubles.length-offset >= 16) {
    		var i = 0
    		while(i < 16) {
    			data(i) = doubles(i+offset)
    			i += 1
    		}
    	}
    }

	/** Copy `value` in each component. */
	def fill(value:Double) {
	   	val n = size
	   	var i = 0
	   	while(i < n) {
	   		data(i) = value
	   		i += 1
	   	}
	}

	def setIdentity() {
	    fill(0)
	    
	    val n = math.min(width, height)
		var i = 0	    

		while(i < n) {
	    	this(i, i) = 1
	    	i += 1
	    }
	}

	/** Add each element of `other` to the corresponding element of this. 
	  * 
	  * The modification is made in place.
	  */
	def addBy(other:NumberGrid) {
	    var w = math.min(width, other.width)
	    var h = math.min(height, other.height)
	   	var x = 0
	   	var y = 0

	   	while(y < h) {
	   		x = 0
	   		var Y = y*w;
	   		while(x < w) {
	   			data(Y+x) += other.data(Y+x)
	   			x += 1
	   		}
	   		y += 1
	   	}

	    // for(row <- 0 until h) {
	    //     for(col <- 0 until w) {
	    //         this(row, col) += other(row, col)
	    //     }
	    // }
	}
	
	/** Add each element of `other` to the corresponding element of this. 
	  * 
	  * The modification is made in place.
	  * 
	  * @return this
	  */
	def +=(other:NumberGrid):ReturnType = { addBy(other); this.asInstanceOf[ReturnType] }

	/** Result of the addition of each element of `other` to the corresponding
	  * element of this. 
	  *  
	  * @return A new number grid result of the addition.
	  */
	def +(other:NumberGrid):ReturnType = {
	    val result = newInstance(width, height)
	    result.copy(this)
	    result.addBy(other)
	    result
	}

	/** Subtract each element of `other` to the corresponding element of this. 
	  * 
	  * The modification is made in place.
	  */
	def subBy(other:NumberGrid) {
	    var w = scala.math.min(width, other.width)
	    var h = scala.math.min(height, other.height)
	   
// TODO 
	    for(row <- 0 until h) {
	        for(col <- 0 until w) {
	            this(row, col) -= other(row, col)
	        }
	    }
	}
	
	/** Subtract each element of `other` to the corresponding element of this. 
	  * 
	  * The modification is made in place.
	  * 
	  * @return this
	  */
	def -=(other:NumberGrid):ReturnType = { subBy(other); this.asInstanceOf[ReturnType] }

	/** Result of the subtraction of each element of `other` to the corresponding
	  * element of this. 
	  *  
	  * @return A new number grid result of the subtraction.
	  */
	def -(other:NumberGrid):ReturnType = {
	    val result = newInstance(width, height)
	    result.copy(this)
	    result.subBy(other)
	    result
	}
	
	/** Result of the multiplication of this by `other` using usual matrix multiplication.
	  * 
	  * The result is a matrix whose width is the one of `other` and height is the one of this. Due
	  * to the fact the new matrix may have a different size, there  is no "in place" multiplication
	  * or *= operator for general matrices, subclasses of square matrices may add it however.
	  * 
	  * @return the result of the multiplication of this by `other`.
	  */
	def mult(other:NumberGrid):ReturnType = {
	    checkSizes(other)
	    val w      = other.width
	    val h      = height
	    val result = newInstance(w, h)
	   
// TODO 
	    for(row <- 0 until h) {
	        for(col <- 0 until w) {
	            var value = 0.0
	            for(i <- 0 until width) {
	            	value += this(row, i) * other(i, col) 
	            }
	            result(row, col) = value
	        }
	    }

	    result
	}
	
	/** Result of the multiplication of this by `other` using usual matrix multiplication. */
	def * (other:NumberGrid):ReturnType = mult(other)
	
	/** Result of the multiplication of this by `other` using usual matrix/vector multiplication. */
	def mult[T<:NumberSeq](other:T):T = {
	    checkSizes(other)
	    val result = other.newInstance.asInstanceOf[T]

// TODO
	    for(row <- 0 until height) {
	        var res = 0.0
	        for(col <- 0 until width) {
	            res += this(row, col) * other(col)
	        }
	        result(row) = res
	    }
	    
	    result
	}
	
	/** Result of the multiplication of this by `other` using usual matrix/vector multiplication. */
	def *[T<:NumberSeq] (other:T):T = mult(other)
	
	/** Transpose in place. */
	def transpose() = {
		var t = 0.0
		var y = 1
		
		while(y < height) {
			var x = 0
			
			while(x < y) {
				t          = this(x, y)
				this(x, y) = this(y, x)
				this(y, x) = t
				
				x += 1
			}
			
			y += 1
		}
	}
	
	/** Result of the transposition of this. */
	def transposed():ReturnType = {
	    val result = newInstance(width, height)
	    result.copy(this)
	    result.transpose()
	    result
	}
	
		
	/** Submatrix of this matrix excepted the given `row` and ` col`umn. 
	  * 
	  * The submatrix is copied in the `result` parameter. This parameter
	  * must be (this.width-1 x this.height-1).
	  */
	def subMatrix(result:NumberGrid, row:Int, col:Int) {
// TODO
	    if(result.width == (width-1) && result.height == (height-1) ) {
	    	for(c <- 0 until width) {
	    		for(r <- 0 until height) {
	    			result(r, c) = this(
	    					if(r>=row) r+1 else r,
	    					if(c>=col) c+1 else c
	            		)
	    		}
	    	}
	    } else {
	    	throw new RuntimeException("the sub matrix should be %d x %d for this%d x %d matix".format(width-1, height-1, width, height))
	    }
	}
	
	protected def checkSizes(other:NumberGrid) {
	    if(width != other.height)
	        throw new RuntimeException("grid multiply : incompatible sizes")
	}
	
	protected def checkSizes(other:NumberSeq) {
	    if(other.size != width)
	        throw new RuntimeException("grid * seq : incompatble sizes")
	}
}

/** Trait for 3x3 number grids.
  *  
  */
trait NumberGrid3 extends NumberGrid {
    def width = 3
    def height = 3

    def row(r:Int):(Double, Double, Double) = (data(r), data(r+3), data(r+6))
    def col(c:Int):(Double, Double, Double) = (data(3*c), data(3*c+1), data(3*c*2))
    def setRow(r:Int, a:Double, b:Double, c:Double) = { this(r,0)=a; this(r,1)=b; this(r,2)=c }

    def row0:(Double, Double, Double) = (this(0,0), this(0,1), this(0,2))
    def row1:(Double, Double, Double) = (this(1,0), this(1,1), this(1,2))
    def row2:(Double, Double, Double) = (this(2,0), this(2,1), this(2,2))
    def row0_=(abc:(Double, Double, Double)) = { this(0,0)=abc._1; this(0,1)=abc._2; this(0,2)=abc._3 }
    def row1_=(abc:(Double, Double, Double)) = { this(1,0)=abc._1; this(1,1)=abc._2; this(1,2)=abc._3 }
    def row2_=(abc:(Double, Double, Double)) = { this(2,0)=abc._1; this(2,1)=abc._2; this(2,2)=abc._3 }

	override def apply(row:Int, col:Int):Double = data(row+col*3)

	override def setIdentity() {
		data(0) = 1;  data(3) = 0;  data(6) = 0
		data(1) = 0;  data(4) = 1;  data(7) = 0
		data(2) = 0;  data(5) = 0;  data(8) = 1
	}
    
    /** Multiply this by `rhs` storing the result in this, using usual matrix multiplication.
      * 
      * The result is stored in place.
      */
    def multBy(rhs:NumberGrid3) = {
		if(rhs eq this)
			throw new RuntimeException("this and rhs cannot be the same matrix")

		var a, b, c = 0.0
		var i = 0

		//
		// For each row of the result.
		//
		while(i < 3) {
			//
			// Row i of this.
			//
			a = this(i,0)
			b = this(i,1)
			c = this(i,2)

			//
			// With each column of rhs.
			//
			this(i,0) = (a * rhs.data(0)) + (b * rhs.data(1)) + (c * rhs.data(2))
			this(i,1) = (a * rhs.data(3)) + (b * rhs.data(4)) + (c * rhs.data(5))
			this(i,2) = (a * rhs.data(6)) + (b * rhs.data(7)) + (c * rhs.data(8))
				
			i += 1
		}
	}
    
    /** Multiply this by `rhs` storing the result in this, using usual matrix multiplication.
      * 
      * The result is stored in place.
      * 
      * @return this.
      */
    def *=(other:Matrix3):ReturnType = { multBy(other); this.asInstanceOf[ReturnType] } 

    /** Multiply this by `rhs` using usual matrix multiplication.
      * 
      * @return a new matrix result of the multiplication.
      */
    def *(other:NumberGrid3):ReturnType = {
        val result = newInstance(3, 3).asInstanceOf[Matrix3]
        result.copy(this)
        result.multBy(other)
        result.asInstanceOf[ReturnType]
    }

    /** Determinant of this. */
    def det:Double = {
        data(0) * (data(4) * data(8) - data(5) * data(7)) -
        data(3) * (data(1) * data(8) - data(2) * data(7)) +
        data(6) * (data(1) * data(5) - data(2) * data(4))
    }

    /** Inverse of this matrix.
      * 
      * If this matrix has no inverse (zero determinant), an identity is returned.
      */
    def inverse:ReturnType = {
  
        val d = det
        val r = newInstance(width, height)

        if(abs(d) > 0.0005) {
        	r(0) = (  data(4) * data(8) - data(7) * data(5) ) / d
        	r(3) = (-(data(3) * data(8) - data(5) * data(6))) / d
        	r(6) = (  data(3) * data(7) - data(4) * data(6) ) / d

        	r(1) = (-(data(1) * data(8) - data(7) * data(2))) / d
        	r(4) = (  data(0) * data(8) - data(2) * data(6) ) / d
    		r(7) = (-(data(0) * data(7) - data(1) * data(6))) / d

    		r(2) = (  data(1) * data(5) - data(2) * data(4) ) / d
    		r(5) = (-(data(0) * data(5) - data(2) * data(3))) / d
    		r(8) = (  data(0) * data(4) - data(3) * data(1) ) / d
        } else {
            r.setIdentity
        }
        
        r
    }

}

/** Trait for 4x4 number grids.
  *
  */
trait NumberGrid4 extends NumberGrid {

	/** Reference to a temporary matrix used internally, to avoid reallocating it constantly. */
	protected var tmpM4:NumberGrid4 = null

    def width = 4
    def height = 4

    def row(r:Int):(Double, Double, Double, Double) = (data(r), data(r+4), data(r+8), data(r+12))
    def col(c:Int):(Double, Double, Double, Double) = (data(4*c), data(4*c+1), data(4*c+2), data(4*c+3))
    def setRow(r:Int, a:Double, b:Double, c:Double, d:Double) = { this(r,0)=a; this(r,1)=b; this(r,2)=c; this(r,3)=d }
    def row0:(Double, Double, Double, Double) = (this(0,0), this(0,1), this(0,2), this(0,3))
    def row1:(Double, Double, Double, Double) = (this(1,0), this(1,1), this(1,2), this(1,3))
    def row2:(Double, Double, Double, Double) = (this(2,0), this(2,1), this(2,2), this(2,3))
    def row3:(Double, Double, Double, Double) = (this(3,0), this(3,1), this(3,2), this(3,3))
    def row0_=(abcd:(Double, Double, Double, Double)) = { this(0,0)=abcd._1; this(0,1)=abcd._2; this(0,2)=abcd._3; this(0,3)=abcd._4 }
    def row1_=(abcd:(Double, Double, Double, Double)) = { this(1,0)=abcd._1; this(1,1)=abcd._2; this(1,2)=abcd._3; this(1,3)=abcd._4 }
    def row2_=(abcd:(Double, Double, Double, Double)) = { this(2,0)=abcd._1; this(2,1)=abcd._2; this(2,2)=abcd._3; this(2,3)=abcd._4 }
    def row3_=(abcd:(Double, Double, Double, Double)) = { this(3,0)=abcd._1; this(3,1)=abcd._2; this(3,2)=abcd._3; this(3,3)=abcd._4 }

	override def apply(row:Int, col:Int):Double = data(row+col*4)

	override def setIdentity() {
		data(0) = 1;  data(4) = 0;  data(8)  = 0;  data(12) = 0
		data(1) = 0;  data(5) = 1;  data(9)  = 0;  data(13) = 0
		data(2) = 0;  data(6) = 0;  data(10) = 1;  data(14) = 0
		data(3) = 0;  data(7) = 0;  data(11) = 0;  data(15) = 1
	}
    
    /** Multiply this by `rhs` storing the result in this, using usual matrix multiplication.
      * 
      * The result is stored in place.
      */
    def multBy(rhs:NumberGrid4) = {
		if(rhs eq this)
			throw new RuntimeException("this and rhs cannot be the same matrix")

		var a, b, c, d = 0.0
		var i = 0

		//
		// For each row of the result.
		//
		while(i < 4) {
			//
			// Row i of this.
			//
			a = data(i+0)
			b = data(i+4)
			c = data(i+8)
			d = data(i+12)

			//
			// With each column of rhs.
			//
			data(i+0)  = (a * rhs.data( 0)) + (b * rhs.data( 1)) + (c * rhs.data( 2)) + ( d * rhs.data( 3))
			data(i+4)  = (a * rhs.data( 4)) + (b * rhs.data( 5)) + (c * rhs.data( 6)) + ( d * rhs.data( 7))
			data(i+8)  = (a * rhs.data( 8)) + (b * rhs.data( 9)) + (c * rhs.data(10)) + ( d * rhs.data(11))
			data(i+12) = (a * rhs.data(12)) + (b * rhs.data(13)) + (c * rhs.data(14)) + ( d * rhs.data(15))
				
			i += 1
		}
	}
    
    /** Multiply this by `rhs` storing the result in this, using usual matrix multiplication.
      * 
      * The result is stored in place.
      * 
      * @return this.
      */
    def *=(other:NumberGrid4):ReturnType = { multBy(other); this.asInstanceOf[ReturnType] } 

    /** Multiply this by `rhs` using usual matrix multiplication.
      * 
      * @return a new matrix result of the multiplication.
      */
    def *(other:NumberGrid4):ReturnType = {
        val result = newInstance(4, 4).asInstanceOf[Matrix4]
        result.copy(this)
        result.multBy(other)
        result.asInstanceOf[ReturnType]
    }
    
    	
	/**
	 * Change into a rotation matrix around the X axis about
	 * <code>angle</code>. <code>angle</code> is expressed in degrees. The old
	 * matrix is erased, not post-multiplied.
	 */
	def setXRotation(angle:Double) = {
		var a = angle
		if     (a < -360) a = -360
		else if(a >  360) a =  360

		val sint = sin((Pi / 180.0) * a)
		val cost = cos((Pi / 180.0) * a)
	
		data(0) = 1;  data(4) = 0;     data(8)  = 0;      data(12) = 0
		data(1) = 0;  data(5) = cost;  data(9)  = -sint;  data(13) = 0
		data(2) = 0;  data(6) = sint;  data(10) =  cost;  data(14) = 0
		data(3) = 0;  data(7) = 0;     data(11) = 0;      data(15) = 1
	}
	
	/**Same as {@link #setXRotation(Double)} but around the Y axis. */
	def setYRotation(angle:Double) = {
		var a = angle
		if     (a < -360) a = -360
		else if(a >  360) a =  360
	
		val sint = sin((Pi / 180.0) * a)
		val cost = cos((Pi / 180.0) * a)
	
		data(0) = cost;   data(4) = 0;  data(8)  = sint;  data(12) = 0
		data(1) = 0;      data(5) = 1;  data(9)  = 0;     data(13) = 0
		data(2) = -sint;  data(6) = 0;  data(10) = cost;  data(14) = 0
		data(3) = 0;      data(7) = 0;  data(11) = 0;     data(15) = 1
	}
	
	/** Same as {@link #setXRotation(Double)} but around the Z axis. */
	def setZRotation(angle:Double) = {
		var a = angle
		if     (a < -360) a = -360
		else if(a >  360) a =  360
	
		val sint = sin((Pi / 180.0) * a)
		val cost = cos((Pi / 180.0) * a)
	
		data(0) = cost;  data(4) = -sint;  data(8)  = 0;  data(12) = 0
		data(1) = sint;  data(5) =  cost;  data(9)  = 0;  data(13) = 0
		data(2) = 0;     data(6) = 0;      data(10) = 1;  data(14) = 0
		data(3) = 0;     data(7) = 0;      data(11) = 0;  data(15) = 1
	}
	
	/**
	 * Set this matrix as a rotation matrix, rotating of `angle` radians
	 * around axis (`u`, `v`, `w`). All the coefficients of the matrix
	 * are changed. 
	 */
	def setRotation(angle:Double, u:Double, v:Double, w:Double) = {
		val rcos = cos(angle)
		val rsin = sin(angle)
	
		data(0)      =      rcos + u*u*(1-rcos)
		data(1)      =  w * rsin + v*u*(1-rcos)
		data(2)      = -v * rsin + w*u*(1-rcos)
		data(3)      = 0

		data(4)      = -w * rsin + u*v*(1-rcos)
		data(5)      =      rcos + v*v*(1-rcos)
		data(6)      =  u * rsin + w*v*(1-rcos)
		data(7)      = 0

		data(8)      =  v * rsin + u*w*(1-rcos)
		data(9)      = -u * rsin + v*w*(1-rcos)
		data(10)     =      rcos + w*w*(1-rcos)
		data(11)     = 0
		
		data(12)     = 0
		data(13)     = 0
		data(14)     = 0
		data(15)     = 1
	}
	
	/** Fill only the translation part of this matrix with the vector `t`. */
	def setTranslation(t:NumberSeq3) { setTranslation(t.x, t.y, t.z) }

	/** Fill only the translation part of this matrix with the vector (`tx`,`ty,`tz`). */
	def setTranslation(tx:Double, ty:Double, tz:Double) = {
		data(12) = tx
		data(13) = ty
		data(14) = tz
	}
	
	/** Fill only the scaling part of this matrix with the values of `s`. */
	def setScale(s:NumberSeq3) { setScale(s.x, s.y, s.z) }
	
	/** Fill only the scaling part of this matrix with the values (`sx`, `sy`, `sz`). */
	def setScale(sx:Double, sy:Double, sz:Double) = {
		data( 0) = sx
		data( 5) = sy
		data(10) = sz
	}
	
	/** Fill the 3x3 upper left matrix with the three axis, the first
	  * column with `X`, the second with `Y` and the third with `Z`.
	  * The other matrix coefficients are not changed.
	  */
	def setRotation(X:NumberSeq3, Y:NumberSeq3, Z:NumberSeq3) {
	    data(0) = X.x; data(4) = Y.x; data(8)  = Z.x;  //data(12) = 0
	    data(1) = X.y; data(5) = Y.y; data(9)  = Z.y;  //data(13) = 0
	    data(2) = X.z; data(6) = Y.z; data(10) = Z.z;  //data(14) = 0
	    //data(3) = 0;   data(7) = 0;   data(11) = 0;    data(15) = 1
	}
	
	/** Fill only the upper left 3x3 matrix. */
	def setRotation(
		r01:Double, r02:Double, r03:Double,
		r11:Double, r12:Double, r13:Double,
		r21:Double, r22:Double, r23:Double ) = {
		data(0) = r01;		data(4) = r02;		data(8)  = r03;		//data(12) = 0
		data(1) = r11;		data(5) = r12;		data(9)  = r13;		//data(13) = 0
		data(2) = r21;		data(6) = r22;		data(10) = r23;		//data(14) = 0
		//data(3) = 0;        data(7) = 0;        data(11) = 0;		data(15) = 1
	}

	/** Inverse each of the translation coefficients. */
	def inverseTranslation() = {
		data(12) = -data(12)
		data(13) = -data(13)
		data(14) = -data(14)
	}

	/**
	 * Make this matrix a rotation matrix using the Euler angles `r`.
	 * @see #setEulerRotation(Double, Double, Double)
	 */
	def setEulerRotation(r:NumberSeq3) { setEulerRotation(r(0), r(1), r(2)) }

	/**
	 * Make this matrix a rotation matrix using the Euler angles
	 * (`rxx`,`ryy`,`rzz`). Euler angles define
	 * three rotation matrices around the X, Y and Z axis. The matrix are then
	 * applied following the order M=X.Y.Z (not using this technique though
	 * that would involve three full matrix multiplications).
	 * @see #setEulerRotation(Vector3)
	 */
	def setEulerRotation(rxx:Double, ryy:Double, rzz:Double) {
		// See the Matrix FAQ for an explanation of this.
		// http://skal.planet-d.net/demo/matrixfaq.htm (or type Matrix FAQ in
		// Google!).

		var rx = rxx
		var ry = ryy
		var rz = rzz
		
		rx *= (Pi / 180)
		ry *= (Pi / 180)
		rz *= (Pi / 180)

		val A    = cos(rx)
		val B    = sin(rx)
		val C    = cos(ry)
		val D    = sin(ry)
		val E    = cos(rz)
		val F    = sin(rz)
		val AD   =   A * D
		val BD   =   B * D
		data(0)  =   C * E
		data(4)  =  -C * F
		data(8)  =   D
		data(1)  =  BD * E + A * F
		data(5)  = -BD * F + A * E
		data(9)  =  -B * C
		data(2)  = -AD * E + B * F
		data(6)  =  AD * F + B * E
		data(10) =   A * C
		data(3)  = 0; data(7) = 0; data(11) = 0; data(12) = 0; data(13) = 0; data(14) = 0
		data(15) =  1
	}

	/** Multiply this matrix by a translation matrix defined by the given
	  * translation vector `t`. 
	  * 
	  * The matrix is changed in place.
	  */
	def translate(t:NumberSeq3) { translate(t.x, t.y, t.z) }	    
	
	/** Multiply this matrix by a translation matrix defined by the given
	  * translation vector (`tx`, `ty`, `tz`). 
	  * 
	  * The matrix is changed in place.
	  */
	def translate(tx:Double, ty:Double, tz:Double) = {
//		val T = newInstance(4, 4).asInstanceOf[NumberGrid4]
//		T.copy(this)
//		setIdentity
//		setTranslation(tx, ty, tz)
//		multBy(T)
		if(tmpM4 eq null) tmpM4 = newInstance(4, 4).asInstanceOf[NumberGrid4]
		tmpM4.setIdentity
		tmpM4.setTranslation(tx, ty, tz)
		multBy(tmpM4)
	}

	/** Multiply this matrix by a scale matrix defined by the given
	  * scaling vector `s`.
	  *
	  * The matrix is changed in place.
	  */
	def scale(s:NumberSeq3) { scale(s.x, s.y, s.z) }	    
	
	/** Multiply this matrix by a scale matrix defined by the given
	  * scaling vector (`sx`, `sy`, `sz`).
	  *
	  * The matrix is changed in place.
	  */
	def scale(sx:Double, sy:Double, sz:Double) = {
//		val S = newInstance(4, 4).asInstanceOf[NumberGrid4]
//		S.copy(this)
//		setIdentity
//		setScale(sx, sy, sz)
//		multBy(S)
		if(tmpM4 eq null) tmpM4 = newInstance(4, 4).asInstanceOf[NumberGrid4]
		tmpM4.setIdentity
		tmpM4.setScale(sx, sy, sz)
		multBy(tmpM4)
	}
	
	/** Mutliply this matrix by a rotation matrix defined by the given
	  * `angle` in radians, and the rotation `axis`.
	  * 
	  * The matrix is changed in place.
	  */
	def rotate(angle:Double, axis:NumberSeq3) { rotate(angle, axis.x, axis.y, axis.z) }	    
	
	/** Mutliply this matrix by a rotation matrix defined by the given
	  * `angle` in radians, and the rotation axis (`u`, `v`, `w`).
	  * 
	  * The matrix is changed in place.
	  */
	def rotate(angle:Double, u:Double, v:Double, w:Double) {
//		val R = newInstance(4, 4).asInstanceOf[NumberGrid4]
//		R.copy(this)
//		setIdentity
//		setRotation(angle, u, v, w)
//		multBy(R)
		if(tmpM4 eq null) tmpM4 = newInstance(4, 4).asInstanceOf[NumberGrid4]
		tmpM4.setRotation(angle, u, v, w)	// This fills the matrix, no need for setIdentity
		multBy(tmpM4)
	}

	/** Mutliply this matrix by a rotation matrix defined by the given
	  * `angle` in radians, and the rotation `axis`.
	  * 
	  * The matrix is changed in place.
	  */
	def rotate(angle:Double, axis:Axis) {
		axis match {
			case Axis.X => rotate(angle, 1, 0, 0)
			case Axis.Y => rotate(angle, 0, 1, 0)
			case Axis.Z => rotate(angle, 0, 0, 1)
			case _      => throw new RuntimeException("unknown axis")
		}
	}
	
	/**
	 * Store a perspective projection into this matrix.
	 * 
	 * This completely replace the current values.
	 * 
	 * @param left The left-most point of the projection plane (near).
	 * @param right The right-most point of the projection plane (near).
	 * @param bottom The bottom-most point of the projection plane (near).
	 * @param top The top-most point of the projection plane (near).
	 * @param near The near clipping plane.
	 * @param far The far clipping plane.
	 */
	def frustum(left:Double, right:Double, bottom:Double, top:Double, near:Double, far:Double) = {
		// | 2n/r-l      0       r+l/r-l     0        |
		// | 0         2n/t-b    t+b/t-b     0        |
		// | 0           0      -(f+n)/f-n   -2fn/f-n |
		// | 0           0         -1        0        |
		setRow(0, (2*near)/(right-left), 0,                     (right+left)/(right-left),  0)
		setRow(1, 0,                     (2*near)/(top-bottom), (top+bottom)/(top-bottom),  0)
		setRow(2, 0,                     0,                    -(far+near)/(far-near),     -(2*far*near)/(far-near))
		setRow(3, 0,                     0,                    -1,                          0)
	}
	
	/**
	 * Store a perspective projection into this matrix.
	 * 
	 * This completely replaces the current values.
	 * 
	 * @param fov The field of view angle in radians.
	 * @param aspect The aspect ratio (width / height).
	 * @param zNear The near clipping plane.
	 * @param zFar The far clipping plane.
	 * @return This matrix.
	 */
	def perspective(fov:Double, aspect:Double, zNear:Double, zFar:Double) = {
		// Found on http://www.opengl3.org/wiki/Tutorial3:_Rendering_3D_Objects_(C_/SDL)

		val range = (tan(fov * 0.00872664625) * zNear) // 0.00872664625 = Pi/360
		fill(0)
		data(0)  =  (2 * zNear) / ((range * aspect) - (-range * aspect))
		data(5)  =  (2 * zNear) / (2 * range)
		data(10) = -(zFar + zNear) / (zFar - zNear)
		data(11) = -1;
		data(14) = -(2 * zFar * zNear) / (zFar - zNear)
	}
	
	/** Store an orthographic projection into this matrix.
	  * 
	  * This completely replaces the current values.
	  *
	  * @param left The left-most point of the projection plane (near and far).
	  * @param right The right-most point of the projection plane (near and far).
	  * @param bottom The bottom-most point of the projection plane (near and far).
	  * @param top The top-most point of the projection plane (near and far).
	  * @param near The near clipping plane.
	  * @param far The far clipping plane.
	  */
	def orthographic(left:Double, right:Double, bottom:Double, top:Double, near:Double, far:Double) {
		setRow(0, 2/(right-left), 0,              0,            -((right+left)/(right-left)))
		setRow(1, 0,              2/(top-bottom), 0,            -((top+bottom)/(top-bottom)))
		setRow(2, 0,              0,              2/(far-near), -((far+near)/(far-near)))
		setRow(3, 0,              0,              0,            1)
	}
	
	/** Quick inverse of an orthographic projection as set in the 'othographic' method.
	  * 
	  * This completely replaces the current values.
	  */
	def inverseOrthographic(left:Double, right:Double, bottom:Double, top:Double, near:Double, far:Double) {
		setRow(0, (right-left)/2, 0,              0,            (right+left)/2)
		setRow(1, 0,              (top-bottom)/2, 0,            (top+bottom)/2)
		setRow(2, 0,              0,              (far-near)/2, (far+near)/2)
		setRow(3, 0,              0,              0,            1)
	}

	/**
	 * Compute the viewing transformation m to fit the camera location.
	 *
	 * The resulting transformation is then multiplied with this matrix.
	 * OpenGL calls the resulting matrix "Modelview".
	 *
	 * The calculations are done using the `eye` and `ctr` points
	 * indicating respectively the position of the viewer and the point in
	 * focus (looked at), and according to the `upv` vector indicating the direction
	 * of the top (allows to "bank" the camera).
	 */
	def lookAt(eye:NumberSeq3, ctr:NumberSeq3, upv:NumberSeq3, rh:Boolean) {
		val la = newInstance(4,4).asInstanceOf[NumberGrid4]
	    la.setLookAt(eye, ctr, upv, rh)
	    multBy(la)
	}

	def lookAt(eyeX:Double, eyeY:Double, eyeZ:Double,
	           ctrX:Double, ctrY:Double, ctrZ:Double,
	           upvX:Double, upvY:Double, upvZ:Double, rh:Boolean) {
		val la = newInstance(4,4).asInstanceOf[NumberGrid4]
		la.setLookAt(eyeX, eyeY, eyeZ, ctrX, ctrY, ctrZ, upvX, upvY, upvZ, rh)
		multBy(la)
	}
	
	/**
	 * Compute the viewing transformation m to fit the camera location.
	 *
	 * The resulting transformation is stored in this matrix. Thus, the viewing
	 * transformation should be multiplied by this. The previous contents of this are
	 * not used. OpenGL calls the resulting matrix "Modelview".
	 *
	 * The calculations are done using the `eye` and `ctr` points
	 * indicating respectively the position of the viewer and the point in
	 * focus (looked at), and according to the `upv` vector indicating the direction
	 * of the top (allows to "bank" the camera).
	 */
	def setLookAt(eye:NumberSeq3, ctr:NumberSeq3, upv:NumberSeq3, rh:Boolean) {
		setLookAt(eye.x, eye.y, eye.z,
		       ctr.x, ctr.y, ctr.z,
		       upv.x, upv.y, upv.z, rh)
	}
	
	def setLookAt(eyeX:Double, eyeY:Double, eyeZ:Double,
	           ctrX:Double, ctrY:Double, ctrZ:Double,
	           upvX:Double, upvY:Double, upvZ:Double, rh:Boolean) {
		// Aligning the camera on the view axis:
		//
		// We know one vector which determines the direction of view: z. We
		// know another vector that determines the up direction: y. They
		// form a plane, thus we can compute a third vector, x,
		// perpendicular to this plane with a cross product. Using the new
		// x and z which form a plane perpendicular to the plane defined by
		// y and z, we can compute anew y so that it is perpendicular to x
		// and z, and thus find an orthogonal matrix.
		//
		// We have thus determined an orthogonal matrix which is the
		// rotation matrix to apply in order to orient the camera.
		// It remains to translate this matrix so that the viewing eye is
		// at the origin, et voil� !
		//
		// This is described in "Computer Graphics Principles and Practice"
		// Chapter 5.7 (page 220 in my edition). The gluLookAt routine
		// also does this job, and inspired me. I used the Mesa
		// implementation. See the file "Mesa_top_dir/src-glu/glu.c".
		//
		// N.B.: Y must not be aligned (colinear) to Z, if this is the case,
		// we must add a small epsilon (or use another rotating technique !
		// quaternions ?).
	
		val mm = newInstance(4, 4).asInstanceOf[NumberGrid4]
		var x:Vector3 = null
		var y  = new Vector3(upvX, upvY, upvZ)
		var z  = new Vector3((eyeX - ctrX), (eyeY - ctrY), (eyeZ - ctrZ))
//		var z  = new Vector3((ctrX - eyeX), (ctrY - eyeY), (ctrZ - eyeZ))

		z.normalize
		x = y X z
		y = z X x
		x.normalize
		y.normalize

		// Set the translation:

		if(rh) {
		    // OpenGL
			mm.setRow(0, 1, 0, 0, -eyeX)
			mm.setRow(1, 0, 1, 0, -eyeY)
			mm.setRow(2, 0, 0, 1, -eyeZ)
			mm.setRow(3, 0, 0, 0, 1)
		} else {
			mm.setRow(0, 1, 0, 0, eyeX)
			mm.setRow(1, 0, 1, 0, eyeY)
			mm.setRow(2, 0, 0, 1, eyeZ)
			mm.setRow(3, 0, 0, 0, 1)
		}

		// Set the rotation:

		setRow(0, x.x, x.y, x.z, 0)
		setRow(1, y.x, y.y, y.z, 0)
		setRow(2, z.x, z.y, z.z, 0)
		setRow(3, 0,   0,   0,   1)

		// Compose the translation with the rotation:

		multBy(mm)
		//transpose()
	}
	
	/** 3x3 submatrix of this 4x4 matrix excepted the given `row` and ` col`umn. 
	  * 
	  * The submatrix is copied in the `result` parameter.
	  */
	def subMatrix(result:NumberGrid3, row:Int, col:Int) {
// TODO
	    for(c <- 0 until 3) {
	        for(r <- 0 until 3) {
	            result(r, c) = this(
	            		if(r>=row) r+1 else r,
	            		if(c>=col) c+1 else c
	            	)
	        }
	    }
	}
	
	/** Determinant of this. */
	def det:Double = {
	    var result = 0.0
	    var i = 1.0
	    val msub3 = Matrix3()
	   
// TODO 
	    for(n <- 0 until 4) {
	        subMatrix(msub3, 0, n)
	        result += this(0, n) * msub3.det * i
	        i *= -1
	    }
	    
	    result
	}
	
	/** Inverse of this matrix.
	  * 
	  * If the inverse does not exist, the identity is returned.
	  */
	def inverse():ReturnType = {
	    val result = newInstance(width, height)
	    val d      = det
	    
	    if(abs(d) > 0.0005) {
	        val mtmp = Matrix3()
	        var sign = 0.0
	   
// TODO     
	        for(c <- 0 until 4) {
	            for(r <- 0 until 4) {
	                sign = 1 - ((c + r) % 2) * 2
	                subMatrix(mtmp, c, r)
	                result(r, c) = (mtmp.det * sign ) / det
	            }
	        }
	    } else {
	        result.setIdentity
	    }
	    
	    result
	}
}
