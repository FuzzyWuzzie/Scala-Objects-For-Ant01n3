package org.sofa.opengl.backend

import org.sofa.nio.ByteBuffer
import org.sofa.opengl.{SGL, Texture, TextureLoader, TextureImage, ImageFormat, TexParams, TexMipMap, TexAlpha}
import java.io.{InputStream, IOException}
import android.content.res.Resources
import android.opengl.{GLUtils, GLES20}
import android.graphics.{Bitmap, BitmapFactory, Color}
import org.sofa.backend.AndroidLoader
import scala.collection.mutable.{ArrayBuffer => ScalaArrayBuffer}

class AndroidTextureLoader(val resources:Resources) extends TextureLoader with AndroidLoader {
	def open(resource:String, params:TexParams):TextureImage = {
		//new TextureImageAndroid(BitmapFactory.decodeStream(resources.getAssets.open(searchInAssets(resource, Texture.path))))

		import TexMipMap._

		var name = params.mipMap match {
			case Load => {
				val pos  = resource.lastIndexOf('.')
		    	val res  = if(pos>0) resource.substring(0, pos) else resource
    			val ext  = if(pos>0) resource.substring(pos+1, resource.length) else ""
    			"%s_0.%s".format(res, ext)
			}
			case _ => { resource }
		}
		
		searchInAssets(name,Texture.path) match {
			case null     => throw new IOException("cannot locate texture %s (path %s)".format(name, Texture.path.mkString(":")))
			case x:String => new TextureImageAndroid(x, params)
		}
	}
}

object TextureImageAndroid {
	protected def read(fileName:String, params:TexParams):ScalaArrayBuffer[Bitmap] = {
		import TexMipMap._

		val images = new ScalaArrayBuffer[Bitmap]
		val loader = Texture.loader.asInstanceOf[AndroidLoader]

		params.mipMap match {
			case Load => {
				var i    = 0
				val pos  = fileName.lastIndexOf('_')
		    	val res  = if(pos>0) fileName.substring(0, pos) else fileName
    			val ext  = if(pos>0) fileName.substring(pos+3, fileName.length) else ""
    			var file = "%s_%d.%s".format(res, i, ext)

    			while(loader.exists(file)) {
    				images += BitmapFactory.decodeStream(loader.resources.getAssets.open(file))
    				i      += 1
    				file    = "%s_%d.%s".format(res, i, ext)
    			}

				if(images.size <= 0)
					throw new RuntimeException("cannot load any mipmap level for %s (at least %s_%d.%s)".format(fileName, res, i, ext))
			}
			case _    => {
				images += BitmapFactory.decodeStream(loader.resources.getAssets.open(fileName)) //ImageIO.read(new File(fileName))
			}
		}

		images
	}
}

/** A texture image class for Android. */
class TextureImageAndroid(val data:ScalaArrayBuffer[Bitmap], val params:TexParams) extends TextureImage {
	def this(fileName:String, params:TexParams) { this(TextureImageAndroid.read(fileName, params), params) }

	def this(data:Bitmap, params:TexParams) { this(ScalaArrayBuffer[Bitmap](data), params) }

	protected val imgFormat = verify

	protected def verify():ImageFormat.Value = {
		import Bitmap.Config._
		data(0).getConfig match {
            case ARGB_8888        => ImageFormat.RGBA_8888
            case ALPHA_8          => ImageFormat.A_8
            case _ => { throw new RuntimeException("unsupported bitmap format (supports ALPHA_8, ARGB_8888)") }
        }
	}

