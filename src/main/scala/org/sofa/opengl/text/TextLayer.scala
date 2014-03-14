package org.sofa.opengl.text

import scala.math._
import scala.collection.mutable.{ArrayBuffer, HashMap}

import org.sofa.opengl.{SGL, Space, ShaderProgram}
import org.sofa.math.{Point3, Rgba}


/** A text renderer that memorize couples of points and strings in pixel space,
  * and render them in one pass. */
class TextLayer(val gl:SGL, val textShader:ShaderProgram) {

	// TODO
	// - Group texts string by font, so that we minimize texture and shader changes.
	// - Allow some saving of some strings that are often used -> a cache ?

	val items = new ArrayBuffer[TextItem]()

	/** Map of known fonts. */
	val fonts = new HashMap[(String,Int), FontLayer]()

	/** Current font. */
	var font:FontLayer = null

	/** Current color. */
	var color = Rgba.Black

	def font(fontName:String, size:Int) {
		font = fonts.get((fontName,size)).getOrElse {
			val f = new FontLayer(gl, new GLFont(gl, fontName, size, textShader))
			fonts += ((fontName, size) -> f)
			f
		}
	}

	def color(newColor:Rgba) { color = newColor }

	def color(r:Double, g:Double, b:Double, a:Double) { color = Rgba(r, g, b, a) }

	def string(text:String, x:Double, y:Double, z:Double) { string(text, Point3(x, y, z)) }

	def string(text:String, position:Point3) { font.addItem(text, position, color) }

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
}


object FontLayer {
	val MaxChars = 128
}


/** A layer of text items with an unique font.
  *
  * This allows to render strings with the same font in grousp, to avoid
  * texture and GL attribute switches. 
  *
  * The font layer uses the following strategy to avoid create new strings
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
class FontLayer(val gl:SGL, val font:GLFont) {

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
	def addItem(text:String, position:Point3, color:Rgba) {
		// Take an item from the pool if possible or create it.

		var item:TextItem = null

		if(! pool.isEmpty) {
			item = pool.remove(pool.size-1)
			item.rebuild(text, position, color)
		} else {
 			item = new TextItem(text, this, position, color)
		}

		items += item
	} 
}


/** A single text item.
  *
  * A text item contains and maintain a single [[GLString]], its position, font, and 
  * color. The string has by default a capacity of [[FontLayer.MaxChars]] characters.
  * It can grow if one uses a text string larger and will never shrink. 
  */
class TextItem(var text:String, val font:FontLayer, val position:Point3, val color:Rgba) {
	
	/** The GL string used to render the text. */
	protected[this] var string:GLString = font.font.newString(text, FontLayer.MaxChars)

	/** Release the resources of the string. */
	def dispose() { string.dispose }

	/** Rebuild the item with a `newText` string at `newPosition` with `newColor`. */
	def rebuild(newText:String, newPosition:Point3, newColor:Rgba) {
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
			space.translate(position)
			string.setColor(color)
			string.render(space)
		}
		// space.translate(position)
		// string.setColor(color)
		// string.render(space)
		// space.translate(-position.x, -position.y, -position.z)
	}
}