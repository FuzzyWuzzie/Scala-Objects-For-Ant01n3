package org.sofa.gfx.backend

import org.sofa.math.{Vector3, Point2, Point3}
import org.sofa.backend.SOFALog
import org.sofa.gfx._
import org.sofa.gfx.surface.{Surface, SurfaceRenderer}
import org.sofa.gfx.surface.event._

import android.os.Debug
import android.opengl.GLSurfaceView
import android.content.Context
import android.app.Activity
import android.view.{GestureDetector, ScaleGestureDetector, MotionEvent=>AndroidMotionEvent}
import android.support.v4.view.GestureDetectorCompat
import android.util.{DisplayMetrics, TypedValue}

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
  * By default the surface is created in 32bits RGBA with a 16bits depth buffer.
  *
  * Implementation detail: the surface is used by two threads:
  *   - The rendering thread,
  *   - The activity UI thread for events.
  *
  * Some fields are managed by the UI thread, others by the rendering 
  * thread, as indicated in comments. */
class SurfaceAndroidES20(
		context:Context,
		attrs:android.util.AttributeSet,
    	/** Requested frames per second. */
		protected[this] var expectedFps:Int
	) extends
		GLSurfaceViewExt(context, attrs)	// See this in the Java part.
		with Surface
		with GLSurfaceViewExt.Renderer
		with SOFALog
		with GestureDetector.OnGestureListener
        with GestureDetector.OnDoubleTapListener
        with ScaleGestureDetector.OnScaleGestureListener {

	/** Gl Context. */
    protected var sgl:SGL = null

    /** Width of the surface. */
    protected var w:Int = 0

    /** Height of the surface. */
    protected var h:Int = 0

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

    /** The gesture detector. */
    protected var gestures:GestureDetectorCompat = null
    
    /** The scale gesture detector. */
    protected var scaleGestures:ScaleGestureDetector = null

    /** Send the base motion events ? */
    protected var motionEvents = true

    /** Send high-level events ? */
    protected var hlEvents = true

    /** Send motion events even when no button is pressed (only mouse or pen, touch requires... a touch). */
    protected var noButtonMotionEvents = false

// -- Build --------------------

	def this(context:Context, attrs:android.util.AttributeSet) {
		this(context, attrs, 30)
	}

    def build(renderer:SurfaceRenderer, activity:Activity) {
    	build(renderer, activity, 30)
    }

    def build(renderer:SurfaceRenderer, activity:Activity, expectedFps:Int) {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8,8,8,8,16,0)

        this.renderer    = renderer
        this.activity    = activity
        this.expectedFps = expectedFps
    
        if(created) {
            if(renderer.initSurface ne null) renderer.initSurface(gl, this)
        }
    
        setRenderer(this)

        gestures      = new GestureDetectorCompat(activity, this)
        scaleGestures = new ScaleGestureDetector(activity, this)

//debug(">> GLSurface built waiting creation....")
    }
    
    def gl:SGL = { if(sgl eq null) sgl = new SGLAndroidES20("#version 100"); sgl }
    def width:Int = w
    def height:Int = h
    def fps:Int = expectedFps
    def multiSampling:Int = 1
    def dpc:Double = {
    	// Some android devices may change screen, for example phones that dock to tablets.

    	if(dpcValue <= 0)
    		dpcValue = computeDpc

    	dpcAccess += 1

    	if(dpcAccess > 120) {
    		dpcValue = computeDpc
    		dpcAccess = 0
    	}

    	dpcValue
    }

    def mmToFontSize(value:Int, ratio:Double = 1.0):Int = {
    	(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, value, context.getResources.getDisplayMetrics) * ratio).toInt
    }

    protected[this] var dpcValue = 0.0
    protected[this] var dpcAccess = 0

    def swapBuffers():Unit = {}	// Implicit with Android.
    
    def fps(newFps:Int) { expectedFps = newFps }

    def sendMotionEvents(on:Boolean) { motionEvents = on }

    def sendHighLevelEvents(on:Boolean) { hlEvents = on }

    def animation(on:Boolean) {
    	if(on)
		     setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
    	else setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
    }

    override def onPause() {
        super.onPause
       	animation(false)	// Not really needed, the rendering thread restarts.
        if(created && (renderer ne null) && (renderer.pause ne null)) renderer.pause(this)
println("*************************************************************")
println("*********** STOPING DEBUG SURFACE ***************************")
println("*************************************************************")
Debug.stopMethodTracing()
    }
    
    override def onResume() {
        super.onResume
       	animation(true)		// Not really needed, the rendering thread pauses.
        if(created && (renderer ne null) && (renderer.resume ne null)) renderer.resume(this)
    }
    
    def onSurfaceCreated(unused:GL10, config:EGLConfig) {
//debug(s"** onSurfaceCreated(${Thread.currentThread.getName})")
        created = true
//debug(">> GLSurface created !")
    	if((renderer ne null) && (renderer.initSurface ne null)) renderer.initSurface(gl, this)
    }

