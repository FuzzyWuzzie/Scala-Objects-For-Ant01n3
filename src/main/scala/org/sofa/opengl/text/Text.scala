package org.sofa.opengl.text

import org.sofa.Timer
import scala.collection.mutable.ArrayBuffer
import java.awt.{Font => AWTFont, Color => AWTColor, RenderingHints => AWTRenderingHints, Graphics2D => AWTGraphics2D}
import java.awt.image.BufferedImage
import java.io.{File, IOException, InputStream, FileInputStream}
import org.sofa.math.{Rgba,Matrix4}
import org.sofa.opengl.{SGL, Texture, TextureImageAwt, ShaderProgram, VertexArray, Camera}
import org.sofa.opengl.mesh.{TrianglesMesh, VertexAttribute}

object GLFont {
	/** First character Unicode. */
	val CharStart = 32
	
	/** Last character Unicode. */
	val CharEnd = 255
	
	/** Number of represented characters. */
	val CharCnt = (((CharEnd-CharStart)+1)+1)
	
	/** Character to use for unknown. */
	val CharNone = 32	// Must be in the range CharStart .. CharEnd
	
	/** Index of unknown character. */
	val CharUnknown = (CharCnt-1)

	/** Minimum font size (pixels). */
	val FontSizeMin = 6
	
	/** Maximum font size (pixels). */
	val FontSizeMax = 180
	
	/** Path to lookup for font files. */
	val path = new ArrayBuffer[String]()

	/** Loader for the font. */
	var loader:GLFontLoader = new GLFontLoaderAWT()
}

// TODO: This thing is actually only able to understand characters in a very limited range
// However we could imagine "blocks" that maps to unicode blocks and that are textures,
// rendered on demand, when needing some characters.
//
// Also we could get rid of the configuration by a static object ?

/** A font allowing to draw text in OpenGL.
  *
  * This code is derived and largely inspired by the implementation of Fractious:
  * http://fractiousg.blogspot.fr/2012/04/rendering-text-in-opengl-on-android.html
  */
class GLFont(val gl:SGL, file:String, val size:Int) {
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

	/** The full texture region. */
	var textureRgn:TextureRegion = null

	var textureMin:Int = gl.NEAREST

	var textureMag:Int = gl.LINEAR

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

	GLFont.loader.load(gl, file, size, this)

	/** Set the minification and magnification filters for the glyph
	  * texture. For 2D pixel perfect use, the NEAREST-LINEAR mode is better.
	  * For 3D use the LINEAR-LINEAR mode is better. */
	def minMagFilter(minFilter:Int, magFilter:Int) {
		textureMin = minFilter
		textureMag = magFilter
		texture.minMagFilter(textureMin, textureMag)
	}

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
}

trait GLFontLoader {
	def load(gl:SGL, file:String, size:Int, font:GLFont)
}

class GLFontLoaderAWT extends GLFontLoader {
	def load(gl:SGL, resource:String, size:Int, font:GLFont) {

		val padX = size * 0.5f	// Start drawing at this distance from the left border (for slanted fonts).

		font.isAlphaPremultiplied = false

		// Load the font.

		var theFont = loadFont(resource)
		var awtFont = theFont.deriveFont(AWTFont.PLAIN, size.toFloat)

		// Java2D forces me to create an image before I have access to font metrics
		// However, I need font metrics to know the size of the image ... hum ...

		val w = size*1.4 		// factor to make some room.
		val h = size*1.4 		// idem
		val textureSize = math.sqrt((w*1.1) * (h*1.1) * GLFont.CharCnt).toInt
		
//		val image = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_BYTE_GRAY)
		val image = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_4BYTE_ABGR)	// <- Do not know yet why,
		val gfx   = image.getGraphics.asInstanceOf[AWTGraphics2D]								// but rendering is far better.

		gfx.setRenderingHints(java.awt.Toolkit.getDefaultToolkit.getDesktopProperty("awt.font.desktophints").asInstanceOf[java.util.Map[String,String]])

//		gfx.setColor(new AWTColor(0.2f, 0f, 0f, 1))
//		gfx.fillRect(0, 0, textureSize, textureSize)

		gfx.setFont(awtFont)
		gfx.setRenderingHint(AWTRenderingHints.KEY_TEXT_ANTIALIASING,
//		                     AWTRenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
 	 	 				     AWTRenderingHints.VALUE_TEXT_ANTIALIAS_ON)

		val metrics = gfx.getFontMetrics(awtFont)

		// Get Font metrics.

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

		// Render each of the character to the image.

		var x = padX
		var y = ((font.cellHeight - 1) - font.descent).toInt

		c = GLFont.CharStart

		gfx.setColor(AWTColor.white)

		while(c < GLFont.CharEnd) {
			gfx.drawString("%c".format(c), x, y)

			x += font.cellWidth

			if((x + font.cellWidth) >= textureSize) {
				x  = padX
				y += font.cellHeight
			}

			c += 1
		}

		// Generate a new texture.

		font.texture = new Texture(gl,new TextureImageAwt(image), false)
		font.texture.minMagFilter(gl.NEAREST, gl.LINEAR)
		font.texture.wrap(gl.CLAMP_TO_EDGE)

		// Setup the array of character texture regions.

		x = padX
		y = 0
		c = 0

		font.pad = font.charWidthMax * 0.1f

		assert(font.pad < padX)

