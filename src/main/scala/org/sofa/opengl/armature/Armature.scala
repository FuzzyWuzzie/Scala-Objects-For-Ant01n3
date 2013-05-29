package org.sofa.opengl.armature

import scala.collection.mutable.HashMap

import org.sofa.FileLoader
import org.sofa.opengl.{SGL, Camera}
import org.sofa.math.{Point3, Point2, Vector2}
import org.sofa.opengl.{Texture, ShaderProgram, Libraries}
import org.sofa.opengl.mesh.{TrianglesMesh, VertexAttribute}

case class NoSuchJointException(message:String) extends Exception(message)

/** Pluggable loader for armature sources. */
trait ArmatureLoader extends FileLoader {
    /** Try to open a resource, or throw an IOException if not available. */
    def open(name:String, texRes:String, shaderRes:String, resource:String):Armature
}

/** Default loader for armatures, based on files and the include path.
  * This loader tries to open the given resource directly, then if not
  * found, tries to find it in each of the pathes provided by the include
  * path. If not found it throws an IOException. */
class DefaultArmatureLoader extends ArmatureLoader {
	private[this] val SVGLoader = new SVGArmatureLoader()

    def open(name:String, texRes:String, shaderRes:String, resource:String):Armature = {
        SVGLoader.load(name, texRes, shaderRes, findPath(resource, Armature.path))
    }
}

object Armature {
//	/** The set of armatures, added elsewhere. */
//	val armatures = new HashMap[String, Armature]()

	/** The set of paths to try to load a shader. */
	val path = scala.collection.mutable.ArrayBuffer[String]()

	/** Loader for armatures. */
	var loader:ArmatureLoader = new DefaultArmatureLoader

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

	/** For fast retrieval of joints. */
	val jointMap = new HashMap[String,Joint]

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

	/** Display the whole armature, but only joints that are visible. */
	def display(gl:SGL, camera:Camera) {
		camera.pushpop {
			shader.use
			texture.bindUniform(gl.TEXTURE0, shader, "texColor")
			root.display(gl, this, camera, shader)
		}
	}

	/** Obtain the joint corresponding to the given name or throw a [[NoSuchJointException]] exception. */
	def apply(jointName:String):Joint = { jointMap.get(jointName).getOrElse(throw NoSuchJointException("Joint %s is not registered in armature".format(jointName))) } 

	/** Obtain the joint corresponding to the given name or throw a [[NoSuchJointException]] exception. */
	def \\ (jointName:String):Joint = apply(jointName)

	override def toString():String = "Armature(%s, [%s], {%s})".format(name, area, root)

	def toIndentedString():String = {
		"Armature(%s, [%s] {%n%s%n})%n".format(name, area, root.toIndentedString(1))
	}
}


// == Joint Transform ===============================================================================


class JointTransform {

	/** Current rotation in radians. */
	var angle = 0.0

	/** Current translation (before or after rotation ??? or the twos ?) */
	var translation = Vector2(0, 0)

	/** Current scale (before or after translation ??? or the twos ?) */
	var scale = Vector2(1, 1)

	def transform(camera:Camera) {
		if(angle != 0)
			camera.rotateModel(angle, 0, 0, 1)

		if(scale.x != 0 || scale.y != 0)
			camera.scaleModel(scale.x, scale.y, 1)

		if(translation.x != 0 || translation.y != 0)
			camera.translateModel(translation.x, translation.y, 1)
	}
}


// == Joint =========================================================================================


