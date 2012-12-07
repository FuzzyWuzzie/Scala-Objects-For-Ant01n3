package org.sofa.opengl.io.collada

import scala.xml.{Node, NodeSeq}
import scala.collection.mutable.{HashMap, ArrayBuffer}
import org.sofa.math.{Matrix4}

object Controller {
	def apply(node:Node):Controller = new Controller(node)
}

/** Controller. */
class Controller(node:Node) extends ColladaFeature {
	/** Name. */
	var name = ""

	/** Identifier. */
	var id = ""

	/** The optionnal skin. */
	var skin:Skin = null

	/** The optionnal morph. */
	var morph:Morph = null

	parse(node)

	protected def parse(node:Node) {
		name  = (node \ "@name").text
		id    = (node \ "@id").text
		val s = (node \ "skin")
		val m = (node \ "morph")

		if((s ne null) && (!s.isEmpty))
			skin = new Skin(s.head)

		if((m ne null) && (!m.isEmpty))
			morph = new Morph(m.head)
	}

	override def toString():String = "controller(%s, %s, %s)".format(id, name, skin)
}

// TODO
class Morph(node:Node) {}

/** A skin description. */
class Skin(node:Node) {
	/** Name of the mesh tied to this armature. */
	var source:String = ""

	/** Global matrix for the armature. */
	var bindShape:Matrix4 = null

	/** Bones. */
	var joints:Array[Joint] = null

	/** Data of the weights for each vertex. */
	var weightData:Array[Float] = null

	/** Number of weights for each vertex. */
	var vcount:Array[Int] = null

	/** Index of the weights in the weightData for each vertex, according to vcount. */
	var vdata:Array[Int] = null

	parse(node)

	protected def parse(node:Node) {
		source = (node \ "@source").text
		bindShape = matrixFromFloats((node \ "bind_shape_matrix").text, 0)

		// Find the "joints" node that indicate two sources:
		//	- for the joints name and arrangement,
		//	- for the poses of the joints (matrices).

		val joints              = (node \ "joints" \ "input")
		val jointSource         = (joints.find({item => (item \ "@semantic").text.equals("JOINT")}).getOrElse(throw new RuntimeException("no joint source in controller")) \ "@source").text.substring(1)
		val invVindMatrixSource = (joints.find({item => (item \ "@semantic").text.equals("INV_BIND_MATRIX")}).getOrElse(throw new RuntimeException("no inv_bind_matrix in controller")) \ "@source").text.substring(1)
		
		val sources       = (node \ "source")
		val joint         = sources.find({item => (item \ "@id").text.equals(jointSource)}).getOrElse(throw new RuntimeException("no source for joints %s".format(jointSource)))
		val invBindMatrix = sources.find({item => (item \ "@id").text.equals(invVindMatrixSource)}).getOrElse(throw new RuntimeException("no source for inv_bind_matrix %s".format(invVindMatrixSource)))

		parseJoints(joint)
		parseInvBindMatrix(invBindMatrix)

		// Then find the "vertex_weights" node that ties the joints name
		// and the skin weights to vertices in the model.

		parseVertexWeights(node, (node \ "vertex_weights").head)
	}

	protected def parseJoints(node:Node) {
		val names = (node \ "Name_array").text.split(" ")

		joints = new Array[Joint](names.length)
		var i = 0

		names.foreach { name =>
			joints(i) = new Joint()
			joints(i).name = name
			i += 1
		}
	}

	protected def parseInvBindMatrix(node:Node) {
		val count  = (node \ "technique_common" \ "accessor" \ "@count").text.toInt
		val stride = (node \ "technique_common" \ "accessor" \ "@stride").text.toInt
		val data   = (node \ "float_array").text.split(" ").map(_.toFloat)

		assert(joints ne null)
		assert(count == joints.length)
		assert(stride == 16)

		var i = 0
		
		while(i < count) {
			joints(i).pose = Matrix4(data, i*16)
			i += 1
		}
	}

	protected def parseVertexWeights(parent:Node, node:Node) {
		val inputs       = (node \ "input")
		val inputJoint   = inputs.find({item => (item \ "@semantic").text.equals("JOINT")}).getOrElse(throw new RuntimeException("no joint weight"))
		val inputWeight  = inputs.find({item => (item \ "@semantic").text.equals("WEIGHT")}).getOrElse(throw new RuntimeException("no joint weight"))
		val jointOffset  = (inputJoint \ "@offset").text.toInt
		val weightOffset = (inputWeight \ "@offset").text.toInt
		val weightSource = (inputWeight \ "@source").text.substring(1)

		vcount = (node \ "vcount").text.split(" ").map(_.toInt)
		vdata  = (node \ "v").text.split(" ").map(_.toInt)

		val weightNode = (parent \ "source").find({item => (item \ "@id").text.equals(weightSource)}).getOrElse(throw new RuntimeException("no weight source"))

		parseWeights(weightNode)
	}

	protected def parseWeights(node:Node) {
		weightData = (node \ "float_array").text.split(" ").map(_.toFloat)
	}

	protected def matrixFromFloats(data:String, offset:Int):Matrix4 = {
		Matrix4(data.split(" ").map(_.toFloat), offset)
	}

	override def toString():String = "skin(%s, bindShape(%s), { %s }, vcount(%s), v(%s), weights(%s))".format(source, bindShape.toCompactString, joints.mkString(", "), vcount.mkString(", "), vdata.mkString(", "), weightData.mkString(", "))
}

class Joint {
	var name:String = null

	var pose:Matrix4 = null

	override def toString():String = "joint(%s, %s)".format(if(name ne null) name else "<noname>", if(pose ne null) pose.toCompactString else "<nopose>")
}