package org.sofa.opengl.io.collada

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.xml.Node
import org.sofa.math.Matrix4
import org.sofa.opengl.mesh.skeleton.Bone

abstract class Transform(val sid:String) {
	/** Apply the transform to the given matrix. */
	def applyTo(m:Matrix4) { applyTo(m, false) }

	/** Apply the transform to the given matrix. If blenderToOpenGLCoos is true,
      * the transforms are modified to mach OpenGL axes, not blender ones. */
	def applyTo(m:Matrix4, blenderToOpenGLCoos:Boolean)
}

class Rotate(sid:String, text:String) extends Transform(sid) {
	var axis = new Array[Float](3)
	
	var angle = 0f
	
	parse(text)
	
	protected def parse(text:String) {
		val data = text.trim.split("\\s+").map(_.toFloat)
		axis(0) = data(0)
		axis(1) = data(1)
		axis(2) = data(2)
		angle   = data(3)
	}

	def applyTo(m:Matrix4, b2gl:Boolean) {
		if(b2gl)
		     m.rotate(angle, axis(1), axis(2), axis(0))
		else m.rotate(angle, axis(0), axis(1), axis(2))
	}
	
	override def toString():String = "rotate(angle %f, axis [%s])".format(angle, axis.mkString(", "))
}

class Translate(sid:String, text:String) extends Transform(sid) {
	var data:Array[Float] = null
	
	parse(text)
	
	protected def parse(text:String) {
		data = text.trim.split("\\s+").map(_.toFloat)
	}	

	def applyTo(m:Matrix4, b2gl:Boolean) {
		if(b2gl)
		     m.translate(data(1), data(2), data(0))
		else m.translate(data(0), data(1), data(2))
	}
	
	override def toString():String = "translate(%s)".format(data.mkString(", "))
}

class Matrix(sid:String, text:String) extends Transform(sid) {
	var matrix:Matrix4 = null

	parse(text)

	protected def parse(text:String) {
		val data = text.trim.split("\\s+").map(_.toFloat)
		matrix = Matrix4(data, 0)
		matrix.transpose()
	} 

	def applyTo(m:Matrix4, b2gl:Boolean) {
		var mm = matrix

		if(b2gl) {
			mm = Matrix4(matrix)
			val r1 = mm.row1
			val r2 = mm.row2
			mm.row1 = r2
			mm.row2 = r1
		}

		m.multBy(mm)
	}

	override def toString():String = "matrix(%s)".format(matrix.toCompactString)
}

object SceneNodeType extends Enumeration {
	val NodeType = Value
	val JointType = Value

	type SceneNodeType = SceneNodeType.Value 
}

/** An element of a Collada visual scene. */
class SceneNode(node:Node) {
	import SceneNodeType._

	var name = ""
	
	var id = ""

	var sid = ""
	
	val transforms = new ArrayBuffer[Transform]()
	
	var instance = ""

	val child = new HashMap[String, SceneNode]()

	var nodeType:SceneNodeType = null
	
	parse(node)

	/** Get a transform by its sid. */
	def getTransform(sid:String):Option[Transform] = transforms.find { item => item.sid == sid }
	
	protected def parse(node:Node) {
		name = (node \ "@name").text
		id   = (node \ "@id").text
		sid  = (node \ "@sid").text

		(node \ "@type").text match {
			case "NODE"   => nodeType = SceneNodeType.NodeType
			case "JOINT"  => nodeType = SceneNodeType.JointType
			case x:String => throw new RuntimeException("unknow scene node type %s".format(x))
		}
		
		node.child.foreach { item =>
			item.label match {
				case "translate" => transforms += new Translate((item \ "@sid").text, item.text)
				case "rotate"    => transforms += new Rotate((item \ "@sid").text, item.text)
				case "matrix"    => transforms += new Matrix((item \ "@sid").text, item.text)
				case "instance_light"    => instance = (item \ "@url").text.substring(1)
				case "instance_camera"   => instance = (item \ "@url").text.substring(1)
				case "instance_geometry" => instance = (item \ "@url").text.substring(1)
				case "node"              => { val n = new SceneNode(item); child += ((n.id, n)) }
				case _ => {}
			} 
		}
	}

