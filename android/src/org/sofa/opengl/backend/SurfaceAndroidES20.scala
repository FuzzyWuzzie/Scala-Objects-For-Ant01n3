package org.sofa.opengl.backend

import org.sofa.backend.SOFALog
import org.sofa.opengl._
import org.sofa.opengl.surface.{Surface, SurfaceRenderer}

import android.opengl.GLSurfaceView
import android.content.Context
import android.app.Activity

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
class SurfaceAndroidES20(context:Context, attrs:android.util.AttributeSet)
	extends GLSurfaceView(context, attrs) with Surface with GLSurfaceView.Renderer with SOFALog {

	/** Gl Context. */
    protected var sgl:SGL = null

    /** Width of the surface. */
    protected var w:Int = 0

    /** Height of the surface. */
    protected var h:Int = 0

    /** Requested frames per second. */
    protected var fps:Int = 30

    /** Used to compute fps. */
    protected var endTime:Long = 0

    /** Used to compute fps. */
    protected var startTime:Long = 0

    /** True once the surface is created. */
    protected var created = false

    /** The renderer. */
    protected var renderer:SurfaceRenderer = null

    /** The activity owning this surface (to call code on the UI thread). */
    protected var activity:Activity = null
    
    //build

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
    }
    
    def gl:SGL = { if(sgl eq null) sgl = new SGLAndroidES20("110"); sgl }
    def width:Int = w
    def height:Int = h
    def swapBuffers():Unit = {}	// Implicit with Android.
    
    override def onPause() {
        super.onPause
        if(created && (renderer ne null) && (renderer.pause ne null)) renderer.pause(this)
    }
    
    override def onResume() {
        super.onResume
        if(created && (renderer ne null) && (renderer.resume ne null)) renderer.resume(this)
    }
    
    def onSurfaceCreated(unused:GL10, config:EGLConfig) {
        created = true
    	if((renderer ne null) && (renderer.initSurface ne null)) renderer.initSurface(gl, this)
    }
    
    def onDrawFrame(unused:GL10) {
    	// XXX
    	// TODO find how to trigger timed redrawing in Android
    	// XXX

    	endTime = System.currentTimeMillis();
    	
    	val dt = endTime - startTime;

    	if (dt < (1000/fps))
    		Thread.sleep((1000/fps) - dt);

    	startTime = System.currentTimeMillis();

        if(renderer.frame ne null) renderer.frame(this)
    }
    
    def onSurfaceChanged(unused:GL10, width:Int, height:Int) {
        w = width
        h = height
        if(created && (renderer ne null) && (renderer.surfaceChanged ne null)) {
            renderer.surfaceChanged(this)
        }
    }
    
    override def onTouchEvent(e:android.view.MotionEvent):Boolean = {
    	if(created && (renderer ne null)) {
    	    e.getActionMasked match {
                // SCROLL only available > 10 (> android 2.3)
    	        // case android.view.MotionEvent.ACTION_SCROLL => {
    	        //     if(renderer.scroll ne null) renderer.scroll(this, new ScrollEventAndroid(e))
    	        //     true
    	        // }
    	        case android.view.MotionEvent.ACTION_DOWN => {
    	            if(renderer.motion ne null) renderer.motion(this, new MotionEventAndroid(e))
    	            true
    	        }
    	        case android.view.MotionEvent.ACTION_MOVE => {
    	            if(renderer.motion ne null) renderer.motion(this, new MotionEventAndroid(e))
    	            true
    	        }
    	        case android.view.MotionEvent.ACTION_POINTER_DOWN => {
    	            if(renderer.motion ne null) renderer.motion(this, new MotionEventAndroid(e))
    	            true
    	        }
    	        case android.view.MotionEvent.ACTION_UP => {
    	            if(renderer.motion ne null) renderer.motion(this, new MotionEventAndroid(e))
    	            true
    	        }
    	        case android.view.MotionEvent.ACTION_POINTER_UP => {
    	            if(renderer.motion ne null) renderer.motion(this, new MotionEventAndroid(e))
    	            true
    	        }
    	        case _ => {
    	            false
    	        }
    	    }
    	} else {
    	    false
    	}
    }

    def resize(newWidth:Int, newHeight:Int) {
    	// Can we ??
    	// XXX 
    }

    def invoke(code:(Surface)=>Boolean) {
    	val me = this
    	//activity.runOnUiThread(new Runnable() { override def run() { code(me) } } ) 
    	queueEvent(
    		new Runnable() { override def run() { code(me) } }
    	)
    }

    def invoke(runnable:Runnable) { 
    	//activity.runOnUiThread(runnable)
    	queueEvent(runnable)
    }

    def destroy() {
    	// TODO ? HOW ?
    }

	def fullscreen(on: Boolean) { /* TODO ? HOW ? */ }
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