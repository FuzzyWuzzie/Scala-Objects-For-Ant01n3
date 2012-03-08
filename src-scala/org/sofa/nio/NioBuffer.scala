package org.sofa.nio

trait NioBuffer {
    def isByte:Boolean
    def isInt:Boolean
    def isFloat:Boolean
    def isDouble:Boolean
    def size():Int
    def rewind()
}

/** Base for NIO buffers, seen as Scala IndexedSeq. */
trait NioTypedBuffer[T] extends IndexedSeq[T] with NioBuffer {
	type BufferLike <: { def get(i:Int):T }
	/** The maximum capacity of the buffer. */
    var capacity:Int
    /** The maximum capacity of the buffer, synonym of `capacity()`. */
	def length:Int = capacity
	/** The underlying buffer. */
    var buffer:BufferLike
    /** The `i`-th element of the buffer, array-like access. */
	def apply(i:Int):T = buffer.get(i)
	/** Size of the buffer. */
	override def size():Int = capacity
}

object ByteBuffer {
    implicit def ByteBufferToNio(bb:ByteBuffer):java.nio.ByteBuffer = { bb.rewind; bb.buffer }
    def apply(capacity:Int, direct:Boolean) = new ByteBuffer(capacity, direct)
    def apply(data:Array[Byte], direct:Boolean) = { val b = new ByteBuffer(data.size, direct); b.copy(data); b.rewind; b } 
    def apply(direct:Boolean, data:Byte*) = {
        val buffer = new ByteBuffer(data.size, direct)
	    var i = 0
	    data.foreach { item =>
	        buffer(i) = item
	        i += 1
	    }
	    buffer
    }
}

object IntBuffer {
    implicit def IntBufferToNio(ib:IntBuffer):java.nio.IntBuffer = { ib.rewind; ib.buffer }
    def apply(capacity:Int, direct:Boolean) = new IntBuffer(capacity, direct)
    def apply(from:ByteBuffer) = new IntBuffer(from)
    def apply(capacity:Int) = new IntBuffer(capacity)
    def apply(data:Array[Int]) = { val b = new IntBuffer(data.size); b.copy(data); b.rewind; b } 
    def apply(data:Int*) = {
        val buffer = new IntBuffer(data.size)
	    var i = 0
	    data.foreach { item =>
	        buffer(i) = item
	        i += 1
	    }
	    buffer
    }
    protected def allocate(capacity:Int, direct:Boolean):java.nio.IntBuffer = {
        if(direct) {
            java.nio.ByteBuffer.allocateDirect(capacity*4).order(java.nio.ByteOrder.nativeOrder).asIntBuffer
        } else {
            java.nio.IntBuffer.allocate(capacity)
        }
    }
}

object FloatBuffer {
    implicit def FloatBufferToNio(fb:FloatBuffer):java.nio.FloatBuffer = { fb.rewind; fb.buffer }
    def apply(capacity:Int, direct:Boolean) = new FloatBuffer(capacity, direct)
    def apply(from:ByteBuffer) = new FloatBuffer(from)
    def apply(capacity:Int) = new FloatBuffer(capacity)
    def apply(data:Array[Float]) = { val b = new FloatBuffer(data.size); b.copy(data); b.rewind; b } 
    def apply(data:Float*) = {
        val buffer = new FloatBuffer(data.size)
	    var i = 0
	    data.foreach { item =>
	        buffer(i) = item
	        i += 1
	    }
	    buffer
    }
    protected def allocate(capacity:Int, direct:Boolean):java.nio.FloatBuffer = {
        if(direct) {
            java.nio.ByteBuffer.allocateDirect(capacity*4).order(java.nio.ByteOrder.nativeOrder).asFloatBuffer
        } else {
            java.nio.FloatBuffer.allocate(capacity)
        }
    }
}

object DoubleBuffer {
    implicit def DoubleBufferToNio(db:DoubleBuffer):java.nio.DoubleBuffer = { db.rewind; db.buffer }   
    def apply(capacity:Int, direct:Boolean) = new DoubleBuffer(capacity, direct)
    def apply(from:ByteBuffer) = new DoubleBuffer(from)
    def apply(capacity:Int) = new DoubleBuffer(capacity)
    def apply(data:Array[Double]) = { val b = new DoubleBuffer(data.size); b.copy(data); b.rewind; b } 
    def apply(data:Double*) = {
        val buffer = new DoubleBuffer(data.size)
	    var i = 0
	    data.foreach { item =>
	        buffer(i) = item
	        i += 1
	    }
	    buffer
    }
    protected def allocate(capacity:Int, direct:Boolean):java.nio.DoubleBuffer = {
        if(direct) {
            java.nio.ByteBuffer.allocateDirect(capacity*8).order(java.nio.ByteOrder.nativeOrder).asDoubleBuffer
        } else {
            java.nio.DoubleBuffer.allocate(capacity)
        }
    }
}

