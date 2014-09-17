package org.sofa.gfx.text

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.math._

import java.awt.{Font => AWTFont, Color => AWTColor, RenderingHints => AWTRenderingHints, Graphics2D => AWTGraphics2D}
import java.awt.image.BufferedImage
import java.io.{File, IOException, InputStream, FileInputStream}

import org.sofa.Timer
import org.sofa.math.{Rgba,Matrix4}
import org.sofa.gfx.{SGL, Texture, ShaderProgram, VertexArray, Camera, Space, TexParams, TexAlpha, TexMin, TexMag, TexWrap, TexMipMap}
import org.sofa.gfx.backend.{TextureImageAwt}
import org.sofa.gfx.mesh.{TrianglesMesh, VertexAttribute}


object TextAlign extends Enumeration {
	final val Left = Value
	final val Right = Value
	final val Center = Value

	type TextAlign = Value
}

object VerticalAlign extends Enumeration {
	final val Baseline = Value
	final val Center = Value

	type VerticalAlign = Value
}


// TODO:
//
// 1. This thing is actually only able to understand characters in a very limited range
//    However we could imagine "blocks" that maps to unicode blocks and that are textures,
//    rendered on demand, when needing some characters.
//
// 2. Also we could get rid of the configuration by a static object ?
//
// 3. Allow a Text loader to read textures from bitmaps stored in images, instead of
//    rasterizing them. For games this could save time.


object GLFont {
	/** First character Unicode. */
	final val CharStart = 32
	
	/** Last character Unicode. */
	final val CharEnd = 255
	
	/** Number of represented characters. */
	final val CharCnt = (((CharEnd-CharStart)+1)+1)
	
	/** Character to use for unknown. */
	final val CharNone = 32	// Must be in the range CharStart .. CharEnd
	
	/** Index of unknown character. */
	final val CharUnknown = (CharCnt-1)

	/** Minimum font size (pixels). */
	final val FontSizeMin = 6
	
	/** Maximum font size (pixels). */
	final val FontSizeMax = 180
	
	/** Path to lookup for font files. */
	val path = new ArrayBuffer[String]()

	/** Loader for the font. */
	var loader:GLFontLoader = new GLFontLoaderAWT()
}


/** A set of the same type face fonts at various sizes.
  *
  * This object should handle both style, weight and sizes, but only size is handled actually. */
class GLTypeFace(val gl:SGL, val fileName:String, val shader:ShaderProgram) {

	/** The set of already allocated fonts. */
	protected val fonts = new HashMap[Int, GLFont]()

	/** Retrieve or allocate a font of this type face at a given `size` in points. */
	def font(size:Int):GLFont = fonts.get(size).getOrElse {
		val font = new GLFont(gl, fileName, size, shader, false, false, false)
		fonts += (size -> font)
		font
	}
}


/** A font allowing to draw text in OpenGL.
  *
  * The file indicate a file that appears in the font path. The size is the size 
  * in pixels of the font. use the `isMipMapped` parameter to try to enable mip-maps
  * for a font. This is typically useful when the font will be used in 3D or with
  * a 2D view where the pixels are not matched one to one (the text may be zoomed
  * for example). 
  *
  * If your text is rendered at a given pixel resolution, that never changes, the
  * better rendering is to use no mip-maps. If your text is rendered in 3D or with
  * a changing size or with any transformations, enable mip-maps for better quality.
  *
  * If you enable mip-maps, you can ask to try to rasterize the text for each mip-map
  * level (instead of rasterizing it once at the highest level an producing sub-levels
  * by resizing this image) with the option `rasterizeMipMaps`.
  * This will produce crispier text, but can introduce artifacts
  * between levels since the rasterizer will move some pixels (indeed in order to produce
  * crispier text). In this case if your text will always be in 2D, specify `optimizeFor3D`
  * to false (the default).
  *
  * This code is derived and largely inspired by the implementation of Fractious:
  * http://fractiousg.blogspot.fr/2012/04/rendering-text-in-opengl-on-android.html
  *
  * @param file The filename of the font.
  * @param size The size in points as used by the system.
  * @param shader The shader used to render text, used to allocate the vertex arrays.
  */
