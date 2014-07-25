package org.sofa.gfx.text

import scala.math._
import scala.collection.mutable.{ArrayBuffer, HashMap}

import org.sofa.gfx.{SGL, Space, ShaderProgram}
import org.sofa.math.{Point2, Point3, Point4, Rgba}


/** A text renderer that memorize couples of points and strings in pixel space,
  * and render them in one pass.
  *
  * The way the text layer work is that at some point in your rendering process,
  * you issue a string at a given position. This position can either be pixels,
  * or your current space (in which case the position is projected in pixels).
  * Then at the end of the current frame rendering, the `render()` method is
  * called, and all strings are displayed at once in an optimized way. Then
  * these memorized strings are cleared for the next rendering pass.
  *
  * There is a caching mechanism to reuse strings. */
class TextLayer(val gl:SGL, val textShader:ShaderProgram) {

	// TODO
	//
	// - do not render items out of screen. We know the size and positions.
	// - Allow some items to remain constant, when the user know it will reuse it.

	/** Map of known fonts. */
	protected[this] val fonts = new HashMap[(String,Int), FontLayer]()

	/** Current font. */
	protected[this] var font:FontLayer = null

	/** Current color. */
	protected[this] var color = Rgba.Black

	/** Last advance of inserted string. */
	protected[this] var la = -1.0

	/** Last height of inserted string. */
	protected[this] var lh = -1.0

	/** Overall width in pixels of the last inserted string. */
	def lastAdvance:Double = la

	/** Overall height in pixels of the last inserted string. */
	def lastHeight:Double = lh

	/** Set the current font. All subsequently inserted strings will use it.
	  *
	  * Using a font for the first time is time-consuming since it muse load it.
	  * Then a call to this method is in amortized O(1) time. */
	def font(fontName:String, size:Int) {
		font = fonts.get((fontName,size)).getOrElse {
			val f = new FontLayer(gl, new GLFont(gl, fontName, size, textShader), this)
			fonts += ((fontName, size) -> f)
			f
		}
	}

	/** Set the current color. All subsequently inserted strings will use it. */
	def color(newColor:Rgba) { color = newColor }

	/** Set the current color. All subsequently inserted strings will use it. */
	def color(r:Double, g:Double, b:Double, a:Double) { color = Rgba(r, g, b, a) }

	/** Request that the string `text` be displayed at next call to `render()` at (`x`, `y`) in pixels. */
	def stringpx(text:String, x:Double, y:Double) { stringpx(text, Point4(x, y, 0, 1)) }

	/** Request that the string `text` be displayed at next call to `render()` at `position` in pixels. */
	def stringpx(text:String, position:Point2) { stringpx(text, Point4(position.x, position.y, 0, 1)) }

	/** Request that the string `text` be displayed at next call to `render()` at `position` in pixels. */
	def stringpx(text:String, position:Point3) { stringpx(text, Point4(position.x, position.y, 0, 1)) }

	/** Request that the string `text` be displayed at next call to `render()` at `position` in pixels. */
	def stringpx(text:String, position:Point4) { font.addItem(text, position, Rgba(color)) }

	/** Request that the string `text` be displayed at next call to `render()` at (`x`, `y`, `z`) in the current `space`.
	  * This position is first "projected" in pixel coordinates using `space`. */
	def string(text:String, x:Double, y:Double, z:Double, space:Space) { string(text, Point4(x, y, z, 1), space) }

	/** Request that the string `text` be displayed at next call to `render()` at `position` in the current `space`.
	  * This position is first "projected" in pixel coordinates using `space`. */
	def string(text:String, position:Point3, space:Space) { string(text, Point4(position.x, position.y, position.z, 1), space) }

	/** Request that the string `text` be displayed at next call to `render()` at `position` in the current `space`.
	  * This position is first "projected" in pixel coordinates using `space`. */
	def string(text:String, position:Point4, space:Space) {
		var pos:Point4 = position

		//print(s"${position} --> ")
		if(space ne null) {
			pos = space.transform(position)
			pos.perspectiveDivide

			val w:Double = space.viewportPx(0)
			val h:Double = space.viewportPx(1)

			pos.x = pos.x / 2 * w + w / 2
			pos.y = pos.y / 2 * h + h / 2
			pos.z = 0
		}

		val item = font.addItem(text, pos, Rgba(color))

		la = item.advance
		lh = item.height
	}

