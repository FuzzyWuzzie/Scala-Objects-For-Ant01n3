package org.sofa.math

import org.sofa.nio._

abstract class Matrix(val width:Int, val height:Int) extends NumberGrid {
}

class ArrayMatrix(width:Int, height:Int) extends Matrix(width, height) {
    type ArrayLike = Array[Double]
    type ReturnType = ArrayMatrix
    val data = new Array[Double](width*height)
    def newInstance(w:Int, h:Int) = new ArrayMatrix(w, h)
    override def toDoubleArray = data 
}

class NioBufferMatrix(width:Int, height:Int) extends Matrix(width, height) {
    type ArrayLike = DoubleBuffer
    type ReturnType = NioBufferMatrix
    val data = new DoubleBuffer(width*height)
    def newInstance(w:Int, h:Int) = new NioBufferMatrix(w, h)
    override def toDoubleBuffer = { data.rewind; data }
}

object Matrix {
    def apply(rows:Seq[Double]*):ArrayMatrix = ArrayMatrix(rows:_*)
    def apply(other:ArrayMatrix):ArrayMatrix = ArrayMatrix(other)
    def apply(width:Int, height:Int):ArrayMatrix = ArrayMatrix(width, height) 
}

object ArrayMatrix {
    def apply(rows:Seq[Double]*):ArrayMatrix = {
        val h = rows.size
        val w = rows.max(new Ordering[Seq[Double]]{ def compare(a:Seq[Double], b:Seq[Double]):Int = { a.size-b.size } }).size
        val result = new ArrayMatrix(w, h)
        var r = 0
        
        rows.foreach { row =>
            for(c <- 0 until row.size) {
            	result(r,c) = row(c)
            }
            r += 1
        }
        
        result
    }
    def apply(other:ArrayMatrix):ArrayMatrix = {
        val result = new ArrayMatrix(other.width, other.height)
        result.copy(other)
        result
    }
    def apply(width:Int, height:Int):ArrayMatrix = {
        val result = new ArrayMatrix(width, height)
        result.setIdentity
        result
    }
}

object NioBufferMatrix {
    def apply(rows:Seq[Double]*):ArrayMatrix = {
        val h = rows.size
        val w = rows.max(new Ordering[Seq[Double]]{ def compare(a:Seq[Double], b:Seq[Double]):Int = { a.size-b.size } }).size
        val result = new ArrayMatrix(w, h)
        var r = 0
        
        rows.foreach { row =>
            for(c <- 0 until row.size) {
            	result(r,c) = row(c)
            }
            r += 1
        }
        
        result
    }
    def apply(other:ArrayMatrix):ArrayMatrix = {
        val result = new ArrayMatrix(other.width, other.height)
        result.copy(other)
        result
    }
    def apply(width:Int, height:Int):NioBufferMatrix = {
        val result = new NioBufferMatrix(width, height)
        result.setIdentity
        result
    }
}

//==================================================================================================

abstract class Matrix3 extends NumberGrid3 {
}

class ArrayMatrix3 extends Matrix3 {
    type ArrayLike = Array[Double]
    type ReturnType = ArrayMatrix3
    val data = new Array[Double](9)
    def newInstance(w:Int, h:Int) = new ArrayMatrix3()
    override def toDoubleArray = data 
}

class NioBufferMatrix3 extends Matrix3 {
    type ArrayLike = DoubleBuffer
    type ReturnType = NioBufferMatrix3
    val data = new DoubleBuffer(9)
    def newInstance(w:Int, h:Int) = new NioBufferMatrix3()    
    override def toDoubleBuffer = { data.rewind; data }
}

object Matrix3 {
    def apply(row0:(Double,Double,Double), row1:(Double,Double,Double), row2:(Double,Double,Double)):ArrayMatrix3 = ArrayMatrix3(row0, row1, row2)
    def apply(other:ArrayMatrix3):ArrayMatrix3 = ArrayMatrix3(other)
    def apply():ArrayMatrix3 = ArrayMatrix3()
}

