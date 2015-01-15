package org.sofa.gfx

import org.sofa.nio._

object ArrayBuffer {
    def apply(gl:SGL, valuesPerElement:Int, data:Array[Int], drawMode:Int):ArrayBuffer = new ArrayBuffer(gl, valuesPerElement, data, drawMode)
    def apply(gl:SGL, valuesPerElement:Int, data:Array[Float], drawMode:Int):ArrayBuffer = new ArrayBuffer(gl, valuesPerElement, data, drawMode)
    def apply(gl:SGL, valuesPerElement:Int, data:NioBuffer, drawMode:Int):ArrayBuffer = new ArrayBuffer(gl, valuesPerElement, data, drawMode)
}

/** Store a sequence of data elements (vertices, colors, normals, etc) in OpenGL memory.
  *
  * The `valuesPerElement` argument tells the number of components of
  * each element (for example, vertices are usually made of three components
  * (x, y and z) whereas colors are made of four components (r, g, b and a)).
  *
  * The `elementCount` accessor gives the number of elements (number of colors,
  * or vertices, for example. Each element is composed of `valuesPerElement` components).
  *
  * The `componentCount` accessor gives the total number of components, this is the
  * `elementCount` times the `valuesPerElement`
  *
  * The default constructor does not allocate or create the buffer, see the additional constructors.
  * 
  * The `drawMode` specify if and how the elements will be updated. You can
  * pass values like gl.STATIC_DRAW, gl.STREAM_DRAW and gl.DYNAMIC_DRAW. 
  *
  * The buffer can be mapped into client memory for fast access.
  */
class ArrayBuffer(val gl:SGL, val valuesPerElement:Int, val byteSize:Int, val drawMode:Int) extends OpenGLObject(gl) {
    import gl._

    /** Access mode when the buffer is mapped. */
    object AccessMode extends Enumeration {
    	type AccessMode = Value
    	val ReadOnly = Value
    	val WriteOnly = Value
    	val ReadWrite = Value
    }

    import AccessMode._

    /** Type of data stored in the buffer. */
    var glType:Int = 0
    
    /** Number of components in the buffer. */
    var componentCount:Int = 0

    /** The actually mapped buffer. */
    var data:NioBuffer = null

    /** Number of elements (components divided by the number of components per element). */
    def elementCount:Int = componentCount / valuesPerElement

    /** Overall number of components in the array (not the number of elements!). */
    def size:Int = elementCount
    
// Creation
    
    /** Create, allocate and initialize a buffer whose size and contents will be set using the
      * given `data`. The type of the buffer is deduced from the data. The buffer has `valuesPerElement`
      * values for each element (a color has four values per element for example). The `drawMode` can
      * be STATIC_DRAW, DYNAMIC_DRAW, or STREAM_DRAW. The `valueType` gives the type of the values stored
      * in each element. */
    def this(gl:SGL, valuesPerElement:Int, data:Array[Float], drawMode:Int) {
        this(gl, valuesPerElement, data.length * 4, drawMode)
        initWithData(FloatBuffer(data))
    }
    
    /** Create, allocate and initialize a buffer whose size and contents will be set using the
      * given `data`. The type of the buffer is deduced from the data. The buffer has `valuesPerElement`
      * values for each element (a color has four values per element for example). The `drawMode` can
      * be STATIC_DRAW, DYNAMIC_DRAW, or STREAM_DRAW. The `valueType` gives the type of the values stored
      * in each element. */
    def this(gl:SGL, valuesPerElement:Int, data:Array[Int], drawMode:Int) {
        this(gl, valuesPerElement, data.length * 4, drawMode)
        initWithData(IntBuffer(data))
    }

    /** Create, allocate and initialize a buffer whose size and contents will be set using the
      * given `data`. The type of the buffer is deduced from the data. The buffer has `valuesPerElement`
      * values for each element (a color has four values per element for example). The `drawMode` can
      * be STATIC_DRAW, DYNAMIC_DRAW, or STREAM_DRAW. The `valueType` gives the type of the values stored
      * in each element. */
    def this(gl:SGL, valuesPerElement:Int, data:NioBuffer, drawMode:Int) {
    	this(gl, valuesPerElement, data.size * (data match {
    		case bb:ByteBuffer   => 1
    		case ib:IntBuffer    => 4
    		case fb:FloatBuffer  => 4
    		case db:DoubleBuffer => 8
    	}), drawMode)

    	initWithData(data)
    }
    
    /** Create and allocate a buffer whose size is given by `byteSize`. The buffer has `valuesPerElement`
      * values for each element (a color has four values per element for example). The `drawMode` can
      * be STATIC_DRAW, DYNAMIC_DRAW, or STREAM_DRAW. The `valueType` gives the type of the values stored
      * in each element. You can pass `UNSIGNED_INT`, `UNSIGNED_BYTE`, `FLOAT` and `DOUBLE`. With this
      * constructor, the buffer is allocated but its contents are undefined. */
    def this(gl:SGL, valuesPerElement:Int, byteSize:Int, valueType:Int, drawMode:Int) {
    	this(gl, valuesPerElement, byteSize, drawMode)
    	initEmpty(valueType)
    }

