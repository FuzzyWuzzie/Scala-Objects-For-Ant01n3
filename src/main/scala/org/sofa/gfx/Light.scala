package org.sofa.gfx

import org.sofa.math.{Vector3, Vector4, NumberSeq3, Rgba}


trait Light {
	/** Setup all the parameters of the light in the given `shader` excepted the position for positional lights. */
	def uniform(shader:ShaderProgram)

	/** When lights are in arrays, this indicate the light index in this array. Set to -1 to disable (by default). */
	def setIndex(i:Int)
}


trait PositionalLight extends Light {
	def pos:Vector4

	def moveAt(x:Double, y:Double, z:Double) { pos.set(x, y, z, pos.w) }

	def moveAt(p:NumberSeq3) { pos.set(p.x, p.y, p.z, pos.w) }

	/** Setup the position of the light in the given `shader`, transforming its position
	  * using the given `space` if non null. */
	def uniformPosition(shader:ShaderProgram, space:Space=null)
}


// == White light ===============================================================


/** WhiteLight companion object. */
object WhiteLight {
	def apply(x:Double, y:Double, z:Double, Kd:Float, Ks:Float, Ka:Float, roughness:Float=32f):WhiteLight = new WhiteLight(x,y,z, Kd, Ks, Ka, roughness)
}


/** A simple, non colored, light modelisation.
  *
  * This light class is able to setup a shader uniform variables with its
  * `uniform()` methods, provided
  * the shader uses either one light which is a structure of the form:
  *     struct WhiteLight {
  *	        vec3 pos;
  *         float Kd;
  *         float Ks;
  *         float Ka;
  *         float roughness;
  *     }
  * Or several such lights in an array. The unique light must be declared as:
  *     WhiteLight whitelight;
  * A set of lights must be declared as:
  *     WhiteLight whitelight[4];
  * for example. */
class WhiteLight(x:Double, y:Double, z:Double, var Kd:Float, var Ks:Float, var Ka:Float, var roughness:Float = 32f) extends PositionalLight {
	/** Light position. */
	val pos = Vector4(x, y, z, 1)

	/** When there are several lights, this is >= 0 and indicates the index of the light. */
	var index = -1

	def this(position:NumberSeq3, Kd:Float, Ks:Float, Ka:Float, roughness:Float) {
		this(position.x, position.y, position.z, Kd, Ks, Ka, roughness)
	}

	def setIndex(i:Int) { index = i }

	def uniform(shader:ShaderProgram) {
		if(index < 0) {
			shader.uniform("L.Kd", Kd)
			shader.uniform("L.Ks",  Ks)
			shader.uniform("L.Ka",   Ka)
			shader.uniform("L.R", roughness)
		} else {
			shader.uniform("L[%d].Kd".format(index), Kd)
			shader.uniform("L[%d].Ks".format(index),  Ks)
			shader.uniform("L[%d].Ka".format(index),   Ka)
			shader.uniform("L[%d].R".format(index), roughness)			
		}	
	}

	def uniformPosition(shader:ShaderProgram, space:Space=null) {
		if(index < 0)
		     shader.uniform("L.P", if(space ne null) Vector3(space.modelview.top * pos) else Vector3(pos))
		else shader.uniform("L[%d].P".format(index), if(space ne null) Vector3(space.modelview.top * pos) else Vector3(pos))
	}
}


// == Colored light =====================================================


object ColoredLight {
	def apply(x:Double, y:Double, z:Double, diffuse:Rgba, specular:Rgba, ambient:Rgba, Kd:Double, Ks:Double, Ka:Double, roughness:Double, constAtt:Double, linAtt:Double, quadAtt:Double):ColoredLight = {
		new ColoredLight(x,y,z, diffuse, specular, ambient, Kd, Ks, Ka, roughness, constAtt, linAtt, quadAtt)
	}
}


class ColoredLight(x:Double, y:Double, z:Double, val diffuse:Rgba, val specular:Rgba, val ambient:Rgba, val Kd:Double, val Ks:Double, val Ka:Double, val roughness:Double, val constAtt:Double, val linAtt:Double, val quadAtt:Double) extends PositionalLight {
	/** Light position. */
	val pos = Vector4(x, y, z, 1)