	/** Render all strings and flush them. */
	def render(space:Space) {
		space.pushpop {
			space.pushpopProjection {
				space.orthographicPixels()
				space.viewIdentity
				renderText(space)
			}
		}
	}

	protected def renderText(space:Space) {
		fonts.foreach { _._2.renderItems(space) }
	}

	/** Release all resources (OpenGL and fonts). */
	def dispose() {
		fonts.foreach { _._2.dispose }
		font = null
	}
}


object FontLayer {
	val MaxChars = 128
}


/** A layer of text items with an unique font.
  *
  * This allows to render strings with the same font in groups, to avoid
  * texture and GL attribute switches. 
  *
  * The font layer uses the following strategy to avoid creating new strings
  * at each render pass :
  *   - There is a pool of already used strings.
  *   - There is a set of strings to render.
  *   - At each insertion of a text item, a string is taken from the pool
  *     if the pool is large enougth, else a new one is created.
  *   - After each rendering pass, the pool is emptyed from remaining
  *     strings if any, and the set of strings is switched with the pool.
  * This strategy allows to maintain a set of strings that is as large as
  * the max number of strings used at the last render.
  */
class FontLayer(val gl:SGL, val font:GLFont, val textlayer:TextLayer) {

	/** Pool of unused text items. */
	protected[this] var pool = new ArrayBuffer[TextItem]()

	/** Set of text items to render at next rendering pass. */
	protected[this] var items = new ArrayBuffer[TextItem]()

	/** Render each stored text items. */
	def renderItems(space:Space) { 
		font.beginRender
		items.foreach { _.render(space) } 
		font.endRender
		
		// Remove remaining items in pool.

		pool.foreach { _.dispose }
		pool.clear

		// Exchange pool with items

		var tmp = items
		items = pool
		pool  = tmp		
	}

	/** Add an item */
	def addItem(text:String, position:Point4, color:Rgba):TextItem = {
		// Take an item from the pool if possible or create it.

		var item:TextItem = null

		if(! pool.isEmpty) {
			item = pool.remove(pool.size-1)
			item.rebuild(text, position, color)
		} else {
 			item = new TextItem(text, this, position, color)
		}

		items += item

		item
	}

	/** Release all GL and font resources. */
	def dispose() {
		pool.foreach { _.dispose }
		items.foreach { _.dispose }
		pool.clear
		items.clear
	}
}


/** A single text item.
  *
  * A text item contains and maintain a single [[GLString]], its position, font, and 
  * color. The string has by default a capacity of [[FontLayer#MaxChars]] characters.
  * It can grow if one uses a text string larger and will never shrink.
  */
class TextItem(var text:String, val font:FontLayer, val position:Point4, val color:Rgba) {
	
	/** The GL string used to render the text. */
	protected[this] var string:GLString = font.font.newString(text, FontLayer.MaxChars)

	/** Total advance in pixels. */
	def advance:Double = string.advance

	/** Total height in pixels. */
	def height:Double = string.height

	/** Advance and height in pixels. */
	def sizes:(Double,Double) = (string.advance, string.height)

	/** Release the resources of the string. */
	def dispose() { string.dispose }

	/** Rebuild the item with a `newText` string at `newPosition` with `newColor`. */
	def rebuild(newText:String, newPosition:Point4, newColor:Rgba) {
		color.copy(newColor)
		position.copy(newPosition)
		text = newText

		if(string.maxLength < newText.length) {
			string.dispose
			string = font.font.newString(text, newText.length)
		}

		string.build(newText)
	}

	/** Push a new space, translate to the string position, render the string and restore the space. */
	def render(space:Space) {
		// Push and pop or translate and translate back ?
		space.pushpop {
			space.translate(position.x, position.y, 0)
			string.setColor(color)
			string.render(space)
		}
		// space.translate(position.x, position.y, 0)
		// string.setColor(color)
		// string.render(space)
		// space.translate(-position.x, -position.y, 0)
	}
}