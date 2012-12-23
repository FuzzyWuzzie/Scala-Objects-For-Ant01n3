package org.sofa.opengl.backend

import org.sofa.opengl._
import org.sofa.opengl.surface.{Surface, SurfaceRenderer}
import android.opengl.GLSurfaceView
import android.content.Context
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** A surface that is suitable for an Android application.
  *
  * The constructor of this class must be (Context,AttributeSet), this is
  * required to instantiate it automatically. Therefore, you must call
  * explicitely the buil(Camera,SurfaceRenderer) method to finish the
  * construction phase. */
class SurfaceAndroidES20(context:Context, attrs:android.util.AttributeSet)
	extends GLSurfaceView(context, attrs) with Surface with GLSurfaceView.Renderer {

    protected var sgl:SGL = null
    protected var w:Int = 0
    protected var h:Int = 0
    protected var fps:Int = 25
    protected var endTime:Long = 0
    protected var startTime:Long = 0
    protected var created = false
    protected var camera:Camera = null
    protected var renderer:SurfaceRenderer = null
    
    //build

    def build(camera:Camera, renderer:SurfaceRenderer) {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8,8,8,8,0,0)
        this.camera = camera
        this.renderer = renderer
    
        if(created) {
            if(renderer.initSurface ne null) renderer.initSurface(gl, this)
        }
    
        setRenderer(this)
    }
    
    def gl:SGL = { if(sgl eq null) sgl = new SGLAndroidES20(); sgl }
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
}

class ScrollEventAndroid(val source:android.view.MotionEvent) extends org.sofa.opengl.surface.ScrollEvent {
    // Not supported in > 10 (> android 2.3)
    def amount:Int = 0//source.getAxisValue(android.view.MotionEvent.AXIS_VSCROLL).toInt
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