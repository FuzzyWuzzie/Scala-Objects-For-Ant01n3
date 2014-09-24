package org.sofa.math

import org.sofa.nio._


class Matrix(val width:Int, val height:Int) extends NumberGrid {
    type ReturnType = Matrix
    val data = new Array[Double](width*height)
    def newInstance(w:Int, h:Int) = new Matrix(w, h)
}


object Matrix {
    def apply(rows:Seq[Double]*):Matrix = {
        val h = rows.size
// TODO
        val w = rows.max(new Ordering[Seq[Double]]{ def compare(a:Seq[Double], b:Seq[Double]):Int = { a.size-b.size } }).size
        val result = new Matrix(w, h)
        var r = 0
        
// TODO
        rows.foreach { row =>
            for(c <- 0 until row.size) {
            	result(r,c) = row(c)
            }
            r += 1
        }
        
        result
    }
    def apply(other:Matrix):Matrix = {
        val result = new Matrix(other.width, other.height)
        result.copy(other)
        result
    }
    def apply(width:Int, height:Int):Matrix = {
        val result = new Matrix(width, height)
        result.setIdentity
        result
    }
}


//==================================================================================================


class Matrix3 extends NumberGrid3 {
    type ReturnType = Matrix3
    //val data = new Array[Double](9)
    def newInstance(w:Int, h:Int) = new Matrix3()
}


object Matrix3 {
    def apply(row0:(Double,Double,Double), row1:(Double,Double,Double), row2:(Double,Double,Double)):Matrix3 = {
    	val result = new Matrix3()
    	result.row0 = row0
    	result.row1 = row1 
    	result.row2 = row2
    	result
    }
    def apply(other:Matrix3):Matrix3 = {
        val result = new Matrix3()
        result.copy(other)
        result
    }
    def apply():Matrix3 = {
        val result = new Matrix3()
        result.setIdentity
        result
    }
}


//==================================================================================================


class Matrix4 extends NumberGrid4 {
    type ReturnType = Matrix4
    //val data = new Array[Double](16)
    def newInstance(w:Int, h:Int) = new Matrix4()
    def top3x3:Matrix3 = {
    	val m    = new Matrix3()
    	val odat = m.data
    	val dat  = data

    	odat(0) = dat(0);  odat(4) = dat(4);  odat(8)  = dat(8)
    	odat(1) = dat(1);  odat(5) = dat(5);  odat(9)  = dat(9)
    	odat(2) = dat(2);  odat(6) = dat(6);  odat(10) = dat(10)
    	odat(3) = dat(3);  odat(7) = dat(7);  odat(11) = dat(11)

    	m
    }

    // Matrix3((this(0,0), this(0,1), this(0,2)),
    //                              (this(1,0), this(1,1), this(1,2)),
    //                              (this(2,0), this(2,1), this(2,2)))
}


object Matrix4 {
    def apply(row0:(Double,Double,Double,Double),
              row1:(Double,Double,Double,Double),
              row2:(Double,Double,Double,Double),
              row3:(Double,Double,Double,Double)):Matrix4 = {
    	val result = new Matrix4()
    	result.row0 = row0
    	result.row1 = row1 
    	result.row2 = row2
    	result.row3 = row3
    	result
    }
    def apply(data:Array[Float], offset:Int):Matrix4 = {
    	val result = new Matrix4()
    	result.copy(data, offset)
    	result
    }
    def apply(data:Array[Double], offset:Int):Matrix4 = {
    	val result = new Matrix4()
    	result.copy(data, offset)
    	result
    }
    def apply(other:Matrix4):Matrix4 = {
        val result = new Matrix4()
        result.copy(other)
        result
    }
    def apply():Matrix4 = {
        val result = new Matrix4()
        result.setIdentity
        result
    }
}