class ByteBuffer(var capacity:Int, direct:Boolean) extends NioTypedBuffer[Byte] {
	type BufferLike = java.nio.ByteBuffer
	var buffer = if(direct) java.nio.ByteBuffer.allocateDirect(capacity) else java.nio.ByteBuffer.allocate(capacity)
	nativeOrder
	def this(data:Array[Byte], direct:Boolean) { this(data.size, direct); copy(data) }
	def rewind() { buffer.rewind() }
	def position(i:Int) { buffer.position(i) }
	def update(i:Int, value:Byte):Unit = buffer.put(i, value)
	def copy(other:ByteBuffer) = { buffer.rewind; other.rewind; buffer.put(other.buffer) }
	def copy(data:Array[Byte]) = { buffer.rewind; buffer.put(data) }
	def nativeOrder() = buffer.order(java.nio.ByteOrder.nativeOrder())
	def bigEndian() = buffer.order(java.nio.ByteOrder.BIG_ENDIAN)
	def littleEndian() = buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
    def isByte:Boolean = true
    def isInt:Boolean = false
    def isFloat:Boolean = false
    def isDouble:Boolean = false
}

class IntBuffer(var capacity:Int, direct:Boolean) extends NioTypedBuffer[Int] {
	type BufferLike = java.nio.IntBuffer
	var buffer:java.nio.IntBuffer = if(capacity>0) IntBuffer.allocate(capacity, direct) else null    
	def this(capacity:Int) { this(capacity, true) }
	def this(data:Array[Int]) { this(data.size); copy(data) }
	def this(from:ByteBuffer) { this(0); buffer = from.buffer.asIntBuffer; capacity = buffer.capacity/4 }
	def rewind() { buffer.rewind() }
	def position(i:Int) { buffer.position(i) }
	def update(i:Int, value:Int):Unit = buffer.put(i, value)
	def copy(other:IntBuffer) = { buffer.rewind; other.rewind; buffer.put(other.buffer) }
	def copy(data:Array[Int]) = { buffer.rewind; buffer.put(data) }
    def isByte:Boolean = false
    def isInt:Boolean = true
    def isFloat:Boolean = false
    def isDouble:Boolean = false
}

class FloatBuffer(var capacity:Int, direct:Boolean) extends NioTypedBuffer[Float] {
	type BufferLike = java.nio.FloatBuffer
	var buffer:java.nio.FloatBuffer = if(capacity>0) FloatBuffer.allocate(capacity, direct) else null
	def this(capacity:Int) { this(capacity, true) }
	def this(data:Array[Float]) { this(data.size); copy(data) }
	def this(from:ByteBuffer) { this(0); buffer = from.buffer.asFloatBuffer; capacity = buffer.capacity/4 }
	def rewind() { buffer.rewind() }
	def position(i:Int) { buffer.position(i) }
	def update(i:Int, value:Float):Unit = buffer.put(i, value)
	def copy(other:FloatBuffer) = { buffer.rewind; other.rewind; buffer.put(other.buffer) }
	def copy(data:Array[Float]) = { buffer.rewind; buffer.put(data) }
    def isByte:Boolean = false
    def isInt:Boolean = false
    def isFloat:Boolean = true
    def isDouble:Boolean = false
}

class DoubleBuffer(var capacity:Int, direct:Boolean) extends NioTypedBuffer[Double] {
    type BufferLike = java.nio.DoubleBuffer
	var buffer:java.nio.DoubleBuffer = if(capacity>0) DoubleBuffer.allocate(capacity, direct) else null
	def this(capacity:Int) { this(capacity, true) }
	def this(data:Array[Double]) { this(data.size); copy(data) }
    def this(from:ByteBuffer) { this(0); buffer = from.buffer.asDoubleBuffer; capacity = buffer.capacity/8 }
	def rewind() { buffer.rewind() }
	def position(i:Int) { buffer.position(i) }
	def update(i:Int, value:Double):Unit = buffer.put(i, value)
	def copy(other:DoubleBuffer) = { buffer.rewind; other.rewind; buffer.put(other.buffer) }
	def copy(data:Array[Double]) = { buffer.rewind; buffer.put(data) }
    def isByte:Boolean = false
    def isInt:Boolean = false
    def isFloat:Boolean = false
    def isDouble:Boolean = true
}