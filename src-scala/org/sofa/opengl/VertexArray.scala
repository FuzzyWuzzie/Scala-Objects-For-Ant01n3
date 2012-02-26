package org.sofa.opengl

import org.sofa.nio._

/** Associates several array buffers and an eventual element buffer in a common structure. */
class VertexArray(gl:SGL) extends OpenGLObject(gl) {
    import gl._
 
    /** Set of data (vertices, colors, normals, etc.) */
    private[this] var buffers:Array[ArrayBuffer] = null

    /** Eventually null set of indices in the array buffers. */
    private[this] var elements:ElementBuffer = null
    
    init
    
    /** Initialize the vertex array. */
    protected def init() { super.init(if(gl.isES) 0 else genVertexArray) }
    
    /** Store the indices and array buffers. */
    protected def storeData(gl:SGL, indices:IntBuffer, data:(Int,FloatBuffer)*) {
        this.buffers = new Array[ArrayBuffer](data.size)
        if(!gl.isES) bindVertexArray(oid)
        var i=0
        data.foreach { item =>
            buffers(i) = ArrayBuffer(gl, item._1, item._2)
            buffers(i).vertexAttrib(i)      // This is here that the association is
            							    // done between the buffer and the vertex array.
            enableVertexAttribArray(i)
            i += 1
        }
        if(indices ne null) {
        	elements = ElementBuffer(gl, indices)
        	// Creating the element buffer will also bind it, therefore
        	// this will assign it to this vertex array.
        }
        if(!gl.isES) bindVertexArray(0)
    }
    
    /** Create a vertex array without indices, only made of vertices, colors, normals, etc.
      * The `data` must be a tuple with two values, first the number of component per element
      * (for example vertices have 3 components (x, y and z), colors have four components
      * (r, g, b and a)), and then a float buffer containing the data, whose length must be
      * a multiple of the number of components per element. */
    def this(gl:SGL, data:(Int, FloatBuffer)*) {
        this(gl)
        storeData(gl, null, data:_*)
    }
    
    /** Create a vertex array with indices, made of vertices, colors, normals, etc.
      * The `indices` must be set of integers defining which element to use in the `data`. The
      * use of the indices depends on the way elements are drawn (triangles, lines, etc.). 
      * The `data` must be a tuple with two values, first the number of component per element
      * (for example vertices have 3 components (x, y and z), colors have four components
      * (r, g, b and a)), and then a float buffer containing the data, whose length must be
      * a multiple of the number of components per element. */
    def this(gl:SGL, indices:IntBuffer, data:(Int, FloatBuffer)*) {
        this(gl)
        storeData(gl, indices, data:_*)
    }
    
    override def dispose() {
        checkId
        buffers.foreach { _.dispose }
        
        if(! gl.isES) {
        	bindVertexArray(0)
        	deleteVertexArray(oid)
        }

        super.dispose
    }

    /** Draw the elements (vertices, colors, normals) eventually using indices if
      * specified, and following a drawing mode given by the `kind` argument. This
      * last argument takes as value either the `Mesh.drawAs()` value of one of the
      * OpenGL constants `GL_TRIANGLES`, `GL_LINES`, `GL_LINE_LOOP`, `GL_POINTS`,
      * etc.  */
    def draw(kind:Int):Unit = {
        checkId
        bind
        
        if(elements ne null)
             drawElements(kind, elements.size, gl.UNSIGNED_INT, 0)
        else drawArrays(kind, 0, buffers(0).size)

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
            var i = 0
            buffers.foreach { buffer =>
                buffer.bind
                enableVertexAttribArray(i)
                buffer.vertexAttrib(i)
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
}