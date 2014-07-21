package org.sofa.opengl.actor.renderer.avatar.game

import scala.compat.Platform

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Box3Sized}
import org.sofa.opengl.{Texture, ShaderResource, ModelResource, TextureResource, TexParams}
import org.sofa.opengl.armature.{Armature}
import org.sofa.behavior.{Behavior}
import org.sofa.opengl.armature.behavior.{ArmatureBehavior}
import org.sofa.opengl.actor.renderer.{Screen}
import org.sofa.opengl.actor.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarRenderState, AvatarBaseStates}
import org.sofa.opengl.actor.renderer.{AvatarEvent, AvatarSpatialEvent, AvatarMotionEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarKeyEvent, AvatarZoomEvent}
import org.sofa.opengl.actor.renderer.{NoSuchAvatarException}

import org.sofa.opengl.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.opengl.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh, HexaTilesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


// == Renders ====================================================================


case class IsoEntityConfig(armature:String, behavior:String, mask:String) extends AvatarRenderState {}


object IsoEntity {
	/** Shortcut to the value of sqrt(3). */
	final val Sqrt3 = math.sqrt(3).toFloat
}


class IsoEntityRender(avatar:Avatar) extends IsoRender(avatar) with IsoRenderUtils {

	fillColor = Rgba.White

	lineColor = Rgba.Black

	protected[this] var armature:Armature = null

	protected[this] var shader:ShaderProgram = null

	protected[this] var texColor:Texture = null

	protected[this] var texMask:Texture = null

	protected[this] var behavior:Behavior = null

	protected[this] var world:Avatar = null

	override def changeRender(state:AvatarRenderState) {
		state match {
			case IsoEntityConfig(armature, behavior, mask) => init(armature, behavior, mask)
			case _                                         => super.changeRender(state)
		}
	}

	protected def init(armature:String, behavior:String, mask:String) {
		val gl   = self.screen.gl
		this.armature = screen.libraries.armatures.get(gl, armature)
		this.behavior = screen.libraries.behaviors.getOr(gl, behavior) { null }
		this.shader   = this.armature.shader
		this.texColor = this.armature.texture
		this.texMask  = screen.libraries.textures.get(gl, mask)

		if(this.behavior ne null)
			this.behavior.start(Platform.currentTime)

		world = screen.avatar(AvatarName("root.world")).getOrElse(throw new RuntimeException("no world avatar ??"))
	}

	override def animateRender() {
		if(behavior ne null) {
			val T = Platform.currentTime

			if(! behavior.finished(T))
				behavior.animate(T)
		}
	}

	override def render() {
		val space = self.space
		val text  = screen.textLayer
		val gl    = self.screen.gl

		space.pushSubSpace
			if(armature ne null) {
				val sunDir = world.renderer.asInstanceOf[IsoWorldRender].sunDir
//				fillAvatarBox

				gl.enable(gl.BLEND)
				gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)		// Premultiplied alpha
				gl.disable(gl.DEPTH_TEST)
				gl.disable(gl.CULL_FACE)
				shader.use
				shader.uniform(IsoWorld.SunDir, sunDir)
				texColor.bindUniform(gl.TEXTURE0, shader, "texColor")
				texMask.bindUniform(gl.TEXTURE1, shader, "texMask")
				armature.displayArmature(gl, screen.space)
				gl.disable(gl.BLEND)
			}

			self.renderSubs

		space.popSubSpace		
	}
}


// == Spaces =====================================================================


/** Space for an entity.
  * The entity does not resizes the space, it only translates to its position.
  * The while game works in the world coordinates. */
class IsoEntitySpace(avatar:Avatar) extends IsoSpace(avatar) {
	var scale1cm = 1.0

	var fromSpace = new Box3Sized {
		pos.set(0, 0, 0)
		size.set(1, 1, 1)
	}

	var toSpace = fromSpace

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def changeSpace(newState:AvatarSpaceState) {
		import IsoEntity._

		newState match {
			case AvatarBaseStates.Move(offset) => {
				println("Cannot move an iso entity, use MoveAt")
			}
			case AvatarBaseStates.MoveAt(position:Point3) => {
				fromSpace.pos.x = (position.x * Sqrt3 * 0.5) + (position.y * Sqrt3 * 0.5)
				fromSpace.pos.y = (position.y * 0.5) - (position.x * 0.5)
				fromSpace.pos.z = 0
			}
			case AvatarBaseStates.Resize(size) => {
				println("Cannot resize an iso entity")
			}
			case _ => super.changeSpace(newState)
		}
	}

	override def animateSpace() {
	}

	override def pushSubSpace() {
		val space  = self.screen.space

 		scale1cm = self.parent.space.scale1cm

 		space.push
 		space.translate(fromSpace.pos.x, fromSpace.pos.y, 0)
	}

	override def popSubSpace() {
		self.screen.space.pop
	}
}


// == Avatars ====================================================================


class IsoEntity(name:AvatarName, screen:Screen)
	extends IsoAvatar(name, screen) {

	var space = new IsoEntitySpace(this) 

	var renderer = new IsoEntityRender(this)

	def consumeEvent(event:AvatarEvent):Boolean = {
		//println("%s ignore event %s".format(name, event))
		false
	}
}