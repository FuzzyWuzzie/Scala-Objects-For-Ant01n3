package org.sofa.opengl.mesh.skeleton

import scala.collection.mutable.ArrayBuffer
import org.sofa.opengl.{VertexArray, SGL, Camera, ShaderProgram}
import org.sofa.opengl.mesh._
import org.sofa.math._

import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._

/** Bone companion object. */
object Bone {
    val boneMesh = new BoneMesh()
    var bone:VertexArray = null
}

/** A simple bone hierarchy. */
class Bone(val id:Int) {
// Attributes
    
    /** The matrix that transforms the bone at its original location in the untransformed model mesh. */
	val orientation:Matrix4 = ArrayMatrix4()
	
	/** The matrix that animates the bone. */
	val animation:Matrix4 = ArrayMatrix4()
	
	/** Inverse of [[orientation]] matrix. */
	val inverseOrientation:Matrix4 = ArrayMatrix4()
	
	/** Final matrix computed for the vertices attached to this bone. */
	val TX:Matrix4 = ArrayMatrix4()
	
	/** Bone color. */
	var color = new Rgba(1, 1, 1, 1)
	
	/** Parent bone. */
	var parent:Bone = null
	
	/** Child bones. */
	val children = new ArrayBuffer[Bone]()

// Construction
	
	orientation.setIdentity
	animation.setIdentity
	inverseOrientation.setIdentity
	TX.setIdentity
	
// Commands
	
	/** Add a child bone to the hierarchy. */
	def addChild(id:Int):Bone = {
	    val child = new Bone(id)
	    
	    children += child
	    child.parent = this
	    child
	}
	
	/** Child bone `index`. */
	def apply(index:Int):Bone = children(index)

	/** The shader must have an uniform vec4 variable whose name is passed
	  * as `uniformColorName` to set the color of the bone. */
	def drawSkeleton(gl:SGL, camera:Camera, shader:ShaderProgram, uniformColorName:String) {
	    recursiveDrawSkeleton(gl, camera, shader, uniformColorName)
	}
	
	def drawModel(gl:SGL, camera:Camera, model:Mesh, modelInstance:VertexArray, shader:ShaderProgram) {
		val orientation = ArrayMatrix4(); orientation.setIdentity
	    val forward = ArrayMatrix4(); forward.setIdentity
	    
	    recursiveComputeMatrices(orientation, forward)
	    recursiveUniformMatrices(shader, camera)
	    camera.uniformMVP(shader)
	    modelInstance.draw(model.drawAs)
	}
	
	def identity() = orientation.setIdentity
	
	def translate(tx:Double, ty:Double, tz:Double) = orientation.translate(tx, ty, tz)
	
	def translate(t:NumberSeq3) = orientation.translate(t)
	    
	def scale(sx:Double, sy:Double, sz:Double) = orientation.scale(sx, sy, sz)
	
	def scale(s:NumberSeq3) = orientation.scale(s)

	def rotate(angle:Double, x:Double, y:Double, z:Double) = orientation.rotate(angle, x, y, z)
	
	def rotate(angle:Double, axis:NumberSeq3) = orientation.rotate(angle, axis)
	
	protected def recursiveDrawSkeleton(gl:SGL, camera:Camera, shader:ShaderProgram, uniformColorName:String) {
	    camera.pushpop {
	        camera.transformModel(orientation)
	        camera.uniformMVP(shader)

	        if(Bone.bone == null) Bone.bone = Bone.boneMesh.newVertexArray(gl)
	        shader.uniform(uniformColorName, color)
	        Bone.bone.draw(Bone.boneMesh.drawAs)
	        
	        children.foreach { child =>
	        	child.recursiveDrawSkeleton(gl, camera, shader, uniformColorName)
	        }
	    }
	}
	
	protected def recursiveComputeMatrices(orientation:ArrayMatrix4, forward:ArrayMatrix4) {
	    // Concatenate the parents bone orientation with this bone orientation.
	    val localOrientation = orientation * this.orientation
	    // Save the inverse.
	    inverseOrientation.copy(localOrientation.inverse)
	    // Concatenate to the whole transformation (orientation + animation) this bone orientation...
	    val localForward = forward * this.orientation
	    // ... and this bone animation.
	    localForward *= this.animation
	    // The final matrix is then multiplied by the inverse bone orientation since the mesh
	    // points are not naturally transformed by the bone initial orientation).
	    TX.copy(localForward * inverseOrientation)
//Console.println("bone[%d] TX=%s".format(id, TX))
	    // Process the children.
	    children.foreach { child =>
	    	child.recursiveComputeMatrices(localOrientation, localForward)
	    }
	    // At the end of the process each bone as a TX matrix setup. 
	}
	
	protected def recursiveUniformMatrices(shader:ShaderProgram, camera:Camera) {
	    // Store in the shader each bone final TX matrix.
	    shader.uniformMatrix("bone[%d].MV".format(id), TX)
	    shader.uniformMatrix("bone[%d].MV3x3".format(id), TX.top3x3)
//	    shader.uniform("bone[%d].color", color)
	    
	    children.foreach { child =>
	    	child.recursiveUniformMatrices(shader, camera)
	    }
	}
}