class GLFont(val gl:SGL, val file:String, val size:Int, val shader:ShaderProgram, val isMipMapped:Boolean = false, rasterizeMipMaps:Boolean = true, optimizeFor3D:Boolean = false) {
	/** Font height (actual, pixels). */
	var height = 0f

	/** Font ascent (above baseline, pixels). */
	var ascent = 0f

	/** Font descent (below baseline, pixels). */
	var descent = 0f

	/** Padding arround each character. */
	var pad = 0f

	//---------------------

	/** The texture with each glyph. */
	var texture:Texture = null

	//----------------------

	/** Maximal character width (pixels). */
	var charWidthMax = 0f

	/** Maximal character height (pixels). */
	var charHeight = 0f

	/** All character widths. */
	val charWidths = new Array[Float](GLFont.CharCnt)

	/** Texture regions for each character. */
	val charRgn = new Array[TextureRegion](GLFont.CharCnt)

	/** Character cell width (in the texture). */
	var cellWidth = 0

	/** Character cell height (in the texture). */
	var cellHeight = 0

	/** Number of rows in the texture. */
	var rowCnt = 0

	/** Number of columns in the texture. */
	var colCnt = 0

	/** True if the texture contains bitmaps with premultiplied alpha. */
	var isAlphaPremultiplied = false

	//--------------------------

	GLFont.loader.load(gl, file, size, this, isMipMapped, rasterizeMipMaps, optimizeFor3D)

	//--------------------------

	/** Width of the given char, if unknown, returns the default character width. */
	def charWidth(c:Char):Float = {
		if(c >= GLFont.CharStart && c <= GLFont.CharEnd) {
			charWidths(c.toInt - GLFont.CharStart)
		} else {
			charWidths(GLFont.CharNone)
		}
	}

	/** Texture region of the given char, if unknown, returns the default character texture region. */
	def charTextureRegion(c:Char):TextureRegion = {
		if(c >= GLFont.CharStart && c <= GLFont.CharEnd) {
			charRgn(c.toInt - GLFont.CharStart)
		} else {
			charRgn(GLFont.CharNone)
		}		
	}

	private[this] var ff:Int  = -1
	private[this] var src:Int = -1
	private[this] var dst:Int = -1
	private[this] var blend   = true
	private[this] var depth   = true

	/** Push the GL state to draw strings using this font. */
	def beginRender() {
		ff  = gl.getInteger(gl.FRONT_FACE)
		src = gl.getInteger(gl.BLEND_SRC) 
		dst = gl.getInteger(gl.BLEND_DST)
		blend = gl.isEnabled(gl.BLEND)
		depth = gl.isEnabled(gl.DEPTH_TEST)

		gl.enable(gl.BLEND)
		gl.disable(gl.DEPTH_TEST)
		gl.frontFace(gl.CCW)

		if(isAlphaPremultiplied) {
			 gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)
	 	} else {
	 		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
	 	}

		shader.use
		texture.bindTo(gl.TEXTURE0)
	    //shader.uniform("texColor", 0)
	}

	/** Pop the GL state and restore attributes affected by `beginRender`. */
	def endRender() {
		//gl.bindTexture(gl.TEXTURE_2D, null)	// Paranoia ?				
		gl.blendFunc(src, dst)
		gl.frontFace(ff)		

		if(!blend) gl.disable(gl.BLEND)
		if(depth) gl.enable(gl.DEPTH_TEST)

		src = -1
		dst = -1
		ff  = -1
	}

	/** Create a string with th given `text`. */
	def newString(text:String):GLString = new GLString(gl, this, text)

	/** Create an empty string, re-composable, with a given maximum length. */
	def newString(maxLength:Int):GLString = new GLString(gl, this, maxLength)

	/** Create a string with a given maximum length of character, initialized with `text`.
	  * `text` length must be less or equal to `maxLength`. */
	def newString(text:String, maxLength:Int):GLString = new GLString(gl, this, maxLength, text)

	/** Compute the width of the given string `s. */
	def stringWidth(s:String):Float = {
		val n = s.length
		var i = 0
		var w = 0f

		while(i < n) {
			w += charWidth(s.charAt(i))
			i += 1
		}

		w
	}
}


