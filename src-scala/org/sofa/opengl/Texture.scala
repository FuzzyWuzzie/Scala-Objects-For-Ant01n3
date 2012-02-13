package org.sofa.opengl

import javax.media.opengl._
import org.sofa.nio._
import GL._
import GL2._
import GL2GL3._
import GL2ES2._
import GL3._
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File

object TextureMode extends Enumeration {
    val Texture1D:Int = GL_TEXTURE_1D
    val Texture2D:Int = GL_TEXTURE_2D
    val Texture3D:Int = GL_TEXTURE_3D
}

/** Define a new 1D, 2D or 3D texture.
  * 
  * The binding of the texture is not automatic. You must bind it yourself before doing any
  * operation on it.
  */
class Texture(gl:SGL, val mode:Int, val width:Int, val height:Int, val depth:Int) extends OpenGLObject(gl) {
    import TextureMode._
    import gl.gl._
    import gl._

    init
    
    protected def init() {
        super.init(genTexture)
        bindTexture(mode, oid)
        checkErrors
    }
    
    def this(gl:SGL, image:BufferedImage, generateMipmaps:Boolean) {
        this(gl, GL_TEXTURE_2D, image.getWidth, image.getHeight, 0)
        initFromImage(image, generateMipmaps)
    }

    def this(gl:SGL, imageFileName:String, generateMipmaps:Boolean) {
        this(gl, ImageIO.read(new File(imageFileName)), generateMipmaps)
    }
    
    protected def initFromImage(image:BufferedImage, doGenerateMipmaps:Boolean) {
       val (format, theType) = imageFormatAndType(image)
       val data = imageData(image)
       texImage2D(mode, 0, GL_RGBA, width, height, 0, format, theType, data)
       if(doGenerateMipmaps)
           generateMipmaps(GL_TEXTURE_2D)
       checkErrors
    }
    
    override def dispose() {
        deleteTexture(oid)
        super.dispose
    }
    
    def minMagFilter(minFilter:Int, magFilter:Int) {
        parameter(GL_TEXTURE_MIN_FILTER, minFilter)
        parameter(GL_TEXTURE_MAG_FILTER, magFilter)
        checkErrors
    }
    
    def wrap(value:Int) {
        parameter(GL_TEXTURE_WRAP_S, value)
        parameter(GL_TEXTURE_WRAP_T, value)
        checkErrors
    }
    
    def parameter(name:Int, value:Float) = texParameter(mode, name, value)
    def parameter(name:Int, value:Int) = texParameter(mode, name, value)
    def parameter(name:Int, values:FloatBuffer) = texParameter(mode, name, values)
    def parameter(name:Int, values:IntBuffer) = texParameter(mode, name, values)
    
    def bind() {
        bindTexture(mode, oid)
    }
    
    def bindTo(texture:Int) {
        activeTexture(texture)
        bindTexture(mode, oid)
    }
    
    protected def imageFormatAndType(image:BufferedImage):(Int, Int) = {
        (GL_RGBA, GL_UNSIGNED_BYTE)	// TODO, see inefficient imageData()
    }
    
    protected def imageData(image:BufferedImage):ByteBuffer = {
        val buf = new ByteBuffer(width * height * 4)
        
        // Very inefficient.
        
        var b = 0
        
        for(y <- 0 until height) {
            for(x <-0 until width) {
                val rgba = image.getRGB(x, y)
                buf(b+0) = ((rgba>>16) & 0xFF).toByte	// R
                buf(b+1) = ((rgba>> 8) & 0xFF).toByte	// G
                buf(b+2) = ((rgba>> 0) & 0xFF).toByte	// B
                buf(b+3) = ((rgba>>24) & 0xFF).toByte	// A
                
                b += 4
            }
        }
        
        buf
    }
}
