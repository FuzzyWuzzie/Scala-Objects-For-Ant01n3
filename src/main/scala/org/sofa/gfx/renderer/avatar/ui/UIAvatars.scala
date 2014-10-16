package org.sofa.gfx.renderer.avatar.ui

import scala.math.{min, max}

import org.sofa.math.{Point3, Point4, Vector3, Matrix4, Rgba, Box3, Box3From, Box3PosCentered, Box3Default}
import org.sofa.gfx.{ShaderResource, TextureFramebuffer}
import org.sofa.gfx.text.{GLFont}
import org.sofa.gfx.renderer.{Screen}
import org.sofa.gfx.renderer.{Avatar, AvatarLayer, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState}
import org.sofa.gfx.surface.event._
import org.sofa.gfx.renderer.{NoSuchAvatarException}

import org.sofa.gfx.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.gfx.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}
import org.sofa.gfx.dl.DisplayList


class UIAvatarFactory extends DefaultAvatarFactory {
	override def avatarFor(name:AvatarName, screen:Screen, kind:String):Avatar = {
		kind.toLowerCase match {
			case "ui.root"        => new UIRoot(name, screen)
			case "ui.root-events" => new UIRootEvents(name, screen)
			case "ui.list"        => new UIList(name, screen)
			case "ui.list-item"   => new UIListItem(name, screen)
			case "ui.perspective" => new UIPerspective(name, screen)
			case "ui.panel"       => new UIPanel(name, screen)
			case "ui.toolbar"     => new UIToolbar(name, screen)
			case _ => chainAvatarFor(name, screen, kind)
		}
	}
}


object UIAvatar {
	/** Width of a knob, by default 1.5 mm. TODO CSS. */
	final val KnobWidth = 0.1

	/** Color of a knob. TODO CSS. */
	final val KnobColor = Rgba.Grey40

	/** Set the layout of an avatar. */
	case class SetLayout(layout:UILayout) extends AvatarSpaceState {}
}


/** Base class for a hierarchy of avatar allowing to build user interfaces. */
abstract class UIAvatar(name:AvatarName, screen:Screen) extends DefaultAvatarComposed(name, screen) {

	/** Another consumeOrPropagateEvent that looks if an avatar is visible to propagate the
	  * event to it. To call only if the event is a spatial one. */
	def consumeOrPropagateEventVisible(event:SpatialEvent):Boolean = {
		if(! consumeEvent(event)) {
			self.findSub { sub =>
				if(space.isVisible(sub))
					 sub.events.consumeOrPropagateEvent(event)
				else false
			} match {
				case Some(a) => true
				case _       => false
			}
		} else {
			true
		}
	}

	/** Overriding of the consume-event operation to push/pop the avatar space.
	  * This allows to be able to test if a spatial event is inside an avatar or
	  * not. */
	override def consumeOrPropagateEvent(event:Event):Boolean = {
		var res:Boolean = false

		event match {
			case e:SpatialEvent => {
				space.pushSubSpace
				res = consumeOrPropagateEventVisible(e)
				space.popSubSpace
			}
			case _ => super.consumeOrPropagateEvent(event)
		}

		res
	}

	def containsEvent(event:Event):Boolean = {
		if(event.isInstanceOf[SpatialEvent]) {
			val space    = screen.space
			val sevent   = event.asInstanceOf[SpatialEvent] 
			val from     = space.project(Point4(0, 0, 0, 1))
			val to       = space.project(Point4(this.space.subSpace.sizex, this.space.subSpace.sizey, 0, 1))
			val origPos  = sevent.position()
			val w:Double = space.viewport(0)
			val h:Double = space.viewport(1)

			// // project yield elements in [-1:1] along X and Y, convert to pixels:

			val posx  = origPos.x
			val posy  = h - origPos.y
			val fromx = from.x / 2 * w + w / 2
			val fromy = from.y / 2 * h + h / 2
			val tox   = to.x   / 2 * w + w / 2
			val toy   = to.y   / 2 * h + h / 2

			// Test inclusion in pixels:

			(( (posx >= scala.math.min(fromx, tox)) && (posx <= scala.math.max(fromx, tox)) ) &&
			 ( (posy >= scala.math.min(fromy, toy)) && (posy <= scala.math.max(fromy, toy)) ))
		} else {
			false
		}
	}

	override def renderFilterRequest() {
		space match {
			case s:UIAvatarSpace => s.layoutRequest
			case _ => {}
		}
		super.renderFilterRequest
		self.screen.requestRender		
	}
}


// ----------------------------------------------------------------------------------------------


object UIrenderUtils {
	/** A quad mesh to fill a rectangular area. */
	var plainRect:TrianglesMesh = null

