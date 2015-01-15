package org.sofa.gfx

import org.sofa.nio._


object ElementBuffer {
    def apply(gl:SGL, data:Array[Int]):ElementBuffer = new ElementBuffer(gl, data)
    def apply(gl:SGL, data:IntBuffer):ElementBuffer = new ElementBuffer(gl, data)
}


/** Stores a sequence of indices describing primitives (points, lines,
  * triangles, etc.) in an array buffer.
  */
class ElementBuffer(gl:SGL, val elementCount:Int) extends OpenGLObject(gl) {
    import gl._
    
    def this(gl:SGL, data:Array[Int]) {
        this(gl, data.size)
        initFromData(IntBuffer(data), gl.STATIC_DRAW)
    }

    def this(gl:SGL, data:Array[Int], drawMode:Int) {
    	this(gl, data.size)
    	initFromData(IntBuffer(data), drawMode)
    }

    def this(gl:SGL, data:IntBuffer) {
    	this(gl, data.size)
    	initFromData(data, gl.STATIC_DRAW)
    }

    def this(gl:SGL, data:IntBuffer, drawMode:Int) {
    	this(gl, data.size)
    	initFromData(data, drawMode)
    }

    def this(gl:SGL, eltCount:Int, drawMode:Int) {
    	this(gl, eltCount)
    	initEmpty(drawMode)
    }
    
    protected def initFromData(data:IntBuffer, drawMode:Int) {
        super.init(createBuffer)
        bind
        bufferData(gl.ELEMENT_ARRAY_BUFFER, data, drawMode)
        checkErrors
    }

    protected def initEmpty(drawMode:Int) {
    	super.init(createBuffer)
    	bind
    	bufferData(gl.ELEMENT_ARRAY_BUFFER, elementCount*4, drawMode)
    	checkErrors
    }
    
    def size:Int = elementCount

    def intType:Int = gl.UNSIGNED_INT
    
    def bind() {
        checkId
        bindBuffer(gl.ELEMENT_ARRAY_BUFFER, oid)
    }

    def update(data:IntBuffer) { update(0, elementCount, data) }
    
    /** The `from` and `end` values indicate elements, not primitives (at the
      * contrary of [[ArrayBuffer]]). */
    def update(from:Int, to:Int, data:IntBuffer, alsoPositionInData:Boolean = true) {
    	bind    	
    	bufferSubData(gl.ELEMENT_ARRAY_BUFFER, from, (to-from), data, alsoPositionInData)    	
    	checkErrors
    }
    
    override def dispose() {
        checkId
        bindBuffer(gl.ELEMENT_ARRAY_BUFFER, null)
        deleteBuffer(oid)
        super.dispose
    }    
}
