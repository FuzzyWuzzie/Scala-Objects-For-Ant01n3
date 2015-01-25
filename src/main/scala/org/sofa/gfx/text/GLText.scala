package org.sofa.gfx.text

import scala.collection.mutable.ArrayBuffer
import scala.math._

import java.awt.{Font => AWTFont, Color => AWTColor, RenderingHints => AWTRenderingHints, Graphics2D => AWTGraphics2D}
import java.awt.image.BufferedImage
import java.io.{File, IOException, InputStream, FileInputStream}

import org.sofa.Timer
import org.sofa.math.{Rgba,Matrix4}
import org.sofa.gfx.{SGL, Texture, ShaderProgram, VertexArray, Camera, Space, TexParams, TexAlpha, TexMin, TexMag, TexWrap, TexMipMap}
import org.sofa.gfx.backend.{TextureImageAwt}
import org.sofa.gfx.mesh.{TrianglesMesh, VertexAttribute}


/** GLText companion object.
  *
  * Provides methods to make batch processing of several [[GLText]]
  * instances using the same font faster. */
object GLText {

	/** Allows to check wrongly nested calls to `beginRender` and `endRender`. */
	protected[this] var currentFont:GLFont = null

	/** When doing batch processing of several spans of text with the same font
	  * event if using several [[GLText]] instances, you can use this method to
	  * prepare rendering and therefore avoid to redo it. It binds the shader and
	  * texture, and setup OpenGL for rendering. The [[GLText]] class provides
	  * `render` methods that will not invoke `beginRender` nor `endRender`
	  * contrary to `draw` methods. When the rendering is finished, or to change
	  * font, you must call `endRender`. */
	def beginRender(font:GLFont) {
		if(currentFont ne null)
			throw new RuntimeException("nested, beginRender(), call endRender() first")

		currentFont = font
		val gl = font.gl

		gl.enable(gl.BLEND)
		gl.disable(gl.DEPTH_TEST)
		gl.frontFace(gl.CCW)

		if(font.isAlphaPremultiplied)
		     gl.blendFunc(gl.ONE,       gl.ONE_MINUS_SRC_ALPHA)
	 	else gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)

		font.shader.use
		font.texture.bindTo(gl.TEXTURE0)
	}

	/** Must be called after a rendering started with `beginRender`. */
	def endRender() {
		if(currentFont eq null)
			throw new RuntimeException("endRender() whithout beginRender()")

		currentFont = null
	}
}


/** A set of glyphs at arbitrary positions with colors.
  *
  * This allows to prepare and draw text, with postionning of sub-strings, and
  * changes of color. The text is not necesarilly on the same baseline, and
  * color can vary per-character.
  * 
  * At the contrary to a string that are a linear sequence of characters, all with
  * the same color, a text is a set of characters that can be positionned everywhere,
  * have each their own color.
  *
  * More formally here are the differences:
  *   - text is a vertex buffer object that both contains vertices and tex coords,
  *   - but also colors values for each vertex.
  *
  * This means you can change the color of each glyph independantly, but you cannot
  * reuse the text and change its whole color. This also means it does not use the
  * same shader as [[GLString]].
  *
  * Furthermore, at the contrary of
  * strings that are always at (0,0) and must be translated, it supports composition
  * operations to draw strings centered around a point or at left or right for
  * example.
  *
  * There are two parts in the GLText API, one for compositing the text, one for drawing
  * it.
  *
  * The drawing must usually be done in "pixel space". This means that each pixel of the
  * font texture will directly map to a pixel of the rendering space. However there are
  * commands to draw text in arbitrary space, although they involve more calculus and are
  * therefore a bit slower.
  */
class GLText(val gl:SGL, var font:GLFont, val maxCharCnt:Int) {

// Text storage

	/** Mesh used to build the quads of the batch. */
	protected val batchMesh = new TrianglesMesh(gl, maxCharCnt * 2)
	// Cannot use triangle strips, since chars can overlap (kerning), and
	// may not follow each others.

// Used during text composition.

	/** Current color, used for each following glyph. */
	protected var color = Rgba.Black

	/** Current triangle. */
	protected var t = 0

	/** Current point. */
	protected var p = -1

	/** Current x. */
	protected var x = 0f

	/** Current y. */
	protected var y = 0f

// Creation

	init

