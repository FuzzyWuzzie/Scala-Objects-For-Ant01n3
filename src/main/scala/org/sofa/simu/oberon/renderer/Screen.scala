package org.sofa.simu.oberon.renderer

import scala.collection.mutable.{HashMap, HashSet}
import akka.actor.{ActorRef}

import org.sofa.math.{Rgba, Axes, AxisRange, Point3, Vector3, NumberSeq3, SpatialHash, SpatialObject, SpatialPoint}
import org.sofa.opengl.{Camera, Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, LinesMesh, VertexAttribute}
import org.sofa.opengl.surface.{MotionEvent}

/** Sent when a screen is not found. */
case class NoSuchScreenException(msg:String) extends Exception(msg)

/** A screen in the game.
  *
  * A screen is necessarily (actually) occupying the whole display.
  * Only one screen is active and rendered at a time.
  */
abstract class Screen(val name:String, val renderer:Renderer) extends Renderable {

	/** The spatial index used in screens to retrieve indexed avatars. */
	type SpatialIndex = SpatialHash[SpatialObject, SpatialPoint, AvatarIndex]

	/** OpenGL. */
	val gl = renderer.gl

	/** Frame buffer. */
	val surface = renderer.surface

	/** User space in this screen. */
	var axes = Axes(AxisRange(-1,1), AxisRange(-1,1), AxisRange(-1,1))

	/** Allow to move the view in this screen. */
	val camera = new Camera()

	/** Set of child avatars. */
	protected val avatars = new HashMap[String,Avatar]()

	/** Allow to quicly find objects at a given position, and detect collisions. */
	protected val spash = new SpatialIndex(axes.y.length / 20.0)

	/** Set to true after begin() and reset to false after end(). */
	protected var rendering = false

	// Acccess

	/** Width of the screen in game units. This is the maximum visible space, independant of any camera zoom. */
	def width:Double = axes.x.length

	/** Height of the screen in game units. This is the maximum visible space, independant of any camera zoom. */
	def height:Double = axes.y.length

	// Modification

	/** Something changed in the screen. */
	def change(axis:String, values:AnyRef*)

	/** Change the axes and therefore the size of the drawing space in the screen (not related
	  * with the real pixel width). */
	def changeAxes(newAxes:Axes) {
		axes = newAxes

		if(rendering) reshape
	}

	// Interaction Events

	/** The screen has been touched. */
	def motion(e:MotionEvent) {
		val things = new HashSet[AvatarIndex]()
		val (xx, yy) = px2gu(e.x, e.y)

		spash.getThings(null, things, xx - 0.001, yy - 0.001, xx + 0.001, yy + 0.001)
		
		things.filter(thing ⇒ thing.contains(xx, yy, 0)).foreach { thing ⇒
			thing.touched(xx, yy, 0, e.isStart, e.isEnd)
		}
	}

	// Renderable

	/** Set the `rendering` flag to true and send a begin signal to all child avatars. */
	def begin() {
		rendering = true
		beginAvatars
	}	

	/** By default renders all the child avatars. */
	def render() {
		renderAvatars
	}

	/** By default sets the size of the viewport to the size in pixels of the surface. */
	def reshape() {
		camera.viewportPx(surface.width, surface.height)
		gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
	}

	/** By default animate each avatar. */
	def animate() {
		animateAvatars
	}

	/** By default send the end signal to all child avatars. */
	def end() {
		endAvatars
		rendering = false
	}

	// Avatars.

	/** Add an avatar (and send it the begin signal if the screen is rendering). */
	def addAvatar(name:String, avatar:Avatar) {
		avatars += (name -> avatar)

		if(avatar.isIndexed) {
			avatar.index.spash = spash
			spash += avatar.index
		}

		if(rendering) avatar.begin
	}

	/** Remove and avatar (and send it the end signal if the screen is rendering).
	  * Does nothing if the avatar does not exist. */
	def removeAvatar(name:String) { 
		avatars.get(name).foreach { avatar ⇒
			avatar.end
			
			if(avatar.isIndexed) {
				spash -= avatar.index
				avatar.index.spash = null
			}
			
			avatars -= name
		}
	}

	/** An avatar changed position. */
	def changeAvatarPosition(name:String, newPos:NumberSeq3) {
		avatar(name).changePosition(newPos)
	}

	/** An avatar changed size. */
	def changeAvatarSize(name:String, newSize:NumberSeq3) {
		avatar(name).changeSize(newSize)
	}

	/** Something changed in an avatar of this screen. */
	def changeAvatar(name:String, state:AvatarState) {
		avatar(name).change(state)
	}

	/** Ask the avatar `name` to send events to `acquaintance`. */
	def addAvatarAcquaintance(name:String, acquaintance:ActorRef) {
		avatar(name).addAcquaintance(acquaintance)
	}

