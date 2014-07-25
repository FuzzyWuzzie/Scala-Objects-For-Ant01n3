package org.sofa.gfx.backend


import java.io.{File, InputStream, FileInputStream, FileOutputStream, IOException}
import java.nio.channels.{FileChannel}
import javax.imageio.ImageIO

import scala.collection.mutable.{ArrayBuffer=>ScalaArrayBuffer}

import org.sofa.Timer
import org.sofa.nio._
import org.sofa.gfx.{SGL, Texture, TextureImage, TexParams, TexMipMap, ImageFormat, TexAlpha, Libraries, TextureIOException, TextureLoader}


/** Thrown for any error while loading an [[ImageTEX]]. */
class ImageTEXIOException(msg:String, cause:Throwable=null) extends Exception(msg, cause)


/** Loader for the TEX format. */
trait ImageTEXLoader {
	/** Try to load an image in TEX format from the given `fileName`. Return null if no image was loaded. */
	def load(fileName:String):ImageTEX	

	/** Try to load one or several images from the `fileName` and texture `params`.
	  * Either one image is loaded or a pyramid of mipmaps is loaded. The mipmaps
	  * must be nammed using the `fileName` with a `_number` appended before the
	  * extension. For example when loading image `foo.png` the mipmaps will be
	  * nammed `foo_0.png`, `foo_1.png`, etc. */
	def load(fileName:String, params:TexParams):Array[ImageTEX]
}


object ImageTEXLoaderDefault extends ImageTEXLoader {

	def load(fileName:String):ImageTEX = load(new File(fileName))

	def load(fileName:String, params:TexParams):Array[ImageTEX] = {
		import TexMipMap._

		val images = new ScalaArrayBuffer[ImageTEX]

Timer.timer.measure("ImageTEX.load()") {

		params.mipMap match {
			case Load => {
				var i    = 0
				val pos  = fileName.lastIndexOf('_')
				val res  = if(pos > 0) fileName.substring(0, pos) else fileName
				val ext  = if(pos > 0) fileName.substring(pos+3, fileName.length) else ""
				var file = new File("%s_%d.%s".format(res, i, ext))

				while(file.exists) {
					images += load(file)
					i += 1
					file = new File("%s_%d.%s".format(res, i, ext))
				}

				if(images.size <= 0)
					throw new RuntimeException("cannot load any mipmap level for %s (at least %s_%d.%s)".format(fileName, res, i, ext))
			}
			case _ => {
				images += load(fileName)
			}
		}
}
		images.toArray
	}

	/** TEX format image loader for Java. 
  	  *
  	  * This loader and saver will work only where NIO is available (this means no JavaScript backend).
  	  * Throws a [[ImageTEXIOException]] for any loading I/O error. */
	def load(file:File):ImageTEX = {
		if(file.exists) {	
			load(new FileInputStream(file))
		}  else {
			throw new ImageTEXIOException(s"file `${file.getPath()}` not found")
			null
		}
	}

	def load(stream:FileInputStream):ImageTEX = {
		val channel  = stream.getChannel
		val header   = new IntBufferJava(channel.map(FileChannel.MapMode.READ_ONLY, 0, 3*4))

		if(header(0) == 0x7E01) {
			val width    = header(1)
			val height   = header(2)
			val fileSize = width * height * 4
			val data     = new ByteBufferJava(channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize))

			channel.close
			stream.close

			ImageTEX(width, height, data)

		} else {
			throw new ImageTEXIOException("ImageTEX.load() invalid header 0x`%X`. Is `%s` in TEX format ?".format(header(0), stream))
			channel.close
			stream.close
			null
		}
	}
}


/** TEX format image loader for Java. 
  *
  * This loader and saver will work only where NIO is available (this means no JavaScript backend). */
object ImageTEX {
	/** The interchangeable loader for the TEX format. */
	var loader:ImageTEXLoader = ImageTEXLoaderDefault

	/** TEX format image loader. */
	def load(fileName:String):ImageTEX = loader.load(fileName)

	/** TEX format image loader. */
	def load(fileName:String, params:TexParams):Array[ImageTEX] = loader.load(fileName, params)

	/** Try to save an `image` at `fileName` in TEX format. */
	def save(fileName:String, image:ImageTEX) {
		try {
			val headerData = ByteBuffer(3*4)
			val header = IntBuffer(headerData)

			header(0) = 0x7E01		// Magic code
			header(1) = image.width
			header(2) = image.height

			val file = new File(fileName)
			val channel = new FileOutputStream(file, false/*!append=overwrite*/).getChannel
			
			channel.write(headerData.buffer.asInstanceOf[java.nio.ByteBuffer])
			channel.write(image.data.buffer.asInstanceOf[java.nio.ByteBuffer])
			channel.close
		} catch {
			case e:IOException => throw new ImageTEXIOException("I/O error while loading image ${fileName} in TEX format", e)
			case x:Throwable => throw new ImageTEXIOException("Unknown error while loading image ${fileName} in TEX format", x)
		}
	}	

