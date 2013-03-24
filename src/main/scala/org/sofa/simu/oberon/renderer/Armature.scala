package org.sofa.simu.oberon.renderer

import scala.collection.mutable.HashMap

import org.sofa.opengl.{SGL, Camera}
import org.sofa.math.{Point3, Point2}
import org.sofa.opengl.{Texture, ShaderProgram}
import org.sofa.opengl.mesh.{TrianglesMesh, VertexAttribute}

object Armature {
	/** The set of armatures, added elsewhere. */
	val armatures = new HashMap[String, Armature]()

	def apply(name:String, area:(Double,Double), texResource:String, shaderResource:String, root:Joint):Armature = 
		new Armature(name, Point2(area._1, area._2), texResource, shaderResource, root)
}

/** A hierachical set of joints. */
class Armature(val name:String, val area:Point2, val texResource:String, val shaderResource:String, val root:Joint) {

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

		root.build(gl, this)
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
	def apply(name:String, z:Double, area:(Double,Double,Double,Double), pivot:(Double,Double), anchor:(Double,Double), visible:Boolean, joints:Joint*):Joint =
		new Joint(name, z, Point2(area._1, area._2), Point2(area._3, area._4), Point2(pivot._1, pivot._2), Point2(anchor._1, anchor._2), visible, joints.toArray) 
}

class Joint(val name:String, val z:Double, val from:Point2, val size:Point2 ,val pivot:Point2, val anchor:Point2, var visible:Boolean, var sub:Array[Joint]) {

	protected var parent:Joint = null

	protected var triangle:Int = 0

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

	def build(gl:SGL, armature:Armature) {
		triangle = armature.count * 2

		var tri = triangle
		var pt  = tri * 3
		val triangles = armature.triangles

		var x0 = (-pivot.x).toFloat
		var y0 = (-pivot.y).toFloat
		var x1 = x0 + size.x.toFloat
		var y1 = y0 + size.y.toFloat
		var z  = this.z.toFloat

		triangles.setPoint(pt+0, x0, y0, z)
		triangles.setPoint(pt+1, x1, y1, z)
		triangles.setPoint(pt+2, x1, y0, z)
		triangles.setPoint(pt+3, x0, y0, z)
		triangles.setPoint(pt+4, x0, y1, z)
		triangles.setPoint(pt+5, x1, y1, z)

		x0 = from.x.toFloat
		y0 = from.y.toFloat
		x1 = x0 + size.x.toFloat
		y1 = y0 + size.y.toFloat

		triangles.setPointTexCoord(pt+0, x0, y0)
		triangles.setPointTexCoord(pt+1, x1, y1)
		triangles.setPointTexCoord(pt+2, x1, y0)
		triangles.setPointTexCoord(pt+3, x0, y0)
		triangles.setPointTexCoord(pt+4, x0, y1)
		triangles.setPointTexCoord(pt+5, x1, y1)

		triangles.setTriangle(tri+0, pt+0, pt+1, pt+2)
		triangles.setTriangle(tri+1, pt+3, pt+4, pt+5)

		armature.count += 1

		sub.foreach { _.build(gl, armature) }
	}

	protected def normalize(armature:Armature) {
		val sz = armature.area
		
		from.set(from.x/sz.x, from.y/sz.y)
		size.set(size.x/sz.x, size.y/sz.y)
		pivot.set((pivot.x/sz.x)-from.x, (pivot.y/sz.y)-from.y)
		
		if(parent ne null)
			anchor.set((anchor.x/sz.x)-(parent.from.x+parent.pivot.x), (anchor.y/sz.y)-(parent.from.y+parent.pivot.y))
	}

	def display(gl:SGL, armature:Armature, camera:Camera) {
		if(visible) {
			// Draw all elements in Z order. We have to draw ourself 
			// at the correct place, hence the complicated tests.

			camera.pushpop {
				camera.translateModel(anchor.x, anchor.y, 0)
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

	override def toString():String = "Joint(%s(%.2f) [%s->%s] (%s) <%s> {%s})".format(name, z, from, size, pivot, anchor,
			if(sub ne null) sub.mkString(",") else "")
}