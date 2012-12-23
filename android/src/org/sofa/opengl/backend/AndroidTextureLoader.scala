package org.sofa.opengl.backend

import org.sofa.opengl.{SGL, Texture, TextureLoader, TextureImage, ImageFormat}
import java.io.{InputStream, IOException}
import android.content.res.Resources
import android.opengl.{GLUtils, GLES20}
import android.graphics.{Bitmap, BitmapFactory}

class AndroidTextureLoader(val resources:Resources) extends TextureLoader {
	def open(resource:String):TextureImage = {
		if(exists("", resource)) {
			new TextureImageAndroid(BitmapFactory.decodeStream(resources.getAssets.open(resource)))
		} else {
			Texture.path.find(path => exists(path, resource)) match {
				case path:Some[String] => { new TextureImageAndroid(BitmapFactory.decodeStream(resources.getAssets.open("%s/%s".format(path.get,resource)))) }
				case None => { throw new IOException("cannot open shader resource %s".format(resource)) }
			}
		}
	}

	protected def exists(path:String, resource:String):Boolean = {
		// We cut the path/resource name anew, since the resource can also contain path
		// separators.
		
		val fileName = if(path.length>0) "%s/%s".format(path, resource) else resource
		val pos      = fileName.lastIndexOf('/')
		var newName  = fileName
		var newPath  = ""
		if(pos >= 0) {
			newPath = fileName.substring(0, pos)
			newName = fileName.substring(pos+1)
		}
System.err.println("testing path %s".format(newName))
		resources.getAssets.list(newPath).contains(newName)
	}
}

class TextureImageAndroid(val data:Bitmap) extends TextureImage {
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