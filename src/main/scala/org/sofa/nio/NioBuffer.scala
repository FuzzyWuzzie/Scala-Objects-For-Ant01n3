package org.sofa.nio

import scala.collection.mutable.ArrayBuffer
import language.implicitConversions


/** Base for any buffer.
  *
  * This interface does not allow to access the elements, only to know
  * their type. */
trait NioBuffer {

// Access

	/** Does this buffer contains bytes ? */
    def isByte:Boolean

    /** Does this buffer contains 32 bits signed integers ? */
    def isInt:Boolean

    /** Does this buffer contains 32 bits real numbers ? */
    def isFloat:Boolean

    /** Does this buffer contains 64 bits real numbers ? */
    def isDouble:Boolean

	/** The underlying buffer implementation.
	  * This depends on the backend used. You must cast it. */
	def buffer:AnyRef

	/** The maximum capacity of the buffer. */
    def capacity:Int

	/** The maximum capacity of the buffer. */
    def size:Int

    /** The maximum capacity of the buffer, synonym of `capacity()`. */
	def length:Int = capacity
}


/** Base for any buffer, defining the type and access to elements. */
trait TypedNioBuffer[T] extends NioBuffer with IndexedSeq[T] {

    /** The maximum capacity of the buffer, synonym of `capacity()` */
    override def size:Int = capacity

// Read

    /** The `i`-th element of the buffer, array-like access. */
	def apply(i:Int):T

// Write

	/** Assign `value` to the `i`-th cell, array-like write. */
	def update(i:Int, value:T)

	/** Copy the contents of another NIO buffer of the same kind into this. Sizes must coincide. */
	def copy(other:TypedNioBuffer[T])
	
	/** Copy the contents of an array buffer into this. Sizes must coincide. */
	def copy(data:ArrayBuffer[T])

	/** Copy the contents of an array into this. Sizes must coincide. */
	def copy(data:Array[T])
}


/** A buffer for bytes. */
abstract class ByteBuffer extends TypedNioBuffer[Byte] {}


/** A buffer for 32 bits integers. */
abstract class IntBuffer extends TypedNioBuffer[Int] {}


/** A buffer for 32 bits real numbers. */
abstract class FloatBuffer extends TypedNioBuffer[Float] {}


/** A buffer for 64 bits real numbers. */
abstract class DoubleBuffer extends TypedNioBuffer[Double] {}


/** NioBuffer companion object, acts as a factory for buffer implementations. */
object NioBuffer {
	/** The factory for buffers. */
	var factory:NioBufferFactory = new NioBufferFactoryJava()
}


/** Factory for [[NioBuffer]]s.
  *
  * Buffers may vary, depending on the system (desktop, Android) or the virtual machine
  * (JVM, Dalvik, Web). */
trait NioBufferFactory {
	def newByteBuffer(capacity:Int, direct:Boolean):ByteBuffer
	def newByteBuffer(capacity:Int, direct:Boolean, data:Array[Byte]):ByteBuffer
	
	def newIntBuffer(capacity:Int, direct:Boolean):IntBuffer
	def newIntBuffer(from:ByteBuffer):IntBuffer
	
	def newFloatBuffer(capacity:Int, direct:Boolean):FloatBuffer
	def newFloatBuffer(from:ByteBuffer):FloatBuffer
	
	def newDoubleBuffer(capacity:Int, direct:Boolean):DoubleBuffer
	def newDoubleBuffer(from:ByteBuffer):DoubleBuffer
}


/** A [[NioBuffer]] factory for JVM backends. */
class NioBufferFactoryJava extends NioBufferFactory {
	def newByteBuffer(capacity:Int, direct:Boolean) = new ByteBufferJava(capacity, direct)
	def newByteBuffer(capacity:Int, direct:Boolean, data:Array[Byte]) = new ByteBufferJava(capacity, direct, data)
	
	def newIntBuffer(capacity:Int, direct:Boolean) = new IntBufferJava(capacity, direct)
	def newIntBuffer(from:ByteBuffer):IntBuffer = new IntBufferJava(from)
	
	def newFloatBuffer(capacity:Int, direct:Boolean) = new FloatBufferJava(capacity, direct)
	def newFloatBuffer(from:ByteBuffer):FloatBuffer = new FloatBufferJava(from)
	
	def newDoubleBuffer(capacity:Int, direct:Boolean) = new DoubleBufferJava(capacity, direct)
	def newDoubleBuffer(from:ByteBuffer):DoubleBuffer = new DoubleBufferJava(from)
}


// ---------------------------------------------------------


object ByteBuffer {
    def apply(capacity:Int) = NioBuffer.factory.newByteBuffer(capacity, true)
    def apply(capacity:Int, direct:Boolean) = NioBuffer.factory.newByteBuffer(capacity, direct)
    def apply(data:Array[Byte], direct:Boolean) = NioBuffer.factory.newByteBuffer(data.size, direct, data) 
    def apply(direct:Boolean, data:Byte*) = NioBuffer.factory.newByteBuffer(data.size, direct, data.toArray)
    def apply(from:ArrayBuffer[Byte], direct:Boolean) = NioBuffer.factory.newByteBuffer(from.size, direct, from.toArray)
}


