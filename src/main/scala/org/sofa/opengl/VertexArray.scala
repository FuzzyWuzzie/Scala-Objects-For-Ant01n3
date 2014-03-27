package org.sofa.opengl

import org.sofa.nio._
import scala.collection.mutable.HashMap

/** Associates several buffers containing arrays representing vertex attributes and
  * an eventual element buffer (indices into these arrays) in a common structure.
  * 
  * The vertex array is an unique OpenGl object that identifies a set of buffers storing vertex
  * attributes and an optional buffer storing indices in the vertex attributes. This allows
  * to quickly bind several attributes for drawing.
  * 
  * This class provides facilities to create the vertex attribute buffers from arbitrary data,
  * and the indices buffer. It can handle OpenGL ES 2.0 absence of vertex arrays by emulating it.
  * 
  * You can easily choose which buffer is associated with each vertex attribute index at
  * construction.
  * 
  * You can also provide directly several ArrayBuffer objects representing the various
  * vertex attributes. */
class VertexArray(gl:SGL) extends OpenGLObject(gl) {
    import gl._
 
    /** Associate names with the buffers indices. */
    private[this] val bufferNames = new HashMap[String,Int]()
    
    /** Set of data (vertices, colors, normals, etc.) */
    private[this] var buffers:Array[(Int,ArrayBuffer)] = null

    /** Eventually null set of indices in the array buffers. */
    private[this] var elements:ElementBuffer = null
    
    init
    
    /** Initialize the vertex array. */
    protected def init() { super.init(if(gl.isES) this else createVertexArray) }
    
    /** Store the indices and array buffers. Indices may be null. */
    protected def storeData(gl:SGL, indices:IntBuffer, drawMode:Int, attributes:(String,Int,Int,NioBuffer)*) {
        this.buffers = new Array[(Int,ArrayBuffer)](attributes.size)
        if(!gl.isES) bindVertexArray(oid)
        var i=0
        attributes.foreach { item =>
            // The creation binds the buffer.
        	bufferNames += ((item._1, i))
            buffers(i) = (item._2, ArrayBuffer(gl, item._3, item._4, drawMode))
            if(!gl.isES) buffers(i)._2.vertexAttrib(item._2, true)	// Identify the attribute to the vertex array.
            i += 1
        }
        if(indices ne null) {
            // The creation binds the buffer.
        	elements = ElementBuffer(gl, indices)
        }
        if(!gl.isES) bindVertexArray(null)
    }
    
    /** Store the indices and array buffers. Indices may be null. */
    protected def storeData(gl:SGL, indices:ElementBuffer, attributes:(String,Int,ArrayBuffer)*) {
    	this.buffers = new Array[(Int,ArrayBuffer)](attributes.size)
    	if(!gl.isES) bindVertexArray(oid)
    	var i=0
    	attributes.foreach { item =>
    		bufferNames += ((item._1, i))
    		buffers(i) = (item._2, item._3)
    		if(!gl.isES) {
    			item._3.bind
    			item._3.vertexAttrib(item._2, true)
    			i += 1
    		}
    	}
    	if(indices ne null) {
    		elements = indices
    	}
    	if(!gl.isES) bindVertexArray(null)
    }
    
    /** Create a vertex array without indices, only made of vertices, colors, normals, etc.
      * The `data` must be a tuple with four values, first the attribute name, then the attribute index, then
      * the attribute number of component per element(for example vertices have 3 components
      * (x, y and z), colors have four components (r, g, b and a)), and finally the attribute
      * data as a float buffer containing the data, whose length must be a multiple of the
      * number of components per element. The array buffers are created with gl.STATIC_DRAW
      * draw mode. */
    def this(gl:SGL, attributes:(String, Int, Int, NioBuffer)*) {
        this(gl)
        storeData(gl, null, gl.STATIC_DRAW, attributes:_*)
    }
    
    /** Create a vertex array with indices, made of vertices, colors, normals, etc.
      * The `indices` must be a set of integers defining which element to use in the `data`. The
      * use of the indices depends on the way elements are drawn (triangles, lines, etc.). 
      * The `data` must be a tuple with four values, first the attribute name, then the attribute index, then
      * the attribute number of component per element(for example vertices have 3 components
      * (x, y and z), colors have four components (r, g, b and a)), and finally the attribute
      * data as a float buffer containing the data, whose length must be a multiple of the
      * number of components per element. The array buffers are created with gl.STATIC_DRAW
      * draw mode. */
    def this(gl:SGL, indices:IntBuffer, attributes:(String, Int, Int, NioBuffer)*) {
        this(gl)
        storeData(gl, indices, gl.STATIC_DRAW, attributes:_*)
    }
    
    /** Create a vertex array without indices, only made of vertices, colors, normals, etc.
      * The `data` must be a tuple with four values, first the attribute name, then the attribute index, then
      * the attribute number of component per element(for example vertices have 3 components
      * (x, y and z), colors have four components (r, g, b and a)), and finally the attribute
      * data as a float buffer containing the data, whose length must be a multiple of the
      * number of components per element. */
    def this(gl:SGL, drawMode:Int, attributes:(String, Int, Int, NioBuffer)*) {
        this(gl)
        storeData(gl, null, drawMode, attributes:_*)
    }
    
