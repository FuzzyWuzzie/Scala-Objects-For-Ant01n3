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
	val orientation:Matrix4 = Matrix4()
	
	/** The matrix that animates the bone. */
	val animation:Matrix4 = Matrix4()
	
	/** Inverse of [[orientation]] matrix. */
	val inverseOrientation:Matrix4 = Matrix4()
	
	/** Final matrix computed for the vertices attached to this bone. */
	val TX:Matrix4 = Matrix4()
	
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

// Render
	
	/** Draw a representation of the skeleton.
	  * The given `shader` must have an uniform `vec4` variable whose name is passed
	  * as `uniformColorName` to set the color of the bone.
	  * 
	  * The [[Camera]] class being used, the shader must also define:
	  *  
	  *      uniform mat4 MV;    // Model-View matrix.
	  *      uniform mat3 MV3x3; // Upper 3x3 model-view matrix.
	  *      uniform mat4 MVP;   // Projection-Model-View matrix.
	  */
	def drawSkeleton(gl:SGL, camera:Camera, shader:ShaderProgram, uniformColorName:String) {
	    camera.uniformMVP(shader)
	    recursiveDrawSkeleton(gl, camera, shader, uniformColorName)
	}
	
	/** Draw the model deformed by bones animations.
	  * The given `shader` must define a bone structure that at least contains two
	  * fields `MV` for the animation matrix and `MV3x3` for the upper 3x3 matrix
	  * extracted from `MV`.
	  *      
	  *     struct Bone {
	  *         mat4 MV;
	  *         mat3 MV3x3;
	  *     };
	  * 
	  *  In addition the shader must define an uniform variable named 'bone'
	  *  which is an array of the form:
	  *  
	  *      uniform Bone bone[n];
	  *  
	  *  Where `n` is the max number of bones used in the skeleton. The [[Camera]] class
	  *  being used, the shader must also define:
	  *  
	  *      uniform mat4 MV;    // Model-View matrix.
	  *      uniform mat3 MV3x3; // Upper 3x3 model-view matrix.
	  *      uniform mat4 MVP;   // Projection-Model-View matrix.
	  */
	def drawModel(gl:SGL, camera:Camera, model:Mesh, modelInstance:VertexArray, shader:ShaderProgram) {
		val orientation = Matrix4(); orientation.setIdentity
	    val forward = Matrix4(); forward.setIdentity
	    
	    recursiveComputeMatrices(orientation, forward)
	    recursiveUniformMatrices(shader, camera)
	    camera.uniformMVP(shader)
	    modelInstance.draw(model.drawAs)
	}
	
// Orientation
	
	def orientationIdentity() = orientation.setIdentity
	
	def orientationTranslate(tx:Double, ty:Double, tz:Double) = orientation.translate(tx, ty, tz)
	
	def orientationTranslate(t:NumberSeq3) = orientation.translate(t)
	    
	def orientationScale(sx:Double, sy:Double, sz:Double) = orientation.scale(sx, sy, sz)
	
	def orientationScale(s:NumberSeq3) = orientation.scale(s)

	def orientationRotate(angle:Double, x:Double, y:Double, z:Double) = orientation.rotate(angle, x, y, z)
	
	def orientationRotate(angle:Double, axis:NumberSeq3) = orientation.rotate(angle, axis)
	
// Animation
	
	def identity() = animation.setIdentity
	
	def translate(tx:Double, ty:Double, tz:Double) = animation.translate(tx, ty, tz)
	
	def translate(t:NumberSeq3) = animation.translate(t)
	
	def scale(sx:Double, sy:Double, sz:Double) = animation.scale(sx, sy, sz)
	
	def scale(s:NumberSeq3) = animation.scale(s)

	def rotate(angle:Double, x:Double, y:Double, z:Double) = animation.rotate(angle, x, y, z)
	
	def rotate(angle:Double, axis:NumberSeq3) = animation.rotate(angle, axis)

// Compute
	
	protected def recursiveDrawSkeleton(gl:SGL, camera:Camera, shader:ShaderProgram, uniformColorName:String) {
	    camera.pushpop {
	        camera.transformModel(orientation)
	        camera.transformModel(animation)
	        camera.uniformMVP(shader)

	        if(Bone.bone == null) Bone.bone = Bone.boneMesh.newVertexArray(gl)
	        shader.uniform(uniformColorName, color)
	        Bone.bone.draw(Bone.boneMesh.drawAs)
	        
	        children.foreach { child =>
	        	child.recursiveDrawSkeleton(gl, camera, shader, uniformColorName)
	        }
	    }
	}
	
	protected def recursiveComputeMatrices(orientation:Matrix4, forward:Matrix4) {
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