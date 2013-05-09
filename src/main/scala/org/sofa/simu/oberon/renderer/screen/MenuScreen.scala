package org.sofa.simu.oberon.renderer.screen

import scala.collection.mutable.{HashMap, HashSet}
import akka.actor.{ActorRef}

import org.sofa.math.{Rgba, Axes, AxisRange, Point3, Vector3, NumberSeq3, SpatialHash, SpatialObject, SpatialPoint}
import org.sofa.opengl.{Camera, Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, LinesMesh, VertexAttribute}
import org.sofa.opengl.surface.{MotionEvent}
import org.sofa.simu.oberon.renderer.{Screen, Renderer, NoSuchAxisException}

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
		gridShader = renderer.libraries.shaders.get(gl, "plain-shader")
	}

	protected def beginGeometry() {
		import VertexAttribute._
        backgroundMesh.newVertexArray(gl, backgroundShader, Vertex -> "position", TexCoord -> "texCoords")

        setGrid
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

	override def changeAxes(newAxes:Axes, spashUnit:Double) {
		super.changeAxes(newAxes, spashUnit)

		if(background ne null) {
			h = axes.y.length
			w = h * background.ratio
		} else {
  			w = axes.x.length
  			h = axes.y.length
  		}

		setGrid
	}

  	protected def setGrid() {
		import VertexAttribute._

		if(grid.lastVertexArray ne null) {
			grid.lastVertexArray.dispose
		}

		grid.setXYGrid((w/2).toFloat, (h/2).toFloat, 0f, 0f, (w/spash.bucketSize).toInt, (h/spash.bucketSize).toInt, spash.bucketSize.toFloat, spash.bucketSize.toFloat, Rgba(0.7, 0.2, 0.9, 0.5))
		grid.newVertexArray(gl, gridShader, Vertex -> "position", Color -> "color")
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
	override protected def positionPX2GU(x:Double, y:Double):(Double, Double) = {
		val ratio   = camera.viewportRatio
		val xfrom   = axes.x.from * ratio
		val xlength = ((axes.x.to * ratio) - xfrom)

		(xfrom       + (xlength       * (   x / camera.viewportPx(0))),
		 axes.y.from + (axes.y.length * (1-(y / camera.viewportPx(1)))))
	}
}