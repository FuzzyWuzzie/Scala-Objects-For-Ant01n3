package org.sofa.opengl.backend

import org.sofa.opengl.{Texture, TextureLoader, ImageFormat}
import scala.collection.mutable.{ArrayBuffer=>ScalaArrayBuffer}



/** Default texture loader that uses files in the local file system
  * and [[TextureImageAwt]]. */
class TextureLoaderWeb extends TextureLoader {
	def open(resource:String, params:TexParams):TextureImage = {
		import TexMipMap._

		var name = params.mipMap match {
			case Load => { 
				val pos  = resource.lastIndexOf('.')
		    	val res  = if(pos>0) resource.substring(0, pos) else resource
    			val ext  = if(pos>0) resource.substring(pos+1, resource.length) else ""
    			
    			"%s_0.%s".format(res, ext)
			}
			case _ => {  resource }
		}

		//findPath(name, Texture.path) match {
		//	case null     => throw new IOException("cannot locate texture %s (path %s)".format(name, Texture.path.mkString(":")))
		//	case x:String => new TextureImageAwt(x, params)
		//}
		new TextureImageWeb(name, params)
	}
}



object TextureImageWeb {
		protected def read(resource:String, params:TexParams):ScalaArrayBuffer[BufferedImage] = {
		import TexMipMap._

		val images = new ScalaArrayBuffer[BufferedImage]

		params.mipMap match {
			case Load => {
				var i    = 0
				val pos  = resource.lastIndexOf('_')
		    	val res  = if(pos>0) resource.substring(0, pos) else resource
    			val ext  = if(pos>0) resource.substring(pos+3, resource.length) else ""
    			var file = new File("%s_%d.%s".format(res, i, ext))

    	// HERE
    			while(file.exists) {
    				images += ImageIO.read(file)
    				i      += 1
    				file    = new File("%s_%d.%s".format(res, i, ext))
    			}

				if(images.size <= 0)
					throw new RuntimeException("cannot load any mipmap level for %s (at least %s_%d.%s)".format(resource, res, i, ext))
			}
			case _    => {
		// HERE
				val image = new Image()
				image = resource
				images += image //ImageIO.read(new File(resource))
			}
		}

		images
	}
}


class TextureImageWeb(resource:String, params:TexParams) extends TextureImage {
    import ImageFormat._

	def this(fileName:String, params:TexParams) { this(TextureImageAwt.read(fileName, params), params) }

	def this(data:BufferedImage, params:TexParams) { this(ScalaArrayBuffer[BufferedImage](data), params) }


    /** Width in pixels. */
    def width:Int

    /** Height in pixels. */
    def height:Int

    /** Image type. */
    def format:ImageFormat.Value

    /** Initialize the current texture with this image data. */
    def texImage2D(gl:SGL, mode:Int)
}