    def texImage2D(gl:SGL, mode:Int) {
// TODO XXX premultiplied alpha !

    	var level = 0
    	val maxLevels = data.length

    	while(level < maxLevels) {
    		// We have to send the whole mip-map pyramid in OpenGLES 2.0 since it does not
    		// support TEXTURE_MAX_LEVEL and TEXTURE_BASE_LEVEL :(

    		if(format == ImageFormat.A_8) {
				val align = gl.getInteger(gl.UNPACK_ALIGNMENT)
	        	val pad   = (align - (width % align)) % align
    	     
				if(pad == 0) {
    	    		GLUtils.texImage2D(gl.TEXTURE_2D, level, GLES20.GL_ALPHA, data(level), 0)
    	 		} else {
    	 			// We need to align the image data with the unpack alignment
    	 			// in order to have an efficient texture memory access.

    	 			val bytes = imageDataGray(data(level), align)

    	 			gl.texImage2D(mode, level, GLES20.GL_ALPHA, data(level).getWidth, data(level).getHeight, 0, GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, bytes)
    	 		}
    		} else {
    			if(params.alpha == TexAlpha.Premultiply) {
    				val bytes = imageDataRgba(data(level), true)

    				gl.texImage2D(mode, level, GLES20.GL_RGBA, data(level).getWidth, data(level).getHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bytes) 
    			} else {
	        		GLUtils.texImage2D(gl.TEXTURE_2D, level, GLES20.GL_RGBA,  data(level), 0)
    			}
        	}

        	level += 1
    	}

        if(maxLevels == 1 && params.mipMap == TexMipMap.Generate)
            gl.generateMipmaps(gl.TEXTURE_2D)
    }

    protected def imageDataRgba(image:Bitmap, premultiplyAlpha:Boolean):ByteBuffer = {
    	val width  = image.getWidth
    	val height = image.getHeight
    	val bytes  = ByteBuffer(width*height*4, true)

    	// Very very inefficient.

    	var b = 0
    	var y = height - 1
    	var x = 0

    	while(y >= 0) {
    		x = 0
    		while(x < width) {
    			val rgba = image.getPixel(x, y)
    			bytes(b+3) = (Color.alpha(rgba) & 0xFF).toByte

    			if(premultiplyAlpha) {
    				val alpha  =   Color.alpha(rgba) / 255.0
    				bytes(b+0) = ((Color.red(rgba)   * alpha).toInt & 0xFF).toByte
	    			bytes(b+1) = ((Color.green(rgba) * alpha).toInt & 0xFF).toByte
    				bytes(b+2) = ((Color.blue(rgba)  * alpha).toInt & 0xFF).toByte
    			} else {
    				bytes(b+0) = (Color.red(rgba)   & 0xFF).toByte
    				bytes(b+1) = (Color.green(rgba) & 0xFF).toByte
	    			bytes(b+2) = (Color.blue(rgba)  & 0xFF).toByte
    			}

    			x += 1
    			b += 4
    		}

    		y -= 1
    	}

    	bytes
    }

    protected def imageDataGray(image:Bitmap, align:Int):ByteBuffer = {
        val width  = image.getWidth
        val height = image.getHeight
        val pad    = (align - (width % align)) % align
        val buf    = ByteBuffer((width + pad) * height, true)   // Take care to pad data on 4 bytes.
        
        // Very, but very, inefficient. All this to add padding.
        // I have to do test to assert this will bring a a real
        // benefit while drawing, compared to changing the unpack
        // alignment. Furthermore there is a old bug in Bitmap
        // on Android, where 8 bit grey bitmaps getPixel or
        // getPixels always return black... No luck.
Console.err.println("## Be careful very slow texImage2D for grey bitmap with unpack align != 4")
        val bytes = ByteBuffer(width*height, true)
        image.copyPixelsToBuffer(bytes.buffer.asInstanceOf[java.nio.ByteBuffer]) 

		var y = 0        
		var x = 0
		var b = 0

		while(y < height) {
            x = 0
            while(x < width) {
            	buf(b) = bytes(y*width+x)
            	x += 1
            	b += 1
            }
            b += pad
            y += 1
		}

//        var b = 0
//         var y = 0
//         var x = 0
//
//         while(y < height) {
//             x = 0
//             while(x < width) {
//                 val rgba = image.getPixel(x,y)
//                 buf(b) = Color.blue(rgba).toByte
//                 x += 1
//                 b += 1
//             }
//             b += pad   // Take care of align at end of row.
//             y += 1
//         }

        buf
    }

    def format:ImageFormat.Value = imgFormat

    def width:Int = data(0).getWidth

    def height:Int = data(0).getHeight
}