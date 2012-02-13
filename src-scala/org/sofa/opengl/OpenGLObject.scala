package org.sofa.opengl

import javax.media.opengl._

import GL._
import GL2._
import GL2ES2._
import GL3._ 

/** Represents a base for any OpenGL object identified by a "name" that is an integer identifier
  * in the OpenGL jargon.
  */
class OpenGLObject(val sgl:SGL) {
    import sgl.glu._
    import sgl.gl._

    /** The OpenGL name. */
    protected[this] var oid = -1
    
    /** Initialize the object name. */
    protected def init(id:Int) { oid = id }
    
    /** This object name. */
    def id:Int = oid
    
    /** Release the object name. */
    def dispose() { oid = -1 }
    
    /** Check if an error was raised by previous actions, if so, print it and raise a runtime
      * exception. */
    def checkErrors() {
        val error = glGetError
        if(error != 0) {
        	throw new RuntimeException(gluErrorString(error))
        }
    }
    
    /** Check the id has not been disposed, if so it it is less than 0. */
    protected def checkId() {
        if(oid < 0)
            throw new RuntimeException("you cannot use a disposed OpenGL object")
    }
}