	override def toString():String = "node(%s, %s, instance %s, transform(%s), child { %s })".format(name, nodeType, instance, transforms.mkString(", "), child.mkString(", "))
}

object VisualScene {
	def apply(node:Node):VisualScene = new VisualScene(node)
}

/** A scene in the Collada library of visual scenes. */
class VisualScene(node:Node) extends ColladaFeature {
	/** Invert x with y, y with z and z with x. Useful to pass from Blender coordinates to OpenGL ones. */
	var blenderToOpenGLCoos:Boolean = false

	/** Name of the scene. */
	var name = ""
	
	/** Unique identifier of the scene. */
	var id = ""

	/** Set of scene nodes making up the scene. */
	val nodes = new HashMap[String,SceneNode]()
	
	parse(node)
	
	/** Obtain the node referenced by `nodeId`. */
	def apply(nodeId:String):Option[SceneNode] = nodes.get(nodeId)

	/** Try to swap axis considering the source uses Blender axis and pass them to OpenGL axis.
	  * This means that the x becomes y, the y becomes z and the z becomes x. This setting is applyed
	  * when the mesh is transformed to a SOFA [[Mesh]] when calling [[toMesh()]]. */
	def blenderToOpenGL(on:Boolean) { blenderToOpenGLCoos = on }

	/** Convert the given node and sub-nodes to a SOFA [[Bone]] hierarchy.
	  * The given node `id` must identify the node in the visual scene that
	  * contains the armature under the form of child nodes of type `JointType`.
	  * The given `controller` is used to check the bones names and indices. */
	def toSkeleton(id:String, controller:Controller):Bone = {
		import SceneNodeType._

		val node = nodes.get(id) match {
			case Some(x) => x
			case None    => throw new RuntimeException("no node with id %s".format(id))
		}
		
		val bone = node.child.find { item => item._2.nodeType == JointType } match {
			case Some(n) => recursiveProcessSkeleton(n._2, controller)
			case None    => throw new RuntimeException("the given id %s does not point to a node containing a joint".format(id))
		}

		// Apply the node transforms on the root bone orientation/pose matrix.

		node.transforms.foreach { _.applyTo(bone.orientation, blenderToOpenGLCoos) }

		// Pass from blender axes to OpenGL axes, (modify only the root bone orientation/pose matrix).

		if(blenderToOpenGLCoos) {
			val r1 = bone.orientation.row1
			val r2 = bone.orientation.row2

			bone.orientation.row1 = r2
			bone.orientation.row2 = r1
		}

		bone.computeLength

		bone
	}

	/** Recursively build a bone hierarchy starting from the given `node` (that must have JointType). */
	protected def recursiveProcessSkeleton(node:SceneNode, controller:Controller):Bone = {
		// Need the controller to get back the name of the bone ?

		val joint = controller.skin.joint(node.sid) match {
			case Some(x) => x 
			case None    => throw new RuntimeException("cannot find joint %s referenced by JOINT node in visual scene".format(node.sid))
		}

		val bone = new Bone(joint.index)

		bone.setName(joint.name)

		val pose = node.getTransform("transform") match {
			case Some(m:Matrix) => m.matrix
			case Some(x)        => throw new RuntimeException("need a matrix transform for bone pose")
			case None           => throw new RuntimeException("need a transform for bone pose")
		}
	
		bone.setPose(pose)

		node.child.foreach { n =>
			bone.addChild(recursiveProcessSkeleton(n._2, controller))
		}

		bone
	}

	protected def parse(node:Node) {
		name = (node \ "@name").text
		id   = (node \ "@id").text
		
		(node \\ "node").foreach { item => val n = new SceneNode(item); nodes += ((n.id, n)) }
	}
	
	override def toString():String = "visualScene(%s, (%s)".format(name, nodes.mkString(", "))
}