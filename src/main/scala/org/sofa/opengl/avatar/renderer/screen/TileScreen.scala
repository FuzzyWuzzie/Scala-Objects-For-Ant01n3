package org.sofa.opengl.avatar.renderer.screen

import scala.collection.mutable.{HashMap, HashSet}
import akka.actor.{ActorRef}

import org.sofa.math.{Rgba, Axes, AxisRange, Point3, Vector3, NumberSeq3, SpatialHash, SpatialObject, SpatialPoint, Point2, Vector2}
import org.sofa.opengl.{Camera, Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, LinesMesh, VertexAttribute, TrianglesMesh}
import org.sofa.opengl.surface.{MotionEvent}
import org.sofa.opengl.avatar.renderer.{Screen, Renderer, NoSuchScreenStateException, ScreenState}

import scala.math._

/** The instantaneous motion of the touch pointer. */
class TouchMotion {
	val start = Point2(0, 0)
	
	val end = Point2(0, 0)

	def start(x:Double, y:Double) { end.set(x, y) }

	def motion(x:Double, y:Double) { start.copy(end); end.set(x, y) }

	def end(x:Double, y:Double) { motion(x, y) }

	def delta:Vector2 = start --> end
}

object TileScreen {
	case class BackgroundImage(resource:String) extends ScreenState
}

/** A screen where a 2D set of tiles are arranged in a zoomable grid. */
class TileScreen(name:String, renderer:Renderer) extends Screen(name, renderer) {
	import TileScreen._

	/** Color for parts not covered by the background image. */
	val clearColor = Rgba(0.9, 0.9, 0.9, 1)

	/** The background plane. */
	var backgroundMesh:PlaneMesh = null

	/** The background shadow. */
	var bgShadowMesh = new TrianglesMesh(16)

	/** The background shader. */
	var backgroundShader:ShaderProgram =null

	/** The background shadow shader. */
	var bgShadowShader:ShaderProgram = null

	/** The background imagE. */
	var background:Texture = null

	// == Level size, position and zoom ========================

	/** Size of the level in game units along the X axis. On game unit in level means one tile. */
	var w:Double = 1

	/** Size of the level in game units along the Y axis. On game unit in level means one tile. */
	var h:Double = 1

	/** Offset along the X axis from the initial origin (center of the level). */
	var offx:Double = 0

	/** Offset laong the Y axis from the initial origin (center of the level). */
	var offy:Double = 0

	/** Zoom value. 1 Means the whole level is visible (should not allow to go under). Larger
	  * values allow to zoom in. */
	var zoom:Double = 1

	// == Debug ==============================

	var debug = true	

	var imageShader:ShaderProgram = null

	var gridShader:ShaderProgram = null
	
	val grid = new LinesMesh(100)

	// == Access ============================

	override def width:Double = w

	override def height:Double = h

	// == Avatar ============================

	override def begin() {
		gl.clearColor(clearColor)
		gl.clearDepth(1f)
	    
	    gl.disable(gl.DEPTH_TEST)
	    
//		gl.enable(gl.CULL_FACE)
//		gl.cullFace(gl.BACK)
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
		gridShader       = renderer.libraries.shaders.get(gl, "plain-shader")
		bgShadowShader   = gridShader
	}

	override def changeAxes(newAxes:Axes, spashUnit:Double) {
		super.changeAxes(newAxes, spashUnit)

  		w = axes.x.length.toFloat
  		h = axes.y.length.toFloat

  		setBgShadow
		setGrid
	}

	protected def beginGeometry() {
		import VertexAttribute._

		setBgShadow
  		setGrid  	    
	}

  	protected def setGrid() {
		import VertexAttribute._

		grid.setXYGrid((w/2).toFloat, (h/2).toFloat, 0f, 0f, w.toInt, h.toInt, spash.bucketSize.toFloat, spash.bucketSize.toFloat, Rgba(1, 1, 1, 0.1))
		
		if(grid.lastVertexArray eq null)
			 grid.newVertexArray(gl, gridShader, Vertex -> "position", Color -> "color")
		else grid.updateVertexArray(gl)
	}

	protected def setBgShadow() {
		import VertexAttribute._

		var x0 = axes.x.from
		var x1 = axes.x.to
		var y0 = axes.y.from
		var y1 = axes.y.to
		var xlen = axes.x.length/1.8
		var ylen = axes.y.length/2

		// Four first points around the drawing area (axes), from bottom-left in CCW.
		var i = 0
		val c0 = Rgba(0,0,0,0)
		val c1 = Rgba(0,0,0,0.5)
		Array((x0,y0,c0),(x1,y0,c0),(x1,y1,c0),(x0,y1,c0),
		      (x0-xlen,y0-ylen,c1), (x0,     y0-ylen,c1), (x1,     y0-ylen,c1),
		      (x1+xlen,y0-ylen,c1), (x1+xlen,y0,     c1), (x1+xlen,y1,     c1),
		      (x1+xlen,y1+ylen,c1), (x1,     y1+ylen,c1), (x0,     y1+ylen,c1),
		      (x0-xlen,y1+ylen,c1), (x0-xlen,y1,     c1), (x0-xlen,y0,     c1)) foreach { pt =>
			bgShadowMesh.setPoint(i, pt._1.toFloat, pt._2.toFloat, 0f)
			bgShadowMesh.setPointColor(i, pt._3)
			i += 1
		}

		i = 0
		// Array((15, 0, 5),(15, 5, 4),( 0, 6, 5),( 0, 1, 6),
		// 	  ( 1, 8, 6),( 6, 8, 7),( 2, 8, 1),( 2, 9, 8),
		// 	  (11, 9, 2),(11,10, 9),(12,11, 2),(12, 2, 3),
		// 	  (13,12,14),(14,12, 3),(14, 0,15),(14, 3, 0)) foreach { tri =>
		Array((15, 0, 4),( 0, 5, 4),( 0, 6, 5),( 0, 1, 6),
			  ( 1, 7, 6),( 1, 8, 7),( 2, 8, 1),( 2, 9, 8),
			  (11,10, 2),( 2,10, 9),(12,11, 2),(12, 2, 3),
			  (13,12, 3),(13, 3,14),(14, 0,15),(14, 3, 0)) foreach { tri =>
			bgShadowMesh.setTriangle(i, tri._1, tri._2, tri._3)
			i += 1
		}

		if(bgShadowMesh.lastVertexArray eq null) 
		     bgShadowMesh.newVertexArray(gl, bgShadowShader, Vertex -> "position", Color -> "color")
		else bgShadowMesh.updateVertexArray(gl)
	}

