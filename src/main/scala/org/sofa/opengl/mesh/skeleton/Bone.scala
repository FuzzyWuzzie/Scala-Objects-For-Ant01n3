package org.sofa.opengl.mesh.skeleton

import scala.collection.mutable.ArrayBuffer
import org.sofa.opengl.{VertexArray, SGL, Camera, ShaderProgram}
import org.sofa.opengl.mesh.{VertexAttribute, Mesh, BoneLineMesh}
import org.sofa.math.{Matrix4, Rgba, NumberSeq3, Point4}

import javax.media.opengl._
import GL._
import GL2._
import GL2ES2._
import GL3._

/** Bone companion object. */
object Bone {
    val boneMesh = new BoneLineMesh()
    
    var bone:VertexArray = null
}

// TODO this bone class makes more computation than needed
// the or=parent.orientation*orientation is done at each step of animation, but could be done only once !
// the or.inverse is also done at each step but could be done only once !
// -> save them in the bone once and for all !
//    -> keep inverseOrientation but not not compute it at each step.
//    -> add pose = parent.orientation*orientation 

/** A simple bone hierarchy. */
class Bone(val id:Int) {
// Attributes

	/** Bone optional name. */    
	var name = "bone"

    /** The matrix that transforms the bone at its original location in the untransformed model mesh.
      * This matrix will not change during animation, once the skeleton is set up in its initial pose. */
	val orientation:Matrix4 = Matrix4()
	
	/** The matrix that animates the bone. */
	val animation:Matrix4 = Matrix4()
	
	/** Inverse of [[orientation]] matrix. This matrix will not change during animation once the
	  * skeleton is set up in its initial pose. */
	val inverseOrientation:Matrix4 = Matrix4()
	
	/** Final matrix computed for the vertices attached to this bone. */
	val TX:Matrix4 = Matrix4()
	
	/** Bone color. */
	var color = new Rgba(1, 1, 1, 1)
	
	/** Parent bone. */
	var parent:Bone = null
	
	/** Child bones. */
	val children = new ArrayBuffer[Bone]()

	/** Bone lenth (from base (joint) to childs base). */
	var length = 1.0

// Construction
	
	orientation.setIdentity
	animation.setIdentity
	inverseOrientation.setIdentity
	TX.setIdentity
	
// Commands
	
	/** Change the bone name. The default name is "bone". Names need not be unique. */
	def setName(newName:String) { name = newName }

	/** Add a child bone to the hierarchy. The child matrices are untouched. */
	def addChild(id:Int):Bone = {
	    val child = new Bone(id)
	    
	    children += child
	    child.parent = this
	    child
	}

	/** Add a child bone to the hierarchy. The child matrices are untouched, the given
	  * child must not already have a parent. */
	def addChild(bone:Bone) {
		assert(bone.parent eq null)
		children += bone
		bone.parent = this
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
	    camera.setUniformMVP(shader)
	    recursiveDrawSkeleton(gl, camera, shader, uniformColorName)
	}

