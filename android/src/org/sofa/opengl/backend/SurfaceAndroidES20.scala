package org.sofa.opengl.backend

import org.sofa.backend.SOFALog
import org.sofa.opengl._
import org.sofa.opengl.surface.{Surface, SurfaceRenderer}

import android.opengl.GLSurfaceView
import android.content.Context
import android.app.Activity
import android.view.{GestureDetector, ScaleGestureDetector, MotionEvent}
import android.support.v4.view.GestureDetectorCompat

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/** A surface that is suitable for an Android application.
  *
  * The constructor of this class must be (Context,AttributeSet), this is
  * required to instantiate it automatically. Therefore, you must call
  * explicitely the `build(SurfaceRenderer,Activity)` method to finish the
  * construction phase. 
  *
  * Be careful that the surface renderer will run in a dedicated thread
  * for this surface.
  *
  * By default the surface is created in 32bits RGBA with a 16bits depth buffer. */
class SurfaceAndroidES20(
		context:Context,
		attrs:android.util.AttributeSet,
    	/** Requested frames per second. */
		protected[this] var fps:Int = 30
	) extends
		GLSurfaceView(context, attrs)
		with Surface
		with GLSurfaceView.Renderer
		with SOFALog
		with GestureDetector.OnGestureListener
        with GestureDetector.OnDoubleTapListener
        with ScaleGestureDetector.OnScaleGestureListener {

	/** Gl Context. */
    protected[this] var sgl:SGL = null

    /** Width of the surface. */
    protected[this] var w:Int = 0

    /** Height of the surface. */
    protected[this] var h:Int = 0

    /** Used to compute fps. */
    protected[this] var endTime:Long = 0

    /** Used to compute fps. */
    protected[this] var startTime:Long = 0

    /** True once the surface is created. */
    protected[this] var created = false

    /** The renderer. */
    protected[this] var renderer:SurfaceRenderer = null

    /** The activity owning this surface (to call code on the UI thread). */
    protected[this] var activity:Activity = null

    /** The gesture detector. */
    protected[this] var gestures:GestureDetectorCompat = null
    
    /** The scale gesture detector. */
    protected[this] var scaleGestures:ScaleGestureDetector = null

// -- Build --------------------

    def build(renderer:SurfaceRenderer, activity:Activity) {
    	build(renderer, activity, 30)
    }

    def build(renderer:SurfaceRenderer, activity:Activity, fps:Int) {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8,8,8,8,16,0)

        this.renderer = renderer
        this.activity = activity
        this.fps      = fps
    
        if(created) {
            if(renderer.initSurface ne null) renderer.initSurface(gl, this)
        }
    
        setRenderer(this)

        gestures      = new GestureDetectorCompat(activity, this)
        scaleGestures = new ScaleGestureDetector(activity, this)

        debug(">> GLSurface built waiting creation....")
    }
    
    def gl:SGL = { if(sgl eq null) sgl = new SGLAndroidES20("#version 110"); sgl }
    def width:Int = w
    def height:Int = h
    def swapBuffers():Unit = {}	// Implicit with Android.
    
    def animation(on:Boolean) {
    	if(on)
		     setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
    	else setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
    }

    override def onPause() {
        super.onPause
       	animation(false)	// Not really needed, the rendering thread restarts.
        if(created && (renderer ne null) && (renderer.pause ne null)) renderer.pause(this)
    }
    
    override def onResume() {
        super.onResume
       	animation(true)		// Not really needed, the rendering thread pauses.
        if(created && (renderer ne null) && (renderer.resume ne null)) renderer.resume(this)
    }
    
    def onSurfaceCreated(unused:GL10, config:EGLConfig) {
//debug(s"** onSurfaceCreated(${Thread.currentThread.getName})")
        created = true
      	debug(">> GLSurface created !")
    	if((renderer ne null) && (renderer.initSurface ne null)) renderer.initSurface(gl, this)
    }
    
    def onDrawFrame(unused:GL10) {
//debug(s"** onDrawFrame(${Thread.currentThread.getName})")
    	// It seems there is actually no better way to do FPS rendering than what
    	// follows. After all it is not really a problem, since the thread doing
    	// rendering is dedicated to this.
    	//
    	// See: http://stackoverflow.com/questions/4772693/how-to-limit-framerate-when-using-androids-glsurfaceview-rendermode-continuousl

    	endTime = System.currentTimeMillis();
    	
    	val dt = endTime - startTime;

    	if (dt < (1000/fps))
    		Thread.sleep((1000/fps) - dt);

    	startTime = System.currentTimeMillis();

    	// We mesure time between calls to this in order to consider the buffer swap time.
    	// Real rendering occurs here :

        if(renderer.frame ne null) renderer.frame(this)
    }
    
    def onSurfaceChanged(unused:GL10, width:Int, height:Int) {
debug(s"** onSurfaceChanged(${Thread.currentThread.getName})")
        w = width
        h = height
        if(created && (renderer ne null) && (renderer.surfaceChanged ne null)) {
            renderer.surfaceChanged(this)
        }
    }

    override def onTouchEvent(e:MotionEvent):Boolean = {
		gestures.onTouchEvent(e)
		scaleGestures.onTouchEvent(e)
		true//super.onTouchEvent(e)
    }

    def resize(newWidth:Int, newHeight:Int) {
    	// Can we ??
    	// XXX 
    }

    /** Process something inside the surface. */
    def queueEvent(code: =>Unit) { queueEvent(new Runnable { def run() { code } } ) }

    def invoke(code:(Surface)=>Boolean) {
    	val me = this
    	//activity.runOnUiThread(new Runnable() { override def run() { code(me) } } ) 
    	queueEvent(new Runnable { def run() { code(me) } })
    }

    def invoke(runnable:Runnable) { 
    	//activity.runOnUiThread(runnable)
    	queueEvent(runnable)
    }

    def destroy() {
    	// TODO ? HOW ?
    }

	def fullscreen(on: Boolean) { /* TODO ? HOW ? */ }

