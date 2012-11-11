package org.sofa.opengl

import org.sofa.math._

object MatrixStack {
	implicit def toMatrix4(stack:MatrixStack[Matrix4]):Matrix4 = stack.top

    def apply(initialMatrix:Matrix4):MatrixStack[Matrix4] = new MatrixStack[Matrix4](initialMatrix)
}

/** A stack of 4x4 matrices.
  * 
  * The goal of this class is to mimic the OpenGL matrix stack. It starts with an initial matrix
  * and allows you to push it on the stack, installing a new matrix at the top, copy of the
  * previous one. The new matrix can be modified, used, and then popped from the stack to
  * return to the old one.
  * 
  * You can push and pop as many times as you want. This stack is somewhat optimized to reuse
  * old popped matrices in the stack, therefore, references to old popped matrices cannot be
  * used elsewhere. */
class MatrixStack[M<:Matrix4](initialMatrix:M) {
    
// Attributes
    
    /** The stack of matrices, excepted the top. */
    protected var stack = new scala.collection.mutable.ArrayBuffer[M]()

    /** Position the top matrix. */
    protected var end = 0

    stack += initialMatrix
    
// Access
    
    /** Access to the top-most matrix. */
    def top:M = stack(end)
    
    /** Number of matrices in the stack. */
    def size = end + 1
    
// Commands
    
    /** Make a copy of the top matrix and push it on the top of the stack. */
    def push() {
        //stack.push(current)
        //current = current.newClone.asInstanceOf[M]
        if(stack.size == (end+1)) {
            val m  = stack(end).newClone.asInstanceOf[M]
            stack += m
            end   += 1
        } else {
            end += 1
            stack(end).copy(stack(end-1))
        }
    }
    
    /** Delete the top matrix and install the previous one on the top of the stack. */
    def pop() {
        if(stack.size>0) {
        	end -= 1
        } else {
            throw new RuntimeException("cannot pop more elements of the matrix stack, at least one element must remain")
        }
    }
    
    /** Make a copy of the top matrix, execute the given code using the copy, then
      * reinstall the original top matrix. Equivalent to using [[push()]] then
      * [[pop()]]. */
    def pushpop(code: =>Unit):Unit = {
        push
        code
        pop
    }

// Commands -- Transforms
    
    /** Make the top matrix an identity matrix. */
    def setIdentity() = stack(end).setIdentity
    
    /** Multiply the top matrix by a translation matrix whose coefficient are given
     *  as (`dx`, `dy`, `dz`). */
    def translate(dx:Double, dy:Double, dz:Double) = stack(end).translate(dx, dy, dz)
    
    def translate(of:NumberSeq3) = stack(end).translate(of)
    
    /** Multiply the top matrix by a rotation matrix defining a rotation of `angle` degrees
     * around axis (`x`, `y`, `z`). */
    def rotate(angle:Double, x:Double, y:Double, z:Double) = stack(end).rotate(angle, x, y, z)
    
    def rotate(angle:Double, axis:NumberSeq3) = stack(end).rotate(angle, axis)
    
    /** Multiply the top matrix by a scaling matrix with factors (`sx`, `sy`, `sz`). */
    def scale(sx:Double, sy:Double, sz:Double) = stack(end).scale(sx, sy, sz)
    
    def scale(by:Vector3) = stack(end).scale(by)
    
    /** Replace the top matrix by new transformations matrix that mimics the positioning of
     * a camera whose position would be (`eyex`, `eyey`, `eyez`), that would point at a center
     * located at (`ctrx`, `ctry`, `ctrz`). The camera could bank in one or another direction,
     * its up vector (a vector perpendicular to its horizon) being (`upvx`, `upvy`, `upvz`). */
    def setLookAt(eyex:Double, eyey:Double, eyez:Double, ctrx:Double, ctry:Double, ctrz:Double, upvx:Double, upvy:Double, upvz:Double) =
    	stack(end).setLookAt(eyex, eyey, eyez, ctrx, ctry, ctrz, upvx, upvy, upvz, true)
    
    /** Replace the top matrix by new transformations matrix that mimics the positioning of
     * a camera whose position would be `eye`, that would point at a center
     * located at `ctr`. The camera could bank in one or another direction,
     * its up vector (a vector perpendicular to its horizon) being `upv`. */
    def setLookAt(eye:NumberSeq3, ctr:NumberSeq3, upv:NumberSeq3) = stack(end).setLookAt(eye, ctr, upv, true)

    /** Multiply the top matrix by new transformations matrix that mimics the positioning of
     * a camera whose position would be (`eyex`, `eyey`, `eyez`), that would point at a center
     * located at (`ctrx`, `ctry`, `ctrz`). The camera could bank in one or another direction,
     * its up vector (a vector perpendicular to its horizon) being (`upvx`, `upvy`, `upvz`). */
    def lookAt(eyex:Double, eyey:Double, eyez:Double, ctrx:Double, ctry:Double, ctrz:Double, upvx:Double, upvy:Double, upvz:Double) =
    	stack(end).lookAt(eyex, eyey, eyez, ctrx, ctry, ctrz, upvx, upvy, upvz, true)
    
    /** Multiply the top matrix by new transformations matrix that mimics the positioning of
     * a camera whose position would be `eye`, that would point at a center
     * located at `ctr`. The camera could bank in one or another direction,
     * its up vector (a vector perpendicular to its horizon) being `upv`. */
    def lookAt(eye:NumberSeq3, ctr:NumberSeq3, upv:NumberSeq3) = stack(end).lookAt(eye, ctr, upv, true)
}