	protected def init() {
		import VertexAttribute._
		// set a vertex and a texcoord to declare the mesh has having these attributes.
		batchMesh.addAttributeTexCoord
		batchMesh.addAttributeColor
		batchMesh.modify() {
			batchMesh v(0) pos (0,0,0) tex (0,0) clr(0,0,0,1)
		}
		batchMesh.bindShader(font.shader,
			Position -> "position",
			TexCoord -> "texCoords",
			Color    -> "color")
	}

	/** Release the resources of this string, the string is no more usable after this. */
	def dispose() { batchMesh.dispose }

// Access

	/** Maximum number of characters composable in the text. */
	def maxLength:Int = maxCharCnt

	/** height of a line of text in pixels. */
	def lineHeight:Float = font.charHeight

	/** Ascent of a glyph. */
	def ascent:Float = font.ascent

	/** Descent of a glyph. */
	def descent:Float = font.descent

// Rendering in pixel space

	/** Render only this text, but do not setup the font before, you must have
	  * called `GLText.beginRender()` before and you must call `GLText.endRender()`
	  * after. This is used for bulk processing, when several texts of the same
	  * font have to be rendered. The `camera` space is considered to match pixels. */
	def render(camera:Camera) {
	    camera.uniformMVP(font.shader)
		batchMesh.vertexArray.draw(batchMesh.drawAs, t*3)
	}

	/** Render only this string, but do not setup the font before, you must have
	  * called `GLText.beginRender()` before and you must call `GLText.endRender()`
	  * after. This is used for bulk processing, when several strings of the same
	  * font have to be rendered. The `mvp` space is considered to match pixels. */
	def render(mvp:Matrix4) {
		font.shader.uniformMatrix("MVP", mvp)
		batchMesh.vertexArray.draw(batchMesh.drawAs, t*3)	
	}

	/** Render only this string, but do not setup the font before, you must have
	  * called `GLText.beginRender()` before and you must call `GLText.endRender()`
	  * after. This is used for bulk processing, when several strings of the same
	  * font have to be rendered. The `space` is considered to match pixels. */
	def render(space:Space) {
		space.uniformMVP(font.shader)
		batchMesh.vertexArray.draw(batchMesh.drawAs, t*3)
	}

	/** Draw the string with the baseline at (0,0). Use the current translation of the camera.
	  * This in fact calls `GLFont.beginRender`, `render` and finally `GLFont.endRender`. */
	def draw(camera:Camera) {
		GLText.beginRender(font)
		render(camera)
		GLText.endRender
	}

	/** Draw the string with the baseline at (0,0). Use current translation of the MVP.
	  * This in fact calls `GLFont.beginRender`, `render` and finally `GLFont.endRender`. */
	def draw(mvp:Matrix4) {
		GLText.beginRender(font)
		render(mvp)
		GLText.endRender
	}

	/** Draw the string with the baseline at (0, 0). Use current translation of the space.
	  * This in fact calls `GLFont.beginRender`, `render` and finally `GLFont.endRender`. */
	def draw(space:Space) {
		GLText.beginRender(font)
		render(space)
		GLText.endRender
	}

// Rendering in arbitrary space

	/** Render the text from an arbitrary position in an arbitrary space. The given position
	  * is projected in `pixelSpace` then this space is setup and `render` is called.
	  * This ensures the text will not be scalled or rotated by `space` but that the given
	  * position will match both in `space` and `pixelSpace`. If you render several strings, you should
	  * setup a special space once and render all text after. This method needs
	  * `GLText.beginRender()` to be called before and `GLText.endRender()`to be called
	  * after. */
	def renderAt(x:Double, y:Double, z:Double, space:Space, pixelSpace:Space) {
		gl.checkErrors
		val vp = space.viewport
		val p = org.sofa.gfx.dl.TextDL.tmppos
		p.set(x, y, z, 1)
		space.projectInPlace(p)
		p.perspectiveDivide
		gl.checkErrors
		val w = vp(0)
		val h = vp(1)
		p.set(p.x/2*w+w/2, p.y/2*h+h/2, 0, 1)
		gl.checkErrors
		pixelSpace.translate(p.x, p.y, 0)
		render(pixelSpace)
		pixelSpace.translate(-p.x, -p.y, 0)
		gl.checkErrors
	}

	/** Like `renderAt` but first call `GLText.beginRender`, then `renderAt` and
	  * finally `GLText.endRender`. */
	def drawAt(x:Double, y:Double, z:Double, space:Space, textSpace:Space) {
		GLText.beginRender(font)
		renderAt(x, y, z, space, textSpace)
		GLText.endRender
	}

// Text composition

