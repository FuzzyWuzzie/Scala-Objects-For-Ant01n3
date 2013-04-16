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

/** A hierachical set of joints. */
class Armature(val name:String, val scale:Double, val area:Point2, val texResource:String, val shaderResource:String, val root:Joint) {

	/** Global texture containing each part of the armature, each joint is a rectangle in this texture. */
	var texture:Texture = null

	/** The shader to use to display each joint. */
	var shader:ShaderProgram = null

	/** Set of triangles, two for each joint. */
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
			camera.scaleModel(1,-1,1)
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
	def apply(name:String, z:Double, area:(Double,Double,Double,Double), pivot:(Double,Double), anchor:(Double,Double), visible:Boolean, joints:Joint*):Joint =
		new Joint(name, z, Point2(area._1, area._2), Point2(area._3, area._4), Point2(pivot._1, pivot._2), Point2(anchor._1, anchor._2), visible, joints.toArray) 
}

class Joint(val name:String, val z:Double, val fromUV:Point2, val sizeUV:Point2 ,val pivotGU:Point2, val anchorGU:Point2, var visible:Boolean, var sub:Array[Joint]) {

	/** Parent joint. */
	protected var parent:Joint = null

	/** Position in the texture image in game units. */
	protected val fromGU:Point2 = new Point2

	/** The size of the joint in game units. */
	protected val sizeGU:Point2 = new Point2

	/** The triangle index in the armature when drawing. */
	protected var triangle:Int = 0

	/** Current rotation. */
	var angle = 0.0

	def apply(id:String):Option[Joint] = sub.find(_.name == id)

	def init(parent:Joint, armature:Armature):Int = {
		// Sort all elements by their Z level to draw them in order.	
		sub = sub.sortWith({ (j0,j1) => j0.z < j1.z })

		this.parent = parent
		normalize(armature)
		var count = 1
		sub.foreach { s => count += s.init(this, armature) }
		count
	}

	/** Compute the size in texture UV (between 0 and 1) and the size in GU (game units), also
	  * resposition the pivot and anchor to be relative to this joint and the parent joint in GU. */
	protected def normalize(armature:Armature) {
		var scale = armature.scale

		// At start fromUV is in absolute pixels.
		// We scale the GU points and make all values relative to the pivot.

		fromGU.set(fromUV.x*scale, fromUV.y*scale)
		sizeGU.set(sizeUV.x*scale, sizeUV.y*scale)
		pivotGU.set((pivotGU.x*scale)-fromGU.x, (pivotGU.y*scale)-fromGU.y)
		
		if(parent ne null)
			anchorGU.set((anchorGU.x*scale)-parent.fromGU.x-(parent.pivotGU.x), (anchorGU.y*scale)-parent.fromGU.y-(parent.pivotGU.y))

		var sz = armature.area
		
		fromUV.set(fromUV.x/sz.x, fromUV.y/sz.y)
		sizeUV.set(sizeUV.x/sz.x, sizeUV.y/sz.y)
	}

	def build(armature:Armature) {
		triangle = armature.count * 2

		var tri = triangle
		var pt  = tri * 3
		val triangles = armature.triangles

		var x0 = (-pivotGU.x).toFloat
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

				if(sub.size > 0) {
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
			sizeUV.toShortString, fromGU.toShortString, sizeGU.toShortString, pivotGU.toShortString, anchorGU.toShortString,
			if(sub ne null) sub.mkString(",") else "")
}