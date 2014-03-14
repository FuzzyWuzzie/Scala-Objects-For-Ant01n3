package org.sofa.opengl.actor.renderer.avatar.ui

import org.sofa.math.{Point3, Vector3, Rgba, Box3, Box3From, Box3PosCentered, Box3Default}
import org.sofa.opengl.actor.renderer.{Screen}
import org.sofa.opengl.actor.renderer.{Avatar, DefaultAvatar, DefaultAvatarComposed, AvatarName, AvatarRender, AvatarInteraction, AvatarSpace, AvatarContainer, AvatarFactory, DefaultAvatarFactory, AvatarSpaceState, AvatarState}
import org.sofa.opengl.actor.renderer.{AvatarEvent, AvatarSpatialEvent, AvatarMotionEvent, AvatarClickEvent, AvatarLongClickEvent, AvatarKeyEvent}
import org.sofa.opengl.actor.renderer.{NoSuchAvatarException}

import org.sofa.opengl.{SGL, ShaderProgram}//, Camera, VertexArray, Texture, HemisphereLight, ResourceDescriptor, Libraries}
import org.sofa.opengl.mesh.{TrianglesMesh, Mesh, VertexAttribute, LinesMesh}//, PlaneMesh, BoneMesh, EditableMesh, VertexAttribute, LinesMesh}


class UIAvatarFactory extends DefaultAvatarFactory {
	override def avatarFor(name:AvatarName, screen:Screen, kind:String):Avatar = {
		kind.toLowerCase match {
			case "ui-root" => new UIRoot(name, screen)
			case "ui-list" => new UIList(name, screen)
			case "ui-list-item" => new UIListItem(name, screen)
			case _ => throw new NoSuchAvatarException("avatar kind %s does not exist in avatar factory 'UI'".format(kind))
		}
	}
}


// ----------------------------------------------------------------------------------------------


trait UIrenderUtils {
	var mesh:TrianglesMesh = null

	var lines:LinesMesh = null

	var shader:ShaderProgram = null

	var color = Rgba.Cyan

	var lineColor = Rgba.Red

	def self:Avatar

	def fill() {
		import VertexAttribute._

		val screen = self.screen
		val space  = screen.space
		val gl     = screen.gl

		if(shader eq null) {
			shader = ShaderProgram(gl, "uniform-color-shader", "uniform_color.vert.glsl", "uniform_color.frag.glsl")
		}

		if(mesh eq null) {
			mesh = new TrianglesMesh(2)
			mesh.setPoint(0, 0, 0, 0)
			mesh.setPoint(1, 1, 0, 0)
			mesh.setPoint(2, 1, 1, 0)
			mesh.setPoint(3, 0, 1, 0)
			mesh.setTriangle(0, 0, 1, 2)
			mesh.setTriangle(1, 0, 2, 3)
			mesh.newVertexArray(gl, shader, Vertex -> "position")

			lines = new LinesMesh(4)
			lines.setLine(0, 0,0,0, 1,0,0)
			lines.setLine(1, 1,0,0, 1,1,0)
			lines.setLine(2, 1,1,0, 0,1,0)
			lines.setLine(3, 0,1,0, 0,0,0)
			lines.newVertexArray(gl, shader, Vertex -> "position")
		}

		val subSpace = self.space.subSpace

		shader.use
		space.pushpop {
			//println(s"    | rendering ${self.name} scale(${subSpace.size(0)},${subSpace.size(1)})")
			shader.use
			shader.uniform("uniformColor", color)
			space.scale(subSpace.size(0), subSpace.size(1), 1)
			space.uniformMVP(shader)
			mesh.lastva.draw(mesh.drawAs)
			shader.uniform("uniformColor", lineColor)
			lines.lastva.draw(lines.drawAs)
		}
	}
}


class UIAvatarRender(var self:Avatar) extends AvatarRender {
	def screen:Screen = self.screen
}


class UIAvatarRenderRoot(avatar:Avatar) extends UIAvatarRender(avatar) {
	override def render() {
		val gl = screen.gl

		gl.clearColor(Rgba.White)
		gl.clear(gl.COLOR_BUFFER_BIT)

		gl.lineWidth(1f)
		gl.disable(gl.DEPTH_TEST)
		gl.enable(gl.BLEND)
		gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
		//gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)		// Premultiplied alpha

		//println(s"* render ${self.name}")
		gl.checkErrors
		super.render
	}
}


