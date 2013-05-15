package org.sofa.simu.oberon.renderer

import java.io.IOException

import scala.collection.mutable.HashMap

import org.sofa.FileLoader
import org.sofa.opengl.{SGL, ShaderProgram, MatrixStack, VertexArray, Camera, Shader, Texture, TexParams}
import org.sofa.opengl.text.{GLFont, GLString}
import org.sofa.opengl.mesh.{Mesh, PlaneMesh, CubeMesh, WireCubeMesh, AxisMesh, LinesMesh, VertexAttribute}

/** When a resource cannot be loaded. */
case class NoSuchResourceException(msg:String,nested:Throwable=null) extends Exception(msg,nested)

/** A resource. */
abstract class ResourceDescriptor[+T](val name:String) {
	/** The resource (lazily load it when needed). Throws NoSuchResourceException
	  * if the resource cannot be loaded. */
	def value(gl:SGL):T
}

/** A set of resources of a given type. */
abstract class Library[T](val gl:SGL) {
	/** Set of loaded resources. */
	protected val library = new HashMap[String,ResourceDescriptor[T]]

	/** Add a new resource to the library. If `load` is true, the resource is 
	  * loaded when added, else it is loaded lazily at first access to its value. */
	def add(newResource:ResourceDescriptor[T], load:Boolean = false) { library += (newResource.name -> newResource); if(load) newResource.value(gl) }

	/** Add a new resource to the library. If `load` is true, the resource is 
	  * loaded when added, else it is loaded lazily at first access to its value. */
	def +=(newResource:ResourceDescriptor[T], load:Boolean = false) { add(newResource, load) }

	/** Load or retrieve a resource. */
	def get(gl:SGL, name:String):T = {
		library.get(name).getOrElse(
			throw new NoSuchResourceException("resource %s unknown, use add()".format(name))
		).value(gl)
	}

	/** Remove an free a previously loaded resource. */
	def forget(name:String) { library -= name }
}

// == Libraries ==========================================

//object Libraries { def apply(gl:SGL):Libraries = { new Libraries(gl) } }

/** The set of libraries for shaders, textures and models. */
case class Libraries(gl:SGL) {
	/** Shader resources. */
	val shaders = ShaderLibrary(gl)

	/** Textures resources. */
	val textures = TextureLibrary(gl)

	/** Model resources. */
	val models = ModelLibrary(gl)

	/** Font resources. */
	val fonts = FontLibrary(gl)

	/** Armature resources. */
	val armatures = ArmatureLibrary(gl)

	/** Add a new resource in the corresponding library. */
	def addResource(res:ResourceDescriptor[AnyRef]) {
		res match {
			case r:ShaderResource   ⇒ shaders.add(r)
			case r:TextureResource  ⇒ textures.add(r)
			case r:ModelResource    ⇒ models.add(r)
			case r:FontResource     ⇒ fonts.add(r)
			case r:ArmatureResource ⇒ armatures.add(r)
			case _ ⇒ throw NoSuchResourceException("unknown kind of resource %s".format(res))
		}
	}
}

// == Shaders ============================================

object ShaderResource { def apply(name:String, vertex:String, fragment:String):ShaderResource = new ShaderResource(name, vertex, fragment) }

class ShaderResource(name:String, val vertex:String, val fragment:String) extends ResourceDescriptor[ShaderProgram](name) {
	private[this] var data:ShaderProgram = null

	def value(gl:SGL):ShaderProgram = {
		if(data eq null) {
			try {
				data = ShaderProgram(gl, name, vertex, fragment)
			} catch {
				case e:Exception ⇒ throw NoSuchResourceException(e.getMessage, e)
			}
		}

		data
	}
}

object ShaderLibrary { def apply(gl:SGL):ShaderLibrary = new ShaderLibrary(gl) }

class ShaderLibrary(gl:SGL) extends Library[ShaderProgram](gl)

// == Textures ============================================

object TextureResource { def apply(name:String,fileName:String,params:TexParams):TextureResource = new TextureResource(name, fileName, params) }

class TextureResource(
	name:String,
	val fileName:String,
	val params:TexParams)
		extends ResourceDescriptor[Texture](name) {
	
	private[this] var data:Texture = null

	def this(name:String, fileName:String) { this(name, fileName, TexParams()) }

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
}

object TextureLibrary { def apply(gl:SGL):TextureLibrary = new TextureLibrary(gl) }

class TextureLibrary(gl:SGL) extends Library[Texture](gl)

// == Models ============================================

object ModelResource { def apply(name:String,fileName:String):ModelResource = new ModelResource(name,fileName) }

class ModelResource(name:String, val fileName:String) extends ResourceDescriptor[Mesh](name) {
	private var data:Mesh = null

	def value(gl:SGL):Mesh = {
		throw NoSuchResourceException("TODO")
	}
}

object ModelLibrary { def apply(gl:SGL):ModelLibrary = new ModelLibrary(gl) }

class ModelLibrary(gl:SGL) extends Library[Mesh](gl)

// == Fonts ============================================

object FontResource { def apply(name:String,fontName:String,size:Int):FontResource = new FontResource(name,fontName,size) }

class FontResource(name:String, val fontName:String, val size:Int) extends ResourceDescriptor[GLFont](name) {
	private var data:GLFont = null

	def value(gl:SGL):GLFont = {
		throw NoSuchResourceException("TODO")
	}
}

object FontLibrary { def apply(gl:SGL):FontLibrary = new FontLibrary(gl) }

class FontLibrary(gl:SGL) extends Library[GLFont](gl)

// == Armatures ========================================

object ArmatureResource {
	def apply(name:String, texRes:String, shaderRes:String, fileName:String):ArmatureResource = {
		new ArmatureResource(name, texRes, shaderRes, fileName)
	}
}

class ArmatureResource(name:String, texRes:String, shaderRes:String, fileName:String, private var data:Armature) extends ResourceDescriptor[Armature](name) {

	def this(name:String, texRes:String, shaderRes:String, fileName:String) {
		this(name, texRes, shaderRes, fileName, null)
	}

	def this(name:String, armature:Armature) {
		this(name, armature.texResource, armature.shaderResource, null, armature)
	}

	def value(gl:SGL):Armature = {
		if(data eq null) {
			try {
				data = Armature.loader.open(name, texRes, shaderRes, fileName)
			} catch {
				case e:IOException => throw NoSuchResourceException(e.getMessage, e)
			}
		} 

		data
	}
}

object ArmatureLibrary { def apply(gl:SGL):ArmatureLibrary = new ArmatureLibrary(gl) }

class ArmatureLibrary(gl:SGL) extends Library[Armature](gl)