    /** Create a vertex array with indices, made of vertices, colors, normals, etc.
      * The `indices` must be a set of integers defining which element to use in the `data`. The
      * use of the indices depends on the way elements are drawn (triangles, lines, etc.). 
      * The `data` must be a tuple with four values, first the attribute name, then the attribute index, then
      * the attribute number of component per element(for example vertices have 3 components
      * (x, y and z), colors have four components (r, g, b and a)), and finally the attribute
      * data as a float buffer containing the data, whose length must be a multiple of the
      * number of components per element. */
    def this(gl:SGL, indices:IntBuffer, drawMode:Int, attributes:(String, Int, Int, NioBuffer)*) {
        this(gl)
        storeData(gl, indices, drawMode, attributes:_*)
    }
    
    /*  Create a vertex array without indices, only made of vertices, colors, normals, etc.
      * The `data` must be a tuple with two values, first the attribute index, then the attribute
      * data as an array buffer already allocated an bindable.
    def this(gl:SGL, attributes:(Int,ArrayBuffer)*) {
    	this(gl)
    	storeData(gl, null, attributes:_*)
    }
    */
    
    /** Create a vertex array with indices, made of vertices, colors, normals, etc.
      * The `indices` must be a set of integers defining which element to use in the `data`. The
      * use of the indices depends on the way elements are drawn (triangles, lines, etc.). 
      * The `indices` array may be null if no indices are used.
      * The `data` must be a tuple with three values, first the attribute name, then the attribute index, then
      * the attribute data as an array buffer already allocated and bindable. */
    def this(gl:SGL, indices:ElementBuffer, attributes:(String, Int,ArrayBuffer)*) {
    	this(gl)
    	storeData(gl, indices, attributes:_*)
    }
    
    override def dispose() {
        checkId
        buffers.foreach { _._2.dispose }
        
        if(! gl.isES) {
        	bindVertexArray(null)
        	deleteVertexArray(oid)
        }

        super.dispose
    }

    /** Draw the elements (vertices, colors, normals) eventually using indices if
      * specified, and following a drawing mode given by the `kind` argument. This
      * last argument takes as value either the `Mesh.drawAs()` value of one of the
      * OpenGL constants `GL_TRIANGLES`, `GL_LINES`, `GL_LINE_LOOP`, `GL_POINTS`,
      * etc. Each attribute is associated to the index given at construction. */
    def draw(kind:Int):Unit = {
        checkId
        bind
        
        if(elements ne null)
             drawElements(kind, elements.size, gl.UNSIGNED_INT, 0)
        else drawArrays(kind, 0, buffers(0)._2.size)

        checkErrors
    }

    /** Draw the elements (vertices, colors, normals) eventually using indices if
      * specified, and following a drawing mode given by the `kind` argument. This
      * last argument takes as value either the `Mesh.drawAs()` value of one of the
      * OpenGL constants `GL_TRIANGLES`, `GL_LINES`, `GL_LINE_LOOP`, `GL_POINTS`,
      * etc. Each attribute is associated to the index given at construction. */
    def draw(kind:Int, countElement:Int):Unit = {
    	if(countElement > 0) {
    		checkId
    		bind

    		if(elements ne null)
    			drawElements(kind, countElement, gl.UNSIGNED_INT, 0)
    		else drawArrays(kind, 0, countElement)

    		checkErrors
    	}
    }

    def draw(kind:Int, offset:Int, countElement:Int):Unit = {
    	if(countElement > 0) {
    		checkId
    		bind

    		if(elements ne null)
    		     drawElements(kind, countElement, gl.UNSIGNED_INT, offset)
    		else drawArrays(kind, offset, countElement)

    		checkErrors
    	}	
    }

    def drawArrays(kind:Int, offset:Int, count:Int) {
    	checkId
    	bind
    	gl.drawArrays(kind, offset, count)
    	checkErrors
    }
    
    def multiDraw(kind:Int, firsts:IntBuffer, counts:IntBuffer, primcount:Int) {
        checkId
        bind
        
        if(elements ne null) { /* TODO */ }
        else multiDrawArrays(kind, firsts, counts, primcount)
    }

    protected def bind() {
        if(gl.isES) {
            // No vertex arrays in ES2, a shame ! :-)
            var i = 0
            val n = buffers.length
            while(i < n) {
                val pos = buffers(i)._1
                val buf = buffers(i)._2
                buf.bind
                buf.vertexAttrib(pos, true)
                i += 1
            }
            if(elements ne null) {
                elements.bind
            }
        } else {
            bindVertexArray(oid)
        }
    }
    
    def drawTriangles() {
        draw(gl.TRIANGLES)
    }

    def indices:ElementBuffer = elements
    
    /** I-th stored buffer. Buffers are stored in the order they where first given at creation. */
    def buffer(i:Int):ArrayBuffer = buffers(i)._2
    
    /** The buffer with the given name or null if not found. */
    def buffer(name:String):ArrayBuffer = buffer(bufferNames(name))
}