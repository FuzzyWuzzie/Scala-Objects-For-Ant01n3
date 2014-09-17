package org.sofa.gfx

import java.io.IOException
import java.net.URL

import scala.collection.mutable.HashMap

import org.sofa.{FileLoader, Timer}
import org.sofa.gfx.text.{GLFont, GLTypeFace, GLText, GLString}
import org.sofa.gfx.mesh.{Mesh, PlaneMesh, CubeMesh, WireCubeMesh, AxisMesh, LinesMesh, VertexAttribute}
import org.sofa.gfx.armature.{Armature, Joint}
import org.sofa.behavior.{Behavior, Wait, InParallel, InSequence, Loop}
import org.sofa.gfx.armature.behavior.{ArmatureBehavior, LerpToAngle, LerpToPosition, LerpToScale, LerpMove, Switch, LerpKeyArmature}

import scala.xml.{XML, Elem, Node, NodeSeq}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.core.{JsonParser}
import java.io.File


// == Shaders ============================================


/** Resource descriptor for shaders. */
case class ShaderResource(override val id:String, vertex:String, fragment:String) extends ResourceDescriptor[ShaderProgram](id) {

	private[this] var data:ShaderProgram = null

	def value(gl:SGL):ShaderProgram = {
		if(data eq null) {
			try {
				data = ShaderProgram(gl, id, vertex, fragment)
			} catch {
				case e:Exception ⇒ throw NoSuchResourceException(e.getMessage, e)
			}
		}

		data
	}
}

object ShaderLibrary { def apply(gl:SGL):ShaderLibrary = new ShaderLibrary(gl) }

/** A set of shaders. */
class ShaderLibrary(gl:SGL) extends Library[ShaderProgram](gl)


// == Textures ============================================


/** Resource descriptor for textures. */
case class TextureResource(override val id:String, fileName:String, params:TexParams) extends ResourceDescriptor[Texture](id) {
	
	private[this] var data:Texture = null

	def this(id:String, fileName:String) { this(id, fileName, TexParams()) }

	def value(gl:SGL):Texture = {
		if(data eq null) {
			try {
				data = new Texture(gl, fileName, params)
			} catch {
				case e:IOException ⇒ throw NoSuchResourceException(e.getMessage, e)
			}
		}

		data
	}

	override def forget(gl:SGL) {
		if(data ne null) {
			data.dispose
			data = null
		}
	}
}

object TextureLibrary { def apply(gl:SGL):TextureLibrary = new TextureLibrary(gl) }

/** A set of textures. */
class TextureLibrary(gl:SGL) extends Library[Texture](gl)


// == Models ============================================


object ModelResource {
	def apply(id:String, fileName:String, geometry:String):ModelResource = new ModelResource(id,fileName, geometry)
	def apply(id:String, mesh:Mesh):ModelResource = new ModelResource(id, mesh)
}

/** A resource descriptor for mesh models. */
class ModelResource(id:String, mesh:Mesh, aFileName:String = "", aGeometry:String = "") extends ResourceDescriptor[Mesh](id) {
	private[this] var data:Mesh = mesh

	private[this] var fileName = aFileName

	private[this] var geometry = aGeometry

	def this(id:String, fileName:String, geometry:String) {
		this(id, null, fileName, geometry)
	}

	def value(gl:SGL):Mesh = {
		if(data eq null) {
			try {
				data = Mesh.loader.open(fileName, geometry)
			} catch {
				case e:IOException => throw NoSuchResourceException(e.getMessage, e)
			}
		}

		data
	}
}

object ModelLibrary { def apply(gl:SGL):ModelLibrary = new ModelLibrary(gl) }

/** A set of models. */
class ModelLibrary(gl:SGL) extends Library[Mesh](gl)


// == Type faces & Fonts ============================================


/** A resource descriptor for type faces. */
case class TypeFaceResource(override val id:String, fontName:String, shader:ShaderProgram) extends ResourceDescriptor[GLTypeFace](id) {
	private[this] var data:GLTypeFace = null

	def value(gl:SGL):GLTypeFace = {
		if(data eq null)
			data = new GLTypeFace(gl, fontName, shader)
		data
	}
}

object TypeFaceLibrary { def apply(gl:SGL):TypeFaceLibrary = new TypeFaceLibrary(gl) }

/** A set of fonts. */
class TypeFaceLibrary(gl:SGL) extends Library[GLTypeFace](gl)


// == Armatures ========================================


/** A resource descriptor for armatures. */
case class ArmatureResource(
			override val id:String,
			texRes:String,
			shaderRes:String,
			fileName:String,
			armatureId:String,
			scale:Double,
			val libraries:Libraries, var data:Armature = null)
	extends ResourceDescriptor[Armature](id) {

	def this(id:String, texRes:String, shaderRes:String, fileName:String,
			 armatureId:String, libraries:Libraries, scale:Double) {
		this(id, texRes, shaderRes, fileName, armatureId, scale, libraries, null)
	}

	def this(id:String, armature:Armature, libraries:Libraries, scale:Double) {
		this(id, armature.texResource, armature.shaderResource, null, "Armature", scale, libraries, armature)
	}

	def value(gl:SGL):Armature = {
		if(data eq null) {
			try {
				data = Armature.loader.open(id, texRes, shaderRes, fileName, armatureId, scale)
				data.init(gl, libraries)
			} catch {
				case e:IOException => throw NoSuchResourceException(e.getMessage, e)
			}
		} 

		data
	}
}

