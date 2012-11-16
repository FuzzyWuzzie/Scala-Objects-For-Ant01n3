package org.sofa.opengl

import org.sofa.nio._
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.{File, IOException}

/** Companion object for Texture.
  *
  * This object allows to define a loader a thing that loads images and transform them
  * as usable textures, as well as an include path thatallows to find textures in
  * the current environment (depending on the loader). */
object Texture {
    /** Set of paths where texture images are to be searched. */
    var includePath = new scala.collection.mutable.ArrayBuffer[String]()

    /** Loaded, dependant on the underlying system that allows to find, read
      * and transform an image into an input stream. */
    var loader:TextureLoader = new DefaultTextureLoader()
}

/** The image data used for the texture. */
abstract class TextureImage {
    /** Width in pixels. */
    def width:Int
    /** Height in pixels. */
    def height:Int
    /** Initialise this image. */
    def initFromData(gl:SGL, mode:Int, doGenerateMipmaps:Boolean)
}

/** A texture image data using an AWT buffered image. */
class TextureImageAwt(val data:BufferedImage) extends TextureImage {
    def initFromData(gl:SGL, mode:Int, doGenerateMipmaps:Boolean) {
       val (format, theType) = imageFormatAndType(gl, this.data)
       val data = imageData(this.data)
       val width = this.data.getWidth
       val height = this.data.getHeight
       gl.texImage2D(mode, 0, gl.RGBA, width, height, 0, format, theType, data)
       if(doGenerateMipmaps)
           gl.generateMipmaps(gl.TEXTURE_2D)
    }

    def width:Int = data.getWidth

    def height:Int = data.getHeight

    protected def imageFormatAndType(gl:SGL, image:BufferedImage):(Int, Int) = {
        (gl.RGBA, gl.UNSIGNED_BYTE) // TODO, see inefficient imageData()
    }
    
    protected def imageData(image:BufferedImage):ByteBuffer = {
        val width = data.getWidth
        val height = data.getHeight
        val buf = new ByteBuffer(width * height * 4, true)
        
        // Very inefficient.
        
        var b = 0
        
        for(y <- 0 until height) {
            for(x <-0 until width) {
                val rgba = image.getRGB(x, y)
                buf(b+0) = ((rgba>>16) & 0xFF).toByte   // R
                buf(b+1) = ((rgba>> 8) & 0xFF).toByte   // G
                buf(b+2) = ((rgba>> 0) & 0xFF).toByte   // B
                buf(b+3) = ((rgba>>24) & 0xFF).toByte   // A
                
                b += 4
            }
        }
        
        buf
    }
}

/** Locate and fetch image data. */
trait TextureLoader {
    /** Try to locate a resource in the include path and load it. */
    def open(resource:String):TextureImage
}

/** Default texture loader that uses files in the local file system. */
class DefaultTextureLoader extends TextureLoader {
    def open(resource:String):TextureImage = {
        var file = new File(resource)
        if(file.exists) {
            new TextureImageAwt(ImageIO.read(new File(resource)))
        } else {
            val sep = sys.props.get("file.separator").get

            Texture.includePath.find(path => (new File("%s%s%s".format(path, sep, resource))).exists) match {
                case path:Some[String] => { new TextureImageAwt(ImageIO.read(new File("%s%s%s".format(path.get,sep,resource)))) }
                case None => { throw new IOException("cannot locate texture %s (path %s)".format(resource, Texture.includePath.mkString(":"))) }
            }
        }
    }
}

/** Define a new 1D, 2D or 3D texture.
  * 
  * The binding of the texture is not automatic. You must bind it yourself before doing any
  * operation on it.
  */
class Texture(gl:SGL, val mode:Int, val width:Int, val height:Int, val depth:Int) extends OpenGLObject(gl) {
    import gl._

    init
    
    protected def init() {
        super.init(genTexture)
        bindTexture(mode, oid)
        checkErrors
    }
    
    // def this(gl:SGL, buffer:ByteBuffer, width:Int, height:Int, format:Int, theType:Int, doGenerateMipmaps:Boolean) {
    //     this(gl, gl.TEXTURE_2D, width, height, 0)
    //     initFromBuffer(buffer, format, theType, doGenerateMipmaps)
    // }
    
    def this(gl:SGL, image:TextureImage, generateMipmaps:Boolean) {
        this(gl, gl.TEXTURE_2D, image.width, image.height, 0)
        image.initFromData(gl, mode, generateMipmaps)
       checkErrors
    }

    def this(gl:SGL, imageFileName:String, generateMipmaps:Boolean) {
        this(gl, Texture.loader.open(imageFileName), generateMipmaps)
    }
    
    override def dispose() {
        deleteTexture(oid)
        super.dispose
    }
    
    /** Specify the minification and magnification filters. */
    def minMagFilter(minFilter:Int, magFilter:Int) {
        parameter(gl.TEXTURE_MIN_FILTER, minFilter)
        parameter(gl.TEXTURE_MAG_FILTER, magFilter)
        checkErrors
    }

    /** Specify the wrapping behavior along S and T axes. */
    def wrap(value:Int) {
        parameter(gl.TEXTURE_WRAP_S, value)
        parameter(gl.TEXTURE_WRAP_T, value)
        checkErrors
    }
    
    def parameter(name:Int, value:Float) = texParameter(mode, name, value)
    def parameter(name:Int, value:Int) = texParameter(mode, name, value)
    def parameter(name:Int, values:FloatBuffer) = texParameter(mode, name, values)
    def parameter(name:Int, values:IntBuffer) = texParameter(mode, name, values)
    
    /** Bind the texture as current. */
    def bind() {
        bindTexture(mode, oid)
    }
    
    /** Both bind the texture and specify to which texture unit it binds. */
    def bindTo(textureUnit:Int) {
        activeTexture(textureUnit)
        bindTexture(mode, oid)
    }
    
}

/** An alternate frame buffer that renders in a texture that can then be used onto onto objects. */
class TextureFramebuffer(gl:SGL, val width:Int, val height:Int) extends OpenGLObject(gl) {
    import gl._

    protected var colorid:Int = -1 

    protected var depthid:Int = -1

    init

    protected def init() {
        super.init(genFramebuffer)
        
        // Generate a texture to hold the colour buffer.

        colorid = genTexture
        val buffer = ByteBuffer(width*height*4, true)   // TODO not sure we have to create this.

        gl.bindTexture(gl.TEXTURE_2D, colorid)
        texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, width, height, 0, gl.RGBA, gl.UNSIGNED_BYTE, buffer)

        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST)

        bindTexture(gl.TEXTURE_2D, 0)

        checkErrors

        // Create a texture to hold the depth buffer.
    
        depthid = genTexture
        
        gl.bindTexture(gl.TEXTURE_2D, depthid)
        texImage2D(gl.TEXTURE_2D, 0, gl.DEPTH_COMPONENT, width, height, 0, gl.DEPTH_COMPONENT, gl.UNSIGNED_SHORT, buffer)

        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST)

        bindTexture(gl.TEXTURE_2D, 0)

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
        bindFramebuffer(gl.FRAMEBUFFER, 0)
    }

    override def dispose() {
        deleteFramebuffer(oid)
        super.dispose
    }
}