	def change(state:ScreenState) {
		state match {
			case BackgroundImage(res) ⇒ {
				val w = (max(axes.x.length, axes.y.length) * 3).toInt

				background = renderer.libraries.textures.get(gl, res)

				if(backgroundMesh ne null) {
					backgroundMesh.lastVertexArray.dispose
				}

				backgroundMesh = new PlaneMesh(2, 2, w, w, true)
				backgroundMesh.setTextureRepeat(w, w)
			  	backgroundMesh.newVertexArray(gl, backgroundShader, VertexAttribute.Vertex -> "position", VertexAttribute.TexCoord -> "texCoords")
			}
			case _ ⇒ {
				throw NoSuchScreenStateException(state)
			}
		}
	}

	override def pinch(amount:Int) {
		zoom += (amount*0.005)	

		if(zoom < 0.1) zoom = 0.1
		if(zoom > 1) zoom = 1

		reshape
	}

	val touchMotion = new TouchMotion()

	override def motion(e:MotionEvent):Boolean = {
		if(!super.motion(e)) {
			if     (e.isStart) touchMotion.start( e.x, camera.viewportPx.y-e.y)
			else if(e.isEnd)   touchMotion.end(   e.x, camera.viewportPx.y-e.y)
			else               touchMotion.motion(e.x, camera.viewportPx.y-e.y)

			if(!e.isStart) {
				val delta = touchMotion.delta

				offx -= lengthPX2GU(delta.x)
				offy -= lengthPX2GU(delta.y)

				if(     offx > axes.x.to)   offx = axes.x.to
				else if(offx < axes.x.from) offx = axes.x.from
				if(     offy > axes.y.to)   offy = axes.y.to
				else if(offy < axes.y.from) offy = axes.y.from

				resetCamera
			}
			false
		} else {
			true
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

		resetCamera
	}

	protected def resetCamera() {
		val ratio = camera.viewportRatio
		val xfrom = (axes.y.from * ratio * zoom) + offx
		val xto   = (axes.y.to   * ratio * zoom) + offx
		val yfrom = (axes.y.from         * zoom) + offy
		val yto   = (axes.y.to           * zoom) + offy

		// Works because offx and offy are always expressed in GU.

		camera.orthographic(xfrom, xto, yfrom, yto, axes.z.to, axes.z.from)
	}

	override def animate() {
		super.animate
	}

	override def end() {
		super.end
	}

	/** Render the background image (if any). */
	protected def renderBackground() {
		//Origin is in the middle of the screen and of the image.
		if(background ne null) {
			backgroundShader.use
			background.bindUniform(gl.TEXTURE0, backgroundShader, "texColor")
			camera.uniformMVP(backgroundShader)
			backgroundMesh.lastVertexArray.draw(backgroundMesh.drawAs)
	
			gl.enable(gl.BLEND)
			bgShadowShader.use
			camera.uniformMVP(bgShadowShader)
			bgShadowMesh.lastVertexArray.draw(bgShadowMesh.drawAs)
			gl.disable(gl.BLEND)
		}
		if(debug) {
			renderGrid
		}
	}

	/** Render a grid alligned with the spash. */
	protected def renderGrid() {
		gl.enable(gl.BLEND)
		gridShader.use
		camera.uniformMVP(gridShader)
		grid.lastVertexArray.draw(grid.drawAs)
		gl.disable(gl.BLEND)
	}
	
	/** Pass from pixels to game units. */
	override protected def positionPX2GU(x:Double, y:Double):(Double, Double) = {
		// TODO handle zoom and offset.
		val ratio   = camera.viewportRatio
		val xfrom   = ( axes.x.from * ratio         ) * zoom + offx
		val xlength = ((axes.x.to   * ratio) - xfrom) * zoom
		val yfrom   = ( axes.y.from                 ) * zoom + offy
		val ylength = ( axes.y.length               ) * zoom

		(xfrom + (xlength * (   x / camera.viewportPx(0))),
		 yfrom + (ylength * (1-(y / camera.viewportPx(1)))))
	}


	/** Transform a length from pixels to game units. */
	override protected def lengthPX2GU(length:Double):Double = {
		// By default we map lengths on the Y (the X can be larger since we use a rectangular aspect ratio in landscape).
		(length * (axes.y.length / camera.viewportPx.y)) * zoom
	}
}