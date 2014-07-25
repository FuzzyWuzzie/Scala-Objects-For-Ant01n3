package org.sofa.gfx


/** Represents a base for any OpenGL object.
  *
  * The identifier of the object is free (ofen an integer, but in webgl these are
  * objects for example).
  */
class OpenGLObject(val sgl:SGL) {
    
    /** The OpenGL name. */
    protected[this] var oid:AnyRef = null
    
    /** Initialize the object name. */
    protected def init(id:AnyRef) { oid = id }

    /** Initialiaz the object name. */
    protected def init(id:Int) { oid = id.asInstanceOf[Integer] }
    
    /** This object name. */
    def id:AnyRef = oid
    
    /** Release the object name. */
    def dispose() { oid = null }
    
    /** Check if an error was raised by previous actions, if so, print it and raise a runtime
      * exception. */
    def checkErrors()  = sgl.checkErrors
    
    /** Check the id has not been disposed, if so it it is less than 0. */
    protected def checkId() {
        if(oid eq null)
            throw new RuntimeException("you cannot use a disposed OpenGL object")
    }
}