	/** A square line plainRect to stroke a rectangular area. */
	var strokeRect:LinesMesh = null

	/** A quad mesh to fill a rectangular area with a texture. */
	var texRect:TrianglesMesh = null

	/** A quad mesh to fill a rectangular area with a texture for off-screen layers. */
	var layerRect:TrianglesMesh = null

	/** A quad mesh with the two upper vertices black and the two lower vertices transparent. */
	var shadowUnderRect:TrianglesMesh = null

	/** A shader that takes an uniform color named `uniformColor` an apply it to each vertex. */
	var shaderUniform:ShaderProgram = null

	/** A shader that waits color information on each vertex, and allows a `transparency`
	  * uniform to make it more or less transparent. */
	var shaderColor:ShaderProgram = null

	/** A shader that waits tex coords on each vertex. */
	var shaderTex:ShaderProgram = null

	/** A shader dedicated to rending off-screen layers from FBO. */
	var shaderLayer:ShaderProgram = null
}


trait UIrenderUtils {

	var color = Rgba.Cyan

	var lineColor = Rgba.Red

	def self:Avatar

	def shaderUniform:ShaderProgram = {
		if(UIrenderUtils.shaderUniform eq null)
			UIrenderUtils.shaderUniform = self.screen.libraries.shaders.getOrAdd(self.screen.gl,
				"uniform-color-shader",
				ShaderResource("uniform-color-shader", "uniform_color.vert.glsl", "uniform_color.frag.glsl"))
		UIrenderUtils.shaderUniform
	}

	def shaderColor:ShaderProgram = {
		if(UIrenderUtils.shaderColor eq null) {
			UIrenderUtils.shaderColor = self.screen.libraries.shaders.getOrAdd(self.screen.gl,
				"color-shader",
				ShaderResource("color-shader", "plain_shader.vert.glsl", "plain_shader.frag.glsl"))
		}
		UIrenderUtils.shaderColor
	}

	def shaderTex:ShaderProgram = {
		if(UIrenderUtils.shaderTex eq null) {
			UIrenderUtils.shaderTex = self.screen.libraries.shaders.getOrAdd(self.screen.gl,
				"image-shader",
				ShaderResource("image-shader", "image_shader.vert.glsl", "image_shader.frag.glsl"))
		}
		UIrenderUtils.shaderTex
	}

	def shaderLayer:ShaderProgram = {
		if(UIrenderUtils.shaderLayer eq null) {
			UIrenderUtils.shaderLayer = self.screen.libraries.shaders.getOrAdd(self.screen.gl,
				"layer-shader",
				if(self.screen.gl.hasTexImage2DMultisample && self.screen.surface.multiSampling > 1) {
					val ms = self.screen.surface.multiSampling
					ShaderResource("layer-shader", "multisample_image_shader.vert.glsl", "multisample%d_image_shader.frag.glsl".format(ms))
				} else {
					ShaderResource("layer-shader", "image_shader.vert.glsl", "image_shader.frag.glsl")
				})
		}
		UIrenderUtils.shaderLayer
	}

	def plainRect:TrianglesMesh = {
		if(UIrenderUtils.plainRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIrenderUtils.plainRect = new TrianglesMesh(2)
			UIrenderUtils.plainRect.setPoint(0, 0, 0, 0)
			UIrenderUtils.plainRect.setPoint(1, 1, 0, 0)
			UIrenderUtils.plainRect.setPoint(2, 1, 1, 0)
			UIrenderUtils.plainRect.setPoint(3, 0, 1, 0)
			UIrenderUtils.plainRect.setTriangle(0, 0, 1, 2)
			UIrenderUtils.plainRect.setTriangle(1, 0, 2, 3)
			UIrenderUtils.plainRect.newVertexArray(gl, shaderUniform, Vertex -> "position")			
		}

		UIrenderUtils.plainRect
	}

	def texRect:TrianglesMesh = {
		if(UIrenderUtils.texRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIrenderUtils.texRect = new TrianglesMesh(2)
			UIrenderUtils.texRect v(0) xyz(0, 0, 0) uv(0, 1)
			UIrenderUtils.texRect v(1) xyz(1, 0, 0) uv(1, 1)
			UIrenderUtils.texRect v(2) xyz(1, 1, 0) uv(1, 0)
			UIrenderUtils.texRect v(3) xyz(0, 1, 0) uv(0, 0)
			UIrenderUtils.texRect.setTriangle(0, 0, 1, 2)
			UIrenderUtils.texRect.setTriangle(1, 0, 2, 3)
			UIrenderUtils.texRect.newVertexArray(gl, shaderTex, Vertex -> "position", TexCoord -> "texCoords")
		}

		UIrenderUtils.texRect
	}