object ArmatureLibrary { def apply(gl:SGL):ArmatureLibrary = new ArmatureLibrary(gl) }

/** A set of armatures. */
class ArmatureLibrary(gl:SGL) extends Library[Armature](gl)


// == Behaviors ========================================


/** Base behavior descriptor. */
abstract class BehaviorDesc(val id:String) { def get(gl:SGL, libraries:Libraries):Behavior }

case class InParallelDesc(override val id:String, behaviors:Array[String]) extends BehaviorDesc(id) {
	def get(gl:SGL, libraries:Libraries):Behavior = {
		InParallel(id, libraries.behaviors.behaviorArray(libraries, behaviors):_*)
	}
}
case class InSequenceDesc(override val id:String, behaviors:Array[String]) extends BehaviorDesc(id) {
	def get(gl:SGL, libraries:Libraries):Behavior = {
		InSequence(id, libraries.behaviors.behaviorArray(libraries, behaviors):_*)		
	}
}
case class LoopDesc(override val id:String, limit:Long, behaviors:Array[String]) extends BehaviorDesc(id) {
	def get(gl:SGL, libraries:Libraries):Behavior = {
		Loop(id, limit.toInt, libraries.behaviors.behaviorArray(libraries, behaviors):_*)				
	}
}
case class WaitDesc(override val id:String, duration:Long) extends BehaviorDesc(id) {
	def get(gl:SGL, libraries:Libraries):Behavior = {
		Wait(id, duration)
	}
}
case class SwitchDesc(override val id:String, arm:String, joints:Array[String], duration:Long) extends BehaviorDesc(id) {
	def get(gl:SGL, libraries:Libraries):Behavior = {
		val armature = libraries.armatures.get(gl, arm)
		Switch(id, duration, armature.jointArray(joints):_*)
	}
}
case class LerpToAngleDesc(override val id:String, arm:String, joint:String, value:Double, duration:Long) extends BehaviorDesc(id) {
	def get(gl:SGL, libraries:Libraries):Behavior = {
		val armature = libraries.armatures.get(gl, arm)
		LerpToAngle(id, armature \\ joint, value, duration)		
	}
}
case class LerpToPositionDesc(override val id:String, arm:String, joint:String, value:(Double,Double), duration:Long) extends BehaviorDesc(id) {
	def get(gl:SGL, libraries:Libraries):Behavior = {
		val armature = libraries.armatures.get(gl, arm)
		LerpToPosition(id, armature \\ joint, value, duration)
	}
}
case class LerpToScaleDesc(override val id:String, arm:String, joint:String, value:(Double,Double), duration:Long) extends BehaviorDesc(id) {
	def get(gl:SGL, libraries:Libraries):Behavior = {
		val armature = libraries.armatures.get(gl, arm)
		LerpToScale(id, armature \\ joint, value, duration)
	}
}
case class LerpMoveDesc(override val id:String, arm:String, joint:String, value:(Double,Double), duration:Long) extends BehaviorDesc(id) {
	def get(gl:SGL, libraries:Libraries):Behavior = {
		val armature = libraries.armatures.get(gl, arm)
		LerpMove(id, armature \\ joint, value, duration)
	}
}
case class LerpKeysDesc(override val id:String, arm:String, src:String, scale:Double) extends BehaviorDesc(id) {
	def get(gl:SGL, libraries:Libraries):Behavior = {
		val armature = libraries.armatures.get(gl, arm)
		LerpKeyArmature(id, armature, src, scale)
	}
}


/** A resource descriptor for behaviors. 
  * As there can exist a lot of descriptors, and to support automatic instantiation,
  * The real descriptors are descendants of the class [[BehaviorDesc]]. Each
  * descriptor is able to instantiate a behavior of its kind. */
case class BehaviorResource(override val id:String, desc:BehaviorDesc, libraries:Libraries) extends ResourceDescriptor[Behavior](id) {
	protected[this] var data:Behavior = null
	def value(gl:SGL):Behavior = {
		if(data eq null) {
			data = desc.get(gl, libraries)
		}

		data
	}
}


object BehaviorLibrary {
	protected final val WaitExpression = """\s*wait\s*\(\s*(\d+)\s*\)\s*""".r
	def apply(gl:SGL):BehaviorLibrary = new BehaviorLibrary(gl) 
}

/** A set of behaviors */
class BehaviorLibrary(gl:SGL) extends Library[Behavior](gl) {
	/** From an array of behavior ids, return an array of behaviors. */
	def behaviorArray(libraries:Libraries, list:Array[String]):Array[Behavior] =
		list.map { _ match {
	 		case BehaviorLibrary.WaitExpression(duration) => {
	 			val id = s"wait(${duration})"
	 			
	 			if(! library.contains(id))
	 				add(BehaviorResource(id, WaitDesc(id, duration.toLong), libraries))

	 			get(gl, id)
	 		}
	 		case s => {
	 			get(gl, s.trim)
	 		}
		}
	}
}

