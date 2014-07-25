package org.sofa.gfx.io.collada

import scala.xml.Node

/** A camera in the Collada camera library. */
class Camera(val name:String) extends ColladaFeature {}

/** A perspective camera. */
class CameraPerspective(name:String, val fovAxis:Axis.Value, val fov:Double, val aspectRatio:Double, val znear:Double, val zfar:Double) extends Camera(name) {
	override def toString():String = "camera(%s, fov %f(%s), ratio %f, znear %f, zfar %f)".format(name, fov, fovAxis, aspectRatio, znear, zfar)
}

object Camera {
	def apply(xml:Node):Camera = {
		val name  = (xml \ "@name").text
		val cam   = (xml \\ "optics" \\ "technique_common").head
		val persp = cam \ "perspective"
		
		if(!persp.isEmpty) {
			val xfov = (persp \ "xfov")
			val yfov = (persp \ "yfov")
			var fov  = 1.0
			var axis = Axis.X
			
			if(!xfov.isEmpty) {
				fov = xfov.text.toDouble
			} else {
				fov = yfov.text.toDouble
				axis = Axis.Y
			}
			
			new CameraPerspective(
				name,
				axis, fov,
				(persp \ "aspect_ratio").text.toDouble,
				(persp \ "znear").text.toDouble,
				(persp \ "zfar").text.toDouble)
		} else {
			throw new RuntimeException("Collada: ortho camera TODO")
		}
	}
}