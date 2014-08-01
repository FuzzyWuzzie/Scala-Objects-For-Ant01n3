package org.sofa.gfx.renderer.avatar.ui

import org.sofa.math.{ Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default, Point4 }
import org.sofa.gfx.{ ShaderResource }
import org.sofa.gfx.renderer.{ Screen }
import org.sofa.gfx.renderer.{ Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState }
import org.sofa.gfx.surface.event._
import org.sofa.gfx.renderer.{ NoSuchAvatarException }

import org.sofa.gfx.{ SGL, ShaderProgram } //, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.gfx.mesh.{ TrianglesMesh, Mesh, VertexAttribute, LinesMesh } //, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


class UIAvatarRenderRoot(avatar: Avatar) extends UIAvatarRender(avatar) with UIrenderUtils {
	color = Rgba.Yellow

	override def render() {
		val gl = screen.gl
		val text  = screen.textLayer

		gl.clearColor(Rgba.White)
		gl.clear(gl.COLOR_BUFFER_BIT)

		gl.lineWidth(1f)
		gl.disable(gl.DEPTH_TEST)
		gl.enable(gl.BLEND)
		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
		//gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)		// Premultiplied alpha

		gl.checkErrors

		super.render

		// self.space.pushSubSpace
		// text.font("Ubuntu-B.ttf", 20)
		// text.color(Rgba.Red)
		// val size = self.space.subSpace.size
		// text.string("ROOT", size.x/2, size.y/2, 0, screen.space)
		// fill
		// self.renderSubs
		// self.space.popSubSpace		
	}
}


// ----------------------------------------------------------------------------------------------


class UIAvatarSpaceRoot(avatar: Avatar) extends UIAvatarSpace(avatar) {
	/** Ratiohw height / width. */
	protected[this] var ratiohw = 1.0

	var scale1cm = 1.0

	protected val fromSpace = new Box3Default {
		pos.set(0, 0, 0)
		from.set(-1, -1, -1)
		to.set(1, 1, 1)
		size.set(2, 2, 2)
	}

	protected val toSpace = new Box3Default {
		pos.set(0, 0, 0)
		from.set(0, 0, 0)
		to.set(1, 1, 1)
		size.set(1, 1, 1)
	}

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def animateSpace() {
		val screen = avatar.screen
		val surface = screen.surface
		ratiohw = surface.height.toDouble / surface.width.toDouble
		val dpc = 37.0 // 37 dots-pixels per centimeter
		scale1cm = 1.0 / (screen.surface.width / dpc) // One centimeter in the sub-space equals this

		toSpace.to.set(1, 1 * ratiohw, 1)
		toSpace.size.set(1, 1 * ratiohw, 1)

		//println("------------------")
		//println(s"# layout root scale1cm=${scale1cm} ratiohw=${ratiohw} size(2, ${ratiohw*2}) subSize(${toSpace.size(0)}, ${toSpace.size(1)})")
		layoutSubs
	}

	override def pushSubSpace() {
		val space = self.screen.space

		// we pass from x [-1..1] positive right and y [-1..1] positive top to
		// x [0..1] positive right and y [0..ratio] positive down.

		space.push
		space.translate(-1, 1, 0)
		space.scale(2, -2 / ratiohw, 1)
	}

	override def popSubSpace() { self.screen.space.pop }

	def layoutSubs() {
		// A layout that put each child at the same position, filling the avaiable space, one above the other.
		var i = 0
		self.foreachSub { sub ⇒
			sub.space.thisSpace.setSize(toSpace.size(0), toSpace.size(1), 1)
			sub.space.thisSpace.setPosition(0, 0, 0)
			//println("    | layout %d %s".format(i, sub))
			i += 1
		}
	}
}


// ----------------------------------------------------------------------------------------------


/** An avatar that creates a space
  * where width is 1 and with x positive right and y is 1/ratio width/height positive down.
  * The origin is the top-left corner of the avatar. The renderer only clears the frame buffer
  * with white.
  */
class UIRoot(name: AvatarName, screen: Screen)
		extends UIAvatar(name, screen) {

	var space = new UIAvatarSpaceRoot(this)

	var renderer = new UIAvatarRenderRoot(this)

	def consumeEvent(event:Event): Boolean = false
}


/** A special UIRoot to test events dispatching. */
class UIRootEvents(name: AvatarName, screen: Screen)
		extends UIAvatar(name, screen) {

	var space = new UIAvatarSpaceRoot(this)

	var renderer = new UIAvatarRenderRoot(this)

	def consumeEvent(event:Event): Boolean = {
		println("%s event %s".format(name, event))
		false
	}
}