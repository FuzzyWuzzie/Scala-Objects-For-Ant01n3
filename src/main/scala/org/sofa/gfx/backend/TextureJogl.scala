package org.sofa.gfx.backend

import org.sofa.Timer
import org.sofa.nio._
import java.awt.image.{BufferedImage, DataBufferByte}
import javax.imageio.ImageIO
import java.io.{File, IOException}
import scala.collection.mutable.{ArrayBuffer=>ScalaArrayBuffer}
import org.sofa.gfx.{SGL, TextureImage, TexParams, TexMipMap, ImageFormat, TexAlpha, Libraries, TextureIOException}

object TextureImageAwt {
	protected def read(fileName:String, params:TexParams):ScalaArrayBuffer[BufferedImage] = {
		import TexMipMap._

		val images = new ScalaArrayBuffer[BufferedImage]

		params.mipMap match {
			case Load => {
				var i    = 0
				val pos  = fileName.lastIndexOf('_')
		    	val res  = if(pos>0) fileName.substring(0, pos) else fileName
    			val ext  = if(pos>0) fileName.substring(pos+3, fileName.length) else ""
    			var file = new File("%s_%d.%s".format(res, i, ext))

    			while(file.exists) {
    				images += ImageIO.read(file)
    				i      += 1
    				file    = new File("%s_%d.%s".format(res, i, ext))
    			}

				if(images.size <= 0)
					throw new TextureIOException("cannot load any mipmap level for %s (at least %s_%d.%s)".format(fileName, res, i, ext))
			}
			case _    => {
				images += ImageIO.read(new File(fileName))
			}
		}

		images
	}
}

/** A texture image data using one (or several) AWT buffered image(s). */
class TextureImageAwt(val data:ScalaArrayBuffer[BufferedImage], val params:TexParams) extends TextureImage {
	import ImageFormat._

	def this(fileName:String, params:TexParams) { this(TextureImageAwt.read(fileName, params), params) }

	def this(data:BufferedImage, params:TexParams) { this(ScalaArrayBuffer[BufferedImage](data), params) }

    protected var imgFormat = verify

    protected def verify():ImageFormat = {
        import BufferedImage._
        data(0).getType match {
            case TYPE_INT_ARGB    => RGBA_8888
            case TYPE_INT_RGB     => RGBA_8888
            case TYPE_INT_BGR     => RGBA_8888
            case TYPE_3BYTE_BGR   => RGBA_8888
            case TYPE_4BYTE_ABGR  => RGBA_8888
            case TYPE_BYTE_GRAY   => A_8
            case _ => { throw new RuntimeException("unsupported image format (support INT_ARGB, INT_RGB, BYTE_GRAY)") }
        }
    }

    def texImage2D(gl:SGL, mode:Int) {
    	var level = 0
    	val maxLevels = data.length

    	while(level < maxLevels) {
    		// We have to send the whole mip-map pyramid in OpenGLES 2.0 since it does not
    		// support TEXTURE_MAX_LEVEL and TEXTURE_BASE_LEVEL :(

       		val (format, internalFormat, theType, bytes) = imageFormatAndType(gl, data(level))

	    	gl.texImage2D(mode, level, internalFormat, data(level).getWidth, data(level).getHeight, 0, format, theType, bytes)
	    	gl.checkErrors

    		level += 1
    	}
       
    	if(maxLevels == 1 && params.mipMap == TexMipMap.Generate)
        	gl.generateMipmaps(gl.TEXTURE_2D)
    }

    def width:Int = data(0).getWidth

    def height:Int = data(0).getHeight

    def format:ImageFormat = imgFormat

    protected def imageFormatAndType(gl:SGL, image:BufferedImage):(Int, Int, Int, ByteBuffer) = {
        val align        = gl.getInteger(gl.UNPACK_ALIGNMENT)
        val premultAlpha = (params.alpha == TexAlpha.Premultiply)

        imgFormat match {
            case ImageFormat.RGBA_8888 => (gl.RGBA,  gl.RGBA,  gl.UNSIGNED_BYTE, imageDataRGBA(image, align, premultAlpha))
            case ImageFormat.A_8       => (gl.ALPHA, gl.ALPHA, gl.UNSIGNED_BYTE, imageDataGray(image, align))
            case _                     => throw new RuntimeException("WTF?")
        }
    }
    
    protected def imageDataRGBA(image:BufferedImage, align:Int, premultAlpha:Boolean):ByteBuffer = {
    	var buf:ByteBuffer = null
    	Timer.timer.measure("TextureImageAWT.imageDataRGBA()") {
        val width  = image.getWidth
        val height = image.getHeight
        
        buf = ByteBuffer(width * height * 4, true)
        
        // Very inefficient.
        
        // We copy from the height to 0 since the OpenGL spec says:
        //
        // "The first element corresponds to the lower left corner of the texture image."
        // "Subsequent elements progress left-to-right through the remaining texels in the"
        // "lowest row of the texture image, and then in successively higher rows of the"
        // "texture image. The final element corresponds to the upper right corner of the"
        // "texture image."
        //
        // This allows (0,0)UV to be at the lower-left corner and (1,1)UV at the upper-right.

        var b = 0
        var y = height-1
        var x = 0

        while(y >= 0) {
            x = 0
            while(x < width) {
                val rgba = image.getRGB(x,y)
                buf(b+3) = ((rgba>>24) & 0xFF).toByte   // A

                if(premultAlpha) {
                	val alpha = (buf(b+3)&0xFF).toInt / 255.0
                	buf(b+0) = ((((rgba>>16) & 0xFF) * alpha).toInt & 0xFF).toByte // R
                	buf(b+1) = ((((rgba>> 8) & 0xFF) * alpha).toInt & 0xFF).toByte // G
                	buf(b+2) = ((((rgba>> 0) & 0xFF) * alpha).toInt & 0xFF).toByte // B
                } else {
        	        buf(b+0) = ((rgba>>16) & 0xFF).toByte   // R
    	            buf(b+1) = ((rgba>> 8) & 0xFF).toByte   // G
	                buf(b+2) = ((rgba>> 0) & 0xFF).toByte   // B
                }

                x += 1
                b += 4
            }
            y -= 1
        }
        
    	}
        buf
    }
    
    protected def imageDataGray(image:BufferedImage, align:Int):ByteBuffer = {
        val width  = image.getWidth
        val height = image.getHeight
        val pad    = (align - (width % align)) % align
        val buf    = ByteBuffer((width + pad) * height, true)   // Take care to pad data on 4 bytes.
        
        // Very inefficient.
        
        var b = 0
        var y = height-1
        var x = 0

        while(y >= 0) {
            x = 0
            while(x < width) {
                val rgba = image.getRGB(x,y)
                buf(b) = (rgba & 0xFF).toByte
                x += 1
                b += 1
            }
            b += pad   // Take care of align at end of row.
            y -= 1
        }
        
        buf
    }
}