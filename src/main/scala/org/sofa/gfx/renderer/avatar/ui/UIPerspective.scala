package org.sofa.gfx.renderer.avatar.ui

import org.sofa.math.{ Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default }
import org.sofa.gfx.surface.event.{ ActionKey⇒SurfaceActionChar }
import org.sofa.gfx.renderer.{ Screen }
import org.sofa.gfx.renderer.{ Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState, AvatarBaseStates, NoSuchAvatarStateException, AvatarStateException }
import org.sofa.gfx.surface.event._
import org.sofa.gfx.renderer.{ NoSuchAvatarException }

import org.sofa.gfx.{ SGL, ShaderProgram, CameraSpace }


/** Messages that can be sent to [[UIPerspective]] avatars to control the view. */
object UIPerspectiveStates {
	case class Orbit(thetaPhiRadius: Vector3) extends AvatarSpaceState
	case class RotateHorizontal(delta: Double) extends AvatarSpaceState
	case class RotateVertical(delta: Double) extends AvatarSpaceState
	case class Zoom(delta: Double) extends AvatarSpaceState
	case class Eye(eye: Vector3) extends AvatarSpaceState
	case class Focus(focus: Vector3) extends AvatarSpaceState
	case class Projection(width: Double, near: Double, far: Double) extends AvatarSpaceState
	case class ScrollSpeed(speed:Double) extends AvatarState
	case class ScaleSpeed(speed:Double) extends AvatarState
}


/** An avatar that does not render anything, but defines a space acting as a camera
  * that can be positionned in space. */
class UIPerspective(name: AvatarName, screen: Screen)
		extends UIAvatar(name, screen) {

	var space = new UIAvatarSpacePerspective(this)

	var renderer = new UIAvatarRenderBase(this)

	var step = 0.2

	var scaleSpeed = 0.05

	var scrollSpeed = 0.5

	def camera:CameraSpace = space.camera

	override def change(state:AvatarState) {
		import UIPerspectiveStates._

		if(! changed(state)) {
			state match {
				case AvatarBaseStates.ChangeRenderer(renderer) ⇒ {
					renderer match {
						case r:UIAvatarRenderBase ⇒ {
							this.renderer = r
							r.setAvatar(this)
							self.screen.requestRender
						}
						case _ ⇒ throw new AvatarStateException("the renderer type must be UIAvatarRenderBase")
					}
				}
				case ScrollSpeed(speed) ⇒ { scrollSpeed = speed }
				case ScaleSpeed(speed) ⇒ { scaleSpeed = speed }
				case _ ⇒ throw new NoSuchAvatarStateException(state)
			}
		}
	}

	def consumeEvent(event: Event): Boolean = {
		event match {
			case scroll:ScrollEvent ⇒ {
				space.camera.rotateEyeHorizontal(scroll.delta.x * step * scrollSpeed)
				space.camera.rotateEyeVertical(-scroll.delta.y * step * scrollSpeed)
				self.screen.requestRender
				true
			}
			case scale:ScaleEvent ⇒ {
				space.camera.eyeTraveling(scale.delta * scaleSpeed)
				self.screen.requestRender
				true
			}
			case k:ActionKeyEvent ⇒ {
			    import org.sofa.gfx.surface.event.ActionKey._

			    if(k.isEnd) {
					k.key match {
		    			case PageUp   ⇒ space.camera.eyeTraveling(-step)
		    			case PageDown ⇒ space.camera.eyeTraveling(step)
		    			case Up       ⇒ space.camera.rotateEyeVertical(step)
		    			case Down     ⇒ space.camera.rotateEyeVertical(-step)
		    			case Left     ⇒ space.camera.rotateEyeHorizontal(-step)
		    			case Right    ⇒ space.camera.rotateEyeHorizontal(step)
		    			case _        ⇒ {}
					}
					self.screen.requestRender
					true
				}
				else {
					renderer.consumeEvent(event)
				}	
			}
			case _ ⇒ {
				renderer.consumeEvent(event)
			}
		}
	}
}


// ----------------------------------------------------------------------------------------------


class UIAvatarSpacePerspective(avatar: Avatar) extends UIAvatarSpace(avatar) {
	/** Ratiohw height / width. */
	protected[this] var ratiohw = 1.0

	val camera = new CameraSpace

//	var scale1cm = 1.0

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
		//space.projectionIdentity
		space.frustum(from.x, to.x, from.y, to.y, from.z)
		space.maxDepth = to.z

		space.push
		//space.viewIdentity
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

		val screen = self.screen

		newState match {
			case AvatarBaseStates.Move(offset) ⇒ {
				camera.eyeCartesian(camera.cartesianEye.x + offset.x, camera.cartesianEye.y + offset.y, camera.cartesianEye.z + offset.z)
				screen.requestRender
			}
			case AvatarBaseStates.MoveAt(position: Point3) ⇒ {
				camera.eyeCartesian(position.x, position.y, position.z)
				screen.requestRender
			}
			case AvatarBaseStates.Resize(size) ⇒ {
				toSpace.from.x = -size.x / 2
				toSpace.to.x = size.x / 2
				toSpace.from.z = size.z
				screen.requestRender
			}
			case Orbit(tpr) ⇒ {
				camera.eyeSpherical(tpr.x, tpr.y, tpr.z)
				screen.requestRender
			}
			case RotateHorizontal(delta) ⇒ {
				camera.rotateEyeHorizontal(delta)
				screen.requestRender
			}
			case RotateVertical(delta) ⇒ {
				camera.rotateEyeVertical(delta)
				screen.requestRender
			}
			case Zoom(delta) ⇒ {
				camera.eyeTraveling(delta)
				screen.requestRender
			}
			case Eye(eye) ⇒ {
				camera.eyeCartesian(eye.x, eye.y, eye.z)
				screen.requestRender
			}
			case Focus(focus) ⇒ {
				camera.setFocus(focus.x, focus.y, focus.z)
				screen.requestRender
			}
			case Projection(width, near, far) ⇒ {
				toSpace.from.x = -width / 2
				toSpace.to.x = width / 2
				toSpace.from.z = near
				toSpace.to.z = far
				screen.requestRender
			}
			case _ ⇒ super.changeSpace(newState)
		}
	}
}