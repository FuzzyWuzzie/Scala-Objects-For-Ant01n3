package org.sofa.gfx.mesh.skeleton

import scala.collection.mutable.{ArrayBuffer, HashMap}
import org.sofa.gfx.{VertexArray, SGL, Camera, ShaderProgram}
import org.sofa.gfx.mesh.{VertexAttribute, Mesh, BoneLineMesh}
import org.sofa.math.{Matrix4, Rgba, NumberSeq3, Point4}


/** Bone companion object. */
object Bone {
    val boneMesh = new BoneLineMesh()
    
    var bone:VertexArray = null
}

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

	/** Children by their name. */
	val byName = new HashMap[String,Int]()

	/** Bone lenth (from base (joint) to childs base). */
	var length = 1.0

	/** Set to false after a call to computePose(). This step will trigger
	  * th computation of the orientation and inverseOrientation. They will
	  * remain unchanged until the pose (which is usually only setup at start)
	  * is changed anew. */
	private var needRecomputePose = true

// Construction
	
	orientation.setIdentity
	animation.setIdentity
	inverseOrientation.setIdentity
	TX.setIdentity
	
// Commands
	
	/** Called when a child name changed. */
	protected def childRenamed(oldName:String, newName:String) {
		byName.get(oldName) match {
			case Some(x) => { byName.remove(oldName); byName += ((newName, x)) }
			case _ => throw new RuntimeException("no such bone %s in parent %s".format(oldName, name))
		}
	}

	/** Change the bone name. The default name is "bone". Names need not be unique. */
	def setName(newName:String) { 
		val oldName = name
		name = newName
		if(parent ne null)
			parent.childRenamed(oldName, newName)
	}

	/** Add a child bone to the hierarchy. The child matrices are untouched. */
	def addChild(id:Int):Bone = {
	    val child = new Bone(id)
	    
	    child.setName("%d".format(id))
	    
	    val index = children.size
	    children += child
	    child.parent = this
	    byName += ((child.name, index)) 

	    child
	}

	/** Add a child bone to the hierarchy. The child matrices are untouched, the given
	  * child must not already have a parent. */
	def addChild(bone:Bone) {
		assert(bone.parent eq null)
		val index = children.size
		children += bone
	    byName += ((bone.name, index)) 
		bone.parent = this
	}
	
	/** Child bone `index`. */
	def apply(index:Int):Bone = children(index)

	/** Child bone by `name` or throw an exception if not found. */
	def apply(name:String):Bone = {
		byName.get(name) match {
			case Some(x) => children(x)
			case _       => throw new RuntimeException("no such bone %s in parent %s".format(name, this.name))
		}
	}

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

	/** Draw a representation of the skeleton.
	  * 
	  * The [[Camera]] class being used, the shader must define:
	  *  
	  *      uniform mat4 MV;    // Model-View matrix.
	  *      uniform mat3 MV3x3; // Upper 3x3 model-view matrix.
	  *      uniform mat4 MVP;   // Projection-Model-View matrix.
	  */
	def drawSkeleton(gl:SGL, camera:Camera, shader:ShaderProgram) {
	    camera.uniformMVP(shader)
	    recursiveDrawSkeleton(gl, camera, shader, null)
	}
	
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
	    val forward = Matrix4()

	    forward.setIdentity
		computePose	    
	    recursiveComputeAnimation(forward)
	    recursiveUniformMatrices(shader)
	}

	/** Compute the length of this bone and recursively all its children from joint positions.
	  * The last bone has always a length of 1 since we have no way to compute it. */
	def computeLength() {
		val local = Matrix4(orientation)

		recursiveComputeLength(local)
	}
	
