package org.sofa.opengl

import org.sofa.nio._
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.{File, IOException}

object Texture {
    var includePath = new scala.collection.mutable.ArrayBuffer[String]()

    var loader:TextureLoader = new DefaultTextureLoader()
}

abstract class TextureImage {
    def width:Int
    def height:Int
    def initFromData(gl:SGL, mode:Int, doGenerateMipmaps:Boolean)
}

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

trait TextureLoader {
    def open(resource:String):TextureImage
}

class DefaultTextureLoader extends TextureLoader {
    def open(resource:String):TextureImage = {
        var file = new File(resource)
        if(file.exists) {
            new TextureImageAwt(ImageIO.read(new File(resource)))
        } else {
            val sep = sys.props.get("file.separator").get

            Shader.includePath.find(path => (new File("%s%s%s".format(path, sep, resource))).exists) match {
                case path:Some[String] => { new TextureImageAwt(ImageIO.read(new File("%s%s%s".format(path.get,sep,resource)))) }
                case None => { throw new IOException("cannot locate texture %s".format(resource)) }
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
    
    def minMagFilter(minFilter:Int, magFilter:Int) {
        parameter(gl.TEXTURE_MIN_FILTER, minFilter)
        parameter(gl.TEXTURE_MAG_FILTER, magFilter)
        checkErrors
    }
    
    def wrap(value:Int) {
        parameter(gl.TEXTURE_WRAP_S, value)
        parameter(gl.TEXTURE_WRAP_T, value)
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
    
}