// -- GLFontLoader --------------------------------------------------------------------------------------------


/** Loader for fonts. */
trait GLFontLoader {
	/** Try to load the given font file at the given size.
	  *
	  * If `mipmaps` is true, try to generate mip-maps for the text. This is only useful 
	  * if you intent to use the font in 3D or in 2D and zoom on it. If you intent to work
	  * in 2D at a pixel perfect resolution, do not use mip-maps. In the other case, you
	  * can ask to try to render the font for each mip-map level (`rasterizeMipMaps`). In
	  * this case the rasterizer will re-render the font at each mip-map level to try to have
	  * the crispiest text possible. This can incur some rendering problems as the rasterizer
	  * will move some pixels in order to do that. So if you intent to view the text in 3D (no
	  * parallel to the screen) specify `optimizeFor3D` with true.
	  */
	def load(gl:SGL, file:String, size:Int, font:GLFont, mipmaps:Boolean = false, rasterizeMipMaps:Boolean = true, optimizeFor3D:Boolean = false)
}


// -- GLFontLoader AWT ----------------------------------------------------------------------------------------


/** A fond loader that rasterize the text using AWT and Java2D. */
class GLFontLoaderAWT extends GLFontLoader {

	def load(gl:SGL, resource:String, size:Int, font:GLFont, mipmaps:Boolean = false, rasterizeMipMaps:Boolean = true, optimizeFor3D:Boolean = false) {
		val padX = size * 0.5f	// Start drawing at this distance from the left border (for slanted fonts).

		font.isAlphaPremultiplied = true

		// Java2D forces me to create an image before I have access to font metrics
		// However, I need font metrics to know the size of the image ... hum ...

		val w = size * 1.4 		// factor to make some room.
		val h = size * 1.4 		// idem
		val textureSize = math.sqrt((w*1.1) * (h*1.1) * GLFont.CharCnt).toInt

		// If we use generated mipmaps, rasterizeMipMaps is false and we will
		// generate only one image. In this case all the remaining parts of
		// the rasterizer will follow and the mip-maps will be generated.
		
		val images = generateImages(textureSize, mipmaps && rasterizeMipMaps)
		val gfx    = images.map { img => img.getGraphics.asInstanceOf[AWTGraphics2D] }

		// Load the font.

		var theFont = loadFont(resource)
		var awtFont = generateFonts(images.size, size, theFont)

		// By default images are filled with (0,0,0,0) pixels, so alpha is correct
		// and the background is black as needed (we rendr text in white).

		val hints = java.awt.Toolkit.getDefaultToolkit.getDesktopProperty("awt.font.desktophints").asInstanceOf[java.util.Map[String,String]]
		var level = 0

		gfx.foreach { g =>
			g.setRenderingHints(hints)
			g.setFont(awtFont(level))

			g.setRenderingHint(AWTRenderingHints.KEY_ANTIALIASING, AWTRenderingHints.VALUE_ANTIALIAS_OFF)
			g.setRenderingHint(AWTRenderingHints.KEY_TEXT_ANTIALIASING, AWTRenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
//			g.setRenderingHint(AWTRenderingHints.KEY_TEXT_ANTIALIASING,
// 	 	 					   AWTRenderingHints.VALUE_TEXT_ANTIALIAS_ON)
			if(mipmaps)
				g.setRenderingHint(AWTRenderingHints.KEY_FRACTIONALMETRICS,			// Avoid repositionning glyphs on the pixel grid. 
								   AWTRenderingHints.VALUE_FRACTIONALMETRICS_ON)	// We will do this ourselves.
			level += 1
		}

		val metrics = gfx(0).getFontMetrics(awtFont(0))

		// Get Font metrics (from one gfx, others are the same).

		font.height  = metrics.getHeight
		font.ascent  = metrics.getAscent
		font.descent = metrics.getDescent

		// Determine the width of each character (including unnown character)
		// also determine the maximum character width.

		font.charWidthMax = 0
		font.charHeight   = font.height

		var c = GLFont.CharStart 
		var cnt = 0
		
		while(c < GLFont.CharEnd) {
			val w = metrics.charWidth(c)
			if(w > font.charWidthMax)
				font.charWidthMax = w
			font.charWidths(cnt) = w
			cnt += 1
			c += 1
		}

		// Find the maximum size, validate, and setup cell sizes.

		font.cellWidth  = w.toInt;//font.charWidthMax.toInt
		font.cellHeight = h.toInt;//font.charHeight.toInt

		val maxSize = if(font.cellWidth > font.cellHeight) font.cellWidth else font.cellHeight

		if(maxSize < GLFont.FontSizeMin)
			throw new RuntimeException("Cannot create a font this small (%d<%d)".format(maxSize, GLFont.FontSizeMin))
		if(maxSize > GLFont.FontSizeMax)
			throw new RuntimeException("Cannot create a font this large (%d>%d)".format(maxSize, GLFont.FontSizeMax))

		// Calculate number of rows / columns.

		font.colCnt = textureSize / font.cellWidth
		font.rowCnt = (math.ceil(GLFont.CharCnt.toFloat / font.colCnt.toFloat)).toInt

		// Render each of the character to the image(s).

		level = 1
		var i = 0

		images.foreach { img => renderImage(img, textureSize, gfx(i), font, padX, level); level *= 2; i += 1 }

		// Generate a new texture.

		val texParams = if(mipmaps) {
			if(rasterizeMipMaps) {
				if(optimizeFor3D)
					 TexParams(alpha=TexAlpha.Premultiply,minFilter=TexMin.LinearAndMipMapLinear, magFilter=TexMag.Linear,wrap=TexWrap.Clamp,mipMap=TexMipMap.Load)
				else TexParams(alpha=TexAlpha.Premultiply,minFilter=TexMin.LinearAndMipMapNearest,magFilter=TexMag.Linear,wrap=TexWrap.Clamp,mipMap=TexMipMap.Load)
			} else {
				if(optimizeFor3D)
				     TexParams(alpha=TexAlpha.Premultiply,minFilter=TexMin.LinearAndMipMapLinear, magFilter=TexMag.Linear,wrap=TexWrap.Clamp,mipMap=TexMipMap.Generate)
				else TexParams(alpha=TexAlpha.Premultiply,minFilter=TexMin.LinearAndMipMapNearest,magFilter=TexMag.Linear,wrap=TexWrap.Clamp,mipMap=TexMipMap.Generate)
			}
		} else {
			if(optimizeFor3D)
			     TexParams(alpha=TexAlpha.Premultiply,minFilter=TexMin.Linear,magFilter=TexMag.Linear,wrap=TexWrap.Clamp,mipMap=TexMipMap.No)
			else TexParams(alpha=TexAlpha.Premultiply,minFilter=TexMin.Nearest,magFilter=TexMag.Nearest,wrap=TexWrap.Clamp,mipMap=TexMipMap.No)
		}
		
		font.texture = new Texture(gl, new TextureImageAwt(images, texParams), texParams)

		// Setup the array of character texture regions.

		var x = padX
		var y = 0
		    c = 0

		font.pad = font.charWidthMax * 0.1f

		assert(font.pad < padX)

		while(c < GLFont.CharCnt) {
			// We define the texture region with padding at left and at right since most of the characters
			// go outside of their advance (hence the fond.pad, also used in when drawing in GLString).
			font.charRgn(c) = new TextureRegion(textureSize, x-font.pad, textureSize-y,
									font.charWidths(c)+font.pad*2, font.cellHeight)//, font.descent)
			x += font.cellWidth

			if((x + font.cellWidth) >= textureSize) {
				x  = padX
				y += font.cellHeight

				if(y > textureSize && c+1 < GLFont.CharCnt)
					throw new RuntimeException("out of texture bounds!")
			}

			c += 1
		}
	}

	protected def loadFont(resource:String):AWTFont = AWTFont.createFont(AWTFont.TRUETYPE_FONT, openFont(resource))

	protected def openFont(resource:String):InputStream = {
		var file = new File(resource)
        if(file.exists) {
            new FileInputStream(file)
        } else {
            val sep = sys.props.get("file.separator").get

            GLFont.path.find(p => (new File("%s%s%s".format(p, sep, resource))).exists) match {
                case p:Some[String] => {new FileInputStream(new File("%s%s%s".format(p.get,sep,resource))) }
                case None => { throw new IOException("cannot locate font %s".format(resource)) }
            }
        }
	}

	/** Either generate one image or several depending on the mipmap paramter. */
	protected def generateImages(textureSize:Int, mipmaps:Boolean):ArrayBuffer[BufferedImage] = {
		val images = new ArrayBuffer[BufferedImage]()
		var size   = textureSize

		if(mipmaps) {
			while(size > 0) {
				images += new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR) 
				size /= 2
			}
		} else {
			images += new BufferedImage(size, size, BufferedImage.TYPE_4BYTE_ABGR)
		}

		images
	}

