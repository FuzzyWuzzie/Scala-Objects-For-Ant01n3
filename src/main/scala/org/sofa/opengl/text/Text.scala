package org.sofa.opengl.text

import scala.collection.mutable.ArrayBuffer
import scala.math._

import java.awt.{Font => AWTFont, Color => AWTColor, RenderingHints => AWTRenderingHints, Graphics2D => AWTGraphics2D}
import java.awt.image.BufferedImage
import java.io.{File, IOException, InputStream, FileInputStream}

import org.sofa.Timer
import org.sofa.math.{Rgba,Matrix4}
import org.sofa.opengl.{SGL, Texture, ShaderProgram, VertexArray, Camera, Space, TexParams, TexAlpha, TexMin, TexMag, TexWrap, TexMipMap}
import org.sofa.opengl.backend.{TextureImageAwt}
import org.sofa.opengl.mesh.{TrianglesMesh, VertexAttribute}


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
  */
class GLFont(val gl:SGL, file:String, val size:Int, val shader:ShaderProgram, val isMipMapped:Boolean = false, rasterizeMipMaps:Boolean = true, optimizeFor3D:Boolean = false) {
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
	    shader.uniform("texColor", 0)
	}

	/** Pop the GL state and restore attributes affected by `beginRender`. */
	def endRender() {
		gl.bindTexture(gl.TEXTURE_2D, null)	// Paranoia ?				
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

	/** Create a string with a given maximul length of character, initialized with `text`.
	  * `text` length must be less or equal to `maxLength`. */
	def newString(text:String, maxLength:Int):GLString = new GLString(gl, this, maxLength, text)
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

		val w = size*1.4 		// factor to make some room.
		val h = size*1.4 		// idem
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
			g.setRenderingHint(AWTRenderingHints.KEY_TEXT_ANTIALIASING,
 	 	 					   AWTRenderingHints.VALUE_TEXT_ANTIALIAS_ON)
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
			TexParams(alpha=TexAlpha.Premultiply,minFilter=TexMin.Linear,magFilter=TexMag.Linear,wrap=TexWrap.Clamp,mipMap=TexMipMap.No)
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


// -- GLString ----------------------------------------------------------------------------------------------------------


/** GLString companion object, allows easy string creation. */
object GLString {
	def apply(gl:SGL, font:GLFont, maxCharCnt:Int):GLString = new GLString(gl, font, maxCharCnt)
	def apply(gl:SGL, font:GLFont, text:String):GLString = new GLString(gl, font, text)
	def apply(gl:SGL, font:GLFont, maxCharCnt:Int, text:String):GLString = new GLString(gl, font, maxCharCnt, text)
}


/** A single string of text.
  * 
  * The string is stored as a vertex array of quads each one representing
  * a character.
  *
  * TODO: Right-to-Left, Top-to-Bottom advance.
  * TODO: Multi-line string.
  */
class GLString(val gl:SGL, val font:GLFont, val maxCharCnt:Int) {
	/** Mesh used to build the quads of the batch. */
	//protected[this] val batchMesh = new QuadsMesh(maxCharCnt)
	protected[this] val batchMesh = new TrianglesMesh(maxCharCnt*2)
	// Cannot use triangle strips, since chars can overlap (kerning).

	/** Rendering color. */
	protected[this] var color = Rgba.Black

	/** Current triangle. */
	protected[this] var t = 0

	/** Current point. */
	protected[this] var p = 0

	/** Current x. */
	protected[this] var x = 0f

	/** Current y. */
	protected[this] var y = 0f

	/** Length of string. */
	protected[this] var l = 0

	/** Build a GLString from a string. */
	def this(gl:SGL, font:GLFont, text:String) {
		this(gl, font, text.length)
		build(text)
	}

	/** Build a GLString from a string, but allow to reserver more space in characters for future reuse. */
	def this(gl:SGL, font:GLFont, maxLength:Int, text:String) {
		this(gl, font, maxLength)
		if(text.length < maxLength)
			build(text)
	}

	init

	protected def init() {
		import VertexAttribute._
		batchMesh.newVertexArray(gl, font.shader, Vertex -> "position", TexCoord -> "texCoords")
	}

	/** Release the resources of this string, the string is no more usable after this. */
	def dispose() { batchMesh.dispose }

	/** Length of the string in characters. */
	def length:Int = l

	/** Maximum number of characters composable in the string. */
	def maxLength:Int = maxCharCnt

	/** Width of the string in pixels. */
	def advance:Float = x

	/** height of the string in pixels. */
	def height:Float = y

	/** Change the color of the string. */
	def setColor(color:Rgba) { this.color = color }

	/** Start the definition of a new string. This must be called before any call to char(Char).
	  * When finished the end() method must be called. The string is always located at the
	  * origin (0,0,0) on the XY plane. The point at the origin is at the baseline of the text
	  * before the first character. */
	def begin() {
		p = 0
		t = 0
		x = 0f
		y = 0f
		l = 0
	}

	/** Same as calling begin(), a code that uses char(), then end(), you
	  * only pass the code that must call char() one or more time to build
	  * the string. */
	def build(stringBuilder: => Unit) {
		begin
		stringBuilder
		end
	}

	/** Same as calling begin(), char() on each character of the string, then end(). */
	def build(string:String) {
		begin
		var i = 0
		val n = min(string.length, maxCharCnt)
		while(i < n) {
			char(string.charAt(i))
			i += 1
		}
		end
	}

	/** Add a character in the string. This can only be called after a call to begin() and before a call to end(). */
	def char(c:Char) {
		val width = font.charWidth(c)
		val rgn   = font.charTextureRegion(c)

		addCharQuad(rgn, width)

		x += width  	// Triangles may overlap.
		l += 1
	}

	/** End the definition of the new string. This can only be called if begin() has been called before. */
	def end() {
		batchMesh.updateVertexArray(gl, updateVertices=true, updateTexCoords=true)
	}


	def render(camera:Camera) {
		var clr = if(font.isAlphaPremultiplied) color.alphaPremultiplied else color

	    font.shader.uniform("textColor", clr)
	    camera.uniformMVP(font.shader)
		batchMesh.lastVertexArray.draw(batchMesh.drawAs, t*3)		
	}

	def render(mvp:Matrix4) {
		var clr = if(font.isAlphaPremultiplied) color.alphaPremultiplied else color

	    font.shader.uniform("textColor", clr)
		font.shader.uniformMatrix("MVP", mvp)
		batchMesh.lastVertexArray.draw(batchMesh.drawAs, t*3)	
	}

	def render(space:Space) {
		var clr = if(font.isAlphaPremultiplied) color.alphaPremultiplied else color

	    font.shader.uniform("textColor", clr)
		space.uniformMVP(font.shader)
		batchMesh.lastVertexArray.draw(batchMesh.drawAs, t*3)
	}

	/** Draw the string with the baseline at (0,0). Use the current translation of the camera.
	  * This in fact calls `GLFont.beginRender`, `render` and finally `GLFont.endRender`. */
	def draw(camera:Camera) {
		font.beginRender
		render(camera)
		font.endRender
	}

	/** Draw the string with the baseline at (0,0). Use current translation of the MVP.
	  * This in fact calls `GLFont.beginRender`, `render` and finally `GLFont.endRender`. */
	def draw(mvp:Matrix4) {
		font.beginRender
		render(mvp)
		font.endRender
	}

	/** Draw the string with the baseline at (0, 0). Use current translation of the space.
	  * This in fact calls `GLFont.beginRender`, `render` and finally `GLFont.endRender`. */
	def draw(space:Space) {
		font.beginRender
		render(space)
		font.endRender
	}

	/** Define a quad for a character at current `x` position.
	  * The `x` position is a point at the baseline of the character just
	  * at the left of the start of the character. The character may extend before
	  * and after, above and under this position. */
	protected def addCharQuad(rgn:TextureRegion, width:Float) {
		if(t/2 < maxCharCnt) {
			val W = width + font.pad * 2 // rgn.width 		// Overall character drawing width
			val H = font.cellHeight      // rgn.height 		// Overall character drawing height
			val X = x - font.pad                            // Real X start of drawing.
			val Y = y - font.descent                        // Real Y start of drawing.

			//   u1 ---> u2
			//
			// v1   3--2     ^
			//  |   | /|     |
			//  v   |/ |     |
			// v2   0--1   >-+ CCW

			// Vertices

			batchMesh.setPoint(p,   X,   Y,   0)
			batchMesh.setPoint(p+1, X+W, Y,   0)
			batchMesh.setPoint(p+2, X+W, Y+H, 0)
			batchMesh.setPoint(p+3, X,   Y+H, 0)

			// TexCoords

			batchMesh.setPointTexCoord(p,   rgn.u1, rgn.v2)
			batchMesh.setPointTexCoord(p+1, rgn.u2, rgn.v2)
			batchMesh.setPointTexCoord(p+2, rgn.u2, rgn.v1)
			batchMesh.setPointTexCoord(p+3, rgn.u1, rgn.v1)

			// The triangles

			batchMesh.setTriangle(t,   p, p+1, p+2)
			batchMesh.setTriangle(t+1, p, p+2, p+3)

			// The TrianglesMesh supports color per vertice, would it be interesting
			// to allow to color individual characters in a string ? However string
			// would not be reusable for color.

			p += 4
			t += 2
		}
	}
}