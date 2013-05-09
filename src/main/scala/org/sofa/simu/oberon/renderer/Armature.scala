package org.sofa.simu.oberon.renderer

import scala.collection.mutable.HashMap

import org.sofa.opengl.{SGL, Camera}
import org.sofa.math.{Point3, Point2}
import org.sofa.opengl.{Texture, ShaderProgram}
import org.sofa.opengl.mesh.{TrianglesMesh, VertexAttribute}

object Armature {
	/** The set of armatures, added elsewhere. */
	val armatures = new HashMap[String, Armature]()

	def apply(name:String, scale:Double, area:(Double,Double), texResource:String, shaderResource:String, root:Joint):Armature = 
		new Armature(name, scale, Point2(area._1, area._2), texResource, shaderResource, root)
}

/** A hierachical set of joints, and a way to render them. */
class Armature(val name:String,
               val scale:Double,
               val area:Point2,
               val texResource:String,
               val shaderResource:String,
               val root:Joint) {

	/** Global texture containing each part of the armature, each joint is a rectangle in this texture.
	  * The origin of the joints area inside the texture is the bottom-left with positive X going
	  * right and positive Y going up. */
	var texture:Texture = null

	/** The shader to use to display each joint. */
	var shader:ShaderProgram = null

	/** Set of triangles, two for each joint. TODO, use a quad mesh. */
	var triangles:TrianglesMesh = null

	/** Number of pair of triangle (each joint uses one pair of triangles). */
	var count:Int = 0

	/** Setup the hierachical joints armature, set parents, compute the uv positions in the texture and
	  * finaly build a mesh of triangles where each joint is made of two triangles (joints are rectangular
	  * areas in the texture). */
	def init(gl:SGL, libraries:Libraries) {
		import VertexAttribute._

		texture   = libraries.textures.get(gl, texResource)
		shader    = libraries.shaders.get(gl, shaderResource)
		val count = root.init(null, this)
		triangles = new TrianglesMesh(count*2)

		root.build(this)
		assert(this.count == count)
		triangles.newVertexArray(gl, shader, Vertex -> "position", TexCoord -> "texCoords")
	}

	def display(gl:SGL, camera:Camera) {
		camera.pushpop {
			shader.use
			texture.bindUniform(gl.TEXTURE0, shader, "texColor")
			//camera.scaleModel(1,-1,1)
			root.display(gl, this, camera)
		}
	}

	override def toString():String = "Armature(%s, [%s], {%s})".format(name, area, root)
}

object Joint {
	/** Create a joint.
	  *
	  * The joint maps to an area in a  texture whose pixels are indexed with an origin at the top-left corner (as usual).
	  *
	  * @param name    The joint identifier.
	  * @param z       The z level.
	  * @param area    Four pixels, the two first are the position in absolute pixels, the two others the size of the area in the texture for this joint.
	  * @param pivot   The "gravity" center of the joint a position in absolute pixels.
	  * @param anchor  The attach point in the parent joint in absolute pixels.
	  * @param visible If true the joint will be visible at start.
	  * @param joints  Children joints. */
	def apply(name:String,
	          z:Double,
	          area:(Double,Double,Double,Double),
	          pivot:(Double,Double),
	          anchor:(Double,Double),
	          visible:Boolean,
	          joints:Joint*):Joint =
		new Joint(name, z, Point2(area._1, area._2), Point2(area._3, area._4), Point2(pivot._1, pivot._2), Point2(anchor._1, anchor._2), visible, joints.toArray) 

	def apply(name:String):Joint = {
		new Joint(name, 0.0, null, null, null, null, true, null)
	}
}

/** A rectangular area mapped to a part of the armature texture, with a pivot point,
  * and and anchor point in a parent joint.
  *
  * This class uses two units, UV means texture coordinates. They are between 0 and 1
  * whatever be the ratio of the texture. GU means game units, they are the coordinates
  * given at start to build the joint (therefore most often pixels) scaled by a ratio.
  * These coordinates conserve the aspect ratio.
  *
  * @param name     Name of the joint.
  * @param z        The position above or under other joints, allow to give a drawing order.
  * @param fromUV   Give absolute pixels, and it will then contain the position of the left-bottom corner of the joint rectangle in the texture.
  * @param sizeUV   Give pixels, and it will then contain the size of the joint rectangle in the texture (the aspect ratio is not conserved).
  * @param pivotGU  Give absolute pixels, and it will then contain the position of the pivot point relative to (0,0).
  * @param anchorGU Give absolute pixels, and it will then contain the position of the anchor relative to the parent (0,0).
  * @param visible  Is the joint visible at start ?
  * @param sub      The sub joints, can be null.
  */
