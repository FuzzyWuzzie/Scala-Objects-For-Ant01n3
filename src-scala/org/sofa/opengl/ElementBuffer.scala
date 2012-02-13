package org.sofa.opengl

import javax.media.opengl._
import org.sofa.nio._

import GL._
import GL2._
import GL2ES2._
import GL3._ 

object ElementBuffer {
    def apply(gl:SGL, data:Array[Int]):ElementBuffer = new ElementBuffer(gl, data)
    def apply(gl:SGL, data:IntBuffer):ElementBuffer = new ElementBuffer(gl, data)
}

/** Stores a sequence of indices in an array buffer. */
class ElementBuffer(gl:SGL, data:IntBuffer) extends OpenGLObject(gl) {
    import gl.gl._
    import gl._
    
    protected var elementCount:Int = 0
    
    init
    
    def this(gl:SGL, data:Array[Int]) {
        this(gl, new IntBuffer(data))
    }
    
    protected def init() {
        super.init(genBuffer)
        storeData(data)
    }
    
    protected def storeData(data:IntBuffer) {
        checkId
        data.rewind
        elementCount = data.size
        bindBuffer(GL_ELEMENT_ARRAY_BUFFER, oid)
        bufferData(GL_ELEMENT_ARRAY_BUFFER, data, GL_STATIC_DRAW)
        checkErrors
    }
    
    def size:Int = elementCount
    
    override def dispose() {
        checkId
        bindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        deleteBuffer(oid)
        super.dispose
    }    
}