//private[this] var steps=0
    
    def onDrawFrame(unused:GL10):Boolean = {
//debug(s"** onDrawFrame(${Thread.currentThread.getName})")
    	// It seems there is actually no better way to do FPS rendering than what
    	// follows. After all it is not really a problem, since the thread doing
    	// rendering is dedicated to this.
    	//
    	// See: http://stackoverflow.com/questions/4772693/how-to-limit-framerate-when-using-androids-glsurfaceview-rendermode-continuousl

    	endTime = System.currentTimeMillis();
    	
    	val dt = endTime - startTime;

    	if (dt < (1000/expectedFps))
    		Thread.sleep((1000/expectedFps) - dt);

    	startTime = System.currentTimeMillis();

    	// We mesure time between calls to this in order to consider the buffer swap time.
    	// Real rendering occurs here :

    	var swap = false

        if(renderer.frame ne null)
        	swap = renderer.frame(this)

// steps += 1
// if(steps == 240) {
// println("*************************************************************")
// println("*********** STOPING DEBUG SURFACE ***************************")
// println("*************************************************************")
// Debug.stopMethodTracing()
// } else if(steps == 160) {
// println("*************************************************************")
// println("********** STARTING DEBUG SURFACE ***************************")
// println("*************************************************************")
// Debug.startMethodTracing("SOFASurface", 50 * 1024 * 1024)	// 20mb of trace, (8mb ~= 6 secs, 20 ~= 16s)
// } else if(steps > 160 && steps % 10 == 0) println("***************** STEP %d **********************".format(steps))

        swap
    }
    
    def onSurfaceChanged(unused:GL10, width:Int, height:Int) {
//debug(s"** onSurfaceChanged(${Thread.currentThread.getName})")
        w = width
        h = height
        if(created && (renderer ne null) && (renderer.surfaceChanged ne null)) {
            renderer.surfaceChanged(this)
        }
    }

    def resize(newWidth:Int, newHeight:Int) {
    	// Can we ??
    	// XXX 
    }

    /** Process something inside the surface. */
    def queueEvent(code: =>Unit) { queueEvent(new Runnable { def run() { code } } ) }

    /** Send an event to the renderer as soon as possible inside the rendering thread. */
    def queueEvent(event:Event) {
    	val self = this
    	queueEvent(new Runnable {
    		def run() {
    			event match {
    				case e:UnicodeEvent   => { if(renderer.unicode   ne null) renderer.unicode(  self, e) }
    				case e:ActionKeyEvent => { if(renderer.actionKey ne null) renderer.actionKey(self, e) }
    				case e:MotionEvent    => { if(renderer.motion    ne null) renderer.motion(   self, e) else debug("no motion event listener???") }
    				case e:GestureEvent   => { if(renderer.gesture   ne null) renderer.gesture(  self, e) }
    				case e:ShortCutEvent  => { if(renderer.shortCut  ne null) renderer.shortCut( self, e) }
    				case _ => throw new RuntimeException("unexpected event type")
    			}
    		}
    	})
    }

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

	/** Used to track the scaling gesture and avoid sending scroll events during this gesture. */
	protected[this] var scaling = false

    override def onTouchEvent(e:AndroidMotionEvent):Boolean = {
    	import AndroidMotionEvent._

    	// Base events

    	val action = e.getActionMasked

    	if(action == ACTION_HOVER_MOVE  ||
    	   action == ACTION_HOVER_ENTER ||
    	   action == ACTION_HOVER_EXIT) {
    		if(noButtonMotionEvents) queueEvent(MotionEventAndroid(e))
    	} else {
    		if(motionEvents) queueEvent(MotionEventAndroid(e))
    	}

    	// Process Hi-Level events

		if(hlEvents) {		
			scaleGestures.onTouchEvent(e)
			gestures.onTouchEvent(e)
		}
		
		true//super.onTouchEvent(e)
    }

	def onDown(event:AndroidMotionEvent):Boolean = { 
		queueEvent(MotionEventAndroid(event))
        true
    }

    def onFling(event1:AndroidMotionEvent, event2:AndroidMotionEvent, velocityX:Float, velocityY:Float):Boolean = {
		queueEvent(FlingEventAndroid(event2, Vector3(px2cm(velocityX), px2cm(velocityY), 0)))
        true
    }

    def onLongPress(event:AndroidMotionEvent) {
    	queueEvent(LongPressEventAndroid(event))
    }

    def onScroll(event1:AndroidMotionEvent, event2:AndroidMotionEvent, distanceX:Float, distanceY:Float):Boolean = {
    	if(! scaling) {
    		queueEvent(ScrollEventAndroid(event2, Vector3(px2cm(-distanceX), px2cm(-distanceY), 0)))
	        true
    	} else {
    		false
    	}
    }

    def onShowPress(event:AndroidMotionEvent) {
    	//queueEvent(ShowPressEventAndroid)
    }

    def onSingleTapUp(event:AndroidMotionEvent):Boolean = {
//    	queueEvent(TapEventAndroid(event, viewOff))
    	queueEvent(TapEventAndroid(event))
        true
    }

    def onDoubleTap(event:AndroidMotionEvent):Boolean = {
    	queueEvent(DoubleTapEventAndroid(event))
        true
    }

    def onDoubleTapEvent(event:AndroidMotionEvent):Boolean = {
    	//debug("@@@ onDoubleTapEvent")
        true
    }

    def onSingleTapConfirmed(event:AndroidMotionEvent):Boolean = {
    	queueEvent(SingleTapEventAndroid(event))
        true
    }

//    protected def viewOff():Point2 = { printf("### VIEW OFF (%f, %f) ###%n", getX, getY); Point2(getX, getY)}

// -- Gestures scale events -------------------------------

	def onScale(detector:ScaleGestureDetector):Boolean =  {
		queueEvent(ScaleEventAndroid(detector.getPreviousSpan-detector.getCurrentSpan, 0))
		true
	}

	def onScaleBegin(detector:ScaleGestureDetector):Boolean = {
		scaling = true
		//queueEvent(ScaleEventAndroid(delta, -1))
		true
	}

	def onScaleEnd(detector:ScaleGestureDetector) {
		scaling = false
		//queueEvent(ScaleEventAndroid(delta, 1))
	}

// -- Utility ----------------------------------------------

    /** Knowning the DPC of the surface, convertex pixel distances to centimeters. */
    def px2cm(px:Double):Double = px / dpc 

	/** Compute the dots-per-centimeter of the current device screen. */
	protected def computeDpc():Double = {
		context.getResources.getDisplayMetrics.densityDpi / 2.54
	}
}