		while(c < GLFont.CharCnt) {
			// We define the texture region with padding at left and at right since most of the characters
			// go outside of their advance (hence the fond.pad, also used in when drawing in GLString).
			font.charRgn(c) = new TextureRegion(textureSize, x-font.pad, y,
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
}

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
		this(x/texSize, y/texSize, (x+width)/texSize, (y+height)/texSize)
	}
}

/** A single string of text.
  * 
  * The string is stored as a vertex array of quads each one representing
  * a character.
  *
  * TODO: Right-to-Left, Top-to-Bottom advance.
  * TODO: Multi-line string.
  */
class GLString(val gl:SGL, val font:GLFont, val maxCharCnt:Int, var shader:ShaderProgram) {
	/** Mesh used to build the triangles of the batch. */
	protected val batchMesh = new TrianglesMesh(maxCharCnt*2)

	/** Vertex array of the triangles (by two to form a quad) for each character. */
	protected var batch:VertexArray = null

	/** Rendering color. */
	protected var color = Rgba.black

	/** Current triangle. */
	protected var t = 0

	/** Current point. */
	protected var p = 0

	/** Current x. */
	protected var x = 0f

	/** Current y. */
	protected var y = 0f

	init

	protected def init() {
		import VertexAttribute._

//		shader = ShaderProgram(gl, "text shader", "es2/text.vert.glsl", "es2/text.frag.glsl")
		batch  = batchMesh.newVertexArray(gl, shader, Vertex -> "position", TexCoord -> "texCoords")
	}

	/** Size of the string in pixels. */
	def advance:Float = x

	/** height of the string in pixels. */
	def height:Float = y

	def setColor(color:Rgba) {
		this.color = color
	}

	/** Start the definition of a new string. This must be called before any call to char(Char).
	  * When finished the end() method must be called. The string is always located at the
	  * origin (0,0,0) on the XY plane. The point at the origin is at the baseline of the text
	  * before the first character. */
	def begin() {
		p = 0
		x = 0f
		y = 0f
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
		val n = string.length
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

		addCharTriangles(rgn, width)

		x += width  	// Triangles may overlap.
	}

	/** End the definition of the new string. This can only be called if begin() has been called before. */
	def end() {
		batchMesh.updateVertexArray(gl, true, false, false, true)
	}

	def draw(camera:Camera) {
		val ff = gl.getInteger(gl.FRONT_FACE)
		gl.frontFace(gl.CCW)
		shader.use
		font.texture.bindTo(gl.TEXTURE0)
	    shader.uniform("texColor", 0)
	    shader.uniform("textColor", color)
	    camera.setUniformMVP(shader)
		batch.draw(batchMesh.drawAs, p)
		gl.bindTexture(gl.TEXTURE_2D, 0)	// Paranoia ?		
		gl.frontFace(ff)
	}

	/** Draw the string with the baseline at (0,0). Use translation of the current MVP. */
	def draw(mvp:Matrix4) {
		shader.use

		val src = gl.getInteger(gl.BLEND_SRC) 
		val dst = gl.getInteger(gl.BLEND_DST)

		if(font.isAlphaPremultiplied)
			 gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)
		else gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)

		font.texture.bindTo(gl.TEXTURE0)
	    shader.uniform("texColor", 0)
	    shader.uniform("textColor", color)
	    shader.uniformMatrix("MVP", mvp)
		batch.draw(batchMesh.drawAs, p)
		gl.bindTexture(gl.TEXTURE_2D, 0)	// Paranoia ?

		gl.blendFunc(src, dst)
	}

	/** Define two triangles for a character at current `x` position.
	  * The `x` position is a point at the baseline of the character just
	  * at the left of the start of the character. The character may extend before
	  * and after, above and under this position. */
	protected def addCharTriangles(rgn:TextureRegion, width:Float) {
		val W = width + font.pad*2 // rgn.width 		// Overall character drawing width
		val H = font.cellHeight // rgn.height 			// Overall character drawing height
		val X = x - font.pad                            // Real X start of drawing.
		val Y = y - font.descent                        // Real Y start of drawing.

		//  u1 --> u2
		//  
		//  v1
		//   |
		//   v
		//  v2

		//  6--5 3
		//  |2/ /|   ^
		//  |/ /1|   |
		//  4 1--2 >-+ CCW

		// Vertices

		batchMesh.setPoint(p,   X,   Y,   0)
		batchMesh.setPoint(p+1, X+W, Y,   0)
		batchMesh.setPoint(p+2, X+W, Y+H, 0)

		batchMesh.setPoint(p+3, X,   Y,   0)
		batchMesh.setPoint(p+4, X+W, Y+H, 0)
		batchMesh.setPoint(p+5, X,   Y+H, 0)

		// TexCoords

		batchMesh.setPointTexCoord(p,   rgn.u1, rgn.v2)
		batchMesh.setPointTexCoord(p+1, rgn.u2, rgn.v2)
		batchMesh.setPointTexCoord(p+2, rgn.u2, rgn.v1)

		batchMesh.setPointTexCoord(p+3, rgn.u1, rgn.v2)
		batchMesh.setPointTexCoord(p+4, rgn.u2, rgn.v1)
		batchMesh.setPointTexCoord(p+5, rgn.u1, rgn.v1)

		// Triangles

		batchMesh.setTriangle(t,   p,   p+1, p+2)
		batchMesh.setTriangle(t+1, p+3, p+4, p+5)

		// The TrianglesMesh supports color per vertice, would it be interesting
		// to allow to color individual characters in a string ?

		p += 6
		t += 2
	}
}