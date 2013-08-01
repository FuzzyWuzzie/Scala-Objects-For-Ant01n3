package org.sofa.opengl.avatar.renderer.sprite

import scala.math._
import scala.compat.Platform
import scala.collection.mutable.{HashMap, ArrayBuffer}

import org.sofa.math.Axis._
import org.sofa.math.{Vector3, NumberSeq3, SpatialCube, Axis}
import org.sofa.opengl.{Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, VertexAttribute}
import org.sofa.opengl.avatar.renderer.{Sprite, Screen, Avatar, AvatarIndex, AvatarState, AvatarIndex2D, NoSuchStateException}
import org.sofa.opengl.armature.{Armature}
import org.sofa.opengl.armature.behavior.{ArmatureBehavior, InParallelDynamic}


/** ArmatureSprite companion object defining the messages that can be received (change()), and
  * the [[ArmatureSprite.Animator]] class that can be used to tell how the sprite is
  * articulated. */
object ArmatureSprite {
	/** Set the armature to use withing this sprite. */
	case class SetArmature(resourceName:String) extends AvatarState

	/** Declare a new behavior for the sprite.
	  * If the `start` field is true, the behavior is activated as soon as added.
	  * As a behavior as a duration, several behavior can be activated at the same time. */
	case class AddBehavior(name:String, behavior:String, start:Boolean) extends AvatarState

	/** Activate a behavior for the sprite. Several behaviors can be activated at the same time. */
	case class StartBehavior(name:String) extends AvatarState

	/** Deactivate a behavior. If the behavior is cyclic or periodic, but still running, it is
	  * stopped. */
	case class StopBehavior(name:String) extends AvatarState
}


/** A sprite that displays one image at a time.
  *
  * The image sprite defines a set of images. Each image is associated to a state. Switching
  * the sprite state switches the image.
  * 
  * The size and position of the sprite can be animated separately for each sprite. */
class ArmatureSprite(name:String, screen:Screen, override val isIndexed:Boolean = false) extends Sprite(name, screen) {
	import ArmatureSprite._

	/** Shortcut to the GL. */
	protected val gl = screen.renderer.gl

	/** Current armature. */
	protected var armature:Armature = _

	/** All the states. */
	protected val behaviors = new HashMap[String,ArmatureBehavior]()

	/** Set of active behaviors. */
	protected val activity = new InParallelDynamic("activity")

	/** The spatial index anchor. */
	protected val idx:AvatarIndex = if(isIndexed) new AvatarIndex2D(this) else null

	override def index = idx

	override def begin() {
		super.begin
//		import VertexAttribute._
	}

	override def changePosition(newPos:NumberSeq3) {
		super.changePosition(newPos)
	}

	override def changeSize(newSize:NumberSeq3) {
		super.changeSize(newSize)
	}

	override def change(st:AvatarState) = st match {
		case SetArmature(res)                   ⇒ armature = screen.renderer.libraries.armatures.get(gl, res)
		case AddBehavior(name, behavior, start) ⇒ behaviors += (name → screen.renderer.libraries.behaviors.get(gl, behavior))
		case StartBehavior(name)                ⇒ startBehavior(name)
		case StopBehavior(name)                 ⇒ stopBehavior(name)
		case _                                  ⇒ super.change(st)
	}

	protected def startBehavior(name:String) {
		activity.add(Platform.currentTime, behaviors.get(name).getOrElse(throw new RuntimeException("behavior %s not found".format(name))))
	}

	protected def stopBehavior(name:String) {
		activity.remove(behaviors.get(name).getOrElse(throw new RuntimeException("behavior %s not found".format(name))))
	}

	def render() {
		val camera = screen.camera

		if(armature ne null) {
		// 	gl.enable(gl.BLEND)
			gl.disable(gl.DEPTH_TEST)
	    	gl.disable(gl.CULL_FACE)
			armature.display(gl, camera)
		// 	gl.disable(gl.BLEND)
		}
	}

	def animate() {
		activity.animate(Platform.currentTime)
	}
}