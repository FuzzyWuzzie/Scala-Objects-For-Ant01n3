package org.sofa.opengl

import org.sofa.nio._

object ArrayBuffer {
    def apply(gl:SGL, valuesPerElement:Int, data:Array[Int]):ArrayBuffer = new ArrayBuffer(gl, valuesPerElement, data)
    def apply(gl:SGL, valuesPerElement:Int, data:Array[Float]):ArrayBuffer = new ArrayBuffer(gl, valuesPerElement, data)
    def apply(gl:SGL, valuesPerElement:Int, data:NioBuffer):ArrayBuffer = new ArrayBuffer(gl, valuesPerElement, data)
}

/** Store a sequence of data elements (vertices, colors, normals, etc).
  * The `valuesPerElement` argument tells the number of components of
  * each element (for example, vertices are usually made of three components
  * (x, y and z) whereas colors are made of four components (r, g, b and a)).
  * The `data` argument must contain a float buffer whose length is a multiple
  * of the `valuesPerElement` parameter. */
class ArrayBuffer(gl:SGL, val valuesPerElement:Int, data:NioBuffer) extends OpenGLObject(gl) {
    import gl._
    
    /** Number of components in the buffer. */
    var componentCount:Int = 0
    
    var glType:Int = 0
    
    /** Number of elements (components divided by the number of components per element). */
    def elementCount:Int = componentCount / valuesPerElement
    
    init
    
    def this(gl:SGL, valuesPerElement:Int, data:Array[Float]) {
        this(gl, valuesPerElement, new FloatBuffer(data))
    }
    
    def this(gl:SGL, valuesPerElement:Int, data:Array[Int]) {
        this(gl, valuesPerElement, new IntBuffer(data))
    }
    
    protected def init() {
        super.init(genBuffer)
        storeData(data)
    }
    
    protected def storeData(data:NioBuffer) {
        checkId
        data.rewind
        componentCount = data.size
        bindBuffer(gl.ARRAY_BUFFER, oid)
        storeType(data)
        bufferData(gl.ARRAY_BUFFER, data, gl.STATIC_DRAW)
        bindBuffer(gl.ARRAY_BUFFER, 0)
        checkErrors
    }
    
    protected def storeType(data:NioBuffer) {
        if(data.isByte)
            glType = gl.UNSIGNED_BYTE
        else if(data.isInt) 
            glType = gl.UNSIGNED_INT
        else if(data.isFloat)
            glType = gl.FLOAT
        else if(data.isDouble)
            glType = gl.DOUBLE
        else throw new RuntimeException("Unknown Nio data type") 
    }
    
    /** Overall number of components in the array (not the number of elements!). */
    def size:Int = elementCount
    
    def vertexAttrib(index:Int, enable:Boolean) {
        checkId
        bindBuffer(gl.ARRAY_BUFFER, oid)
//Console.err.println("binding buffer %d vpe, type=%s".format(valuesPerElement, if(glType==gl.FLOAT) "float" else if(glType==gl.UNSIGNED_INT) "int" else "other"))
        vertexAttribPointer(index, valuesPerElement, glType, false, 0, 0)
        if(enable) enableVertexAttribArray(index)
        checkErrors
    }
    
    def bind() {
        checkId
        bindBuffer(gl.ARRAY_BUFFER, oid)
    }
    
    def unbind() {
        bindBuffer(gl.ARRAY_BUFFER, 0)
    }
    
    override def dispose() {
        checkId
        bindBuffer(gl.ARRAY_BUFFER, 0)
        deleteBuffer(oid)
        super.dispose
    }
}