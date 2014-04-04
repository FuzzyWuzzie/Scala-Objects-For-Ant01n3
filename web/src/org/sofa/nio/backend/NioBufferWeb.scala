package org.sofa.nio.backend

import scala.scalajs.js
import org.sofa.nio._
import org.scalajs.dom

class ArrayBuffer(length:js.Number) extends js.Object {
	val byteLength:js.Number = ???
	def slide(begin:js.Number, end:js.Number):ArrayBuffer = ???
}


trait ArrayBufferView extends js.Object {
    val buffer:ArrayBuffer = ???
    val byteOffset:js.Number = ???
    val byteLength:js.Number = ???
}


class DataView(buffer:ArrayBuffer, byteOffset:js.Number, byteLength:js.Number) extends ArrayBufferView {
    def getInt8(byteOffset:js.Number):js.Number = ???
    def getUint8(byteOffset:js.Number):js.Number = ???
    def getInt16(byteOffset:js.Number, littleEndian:js.Boolean):js.Number = ???
    def getUint16(byteOffset:js.Number, littleEndian:js.Boolean):js.Number = ???
    def getInt32(byteOffset:js.Number, littleEndian:js.Boolean):js.Number = ???
    def getUint32(byteOffset:js.Number, littleEndian:js.Boolean):js.Number = ???
    def getFloat32(byteOffset:js.Number, littleEndian:js.Boolean):js.Number = ???
    def getFloat64(byteOffset:js.Number, littleEndian:js.Boolean):js.Number = ???

    def setInt8(byteOffset:js.Number, value:js.Number) = ???
    def setUint8(byteOffset:js.Number, value:js.Number) = ???
    def setInt16(byteOffset:js.Number, value:js.Number, littleEndian:js.Boolean) = ???
    def setUint16(byteOffset:js.Number, value:js.Number, littleEndian:js.Boolean) = ???
    def setInt32(byteOffset:js.Number, value:js.Number, littleEndian:js.Boolean) = ???
    def setUint32(byteOffset:js.Number, value:js.Number, littleEndian:js.Boolean) = ???
    def setFloat32(byteOffset:js.Number, value:js.Number, littleEndian:js.Boolean) = ???
    def setFloat64(byteOffset:js.Number, value:js.Number, littleEndian:js.Boolean) = ???
}


class Uint8Array(len:js.Number) extends ArrayBufferView {
    val BYTES_PER_ELEMENT:js.Number = ???

    val length:js.Number = ???

    def this(array:Uint8Array) = {this(0)} //??? Cannot use "???", to keep the compiler happy, we have to use "this". 
    def this(array:js.Array[js.Number]) = {this(0)} // ???
    def this(buffer:ArrayBuffer, byteOffset:js.Number, length:js.Number) = {this(0)} // ??? 

    @scalajs.js.annotation.JSBracketAccess
    def get(index:js.Number):js.Number = ???
    @scalajs.js.annotation.JSBracketAccess
    def set(index:js.Number, value:js.Number) = ???
    def set(array:Uint8Array, offset:js.Number) = ???
    def set(array:js.Array[js.Number], offset:js.Number) = ???
    def subarray(begin:js.Number, end:js.Number):Uint8Array = ???
}


class Int32Array(len:js.Number) extends ArrayBufferView {
    val BYTES_PER_ELEMENT:js.Number = ???

    val length:js.Number = ???

    def this(array:Int32Array) = {this(0)} //??? Cannot use "???", to keep the compiler happy, we have to use "this". 
    def this(array:js.Array[js.Number]) = {this(0)} // ???
    def this(buffer:ArrayBuffer, byteOffset:js.Number, length:js.Number) = {this(0)} // ??? 

    @scalajs.js.annotation.JSBracketAccess
    def get(index:js.Number):js.Number = ???
    @scalajs.js.annotation.JSBracketAccess
    def set(index:js.Number, value:js.Number) = ???
    def set(array:Int32Array, offset:js.Number) = ???
    def set(array:js.Array[js.Number], offset:js.Number) = ???
    def subarray(begin:js.Number, end:js.Number):Int32Array = ???
}


class Float32Array(len:js.Number) extends ArrayBufferView {
    val BYTES_PER_ELEMENT:js.Number = ???

    val length:js.Number = ???

    def this(array:Float32Array) = {this(0)} //??? Cannot use "???", to keep the compiler happy, we have to use "this". 
    def this(array:js.Array[js.Number]) = {this(0); dom.alert("Float32Array !!!")} // ???
    def this(buffer:ArrayBuffer, byteOffset:js.Number, length:js.Number) = {this(0)} // ??? 