class UIAvatarRenderList(avatar:Avatar) extends UIAvatarRender(avatar) with UIrenderUtils {
	override def render() {
		//println(s"* render ${self.name}")
		self.space.pushSubSpace
		fill
		self.renderSubs
		self.space.popSubSpace		
	}
}

class UIAvatarRenderListItem(avatar:Avatar) extends UIAvatarRender(avatar) with UIrenderUtils {

	color = Rgba.White

	lineColor = Rgba.Black

	override def render() {
		//println(s"* render ${self.name}")
		self.space.pushSubSpace
		fill
		self.renderSubs
		self.space.popSubSpace		
	}
}

// ----------------------------------------------------------------------------------------------


abstract class UIAvatarSpace(var self:Avatar) extends AvatarSpace {
	def animateSpace() {}

	def changeSpace(newState:AvatarSpaceState) {}
}


class UIAvatarSpaceRoot(avatar:Avatar) extends UIAvatarSpace(avatar) {
	/** Ratiohw height / width. */
	var ratiohw = 1.0

	var scale1cm = 1.0 

	protected val fromSpace = new Box3Default {
		pos.set(0, 0, 0)
		from.set(-1, -1, -1)
		to.set(1, 1, 1)
		size.set(2, 2, 2)
	}

	protected val toSpace = new Box3Default {
		pos.set(0, 0, 0)
		from.set(0, 0, 0)
		to.set(1, 1, 1)
		size.set(1, 1, 1)
	}

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def animateSpace() {
		val screen  = avatar.screen
		val surface = screen.surface
		ratiohw     = surface.height.toDouble / surface.width.toDouble 		
		val dpc     = 37.0	// 37 dots-pixels per centimeter
		scale1cm    = 1.0 / (screen.surface.width / dpc)	// One centimeter in the sub-space equals this

		toSpace.to.set(1, 1*ratiohw, 1)
		toSpace.size.set(1, 1*ratiohw, 1)

		//println("------------------")
		//println(s"# layout root scale1cm=${scale1cm} ratiohw=${ratiohw} size(2, ${ratiohw*2}) subSize(${toSpace.size(0)}, ${toSpace.size(1)})")
		layoutSubs
	}

	def pushSubSpace() {
		val space = self.screen.space

		// we pass from x [-1..1] positive right and y [-1..1] positive top to
		// x [0..1] positive right and y [0..ratio] positive down.

		space.push
		space.translate(-1, 1, 0)
		space.scale(2, -2/ratiohw, 1)
	}

	def popSubSpace() { self.screen.space.pop }

	def layoutSubs() {
		// A layout that put each child at the same position, filling the avaiable space, one above the other.
 		var i = 0
 		self.foreachSub { sub =>
 			sub.space.thisSpace.setSize(toSpace.size(0), toSpace.size(1), 1)
 			sub.space.thisSpace.setPosition(0, 0, 0)
 			//println("    | layout %d %s".format(i, sub))
 			i += 1
 		}		
	}
}


class UIAvatarSpaceList(avatar:Avatar) extends UIAvatarSpace(avatar) {
 	var scale1cm = 1.0

 	var itemHeight = 1.5	// cm

 	var fromSpace = new Box3From {
 		from.set(0,0,0)
 		to.set(1,1,1)
 	}

 	var toSpace = new Box3From {
 		from.set(0,0,0)
 		to.set(1,1,1)
 	}

	var offsety = 0.0

 	def thisSpace = fromSpace

 	def subSpace = toSpace

	override def changeSpace(newState:AvatarSpaceState) {
		newState match {
			case AvatarOffsetState(amount) => {
				offsety += amount*0.01

				var offsetMax = -(toSpace.size.height - fromSpace.size.height)
				
				if(offsetMax > 0) offsetMax = 0

				if(offsety > 0) offsety = 0
				else if(offsety < offsetMax) offsety = offsetMax
			}
			case _ => super.changeSpace(newState)
		}
	}
 	override def animateSpace() {
 		scale1cm = self.parent.space.scale1cm

 		toSpace.to.set(1, scale1cm * itemHeight * avatar.subCount, 1)

 		//println(s"# layout list available space (${fromSpace.size(0)}, ${fromSpace.size(1)})")
 		//println(s"#        list pos(${fromSpace.pos.x}, ${fromSpace.pos.y}) scale1cm=${scale1cm} items=${avatar.subCount} size=(${toSpace.to.x}, ${toSpace.to.y})")

 		layoutSubs
 	}

