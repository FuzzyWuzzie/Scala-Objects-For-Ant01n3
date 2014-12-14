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
	protected[this] val batchMesh = new TrianglesMesh(maxCharCnt * 2)
	// Cannot use triangle strips, since chars can overlap (kerning).

	/** Rendering color. */
	protected[this] var color = Rgba.Black

	/** Current triangle. */
	protected[this] var t = 0

	/** Current point. */
	protected[this] var p = -1

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
		if(text.length <= maxLength)
			build(text)
	}

	init

	protected def init() {
		import VertexAttribute._
		// set a vertex and a texcoord to declare the mesh has having these attributes.
		batchMesh v(0) xyz (0,0,0) uv (0,0)
		batchMesh.newVertexArray(gl, gl.DYNAMIC_DRAW, font.shader, Vertex -> "position", TexCoord -> "texCoords")
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
	def height:Float = font.charHeight

	/** Ascent of the font. */
	def ascent:Float = font.ascent

	/** Descent of the font. */
	def descent:Float = font.descent

	/** Change the color of the string. */
	def setColor(color:Rgba) { this.color = color }
	
	/** Render only this string, but do not setup the font before, you must have
	  * called `font.beginRender()` before and you must call `font.endRender()`
	  * after. This is used for bulk processing, when several strings of the same
	  * font have to be rendered. */
	def render(camera:Camera) {
		var clr = if(font.isAlphaPremultiplied) color.alphaPremultiplied else color

	    font.shader.uniform("textColor", clr)
	    camera.uniformMVP(font.shader)
		batchMesh.vertexArray.draw(batchMesh.drawAs(gl), t*3)
	}

	/** Render only this string, but do not setup the font before, you must have
	  * called `font.beginRender()` before and you must call `font.endRender()`
	  * after. This is used for bulk processing, when several strings of the same
	  * font have to be rendered. */
	def render(mvp:Matrix4) {
		var clr = if(font.isAlphaPremultiplied) color.alphaPremultiplied else color

	    font.shader.uniform("textColor", clr)
		font.shader.uniformMatrix("MVP", mvp)
		batchMesh.vertexArray.draw(batchMesh.drawAs(gl), t*3)	
	}

	/** Render only this string, but do not setup the font before, you must have
	  * called `font.beginRender()` before and you must call `font.endRender()`
	  * after. This is used for bulk processing, when several strings of the same
	  * font have to be rendered. */
	def render(space:Space) {
		var clr = if(font.isAlphaPremultiplied) color.alphaPremultiplied else color

	    font.shader.uniform("textColor", clr)
		space.uniformMVP(font.shader)
		batchMesh.vertexArray.draw(batchMesh.drawAs(gl), t*3)
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

	/** Start the definition of a new string. This must be called before any call to char(Char).
	  * When finished the end() method must be called. The string is always located at the
	  * origin (0,0,0) on the XY plane. The point at the origin is at the baseline of the text
	  * before the first character. */
	def begin() { begin(0, 0) }

	/** Start the definition of a new string. This must be called before any call to char(Char).
	  * When finished the end() method must be called. The string is located at
	  * (`xStart`, `yStart`, 0), on the XY plane. The point at the origin is at the baseline of
	  * the text before the first character. */
	def begin(xStart:Float, yStart:Float) {
		if(p >= 0)
			throw new RuntimeException("nested call to begin, call end() first")

		p = 0
		t = 0
		x = xStart
		y = yStart
		l = 0
	}

	/** Add a character in the string. This can only be called after a call to begin() and before a call to end(). */
	def char(c:Char) {
		if(p < 0)
			throw new RuntimeException("call to char() without begin()")

		val width = font.charWidth(c)
		val rgn   = font.charTextureRegion(c)

		addCharQuad(rgn, width)

		x += width  	// Triangles may overlap.
		l += 1
	}

	/** The next call to `char()` will start at postion of the last char end with the
	  * (`dx`, `dy`) vector added. */
	def translate(dx:Float, dy:Float) {
		x += dx
		y += dy
	}

	/** End the definition of the new string. This can only be called if begin() has been called before. */
	def end() {
		if(p < 0)
			throw new RuntimeException("call to end() without begin()")
		batchMesh.updateVertexArray(gl, updateVertices=true, updateTexCoords=true)
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

			batchMesh v(p+0) xyz(X,   Y,   0) uv(rgn.u1, rgn.v2)
			batchMesh v(p+1) xyz(X+W, Y,   0) uv(rgn.u2, rgn.v2)
			batchMesh v(p+2) xyz(X+W, Y+H, 0) uv(rgn.u2, rgn.v1)
			batchMesh v(p+3) xyz(X,   Y+H, 0) uv(rgn.u1, rgn.v1)

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