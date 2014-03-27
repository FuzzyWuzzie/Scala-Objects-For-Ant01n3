package org.sofa.opengl

import org.sofa.nio._
import java.io.{File, IOException}
import scala.collection.mutable.{ArrayBuffer=>ScalaArrayBuffer}

/** An alternate frame buffer that renders in a texture that can then be used onto onto objects. */
class TextureFramebuffer(gl:SGL, val width:Int, val height:Int, val minFilter:Int, val magFilter:Int) extends OpenGLObject(gl) {
    import gl._

    protected var colorid:AnyRef = null

    protected var depthid:AnyRef = null

    init

    def this(gl:SGL, width:Int, height:Int) { this(gl, width, height, gl.NEAREST, gl.NEAREST) }

    protected def init() {
        super.init(createFramebuffer)
        
        // Generate a texture to hold the colour buffer.

        colorid = createTexture
        val buffer = ByteBuffer(width*height*4, true)   // TODO not sure we have to create this.

        gl.bindTexture(gl.TEXTURE_2D, colorid)
        texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, width, height, 0, gl.RGBA, gl.UNSIGNED_BYTE, buffer)

        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, magFilter)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, minFilter)

        bindTexture(gl.TEXTURE_2D, null)

        checkErrors

        // Create a texture to hold the depth buffer.
    
        depthid = createTexture
        
        gl.bindTexture(gl.TEXTURE_2D, depthid)
        texImage2D(gl.TEXTURE_2D, 0, gl.DEPTH_COMPONENT, width, height, 0, gl.DEPTH_COMPONENT, gl.UNSIGNED_SHORT, buffer)

        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, magFilter)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, minFilter)

        bindTexture(gl.TEXTURE_2D, null)

        checkErrors
    }

    /** Bind a new frame buffer that render in a texture. Set the viewport of the size of the texture. */
    def bindFrameBuffer() {
        // Associate the textures with the FBO.

        bindFramebuffer(gl.FRAMEBUFFER, oid)

        viewport(0, 0, width, height)

        framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, colorid, 0)
        framebufferTexture2D(gl.FRAMEBUFFER, gl.DEPTH_ATTACHMENT, gl.TEXTURE_2D, depthid, 0)

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
    }

    /** Bind the color texture (the framebuffer) for the TEXTURE_2D target. */
    def bindColorTexture() {
        bindTexture(gl.TEXTURE_2D, colorid)
    }

    /** Both bind the color texture and specify the texture unit it is bound to. */
    def bindColorTextureTo(textureUnit:Int) {
        activeTexture(textureUnit)
        bindTexture(gl.TEXTURE_2D, colorid)        
    }

    /** Bind the depth texture (the depth buffer) for the TEXTURE_2D target. */
    def bindDepthTexture() {
        bindTexture(gl.TEXTURE_2D, depthid)
    }

    /** Both bind the depth texture and specify the texture unit it is bound to. */
    def bindDepthTextureTo(textureUnit:Int) {
        activeTexture(textureUnit)
        bindTexture(gl.TEXTURE_2D, depthid)
    }

    /** Bind the frame buffer, run the rending code, then restore the default frame buffer. */
    def display(code: => Unit) {
        bindFrameBuffer
        code
        bindFramebuffer(gl.FRAMEBUFFER, null)
    }

    override def dispose() {
        deleteFramebuffer(oid)
        super.dispose
    }
}