package org.sofa.gfx

import org.sofa.nio._
import java.io.{File, IOException}
import scala.collection.mutable.{ArrayBuffer=>ScalaArrayBuffer}
import org.sofa.gfx.backend.{TextureImageAwt, TextureImageTEX}
import org.sofa.Loader


/** The image formats supported by the texture system actually. */
object ImageFormat extends Enumeration {
	/** Red-green-blue-alpha (in this order) packed in a 32bits integer. */
    val RGBA_8888 = Value
    /** Alpha in a byte. */
    val A_8 = Value
    /** The enumeration type. */
    type ImageFormat = Value
}


// -- Texture Image --------------------------------------------------------------------------


/** The image data used for the texture.
  *
  * This is an abstract class that represents image data to be transfered into a texture.
  * Most of the time it can be released once the data has been given to OpenGL. Its main
  * method is texImage2D() that will transfer its data to OpenGL. It can be used by multiple
  * texture instances.
  *
  * Image data can represent one or more images. For example, when the texture is a mip-map,
  * the texture image will in fact contain the whole image pyramid. In this case, the width
  * and the height returned are the one of the largest image.
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


/** Thrown when a texture cannot be loaded. */
class TextureIOException(msg:String) extends Exception(msg)


// -- Texture Loader --------------------------------------------------------------------------


/** Locate and fetch image data. */
trait TextureLoader extends Loader {
    /** Try to locate a resource in the include path and load it.
      *
      * The params may indicate how to load the resource.
      *
      * The texture parameters `params` allow to know how to load the
      * texture and which paramters to set. Notably it allows to load or
      * generate mip-maps easily. When mip-maps are loaded the resource name is
      * used as a basis, but integers must be appended to the real name of the
      * resource separated from it by an underscore. The first number must be 0
      * and is the most accurate level. For example if the resource name is
      * "foo.png" the loader will look for "foo_0.png" then for "foo_1.png"
      * etc. until it cannot find the next level.
      *
      * The alpha texture parameter allows to know if the data loaded must be
      * converted to premultiplied alpha (all loaders must be able to do this).
      *
      * @param params The set of texture paramters indicating how to load texture
      *               image or set of images and eventually convert it. */
    def open(resource:String, params:TexParams):TextureImage
}


/** Default texture loader that uses files in the local file system
  * and [[backend.TextureImageAwt]]. */
class DefaultTextureLoader extends TextureLoader {
	def open(resource:String, params:TexParams):TextureImage = {
		import TexMipMap._

		val pos  = resource.lastIndexOf('.')
		val res  = if(pos>0) resource.substring(0, pos) else resource
    	val ext  = if(pos>0) resource.substring(pos+1, resource.length) else ""
		var name = params.mipMap match {
			case Load => "%s_0.%s".format(res, ext)
			case _    => resource
		}

		findPath(name, Texture.path) match {
			case null     => throw new IOException("cannot locate texture %s (path %s)".format(name, Texture.path.mkString(":")))
			case x:String => if(ext == "tex") 
							      new TextureImageTEX(x, params)
							 else new TextureImageAwt(x, params)
		}
	}
}


// -- Texture ----------------------------------------------------------------------------------------------


trait TexParam


/** How to handle mip-maps when loading image data into the texture. */
object TexMipMap extends Enumeration with TexParam {
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

	/** Convert a string to the corresponding value. The string must match
	  * the value name (but case is ignored). */
	def fromString(value:String):TexMipMap = {
		if(value ne null)
			value.toLowerCase match {
				case "generate" => Generate
				case "load" => Load
				case _ => No
			}
		else No
	}
}


/** How to setup the min filter for the texture when loading it. */
object TexMin extends Enumeration with TexParam {
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

