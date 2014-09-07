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


object GLText {

	protected[this] var currentFont:GLFont = null

	protected[this] var theShader:ShaderProgram = null

	private[this] var ff:Int  = -1
	private[this] var src:Int = -1
	private[this] var dst:Int = -1
	private[this] var blend   = true
	private[this] var depth   = true

	def shader(gl:SGL):ShaderProgram = {
		if(theShader eq null) {
			theShader = ShaderProgram(gl, "gltext-shader", "colortext.vert.glsl", "colortext.frag.glsl")
		}
		theShader
	}

	def beginRender(font:GLFont) {
		if(currentFont ne null)
			throw new RuntimeException("nested, beginRender(), call endRender() first")

		currentFont = font
		val gl = font.gl

		ff    = gl.getInteger(gl.FRONT_FACE)
		src   = gl.getInteger(gl.BLEND_SRC) 
		dst   = gl.getInteger(gl.BLEND_DST)
		blend = gl.isEnabled(gl.BLEND)
		depth = gl.isEnabled(gl.DEPTH_TEST)

		gl.enable(gl.BLEND)
		gl.disable(gl.DEPTH_TEST)
		gl.frontFace(gl.CCW)

		if(font.isAlphaPremultiplied) {
			 gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)
	 	} else {
	 		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
	 	}

		shader(gl).use
		font.texture.bindTo(gl.TEXTURE0)
	}

	def endRender() {
		if(currentFont eq null)
			throw new RuntimeException("endRender() whithout beginRender()")

		val gl = currentFont.gl

		gl.blendFunc(src, dst)
		gl.frontFace(ff)		

		if(!blend) gl.disable(gl.BLEND)
		if(depth) gl.enable(gl.DEPTH_TEST)

		src = -1
		dst = -1
		ff  = -1

		currentFont = null
	}
}


/** A set of glyphs at arbitrary positions (not only on a line) with colors.
  * 
  * At the contrary to a string that are a linear sequence of glyphes, all with
  * the same color, a text is a set of glyphs that can be positionned everywhere,
  * have each their own color.
  *
  * More formally here are the differences:
  *   - text is a vertex buffer object that both contains vertices and tex coords,
  *   - but also colors values for each vertex.
  *
  * This means you can change the color of each glyph independantly, but you cannot
  * reuse the text and change its whole color.
  *
  * Furthermore, at the contrary of
  * strings that are always at (0,0) and must be translated, it supports composition
  * operations to draw strings centered around a point or at left or right for
  * example.
  */
class GLText(val gl:SGL, val font:GLFont, val maxCharCnt:Int) {
	/** Mesh used to build the quads of the batch. */
	protected[this] val batchMesh = new TrianglesMesh(maxCharCnt * 2)
	// Cannot use triangle strips, since chars can overlap (kerning), and
	// may not follow each others.

	/** Current color, used for each following glyph. */
	protected[this] var color = Rgba.Black

	/** Current triangle. */
	protected[this] var t = 0

	/** Current point. */
	protected[this] var p = -1

	/** Current x. */
	protected[this] var x = 0f

	/** Current y. */
	protected[this] var y = 0f

	init

	protected def init() {
		import VertexAttribute._
		// set a vertex and a texcoord to declare the mesh has having these attributes.
		batchMesh v(0) xyz (0,0,0) uv (0,0) rgba(0,0,0,1)
		batchMesh.newVertexArray(gl, gl.DYNAMIC_DRAW, GLText.shader(gl),
			Vertex   -> "position",
			TexCoord -> "texCoords",
			Color    -> "color")
	}

	/** Release the resources of this string, the string is no more usable after this. */
	def dispose() { batchMesh.dispose }

	/** Maximum number of characters composable in the text. */
	def maxLength:Int = maxCharCnt

	/** height of a line of text in pixels. */
	def lineHeight:Float = font.charHeight

	/** Ascent of a glyph. */
	def ascent:Float = font.ascent

	/** Descent of a glyph. */
	def descent:Float = font.descent

	/** Render only this text, but do not setup the font before, you must have
	  * called `GLText.beginRender()` before and you must call `GLText.endRender()`
	  * after. This is used for bulk processing, when several texts of the same
	  * font have to be rendered. */
	def render(camera:Camera) {
	    camera.uniformMVP(GLText.shader(font.gl))
		batchMesh.lastVertexArray.draw(batchMesh.drawAs(gl), t*3)
	}

	/** Render only this string, but do not setup the font before, you must have
	  * called `font.beginRender()` before and you must call `font.endRender()`
	  * after. This is used for bulk processing, when several strings of the same
	  * font have to be rendered. */
	def render(mvp:Matrix4) {
		GLText.shader(font.gl).uniformMatrix("MVP", mvp)
		batchMesh.lastVertexArray.draw(batchMesh.drawAs(gl), t*3)	
	}

	/** Render only this string, but do not setup the font before, you must have
	  * called `font.beginRender()` before and you must call `font.endRender()`
	  * after. This is used for bulk processing, when several strings of the same
	  * font have to be rendered. */
	def render(space:Space) {
		space.uniformMVP(GLText.shader(font.gl))
		batchMesh.lastVertexArray.draw(batchMesh.drawAs(gl), t*3)
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
	}

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
		batchMesh.updateVertexArray(gl, updateVertices=true, updateColors=true, updateTexCoords=true)
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

			batchMesh v(p+0) xyz(X,   Y,   0) uv(rgn.u1, rgn.v2) rgba(color)
			batchMesh v(p+1) xyz(X+W, Y,   0) uv(rgn.u2, rgn.v2) rgba(color)
			batchMesh v(p+2) xyz(X+W, Y+H, 0) uv(rgn.u2, rgn.v1) rgba(color)
			batchMesh v(p+3) xyz(X,   Y+H, 0) uv(rgn.u1, rgn.v1) rgba(color)

			// The triangles

			batchMesh.setTriangle(t,   p, p+1, p+2)
			batchMesh.setTriangle(t+1, p, p+2, p+3)

			p += 4
			t += 2
		}
	}
}