package org.sofa.gfx

import org.sofa.nio._
import java.io.{File, IOException}
import scala.collection.mutable.{ArrayBuffer=>ScalaArrayBuffer}


object TextureFramebuffer {
	var currentlyBound:TextureFramebuffer = null
}



/** An alternate frame buffer that renders in a texture that can then be used onto onto objects. */
class TextureFramebuffer(gl:SGL, val width:Int, val height:Int, val useDepth:Boolean, params:TexParams) extends OpenGLObject(gl) {
    import gl._

    //protected var colorid:AnyRef = null
    protected[this] var color:Texture = null

    //protected var depthid:AnyRef = null
    protected[this] var depth:Texture = null

    init

    def this(gl:SGL, width:Int, height:Int) { this(gl, width, height, true, TexParams(minFilter=TexMin.Nearest, magFilter=TexMag.Nearest, wrap=TexWrap.Clamp)) }

    def this(gl:SGL, width:Int, height:Int, useDepth:Boolean) { this(gl, width, height, useDepth, TexParams(minFilter=TexMin.Nearest, magFilter=TexMag.Nearest)) }

    protected def init() {
        super.init(createFramebuffer)
        
        // Generate a texture to hold the colour buffer.

        color = new Texture(gl, gl.RGBA, gl.UNSIGNED_BYTE, width, height, params)

        if(useDepth) {
        	// Create a texture to hold the depth buffer.

			depth = new Texture(gl, gl.DEPTH_COMPONENT, gl.UNSIGNED_SHORT, width, height, params)    
        }
    }

    /** Bind a new frame buffer that render in a texture. Set the viewport of the size of the texture. */
    def bind() {
    	if(TextureFramebuffer.currentlyBound ne this) {
	        // Associate the textures with the FBO.

	        bindFramebuffer(gl.FRAMEBUFFER, oid)

	        viewport(0, 0, width, height)

	        framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, color.id, 0)
	        
	        if(useDepth)
	        	framebufferTexture2D(gl.FRAMEBUFFER, gl.DEPTH_ATTACHMENT, gl.TEXTURE_2D, depth.id, 0)

	        // Check FBO status.    

	        val status = checkFramebufferStatus(gl.FRAMEBUFFER)

	        if(status != gl.FRAMEBUFFER_COMPLETE) {
	            status match {
	                case FRAMEBUFFER_INCOMPLETE_ATTACHMENT         => throw new RuntimeException("cannot create frame buffer object, incomplete attachment")
	                case FRAMEBUFFER_INCOMPLETE_DIMENSIONS         => throw new RuntimeException("cannot create frame buffer object, incomplete dimensions")
	                case FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT => throw new RuntimeException("cannot create frame buffer object, incomplete missing attachment")
	                case FRAMEBUFFER_UNSUPPORTED                   => throw new RuntimeException("cannot create frame buffer object, unsupported")
	                case _                                         => throw new RuntimeException("cannot create frame buffer object")
	            }
	        }

	        checkErrors
			TextureFramebuffer.currentlyBound = this    		
			Texture.resetCurrentlyBound
    	}
    }

    def unbind() {
    	TextureFramebuffer.currentlyBound = null
    	bindFramebuffer(gl.FRAMEBUFFER, null)
    }

    /** Bind the color texture (the framebuffer) for the TEXTURE_2D target. */
    def bindColorTexture() {
        //bindTexture(gl.TEXTURE_2D, color.id)
		color.bind
    }

    /** Both bind the color texture and specify the texture unit it is bound to. */
    def bindColorTextureTo(textureUnit:Int) {
    	color.bindTo(textureUnit)
        //activeTexture(textureUnit)
        //bindTexture(gl.TEXTURE_2D, color.id)
    }

    /** Bind the depth texture (the depth buffer) for the TEXTURE_2D target. */
    def bindDepthTexture() {
    	if(useDepth) {
        	//bindTexture(gl.TEXTURE_2D, depth.id)
        	depth.bind
    	}
    }

    /** Both bind the depth texture and specify the texture unit it is bound to. */
    def bindDepthTextureTo(textureUnit:Int) {
        if(useDepth) {
    	    //activeTexture(textureUnit)
	        //bindTexture(gl.TEXTURE_2D, depth.id)
        	depth.bindTo(textureUnit)
        }
    }

    /** Bind the frame buffer, run the rending code, then restore the default frame buffer. */
    def display(code: => Unit) {
        bind
        code
        unbind
    }

    override def dispose() {
        deleteFramebuffer(oid)
        super.dispose
    }
}