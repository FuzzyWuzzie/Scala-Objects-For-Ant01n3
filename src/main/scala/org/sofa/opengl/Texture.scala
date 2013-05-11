package org.sofa.opengl

import org.sofa.nio._
import java.awt.image.{BufferedImage, DataBufferByte}
import javax.imageio.ImageIO
import java.io.{File, IOException}
import scala.collection.mutable.{ArrayBuffer=>ScalaArrayBuffer}

/** Companion object for Texture.
  *
  * This object allows to define a loader a thing that loads images and transform them
  * as usable textures, as well as an include path thatallows to find textures in
  * the current environment (depending on the loader). */
object Texture {
    /** Set of paths where texture images are to be searched. */
    var path = new scala.collection.mutable.ArrayBuffer[String]()

    /** Loaded, dependant on the underlying system that allows to find, read
      * and transform an image into an input stream. */
    var loader:TextureLoader = new DefaultTextureLoader()
}

object ImageFormat extends Enumeration {
    val RGBA_8888 = Value
    val A_8 = Value
    type ImageFormat = Value
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
    /** Shortcut to width/height ratio. */
    def ratio:Double = (width.toDouble / height.toDouble)
    /** Image type. */
    def format:ImageFormat.Value
    /** Initialize the current texture with this image data. */
    def texImage2D(gl:SGL, mode:Int)
}

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
					throw new RuntimeException("cannot load any mipmap level for %s (at least %s_%d.%s)".format(fileName, res, i, ext))
			}
			case _    => {
				images += ImageIO.read(new File(fileName))
			}
		}

		images
	}
}

/** A texture image data using an AWT buffered image. */
class TextureImageAwt(val data:ScalaArrayBuffer[BufferedImage], val params:TexParams) extends TextureImage {
	import ImageFormat._

    protected var imgFormat:ImageFormat = null

	def this(fileName:String, params:TexParams) {
		this(TextureImageAwt.read(fileName, params), params)
	}

	imgFormat = verify

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

//		gl.texParameter(gl.TEXTURE_2D, gl.TEXTURE_BASE_LEVEL, 0);
//		gl.texParameter(gl.TEXTURE_2D, gl.TEXTURE_MAX_LEVEL, maxLevels-1);

    	while(level < maxLevels) {
    		// We have to send the whole mip-map pyramid in OpenGLES 2.0 since it does not
    		// support TEXTURE_MAX_LEVEL and TEXTURE_BASE_LEVEL :(

       		val (format, internalFormat, theType, bytes) = imageFormatAndType(gl, data(level))

	    	gl.texImage2D(mode, level, format, data(level).getWidth, data(level).getHeight, 0, internalFormat, theType, bytes)
	    	gl.checkErrors

    		level += 1
    	}
       
    	if(params.mipMap == TexMipMap.Generate)
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
        val width  = image.getWidth
        val height = image.getHeight
        
        val buf = new ByteBuffer(width * height * 4, true)
        
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
        