	/** Convert a string to the corresponding value. The string must match
	  * the value name (but case is ignored). */
	def fromString(value:String):TexMin = {
		if(value ne null)
			value.toLowerCase match {
				case "linear" => Linear
				case "nearestandmipmapnearest" => NearestAndMipMapNearest
				case "linearandmipmapnearest" => LinearAndMipMapNearest
				case "nearestandmipmaplinear" => NearestAndMipMapLinear
				case "linearandmipmaplinear" => LinearAndMipMapLinear
				case _ => Nearest
			}
		else Nearest
	}
}


/** How to setup the mag filter for the texture when loading it. */
object TexMag extends Enumeration with TexParam {
	/** The nearest texel. */
	val Nearest = Value
	
	/** Average of the four nearest texels. */
	val Linear = Value
	
	/** Enumeration type. */
	type TexMag = Value

	/** Convert a string to the corresponding value. The string must match
	  * the value name (but case is ignored). */
	def fromString(value:String):TexMag = {
		if(value ne null)
			value.toLowerCase match {
				case "linear" => Linear
				case _ => Nearest
			}
		else Nearest
	}
}


/** How to handle alpha valuers when loading the image. */
object TexAlpha extends Enumeration with TexParam {
	/** Do nothing. */
	val Nop = Value

	/** Premultiply the values by the alpha. */
	val Premultiply = Value

	/** Enumeration type. */
	type TexAlpha = Value

	/** Convert a string to the corresponding value. The string must match
	  * the value name (but case is ignored). */
	def fromString(value:String):TexAlpha = {
		if(value ne null)
			value.toLowerCase match {
				case "premultiply" => Premultiply
				case _ => Nop
			}
		else Nop
	}
}


object TexWrap extends Enumeration with TexParam {
	/** No repetition. */
	val Clamp = Value 
	
	/** Repetition. */
	val Repeat = Value

	/** Repetition with mirroring. */
	val MirroredRepeat = Value

	/** Enumeration type. */
	type TexWrap = Value

	/** Convert a string to the corresponding value. The string must match
	  * the value name (but case is ignored). */
	def fromString(value:String):TexWrap = {
		if(value ne null )
			value.toLowerCase match {
				case "clamp" => Clamp
				case "mirroredrepeat" => MirroredRepeat
				case _ => Repeat
			}
		else Repeat
	}
}


/** Main parameters driving the way a texture is built. */
case class TexParams(
		val alpha:TexAlpha.Value   = TexAlpha.Nop,
		val minFilter:TexMin.Value = TexMin.Linear,
		val magFilter:TexMag.Value = TexMag.Linear,
		val mipMap:TexMipMap.Value = TexMipMap.No,
		val wrap:TexWrap.Value     = TexWrap.Repeat) {
}


/** Companion object for Texture.
  *
  * This object allows to define a loader a thing that loads images and transform them
  * as usable textures, as well as an include path thatallows to find textures in
  * the current environment (depending on the loader). */
object Texture {
    /** Set of paths where texture images are to be searched. */
    var path = new ScalaArrayBuffer[String]()

    /** Loaded, dependant on the underlying system that allows to find, read
      * and transform an image into an input stream. */
    var loader:TextureLoader = new DefaultTextureLoader()

    /** Used to avoid re-binding the same texture again and again. Not thread-safe.
      * However we always use the same thread with OpenGL. */
    var currentlybound = new Array[Texture](10)

    def resetCurrentlyBound() {
    	var i = 0
    	val n = currentlybound.length
    	while(i < n) {
    		currentlybound(i) = null
    		i += 1
    	}
    }
}


