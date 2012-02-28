package org.sofa.opengl.surface

import org.sofa.opengl.SGL

/** Abstraction above various rendering surface for different OpenGL back-ends.
  * 
  * The surface system is not mandatory to use SGL. It only provides an abstraction
  * above NEWT (Jogl) and the GLSurfaceView (Android).
  * 
  * The surface works by registering a surface renderer that is called for each
  * important event, like rendering a frame, and interaction via the keyboard,
  * or the various devices like stylus, mouses and fingers.
  * 
  * The surface interface is quite limited, and therefore will only fulfill
  * basic needs. For more advanced uses, using the native interfaces of the
  * system is recommended. */
abstract trait Surface {
    /** SGL implementation. */
    def gl:SGL
    
    def swapBuffers():Unit
    
    /** Surface width in pixels. */
    def width:Int
    
    /** Surface height in pixels. */
    def height:Int
}

/** Rendering and event managing class associated to a rendering surface.
  * 
  * In the model used by SGL, each rendering surface has a renderer associated to it.
  * The renderer is called (regularly or on demand) to render individual frames, or
  * when an event occurs on the rendering surface. */
trait SurfaceRenderer {
    /** Code to call to render a frame. */
	type Frame = (Surface)=>Unit
	/** Code to call to initialize a rendering surface. */
	type InitSurface  = (SGL, Surface)=>Unit
	/** Code to call at each surface reconfiguration. The two arguments are the width and height in pixels. */
	type SurfaceChanged = (Surface)=>Unit
	/** Code to call when the application pauses. */
	type Pause = (Surface)=>Unit
	/** Code to call when the application resumes. */
	type Resume = (Surface)=>Unit
	/** Code to call when a key was type and the rendering surface was active. */
	type Key = (Surface, KeyEvent)=>Unit
	/** Code to call when the surface has been clicked (touch-click on touchable devices, left-click on desktops). */
	type Action = (Surface, ActionEvent)=>Unit
	/** Code to call when the user requested configuration (long-click on touchable devices, right-click on desktops). */
	type Configure = (Surface, ConfigureEvent)=>Unit
	/** Code to call when the user moved one or more pointers (the mouse on desktops, touch motions of touchable devices). */
	type Motion = (Surface, MotionEvent)=>Unit
	/** Code to call when a scroll wheel is used. */
	type Scroll = (Surface, ScrollEvent)=>Unit
	/** Code to call when closing the surface. */
	type Close = (Surface)=>Unit
	
	var frame:Frame = null
	var initSurface:InitSurface = null
	var surfaceChanged:SurfaceChanged = null
	var key:Key = null
	var action:Action = null
	var configure:Configure = null
	var motion:Motion = null
	var scroll:Scroll = null
	var pause:Pause = null
	var resume:Resume = null
	var close:Close = null
}

/** Generic event. */
trait Event {}

/** Key. */
abstract class KeyEvent extends Event {
    /** True if the key-code is a printable character. */
    def isPrintable:Boolean
    
    /** If the event reports a printable character, this is the Unicode value of this character. */
    def unicodeChar:Char

    /** If the  */
    def actionChar:ActionChar.Value
    
    def isControlDown:Boolean
    
    def isAltDown:Boolean
    
    def isAltGrDown:Boolean
    
    def isShiftDown:Boolean
    
    def isMetaDown:Boolean
    
    object ActionChar extends Enumeration {
    	val Escape = Value
    			val PageUp = Value
    			val PageDown = Value
    			val Up = Value
    			val Down = Value
    			val Right = Value
    			val Left = Value
    			val Unknown = Value
    }
}

/** Action. */
class ActionEvent extends Event {}

/** The configure action. */
class ConfigureEvent extends Event {}

/** Event for motion of various devices (mouse, fingers, stylus). */
abstract class MotionEvent extends Event {
    def deviceType:DeviceType.Value
    
    def isStart:Boolean
    
    def isEnd:Boolean
    
    def x:Double
    
    def x(pointer:Int):Double
    
    def y:Double
    
    def y(pointer:Int):Double
    
    def pressure:Double
    
    def pressure(pointer:Int):Double
    
    def pointerCount:Int
    
    object DeviceType extends Enumeration {
        val Stylus = Value
        val Eraser = Value
        val Mouse = Value
        val Finger = Value
        val Unknown = Value
    }
    
    def sourceEvent:AnyRef
}

/** Event for all sorts of scrolling. */
abstract class ScrollEvent extends Event {
    /** Amount of scrolling, positive if the "real" movement goes down, right or clock-wise. */
    def amount:Int
}