	/** Generate fonts fro each mipmap level */
	protected def generateFonts(count:Int, size:Int, theFont:AWTFont):ArrayBuffer[AWTFont] = {
		val fonts = new ArrayBuffer[AWTFont]()
		var sz    = size

		for(level <- 1 to count) {
			fonts += theFont.deriveFont(AWTFont.PLAIN, sz.toFloat)
			sz    /= 2
		}

		fonts
	}

	protected def renderImage(image:BufferedImage, textureSize:Int, gfx:AWTGraphics2D, font:GLFont, padX:Double, div:Int) {
		// if size < 30 we stop rendering.
		// OGL ES requires a full mip map pyramid, but it has no meaning
		// to render text so small.

		var size = textureSize / div

		if(size > 30) {
			val cw   = font.cellWidth / div
			val ch   = font.cellHeight / div
			var x    = padX / div
			var y    = ((ch - 1) - (font.descent/div)).toInt
			var c    = GLFont.CharStart

			gfx.setColor(AWTColor.white)

			while(c < GLFont.CharEnd) {
				gfx.drawString("%c".format(c), x.toInt, y.toInt)

				x += cw

				if((x + cw) >= size) {
					x  = padX / div
					y += ch
				}

				c += 1
			}
		} else {
			gfx.setColor(new AWTColor(1.0f, 1.0f, 1.0f, 0.25f))
		    gfx.fillRect(0, 0, size, size)
			gfx.setColor(AWTColor.white)
		}
	}
}


// -- Texture Region ----------------------------------------------------------------------------------------------------


/** Region in a texture that identify a character.
  *
  * @param u1 left
  * @param v1 top
  * @param u2 right
  * @param v2 bottom */
class TextureRegion(val u1:Float, val v1:Float, val u2:Float, val v2:Float) {

	/** Calculate U,V coordinate from specified texture coordinates.
	  * @param texSize The width and height of the texture region.
	  * @param x The left of the texture region in pixels.
	  * @param y The top of the texture region in pixels.
	  * @param width The width of the texture region in pixels.
	  * @param height The height of the texture region in pixels. */
	def this(texSize:Float, x:Float, y:Float, width:Float, height:Float) {
		this(x/texSize, y/texSize, (x+width)/texSize, (y-height)/texSize)
	}
}