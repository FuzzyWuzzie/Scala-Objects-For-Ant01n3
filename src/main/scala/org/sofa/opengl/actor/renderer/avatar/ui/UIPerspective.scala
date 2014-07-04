package org.sofa.opengl.actor.renderer.avatar.ui

import org.sofa.math.{ Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default }
import org.sofa.opengl.surface.{ ActionChar=>SurfaceActionChar }
import org.sofa.opengl.actor.renderer.{ Screen }
import org.sofa.opengl.actor.renderer.{ Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarBaseStates }
import org.sofa.opengl.actor.renderer.{ AvatarEvent, AvatarSpatialEvent, AvatarMotionEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarKeyEvent, AvatarZoomEvent }
import org.sofa.opengl.actor.renderer.{ NoSuchAvatarException }

import org.sofa.opengl.{ SGL, ShaderProgram, CameraSpace }


/** Messages that can be sent to [[UIPerspective]] avatars to control the view. */
object UIPerspectiveStates {
	case class Orbit(thetaPhiRadius: Vector3) extends AvatarSpaceState
	case class RotateHorizontal(delta: Double) extends AvatarSpaceState
	case class RotateVertical(delta: Double) extends AvatarSpaceState
	case class Zoom(delta: Double) extends AvatarSpaceState
	case class Eye(eye: Vector3) extends AvatarSpaceState
	case class Focus(focus: Vector3) extends AvatarSpaceState
	case class Projection(width: Double, near: Double, far: Double) extends AvatarSpaceState
}


/** An avatar that does not render anything, but acts as a camera that can be positionned in space. */
class UIPerspective(name: AvatarName, screen: Screen)
		extends UIAvatar(name, screen) {

	var space = new UIAvatarSpacePerspective(this)

	var renderer = new UIAvatarRenderPerspective(this)

	def consumeEvent(event: AvatarEvent): Boolean = {
		event match {
			case ev:AvatarZoomEvent ⇒ {
				space.camera.rotateEyeHorizontal(ev.amount/1000.0)
				true
			}
			case ev:AvatarKeyEvent ⇒ {
				ev.actionChar match {
					case SurfaceActionChar.PageUp ⇒ {
						space.camera.eyeTraveling(-0.1)
						true 
					}
					case SurfaceActionChar.PageDown ⇒ {
						space.camera.eyeTraveling(0.1)
						true
					}
					case SurfaceActionChar.Left ⇒ { 
						space.camera.rotateEyeHorizontal(-0.05)
						true
					}
					case SurfaceActionChar.Right ⇒ {
						space.camera.rotateEyeHorizontal(0.05)
						true
					}
					case SurfaceActionChar.Up ⇒ { 
						space.camera.rotateEyeVertical(0.05)
						true
					}
					case SurfaceActionChar.Down ⇒ {
						space.camera.rotateEyeVertical(-0.05)
						true
					}
					case _ ⇒ { false }
				}
			}
			case _ ⇒ false
		}
	}
}


// ----------------------------------------------------------------------------------------------


class UIAvatarRenderPerspective(avatar: Avatar) extends UIAvatarRender(avatar) {
	override def render() {
		val gl = screen.gl

		gl.checkErrors
		super.render
	}
}


// ----------------------------------------------------------------------------------------------


class UIAvatarSpacePerspective(avatar: Avatar) extends UIAvatarSpace(avatar) {
	/** Ratiohw height / width. */
	protected[this] var ratiohw = 1.0

	val camera = new CameraSpace

	var scale1cm = 1.0

	protected val fromSpace = new Box3Default {
		pos.set(0, 0, 0)
		from.set(0, 0, 0)
		to.set(1, 1, 1)
		size.set(1, 1, 1)
	}

	protected val toSpace = new Box3PosCentered {
		pos.set(0, 0, 0)
		from.set(-1, -1, 1)
		to.set(1, 1, 1000)
	}

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def animateSpace() {
		val pspace = self.parent.space
		scale1cm   = pspace.scale1cm * (pspace.subSpace.size.x / fromSpace.size.x)
		ratiohw    = fromSpace.size.y / fromSpace.size.x

		// The toSpace represents the information needed by the frustum.
		// from and to x and y are the dimensions of the projection plane.
		// from.z is the near plane, to.z is the far clipping plane. We do
		// not change them excepted y that is always the same as x times
		// the ratio of the parent.

		toSpace.from.y = toSpace.from.x * ratiohw
		toSpace.to.y   = toSpace.to.x * ratiohw
	}

	override def pushSubSpace() {
		val space = self.screen.space
		val from = toSpace.from
		val to = toSpace.to

		// We do not need the thisSpace information excepted for the ratio h/w.

		space.pushProjection
		space.frustum(from.x, to.x, from.y, to.y, from.z)
		space.maxDepth = to.z
		space.push
		camera.setSpace(space)
		camera.lookAt
	}

	override def popSubSpace() {
		val space = self.screen.space

		space.pop
		space.popProjection
	}

	override def changeSpace(newState: AvatarSpaceState) {
		import UIPerspectiveStates._

		newState match {
			case AvatarBaseStates.Move(offset) ⇒ {
				camera.eyeCartesian(camera.cartesianEye.x + offset.x, camera.cartesianEye.y + offset.y, camera.cartesianEye.z + offset.z)
			}
			case AvatarBaseStates.MoveAt(position: Point3) ⇒ {
				camera.eyeCartesian(position.x, position.y, position.z)
			}
			case AvatarBaseStates.Resize(size) ⇒ {
				toSpace.from.x = -size.x / 2
				toSpace.to.x = size.x / 2
				toSpace.from.z = size.z
			}
			case Orbit(tpr) ⇒ {
				camera.eyeSpherical(tpr.x, tpr.y, tpr.z)
			}
			case RotateHorizontal(delta) ⇒ {
				camera.rotateEyeHorizontal(delta)
			}
			case RotateVertical(delta) ⇒ {
				camera.rotateEyeVertical(delta)
			}
			case Zoom(delta) ⇒ {
				camera.eyeTraveling(delta)
			}
			case Eye(eye) ⇒ {
				camera.eyeCartesian(eye.x, eye.y, eye.z)
			}
			case Focus(focus) ⇒ {
				camera.setFocus(focus.x, focus.y, focus.z)
			}
			case Projection(width, near, far) ⇒ {
				toSpace.from.x = -width / 2
				toSpace.to.x = width / 2
				toSpace.from.z = near
				toSpace.to.z = far
			}
			case _ ⇒ super.changeSpace(newState)
		}
	}
}