class Joint(val name:String,
            var z:Double,
            var fromUV:Point2,
            var sizeUV:Point2,
            var pivotGU:Point2,
            var anchorGU:Point2,
            var visible:Boolean,
            var sub:Array[Joint]) {

	/** Parent joint. */
	protected var parent:Joint = null

	/** Position in the texture image in game units. */
	protected val fromGU:Point2 = new Point2

	/** The size of the joint in game units. Preserve the ratio. */
	protected val sizeGU:Point2 = new Point2

	/** The triangle index in the armature when drawing. */
	protected var triangle:Int = 0

	/** Current rotation. */
	var angle = 0.0

	def apply(id:String):Option[Joint] = if(sub ne null) sub.find(_.name == id) else None

	def init(parent:Joint, armature:Armature):Int = {
		// Sort all elements by their Z level to draw them in order.	
		
		if(sub ne null)
			sub = sub.sortWith { (j0,j1) => j0.z < j1.z }

		this.parent = parent

		// Normalize the coordinates in the texture (that are in pixels) into UV coordinates (between 0 and 1).

		normalize(armature)
		
		var count = 1
		
		if(sub ne null)
			sub.foreach { s => count += s.init(this, armature) }
		
		count
	}

	/** Compute the size in texture UV (between 0 and 1) and the size in GU (game units), also
	  * resposition the pivot and anchor to be relative to this joint and the parent joint in GU. */
	protected def normalize(armature:Armature) {
		var scale = armature.scale

		// At start, fromUV is in absolute pixels.
		// We scale the GU points and make all values relative to the pivot.

		fromGU.set(fromUV.x*scale, fromUV.y*scale)
		sizeGU.set(sizeUV.x*scale, sizeUV.y*scale)
		pivotGU.set((pivotGU.x*scale)-fromGU.x, (pivotGU.y*scale)-fromGU.y)
		
		if(parent ne null) {
			// The anchor is set according to the parent pivot point.
			anchorGU.set(
				(anchorGU.x*scale)-parent.fromGU.x-(parent.pivotGU.x),
				(anchorGU.y*scale)-parent.fromGU.y-(parent.pivotGU.y))
		}

		// Finally the UV are set between 0 and 1.

		var sz = armature.area
		
		fromUV.set(fromUV.x/sz.x, fromUV.y/sz.y)
		sizeUV.set(sizeUV.x/sz.x, sizeUV.y/sz.y)
	}

	def build(armature:Armature) {
		triangle      = armature.count * 2
		var tri       = triangle
		var pt        = tri * 3
		val triangles = armature.triangles

		var x0 = (-pivotGU.x).toFloat	// Pivot at (0.0)
		var y0 = (-pivotGU.y).toFloat
		var x1 = x0 + sizeGU.x.toFloat
		var y1 = y0 + sizeGU.y.toFloat
		var z  = this.z.toFloat

		triangles.setPoint(pt+0, x0, y0, z)
		triangles.setPoint(pt+1, x1, y1, z)
		triangles.setPoint(pt+2, x1, y0, z)
		triangles.setPoint(pt+3, x0, y0, z)
		triangles.setPoint(pt+4, x0, y1, z)
		triangles.setPoint(pt+5, x1, y1, z)

		x0 = fromUV.x.toFloat
		y0 = fromUV.y.toFloat
		x1 = x0 + sizeUV.x.toFloat
		y1 = y0 + sizeUV.y.toFloat

		triangles.setPointTexCoord(pt+0, x0, y0)
		triangles.setPointTexCoord(pt+1, x1, y1)
		triangles.setPointTexCoord(pt+2, x1, y0)
		triangles.setPointTexCoord(pt+3, x0, y0)
		triangles.setPointTexCoord(pt+4, x0, y1)
		triangles.setPointTexCoord(pt+5, x1, y1)

		triangles.setTriangle(tri+0, pt+0, pt+1, pt+2)
		triangles.setTriangle(tri+1, pt+3, pt+4, pt+5)

		armature.count += 1

		if(sub ne null)
			sub.foreach { _.build(armature) }
	}

	def display(gl:SGL, armature:Armature, camera:Camera) {
		if(visible) {
			// Draw all elements in Z order. We have to draw ourself 
			// at the correct place, hence the complicated tests.

			camera.pushpop {
				camera.translateModel(anchorGU.x, anchorGU.y, 0)
				if(angle != 0) {
					camera.rotateModel(angle, 0, 0, 1)
				}

				if((sub ne null) && sub.size > 0) {
					var ok = true
		
					if(z < sub(0).z) {
						displaySelf(armature, camera)
						ok = false
					}

					sub.foreach { s =>
						if(z < s.z && ok) {
							displaySelf(armature, camera)
							ok = false 
						}

						s.display(gl, armature, camera)
					}

					if(ok) {
						displaySelf(armature, camera)
					}
				} else {
					camera.setUniformMVP(armature.shader)
					armature.triangles.lastVertexArray.drawArrays(armature.triangles.drawAs, triangle*3, 2*3)
				}
			}
		}
	}

	protected def displaySelf(armature:Armature, camera:Camera) {
		camera.setUniformMVP(armature.shader)
		armature.triangles.lastVertexArray.drawArrays(armature.triangles.drawAs, triangle*3, 2*3)		
	}

	override def toString():String = "Joint(%s(%.2f) [%s->%s]UV [%s->%s]GU (%s)GU <%s>GU {%s})".format(name, z, fromUV.toShortString,
			sizeUV.toShortString, fromGU.toShortString, sizeGU.toShortString, pivotGU.toShortString, if(anchorGU ne null )anchorGU.toShortString else "noAnchor",
			if(sub ne null) sub.mkString(",") else "")
}