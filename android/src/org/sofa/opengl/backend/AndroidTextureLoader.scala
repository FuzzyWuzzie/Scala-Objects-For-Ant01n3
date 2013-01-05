package org.sofa.opengl.backend

import org.sofa.nio.ByteBuffer
import org.sofa.opengl.{SGL, Texture, TextureLoader, TextureImage, ImageFormat}
import java.io.{InputStream, IOException}
import android.content.res.Resources
import android.opengl.{GLUtils, GLES20}
import android.graphics.{Bitmap, BitmapFactory, Color}
import org.sofa.backend.AndroidLoader

class AndroidTextureLoader(val resources:Resources) extends TextureLoader with AndroidLoader {
	def open(resource:String):TextureImage = {
		new TextureImageAndroid(BitmapFactory.decodeStream(resources.getAssets.open(searchInAssets(resource, Texture.path))))
	}
}

/** A texture image class for Android. */
class TextureImageAndroid(val data:Bitmap) extends TextureImage {
	// We can use the wonderfull GLUtils.texImage2D ... thank you
	// Android for being a descent API. Shame on you, JDK.

	protected val imgFormat = verify

	protected def verify():ImageFormat.Value = {
		import Bitmap.Config._
		data.getConfig match {
            case ARGB_8888        => ImageFormat.RGBA_8888
            case ALPHA_8          => ImageFormat.A_8
            case _ => { throw new RuntimeException("unsupported bitmap format (supports ALPHA_8, ARGB_8888)") }
        }
	}

    def texImage2D(gl:SGL, mode:Int, doGenerateMipmaps:Boolean) {
    	if(format == ImageFormat.A_8) {
			val align = gl.getInteger(gl.UNPACK_ALIGNMENT)
	        val pad   = (align - (width % align)) % align
    	     
			if(pad == 0) {
    	    	GLUtils.texImage2D(gl.TEXTURE_2D, 0, GLES20.GL_ALPHA, data, 0)
    	 	} else {
    	 		// We need to align the image data with the unpack alignment
    	 		// in order to have an efficient texture memory access.

    	 		val bytes = imageDataGray(data, align)

    	 		gl.texImage2D(mode, 0, GLES20.GL_ALPHA, width, height, 0, GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, bytes)
    	 	}
    	} else {
        	GLUtils.texImage2D(gl.TEXTURE_2D, 0, GLES20.GL_RGBA,  data, 0)
        }

        if(doGenerateMipmaps)
            gl.generateMipmaps(gl.TEXTURE_2D)
    }

    protected def imageDataGray(image:Bitmap, align:Int):ByteBuffer = {
        val width  = image.getWidth
        val height = image.getHeight
        val pad    = (align - (width % align)) % align
        val buf    = new ByteBuffer((width + pad) * height, true)   // Take care to pad data on 4 bytes.
        
        // Very, but very, inefficient. All this to add padding.
        // I have to do test to assert this will bring a a real
        // benefit while drawing, compared to changing the unpack
        // alignment. Furthermore there is a old bug in Bitmap
        // on Android, where 8 bit grey bitmaps getPixel or
        // getPixels always return black... No luck.

        val bytes = new ByteBuffer(width*height, true)
        image.copyPixelsToBuffer(bytes.buffer) 

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

    def width:Int = data.getWidth

    def height:Int = data.getHeight
}