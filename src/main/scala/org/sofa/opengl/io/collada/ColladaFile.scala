package org.sofa.opengl.io.collada

import scala.xml.NodeSeq
import scala.collection.mutable.HashMap

/** A Collada file, assets, library of elements and a scene. */
class File(root:NodeSeq) {
	/** Meta-data on the file. */
	val asset:Assets = new Assets((root \\ "asset").head)
	
	/** All the objects and elements in the file. */
	val library = new Library(root)
	
	// TODO add the scene description.
	
	override def toString():String = "%s%n%s".format(asset, library)
}


/** Library of elements used an referenced inside a Collada file. */
class Library(root:NodeSeq) {
	/** All the cameras. */
	val cameras = new HashMap[String,Camera]()
	
	/** All the lights. */
	val lights = new HashMap[String,Light]()
	
	/** All the geometries. */
	val geometries = new HashMap[String,Geometry]()
	
	/** All the scenes. */
	val visualScenes = new HashMap[String,VisualScene]()

	parse(root)
	
	def parse(root:NodeSeq) {
		// Cameras
		(root \\ "library_cameras" \ "camera").foreach { node =>
			cameras += ((node.label, Camera(node)))
		}  
		// Lights
		(root \\ "library_lights" \ "light").foreach { node =>
			lights += ((node.label, Light(node)))
		}
		// Geometries
		(root \\ "library_geometries" \ "geometry").foreach { node =>
			geometries += ((node.label, Geometry(node)))
		}
		// Visual Scenes
		(root \\ "library_visual_scenes" \ "visual_scene").foreach { node =>
			visualScenes += ((node.label, VisualScene(node))) }
	}
	
	override def toString():String = {
		val builder = new StringBuilder()
		if(!cameras.isEmpty) {
			cameras.foreach { item => builder.append(item._2.toString); builder.append("%n".format()) }
		}
		if(!lights.isEmpty) {
			lights.foreach { item => builder.append(item._2.toString); builder.append("%n".format()) }
		}
		if(!geometries.isEmpty) {
			geometries.foreach { item => builder.append(item._2.toString); builder.append("%n".format()) }
		}
		if(!visualScenes.isEmpty) {
			visualScenes.foreach { item => builder.append(item._2.toString); builder.append("%n".format()) }
		}
		builder.toString
	}
}