	def drawSkeleton(gl:SGL, camera:Camera, shader:ShaderProgram) {
	    camera.setUniformMVP(shader)
	    recursiveDrawSkeleton(gl, camera, shader, null)
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
	// def drawModel(gl:SGL, camera:Camera, model:Mesh, modelInstance:VertexArray, shader:ShaderProgram) {
	// 	val orientation = Matrix4(); orientation.setIdentity
	//     val forward = Matrix4(); forward.setIdentity
	    
	//     recursiveComputeMatrices(orientation, forward)
	//     recursiveUniformMatrices(shader, camera)
	//     camera.uniformMVP(shader)
	//     //camera.setUniformMVP(shader)
	//     modelInstance.draw(model.drawAs)
	// }
	
	/** Install the bones matrices in the given `shader`.
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
	  *  Where `n` is the max number of bones used in the skeleton.
	  */
	def uniform(shader:ShaderProgram) {
		val orientation = Matrix4(); orientation.setIdentity
	    val forward = Matrix4(); forward.setIdentity
	    
	    recursiveComputeMatrices(orientation, forward)
	    recursiveUniformMatrices(shader)
	}

	/** Compute the length of this bone and recursively all its children from joint positions.
	  * The last bone has always a length of 1 since we have no way to compute it. */
	def computeLength() {
		val from = Point4(0, 0, 0, 1)

		recursiveComputeLength(orientation * from) 
	}

	protected def recursiveComputeLength(from:Point4) {
		if(!children.isEmpty) {
			val to = children.head.orientation * from
			length = from.distance(to)
			children.foreach { _.recursiveComputeLength(to) }
		} else {
			length = 1.0 /// How to compute last bone length ??
		}
	}
	
// Orientation -- Allow to give the initial pose of bones.
	
	def poseIdentity() = orientation.setIdentity
	
	def poseTranslate(tx:Double, ty:Double, tz:Double) = orientation.translate(tx, ty, tz)
	
	def poseTranslate(t:NumberSeq3) = orientation.translate(t)
	    
	def poseScale(sx:Double, sy:Double, sz:Double) = orientation.scale(sx, sy, sz)
	
	def poseScale(s:NumberSeq3) = orientation.scale(s)

	def poseRotate(angle:Double, x:Double, y:Double, z:Double) = orientation.rotate(angle, x, y, z)
	
	def poseRotate(angle:Double, axis:NumberSeq3) = orientation.rotate(angle, axis)

	def setPose(matrix:Matrix4) = orientation.copy(matrix)
	
// Animation -- Allow to deform the model.
	
	def identity() = animation.setIdentity
	
	def translate(tx:Double, ty:Double, tz:Double) = animation.translate(tx, ty, tz)
	
	def translate(t:NumberSeq3) = animation.translate(t)
	
	def scale(sx:Double, sy:Double, sz:Double) = animation.scale(sx, sy, sz)
	
	def scale(s:NumberSeq3) = animation.scale(s)

	def rotate(angle:Double, x:Double, y:Double, z:Double) = animation.rotate(angle, x, y, z)
	
	def rotate(angle:Double, axis:NumberSeq3) = animation.rotate(angle, axis)

	def setAnimation(matrix:Matrix4) = animation.copy(matrix)

// Compute
	
	/** Recursively draw the whole sub-skeleton. */
	protected def recursiveDrawSkeleton(gl:SGL, camera:Camera, shader:ShaderProgram, uniformColorName:String) {
	    if(Bone.bone eq null)
	    	Bone.bone = Bone.boneMesh.newVertexArray(gl, shader, VertexAttribute.Vertex -> "position", VertexAttribute.Color -> "color")
	    
	    camera.pushpop {
//println("drawing bone %s :%n%s".format(name, orientation))
	        camera.transformModel(orientation)
	        camera.transformModel(animation)

	        if(uniformColorName ne null) shader.uniform(uniformColorName, color)
	        camera.pushpop {
	        	camera.scaleModel(1, length, 1)
		        camera.setUniformMVP(shader)
		        Bone.bone.draw(Bone.boneMesh.drawAs)
	        }
	        
	        children.foreach { child =>
	        	child.recursiveDrawSkeleton(gl, camera, shader, uniformColorName)
	        }
	    }
	}
	
	/** Compute the [[orientation]], [[inverseOrientation]], [[TX]] matrices for this sub-skeleton. */
	protected def recursiveComputeMatrices(orientation:Matrix4, forward:Matrix4) {
	    // Concatenate the parents bone orientation with this bone orientation.
	    val localOrientation = orientation * this.orientation 	// TODO we could save this matrix to go faster !
	    // Save the inverse.
	    inverseOrientation.copy(localOrientation.inverse)		// TODO we could save this matrix to go faster !
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
	
	/** Apply the [[TX]] matrix as `MV` and `MV3x3` for this sub-skeleton. */
	protected def recursiveUniformMatrices(shader:ShaderProgram) {
	    // Store in the shader each bone final TX matrix.
	    shader.uniformMatrix("bone[%d].MV".format(id), TX)
	    shader.uniformMatrix("bone[%d].MV3x3".format(id), TX.top3x3)
	    //shader.uniform("bone[%d].color", color)
	    
	    children.foreach { child =>
	    	child.recursiveUniformMatrices(shader)
	    }
	}
}