    protected def initWithData(data:NioBuffer) {
        super.init(createBuffer)
        bind

        componentCount = data.size

        if(data.isByte)
            glType = gl.UNSIGNED_BYTE
        else if(data.isInt) 
            glType = gl.UNSIGNED_INT
        else if(data.isFloat)
            glType = gl.FLOAT
        else if(data.isDouble)
            glType = gl.DOUBLE
        else throw new RuntimeException("Unknown Nio data type") 
       
        bufferData(gl.ARRAY_BUFFER, data, drawMode)
        //unbind
        checkErrors
    }
    
    protected def initEmpty(valueType:Int) {
    	super.init(createBuffer)
    	bind

    	valueType match {
    		case gl.UNSIGNED_BYTE => { componentCount = byteSize }
    		case gl.UNSIGNED_INT  => { componentCount = byteSize / 4}
    		case gl.FLOAT         => { componentCount = byteSize / 4 }
    		case gl.DOUBLE        => { componentCount = byteSize / 8 }
    		case _                => throw new RuntimeException("Unhandled GL type, you can use ")
    	}

    	glType = valueType

    	bufferData(gl.ARRAY_BUFFER, byteSize, drawMode)
    	//unbind
    }
    
// Access & Command

    def vertexAttrib(index:Int, enable:Boolean, divisor:Int = -1) {
    	bind
//Console.err.println("binding buffer %d vpe, type=%s".format(valuesPerElement, if(glType==gl.FLOAT) "float" else if(glType==gl.UNSIGNED_INT) "int" else "other"))
        vertexAttribPointer(index, valuesPerElement, glType, false, 0, 0)
        if(enable) enableVertexAttribArray(index)
        if(divisor >= 0) vertexAttribDivisor(index, divisor)
        checkErrors
    }

    def bind() {
        checkId
        bindBuffer(gl.ARRAY_BUFFER, oid)
    }
    
    def unbind() {
        bindBuffer(gl.ARRAY_BUFFER, null)
    }
    
    /** Update the whole buffer with the data given (that must be the same size). */
    def update(data:FloatBuffer) { update(0, size, data) }
    
    /** Update a part of the buffer with the data given, the buffer start index
      * is given by `from` and the buffer last index is given by `to` not included.
      *
      * These are position in the GL buffer, not in the data. The data must be
	  * correctly positionned (use position()) and its limit correct (use limit()).
	  * However, if the alsoPositionInData flag is true (by default), the data 
	  * position and limit are set before passing them to the GL. You use this setting
	  * when the data is as large as the whole buffer and you sync a part of the data
	  * with a part of the buffer.
	  *
      * The `to` and `from` values are expressed in elements (series of valuesPerElement
      * items). */
    def update(from:Int, to:Int, data:FloatBuffer, alsoPositionInData:Boolean = true) {
    	bind 
    	bufferSubData(gl.ARRAY_BUFFER, from*valuesPerElement, (to-from)*valuesPerElement, data, alsoPositionInData)
    	checkErrors
    	
//    	Console.err.println("updating %d values that is %d elements (start value=%d or element=%d)".format((to-from)*valuesPerElement,
//    			(to-from), from*valuesPerElement, from))
    }
    
    override def dispose() {
        bind
        deleteBuffer(oid)
        super.dispose
    }

    /** Map the buffer memory so that update accesses do not send data to OpenGL but write it
      * directly in the buffer. The behavior of `update()` methods is changed by the mapping status
      * of the buffer.
      */
    def map(accessMode:AccessMode) {
    	if(! isMapped) {
	    	bind
	    	// ??? componentCount or elementCount ????
    		// Probably componentCount times bytes per component !!!

    		data = mapBufferRange(gl.ARRAY_BUFFER, 0, componentCount, accessMode match {
    			case AccessMode.ReadOnly  => gl.MAP_READ_BIT
    			case AccessMode.WriteOnly => gl.MAP_WRITE_BIT
    			case AccessMode.ReadWrite => gl.MAP_READ_BIT | gl.MAP_WRITE_BIT
    			case _ => throw new RuntimeException("unknown access mode %s".format(accessMode))
    		})
    	}
	}

    /** Unmap the buffer. */
    def unmap() {
    	if(isMapped) {
    		bind
    		unmapBuffer(gl.ARRAY_BUFFER)
    	}
    }

    /** True if the buffer is actually mapped. You cannot call drawing commands using this buffer while mapped. */
    def isMapped():Boolean = (data ne null)
}