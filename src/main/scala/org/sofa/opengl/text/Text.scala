package org.sofa.opengl.text

import org.sofa.Timer
import scala.collection.mutable.ArrayBuffer
import java.awt.{Font => AWTFont, Color => AWTColor, RenderingHints => AWTRenderingHints, Graphics2D => AWTGraphics2D}
import java.awt.image.BufferedImage
import java.io.{File, IOException, InputStream, FileInputStream}
import org.sofa.opengl.{SGL, Texture, TextureImageAwt}


object GLFont {
	/** First character Unicode. */
	val CharStart = 32
	
	/** Last character Unicode. */
	val CharEnd = 255
	
	/** Number of represented characters. */
	val CharCnt = (((CharEnd-CharStart)+1)+1)
	
	/** Character to use for unknown. */
	val CharNone = 32
	
	/** Index of unknown character. */
	val CharUnknown = (CharCnt-1)
	
	/** Minimum font size (pixels). */
	val FontSizeMin = 6
	
	/** Maximum font size (pixels). */
	val FontSizeMax = 180
	
	/** Number of characters to render per batch. */
	val CharBatchSize = 100

	/** Path to lookup for font files. */
	val path = new ArrayBuffer[String]()

	/** Loader for the font. */
	var loader:GLFontLoader = new GLFontLoaderAWT()
}

class GLFont(val gl:SGL, file:String, val size:Int, padX:Int, padY:Int) {
	/** Font X padding (pixels, on each side, doubled on X axis) */
	var fontPadX = 0

	/** Font Y padding (pixels, on each side, doubled on Y axis). */
	var fontPadY = 0

	/** Font height (actual, pixels). */
	var fontHeight = 0f

	/** Font ascent (above baseline, pixels). */
	var fontAscent = 0f

	/** Font descent (below baseline, pixels). */
	var fontDescent = 0f

	//---------------------

	/** The texture with each glyph. */
	var texture:Texture = null

	/** The full texture region. */
	var textureRgn:TextureRegion = null

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

	/** Font scale along X. */
	var scaleX = 1f

	/** Font scale along Y. */
	var scaleY = 1f

	/** Additional X spacing (unscaled). */
	var spaceX = 0f
	
	GLFont.loader.load(gl, file, size, padX, padY, this)
}

trait GLFontLoader {
	def load(gl:SGL, file:String, size:Int, padX:Int, padY:Int, font:GLFont)
}

class GLFontLoaderAWT extends GLFontLoader {
	def load(gl:SGL, resource:String, size:Int, padX:Int, padY:Int, font:GLFont) {
val timer = new Timer(Console.out)
timer.measure("load fond %s".format(resource)) {
		font.fontPadX = padX
		font.fontPadY = padY

		// Load the font.

		var theFont = loadFont(resource)
		var awtFont = theFont.deriveFont(AWTFont.PLAIN, size.toFloat)

		// Java2D forces me to create an image before I have access to font metrics
		// However, I need font metrics to know the size of the image ... hum ... interesting.

		val w = size*1.2 + 2*padX       // 1.2 factor to make some room.
		val h = size*1.2 + 2*padY 		// idem
		val textureSize = math.sqrt(w * h * GLFont.CharCnt).toInt

println("size=%d textureSize=%d".format(size, textureSize))
		
		val image = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_BYTE_GRAY);
		val gfx   = image.getGraphics.asInstanceOf[AWTGraphics2D]

		gfx.setFont(awtFont)
		gfx.setRenderingHint(AWTRenderingHints.KEY_TEXT_ANTIALIASING,
 	 	 				     AWTRenderingHints.VALUE_TEXT_ANTIALIAS_ON)

		val metrics = gfx.getFontMetrics(awtFont)

		// Get Font metrics.

		font.fontHeight  = metrics.getHeight
		font.fontAscent  = metrics.getAscent
		font.fontDescent = metrics.getDescent

		// Determine the width of each character (including unnown character)
		// also determine the maximum character width.

		font.charWidthMax = 0
		font.charHeight   = font.fontHeight

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

		// Font the maximum size, validate, and setup cell sizes.

		font.cellWidth  = (font.charWidthMax + 2 * font.fontPadX).toInt
		font.cellHeight = (font.charHeight + 2 * font.fontPadY).toInt

		val maxSize = if(font.cellWidth > font.cellHeight) font.cellWidth else font.cellHeight

		if(maxSize < GLFont.FontSizeMin)
			throw new RuntimeException("Cannot create a font this small (%d<%d)".format(maxSize, GLFont.FontSizeMin))
		if(maxSize > GLFont.FontSizeMax)
			throw new RuntimeException("Cannot create a font this large (%d>%d)".format(maxSize, GLFont.FontSizeMax))

		// Calculate number of rows / columns.

		font.colCnt = textureSize / font.cellWidth
		font.rowCnt = (math.ceil(GLFont.CharCnt.toFloat / font.colCnt.toFloat)).toInt

		// Render each of the character to the image.

		var x = font.fontPadX
		var y = ((font.cellHeight - 1) - font.fontDescent - font.fontPadY).toInt

		c = GLFont.CharStart

		gfx.setColor(AWTColor.white)

		while(c < GLFont.CharEnd) {
			gfx.drawString("%c".format(c), x, y)
//			if((x+font.cellWidth-font.fontPadX) > textureSize) {

			x += font.cellWidth

			if((x+font.cellWidth) >= textureSize) {
				x  = font.fontPadX
				y += font.cellHeight
			}

			c += 1
		}

		// Generate a new texture.

		font.texture = new Texture(gl,new TextureImageAwt(image), false)
		font.texture.minMagFilter(gl.NEAREST, gl.LINEAR)
		font.texture.wrap(gl.CLAMP_TO_EDGE)

		// Setup the array of character texture regions.

		x = 0
		y = 0
		c = 0

		while(c < GLFont.CharCnt) {
			font.charRgn(c) = new TextureRegion(textureSize, textureSize, x, y, font.cellWidth-1, font.cellHeight-1)
			if(x+font.cellWidth > textureSize) {
				x  = 0
				y += font.cellHeight
			} else {
				x += font.cellWidth
			}

			c += 1
		}
}
timer.printAvgs("-- Load Font %s --".format(resource))
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
}

/** Region in a texture that identify a character.
  *
  * @param u1 left
  * @param v1 top
  * @param u2 right
  * @param v2 bottom */
class TextureRegion(val u1:Float, val v1:Float, val u2:Float, val v2:Float) {

	/** Calculate U,V coordinate from specified texture coordinates.
	  * @param texWidth The width of the texture region
	  * @param texHeight The height of the texture region
	  * @param x The left of the texture region in pixels
	  * @param y The top of the texture region in pixels.
	  * @param width The width of the texture region in pixels.
	  * @param height The height of the texture region in pixels. */
	def this(texWidth:Float, texHeight:Float, x:Float, y:Float, width:Float, height:Float) {
		this((x/texWidth), (y/texHeight), ((x/texWidth) + (width/texWidth)), ((y/texHeight) + (height/texHeight)))
	}
}