	/** Try to load an image from another format into a TEX image. 
      *
      * This loads an image using Java's `ImageIO` into a [[BufferedImage]], then
      * copy it to a [[ByteBuffer]] inverting the Y axis, and premultiplying each
      * pixel red, green, and blue by the pixel alpha value (premultiplied alpha).
      * Then create a [[ImageTEX]] from this buffer.
	  */
	def loadFrom(fileName:String):ImageTEX = {
		val file = new File(fileName) 

		if(file.exists) {
			val image  = ImageIO.read(file)
	        val width  = image.getWidth
	        val height = image.getHeight
	    	val buf    = ByteBuffer(width * height * 4, true)
	        
	        // Very inefficient... and probably a cause to use the TEX format...

	        var b = 0
	        var y = height - 1
	        var x = 0

	        while(y >= 0) {
	            x = 0
	            while(x < width) {
	                val rgba = image.getRGB(x,y)
	                buf(b+3) = ((rgba>>24) & 0xFF).toByte   // A
	                val alpha = (buf(b+3)&0xFF).toInt / 255.0
	                buf(b+0) = ((((rgba>>16) & 0xFF) * alpha).toInt & 0xFF).toByte // R
	                buf(b+1) = ((((rgba>> 8) & 0xFF) * alpha).toInt & 0xFF).toByte // G
	                buf(b+2) = ((((rgba>> 0) & 0xFF) * alpha).toInt & 0xFF).toByte // B

	                x += 1
	                b += 4
	            }
	            y -= 1
	        }
	        
	        ImageTEX(width, height, buf)
	    } else {
	    	throw new TextureIOException(s"cannot load image file ${fileName}, file not found")
	    }
	}

	/** New empty image in TEX format of `width` times `height` pixels. */
	def apply(width:Int, height:Int, data:ByteBuffer):ImageTEX = new ImageTEX(width, height, data)
}


/** An image in the TEX format.
  *
  * The goal of this format is to be directly usable with OpenGL. The pixel data is always
  * stored as 32bits integers. The first pixel in the buffer maps to the lower-left corner
  * of the image. Subsequent rows go up in the image. Each pixel contains four bytes,
  * each byte contains red, green, blue and finally alpha values between 0 and 255.
  * The alpha is always premultiplied to the red, green and blue values. 
  *
  * The file format is very simple. A file contains the raw data under of the image
  * prefixed by 3 32bits integers (12 bytes). The first integer is the magic number
  * 0x7E01 (last two digits is the version of the file). The second two integers are
  * the width and the height of the image, allowing to deduce the size of the remaining
  * part of the file, since it is always `width * height * 4` bytes. */
class ImageTEX(val width:Int, val height:Int, val data:ByteBuffer)  {
	def this(width:Int, height:Int) { this(width, height, ByteBuffer(width*height*4)) }
}


/** A [[TextureImage]] adapted to the [[ImageTEX]] format. */
class TextureImageTEX(val images:Array[ImageTEX], val params:TexParams) extends TextureImage {
	import ImageFormat._

	def this(fileName:String, params:TexParams) { this(ImageTEX.load(fileName, params), params) }

	def this(image:ImageTEX, params:TexParams) { this(Array[ImageTEX](image), params) }

    def width:Int = images(0).width

    def height:Int = images(0).height

    def format:ImageFormat.Value = RGBA_8888

    def texImage2D(gl:SGL, mode:Int) {
    	var level = 0
    	val maxLevels = images.length

Timer.timer.measure("TextureImageTEX.texImage2D()") {
    	while(level < maxLevels) {
    		gl.texImage2D(mode, level, gl.RGBA,
    			images(level).width, images(level).height, 0,
    			gl.RGBA, gl.UNSIGNED_BYTE, images(level).data)

    		level += 1
    	}
}
    	gl.checkErrors
Timer.timer.measure("TextureImageTEX.genMipmaps()") {
    	if(maxLevels == 1 && params.mipMap == TexMipMap.Generate)
    		gl.generateMipmaps(gl.TEXTURE_2D)
    }
}
}


/** Default texture loader that uses files in the local file system
  * and [[backend.TextureImageAwt]]. */
class TextureLoaderTEX extends TextureLoader {
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

		findPath(name, Texture.path) match {
			case null     => throw new IOException("cannot locate texture %s (path %s)".format(name, Texture.path.mkString(":")))
			case x:String => new TextureImageTEX(x, params)
		}
	}
}