// Orientation -- Allow to give the initial pose of bones.
	
	def poseIdentity() = { needRecomputePose = true; orientation.setIdentity }
	
	def poseTranslate(tx:Double, ty:Double, tz:Double) = { needRecomputePose = true; orientation.translate(tx, ty, tz) }
	
	def poseTranslate(t:NumberSeq3) = { needRecomputePose = true; orientation.translate(t) }
	    
	def poseScale(sx:Double, sy:Double, sz:Double) = { needRecomputePose = true; orientation.scale(sx, sy, sz) }
	
	def poseScale(s:NumberSeq3) = { needRecomputePose = true; orientation.scale(s) }

	def poseRotate(angle:Double, x:Double, y:Double, z:Double) = { needRecomputePose = true; orientation.rotate(angle, x, y, z) }
	
	def poseRotate(angle:Double, axis:NumberSeq3) = { needRecomputePose = true; orientation.rotate(angle, axis) }
	
	def poseMultBy(matrix:Matrix4) = { needRecomputePose = true; orientation.multBy(matrix) }

	def setPose(matrix:Matrix4) = { needRecomputePose = true; orientation.copy(matrix) }

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
	        camera.transform(orientation)
	        camera.transform(animation)

	        if(uniformColorName ne null) shader.uniform(uniformColorName, color)
	        camera.pushpop {
		       	camera.scale(1, length, 1)
		        camera.uniformMVP(shader)
		        Bone.bone.draw(Bone.boneMesh.drawAs(gl))
	        }
	        
	        children.foreach { child =>
	        	child.recursiveDrawSkeleton(gl, camera, shader, uniformColorName)
	        }
	    }
	}
	
	/** Compute the animation matrix for this sub-skeleton. This must be done at each modification of the animation. */
	protected def recursiveComputeAnimation(forward:Matrix4) {
	    // Concatenate to the whole transformation (orientation + animation) this bone orientation...
	    val localForward = forward * this.orientation
	    // ... and this bone animation.
	    localForward *= this.animation
	    // The final matrix is then multiplied by the inverse bone orientation since the mesh
	    // points are not naturally transformed by the bone initial orientation).
	    TX.copy(localForward * inverseOrientation)
	    // Process the children.
	    children.foreach { child =>
			child.recursiveComputeAnimation(localForward)
	    }
	    // At the end of the process each bone as a TX matrix setup. 
	}

	/** Recursivelly compute the lenth of each bone excepted the last one that
	  * cannot be computed using this method. */
	protected def recursiveComputeLength(local:Matrix4) {
		if(! children.isEmpty) {
			val from = local * Point4(0,0,0,1)
			local   *= children.head.orientation
			val to   = local * Point4(0,0,0,1)
			length   = from.distance(to)

			children.foreach { _.recursiveComputeLength(local) }
		} else {
			length = 1.0 /// How to compute last bone length ??
		}
	}

	/** Compute the main orientation inverse matrix used while animating for the whole
	  * bone hierarchy. This matrix is also often called inverse bind matrix. If this
	  * method is called from a bone that is not root of a hierarchy, the root is first
	  * searched to start at it. */
	protected def computePose() {
		if(needRecomputePose) {
			// Go to the root of the hierachy if needed.

			var me = this

			while(me.parent ne null)
				me = me.parent 			

			// Now compute the pose (the inverseOrientation matrices (or inverse bind matrices)).

			val orientation = Matrix4()
			orientation.setIdentity
			me.recursiveComputePose(orientation)
		}
	}

	/** Recursively compute the inverse orientation matrices for the hierarchy. Mark
	  * each bone `needRecomputePose` flag when done to avoid recomputing the inverse
	  * until the initial pose change. Usually this is done only once. */
	protected def recursiveComputePose(orientation:Matrix4) {
	    // Concatenate the parents bone orientation with this bone orientation.
	    val localOrientation = orientation * this.orientation
	    
	    // Save the inverse.
	    inverseOrientation.copy(localOrientation.inverse)

	    // Compute recursivelly.
		children.foreach { _.recursiveComputePose(localOrientation) }
		
		// Mark as done.
		needRecomputePose = false
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