object Joint {
	/** Create a joint.
	  *
	  * The joint maps to an area in a  texture whose pixels are indexed with an origin at the top-left corner (as usual).
	  *
	  * @param name      The joint identifier.
	  * @param z         The z level.
	  * @param area      Four pixels, the two first are the position in absolute pixels, the two others the size of the area in the texture for this joint.
	  * @param pivot     The "gravity" center of the joint a position in absolute pixels.
	  * @param anchor    The attach point in the parent joint in absolute pixels.
	  * @param visible   If true the joint will be visible at start.
	  * @param subAbove  Children joints to be drawn (in order) above this.
	  * @param subUnder  Children joints to be drawn (in order) under this. */
	def apply(name:String,
	          z:Double,
	          area:(Double,Double,Double,Double),
	          pivot:(Double,Double),
	          anchor:(Double,Double),
	          visible:Boolean,
	          sub:Joint*):Joint = {
		val subs  = sub.sortWith { (j0,j1) => j0.z < j1.z }
		val (under,above) = subs.partition { s => s.z < z }

		new Joint(name, z,
		          Point2(area._1, area._2),   Point2(area._3, area._4),
		          Point2(pivot._1, pivot._2), Point2(anchor._1, anchor._2),
		          visible, under.toArray, above.toArray) 
	}

