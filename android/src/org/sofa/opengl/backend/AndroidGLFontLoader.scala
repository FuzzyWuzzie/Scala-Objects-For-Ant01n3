package org.sofa.opengl.backend

import org.sofa.opengl.{SGL, Texture}
import org.sofa.backend.AndroidLoader
import android.content.res.Resources
import android.graphics.{Bitmap, Canvas, Paint, Typeface}
import org.sofa.opengl.text.{GLFont, GLFontLoader, TextureRegion}

import scala.math._

class AndroidGLFontLoader(val resources:Resources) extends GLFontLoader with AndroidLoader {
	def load(gl:SGL, resource:String, size:Int, padX:Int, padY:Int, font:GLFont) {
		font.fontPadX = padX
		font.fontPadY = padY

		// Load the font.

		var theFont = loadFont(resource)
		var paint   = new Paint()

		paint.setAntiAlias(true)
		paint.setTextSize(size.toFloat)
		paint.setColor(0xFFFFFFFF)
		paint.setTypeface(theFont)

		val metrics = paint.getFontMetrics

		// Get Font metrics.

		font.fontHeight  = abs(metrics.bottom) + abs(metrics.top)
		font.fontAscent  = abs(metrics.ascent)
		font.fontDescent = abs(metrics.descent)

		// Determine the width of each character (including unnown character)
		// also determine the maximum character width.

		font.charWidthMax = 0
		font.charHeight   = font.fontHeight

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

		// Find the maximum size, validate, and setup cell sizes.

		font.cellWidth  = (font.charWidthMax + 2 * font.fontPadX).toInt
		font.cellHeight = (font.charHeight + 2 * font.fontPadY).toInt

		val maxSize = if(font.cellWidth > font.cellHeight) font.cellWidth else font.cellHeight

		if(maxSize < GLFont.FontSizeMin)
			throw new RuntimeException("Cannot create a font this small (%d<%d)".format(maxSize, GLFont.FontSizeMin))
		if(maxSize > GLFont.FontSizeMax)
			throw new RuntimeException("Cannot create a font this large (%d>%d)".format(maxSize, GLFont.FontSizeMax))

		// Create the bitmap

		val w = size*1.2 + 2*padX       // 1.2 factor to make some room.
		val h = size*1.2 + 2*padY 		// idem
		val textureSize = math.sqrt(w * h * GLFont.CharCnt).toInt
		val bitmap = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ALPHA_8)
      	val image  = new Canvas(bitmap)
      	
      	bitmap.eraseColor(0x00000000)			// Set Transparent Background (ARGB)

		// Calculate number of rows / columns.

		font.colCnt = textureSize / font.cellWidth
		font.rowCnt = (math.ceil(GLFont.CharCnt.toFloat / font.colCnt.toFloat)).toInt

		// Render each of the character to the image.

		var x = font.fontPadX
		var y = ((font.cellHeight - 1) - font.fontDescent - font.fontPadY).toInt

		c = GLFont.CharStart

		while(c < GLFont.CharEnd) {
			C(0) = c.toChar
			image.drawText(C, 0, 1, x, y, paint)

			x += font.cellWidth

			if((x + font.cellWidth - font.fontPadX) >= textureSize) {
				x  = font.fontPadX
				y += font.cellHeight
			}

			c += 1
		}

		// Generate a new texture.

		font.texture = new Texture(gl, new TextureImageAndroid(bitmap), false)
		font.texture.minMagFilter(gl.NEAREST, gl.LINEAR)
		font.texture.wrap(gl.CLAMP_TO_EDGE)

		// Setup the array of character texture regions.

		x = 0
		y = 0
		c = 0

		while(c < GLFont.CharCnt) {
			// Why we remove size/5 -> because the cell size is often too large for
			// italic text and some parts of the characters just aside will appear. Se we restrain
			// the area of the character. We do the same thing in GLString when drawing characters
			// to compensate.
			font.charRgn(c) = new TextureRegion(textureSize, textureSize, x, y, font.cellWidth-font.strangePad, font.cellHeight-font.strangePad)

			x += font.cellWidth

			if((x + font.cellWidth - font.fontPadX) >= textureSize) {
				x  = font.fontPadX
				y += font.cellHeight
			}

			c += 1
		}
	}

	protected def loadFont(resource:String):Typeface = Typeface.createFromAsset(resources.getAssets, searchInAssets(resource, GLFont.path))
}