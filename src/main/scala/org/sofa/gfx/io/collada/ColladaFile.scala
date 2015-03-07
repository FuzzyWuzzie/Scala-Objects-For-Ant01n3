package org.sofa.gfx.io.collada

import org.sofa.Loader
import scala.xml.NodeSeq
import scala.collection.mutable.HashMap


object ColladaFile{
	/** Path to search for collada files. */
	val path = new scala.collection.mutable.ArrayBuffer[String]()

	/** System independant loader. */
	var loader:ColladaLoader = new DefaultColladaLoader()

	def apply(resource:String):ColladaFile = new ColladaFile(resource)
}


/** A Collada file, assets, library of elements and a scene. */
class ColladaFile(root:NodeSeq) {

	/** Try to load a file from the path. */
	def this(resource:String) {
		this(ColladaFile.loader.open(resource))
	}

	/** Meta-data on the file. */
	val asset:Assets = new Assets((root \\ "asset").head)
	
	/** All the objects and elements in the file. */
	val library = new Library(root)
	
	// TODO add the scene description.
	
	override def toString():String = "%s%n%s".format(asset, library)
}


/** Locate and fetch image data. */
trait ColladaLoader extends Loader {
    /** Try to locate a resource in the include path and load it. */
    def open(resource:String):NodeSeq
}


/** Default Collada loader that uses files in the local file system. */
class DefaultColladaLoader extends ColladaLoader {
    def open(resource:String):NodeSeq = {
    	scala.xml.XML.loadFile(findFile(resource, ColladaFile.path))
    }
}


/** Library of elements used an referenced inside a Collada file. */
class Library(root:NodeSeq) {
	/** All the cameras. */
	val cameras = new HashMap[String,Camera]()
	
	/** All the lights. */
	val lights = new HashMap[String,Light]()
	
	/** All the geometries by name. */
	val geometries = new HashMap[String,Geometry]()

	/** all the geometries by id. */
	val geometriesById = new HashMap[String,Geometry]()

	/** All the controllers. **/
	val controllers = new HashMap[String,Controller]()
	
	/** All the scenes. */
	val visualScenes = new HashMap[String,VisualScene]()

	parse(root)

	// All this get or runtime exception is quite bad .... Use option !!!
	
	def geometry(name:String):Option[Geometry] = geometries.get(name)

	def light(name:String):Option[Light] = lights.get(name)

	def camera(name:String):Option[Camera] = cameras.get(name)

	def controller(name:String):Option[Controller] = controllers.get(name)

	def visualScene(name:String):Option[VisualScene] = visualScenes.get(name)

	def parse(root:NodeSeq) {
		// Cameras
		(root \\ "library_cameras" \ "camera").foreach { node =>
			val camera = Camera(node)
			cameras += ((camera.name, camera))
		}  
		// Lights
		(root \\ "library_lights" \ "light").foreach { node =>
			val light = Light(node)
			lights += ((light.name, light))
		}
		// Geometries
		(root \\ "library_geometries" \ "geometry").foreach { node =>
			val geometry = Geometry(node)
			geometries += ((geometry.name, geometry))
			geometriesById += ((geometry.id, geometry))
		}
		// Controllers
		(root \\ "library_controllers" \ "controller").foreach { node =>
			val controller = Controller(node)
			controllers += ((controller.name, controller))
		}
		// Visual Scenes
		(root \\ "library_visual_scenes" \ "visual_scene").foreach { node =>
			val visualScene = VisualScene(node)
			visualScenes += ((visualScene.name, visualScene))
		}

		tieControllersWithGeomtries
	}

	protected def tieControllersWithGeomtries() {
		controllers.foreach { controller =>
			val geometry = geometriesById.get(controller._2.skin.source).getOrElse({
				throw new RuntimeException("Cannot tie controller (%s (%s)) skin with geometry %s, no such geometry id".format(
					controller._2.name, controller._2.id, controller._2.skin.source))
			})

			geometry.mesh.setController(controller._2)
		}
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
		if(!controllers.isEmpty) {
			controllers.foreach { item => builder.append(item._2.toString); builder.append("%n".format()) }
		}
		if(!visualScenes.isEmpty) {
			visualScenes.foreach { item => builder.append(item._2.toString); builder.append("%n".format()) }
		}
		builder.toString
	}
}