    @scalajs.js.annotation.JSBracketAccess
    def get(index:js.Number):js.Number = ???
    @scalajs.js.annotation.JSBracketAccess
    def set(index:js.Number, value:js.Number) = ???
    def set(array:Float32Array, offset:js.Number) = ???
    def set(array:js.Array[js.Number], offset:js.Number) = ???
    def subarray(begin:js.Number, end:js.Number):Float32Array = ???
}


/** A [[NioBuffer]] factory for JVM backends. */
class NioBufferFactoryWeb extends NioBufferFactory {
	def newByteBuffer(capacity:Int, direct:Boolean) = new ByteBufferWeb(capacity)
	def newByteBuffer(capacity:Int, direct:Boolean, data:Array[Byte]) = new ByteBufferWeb(capacity, data)
	
	def newIntBuffer(capacity:Int, direct:Boolean) = throw new RuntimeException("TODO")//new IntBufferWeb(capacity, direct)
	def newIntBuffer(from:ByteBuffer):IntBuffer = throw new RuntimeException("TODO")//new IntBufferWeb(from)
	
	def newFloatBuffer(capacity:Int, direct:Boolean) = new FloatBufferWeb(capacity)
	def newFloatBuffer(from:ByteBuffer):FloatBuffer = new FloatBufferWeb(from)
	
	def newDoubleBuffer(capacity:Int, direct:Boolean) = throw new RuntimeException("TODO")//new DoubleBufferWeb(capacity, direct)
	def newDoubleBuffer(from:ByteBuffer):DoubleBuffer = throw new RuntimeException("TODO")//new DoubleBufferWeb(from)
}



/** Equivalent of a Uint8Array. */
class ByteBufferWeb(var capacity:Int, data:Array[Byte]) extends ByteBuffer {
	var buf:Uint8Array = if(data ne null) {
        	new Uint8Array(data.asInstanceOf[Array[js.Number]])//new ArrayBuffer(capacity), 0, capacity)
        } else {
        	new Uint8Array(new ArrayBuffer(capacity), 0, capacity)
        }
//	nativeOrder
	def this(data:Array[Byte]) { this(data.size, data) }
    def this(capacity:Int) { this(capacity, null) }
	def update(i:Int, value:Byte):Unit = buf.set(i, value)
	def copy(other:TypedNioBuffer[Byte]) = other match {
		case bbj:ByteBufferWeb => { buf.set(bbj.buf, 0) }
		case _ => { throw new RuntimeException("copy of non compatible buffers %s -> %s".format(this.getClass.getName, other.getClass.getName)) }
	}
	def copy(data:scala.collection.mutable.ArrayBuffer[Byte]) {
	 	//buf.set(data.toArray[Byte].asInstanceOf[js.Array[js.Number]], 0) }
		var i = 0; while(i < data.length) { buf.set(i, data(i)); i+=1 }
	}	 	
	def copy(data:Array[Byte]) = { 
//		buf.set(data.asInstanceOf[js.Array[js.Number]], 0) }
		// If I understand well, it is not possible to cast an Array[T] into a js.Array[T]
		// They are not represented the same way, therefore we need to copy each element 
		// by ourself.
		var i = 0; while(i < data.length) { buf.set(i, data(i)); i+=1 }
	}
//	def nativeOrder() = buf.order(java.nio.ByteOrder.nativeOrder())
//	def bigEndian() = buf.order(java.nio.ByteOrder.BIG_ENDIAN)
//	def littleEndian() = buf.order(java.nio.ByteOrder.LITTLE_ENDIAN)
    def isByte:Boolean = true
    def isInt:Boolean = false
    def isFloat:Boolean = false
    def isDouble:Boolean = false
    def apply(i:Int):Byte = buf.get(i).toByte
    def buffer:AnyRef = buf
}


/** Equivalent of A Int32Array. */
class IntBufferWeb(var capacity:Int) extends IntBuffer {
	var buf = if(capacity>0) {
		new Int32Array(new ArrayBuffer(capacity*4), 0, capacity)
    } else {
    	null
    }
	def this(from:ByteBuffer) {
		this(0)
		from match {
			case bbf:ByteBufferWeb => {
				buf = new Int32Array(bbf.buf.buffer, 0, bbf.capacity/4)
				capacity = bbf.capacity / 4
			}
			case _ => {
				throw new RuntimeException("non compatible buffer %s expecting ByteBufferWeb".format(from.getClass.getName))
			}
		}
	}
	def update(i:Int, value:Int):Unit = buf.set(i, value)
	def copy(other:TypedNioBuffer[Int]) = other match {
		case ibj:IntBufferWeb => { buf.set(ibj.buf, 0) }
		case _ => {throw new RuntimeException("copy of non compatible buffers %s -> %s".format(this.getClass.getName, other.getClass.getName)) }
	}
	def copy(data:Array[Int]) = {
		//buf.set(data.asInstanceOf[js.Array[js.Number]], 0) }
		// If I understand well, it is not possible to cast an Array[T] into a js.Array[T]
		// They are not represented the same way, therefore we need to copy each element 
		// by ourself.
		var i = 0; while(i < data.length) { buf.set(i, data(i)); i+=1 }
	}
	def copy(data:scala.collection.mutable.ArrayBuffer[Int]) = {
		//buf.set(data.toArray.asInstanceOf[js.Array[js.Number]], 0) }
				// If I understand well, it is not possible to cast an Array[T] into a js.Array[T]
		// They are not represented the same way, therefore we need to copy each element 
		// by ourself.
		var i = 0; while(i < data.length) { buf.set(i, data(i)); i+=1 }
	}
    def isByte:Boolean = false
    def isInt:Boolean = true
    def isFloat:Boolean = false
    def isDouble:Boolean = false
    def apply(i:Int):Int = buf.get(i).toInt
    def buffer:AnyRef = buf
}


