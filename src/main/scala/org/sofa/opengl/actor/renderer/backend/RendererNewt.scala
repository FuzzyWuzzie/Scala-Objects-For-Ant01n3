package org.sofa.opengl.actor.renderer.backend

import javax.media.opengl._
import javax.media.opengl.glu._

import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.math.Point3
import org.sofa.opengl.actor.renderer.{Renderer, AvatarFactory}
import org.sofa.opengl.actor.renderer.{AvatarEvent, AvatarSpatialEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarMotionEvent, AvatarKeyEvent, AvatarZoomEvent}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, MotionEvent, KeyEvent, ScrollEvent}

import akka.actor.{ActorRef}


/** A renderer class for the Jogl NEWT system. It creates an OpenGL ES 2.0 context,
  * with hardware acceleration and double buffering. */
class RendererNewt(factory:AvatarFactory=null) extends Renderer(factory) {

	protected def newSurface(renderer:SurfaceRenderer, width:Int, height:Int,
		title:String, fps:Int, decorated:Boolean, fullscreen:Boolean, overSample:Int):Surface = {	    
		
		// println("GL2    %s".format(GLProfile.isAvailable(GLProfile.GL2)))
		// println("GL2ES2 %s".format(GLProfile.isAvailable(GLProfile.GL2ES2)))
		// println("GLES2  %s".format(GLProfile.isAvailable(GLProfile.GLES2)))
		// println("GL3bc  %s".format(GLProfile.isAvailable(GLProfile.GL3bc)))
		// println("GL4bc  %s".format(GLProfile.isAvailable(GLProfile.GL4bc)))
		// val caps = new GLCapabilities(GLProfile.get(GLProfile.GL3bc))

	    val caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2))

		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(overSample > 1)
		caps.setNumSamples(overSample)

	    new org.sofa.opengl.backend.SurfaceNewt(this,
	    		width, height, title, caps,
	    		org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2,
	    		fps, decorated, fullscreen)
	}

	def onScroll(surface:Surface, e:ScrollEvent) {
		if(screen ne null) {
			screen.propagateEvent(NewtAvatarZoomEvent(e))
		}
	}

	def onKey(surface:Surface, e:KeyEvent) {
		if(screen ne null) {
			screen.propagateEvent(NewtAvatarKeyEvent(e))
		}
	}

	protected var prevMotionEvent:MotionEvent = null

	protected var prevMotionEventTime:Long = 0L

	def onMotion(surface:Surface, e:MotionEvent) {
		if(screen ne null) {
			// We used isStart/isEnd to track motion
			// click and long clicks.

			if(prevMotionEvent eq null) {
				assert(e.isStart)
				prevMotionEvent = e
				prevMotionEventTime = System.currentTimeMillis
			} else {
				if(e.isEnd) {
					if(prevMotionEvent.isStart) {
						val deltaTime = System.currentTimeMillis - prevMotionEventTime
						if(deltaTime >= 1000) {
							// Long click.
							//println("@@ long click")
							screen.propagateEvent(NewtAvatarLongClickEvent(e))
						} else {
							// Click
							//println("@@ click")
							screen.propagateEvent(NewtAvatarClickEvent(e))
						}
					} else {
						// Send end motion
						screen.propagateEvent(NewtAvatarMotionEvent(e))
						//println("@@ motion end")

					}
					prevMotionEvent = null
				} else {
					prevMotionEvent = e
					// Send motion
					//println("@@ motion")
					screen.propagateEvent(NewtAvatarMotionEvent(e))
				}
			}
		}
	}
}


object NewtAvatarClickEvent { def apply(source:MotionEvent) = new NewtAvatarClickEvent(source) }
class NewtAvatarClickEvent(val source:MotionEvent) extends AvatarClickEvent {
	def position:Point3 = Point3(source.x, source.y, 0)
}


object NewtAvatarLongClickEvent { def apply(source:MotionEvent) = new NewtAvatarLongClickEvent(source) }
class NewtAvatarLongClickEvent(val source:MotionEvent) extends AvatarLongClickEvent {
	def position:Point3 = Point3(source.x, source.y, 0)
}


object NewtAvatarMotionEvent { def apply(source:MotionEvent) = new NewtAvatarMotionEvent(source) }
class NewtAvatarMotionEvent(val source:MotionEvent) extends AvatarMotionEvent {
	def position:Point3 = Point3(source.x, source.y, 0)

	def pointerCount:Int = source.pointerCount

	def position(i:Int):Point3 = Point3(source.x(i), source.y(i), 0)

	def isStart:Boolean = source.isStart

	def isEnd:Boolean = source.isEnd

	def pressure:Double = source.pressure

	def pressure(i:Int):Double = source.pressure(i)

}

object NewtAvatarZoomEvent { def apply(source:ScrollEvent) = new NewtAvatarZoomEvent(source) }
class NewtAvatarZoomEvent(val source:ScrollEvent) extends AvatarZoomEvent {
	// TODO probably add a factor...
	def amount:Double = source.amount
}


object NewtAvatarKeyEvent { def apply(source:KeyEvent) = new NewtAvatarKeyEvent(source) }
class NewtAvatarKeyEvent(val source:KeyEvent) extends AvatarKeyEvent {
	def actionChar:org.sofa.opengl.surface.ActionChar.Value = source.actionChar
	def unicodeChar:Char = source.unicodeChar
	def isControlDown:Boolean = source.isControlDown
    def isAltDown:Boolean = source.isAltDown
    def isAltGrDown:Boolean = source.isAltGrDown
    def isShiftDown:Boolean = source.isShiftDown
    def isMetaDown:Boolean = source.isMetaDown
}