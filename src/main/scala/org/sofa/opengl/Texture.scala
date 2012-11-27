package org.sofa.opengl

import org.sofa.nio._
import java.awt.image.{BufferedImage, DataBufferByte}
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

object ImageFormat extends Enumeration {
    val RGBA_8888 = Value
    val A_8 = Value
}

/** The image data used for the texture.
  *
  * This is an abstract class that represents image data to be transfered into a texture.
  * Most of the time it can be released once the data has been given to OpenGL. Its main
  * method is texImage2D() that will transfer its data to OpenGL. It can be used by multiple
  * texture instances.
  */
trait TextureImage {
    /** Width in pixels. */
    def width:Int
    /** Height in pixels. */
    def height:Int
    /** Image type. */
    def format:ImageFormat.Value 
    /** Initialize the current texture with this image data. */
    def texImage2D(gl:SGL, mode:Int, doGenerateMipmaps:Boolean)
}

/** A texture image data using an AWT buffered image. */
class TextureImageAwt(val data:BufferedImage) extends TextureImage {

    protected val imgFormat = verify

    protected def verify():ImageFormat.Value = {
        import BufferedImage._
        data.getType match {
            case TYPE_INT_ARGB    => ImageFormat.RGBA_8888
            case TYPE_INT_RGB     => ImageFormat.RGBA_8888
            case TYPE_INT_BGR     => ImageFormat.RGBA_8888
            case TYPE_3BYTE_BGR   => ImageFormat.RGBA_8888
            case TYPE_4BYTE_ABGR  => ImageFormat.RGBA_8888
            case TYPE_BYTE_GRAY   => ImageFormat.A_8
            case _ => { throw new RuntimeException("unsupported image format (support INT_ARGB, INT_RGB, BYTE_GRAY)") }
        }
    }

    def texImage2D(gl:SGL, mode:Int, doGenerateMipmaps:Boolean) {
       val (format, internalFormat, theType, bytes) = imageFormatAndType(gl, data)
       
       gl.texImage2D(mode, 0, format, width, height, 0, internalFormat, theType, bytes)
       
       if(doGenerateMipmaps)
           gl.generateMipmaps(gl.TEXTURE_2D)
    }

    def width:Int = data.getWidth

    def height:Int = data.getHeight

    def format:ImageFormat.Value = imgFormat

    protected def imageFormatAndType(gl:SGL, image:BufferedImage):(Int, Int, Int, ByteBuffer) = {
        val align = gl.getInteger(gl.UNPACK_ALIGNMENT)
        
        imgFormat match {
            case ImageFormat.RGBA_8888 => (gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, imageDataRGBA(image, align))
            case ImageFormat.A_8       => (gl.ALPHA, gl.ALPHA, gl.UNSIGNED_BYTE, imageDataGray(image, align))
            case _                     => throw new RuntimeException("WTF?")
        }
    }
    
    protected def imageDataRGBA(image:BufferedImage, align:Int):ByteBuffer = {
        val width  = image.getWidth
        val height = image.getHeight
        val pad    = (align - (width % align)) % align
        
        // val dataBuffer     = image.getRaster.getDataBuffer
        // val buf:ByteBuffer = dataBuffer match {
        //     case b: DataBufferByte => new ByteBuffer(b.getData, false)
        //     case _ => null
        // }
        
        val buf = new ByteBuffer((width + pad) * height * 4, true)
        
        // Very inefficient.
        
        var b = 0
        var y = 0
        var x = 0

        while(y < height) {
            x = 0
            while(x < width) {
                val rgba = image.getRGB(x,y)
                buf(b+0) = ((rgba>>16) & 0xFF).toByte   // R
                buf(b+1) = ((rgba>> 8) & 0xFF).toByte   // G
                buf(b+2) = ((rgba>> 0) & 0xFF).toByte   // B
                buf(b+3) = ((rgba>>24) & 0xFF).toByte   // A
                x += 1
                b += 4
            }
            b += pad
            y += 1
        }
        
        buf
    }
    
    protected def imageDataGray(image:BufferedImage, align:Int):ByteBuffer = {
        val width  = data.getWidth
        val height = data.getHeight
        val pad    = (align - (width % align)) % align
        val buf    = new ByteBuffer((width + pad) * height, true)   // Take care to pad data on 4 bytes.
        
        // Very inefficient.
        
        var b = 0
        var y = 0
        var x = 0

        while(y < height) {
            x = 0
            while(x < width) {
                val rgba = image.getRGB(x,y)
                buf(b) = (rgba & 0xFF).toByte
                x += 1
                b += 1
            }
            b += pad   // Take care of align at end of row.
            y += 1
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
        image.texImage2D(gl, mode, generateMipmaps)
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
class TextureFramebuffer(gl:SGL, val width:Int, val height:Int, val minFilter:Int, val magFilter:Int) extends OpenGLObject(gl) {
    import gl._

    protected var colorid:Int = -1 

    protected var depthid:Int = -1

    init

    def this(gl:SGL, width:Int, height:Int) { this(gl, width, height, gl.NEAREST, gl.NEAREST) }

    protected def init() {
        super.init(genFramebuffer)
        
        // Generate a texture to hold the colour buffer.

        colorid = genTexture
        val buffer = ByteBuffer(width*height*4, true)   // TODO not sure we have to create this.

        gl.bindTexture(gl.TEXTURE_2D, colorid)
        texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, width, height, 0, gl.RGBA, gl.UNSIGNED_BYTE, buffer)

        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, magFilter)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, minFilter)

        bindTexture(gl.TEXTURE_2D, 0)

        checkErrors

        // Create a texture to hold the depth buffer.
    
        depthid = genTexture
        
        gl.bindTexture(gl.TEXTURE_2D, depthid)
        texImage2D(gl.TEXTURE_2D, 0, gl.DEPTH_COMPONENT, width, height, 0, gl.DEPTH_COMPONENT, gl.UNSIGNED_SHORT, buffer)

        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, magFilter)
        texParameter(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, minFilter)

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