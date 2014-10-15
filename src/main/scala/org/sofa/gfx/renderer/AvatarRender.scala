package org.sofa.gfx.renderer

import scala.math._
import scala.collection.mutable.{ArrayBuffer, HashMap}
import akka.actor.{ActorRef}

import org.sofa.math.{Vector3, Point3, NumberSeq3, Rgba}
import org.sofa.collection.{SpatialCube, SpatialHash, SpatialHashException}
import org.sofa.gfx.{Space, TextureFramebuffer}
import org.sofa.gfx.transforms.{SpaceTransform}


/** The base type of all messages going toward an avatar renderer. */
trait AvatarRenderState extends AvatarState {}


/** A renderer for an avatar.
  *
  * The role of the renderer is to change the appearance of the avatar.
  * The renderer acts knowing the space of the avatar.
  */
trait AvatarRender {
	/** The avatar being rendered. */
	protected def self:Avatar 

	/** The screen where the avatar is rendered. */
	def screen:Screen

	/** Called at each frame before `render()` when the avatar is animated. */
	def animateRender() {}

	/** Change the renderer. */
	def changeRender(state:AvatarRenderState) {}

	/** By default push the avatar sub-space, render the sub-avatars then
	  * pop the avatar sub-space (all this is done by the `renderSubs()`
	  * utility method). You can override this behavior, and eventually
	  * render each sub when you want (and if you want). All the resources
	  * to draw are located in the `screen`  or given by the `avatar`. See
	  * [[Avatar.space]], [[Screen.gl]], [[Screen.surface]], and [[Screen.space]]. */
	def render() {
		self.space.pushSubSpace
		self.renderSubs
		self.space.popSubSpace		
	}

	/** If [[Avatar]] `hasLayer()` is true, install an offscreen frame buffer to render
	  * the avatar separatly, and bind it. The frame buffer is automatically created if
	  * needed (see `disposeLayer()` to clear when no more used). A special space is
	  * pushed on the space stack to accomodate the new frame buffer. The [[AvatarSpace]]
	  * `subSpaceLayer()` method is then called to install the specific sub-space for
	  * rendering. This method must put the space in an equivalent state than [[AvatarSpace]]
	  * `pushSubSpace()` so that rendering of sub-avatars is transparently transfered to
	  * the offscreen frame buffer. This offscreen frame buffer then provides a texture
	  * that can be used in lieu of the whole sub hierarchy of avatars. Each call to
	  * `pushLayer()` must be matched by a corresponding call to `popLayer()`. */
	def pushLayer() {
		if(self.hasLayer) {
			if(self.layer eq null) {
				val space    = self.space
				val dpc      = screen.dpc
				val s1cm     = space.scale1cm
				val width    = (space.subSpace.sizex / s1cm) * dpc
				val height   = (space.subSpace.sizey / s1cm) * dpc
				val widthPx  = ceil(width).toInt
				val heightPx = ceil(height).toInt
				
				self.layer = new AvatarLayer(new TextureFramebuffer(screen.gl, widthPx, heightPx, true, screen.surface.multiSampling),
									widthPx/width, heightPx/height)

				println("* %s fb fpx(%f, %f) -> px(%d, %d) -> factor(%f, %f)".format(self.name, width, height, widthPx, heightPx, self.layer.scalex, self.layer.scaley))
			}
			
			val space  = screen.space
			val pixels = screen.pixelSpace
			val fb     = self.layer.fb
			
			// XXX Problem :
			//
			// we go from a space with (w,h) size in real numbers to a buffer
			// with (wpx, hpx) size in integer pixels. To find the size in pixels,
			// we use the dpc of the screen and the size in cm of the avatar.
			//
			// Naturally converting reals to integers will introduce a rounding
			// error. We ensure with a "ceil" that the size in pixels is always
			// larger.
			//
			// Now, how do we handle the fact we draw on a slightly larger area ?
			//
			// Proposition 1:
			//	Introduce a scaling in the "space" from the real size to the new size
			//  to reduce the drawing space.
			//  Then when drawing the quad that represent the avatar, augment it by
			//  the same scale factors..

			space.push
			space.pushProjection
			space.pushViewport(fb.width, fb.height)
			space.projectionIdentity
			space.viewIdentity

//println("** pushlayer (%f, %f)".format(1.0/self.layer.scalex, 1.0/self.layer.scaley))
			//space.scale(1.0/self.layer.scalex, 1.0/self.layer.scaley, 1)

			
			pixels.push
			pixels.pushProjection
			pixels.pushViewport(fb.width, fb.height)
			pixels.orthographicPixels()
			pixels.viewIdentity

			self.space.subSpaceLayer
			fb.bind
//			screen.gl.clearColor(Rgba.None)
//			screen.gl.clearColor(Rgba.Green)
//			screen.gl.clear(screen.gl.COLOR_BUFFER_BIT)
		} 
	}

	/** Restore the state of the rendering and space after a call to `pushLayer()`. */
	def popLayer() {
		if(self.hasLayer) {
			val space  = screen.space
			val pixels = screen.pixelSpace
			
			pixels.popViewport
			pixels.popProjection
			pixels.pop

			space.popViewport
			space.popProjection
			space.pop

			self.layer.fb.unbind
			screen.gl.viewport(0, 0, space.viewport(0).toInt, space.viewport(1).toInt)
		}
	}

	/** Free the optionnal layer frame buffer if one was used and is no more in use.
	  * See [[Avatar]] `hasLayer`. */
	def disposeLayer() {
		if(self.layer ne null) {
			self.layer.fb.dispose
			self.layer = null
		}
	}
}