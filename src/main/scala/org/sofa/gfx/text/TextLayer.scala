package org.sofa.gfx.text

import scala.math._
import scala.collection.mutable.{ArrayBuffer, HashMap}

import org.sofa.gfx.{SGL, Space, ShaderProgram}
import org.sofa.math.{Point2, Point3, Point4, Rgba}


/** A text renderer that memorize couples of points and strings in pixel space,
  * and render them in one pass.
  *
  * This is not done to render paragraphs of text, but to render small strings
  * of text at given positions in space. This could for example be used in a UI
  * for labels or buttons, or in a game.
  *
  * The two base ideas of the text layer are :
  *   - Allow to cache strings of text so that repeated display of the same string is faster.
  *   - Allow to draw text in one optimized pass at a point in the rendering process.
  *
  * The basic usage is to call the various `string` methods to specify a text
  * and a position, either in the actual [[Space]] or directly in pixels. If the position
  * is not specified in pixels, the [[Space]] is used to project the point in pixels.
  *
  * Then at the end of the current frame rendering for example, the `render()` method is
  * called, and all strings are displayed at once in an optimized way.
  *
  * The memorized strings are cached (several implementation are provided, depending on the use)
  * in order to accelerate their redisplay. */
class TextLayer(val gl:SGL, val textShader:ShaderProgram) {

	// TODO
	//
	// - do not render items out of screen. We know the size and positions.
	// - Allow some items to remain constant, when the user know it will reuse it (done with the cached items).
	// - Allow to handle "centered" and "right-aligne" text.
	// - Allow more positionning.

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
			val f = FontLayer(gl, new GLFont(gl, fontName, size, textShader))
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

	/** Request that the string `text` be displayed at next call to `render()` at `position` in pixels.
	  * `position` is stored and not copied. */
	def stringpx(text:String, position:Point2) { stringpx(text, Point4(position.x, position.y, 0, 1)) }

	/** Request that the string `text` be displayed at next call to `render()` at `position` in pixels.
	  * `position` is stored and not copied. */
	def stringpx(text:String, position:Point3) { stringpx(text, Point4(position.x, position.y, 0, 1)) }

	/** Request that the string `text` be displayed at next call to `render()` at `position` in pixels.
	  * `position` is stored and not copied. */
	def stringpx(text:String, position:Point4) { font.addItem(text, position, Rgba(color)) }

	/** Request that the string `text` be displayed at next call to `render()` at (`x`, `y`, `z`) in the current `space`.
	  * This position is first "projected" in pixel coordinates using `space`. */
	def string(text:String, x:Double, y:Double, z:Double, space:Space) { string(text, Point4(x, y, z, 1), space) }

	/** Request that the string `text` be displayed at next call to `render()` at `position` in the current `space`.
	  * This position is first "projected" in pixel coordinates using `space`. `position` is stored and not copied. */
	def string(text:String, position:Point3, space:Space) { string(text, Point4(position.x, position.y, position.z, 1), space) }

