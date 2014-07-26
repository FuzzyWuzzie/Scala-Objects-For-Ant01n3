package org.sofa.gfx.actor.renderer.backend

import android.app.Activity
import android.opengl.GLSurfaceView
//import android.content.res.Resources.Theme

import org.sofa.backend.SOFALog
import org.sofa.math.Point3
import org.sofa.gfx.actor.renderer.{Renderer, RendererFactory, AvatarFactory}
import org.sofa.gfx.surface.{Surface, SurfaceRenderer}

import org.sofa.gfx.backend.{SurfaceAndroidES20}

import akka.actor.{ActorRef}


/** Factory for renderers on Android, needs an activity to be instantiated. */
class RendererFactoryAndroidES20(activity:Activity) extends RendererFactory {
	def newRenderer(avatarFactory:AvatarFactory):Renderer = new RendererAndroidES20(activity, avatarFactory)
}


/** A renderer class for Android. It creates an OpenGL ES 2.0 context,
  * with hardware acceleration and double buffering.
  *
  * Be careful, as in other renderers, this one runs in its own thread (in fact the
  * GLSurfaceView thrread), not the activity UI thread.
  *
  * @param activity The android activity this renderer is attached to.
  * @param factory The avatar factory (or factory chain) to populate the renderer. */
class RendererAndroidES20(activity:Activity, factory:AvatarFactory=null) extends Renderer(factory) with SOFALog {

	protected def newSurface(renderer:SurfaceRenderer, width:Int, height:Int,
		title:String, fps:Int, decorated:Boolean, fullscreen:Boolean, overSample:Int):Surface = {	    
		debug(">>>> RendererAndroidES20.newSurface")		
		val surface = new SurfaceAndroidES20(activity, null/*Theme.obtainStyledAttributes()*/, fps)
		surface.build(this, activity)
		surface
	}

	/** The underlying surface, as a view usable to insert it in an activity.
	  * This is the same object as the surface field, but casted as a
	  * GlSurfaceView. */
	def glSurfaceView:GLSurfaceView = surface.asInstanceOf[GLSurfaceView]
}