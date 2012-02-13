package org.sofa.opengl

import scala.collection.mutable.ArrayStack

import org.sofa.math._

object MatrixStack {
	implicit def toMatrix4(stack:MatrixStack):Matrix4 = stack.top

    def apply(initialMatrix:Matrix4):MatrixStack = new MatrixStack(initialMatrix)
}

/**
 * Define a stack of 4x4 matrices.
 * 
 * The goal of this class is to mimic the OpenGL matrix stack. It starts with an initial matrix
 * and allows you to push it on the stack, installing a new matrix at the top, copy of the
 * previous one. The new matrix can be modified, used, and then popped from the stack to
 * return to the old one.
 * 
 * You can push and pop as many times as you want.
 */
class MatrixStack(initialMatrix:Matrix4) {
    
// Attributes
    
    /** The top matrix. */
    protected var current = initialMatrix
    
    /** The stack of matrices, excepted the top. */
    protected var stack = new ArrayStack[Matrix4]
    
// Access
    
    /** Access to the top-most matrix. */
    def top:Matrix4 = current
    
    /** Number of matrices in the stack. */
    def size = stack.size + 1
    
// Commands
    
    /** Make a copy of the top matrix and push it on the top of the stack. */
    def push() {
        stack.push(current)
        current = current.newClone.asInstanceOf[Matrix4]
    }
    
    /** Delete the top matrix and install the previous one on the top of the stack. */
    def pop() {
        if(stack.size>0) {
        	current = stack.pop
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
    def setIdentity() = current.setIdentity
    
    /** Multiply the top matrix by a translation matrix whose coefficient are given
     *  as (`dx`, `dy`, `dz`). */
    def translate(dx:Double, dy:Double, dz:Double) = current.translate(dx, dy, dz)
    
    /** Multiply the top matrix by a rotation matrix defining a rotation of `angle` degrees
     * around axis (`x`, `y`, `z`). */
    def rotate(angle:Double, x:Double, y:Double, z:Double) = current.rotate(angle, x, y, z)
    
    /** Multiply the top matrix by a scaling matrix with factors (`sx`, `sy`, `sz`). */
    def scale(sx:Double, sy:Double, sz:Double) = current.scale(sx, sy, sz)
    
    /** Replace the top matrix by new transformations matrix that mimics the positioning of
     * a camera whose position would be (`eyex`, `eyey`, `eyez`), that would point at a center
     * located at (`ctrx`, `ctry`, `ctrz`). The camera could bank in one or another direction,
     * its up vector (a vector perpendicular to its horizon) being (`upvx`, `upvy`, `upvz`). */
    def setLookAt(eyex:Double, eyey:Double, eyez:Double, ctrx:Double, ctry:Double, ctrz:Double, upvx:Double, upvy:Double, upvz:Double) =
    	current.setLookAt(eyex, eyey, eyez, ctrx, ctry, ctrz, upvx, upvy, upvz, true)
    
    /** Replace the top matrix by new transformations matrix that mimics the positioning of
     * a camera whose position would be `eye`, that would point at a center
     * located at `ctr`. The camera could bank in one or another direction,
     * its up vector (a vector perpendicular to its horizon) being `upv`. */
    def setLookAt(eye:NumberSeq3, ctr:NumberSeq3, upv:NumberSeq3) = current.setLookAt(eye, ctr, upv, true)

    /** Multiply the top matrix by new transformations matrix that mimics the positioning of
     * a camera whose position would be (`eyex`, `eyey`, `eyez`), that would point at a center
     * located at (`ctrx`, `ctry`, `ctrz`). The camera could bank in one or another direction,
     * its up vector (a vector perpendicular to its horizon) being (`upvx`, `upvy`, `upvz`). */
    def lookAt(eyex:Double, eyey:Double, eyez:Double, ctrx:Double, ctry:Double, ctrz:Double, upvx:Double, upvy:Double, upvz:Double) =
    	current.lookAt(eyex, eyey, eyez, ctrx, ctry, ctrz, upvx, upvy, upvz, true)
    
    /** Multiply the top matrix by new transformations matrix that mimics the positioning of
     * a camera whose position would be `eye`, that would point at a center
     * located at `ctr`. The camera could bank in one or another direction,
     * its up vector (a vector perpendicular to its horizon) being `upv`. */
    def lookAt(eye:NumberSeq3, ctr:NumberSeq3, upv:NumberSeq3) = current.lookAt(eye, ctr, upv, true)
}
