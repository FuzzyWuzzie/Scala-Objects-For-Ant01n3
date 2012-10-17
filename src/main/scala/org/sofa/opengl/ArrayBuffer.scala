package org.sofa.opengl

import org.sofa.nio._

object ArrayBuffer {
    def apply(gl:SGL, valuesPerElement:Int, data:Array[Int], drawMode:Int):ArrayBuffer = new ArrayBuffer(gl, valuesPerElement, data, drawMode)
    def apply(gl:SGL, valuesPerElement:Int, data:Array[Float], drawMode:Int):ArrayBuffer = new ArrayBuffer(gl, valuesPerElement, data, drawMode)
    def apply(gl:SGL, valuesPerElement:Int, data:NioBuffer, drawMode:Int):ArrayBuffer = new ArrayBuffer(gl, valuesPerElement, data, drawMode)
}

/** Store a sequence of data elements (vertices, colors, normals, etc).
  * The `valuesPerElement` argument tells the number of components of
  * each element (for example, vertices are usually made of three components
  * (x, y and z) whereas colors are made of four components (r, g, b and a)).
  * The `data` argument must contain a float buffer whose length is a multiple
  * of the `valuesPerElement` parameter.
  * The `drawMode` specify if and how the elements will be updated. You can
  * pass values like gl.STATIC_DRAW, gl.STREAM_DRAW and gl.DYNAMIC_DRAW. */
class ArrayBuffer(gl:SGL, val valuesPerElement:Int, data:NioBuffer, val drawMode:Int) extends OpenGLObject(gl) {
    import gl._
    
    /** Number of components in the buffer. */
    var componentCount:Int = 0
    
    /** Type of data stored in the buffer. */
    var glType:Int = 0
    
    /** Number of elements (components divided by the number of components per element). */
    def elementCount:Int = componentCount / valuesPerElement
    
    init
    
    def this(gl:SGL, valuesPerElement:Int, data:Array[Float], drawMode:Int) {
        this(gl, valuesPerElement, new FloatBuffer(data), drawMode)
    }
    
    def this(gl:SGL, valuesPerElement:Int, data:Array[Int], drawMode:Int) {
        this(gl, valuesPerElement, new IntBuffer(data), drawMode)
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
        bufferData(gl.ARRAY_BUFFER, data, drawMode)
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
    
    /** Update the whole buffer with the data given (that must be the same size). */
    def update(data:FloatBuffer) { update(0, size, data) }
    
    /** Update a part of the buffer with the data given, the buffer start index
      * is given by `from` and the buffer last index is given by `to` not included.
      * The data must be offset correctly and contain at least `to-from` elements.
      * The `to` and `from` values are expressed in elements (series of valuesPerElement
      * items). */
    def update(from:Int, to:Int, data:FloatBuffer) {
    	bind
    	data.rewind
    	bufferSubData(gl.ARRAY_BUFFER, from*valuesPerElement, (to-from)*valuesPerElement, data)
    	checkErrors
    	
//    	Console.err.println("updating %d values that is %d elements (start value=%d or element=%d)".format((to-from)*valuesPerElement,
//    			(to-from), from*valuesPerElement, from))
    }
    
    override def dispose() {
        checkId
        bindBuffer(gl.ARRAY_BUFFER, 0)
        deleteBuffer(oid)
        super.dispose
    }
}