	/** Get an avatar by its name. */
	def avatar(name:String):Avatar = avatars.get(name).getOrElse(throw NoSuchAvatarException("screen %s does not contain avatar %s".format(this.name,name)))

	// For implementers.

	/** Initialize the avatars. */
	protected def beginAvatars() { avatars.foreach { _._2.begin } }

	/** Render the avatars. */
	protected def renderAvatars() { avatars.foreach { _._2.render } }

	/** Animate the avatars. */
	protected def animateAvatars() { avatars.foreach { _._2.animate } }

	/** Finalize the avatars. */
	protected def endAvatars() { avatars.foreach { _._2.end } }

	// Utility

	/** Pass from pixels to game units. */
	protected def px2gu(x:Double, y:Double):(Double, Double) = {
		(axes.x.from + (axes.x.length * (x / camera.viewportPx(0))),
		 axes.y.from + (axes.y.length * (1-(y / camera.viewportPx(1))))

		 )
	}
}

/** A screen where a 2D set of tiles are arranged in a zoomable grid. */
class TileScreen(name:String, renderer:Renderer) extends Screen(name, renderer) {
	/** Color for parts not covered by the background image. */
	val clearColor = Rgba(1, 0, 0, 1)

	// /** The background plane. */
	// val backgroundMesh = new PlaneMesh(2, 2, 1, 1, true)

	// /** The background shader. */
	// var backgroundShader:ShaderProgram =null

	// /** The background imagE. */
	// var background:Texture = null

	var w:Double = 1

	var h:Double = 1

	// == Debug ==============================

	var debug = true	

	var gridShader:ShaderProgram = null
	
	val grid = new LinesMesh(40)

	// == Access ============================

	override def width:Double = w

	override def height:Double = h

	// == Avatar ============================

	override def begin() {
		gl.clearColor(clearColor)
		gl.clearDepth(1f)
	    
	    gl.disable(gl.DEPTH_TEST)
	    
	    gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CW)
        
        gl.disable(gl.BLEND)
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)

        beginShader
        beginGeometry

		super.begin

        reshape
	}

	protected def beginShader() {
//		backgroundShader = renderer.libraries.shaders.get(gl, "image-shader")
		gridShader = renderer.libraries.shaders.get(gl, "plain-shader")
	}

	protected def beginGeometry() {
		import VertexAttribute._
  //      backgroundMesh.newVertexArray(gl, backgroundShader, Vertex -> "position", TexCoord -> "texCoords")

		grid.setXYGrid(1f, 1f, 0f, 0f, 20, 20, spash.bucketSize.toFloat, spash.bucketSize.toFloat, Rgba(0.7, 0.2, 0.9, 0.5))
		grid.newVertexArray(gl, gridShader, Vertex -> "position", Color -> "color")
	}

	def change(axis:String, values:AnyRef*) {
		axis match {
			// case "background-image" ⇒ {
			// 	if(values(0).isInstanceOf[String]) {
			// 		background = renderer.libraries.textures.get(gl, values(0).asInstanceOf[String])
			// 		h = axes.y.length
			// 		w = h * background.ratio
			// 	}
			// }
			case _ ⇒ {
				throw NoSuchAxisException(axis)
			}
		}
	}

	override def render() {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		renderBackground
		//super.render
		// In order to handle all the transparencies we have
		// to sort elements. Therefore, we can disable the
		// depth test.
		val sorted = avatars.toArray.sortWith(_._2.pos.z < _._2.pos.z)
		sorted.foreach { _._2.render }
	}

	override def reshape() {
		super.reshape
		val ratio = camera.viewportRatio
		camera.orthographic(axes.x.from*(ratio), axes.x.to*(ratio), axes.y.from, axes.y.to, axes.z.to, axes.z.from)
	}

	override def animate() {
		super.animate
	}

	override def end() {
		super.end
	}

	/** Render the background image (if any). */
	protected def renderBackground() {
		// Origin is in the middle of the screen and of the image.
		// if(background ne null) {
		// 	backgroundShader.use
		// 	background.bindUniform(gl.TEXTURE0, backgroundShader, "texColor")
		// 	camera.pushpop {
		// 		camera.scaleModel(w, h, 1)
		// 		camera.setUniformMVP(backgroundShader)
		// 		backgroundMesh.lastVertexArray.draw(backgroundMesh.drawAs)
		// 	}
		// }
		if(debug) {
			renderGrid
		}
	}

	/** Render a grid alligned with the spash. */
	protected def renderGrid() {
		gridShader.use
		camera.setUniformMVP(gridShader)
		grid.lastVertexArray.draw(grid.drawAs)
	}
	
	/** Pass from pixels to game units. */
	override protected def px2gu(x:Double, y:Double):(Double, Double) = {
		val ratio   = camera.viewportRatio
		val xfrom   = axes.x.from * ratio
		val xlength = ((axes.x.to * ratio) - xfrom)

		(xfrom       + (xlength       * (x / camera.viewportPx(0))),
		 axes.y.from + (axes.y.length * (1-(y / camera.viewportPx(1)))))
	}
}

