package org.sofa.opengl.backend

import org.sofa.opengl.{SGL, Texture, TextureLoader, TextureImage, ImageFormat}
import java.io.{InputStream, IOException}
import android.content.res.Resources
import android.opengl.{GLUtils, GLES20}
import android.graphics.{Bitmap, BitmapFactory}
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
    	if(format == ImageFormat.A_8)
    	     GLUtils.texImage2D(gl.TEXTURE_2D, 0, GLES20.GL_ALPHA, data, 0)
        else GLUtils.texImage2D(gl.TEXTURE_2D, 0, GLES20.GL_RGBA,  data, 0)
        
        if(doGenerateMipmaps)
            gl.generateMipmaps(gl.TEXTURE_2D)
    }

    def format:ImageFormat.Value = imgFormat

    def width:Int = data.getWidth

    def height:Int = data.getHeight
}