	/** Start the definition of a new string. This must be called before any call to char(Char).
	  * When finished the end() method must be called. The string is located at
	  * (`xStart`, `yStart`, 0), on the XY plane. The point at the origin is at the baseline of
	  * the text before the first character. */
	def begin() {
		if(p >= 0)
			throw new RuntimeException("nested call to begin, call end() first")

		p = 0
		t = 0
		x = 0
		y = 0
		batchMesh.begin()
	}

	/** Like begin but also changes the font. The font can only be changed if they have the same shader. */
	def begin(font:GLFont) {
		if(font.shader ne this.font.shader) {
			throw new RuntimeException("the font can be changed only if they share the same shader")
		}
		this.font = font
		begin
	}

	/** Replace a call to `begin` / `end`. */
	def compose(code:(GLText)=>Unit) { begin; code(this); end }

	/** Replace a call to `begin(font)` / `end`. */
	def compose(font:GLFont)(code:(GLText)=>Unit) { begin(font); code(this); end }

	def chars(s:String):GLText = {
		var i = 0
		val n = min(s.length, maxCharCnt-(t/2))
		while(i < n) {
			char(s.charAt(i))
			i += 1
		}
		this
	}

	def chars(s:String, valign:TextAlign.Value, halign:VerticalAlign.Value=VerticalAlign.Baseline):GLText = {
		if(halign == VerticalAlign.Center)
			y -= (font.charHeight / 2 - font.descent)

		if(valign == TextAlign.Left)
			chars(s)
		else {
			val w = font.stringWidth(s)
			if(valign == TextAlign.Center)
				x -= w/2
			else if(valign == TextAlign.Right)
				x -= w
			else throw new RuntimeException("unhandled text align %s".format(valign))
			chars(s)
		}

		if(halign == VerticalAlign.Center)
			y += (font.charHeight / 2 - font.descent)

		this
	}

	def align(s:String, valign:TextAlign.Value, halign:VerticalAlign.Value)(code:(GLText)=>Unit):GLText = {
		if(halign == VerticalAlign.Center)
			y -= (font.charHeight / 2 - font.descent)
		
		if(valign == TextAlign.Left)
			code(this)
		else {
			val w = font.stringWidth(s)
			if(valign == TextAlign.Center)
				x -= w/2
			else if(valign == TextAlign.Right)
				x -= w
			else throw new RuntimeException("unhandled text align %s".format(valign))
			code(this)
		}

		if(halign == VerticalAlign.Center)
			y += (font.charHeight / 2 - font.descent)

		this				
	}

	/** Add a character in the text.
	  * The next character will be positionned just after this one, depending on the
	  * width of the character. You can use `moveAt()` or `move()` to translate to another
	  * position. The character will have the current color.
	  * This can only be called after a call to begin() and before a call to end(). */
	def char(c:Char):GLText = {
		if(p < 0)
			throw new RuntimeException("call to char() without begin()")

		val width = font.charWidth(c)
		val rgn   = font.charTextureRegion(c)

		addCharQuad(rgn, width)

		x += width

		this
	}

	/** The next call to `char()` will start at postion (`x`, `y`). */
	def moveAt(x:Float, y:Float):GLText = {
		this.x = x
		this.y = y
		this
	}

	/** The next call to `char()` will start at postion of the last char end with the
	  * (`dx`, `dy`) vector added. */
	def move(dx:Float, dy:Float):GLText = {
		x += dx
		y += dy
		this
	}

	/** Change the color for all subsequent glyphs. */
	def color(color:Rgba):GLText = {
		this.color = if(font.isAlphaPremultiplied) color.alphaPremultiplied else Rgba(color)
		this
	}

	/** End the definition of the new string. This can only be called if begin() has been called before. */
	def end() {
		if(p < 0)
			throw new RuntimeException("call to end() without begin()")
		batchMesh.end
		p = -1
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

			// Vertices & TexCoords

			batchMesh v(p+0) pos(X,   Y,   0) tex(rgn.u1, rgn.v2) clr(color)
			batchMesh v(p+1) pos(X+W, Y,   0) tex(rgn.u2, rgn.v2) clr(color)
			batchMesh v(p+2) pos(X+W, Y+H, 0) tex(rgn.u2, rgn.v1) clr(color)
			batchMesh v(p+3) pos(X,   Y+H, 0) tex(rgn.u1, rgn.v1) clr(color)

			// The triangles

			batchMesh.setTriangle(t,   p, p+1, p+2)
			batchMesh.setTriangle(t+1, p, p+2, p+3)

			p += 4
			t += 2
		}
	}
}