	/** Request that the string `text` be displayed at next call to `render()` at `position` in the current `space`.
	  * This position is first "projected" in pixel coordinates using `space`. `position` is stored and not copied. */
	def string(text:String, position:Point4, space:Space) {
		var pos:Point4 = position

		if(space ne null) {
			space.projectInPlace(position)
			position.perspectiveDivide

			val w:Double = space.viewportPx(0)
			val h:Double = space.viewportPx(1)

			position.x = position.x / 2 * w + w / 2
			position.y = position.y / 2 * h + h / 2
			position.z = 0
		}

		val item = font.addItem(text, position, Rgba(color))

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


/** Acts as a factory for font layer implementations. */
object FontLayer {
	def apply(gl:SGL, font:GLFont):FontLayer = {
		//new FontLayerReuse(gl, font)
		new FontLayerCached(gl, font)
	}
}


/** Interface of a font layer. */
trait FontLayer {
	/** Add a `text` to be renderer at next frame at `position` with `color`. */
	def addItem(text:String, position:Point4, color:Rgba):TextItem

	/** Render all the text items added via `addItem()` in the current `space`. All
	  * The items are them removed for the next frame. You must add them at each frame. */
	def renderItems(space:Space)

	/** Clear and release resources. */
	def dispose()
}


trait TextItem {
	/** The advance in pixels of this text item. */
	def advance:Double

	/** The height in pixels of this text item. */
	def height:Double
}


// -- Reuse a pool of GLStrings recompose them --------------------------------------------


object FontLayerReuse {
	val MaxChars = 128
}


/** A layer of text items with an unique font that reuse text items for each new string.
  *
  * This allows to render strings with the same font in groups, to avoid
  * texture and GL attribute switches.
  *
  * The font layer 'reuse' uses the following strategy to avoid creating new strings
  * at each render pass :
  *   - There is a pool of already used strings.
  *   - There is a set of strings to render.
  *   - At each insertion of a text item, a string is taken from the pool
  *     if the pool is large enougth, else a new one is created.
  *   - After each rendering pass, the pool is emptyed from remaining
  *     strings if any, and the set of strings is switched with the pool.
  * This strategy allows to maintain a set of strings that is as large as
  * the max number of strings used at the last render.
  *
  * The strings are [[GLString]] objects, but the text of the string can vary, and is
  * recomposed each time. See [[FontLayerCached]] for a font layer that tries to
  * cache text strings and reuse them.
  */
class FontLayerReuse(val gl:SGL, val font:GLFont) extends FontLayer {

	/** Pool of unused text items. */
	protected[this] var pool = new ArrayBuffer[TextItemReuse]()

	/** Set of text items to render at next rendering pass. */
	protected[this] var items = new ArrayBuffer[TextItemReuse]()

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

		var item:TextItemReuse = null

		if(! pool.isEmpty) {
			item = pool.remove(pool.size-1)
			item.rebuild(text, position, color)
		} else {
 			item = new TextItemReuse(text, this, position, color)
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
class TextItemReuse(var text:String, val font:FontLayerReuse, val position:Point4, val color:Rgba) extends TextItem {
	
	/** The GL string used to render the text. */
	protected[this] var string:GLString = font.font.newString(text, FontLayerReuse.MaxChars)

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


// -- Use a cache of the most often used texts -------------------------------------


object FontLayerCached {
	final val MaxCacheSize = 1024

	final val CleanEverySteps = 100
}


case class TextItemCached(string:StringItemCached, position:Point4, color:Rgba) extends TextItem {
	/** Total advance in pixels. */
	def advance:Double = string.advance

	/** Total height in pixels. */
	def height:Double = string.height

	/** Advance and height in pixels. */
	def sizes:(Double,Double) = (string.advance, string.height)

	/** Push a new space, translate to the string position, render the string and restore the space. */
	def render(space:Space) {
		// Push and pop or translate and translate back ?
		space.pushpop {
			space.translate(position.x, position.y, 0)
			string.string.setColor(color)
			string.string.render(space)
		}
	}
}


case class StringItemCached(string:GLString, text:String) {

	/** Number of uses of the string. */
	protected[this] var used:Int = 0

	/** Use the string. */
	def use() { used += 1 }

	/** Number of times the string has been used. */
	def usage:Int = used

	/** Total advance in pixels. */
	def advance:Double = string.advance

	/** Total height in pixels. */
	def height:Double = string.height

	/** Advance and height in pixels. */
	def sizes:(Double,Double) = (string.advance, string.height)

	/** Release the resources of the string. */
	def dispose() { string.dispose }
}


class FontLayerCached(val gl:SGL, val font:GLFont) extends FontLayer {
	import FontLayerCached._

	/** The things to draw at next frame. */
	protected[this] var items = new ArrayBuffer[TextItemCached]()

	/** The cache of strings and fonts. */
	protected[this] val pool = new HashMap[String, StringItemCached]()

	protected[this] var needClean = CleanEverySteps

	/** Render each stored text items. */
	def renderItems(space:Space) { 
		font.beginRender
		var i = 0
		val n = items.size
		while(i < n) {
			items(i).render(space)
			i += 1
		}
		//items.foreach { _.render(space) } 
		font.endRender
		
		items.clear
		cleanCache
	}

	/** Add an item */
	def addItem(text:String, position:Point4, color:Rgba):TextItem = {
		val string = pool.get(text).getOrElse {
			val s = new StringItemCached(font.newString(text, text.length), text)
			pool += (text -> s)
			s
		}

		string.use

		val item = TextItemCached(string, position, color)

		items += item

		item
	}

	protected def cleanCache() {
		needClean -= 1

		if(needClean <= 0) {
			val n = pool.size
			
			if(n > MaxCacheSize) {
				org.sofa.Timer.timer.measure("TextLayer.Cache.Clean") {
					val sorted = pool.valuesIterator.toArray.sortWith { (a, b) => a.usage > b.usage }
					var i      = MaxCacheSize
				
					while(i < n) {
						pool.remove(sorted(i).text)
						i += 1
					}
				}
			}

			needClean = CleanEverySteps
		}
	}

	/** Release all GL and font resources. */
	def dispose() {
		pool.foreach { _._2.dispose }
		pool.clear
		items.clear
	}
}