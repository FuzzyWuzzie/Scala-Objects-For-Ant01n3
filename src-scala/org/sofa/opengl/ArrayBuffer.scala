package org.sofa.opengl

import javax.media.opengl._
import org.sofa.nio._

import GL._
import GL2._
import GL2ES2._
import GL3._ 

object ArrayBuffer {
    def apply(gl:SGL, valuesPerElement:Int, data:Array[Float]):ArrayBuffer = new ArrayBuffer(gl, valuesPerElement, data)
    def apply(gl:SGL, valuesPerElement:Int, data:FloatBuffer):ArrayBuffer = new ArrayBuffer(gl, valuesPerElement, data)
}

/** Store a sequence of data elements (vertices, colors, normals, etc).
  * The `valuesPerElement` argument tells the number of components of
  * each element (for example, vertices are usually made of three components
  * (x, y and z) whereas colors are made of four components (r, g, b and a)).
  * The `data` argument must contain a float buffer whose length is a multiple
  * of the `valuesPerElement` parameter. */
class ArrayBuffer(gl:SGL, val valuesPerElement:Int, data:FloatBuffer) extends OpenGLObject(gl) {
    import gl.gl._
    import gl._
    
    /** Number of components in the buffer. */
    var componentCount:Int = 0
    
    /** Number of elements (components divided by the number of components per element). */
    def elementCount:Int = componentCount / valuesPerElement
    
    init
    
    def this(gl:SGL, valuesPerElement:Int, data:Array[Float]) {
        this(gl, valuesPerElement, new FloatBuffer(data))
    }
    
    protected def init() {
        super.init(genBuffer)
        storeData(data)
    }
    
    protected def storeData(data:FloatBuffer) {
        checkId
        data.rewind
        componentCount = data.size
        bindBuffer(GL_ARRAY_BUFFER, oid)
        bufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW)
        checkErrors
    }
    
    /** Overall number of components in the array (not the number of elements!). */
    def size:Int = elementCount
    
    def vertexAttrib(number:Int) {
        checkId
        bindBuffer(GL_ARRAY_BUFFER, oid)
        vertexAttribPointer(number, valuesPerElement, GL_FLOAT, false, 0, 0)
        checkErrors
    }
    
    override def dispose() {
        checkId
        bindBuffer(GL_ARRAY_BUFFER, 0)
        deleteBuffer(oid)
        super.dispose
    }
}
