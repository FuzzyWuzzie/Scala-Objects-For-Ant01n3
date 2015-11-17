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


object UIRenderUtils {
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


trait UIRenderUtils {

	var color = Rgba.Cyan

	var lineColor = Rgba.Red

	def self:Avatar

	def shaderUniform:ShaderProgram = {
		if(UIRenderUtils.shaderUniform eq null)
			UIRenderUtils.shaderUniform = self.screen.libraries.shaders.getOrAdd(self.screen.gl,
				"uniform-color-shader",
				ShaderResource("uniform-color-shader", "uniform_color.vert.glsl", "uniform_color.frag.glsl"))
		UIRenderUtils.shaderUniform
	}

	def shaderColor:ShaderProgram = {
		if(UIRenderUtils.shaderColor eq null) {
			UIRenderUtils.shaderColor = self.screen.libraries.shaders.getOrAdd(self.screen.gl,
				"color-shader",
				ShaderResource("color-shader", "plain_shader.vert.glsl", "plain_shader.frag.glsl"))
		}
		UIRenderUtils.shaderColor
	}

	def shaderTex:ShaderProgram = {
		if(UIRenderUtils.shaderTex eq null) {
			UIRenderUtils.shaderTex = self.screen.libraries.shaders.getOrAdd(self.screen.gl,
				"image-shader",
				ShaderResource("image-shader", "image_shader.vert.glsl", "image_shader.frag.glsl"))
		}
		UIRenderUtils.shaderTex
	}

	def shaderLayer:ShaderProgram = {
		if(UIRenderUtils.shaderLayer eq null) {
			UIRenderUtils.shaderLayer = self.screen.libraries.shaders.getOrAdd(self.screen.gl,
				"layer-shader",
				if(self.screen.gl.hasTexImage2DMultisample && self.screen.surface.multiSampling > 1) {
					val ms = self.screen.surface.multiSampling
					ShaderResource("layer-shader", "multisample_image_shader.vert.glsl", "multisample%d_image_shader.frag.glsl".format(ms))
				} else {
					ShaderResource("layer-shader", "image_shader.vert.glsl", "image_shader.frag.glsl")
				})
		}
		UIRenderUtils.shaderLayer
	}

	def plainRect:TrianglesMesh = {
		if(UIRenderUtils.plainRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIRenderUtils.plainRect = new TrianglesMesh(gl, 2)
			UIRenderUtils.plainRect.setVertexPosition(0, 0, 0, 0)
			UIRenderUtils.plainRect.setVertexPosition(1, 1, 0, 0)
			UIRenderUtils.plainRect.setVertexPosition(2, 1, 1, 0)
			UIRenderUtils.plainRect.setVertexPosition(3, 0, 1, 0)
			UIRenderUtils.plainRect.setTriangle(0, 0, 1, 2)
			UIRenderUtils.plainRect.setTriangle(1, 0, 2, 3)
			UIRenderUtils.plainRect.bindShader(shaderUniform, Position -> "position")			
		}

		UIRenderUtils.plainRect
	}

	def texRect:TrianglesMesh = {
		if(UIRenderUtils.texRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIRenderUtils.texRect = new TrianglesMesh(gl, 2)
			UIRenderUtils.texRect v(0) pos(0, 0, 0) tex(0, 1)
			UIRenderUtils.texRect v(1) pos(1, 0, 0) tex(1, 1)
			UIRenderUtils.texRect v(2) pos(1, 1, 0) tex(1, 0)
			UIRenderUtils.texRect v(3) pos(0, 1, 0) tex(0, 0)
			UIRenderUtils.texRect.setTriangle(0, 0, 1, 2)
			UIRenderUtils.texRect.setTriangle(1, 0, 2, 3)
			UIRenderUtils.texRect.bindShader(shaderTex, Position -> "position", TexCoord -> "texCoords")
		}

		UIRenderUtils.texRect
	}

	def layerRect:TrianglesMesh = {
		if(UIRenderUtils.layerRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIRenderUtils.layerRect = new TrianglesMesh(gl, 2)
			UIRenderUtils.layerRect v(0) pos(0, 0, 0) tex(0, 1)
			UIRenderUtils.layerRect v(1) pos(1, 0, 0) tex(1, 1)
			UIRenderUtils.layerRect v(2) pos(1, 1, 0) tex(1, 0)
			UIRenderUtils.layerRect v(3) pos(0, 1, 0) tex(0, 0)
			UIRenderUtils.layerRect.setTriangle(0, 0, 1, 2)
			UIRenderUtils.layerRect.setTriangle(1, 0, 2, 3)
			UIRenderUtils.layerRect.bindShader(shaderTex, Position -> "position", TexCoord -> "texCoords")
		}

		UIRenderUtils.layerRect
	}

	def strokeRect:LinesMesh = {
		if(UIRenderUtils.strokeRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIRenderUtils.strokeRect = new LinesMesh(gl, 4)
			UIRenderUtils.strokeRect.setLine(0, 0,0,0, 1,0,0)
			UIRenderUtils.strokeRect.setLine(1, 1,0,0, 1,1,0)
			UIRenderUtils.strokeRect.setLine(2, 1,1,0, 0,1,0)
			UIRenderUtils.strokeRect.setLine(3, 0,1,0, 0,0,0)
			UIRenderUtils.strokeRect.bindShader(shaderUniform, Position -> "position")
		}		
		UIRenderUtils.strokeRect
	}

	def shadowUnderRect:TrianglesMesh = {
		if(UIRenderUtils.shadowUnderRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIRenderUtils.shadowUnderRect = new TrianglesMesh(gl, 2)
			UIRenderUtils.shadowUnderRect v(0) pos(0, 0, 0) clr(0, 0, 0, 0.25f)
			UIRenderUtils.shadowUnderRect v(1) pos(1, 0, 0) clr(0, 0, 0, 0.25f)
			UIRenderUtils.shadowUnderRect v(2) pos(1, 1, 0) clr(0, 0, 0, 0)
			UIRenderUtils.shadowUnderRect v(3) pos(0, 1, 0) clr(0, 0, 0, 0)
			UIRenderUtils.shadowUnderRect t(0, 0, 1, 2)
			UIRenderUtils.shadowUnderRect t(1, 0, 2, 3)
			UIRenderUtils.shadowUnderRect.bindShader(shaderColor, Position -> "position", Color -> "color")
		}
		UIRenderUtils.shadowUnderRect
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
			plainRect.draw
			shaderUniform.uniform("uniformColor", lineColor)
			strokeRect.draw
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
			plainRect.draw
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
			shadowUnderRect.draw
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
			layerRect.draw
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