object IntBuffer {
    def apply(capacity:Int) = NioBuffer.factory.newIntBuffer(capacity, true)
    def apply(capacity:Int, direct:Boolean) = NioBuffer.factory.newIntBuffer(capacity, direct)
    def apply(data:Array[Int]) = { val b = NioBuffer.factory.newIntBuffer(data.size, true); b.copy(data); b } 
    def apply(data:Int*) = { val b = NioBuffer.factory.newIntBuffer(data.size, true); b.copy(data.toArray); b }
    def apply(from:ByteBuffer) = NioBuffer.factory.newIntBuffer(from)
    def apply(from:ArrayBuffer[Int]) = { val b = NioBuffer.factory.newIntBuffer(from.size, true); b.copy(from.toArray); b }
}


object FloatBuffer {
    def apply(capacity:Int) = NioBuffer.factory.newFloatBuffer(capacity, true)
    def apply(capacity:Int, direct:Boolean) = NioBuffer.factory.newFloatBuffer(capacity, direct)
    def apply(data:Array[Float]) = { val b = NioBuffer.factory.newFloatBuffer(data.size, true); b.copy(data); b } 
    def apply(data:Float*) = { val b = NioBuffer.factory.newFloatBuffer(data.size, true); b.copy(data.toArray); b }
    def apply(from:ByteBuffer) = NioBuffer.factory.newFloatBuffer(from)
    def apply(from:ArrayBuffer[Float]) = { val b = NioBuffer.factory.newFloatBuffer(from.size, true); b.copy(from.toArray); b }
}


object DoubleBuffer {
    def apply(capacity:Int) = NioBuffer.factory.newDoubleBuffer(capacity, true)
    def apply(capacity:Int, direct:Boolean) = NioBuffer.factory.newDoubleBuffer(capacity, direct)
    def apply(data:Array[Double]) = { val b = NioBuffer.factory.newDoubleBuffer(data.size, true); b.copy(data); b } 
    def apply(data:Double*) = {val b = NioBuffer.factory.newDoubleBuffer(data.size, true); b.copy(data.toArray); b }
    def apply(from:ByteBuffer) = NioBuffer.factory.newDoubleBuffer(from)
    def apply(from:ArrayBuffer[Double]) = { val b = NioBuffer.factory.newDoubleBuffer(from.size, true); b.copy(from.toArray); b }
}


// ---------------------------------------------------------


class ByteBufferJava(var capacity:Int, direct:Boolean, data:Array[Byte]) extends ByteBuffer {
	var buf:java.nio.ByteBuffer = if(direct) {
			var n = if((data ne null) && (capacity < data.length)) data.length else capacity
            val b = java.nio.ByteBuffer.allocateDirect(n)
            if(data ne null) copy(data)
            b
        } else {
            if(data ne null) {
                java.nio.ByteBuffer.wrap(data)
            } else {
                java.nio.ByteBuffer.allocate(capacity)
            }
        }
	nativeOrder
	def this(data:Array[Byte], direct:Boolean) { this(data.size, direct, data) }
    def this(capacity:Int, direct:Boolean) { this(capacity, direct, null) }
	def update(i:Int, value:Byte):Unit = buf.put(i, value)
	def copy(other:TypedNioBuffer[Byte]) = other match {
		case bbj:ByteBufferJava => {
			buf.rewind
			bbj.buf.rewind
			buf.put(bbj.buf)
			buf.rewind
		}
		case _ => {
			throw new RuntimeException("copy of non compatible buffers %s -> %s".format(this.getClass.getName, other.getClass.getName))
		}
	}
	def copy(data:ArrayBuffer[Byte]) { buf.rewind; buf.put(data.toArray[Byte]); buf.rewind }
	def copy(data:Array[Byte]) = { buf.rewind; buf.put(data); buf.rewind }
	def nativeOrder() = buf.order(java.nio.ByteOrder.nativeOrder())
	def bigEndian() = buf.order(java.nio.ByteOrder.BIG_ENDIAN)
	def littleEndian() = buf.order(java.nio.ByteOrder.LITTLE_ENDIAN)
    def isByte:Boolean = true
    def isInt:Boolean = false
    def isFloat:Boolean = false
    def isDouble:Boolean = false
    def apply(i:Int):Byte = buf.get(i)
    def buffer:AnyRef = { buf.rewind; buf }
}


