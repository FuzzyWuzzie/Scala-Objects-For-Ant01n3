package org.sofa.gfx.dl

import scala.collection.mutable.{ArrayBuffer, HashMap}

import org.sofa.math.{Point4, Matrix4}
import org.sofa.gfx.{SGL, Space, ScissorStack, Scissors, ShaderProgram}
import org.sofa.gfx.text.{GLFont, GLString, GLText, TextAlign, VerticalAlign}



trait TextLayerItem {
	val text:GLText
	def render(gl:SGL)
}


object TextDL {
	val tmppos = Point4(0, 0, 0, 1)
}


class TextDL(val text:GLText, val layer:TextLayer) extends DisplayList with TextLayerItem {
	private[this] val mvp = Matrix4()

	def updateSpace(x:Double, y:Double, z:Double, renderSpace:Space) {
		val p = TextDL.tmppos
		val textSpace = layer.textSpace
		p.set(x, y, z, 1)
		renderSpace.projectInPlace(p)
		p.perspectiveDivide
		val w:Double = textSpace.viewportPx(0)
		val h:Double = textSpace.viewportPx(1)
		p.set(p.x / 2 * w + w / 2, p.y / 2 * h + h / 2, 0, 1)
		textSpace.translate(p.x, p.y, 0)
		mvp.copy(textSpace.top)
		textSpace.translate(-p.x, -p.y, 0)
	}

	def compile(x:Double, y:Double, z:Double, renderSpace:Space)(code:(GLText)=>Unit) { 
		updateSpace(x, y, z, renderSpace)
		code(text)
	} 

	def render(gl:SGL) { text.render(mvp) }

	def dispose(gl:SGL) { text.dispose() }
}


case class ScissorsPush(stack:ScissorStack, scissors:Scissors) extends TextLayerItem {
	def render(gl:SGL) { stack.push(gl, scissors) }
	val text:GLText = null
}


case class ScissorsPop(stack:ScissorStack) extends TextLayerItem {
	def render(gl:SGL) { stack.pop(gl) }
	val text:GLText = null
}


class FontLayer(val font:GLFont) {
	protected[this] val items = new ArrayBuffer[TextLayerItem]() 

	def += (item:TextLayerItem) {
		items += item
	}

 	def render(gl:SGL) {
 		GLText.beginRender(font)

 		val n = items.size
 		var i = 0

 		while(i < n) {
 			items(i).render(gl)
 			i += 1
 		}

 		GLText.endRender()
 		items.clear
 	} 
}


class TextSpace extends Space {}


object TextLayer {
}


class TextLayer(val gl:SGL, val textShader:ShaderProgram) {

	/** Map of known fonts. */
	protected[this] val fonts = new HashMap[(String,Int), FontLayer]()

	/** The scissor stack used during rendering. */
	protected[this] var scissorStack:ScissorStack = null

	/** The temporary false stack used while submitting text items.
	  * Used to handle fonts added while some scissor operations are pushed. */
	protected[this] var scissorsTmpStack:ArrayBuffer[Scissors] = null

	val textSpace:Space = new TextSpace()

	def dl(fontName:String, size:Int, maxChars:Int):TextDL = new TextDL(new GLText(gl, font(fontName, size).font, maxChars), this)

	def font(fontName:String, size:Int):FontLayer = fonts.get((fontName,size)).getOrElse {
		val f = new FontLayer(new GLFont(gl, fontName, size, textShader))
		fonts += ((fontName, size) -> f)

		if((scissorsTmpStack ne null) && (scissorsTmpStack.size > 0))
			scissorsTmpStack.foreach { f += ScissorsPush(scissorStack, _) }

		f
	}

 	def += (item:TextLayerItem) {
 		val f = item.text.font
 		font(f.file, f.size) += item
 	}

 		/** All string pushed after this will be potentially cut by the given scissors. */
	def pushScissors(scissors:Scissors) {
		if(scissorStack eq null) {
			scissorStack = ScissorStack()
			scissorsTmpStack = new ArrayBuffer[Scissors]()
		}

		scissorsTmpStack += scissors

		fonts.foreach { _._2 += ScissorsPush(scissorStack, scissors) }
	}

	/** Remove the last pushed scissors. */
	def popScissors() {
		fonts.foreach { _._2 += ScissorsPop(scissorStack) }
		scissorsTmpStack.trimEnd(1)
	}

	def reshape(widthPx:Int, heightPx:Int) {
		textSpace.viewportPx(widthPx, heightPx)
		textSpace.orthographicPixels()
 		textSpace.viewIdentity()
	}

 	def render() {
 		fonts.foreach { _._2.render(gl) }
 	 }
}