        buf
    }
    
    protected def imageDataGray(image:BufferedImage, align:Int):ByteBuffer = {
        val width  = image.getWidth
        val height = image.getHeight
        val pad    = (align - (width % align)) % align
        val buf    = new ByteBuffer((width + pad) * height, true)   // Take care to pad data on 4 bytes.
        
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

/** Locate and fetch image data. */
trait TextureLoader {
    /** Try to locate a resource in the include path and load it.
      *
      * The params may indicate how to load the resource.
      *
      * The mip-map parameter
      * allow to tell if mip-maps must be generated or loaded. If mip-maps are
      * loaded, the resource name is used as a basis, but integers must be appended
      * to the real name of the resource separated from it by an underscore. The
      * first number must be 0 and is the most accurate level. For example if the
      * resource name is "foo.png" the loader will look for "foo_0.png" then
      * for "foo_1.png" etc. until it cannot find the next level.
      *
      * The alpha parameter allows to know if the data loaded must be converted to
      * premultiply by alpha. */
    def open(resource:String, params:TexParams):TextureImage
}

/** Default texture loader that uses files in the local file system. */
class DefaultTextureLoader extends TextureLoader {
	def open(resource:String, params:TexParams):TextureImage = {
		import TexMipMap._

		params.mipMap match {
			case Load => { 
println("***** LOADING mip-maps")
				val pos  = resource.lastIndexOf('.')
		    	val res  = if(pos>0) resource.substring(0, pos) else resource
    			val ext  = if(pos>0) resource.substring(pos+1, resource.length) else ""
    			val name = "%s_0.%s".format(res, ext)

				findPath(name) match {
					case null     => throw new IOException("cannot locate texture %s (path %s)".format(name, Texture.path.mkString(":")))
					case x:String => new TextureImageAwt(x, params)
				}
			}
			case _ => {  
println("***** GENERATING or no mip-maps")
				findPath(resource) match {
					case null     => throw new IOException("cannot locate texture %s (path %s)".format(resource, Texture.path.mkString(":")))
					case x:String => new TextureImageAwt(x, params)
				}
			}
		}
	}

    protected def findPath(resource:String):String = {
    	var res  = resource
    	var file = new File(res)

        if(!file.exists) {
            val sep = sys.props.get("file.separator").get

            Texture.path.find(path => (new File("%s%s%s".format(path, sep, res))).exists) match {
                case path:Some[String] => { res = "%s%s%s".format(path.get, sep, res) }
                case None => { res = null }
            }
        }  

        res
    }
}


// -- Texture ----------------------------------------------------------------------------------------------


/** How to handle mip-maps when loading image data into the texture. */
object TexMipMap extends Enumeration {
	/** No mipmaps. */
	val No       = Value

	/** Automatically generate the mip-maps. */
	val Generate = Value
	
	/** Try to load the mip-maps from files. In this case the given resource
	  * name for the file is modified to append an underscore then an integer
	  * indicating the level of the mipmap. The first one must be 0 then
	  * progressing by 1 until no file is found. For example for resource
	  * "foo.png" the loader will look at "foo_0.png", then "foo_1.png",
	  * etc. until it cannot load a level. */
	val Load     = Value

	/** Enumeration type. */
	type TexMipMap = Value
}

/** How to setup the min filter for the texture when loading it. */
object TexMin extends Enumeration {
	/** The nearest texel. */
	val Nearest = Value
	
	/** Average of the four nearest texels. */
	val Linear = Value
	
	/** Nearest texel in the nearest mip-map. */
	val NearestAndMipMapNearest = Value
	
	/** Average of the four nearest texels in the nearest mip-map. */
	val LinearAndMipMapNearest = Value
	
	/** Average of the nearest texels of the two nearest mip-maps. */
	val NearestAndMipMapLinear = Value
	
	/** Average of the four nearest pixels from the two nearest mip-maps. */
	val LinearAndMipMapLinear = Value
	
	/** Enumeration type. */
	type TexMin = Value
}

/** How to setup the mag filter for the texture when loading it. */
object TexMag extends Enumeration {
	/** The nearest texel. */
	val Nearest = Value
	
	/** Average of the four nearest texels. */
	val Linear = Value
	
	/** Enumeration type. */
	type TexMag = Value
}

/** How to handle alpha valuers when loading the image. */
object TexAlpha extends Enumeration {
	/** Do nothing. */
	val Nop = Value

	/** Premultiply the values by the alpha. */
	val Premultiply = Value

	/** Enumeration type. */
	type TexAlpha = Value
}

object TexWrap extends Enumeration {
	/** No repetition. */
	val Clamp = Value 
	
	/** Repetition. */
	val Repeat = Value

	/** Repetition with mirroring. */
	val MirroredRepeat = Value

	/** Enumeration type. */
	type TexWrap = Value
}

/** Main parameters driving the way a texture is built. */
case class TexParams(
		val alpha:TexAlpha.Value   = TexAlpha.Nop,
		val minFilter:TexMin.Value = TexMin.Linear,
		val magFilter:TexMag.Value = TexMag.Linear,
		val mipMap:TexMipMap.Value = TexMipMap.No,
		val wrap:TexWrap.Value     = TexWrap.Repeat) {
}

/** Define a new 1D, 2D or 3D texture.
  * 
  * The binding of the texture is not automatic. You must bind it yourself before doing any
  * operation on it.
  *
  * The use of the default constructor will not upload any image data to the texture, it is only
  * declared and bindable. In this case the width and height are only indicative, and you have to
  * upload the image by yourself using glTexImage2D or using a [[TextureImage]].
  *
  * The use of the two other constructors will load an image or use a given [[TextureImage]]
  * according to given texture parameters. The parameters will describe if the image
  * is mip-mapped, if the mip-map must be automatically generated or loaded from several files, if
  * the alpha channel is premultiplied, how to wrap the texture, and how to magnify or minify it.
  */
class Texture(gl:SGL, val mode:Int, val width:Int, val height:Int, val depth:Int) extends OpenGLObject(gl) {
    import gl._

    init
    
    protected def init() {
        super.init(genTexture)
        bindTexture(mode, oid)
        checkErrors
    }
    
    def this(gl:SGL, image:TextureImage, params:TexParams) {
        this(gl, gl.TEXTURE_2D, image.width, image.height, 0)
        image.texImage2D(gl, mode)

        minFilter(params.minFilter)
       	magFilter(params.magFilter)
       	wrap(params.wrap)

    	checkErrors
    }

    def this(gl:SGL, imageFileName:String, params:TexParams) {
     	this(gl, Texture.loader.open(imageFileName,params), params)
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

    /** Specify the minification filter. */
    def minFilter(minFilter:TexMin.Value) {
    	import TexMin._
    	minFilter match {
    		case Nearest                 => parameter(gl.TEXTURE_MIN_FILTER, gl.NEAREST)
    		case Linear                  => parameter(gl.TEXTURE_MIN_FILTER, gl.LINEAR)
    		case NearestAndMipMapNearest => parameter(gl.TEXTURE_MIN_FILTER, gl.NEAREST_MIPMAP_NEAREST)
    		case LinearAndMipMapNearest  => parameter(gl.TEXTURE_MIN_FILTER, gl.LINEAR_MIPMAP_NEAREST)
    		case NearestAndMipMapLinear  => parameter(gl.TEXTURE_MIN_FILTER, gl.NEAREST_MIPMAP_LINEAR)
    		case LinearAndMipMapLinear   => parameter(gl.TEXTURE_MIN_FILTER, gl.LINEAR_MIPMAP_LINEAR)
    	}
        checkErrors
    }

    /** Specify the magnification filter. */
    def magFilter(magFilter:TexMag.Value) {
    	import TexMag._
    	magFilter match {
    		case Nearest => parameter(gl.TEXTURE_MAG_FILTER, gl.NEAREST)
    		case Linear  => parameter(gl.TEXTURE_MAG_FILTER, gl.LINEAR)
    	}
        checkErrors
    }

    /** Specify the minification and magnification filters. */
    def minMagFilter(min:TexMin.Value, mag:TexMag.Value) {
    	minFilter(min)
    	magFilter(mag)
    }

    /** Specify the wrapping behavior along S and T axes. */
    def wrap(value:Int) {
        parameter(gl.TEXTURE_WRAP_S, value)
        parameter(gl.TEXTURE_WRAP_T, value)
        checkErrors
    }

    /** Specify the wrapping behavior along S and T axes. */
    def wrap(value:TexWrap.Value) {
    	import TexWrap._
    	value match {
    		case Clamp          => { parameter(gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);   parameter(gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)   }
    		case Repeat         => { parameter(gl.TEXTURE_WRAP_S, gl.REPEAT);          parameter(gl.TEXTURE_WRAP_T, gl.REPEAT)          }
    		case MirroredRepeat => { parameter(gl.TEXTURE_WRAP_S, gl.MIRRORED_REPEAT); parameter(gl.TEXTURE_WRAP_T, gl.MIRRORED_REPEAT) }
    	}
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

    /** Bind the texture, specify to witch texture unit it binds, and set
      * the given uniform in the given shader to this texture unit. */
	def bindUniform(textureUnit:Int, shader:ShaderProgram, uniformName:String) {
		val pos = textureUnit match {
			case gl.TEXTURE0 => 0
			case gl.TEXTURE1 => 1
			case gl.TEXTURE2 => 2
			case _ => throw new RuntimeException("cannot handle more than 3 texture unit yet")
		}

		activeTexture(textureUnit)
		bindTexture(mode, oid)
		shader.uniform(uniformName, pos)
	}

	/** Ratio width over height. */
	def ratio:Double = width.toDouble / height.toDouble
}


// -- FrameBuffer -------------------------------------------------------------------------------------------------


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