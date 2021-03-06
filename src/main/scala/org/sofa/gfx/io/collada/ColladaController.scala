package org.sofa.gfx.io.collada

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

	/** The maximum number of weights/bones per vertex. */
	var stride:Int = 0

	/** Number of vertices for which bones/weights are defined. */
	var vertexCount:Int = 0

	parse(node)

	/** The joint corresponding to the given name. */
	def joint(name:String):Option[Joint] = { joints.find(item => item.name == name) }

	protected def parse(node:Node) {
		source = (node \ "@source").text.substring(1)
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
		val names = (node \ "Name_array").text.trim.split("\\s+")

		joints = new Array[Joint](names.length)
		var i = 0

		names.foreach { name =>
			joints(i)       = new Joint()
			joints(i).name  = name
			joints(i).index = i
			i += 1
		}
	}

	protected def parseInvBindMatrix(node:Node) {
		val count  = (node \ "technique_common" \ "accessor" \ "@count").text.toInt
		val stride = (node \ "technique_common" \ "accessor" \ "@stride").text.toInt
		val data   = (node \ "float_array").text.trim.split("\\s+").map(_.toFloat)

		assert(joints ne null)
		assert(count == joints.length)
		assert(stride == 16)

		var i = 0
		
		while(i < count) {
			joints(i).pose = Matrix4(data, i*16)
			joints(i).pose.transpose()
			i += 1
		}
	}

	protected def parseVertexWeights(parent:Node, node:Node) {
		vertexCount      = (node \ "@count").text.toInt
		val inputs       = (node \ "input")
		val inputJoint   = inputs.find({item => (item \ "@semantic").text.equals("JOINT")}).getOrElse(throw new RuntimeException("no joint weight"))
		val inputWeight  = inputs.find({item => (item \ "@semantic").text.equals("WEIGHT")}).getOrElse(throw new RuntimeException("no joint weight"))
		val jointOffset  = (inputJoint \ "@offset").text.toInt
		val weightOffset = (inputWeight \ "@offset").text.toInt
		val weightSource = (inputWeight \ "@source").text.substring(1)

		vcount = (node \ "vcount").text.trim.split("\\s+").map(_.toInt)
		vdata  = (node \ "v").text.trim.split("\\s+").map(_.toInt)

		stride = vcount.max

		val weightNode = (parent \ "source").find({item => (item \ "@id").text.equals(weightSource)}).getOrElse(throw new RuntimeException("no weight source"))

		parseWeights(weightNode)
	}

	protected def parseWeights(node:Node) {
		weightData = (node \ "float_array").text.trim.split("\\s+").map(_.toFloat)
	}

	protected def matrixFromFloats(data:String, offset:Int):Matrix4 = {
		Matrix4(data.trim.split("\\s+").map(_.toFloat), offset)
	}

	/** From the vcount, vdata and weightsData, compute two arrays where
	  * each vertex is represented with a set of bone indices and associated weights.
	  * As one vertex can be influenced by several bones with distinct weights
	  * the returned arrays have a stride of the maximum number of bones affecting
	  * one vertex. For example if there are at maximum two bones affecting a two vertices,
	  * we could obtain two arrays of four components, the two first components indentify
	  * the first vertex, the two last components identify the second vertex.
	  * As some vertices are influenced by less bones than other, value -1 in the
	  * two arrays tell that there are no more bone / weight.
	  *
	  * It is sometimes usefull to limit the number of bone affecting a vertex. This is 
	  * the meaning of `stride`, if it is less than `realStride` the arrays are rearanged
	  * so that only the `stride` most influent bones are used and the weights are
	  * re-normalized. TODO */
	def computeBonesAndWeights(stride:Int, realStride:Int):(Array[Float],Array[Float]) = {
		val n       = vertexCount
		val bones   = new Array[Float](stride*n)
		val weights = new Array[Float](stride*n)
		var i       = 0		// Current vertex
		var v       = 0		// Current vertex in vdata

		// For each vertex.

		while(i < n) {
			val cnt = vcount(i)
			val idx = i*stride		// Position in bones and weights array (advance by stride)

			if(cnt <= stride) {
				// There is less or exactly stride bones affecting this vertex, ok.
				for(s <- 0 until stride) {
					if(s < cnt) {
						bones(idx+s)   = vdata(v*2+s*2)
						weights(idx+s) = weightData(vdata(v*2+s*2+1))
					} else {
						bones(idx+s)   = -1
						weights(idx+s) = -1
					}
				}
			} else {
				Console.err.println("Vertex %d has more than 4 bones affecting it, rescaling to 4".format(i))
				// There are more bones affecting this vertex than stride, we must
				// select the most influent bones.

				val newBones = lessBones(cnt, stride, v*2)

				for(s <- 0 until stride) {
					bones(idx+s)   = newBones(s)._1
					weights(idx+s) = newBones(s)._2
				}
			}

			i += 1
			v += cnt
		}

		(bones, weights)
	}

	/** Take a set of (bone,weight) pairs whose size is larger than `stride` and
	  * return a new set of (bone,weight) pairs where the first `stride` bones are
	  * the most influent ones, and their weight sum up to 1 (other bones are 
	  * included in the result, at the end, but can be ignored, their weight are not
	  * valid). */
	protected def lessBones(realStride:Int, stride:Int, start:Int):Array[(Float,Float)] = {
		assert(realStride > stride)
		
		// Get the (bone,weight) by pairs to sort them.

		var values = new Array[(Float,Float)](realStride)
		
		for(s <- 0 until realStride) {
			values(s) = (vdata(start+s*2), weightData(vdata(start+s*2+1)))
		}

		// Sort the (bone,weight) pairs so that the most influent bones are at start.
		
		values = values.sortWith { (a,b) => a._2 > b._2 }
		
		// Normalize the 'stride' first weights.

		var i = 0
		var w = 0f
		
		while(i < stride) { w += values(i)._2; i += 1 }
		
		w = 1f / w
		i = 0
		
		while(i < stride) { values(i) = (values(i)._1, values(i)._2 * w); i+= 1 }
		
		// Ok the 'stride' first weights should sum up to 1.

		values
	}

	override def toString():String = "skin(%s, bindShape(%s), { %s }, vcount(%s), v(%s), weights(%s))".format(source, bindShape.toCompactString, joints.mkString(", "), vcount.mkString(", "), vdata.mkString(", "), weightData.mkString(", "))
}

class Joint {
	var index:Int = -1

	var name:String = null

	var pose:Matrix4 = null

	override def toString():String = "joint(%s, %s)".format(if(name ne null) name else "<noname>", if(pose ne null) pose.toCompactString else "<nopose>")
}