	def layerRect:TrianglesMesh = {
		if(UIrenderUtils.layerRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIrenderUtils.layerRect = new TrianglesMesh(2)
			UIrenderUtils.layerRect v(0) xyz(0, 0, 0) uv(0, 1)
			UIrenderUtils.layerRect v(1) xyz(1, 0, 0) uv(1, 1)
			UIrenderUtils.layerRect v(2) xyz(1, 1, 0) uv(1, 0)
			UIrenderUtils.layerRect v(3) xyz(0, 1, 0) uv(0, 0)
			UIrenderUtils.layerRect.setTriangle(0, 0, 1, 2)
			UIrenderUtils.layerRect.setTriangle(1, 0, 2, 3)
			UIrenderUtils.layerRect.newVertexArray(gl, shaderTex, Vertex -> "position", TexCoord -> "texCoords")
		}

		UIrenderUtils.layerRect
	}

	def strokeRect:LinesMesh = {
		if(UIrenderUtils.strokeRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIrenderUtils.strokeRect = new LinesMesh(4)
			UIrenderUtils.strokeRect.setLine(0, 0,0,0, 1,0,0)
			UIrenderUtils.strokeRect.setLine(1, 1,0,0, 1,1,0)
			UIrenderUtils.strokeRect.setLine(2, 1,1,0, 0,1,0)
			UIrenderUtils.strokeRect.setLine(3, 0,1,0, 0,0,0)
			UIrenderUtils.strokeRect.newVertexArray(gl, shaderUniform, Vertex -> "position")
		}		
		UIrenderUtils.strokeRect
	}

	def shadowUnderRect:TrianglesMesh = {
		if(UIrenderUtils.shadowUnderRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIrenderUtils.shadowUnderRect = new TrianglesMesh(2)
			UIrenderUtils.shadowUnderRect v(0) xyz(0, 0, 0) rgba(0, 0, 0, 0.25f)
			UIrenderUtils.shadowUnderRect v(1) xyz(1, 0, 0) rgba(0, 0, 0, 0.25f)
			UIrenderUtils.shadowUnderRect v(2) xyz(1, 1, 0) rgba(0, 0, 0, 0)
			UIrenderUtils.shadowUnderRect v(3) xyz(0, 1, 0) rgba(0, 0, 0, 0)
			UIrenderUtils.shadowUnderRect t(0, 0, 1, 2)
			UIrenderUtils.shadowUnderRect t(1, 0, 2, 3)
			UIrenderUtils.shadowUnderRect.newVertexArray(gl, shaderColor, Vertex -> "position", Color -> "color")
		}
		UIrenderUtils.shadowUnderRect
	}

	/** Stroke the space of the avatar with an uniform color. */
	def fillAndStroke() {
		val screen = self.screen
		val space  = screen.space
		val gl     = screen.gl

		val subSpace = self.space.subSpace

		space.pushpop {
			shaderUniform.use
			//println(s"    | rendering ${self.name} scale(${subSpace.size(0)},${subSpace.size(1)})")
			shaderUniform.uniform("uniformColor", color)
			space.scale(subSpace.sizex, subSpace.sizey, 1)
			space.uniformMVP(shaderUniform)
			plainRect.draw(gl)
			shaderUniform.uniform("uniformColor", lineColor)
			strokeRect.draw(gl)
		}
	}

	class FillDP extends DisplayList {
		val mvp = Matrix4()
		val color = Rgba(0, 0, 0, 0)
		def compile(color:Rgba, mvp:Matrix4, width:Double, height:Double) {
			this.color.copy(color)
			updateSpace(mvp, width, height)
		}
		def updateSpace(mvp:Matrix4, width:Double, height:Double) {
			this.mvp.copy(mvp)
			this.mvp.scale(width, height, 1)
		}
		def render(gl:SGL) {
			shaderUniform.use
			shaderUniform.uniform("uniformColor", color)
			shaderUniform.uniformMatrix("MVP", mvp)
			plainRect.draw(gl)
		}
		def dispose(gl:SGL) {}
	}

	private[this] val fillDP = new FillDP()

	/** Fill the space of the avatar with an uniform color. */
	def fill() {
		val gl = self.screen.gl

		if(self.spaceChanged || self.renderChanged) {
			val subSpace = self.space.subSpace

			fillDP.compile(color, self.screen.space.top, subSpace.sizex, subSpace.sizey)
		}
		
		fillDP.render(gl)
	}

