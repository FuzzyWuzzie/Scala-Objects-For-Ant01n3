package org.sofa.gfx.surface

import org.sofa.gfx.SGL

import org.sofa.gfx.surface.event._


/** Abstraction above various rendering surface for different OpenGL back-ends.
  * 
  * The surface works by registering a [[SurfaceRenderer]] that is called for
  * each important event, like rendering a frame, an interaction via the
  * keyboard, or the various devices like stylus, mouses and fingers. The
  * renderer is given at construction.
  *
  * The `width`, `height` properties and `resize` and `fullscreen` operations
  * are not necessarily obeyed, depending on the operating system and
  * configuration.
  *
  * The surface system does not necessarily need to use [[SGL]] for drawing.
  * It only provides an abstraction above OpenGL views (for example NEWT
  * (Jogl) and the GLSurfaceView (Android)). However for ease of use when
  * porting to various environments, [[SGL]] is recommanded.
  */
abstract trait Surface {
    /** SGL implementation. */
    def gl:SGL
    
    //def swapBuffers():Unit
    
    /** Actual surface width in pixels. */
    def width:Int
    
    /** Actual surface height in pixels. */
    def height:Int

    /** The actual surface frame rate. */
    def fps:Int

    /** Dots per centimeter of the current rendering device. */
    def dpc:Double

    /** Convert a value in milimeters to a font size suitable for the actual screen
      * and system. Millimeters are more practical than centimeters for fonts. The
      * returned value has no unit, since distinct systems use distinct units to
      * scale fonts (Java uses points, but always for an hypothetic screen at 72 dpi,
      * android uses pixels, etc.) */
    def mmToFontSize(value:Int):Int

    /** Try to change the surface width and height. This may not be always possible. */
    def resize(newWidth:Int, newHeight:Int)

    /** Try to switch the fullscreen mode. This may not be always possible. */
    def fullscreen(on:Boolean)

	/** When interested only by high-level events and keyboard, motion events can generate
	  * quite a lot of event traffic. Deactivate them for better efficiency. On by default. */
    def sendMotionEvents(on:Boolean)

	/** Sent by default, sometimes you only need base events, no high-level gestures and
	  * keyboard shortcuts. Deactivate them for better efficiency. On by default. */
    def sendHighLevelEvents(on:Boolean)

	/** Try to have the surface run its rendering loop at this frame-rate. The frame-rate
	  * is indicative. Only values > 0 are accepted, use `animation(false)` to
	  * disable animation, the surface will then be refreshed only when needed. */
    def fps(newFps:Int)

    /** Pause or restart animation. By default animation
      * is started when the surface is built. Animation calls `frame` callback
      * of the surface at a given FPS. If disabled, the callback is called
      * only when the surface contents may have been erased.
      * @param on if false animation is paused. */
    def animation(on:Boolean)

    /** Invoke some code to be executed while the OpenGL context is current, 
      * inside the thread used by the surface to do rendering.
      * The given code must return true if the code does not changed the
      * framebuffer. */
    def invoke(code:(Surface)=>Boolean)

    /** Like `invoke(code:(Surface=>Boolean))` but with a `Runnable`. */
    def invoke(runnable:Runnable)

    /** Stop the animation, remove the surface, and free resources. The
      * surface cannot be reused after this. */
    def destroy()
}


/** Allow to configure various aspects of [[Surface]]s creation and usage. [[Surface]]s 
  * may use descendants of this class for more options. */
class SurfaceConfiguration {
	var sendMotionEvents:Boolean = true

	var sendHighLevelEvents:Boolean = true

	var fps:Int = 60
}


/** Rendering and event managing class associated to a rendering surface.
  * 
  * In the model used by SGL, each rendering surface has a renderer associated to it.
  * The renderer is called (regularly or on demand) to render individual frames, or
  * when an event occurs on the rendering surface.
  *
  * The event system, instead of being an interface, allows to assign a callback
  * function to several variables. */
trait SurfaceRenderer {
	
// Rendering

	/** Called as soon as the surface is usable for rendering. No other renderer method excepted 
	  * `close` are called before. */
	var initSurface:(SGL,Surface)=>Unit = null

	/** Called at each frame (either periodically or when the [[Surface]] needs a redraw).
	  * Return true if you wan the surface to swap the front and back buffers (if double
	  * buffering is enabled).  */
	var frame:(Surface)=>Boolean = null
	
	/** Code to call at each surface reconfiguration. */
	var surfaceChanged:(Surface)=>Unit = null
	
	/** Code to call when closing the surface. */
	var close:(Surface)=>Unit = null
	
// Control of the view

	/** Code to call when the application pauses. */
	var pause:(Surface)=>Unit = null
	
	/** Code to call when the application resumes. */
	var resume:(Surface)=>Unit = null

// Base events
	
	/** Code to call when a key was typed and the rendering surface was active. */
	var unicode:(Surface, UnicodeEvent)=>Unit = null
	
	/** Code to call when the surface has been clicked (ktouch-click on touchable devices, left-click on desktops). */
	var actionKey:(Surface, ActionKeyEvent)=>Unit = null	
	
	/** Code to call when the user moved one or more pointers (the mouse on desktops, touch motions of touchable devices). */
	var motion:(Surface, MotionEvent)=>Unit = null

// High-Level events

	/** Code to call when detecting a gesture. See [[GestureEvent]] and [[HighLevelEvent]]. */
	var gesture:(Surface, GestureEvent)=>Unit = null

	/** Code to call for shortcuts. See [[ShortCutEvent]] and [[HighLevelEvent]]. */
	var shortCut:(Surface, ShortCutEvent)=>Unit = null
}