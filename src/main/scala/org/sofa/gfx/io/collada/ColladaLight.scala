package org.sofa.gfx.io.collada

import org.sofa.math.Rgba
import scala.xml.Node

/** A light in the Collada light library. */
class Light(val name:String, val color:Rgba) extends ColladaFeature {
}

/** A point light. */
class PointLight(name:String, color:Rgba, val constant_att:Double, linear_att:Double, quad_att:Double) extends Light(name, color) {
	override def toString():String = "pointLight(%s, %s, cstAtt %f, linAtt %f, quadAtt %f)".format(name, color, constant_att, linear_att, quad_att)
}

/** A directional light. */
class DirectionalLight(name:String, color:Rgba) extends Light(name, color) {
	override def toString():String = "dirLight(%s, %s)".format(name, color)
}

object Light {
	def apply(xml:Node):Light = {
		val name  = (xml \ "@name").text
		val light = (xml \\ "technique_common").head
		val point = light \\ "point"
		val dir   = light \\ "directional"
		if(!point.isEmpty) {
			new PointLight(name, parseColor((point \ "color").text),
					(point \ "constant_attenuation").text.toDouble,
					(point \ "linear_attenuation").text.toDouble,
					(point \ "quadratic_attenuation").text.toDouble)
		} else if(!dir.isEmpty) {
			new DirectionalLight(name, parseColor((dir \ "color").text))
		} else {
			null
		}
	}
	
	protected def parseColor(text:String):Rgba = {
		val colors = text.split(" ");
		var red    = 0.0
		var green  = 0.0
		var blue   = 0.0
		
		if(colors.length>0) { red   = colors(0).toDouble }
		if(colors.length>1) { green = colors(1).toDouble }
		if(colors.length>2) { blue  = colors(2).toDouble }
		
		new Rgba(red, green, blue, 1)
	}
}