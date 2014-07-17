package org.sofa.opengl.actor.renderer.backend

import android.app.Activity
import android.opengl.GLSurfaceView
//import android.content.res.Resources.Theme

import org.sofa.math.Point3
import org.sofa.opengl.actor.renderer.{Renderer, AvatarFactory}
import org.sofa.opengl.actor.renderer.{AvatarEvent, AvatarSpatialEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarMotionEvent, AvatarKeyEvent, AvatarZoomEvent}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, MotionEvent, KeyEvent, ScrollEvent}

import org.sofa.opengl.backend.{SurfaceAndroidES20}

import akka.actor.{ActorRef}


/** A renderer class for Android. It creates an OpenGL ES 2.0 context,
  * with hardware acceleration and double buffering.
  *
  * Be careful, as in other renderers, this one runs in its own thread (in fact the
  * GLSurfaceView thrread), not the activity UI thread.
  *
  * @param activity The android activity this renderer is attached to.
  * @param factory The avatar factory (or factory chain) to populate the renderer. */
class RendererAndroidES20(activity:Activity, factory:AvatarFactory=null) extends Renderer(factory) {

	protected def newSurface(renderer:SurfaceRenderer, width:Int, height:Int,
		title:String, fps:Int, decorated:Boolean, fullscreen:Boolean, overSample:Int):Surface = {	    
		
		val surface = new SurfaceAndroidES20(activity, null/*Theme.obtainStyledAttributes()*/)
		surface.build(this, activity)
		surface
	}

	/** The underlying surface, as a view usable to insert it in an activity.
	  * This is the same object as the surface field, but casted as a
	  * GlSurfaceView. */
	def glSurfaceView:GLSurfaceView = surface.asInstanceOf[GLSurfaceView]

	// def onScroll(surface:Surface, e:ScrollEvent) {
	// 	if(screen ne null) {
	// 		// screen.propagateEvent(AndroidAvatarZoomEvent(e))
	// 	}
	// }

	// def onKey(surface:Surface, e:KeyEvent) {
	// 	if(screen ne null) {
	// 		// screen.propagateEvent(AndroidAvatarKeyEvent(e))
	// 	}
	// }

	// // protected var prevMotionEvent:MotionEvent = null

	// // protected var prevMotionEventTime:Long = 0L

	// def onMotion(surface:Surface, e:MotionEvent) {
	// 	if(screen ne null) {
	// // 		// We used isStart/isEnd to track motion
	// // 		// click and long clicks.

	// // 		if(prevMotionEvent eq null) {
	// // 			assert(e.isStart)
	// // 			prevMotionEvent = e
	// // 			prevMotionEventTime = System.currentTimeMillis
	// // 		} else {
	// // 			if(e.isEnd) {
	// // 				if(prevMotionEvent.isStart) {
	// // 					val deltaTime = System.currentTimeMillis - prevMotionEventTime
	// // 					if(deltaTime >= 1000) {
	// // 						// Long click.
	// // 						//println("@@ long click")
	// // 						screen.propagateEvent(AndroidAvatarLongClickEvent(e))
	// // 					} else {
	// // 						// Click
	// // 						//println("@@ click")
	// // 						screen.propagateEvent(AndroidAvatarClickEvent(e))
	// // 					}
	// // 				} else {
	// // 					// Send end motion
	// // 					screen.propagateEvent(AndroidAvatarMotionEvent(e))
	// // 					//println("@@ motion end")

	// // 				}
	// // 				prevMotionEvent = null
	// // 			} else {
	// // 				prevMotionEvent = e
	// // 				// Send motion
	// // 				//println("@@ motion")
	// // 				screen.propagateEvent(AndroidAvatarMotionEvent(e))
	// // 			}
	// // 		}
	// 	}
	// }
}


// object AndroidAvatarClickEvent { def apply(source:MotionEvent) = new AndroidAvatarClickEvent(source) }
// class AndroidAvatarClickEvent(val source:MotionEvent) extends AvatarClickEvent {
// 	def position:Point3 = Point3(source.x, source.y, 0)
// }


// object AndroidAvatarLongClickEvent { def apply(source:MotionEvent) = new AndroidAvatarLongClickEvent(source) }
// class AndroidAvatarLongClickEvent(val source:MotionEvent) extends AvatarLongClickEvent {
// 	def position:Point3 = Point3(source.x, source.y, 0)
// }


// object AndroidAvatarMotionEvent { def apply(source:MotionEvent) = new AndroidAvatarMotionEvent(source) }
// class AndroidAvatarMotionEvent(val source:MotionEvent) extends AvatarMotionEvent {
// 	def position:Point3 = Point3(source.x, source.y, 0)

// 	def pointerCount:Int = source.pointerCount

// 	def position(i:Int):Point3 = Point3(source.x(i), source.y(i), 0)

// 	def isStart:Boolean = source.isStart

// 	def isEnd:Boolean = source.isEnd

// 	def pressure:Double = source.pressure

// 	def pressure(i:Int):Double = source.pressure(i)

// }

// object AndroidAvatarZoomEvent { def apply(source:ScrollEvent) = new AndroidAvatarZoomEvent(source) }
// class AndroidAvatarZoomEvent(val source:ScrollEvent) extends AvatarZoomEvent {
// 	// TODO probably add a factor...
// 	def amount:Double = source.amount
// }


// object AndroidAvatarKeyEvent { def apply(source:KeyEvent) = new AndroidAvatarKeyEvent(source) }
// class AndroidAvatarKeyEvent(val source:KeyEvent) extends AvatarKeyEvent {
// 	def actionChar:org.sofa.opengl.surface.ActionChar.Value = source.actionChar
// 	def unicodeChar:Char = source.unicodeChar
// 	def isControlDown:Boolean = source.isControlDown
//     def isAltDown:Boolean = source.isAltDown
//     def isAltGrDown:Boolean = source.isAltGrDown
//     def isShiftDown:Boolean = source.isShiftDown
//     def isMetaDown:Boolean = source.isMetaDown
// }