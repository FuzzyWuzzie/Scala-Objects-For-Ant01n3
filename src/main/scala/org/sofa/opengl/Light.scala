package org.sofa.opengl

import org.sofa.math.{Vector3, Vector4, NumberSeq3, Rgba}

class Light {}

/** WhiteLight companion object. */
object WhiteLight {
	def apply(x:Double, y:Double, z:Double, intensity:Float, specular:Float, ambient:Float):WhiteLight = new WhiteLight(x,y,z, intensity, specular, ambient)
}

/** A simple, non colored, light modelisation.
  *
  * This light class is able to setup a shader uniform variables with its
  * [[uniform()]] methods, provided
  * the shader uses either one light which is a structure of the form:
  *     struct WhiteLight {
  *	        vec3 pos;
  *         float intensity;
  *         float ambient;
  *         float specular;
  *     }
  * Or several such lights in an array. The unique light must be declared as:
  *     WhiteLight whitelight;
  * A set of lights must be declared as:
  *     WhiteLight whitelight[4];
  * for example. */
class WhiteLight(x:Double, y:Double, z:Double, var intensity:Float, var specular:Float, var ambient:Float) extends Light {
	/** Light position. */
	val pos = Vector4(x, y, z, 1)

	def this(position:NumberSeq3, intensity:Float, specular:Float, ambient:Float) {
		this(position.x, position.y, position.z, intensity, specular, ambient)
	}

	/** Setup a given shader uniform variables based on the assertion that
	  * the shader defines one light with the structure defined in the main documentation
	  * bloc of this class. */
	def uniform(shader:ShaderProgram, camera:Camera) {
		shader.uniform("whitelight.pos",       Vector3(camera.modelview.top * pos))
		shader.uniform("whitelight.intensity", intensity)
		shader.uniform("whitelight.specular",  specular)
		shader.uniform("whitelight.ambient",   ambient)
	}

	/** If only the light position changed, you can setup it using this method. */
	def uniformPosition(shader:ShaderProgram, camera:Camera) {
		shader.uniform("whitelight.pos", Vector3(camera.modelview.top * pos))
	}

	/** Setup a given shader uniform variables based on the assertion that
	  * the shader defines several lights with the structure defined in the main documentation
	  * bloc of this class. */
	def uniform(index:Int, shader:ShaderProgram, camera:Camera) {
		shader.uniform("whitelight[%d].pos".format(index), Vector3(camera.modelview.top * pos))
		shader.uniform("whitelight[%d].intensity".format(index), intensity)
		shader.uniform("whitelight[%d].specular".format(index),  specular)
		shader.uniform("whitelight[%d].ambient".format(index),   ambient)
	}

	/** If only the light position changed, you can setup it using this method. */
	def uniformPosition(index:Int, shader:ShaderProgram, camera:Camera) {
		shader.uniform("whitelight[%d].pos".format(index), Vector3(camera.modelview.top * pos))
	}
}

/** A simple hemisphere ligh modelisation.
  *
  * To use the [[uniform()]] methods, your shader must have a `HemisphereLight` structure
  * like this:
  *     struct HemisphereLight {
  *         vec3 pos;
  *         vec3 skyColor;
  *         vec3 groundColor;
  *     }
  * Furthermore the hemisphere light must be declared as:
  *     HemisphereLight hemilight;
  */
class HemisphereLight(x:Double, y:Double, z:Double, val skyColor:Rgba, val groundColor:Rgba) {
	/** Light position. */
	val pos = Vector4(x, y, z, 1)

	def this(position:NumberSeq3, sky:Rgba, ground:Rgba) {
		this(position.x, position.y, position.z, sky, ground)
	}

	def uniform(shader:ShaderProgram, camera:Camera) {
		shader.uniform("hemilight.pos",         Vector3(camera.modelview.top * pos))
		shader.uniform("hemilight.skyColor",    skyColor)
		shader.uniform("hemilight.groundColor", groundColor)
	}

	def uniformPosition(index:Int, shader:ShaderProgram, camera:Camera) {
		shader.uniform("hemilight.pos", Vector3(camera.modelview.top * pos))
	}
}