	def apply(name:String):Joint = {
		new Joint(name, 0.0, null, null, null, null, true, null, null)
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
  * You specify the coordinates in absolute pixels, this means that fromUV, pivotGU,
  * and anchorGU, are given acording to real coordinates the pixels of your reference
  * picture (because this is easier). The name of the fields indicates the units into
  * which they will be converted when the joint is initialized. This means for example
  * that fromUV and sizeUV will then be normalized between 0 and 1. And pivotGU and
  * anchorGU will be scaled by the armature scale, and moved to be relative to (0,0)
  * and the parent pivot point respectivelly.
  *
  * @param name     Name of the joint.
  * @param z        The position above or under other joints, allow to give a drawing order.
  * @param fromUV   Give absolute pixels, and it will then contain the position of the left-bottom corner of the joint rectangle in the texture.
  * @param sizeUV   Give pixels, and it will then contain the size of the joint rectangle in the texture (the aspect ratio is not conserved).
  * @param pivotGU  Give absolute pixels, and it will then contain the position of the pivot point relative to (0,0).
  * @param anchorGU Give absolute pixels, and it will then contain the position of the anchor relative to the parent (0,0).
  * @param visible  Is the joint visible at start ?
  * @param subUnder The sub joints to be drawn (in order) under this, can be null.
  * @param subAbove The sub joints to be drawn (in order) above this, can be null.
  */
class Joint(val name:String,
            var z:Double,		// <- XXX not used XXX
            var fromUV:Point2,
            var sizeUV:Point2,
            var pivotGU:Point2,
            var anchorGU:Point2,
            var visible:Boolean,
            var subUnder:Array[Joint],
            var subAbove:Array[Joint]) {

	/** Parent joint. */
	var parent:Joint = null

	/** Position in the texture image in game units. */
	protected val fromGU:Point2 = new Point2

	/** The size of the joint in game units. Preserve the ratio. */
	protected val sizeGU:Point2 = new Point2

	/** The triangle index in the armature when drawing. */
	protected var triangle:Int = 0

	var selected = false

	/** Current transform for the joint (and its subjoints). Can be null. */
	var transform = new JointTransform

	/** The given sub joint if any. */
	def apply(id:String):Joint = {
		// Not really efficient ...
		if(subUnder ne null) subUnder.find(_.name == id).getOrElse {
			if(subAbove ne null) subAbove.find(_.name == id).getOrElse { null } else null
		} else null
	}

	/** The given sub joint if any. */
	def \ (id:String):Joint = apply(id)

	/** Number of sub-joints to draw in order under this one. */
	def subUnderCount:Int = if(subUnder ne null) subUnder.size else 0

	/** Number of sub-joints to draw in order above this one. */
	def subAboveCount:Int = if(subAbove ne null) subAbove.size else 0

	/** Number of sub-joints under or above this one. */
	def subCount:Int = subUnderCount + subAboveCount

	/** I-th sub-joint starting in the sub-joints under this one, and continuing in the sub-joints above this one.
	  * For example if this joint has two sub-joints under and two above, index 0 is the first sub-joint under,
	  * index 2 is the first sub-joint above. Index 3 is the last sub-joint. */
	def sub(i:Int):Joint = {
		if(subUnder ne null) {
			if(i < subUnder.size) {
				subUnder(i)
			} else if((subAbove ne null) && (i - subUnder.size) < subAbove.size) {
				subAbove(i-subUnder.size)
			} else null
		} else {
			if((subAbove ne null) && (i < subAbove.size)) {
				subAbove(i)
			} else {
				null
			}
		}
	}

	/** Change the absolute pixel coordinates to relative coordinates either in UV (texture) or
	  * game (scaled) coordinates and setup the parent joint. */
	def init(parent:Joint, armature:Armature):Int = {
		this.parent = parent
		armature.jointMap += (name -> this)

		normalize(armature)
		
		var count = 1
		
		if(subUnder ne null) subUnder.foreach { count += _.init(this, armature) }
		if(subAbove ne null) subAbove.foreach { count += _.init(this, armature) }

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

	/** Create the triangles (with the pivot point at the origin) and setup their UV texture, then recursively
      * build the sub joints. */
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

		if(subUnder ne null) subUnder.foreach { _.build(armature) }
		if(subAbove ne null) subAbove.foreach { _.build(armature) }
	}

	def display(gl:SGL, armature:Armature, camera:Camera, shader:ShaderProgram) {
		if(visible) {
			camera.pushpop {
				camera.translateModel(anchorGU.x, anchorGU.y, 0)

				if(transform ne null)
					transform.transform(camera)

				displaySub(gl, subUnder, armature, camera, shader)
				displaySelf(armature, camera, shader)
				displaySub(gl, subAbove, armature, camera, shader)
			}
		}
	}

	/** Display the given sub array of joints. */
	protected def displaySub(gl:SGL, sub:Array[Joint], armature:Armature, camera:Camera, shader:ShaderProgram) {
		if((sub ne null) && sub.size > 0) {
			sub foreach { _.display(gl, armature, camera, shader) }
		}
	}

	/** Display this joint. */
	protected def displaySelf(armature:Armature, camera:Camera, shader:ShaderProgram) {
		if(selected)
		     shader.uniform("highlight", 1.0f)
		else shader.uniform("highlight", 0.0f)
		camera.setUniformMVP(armature.shader)
		armature.triangles.lastVertexArray.drawArrays(armature.triangles.drawAs, triangle*3, 2*3)		
	}

	/** Return a multiline string where sub joints are indented. */
	def toIndentedString(indent:Int):String = {
		val sb = new StringBuilder
		val id = "    " * indent

		sb ++= id
		sb ++= "Joint(%s (%.2fz) [%s / %s]UV [%s / %s]GU pivot(%s)GU anchor(%s)GU".format(name, z,
	 		fromUV.toShortString, sizeUV.toShortString,
	 		fromGU.toShortString, sizeGU.toShortString, pivotGU.toShortString,
	 		if(anchorGU ne null) anchorGU.toShortString else "noAnchor")

		if((subUnder ne null) && subUnder.size > 0) {
			sb ++= " UNDER{%n".format()
			subUnder.foreach { s => sb ++= "%s".format(s.toIndentedString(indent+1)) }
			sb ++= "%s}".format(id)
		}

		if((subAbove ne null) && subAbove.size > 0) {
			sb ++= " ABOVE{%n".format()
			subAbove.foreach { s => sb ++= "%s".format(s.toIndentedString(indent+1)) }
			sb ++= "%s}".format(id)
		}

		sb ++= ")%n".format(id)

		sb.toString
	}

	override def toString():String = "Joint(%s(%.2f) [%s->%s]UV [%s->%s]GU (%s)GU <%s>GU {%s} {%s})".format(name, z,
	 		fromUV.toShortString, sizeUV.toShortString,
	 		fromGU.toShortString, sizeGU.toShortString, pivotGU.toShortString,
			if(anchorGU ne null) anchorGU.toShortString else "noAnchor",
			if(subUnder ne null) subUnder.mkString(",") else "",
			if(subAbove ne null) subAbove.mkString(",") else "")
}