object ArrayMatrix3 {
    def apply(row0:(Double,Double,Double), row1:(Double,Double,Double), row2:(Double,Double,Double)):ArrayMatrix3 = {
    	val result = new ArrayMatrix3()
    	result.row0 = row0
    	result.row1 = row1 
    	result.row2 = row2
    	result
    }
    def apply(other:ArrayMatrix3):ArrayMatrix3 = {
        val result = new ArrayMatrix3()
        result.copy(other)
        result
    }
    def apply():ArrayMatrix3 = {
        val result = new ArrayMatrix3()
        result.setIdentity
        result
    }
}

object NioBufferMatrix3 {
    def apply(row0:(Double,Double,Double), row1:(Double,Double,Double), row2:(Double,Double,Double)):NioBufferMatrix3 = {
    	val result = new NioBufferMatrix3()
    	result.row0 = row0
    	result.row1 = row1 
    	result.row2 = row2
    	result
    }
    def apply(other:ArrayMatrix3):NioBufferMatrix3 = {
        val result = new NioBufferMatrix3()
        result.copy(other)
        result
    }
    def apply():NioBufferMatrix3 = {
        val result = new NioBufferMatrix3()
        result.setIdentity
        result
    }
}

//==================================================================================================

abstract class Matrix4 extends NumberGrid4 {
    def top3x3:Matrix3
}

class ArrayMatrix4 extends Matrix4 {
    type ArrayLike = Array[Double]
    type ReturnType = ArrayMatrix4
    val data = new Array[Double](16)
    def newInstance(w:Int, h:Int) = new ArrayMatrix4()
    override def toDoubleArray = data
    def top3x3:Matrix3 = ArrayMatrix3((this(0,0), this(0,1), this(0,2)),
                                      (this(1,0), this(1,1), this(1,2)),
                                      (this(2,0), this(2,1), this(2,2)))
}

class NioBufferMatrix4 extends Matrix4 {
    type ArrayLike = DoubleBuffer
    type ReturnType = NioBufferMatrix4
    val data = new DoubleBuffer(16)
    def newInstance(w:Int, h:Int) = new NioBufferMatrix4()    
    override def toDoubleBuffer = { data.rewind; data }
    def top3x3:Matrix3 = NioBufferMatrix3((this(0,0), this(0,1), this(0,2)),
                                          (this(1,0), this(1,1), this(1,2)),
                                          (this(2,0), this(2,1), this(2,2)))
}

object Matrix4 {
    def apply(row0:(Double,Double,Double,Double),
              row1:(Double,Double,Double,Double),
              row2:(Double,Double,Double,Double),
              row3:(Double,Double,Double,Double)):ArrayMatrix4 = ArrayMatrix4(row0, row1, row2, row3)
    def apply(other:ArrayMatrix4):ArrayMatrix4 = ArrayMatrix4(other)
    def apply():ArrayMatrix4 = ArrayMatrix4()
}

object ArrayMatrix4 {
    def apply(row0:(Double,Double,Double,Double),
              row1:(Double,Double,Double,Double),
              row2:(Double,Double,Double,Double),
              row3:(Double,Double,Double,Double)):ArrayMatrix4 = {
    	val result = new ArrayMatrix4()
    	result.row0 = row0
    	result.row1 = row1 
    	result.row2 = row2
    	result.row3 = row3
    	result
    }
    def apply(other:ArrayMatrix4):ArrayMatrix4 = {
        val result = new ArrayMatrix4()
        result.copy(other)
        result
    }
    def apply():ArrayMatrix4 = {
        val result = new ArrayMatrix4()
        result.setIdentity
        result
    }
}

object NioBufferMatrix4 {
    def apply(row0:(Double,Double,Double,Double),
              row1:(Double,Double,Double,Double),
              row2:(Double,Double,Double,Double),
              row3:(Double,Double,Double,Double)):NioBufferMatrix4 = {
    	val result = new NioBufferMatrix4()
    	result.row0 = row0
    	result.row1 = row1 
    	result.row2 = row2
    	result.row3 = row3
    	result
    }
    def apply(other:ArrayMatrix3):NioBufferMatrix4 = {
        val result = new NioBufferMatrix4()
        result.copy(other)
        result
    }
    def apply():NioBufferMatrix4 = {
        val result = new NioBufferMatrix4()
        result.setIdentity
        result
    }
}