/** A screen where an image serve as the background and several button sprites
  * can be added.
  *
  * The screen dimensions are given by the axes (user units) and the screen viewport in pixels.
  * The height of the screen in user units, is the one given. The width of the screen in user
  * units is the one given times the ratio width/height in pixels of the screen. This ensures
  * The full height is always visible, and most of the time, more than the width is visible as
  * screens will be used un landscape mode. */
class MenuScreen(name:String, renderer:Renderer) extends Screen(name, renderer) {
	/** Color for parts not covered by the background image. */
	val clearColor = Rgba(1, 0, 0, 1)

	/** The background plane. */
	val backgroundMesh = new PlaneMesh(2, 2, 1, 1, true)

	/** The background shader. */
	var backgroundShader:ShaderProgram =null

	/** The background imagE. */
	var background:Texture = null

	var w:Double = 1

	var h:Double = 1

	// == Debug ==============================

	var debug = true	

	var gridShader:ShaderProgram = null
	
	val grid = new LinesMesh(40)

	// == Access ============================

	override def width:Double = w

	override def height:Double = h

	// == Avatar ============================

	override def begin() {
		gl.clearColor(clearColor)
		gl.clearDepth(1f)
	    
	    gl.disable(gl.DEPTH_TEST)
	    
	    gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CW)
        
        gl.disable(gl.BLEND)
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)

        beginShader
        beginGeometry

		super.begin

        reshape
	}

	protected def beginShader() {
		backgroundShader = renderer.libraries.shaders.get(gl, "image-shader")
		gridShader = renderer.libraries.shaders.get(gl, "plain-shader")
	}

	protected def beginGeometry() {
		import VertexAttribute._
        backgroundMesh.newVertexArray(gl, backgroundShader, Vertex -> "position", TexCoord -> "texCoords")

		grid.setXYGrid(1f, 1f, 0f, 0f, 20, 20, spash.bucketSize.toFloat, spash.bucketSize.toFloat, Rgba(0.7, 0.2, 0.9, 0.5))
		grid.newVertexArray(gl, gridShader, Vertex -> "position", Color -> "color")
	}

	def change(axis:String, values:AnyRef*) {
		axis match {
			case "background-image" ⇒ {
				if(values(0).isInstanceOf[String]) {
					background = renderer.libraries.textures.get(gl, values(0).asInstanceOf[String])
					h = axes.y.length
					w = h * background.ratio
				}
			}
			case _ ⇒ {
				throw NoSuchAxisException(axis)
			}
		}
	}

	override def render() {
		gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
		renderBackground
		//super.render
		// In order to handle all the transparencies we have
		// to sort elements. Therefore, we can disable the
		// depth test.
		val sorted = avatars.toArray.sortWith(_._2.pos.z < _._2.pos.z)
		sorted.foreach { _._2.render }
	}

	override def reshape() {
		super.reshape
		val ratio = camera.viewportRatio
		camera.orthographic(axes.x.from*(ratio), axes.x.to*(ratio), axes.y.from, axes.y.to, axes.z.to, axes.z.from)
	}

	override def animate() {
		super.animate
	}

	override def end() {
		super.end
	}

	/** Render the background image (if any). */
	protected def renderBackground() {
		// Origin is in the middle of the screen and of the image.
		if(background ne null) {
			backgroundShader.use
			background.bindUniform(gl.TEXTURE0, backgroundShader, "texColor")
			camera.pushpop {
				camera.scaleModel(w, h, 1)
				camera.setUniformMVP(backgroundShader)
				backgroundMesh.lastVertexArray.draw(backgroundMesh.drawAs)
			}
		}
		if(debug) {
			renderGrid
		}
	}

	/** Render a grid alligned with the spash. */
	protected def renderGrid() {
		gridShader.use
		camera.setUniformMVP(gridShader)
		grid.lastVertexArray.draw(grid.drawAs)
	}
	
	/** Pass from pixels to game units. */
	override protected def px2gu(x:Double, y:Double):(Double, Double) = {
		val ratio   = camera.viewportRatio
		val xfrom   = axes.x.from * ratio
		val xlength = ((axes.x.to * ratio) - xfrom)

		(xfrom       + (xlength       * (x / camera.viewportPx(0))),
		 axes.y.from + (axes.y.length * (1-(y / camera.viewportPx(1)))))
	}
}