// -- Gesture events --------------------------------

	def onDown(event:MotionEvent):Boolean = { 
		debug("@@@ onDown")
        true
    }

    def onFling(event1:MotionEvent, event2:MotionEvent, velocityX:Float, velocityY:Float):Boolean = {
		debug("@@@ onFling")
        true
    }

    def onLongPress(event:MotionEvent) {
    	debug("@@@ onLongPress")
    }

    def onScroll(event1:MotionEvent, event2:MotionEvent, distanceX:Float, distanceY:Float):Boolean = {
    	debug("@@@ onScroll")
        true
    }

    def onShowPress(event:MotionEvent) {
    	debug("@@@ onShowPress")
    }

    def onSingleTapUp(event:MotionEvent):Boolean = {
    	debug("@@@ onSingleTapUp")
        true
    }

    def onDoubleTap(event:MotionEvent):Boolean = {
    	debug("@@@ onDoubleTap")
        true
    }

    def onDoubleTapEvent(event:MotionEvent):Boolean = {
    	debug("@@@ onDoubleTapEvent")
        true
    }

    def onSingleTapConfirmed(event:MotionEvent):Boolean = {
    	debug("@@@ onSingleTapConfirmed")
        true
    }

// -- Gestures scale events -------------------------------

	def onScale(detector:ScaleGestureDetector):Boolean =  {
		debug("@@@ onScale")
		true
	}

	def onScaleBegin(detector:ScaleGestureDetector):Boolean = {
		debug("@@@ onScaleBegin")
		true
	}

	def onScaleEnd(detector:ScaleGestureDetector) {
		debug("@@@ onScaleEnd")
	}
}





class ScrollEventAndroid(val source:android.view.MotionEvent) extends org.sofa.opengl.surface.ScrollEvent {
    // Not supported in > 10 (> android 2.3)
    def amount:Double = 0//source.getAxisValue(android.view.MotionEvent.AXIS_VSCROLL).toInt
}

class MotionEventAndroid(val source:android.view.MotionEvent) extends org.sofa.opengl.surface.MotionEvent {
/* On android 4 only
    def deviceType:DeviceType.Value = { source.getToolType(0) match {
        	case android.view.MotionEvent.TOOL_TYPE_ERASER => DeviceType.Eraser
        	case android.view.MotionEvent.TOOL_TYPE_FINGER => DeviceType.Finger
        	case android.view.MotionEvent.TOOL_TYPE_MOUSE  => DeviceType.Mouse
        	case android.view.MotionEvent.TOOL_TYPE_STYLUS => DeviceType.Stylus
        	case _                                         => DeviceType.Unknown
    	}
    }
*/
    def deviceType:DeviceType.Value = {
        val sources = source.getDevice.getSources
        	
        if((sources & android.view.InputDevice.SOURCE_TOUCHSCREEN) != 0) {
            DeviceType.Finger
        } else if((sources & android.view.InputDevice.SOURCE_MOUSE) != 0) {
            DeviceType.Mouse
        } else {
        	DeviceType.Unknown
        }
   	}
    
    def isStart:Boolean = {
        (  source.getActionMasked == android.view.MotionEvent.ACTION_DOWN 
        || source.getActionMasked == android.view.MotionEvent.ACTION_POINTER_DOWN )
    }
    
    def isEnd:Boolean = {
        (  source.getActionMasked == android.view.MotionEvent.ACTION_UP
        || source.getActionMasked == android.view.MotionEvent.ACTION_POINTER_UP) }
    
    def x:Double = source.getX
    
    def y:Double = source.getY
    
    def x(pointer:Int):Double = source.getX(pointer)
    
    def y(pointer:Int):Double = source.getY(pointer)
    
    def pressure:Double = source.getPressure
    
    def pressure(pointer:Int):Double = source.getPressure(pointer)
    
    def pointerCount:Int = source.getPointerCount
    
    def sourceEvent:AnyRef = source
}