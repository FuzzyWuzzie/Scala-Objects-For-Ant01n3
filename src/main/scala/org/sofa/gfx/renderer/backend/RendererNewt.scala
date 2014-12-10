package org.sofa.gfx.renderer.backend

import javax.media.opengl._
import javax.media.opengl.glu._

import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.math.Point3
import org.sofa.gfx.renderer.{Renderer, RendererFactory, AvatarFactory}
import org.sofa.gfx.surface.event._
import org.sofa.gfx.surface.{Surface, SurfaceRenderer}


/** Factory for the Newt based renderer. */
class RendererFactoryNewt extends RendererFactory {
	def newRenderer(avatarFactory:AvatarFactory):Renderer = new RendererNewt(avatarFactory)
}


/** A renderer class for the Jogl NEWT system. It creates an OpenGL ES 2.0 context,
  * with hardware acceleration and double buffering. */
class RendererNewt(factory:AvatarFactory=null) extends Renderer(factory) {

	protected def newSurface(renderer:SurfaceRenderer, width:Int, height:Int,
		title:String, fps:Int, decorated:Boolean, fullscreen:Boolean, multiSample:Int):Surface = {	    
		
		// println("GL2    %s".format(GLProfile.isAvailable(GLProfile.GL2)))
		// println("GL2ES2 %s".format(GLProfile.isAvailable(GLProfile.GL2ES2)))
		// println("GLES2  %s".format(GLProfile.isAvailable(GLProfile.GLES2)))
		// println("GL3bc  %s".format(GLProfile.isAvailable(GLProfile.GL3bc)))
		// println("GL4bc  %s".format(GLProfile.isAvailable(GLProfile.GL4bc)))
		// val caps = new GLCapabilities(GLProfile.get(GLProfile.GL3bc))

	    val caps = new GLCapabilities(GLProfile.get(GLProfile.GL3))

		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(multiSample > 1)
		caps.setNumSamples(multiSample)

	    new org.sofa.gfx.backend.SurfaceNewt(this,
	    		width, height, title, caps,
	    		org.sofa.gfx.backend.SurfaceNewtGLBackend.GL2ES2,
	    		fps, decorated, fullscreen, multiSample)
	}
}