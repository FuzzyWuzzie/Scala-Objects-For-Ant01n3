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


trait UIRenderUtils {

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

			UIrenderUtils.plainRect = new TrianglesMesh(gl, 2)
			UIrenderUtils.plainRect.setVertexPosition(0, 0, 0, 0)
			UIrenderUtils.plainRect.setVertexPosition(1, 1, 0, 0)
			UIrenderUtils.plainRect.setVertexPosition(2, 1, 1, 0)
			UIrenderUtils.plainRect.setVertexPosition(3, 0, 1, 0)
			UIrenderUtils.plainRect.setTriangle(0, 0, 1, 2)
			UIrenderUtils.plainRect.setTriangle(1, 0, 2, 3)
			UIrenderUtils.plainRect.bindShader(shaderUniform, Position -> "position")			
		}

		UIrenderUtils.plainRect
	}

	def texRect:TrianglesMesh = {
		if(UIrenderUtils.texRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIrenderUtils.texRect = new TrianglesMesh(gl, 2)
			UIrenderUtils.texRect v(0) pos(0, 0, 0) tex(0, 1)
			UIrenderUtils.texRect v(1) pos(1, 0, 0) tex(1, 1)
			UIrenderUtils.texRect v(2) pos(1, 1, 0) tex(1, 0)
			UIrenderUtils.texRect v(3) pos(0, 1, 0) tex(0, 0)
			UIrenderUtils.texRect.setTriangle(0, 0, 1, 2)
			UIrenderUtils.texRect.setTriangle(1, 0, 2, 3)
			UIrenderUtils.texRect.bindShader(shaderTex, Position -> "position", TexCoord -> "texCoords")
		}

		UIrenderUtils.texRect
	}

	def layerRect:TrianglesMesh = {
		if(UIrenderUtils.layerRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIrenderUtils.layerRect = new TrianglesMesh(gl, 2)
			UIrenderUtils.layerRect v(0) pos(0, 0, 0) tex(0, 1)
			UIrenderUtils.layerRect v(1) pos(1, 0, 0) tex(1, 1)
			UIrenderUtils.layerRect v(2) pos(1, 1, 0) tex(1, 0)
			UIrenderUtils.layerRect v(3) pos(0, 1, 0) tex(0, 0)
			UIrenderUtils.layerRect.setTriangle(0, 0, 1, 2)
			UIrenderUtils.layerRect.setTriangle(1, 0, 2, 3)
			UIrenderUtils.layerRect.bindShader(shaderTex, Position -> "position", TexCoord -> "texCoords")
		}

		UIrenderUtils.layerRect
	}

	def strokeRect:LinesMesh = {
		if(UIrenderUtils.strokeRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIrenderUtils.strokeRect = new LinesMesh(gl, 4)
			UIrenderUtils.strokeRect.setLine(0, 0,0,0, 1,0,0)
			UIrenderUtils.strokeRect.setLine(1, 1,0,0, 1,1,0)
			UIrenderUtils.strokeRect.setLine(2, 1,1,0, 0,1,0)
			UIrenderUtils.strokeRect.setLine(3, 0,1,0, 0,0,0)
			UIrenderUtils.strokeRect.bindShader(shaderUniform, Position -> "position")
		}		
		UIrenderUtils.strokeRect
	}

	def shadowUnderRect:TrianglesMesh = {
		if(UIrenderUtils.shadowUnderRect eq null) {
			import VertexAttribute._	
			val gl = self.screen.gl

			UIrenderUtils.shadowUnderRect = new TrianglesMesh(gl, 2)
			UIrenderUtils.shadowUnderRect v(0) pos(0, 0, 0) clr(0, 0, 0, 0.25f)
			UIrenderUtils.shadowUnderRect v(1) pos(1, 0, 0) clr(0, 0, 0, 0.25f)
			UIrenderUtils.shadowUnderRect v(2) pos(1, 1, 0) clr(0, 0, 0, 0)
			UIrenderUtils.shadowUnderRect v(3) pos(0, 1, 0) clr(0, 0, 0, 0)
			UIrenderUtils.shadowUnderRect t(0, 0, 1, 2)
			UIrenderUtils.shadowUnderRect t(1, 0, 2, 3)
			UIrenderUtils.shadowUnderRect.bindShader(shaderColor, Position -> "position", Color -> "color")
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