	/** When there are several lights, this is >= 0 and indicates the index of the light. */
	var index = -1

	def this(p:NumberSeq3, diffuse:Rgba, specular:Rgba, ambient:Rgba, Kd:Double, Ks:Double, Ka:Double, roughness:Double, constAtt:Double, linAtt:Double, quadAtt:Double) {
		this(p.x, p.y, p.z, diffuse, specular, ambient, Kd, Ks, Ka, roughness, constAtt, linAtt, quadAtt)
	}

	def this(p:NumberSeq3, diffuse:Rgba, Kd:Double, Ks:Double, Ka:Double, roughness:Double, quadAtt:Double) {
		this(p.x, p.y, p.z, diffuse, Rgba.White, diffuse, Kd, Ks, Ka, roughness, 0.0, 1.0, quadAtt)
	}

	def setIndex(i:Int) { index = i }

	def uniform(shader:ShaderProgram) {
		if(index < 0) {
			shader.uniform("L.Cd", diffuse)
			shader.uniform("L.Cs", specular)
			shader.uniform("L.Ca", ambient)
			shader.uniform("L.Kd", Kd.toFloat)
			shader.uniform("L.Ks", Ks.toFloat)
			shader.uniform("L.Ka", Ka.toFloat)
			shader.uniform("L.R", roughness.toFloat)
			shader.uniform("L.Ac", constAtt.toFloat)
			shader.uniform("L.Al", linAtt.toFloat)
			shader.uniform("L.Aq", quadAtt.toFloat)
		} else {
			shader.uniform("L[%d].Cd".format(index), diffuse)
			shader.uniform("L[%d].Cs".format(index), specular)
			shader.uniform("L[%d].Ca".format(index), ambient)
			shader.uniform("L[%d].Kd".format(index), Kd.toFloat)
			shader.uniform("L[%d].Ks".format(index), Ks.toFloat)
			shader.uniform("L[%d].Ka".format(index), Ka.toFloat)
			shader.uniform("L[%d].R".format(index), roughness.toFloat)
			shader.uniform("L[%d].Ac".format(index), constAtt.toFloat)
			shader.uniform("L[%d].Al".format(index), linAtt.toFloat)
			shader.uniform("L[%d].Aq".format(index), quadAtt.toFloat)
		}
	}

	def uniformPosition(shader:ShaderProgram, space:Space=null) {
		if(index < 0)
		     shader.uniform("L.P", if(space ne null) Vector3(space.modelview.top * pos) else Vector3(pos))
		else shader.uniform("L[%d].P".format(index), if(space ne null) Vector3(space.modelview.top * pos) else Vector3(pos))
	}
}


// == Hemisphere light ===========================================================


/** A simple hemisphere ligh modelisation.
  *
  * To use the `uniform()` methods, your shader must have a `HemisphereLight` structure
  * like this:
  *     struct HemisphereLight {
  *         vec3 pos;
  *         vec3 skyColor;
  *         vec3 groundColor;
  *     }
  * Furthermore the hemisphere light must be declared as:
  *     HemisphereLight hemilight;
  */
class HemisphereLight(x:Double, y:Double, z:Double, val skyColor:Rgba, val groundColor:Rgba) extends PositionalLight {
	/** Light position. */
	val pos = Vector4(x, y, z, 1)

	/** When there are several lights, this is >= 0 and indicates the index of the light. */
	var index = -1

	def this(position:NumberSeq3, sky:Rgba, ground:Rgba) {
		this(position.x, position.y, position.z, sky, ground)
	}

	def setIndex(i:Int) { index = i }

	def uniform(shader:ShaderProgram) {
		if(index < 0) {
			shader.uniform("L.skyColor",    skyColor)
			shader.uniform("L.groundColor", groundColor)
		} else {
			shader.uniform("L[%d].skyColor".format(index),    skyColor)
			shader.uniform("L[%d].groundColor".format(index), groundColor)
		}
	}

	def uniformPosition(shader:ShaderProgram, space:Space=null) {
		if(index < 0)
		     shader.uniform("L.P", if(space ne null) Vector3(space.modelview.top * pos) else Vector3(pos))
		else shader.uniform("L[%d].P".format(index), if(space ne null) Vector3(space.modelview.top * pos) else Vector3(pos))
	}
}