/** Define a new 1D, 2D or 3D texture.
  * 
  * The binding of the texture is not automatic. You must bind it yourself before doing any
  * operation on it. The texture class handles a global field to avoid rebinding the same
  * texture again and again. This is why texture cannot be used in a multi-threaded context.
  *
  * The texture class provides several ways to be built, according to various uses:
  *   - If you just want to declare a texture, and handle its contents by yourself, you
  *     can use the default constructor. It will only declare and bind the texture with the
  *     indicative width and height given.
  *   - If you want to declare a texture and upload an image, use the constructors taking as
  *     argument a [[TextureImage]], this class is able to load several image formats and will
  *     upload them in thexture. [[TexParams]] will allow to setup texture parameters like
  *     filtering and often loading parameters (not directly tied to textures in OpenGL, like
  *     alpha premultiplication, or the creation of mip-maps).
  *   - If you want to create an empty texture for later use, for example to create a buffer
  *     or frame buffer for off-screen rendering, you can use the parmater describing the storage
  *     format, the storage type and optional mutlisampling parameters.
  */
class Texture(gl:SGL, val mode:Int, val width:Int, val height:Int, val depth:Int) extends OpenGLObject(gl) {
    import gl._

    init
    
    protected def init() {
        super.init(createTexture)
        //bindTexture(mode, oid)
        bind
        checkErrors
    }
    
    /** New texture created from an image.
      * @param imageFileName the image to upload to the texture.
      * @param params The texture parameters. */
    def this(gl:SGL, image:TextureImage, params:TexParams) {
        this(gl, gl.TEXTURE_2D, image.width, image.height, 0)
        image.texImage2D(gl, mode)

        minFilter(params.minFilter)
       	magFilter(params.magFilter)
       	wrap(params.wrap)

    	checkErrors
    }

    /** New texture created from an image filename.
      * @param imageFileName the image to load.
      * @param params The texture parameters. */
    def this(gl:SGL, imageFileName:String, params:TexParams) {
     	this(gl, Texture.loader.open(imageFileName,params), params)
    }

    /** New texture without image data.
      * The texture is created "empty" with uninitialized pixels.
      * @param format gl.RGBA or gl.DEPTH_COMPONENT for example.
      * @param ttype Type of storage gl.UNSIGNED_BYTE or gl.UNSIGNED_SHORT for example.
      * @param width The texture width in pixels.
      * @param height The texture height in pixels.
      * @param params the Texture parameters.
      * @param multiSample if greater than 1, and multisampling is supported for textures,
      *                    `glTexImage2DMultisample` is used instead of `glTexImage2D`. In
      *                    this case the format MUST be `GL_RGBA8`. */
    def this(gl:SGL, format:Int, ttype:Int, width:Int, height:Int, params:TexParams, multiSample:Int=1) {
    	this(gl, if(multiSample > 1 && gl.hasTexImage2DMultisample) gl.TEXTURE_2D_MULTISAMPLE else gl.TEXTURE_2D, width, height, 0) 

    	if(multiSample > 1 && gl.hasTexImage2DMultisample) {
    		texImage2DMultisample(gl.TEXTURE_2D_MULTISAMPLE, multiSample, format, width, height, false)

    		// No filtering for multisample textures.
    	} else {
    		texImage2D(gl.TEXTURE_2D, 0, format, width, height, 0, format, ttype, null)

	    	minFilter(params.minFilter)
    		magFilter(params.magFilter)
    		wrap(params.wrap)
    	}

    	checkErrors
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
    	//if(Texture.currentlybound(0) eq this) {
        	bindTexture(mode, oid)
        //	Texture.currentlybound(0) = this
        //}
    }
    
    /** Both bind the texture and specify to which texture unit it binds. */
    def bindTo(textureUnit:Int) {
    	// // Yet heavy, but faster.

    	// val i = textureUnit match {
    	// 	case gl.TEXTURE0 => 1
    	// 	case gl.TEXTURE1 => 2
    	// 	case gl.TEXTURE2 => 3
    	// 	case _ => throw new RuntimeException("cannot handle more than 3 texture unit yet")
    	// }

    	// if(Texture.currentlybound(i) eq this) {
			activeTexture(textureUnit)
        	bindTexture(mode, oid)	
        // 	Texture.currentlybound(i) = this
        // }	
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