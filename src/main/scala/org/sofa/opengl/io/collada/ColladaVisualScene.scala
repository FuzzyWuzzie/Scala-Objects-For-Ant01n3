package org.sofa.opengl.io.collada

import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

class Transform {
}

class Rotate(text:String) extends Transform {
	var axis = new Array[Float](3)
	
	var angle = 0f
	
	parse(text)
	
	protected def parse(text:String) {
		val data = text.split(" ").map(_.toFloat)
		axis(0) = data(0)
		axis(1) = data(1)
		axis(2) = data(2)
		angle   = data(3)
	}
	
	override def toString():String = "rotate(angle %f, axis [%s])".format(angle, axis.mkString(", "))
}

class Translate(text:String) extends Transform {
	var data:Array[Float] = null
	
	parse(text)
	
	protected def parse(text:String) {
		data = text.split(" ").map(_.toFloat)
	}	
	
	override def toString():String = "translate(%s)".format(data.mkString(", "))
}

/** An element of a Collada visual scene. */
class SceneNode(node:Node) {
	var name = ""
	var id = ""
	val transforms = new ArrayBuffer[Transform]()
	var instance = ""
	
	parse(node)
	
	protected def parse(node:Node) {
		name = (node\"@name").text
		id   = (node\"@id").text
		
		node.child.foreach { item =>
			item.label match {
				case "translate" => transforms += new Translate(item.text)
				case "rotate"    => transforms += new Rotate(item.text)
				case "instance_light"    => instance = (item \ "@url").text.substring(1)
				case "instance_camera"   => instance = (item \ "@url").text.substring(1)
				case "instance_geometry" => instance = (item \ "@url").text.substring(1)
				case _ => {}
			} 
		}
	}
	
	override def toString():String = "node(%s, instance %s, transform(%s))".format(name, instance, transforms.mkString(", "))
}

object VisualScene {
	def apply(node:Node):VisualScene = new VisualScene(node)
}

/** A scene in the Collada library of visual scenes. */
class VisualScene(node:Node) extends ColladaFeature {
	/** Name of the scene. */
	var name = ""
	/** Unique identifier of the scene. */
	var id = ""
	/** Set of scene nodes making up the scene. */
	val nodes = new ArrayBuffer[SceneNode]()
	
	parse(node)
	
	protected def parse(node:Node) {
		name = (node \ "@name").text
		id   = (node \ "@id").text
		
		(node \\ "node").foreach { item => nodes += new SceneNode(item) }
	}
	
	override def toString():String = "visualScene(%s, (%s)".format(name, nodes.mkString(", "))
}