 	def pushSubSpace() {
 		val space = avatar.screen.space

 		space.push
 		space.translate(thisSpace.pos.x, thisSpace.pos.y + offsety, 0)
 	}

 	def popSubSpace() {
		self.screen.space.pop
 	}

 	protected def layoutSubs() {
 		var i = 0
 		self.foreachSub { sub =>
 			sub.space.thisSpace.setSize(1, itemHeight*scale1cm, 1)
 			sub.space.thisSpace.setPosition(0, itemHeight*scale1cm*i, 0)
 			//println("    | layout %d %s".format(i, sub))
 			i += 1
 		}
 	}
}


case class AvatarOffsetState(amount:Double) extends AvatarSpaceState {}

class UIAvatarSpaceListItem(avatar:Avatar) extends UIAvatarSpace(avatar) {
	var scale1cm = 1.0

	var fromSpace = new Box3From {
		from.set(0, 0, 0)
		to.set(1, 1, 1)
	}

	var toSpace = new Box3From {
		from.set(0, 0, 0)
		to.set(1, 1, 1)
	}

	def thisSpace = fromSpace

	def subSpace = toSpace

	override def animateSpace() {
		toSpace.from.set(0, 0, 0)
		toSpace.setSize(fromSpace.size(0), fromSpace.size(1), 1)

 		scale1cm = self.parent.space.scale1cm

 		//println(s"# layout ListItem pos=${fromSpace.pos} size=${fromSpace.from} sub -> pos=${toSpace.pos} size=${toSpace.size}")
	}

	def pushSubSpace() {
 		val space = self.screen.space

 		space.push
 		space.translate(thisSpace.pos.x, thisSpace.pos.y, 0)
	}

	def popSubSpace() {
		self.screen.space.pop
	}
}


// ----------------------------------------------------------------------------------------------


abstract class UIAvatar(name:AvatarName, screen:Screen) extends DefaultAvatarComposed(name, screen) {
}


class UIRoot(name:AvatarName, screen:Screen)
	extends UIAvatar(name, screen) {

	var space = new UIAvatarSpaceRoot(this)
	
	var renderer = new UIAvatarRenderRoot(this)	

	def consumeEvent(event:AvatarEvent):Boolean = {
		println("%s ignore event %s".format(name, event))
		false
	}
}

class UIList(name:AvatarName, screen:Screen)
	extends UIAvatar(name, screen) {

	var space = new UIAvatarSpaceList(this)

	var renderer = new UIAvatarRenderList(this)

	var prevMotionEvent:AvatarMotionEvent = null

	def consumeEvent(event:AvatarEvent):Boolean = {
		event match {
			case e:AvatarMotionEvent => {
				if(e.isEnd) {
					println("%s consume event END".format(name))
					prevMotionEvent = null
				} else {
					if(prevMotionEvent ne null) {
						println("%s consume event move".format(name))
						space.changeSpace(AvatarOffsetState(e.position.y - prevMotionEvent.position.y))
					} else println("%s consume event no move".format(name))
					prevMotionEvent = e
				}
				//println("%s consume event %s".format(name, event))

				true
			}
			case _ => {
				println("%s ignore event %s".format(name, event))
				false
			}
		}
	}
}

class UIListItem(name:AvatarName, screen:Screen)
	extends UIAvatar(name, screen) {

	var space = new UIAvatarSpaceListItem(this)

	var renderer = new UIAvatarRenderListItem(this)

	def consumeEvent(event:AvatarEvent):Boolean = {
		event match {
			case e:AvatarClickEvent => {
				println("%s received event %s".format(name, event))
				false
			}
			case e:AvatarLongClickEvent => {
				println("%s received event %s".format(name, event))
				false
			}
			case _ => {
				println("%s received event %s".format(name, event))
				false
			}
		}
	}
}