class IntBufferJava(var capacity:Int, direct:Boolean=true) extends IntBuffer {
	var buf:java.nio.IntBuffer = if(capacity>0) {
		if(direct) {
            java.nio.ByteBuffer.allocateDirect(capacity*4).order(java.nio.ByteOrder.nativeOrder).asIntBuffer
        } else {
            java.nio.IntBuffer.allocate(capacity)
        }
    } else {
    	null
    }
	def this(from:ByteBuffer) {
		this(0)
		from match {
			case bbf:ByteBufferJava => {
				buf = bbf.buf.asIntBuffer
				capacity = buf.capacity / 4
			}
			case _ => {
				throw new RuntimeException("non compatible buffer %s expecting ByteBufferJava".format(from.getClass.getName))
			}
		}
	}
	def update(i:Int, value:Int):Unit = buf.put(i, value)
	def copy(other:TypedNioBuffer[Int]) = other match {
		case ibj:IntBufferJava => {
			buf.rewind
			ibj.buf.rewind
			buf.put(ibj.buf)
			buf.rewind 
		}
		case _ => {
			throw new RuntimeException("copy of non compatible buffers %s -> %s".format(this.getClass.getName, other.getClass.getName))			
		}
	}
	def copy(data:Array[Int]) = { buf.rewind; buf.put(data); buf.rewind }
	def copy(data:ArrayBuffer[Int]) = { buf.rewind; buf.put(data.toArray); buf.rewind }
    def isByte:Boolean = false
    def isInt:Boolean = true
    def isFloat:Boolean = false
    def isDouble:Boolean = false
    def apply(i:Int):Int = buf.get(i)
    def buffer:AnyRef = { buf.rewind; buf }
}


class FloatBufferJava(var capacity:Int, direct:Boolean=true) extends FloatBuffer {
	var buf:java.nio.FloatBuffer = if(capacity>0) {
		if(direct) {
            java.nio.ByteBuffer.allocateDirect(capacity*4).order(java.nio.ByteOrder.nativeOrder).asFloatBuffer
        } else {
            java.nio.FloatBuffer.allocate(capacity)
        }
    } else {
    	null
    }
	def this(from:ByteBuffer) {
		this(0)
		from match {
			case bbf:ByteBufferJava => {
				buf = bbf.buf.asFloatBuffer
				capacity = buf.capacity / 4
			}
			case _ => {
				throw new RuntimeException("non compatible buffer %s expecting ByteBufferJava".format(from.getClass.getName))
			}
		}
	}
	def update(i:Int, value:Float):Unit = buf.put(i, value)
	def copy(other:TypedNioBuffer[Float]) = other match {
		case fbj:FloatBufferJava => {
			buf.rewind
			fbj.buf.rewind
			buf.put(fbj.buf)
			buf.rewind 
		}
		case _ => {
			throw new RuntimeException("copy of non compatible buffers %s -> %s".format(this.getClass.getName, other.getClass.getName))			
		}
	}
	def copy(data:Array[Float]) = { buf.rewind; buf.put(data); buf.rewind }
	def copy(data:ArrayBuffer[Float]) = { buf.rewind; buf.put(data.toArray); buf.rewind }
    def isByte:Boolean = false
    def isInt:Boolean = false
    def isFloat:Boolean = true
    def isDouble:Boolean = false
    def apply(i:Int):Float = buf.get(i)
    def buffer:AnyRef = { buf.rewind; buf }
}


class DoubleBufferJava(var capacity:Int, direct:Boolean=true) extends DoubleBuffer {
	var buf:java.nio.DoubleBuffer = if(capacity>0) {
		if(direct) {
            java.nio.ByteBuffer.allocateDirect(capacity*8).order(java.nio.ByteOrder.nativeOrder).asDoubleBuffer
        } else {
            java.nio.DoubleBuffer.allocate(capacity)
        }
    } else {
    	null
	}
    def this(from:ByteBuffer) {
		this(0)
		from match {
			case bbf:ByteBufferJava => {
				buf = bbf.buf.asDoubleBuffer
				capacity = buf.capacity / 8
			}
			case _ => {
				throw new RuntimeException("non compatible buffer %s expecting ByteBufferJava".format(from.getClass.getName))
			}
		}
	}
	def update(i:Int, value:Double):Unit = buf.put(i, value)
	def copy(other:TypedNioBuffer[Double]) = other match {
		case dbj:DoubleBufferJava => {
			buf.rewind
			dbj.buf.rewind
			buf.put(dbj.buf)
			buf.rewind 
		}
		case _ => {
			throw new RuntimeException("copy of non compatible buffers %s -> %s".format(this.getClass.getName, other.getClass.getName))			
		}
	}
	def copy(data:Array[Double]) = { buf.rewind; buf.put(data); buf.rewind }
	def copy(data:ArrayBuffer[Double]) = { buf.rewind; buf.put(data.toArray); buf.rewind }
    def isByte:Boolean = false
    def isInt:Boolean = false
    def isFloat:Boolean = false
    def isDouble:Boolean = true
    def apply(i:Int):Double = buf.get(i)
    def buffer:AnyRef = { buf.rewind; buf }
}