	class ShadowAtDP extends DisplayList {
		val mvp = Matrix4()
		def compile(dx:Double, dy:Double, width:Double, height:Double, mvp:Matrix4) {
			updateSpace(dx, dy, width, height, mvp)
		}
		def updateSpace(dx:Double, dy:Double, width:Double, height:Double, mvp:Matrix4) {
			this.mvp.copy(mvp)
			this.mvp.translate(dx, dy, 0)
			this.mvp.scale(width, height, 1)
		}
		def render(gl:SGL) {
			shaderColor.use
			shaderColor.uniformMatrix("MVP", mvp)
			shadowUnderRect.draw(gl)
		}
		def dispose(gl:SGL) {}
	}

	private[this] val shadowAtDP = new ShadowAtDP()

	def horizShadowAbove(sizeCm:Double) { horizShadowAt(0, 1.0, sizeCm) }

	def horizShadowUnder(sizeCm:Double) { horizShadowAt(self.space.thisSpace.sizey, 1.0, sizeCm) }

	def horizShadowAt(y:Double, alpha:Double, sizeCm:Double) {
		// TODO alpha not used yet

		val gl = self.screen.gl

		if(self.spaceChanged || self.renderChanged) {
			val thisSpace = self.space.thisSpace
			val s1cm      = self.space.scale1cm
			shadowAtDP.compile(thisSpace.fromx, thisSpace.fromy + y, thisSpace.sizex, s1cm * sizeCm, self.screen.space.top)
		}
		
		shadowAtDP.render(gl)
	}

	class LayerDP extends DisplayList {
		val mvp = Matrix4()
		var layer:AvatarLayer = null
		def compile(layer:AvatarLayer, mvp:Matrix4, x:Double, y:Double, width:Double, height:Double) {
			this.layer = layer
			updateSpace(mvp, x, y, width, height)
		}
		def updateSpace(mvp:Matrix4, x:Double, y:Double, width:Double, height:Double) {
			this.mvp.copy(mvp)
			this.mvp.translate(x, y, 0)
			
			// Account for the fact the layer pixels are integers, but the area
			// inside it is smaller (the correct real size). Therefore we must enlarge
			// the layer a bit by the factors stored inside the layer. As we are in
			// a coordinate system where the upper-left corner is the origin, his works.
			
			this.mvp.scale(width * layer.scalex, height * layer.scaley, 1)
		}
		def render(gl:SGL) {
			shaderLayer.use
			layer.fb.bindColorTexture
			shaderLayer.uniformTexture(layer.fb.colorTexture, "texColor")
			shaderLayer.uniformMatrix("MVP", mvp)
			layerRect.draw(gl)
		}
		def dispose(gl:SGL) {}
	}

	protected[this] var layerDP:LayerDP = null

	def renderLayer() {
		val gl = self.screen.gl

		if(self.spaceChanged || self.renderChanged) {
			if(layerDP eq null) layerDP = new LayerDP()

			val subSpace = self.space.subSpace

			layerDP.compile(self.layer, self.screen.space.top, subSpace.posx, subSpace.posy, subSpace.sizex, subSpace.sizey)
		}	

		layerDP.render(gl)
	}
}


class UIAvatarRender(var self:Avatar) extends AvatarRender {
	def screen:Screen = self.screen
}


// ----------------------------------------------------------------------------------------------


abstract class UIAvatarSpace(var self:Avatar) extends AvatarSpace {

 	var scale1cm = 1.0

	/** An independant layout algorithm, eventually shared between instances, if
	  * the space does not already acts as a layout. */
	protected[this] var layout:UILayout = null
	
	/** A flag indicating the layout must be recomputed.
	  * This flag is set to true when (this list is exaustive
	  * and should be kept up to date):
	  *  - the avatar is created.
	  *  - this avatar space changes (Avatar.spaceChanged).
	  *  - a sub-avatar is added or removed.
	  *  - the layoutRequest is issued by the sub-avatars. */
	protected[this] var dirtyLayout = true

	/** Tell this avatar that one of its sub-avatar changed its size or disposition and this
	  * avatar must relayout. If this avatar changes its size or disposition in return, it
	  * can propagate the layout request to its parent avatar. */
	def layoutRequest() { dirtyLayout = true }

	/** True if the layout needs to be recomputed at next animation step. */
	def needRelayout:Boolean = dirtyLayout

	override def subCountChanged(delta:Int) {
		layoutRequest
		self.screen.requestRender
	}

	override def changeSpace(newState:AvatarSpaceState) {
		newState match {
			case UIAvatar.SetLayout(newLayout) => layout = newLayout
			case state => super.changeSpace(state)
		}
	}

	override def animateSpace() {
		if(dirtyLayout) {
			self.screen.requestRender
			dirtyLayout = false
			self.spaceChanged = true
			
			if(layout ne null)
				layout.layout(self, scale1cm)
		}

		//super.animateSpace	// super is a trait with an abstract animateSpace() method.
	}
}
