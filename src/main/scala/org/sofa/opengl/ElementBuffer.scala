package org.sofa.opengl

import org.sofa.nio._

object ElementBuffer {
    def apply(gl:SGL, data:Array[Int]):ElementBuffer = new ElementBuffer(gl, data)
    def apply(gl:SGL, data:IntBuffer):ElementBuffer = new ElementBuffer(gl, data)
}

/** Stores a sequence of indices in an array buffer. */
class ElementBuffer(gl:SGL, data:IntBuffer) extends OpenGLObject(gl) {
    import gl._
    
    protected var elementCount:Int = 0
    
    init
    
    def this(gl:SGL, data:Array[Int]) {
        this(gl, IntBuffer(data))
    }
    
    protected def init() {
        super.init(createBuffer)
        storeData(data)
    }
    
    protected def storeData(data:IntBuffer) {
        checkId
//        data.rewind
        elementCount = data.size
        bind
        bufferData(gl.ELEMENT_ARRAY_BUFFER, data, gl.STATIC_DRAW)
        checkErrors
    }
    
    def size:Int = elementCount
    
    def bind() {
        checkId
        bindBuffer(gl.ELEMENT_ARRAY_BUFFER, oid)
    }

    def update(data:IntBuffer) {
    	bind
//    	data.rewind
    	bufferSubData(gl.ELEMENT_ARRAY_BUFFER, 0, size, data)
    	checkErrors
    }
    
    def update(from:Int, to:Int, data:IntBuffer, alsoPositionInData:Boolean = true) {
    	bind    	
    	bufferSubData(gl.ELEMENT_ARRAY_BUFFER, from, (to-from), data, alsoPositionInData)    	
//    	Console.err.println("updating %d values that is %d elements (start element=%d)".format((to-from),
//    			(to-from),  from))
    	checkErrors
    }
    
    override def dispose() {
        checkId
        bindBuffer(gl.ELEMENT_ARRAY_BUFFER, null)
        deleteBuffer(oid)
        super.dispose
    }    
}
