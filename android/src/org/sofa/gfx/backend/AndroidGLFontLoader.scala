package org.sofa.gfx.backend

import org.sofa.Timer
import org.sofa.gfx.{SGL, Texture, TexParams}
import org.sofa.backend.AndroidLoader
import android.content.res.Resources
import android.graphics.{Bitmap, Canvas, Paint, Typeface, Matrix}
import org.sofa.gfx.text.{GLFont, GLFontLoader, TextureRegion}

import scala.math._

/** Font loader for Android using the bitmap and canvas method to retrieve the font texture. */
class AndroidGLFontLoader(val resources:Resources) extends GLFontLoader with AndroidLoader {
	def load(gl:SGL, resource:String, size:Int, font:GLFont, mipmaps:Boolean, rasterizeMipMaps:Boolean, optimizeFor3D:Boolean) {
	//def load(gl:SGL, resource:String, size:Int, font:GLFont) {
		val padX = size * 0.5f	// Start drawing at this distance from the left border (for slanted fonts).

		font.isAlphaPremultiplied = false

		// Load the font.

		var theFont = loadFont(resource)
		var paint   = new Paint()

		paint.setAntiAlias(true)
		paint.setSubpixelText(true)
		paint.setTextSize(size.toFloat)
		paint.setColor(0xFFFFFFFF)
		paint.setTypeface(theFont)

		val metrics = paint.getFontMetrics

		// Get Font metrics.

		font.height  = abs(metrics.bottom) + abs(metrics.top)
		font.ascent  = abs(metrics.ascent)
		font.descent = abs(metrics.descent)

		// Determine the width of each character (including unnown character)
		// also determine the maximum character width.

		font.charWidthMax = 0
		font.charHeight   = font.height

		var cnt = 0
		var c = GLFont.CharStart 
		var C = new Array[Char](1)
		var W = new Array[Float](1)
		
		while(c < GLFont.CharEnd) {
			C(0) = c.toChar
			paint.getTextWidths(C, 0, 1, W)
			if(W(0) > font.charWidthMax)
				font.charWidthMax = W(0)
			font.charWidths(cnt) = W(0)
			cnt += 1
			c += 1
		}

		// Create the bitmap

		val w = size*1.3		// 1.3 factor to make some room.
		val h = size*1.3 		// idem
		var textureSize = math.sqrt((w*1.1) * (h*1.1) * GLFont.CharCnt).toInt
		textureSize += (4 - (textureSize % 4)) % 4	// avoid unpack alignment problems due to 8 bit bitmap ;-)
//		val bitmap = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ALPHA_8)
		val bitmap = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ARGB_8888)
      	val image  = new Canvas(bitmap)
      	
      	bitmap.eraseColor(0x00000000)			// Set Transparent Background (ARGB)

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

		// Render each of the character to the image.

		var x = padX
		var y = ((font.cellHeight - 1) - font.descent).toInt

		c = GLFont.CharStart

		while(c < GLFont.CharEnd) {
			C(0) = c.toChar
			image.drawText(C, 0, 1, x, y, paint)

			x += font.cellWidth

			if((x + font.cellWidth) >= textureSize) {
				x  = padX
				y += font.cellHeight
			}

			c += 1
		}

		// Generate a new texture.

//		font.texture = new Texture(gl, new TextureImageAndroid(bitmap, TexParams()), TexParams())
		font.texture = new Texture(gl, new TextureImageAndroid(flipUpsideDown(bitmap), TexParams()), TexParams())
		font.texture.minMagFilter(gl.NEAREST, gl.LINEAR)
		font.texture.wrap(gl.CLAMP_TO_EDGE)

		// Setup the array of character texture regions.

		x = padX
		y = 0
		c = 0

		font.pad = font.charWidthMax * 0.1f

		while(c < GLFont.CharCnt) {
			font.charRgn(c) = new TextureRegion(textureSize, x-font.pad, textureSize-y,
								     font.charWidths(c)+font.pad*2, font.cellHeight)
			x += font.cellWidth

			if((x + font.cellWidth) >= textureSize) {
				x  = padX
				y += font.cellHeight
			}

			c += 1
		}
	}

	protected def flipUpsideDown(src:Bitmap):Bitmap = {
    	val matrix = new Matrix()

        matrix.preScale(1.0f, -1.0f)
 
    	Bitmap.createBitmap(src, 0, 0, src.getWidth, src.getHeight, matrix, true)
	}

	protected def loadFont(resource:String):Typeface = Typeface.createFromAsset(resources.getAssets, searchInAssets(resource, GLFont.path))
}