/** Equivalent of a Float32Array. */
class FloatBufferWeb(var capacity:Int) extends FloatBuffer {
	var buf = if(capacity>0) {
		new Float32Array(new ArrayBuffer(capacity*4), 0, capacity)
    } else {
    	null
    }
	def this(from:ByteBuffer) {
		this(0)
		from match {
			case bbf:ByteBufferWeb => {
				buf = new Float32Array(bbf.buf.buffer, 0, bbf.capacity/4)
				capacity = bbf.capacity / 4
			}
			case _ => {
				throw new RuntimeException("non compatible buffer %s expecting ByteBufferWeb".format(from.getClass.getName))
			}
		}
	}
	def update(i:Int, value:Float):Unit = buf.set(i, value)
	def copy(other:TypedNioBuffer[Float]) = other match {
		case fbj:FloatBufferWeb => { buf.set(fbj.buf, 0) }
		case _ => { throw new RuntimeException("copy of non compatible buffers %s -> %s".format(this.getClass.getName, other.getClass.getName)) }
	}
	def copy(data:Array[Float]) = {
		// If I understand well, it is not possible to cast an Array[T] into a js.Array[T]
		// They are not represented the same way, therefore we need to copy each element 
		// by ourself.
		var i = 0; while(i < data.length) { buf.set(i, data(i)); i+=1 }
	  }
	def copy(data:scala.collection.mutable.ArrayBuffer[Float]) = { 
		var i = 0; while(i < data.size) { buf.set(i, data(i)); i+= 1 }
		//buf.set(data.toArray.asInstanceOf[js.Array[js.Number]], 0) 
	}
    def isByte:Boolean = false
    def isInt:Boolean = false
    def isFloat:Boolean = true
    def isDouble:Boolean = false
    def apply(i:Int):Float = buf.get(i).toFloat
    def buffer:AnyRef = buf
}


// /** Equivalent of Float64Array. */
// class DoubleBufferWeb(var capacity:Int, direct:Boolean=true) extends DoubleBuffer {
// 	var buf:java.nio.DoubleBuffer = if(capacity>0) {
// 		if(direct) {
//             java.nio.ByteBuffer.allocateDirect(capacity*8).order(java.nio.ByteOrder.nativeOrder).asDoubleBuffer
//         } else {
//             java.nio.DoubleBuffer.allocate(capacity)
//         }
//     } else {
//     	null
// 	}
//     def this(from:ByteBuffer) {
// 		this(0)
// 		from match {
// 			case bbf:ByteBufferWeb => {
// 				buf = bbf.buf.asDoubleBuffer
// 				capacity = buf.capacity / 8
// 			}
// 			case _ => {
// 				throw new RuntimeException("non compatible buffer %s expecting ByteBufferWeb".format(from.getClass.getName))
// 			}
// 		}
// 	}
// 	def update(i:Int, value:Double):Unit = buf.put(i, value)
// 	def copy(other:TypedNioBuffer[Double]) = other match {
// 		case dbj:DoubleBufferWeb => {
// 			buf.rewind
// 			dbj.buf.rewind
// 			buf.put(dbj.buf)
// 			buf.rewind 
// 		}
// 		case _ => {
// 			throw new RuntimeException("copy of non compatible buffers %s -> %s".format(this.getClass.getName, other.getClass.getName))			
// 		}
// 	}
// 	def copy(data:Array[Double]) = { buf.rewind; buf.put(data); buf.rewind }
// 	def copy(data:ArrayBuffer[Double]) = { buf.rewind; buf.put(data.toArray); buf.rewind }
//     def isByte:Boolean = false
//     def isInt:Boolean = false
//     def isFloat:Boolean = false
//     def isDouble:Boolean = true
//     def apply(i:Int):Double = buf.get(i)
//     def buffer:AnyRef = { buf.rewind; buf }
// }