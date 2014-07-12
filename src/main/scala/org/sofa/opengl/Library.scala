package org.sofa.opengl

import java.io.IOException

import scala.collection.mutable.HashMap

import org.sofa.{FileLoader, Timer}
import org.sofa.opengl.text.{GLFont, GLString}
import org.sofa.opengl.mesh.{Mesh, PlaneMesh, CubeMesh, WireCubeMesh, AxisMesh, LinesMesh, VertexAttribute}
import org.sofa.opengl.armature.{Armature, Joint}
import org.sofa.opengl.armature.behavior.{ArmatureBehavior, Wait, LerpToAngle, LerpToPosition, LerpToScale, LerpMove, InParallel, InSequence, Loop, Switch, LerpKeyArmature}

import scala.xml.{XML, Elem, Node, NodeSeq}


/** When a resource cannot be loaded. */
case class NoSuchResourceException(msg:String,nested:Throwable=null) extends Exception(msg,nested)


/** When the resources XML file cannot be properly parsed. */
case class ResourcesParseException(msg:String,nested:Throwable=null) extends Exception(msg,nested)


/** A resource. */
abstract class ResourceDescriptor[+T](val name:String) {
	/** The resource (lazily load it when needed). Throws NoSuchResourceException
	  * if the resource cannot be loaded. */
	def value(gl:SGL):T
}


/** A set of resources of a given type. */
abstract class Library[T](val gl:SGL) extends Iterable[(String,ResourceDescriptor[T])] {
	/** Set of loaded resources. */
	protected val library = new HashMap[String,ResourceDescriptor[T]]

	/** True if the library contains a resource by the given `name`. */
	def contains(name:String):Boolean = library.contains(name)

	/** Add a new resource to the library. If `load` is true, the resource is 
	  * loaded when added, else it is loaded lazily at first access to its value. */
	def add(newResource:ResourceDescriptor[T], load:Boolean = false) { library += (newResource.name -> newResource); if(load) newResource.value(gl) }

	/** Add a new resource to the library. If `load` is true, the resource is 
	  * loaded when added, else it is loaded lazily at first access to its value. */
	def +=(newResource:ResourceDescriptor[T], load:Boolean = false) { add(newResource, load) }

	/** Load or retrieve a resource. */
	def get(gl:SGL, name:String):T = library.get(name).getOrElse(
			throw new NoSuchResourceException("resource '%s' unknown, did you put it in the Library ? use Library.add()".format(name))
		).value(gl)

	/** Load or retrieve a resource, or if not found, run the given code. The result of the
	  * given code is not inserted in the library since it is not a resource.
	  * See `getOrAdd()`. */
	def getOr(gl:SGL, name:String)(code: => T):T = {
		if(library.contains(name))
			 library.get(name).get.value(gl)
		else code
	}

	/** Retrieve a resource if it already exists, else add it via a descript and return it. */
	def addAndGet(gl:SGL, name:String, newResource:ResourceDescriptor[T]):T = library.get(name) match {
		case Some(res) => res.value(gl)
		case None => {
			add(newResource, true)
			library.get(name) match {
				case Some(res) => res.value(gl)
				case None => throw new NoSuchResourceException("resource '%s' unknown, or cannot load it".format(name))
			}
		}
	}

	/** Retrieve a resource if it already exists, else add it by calling `resCreator` to create
	  * the resource descriptor, then return it. */
	def getOrAdd(gl:SGL, name:String)(resCreator:(SGL,String)=>ResourceDescriptor[T]):T = library.get(name) match {
		case Some(res) => res.value(gl)
		case None => {
			val resource = resCreator(gl, name)
			add(resource)
			library.get(name) match {
				case Some(res) => res.value(gl)
				case None => throw new NoSuchResourceException("resource '%s' unknown, or cannot load it.".format(name))
			}
		}
	}

	/** Remove an free a previously loaded resource. */
	def forget(name:String) { library -= name }

	def iterator = new Iterator[(String,ResourceDescriptor[T])] {
		var i = library.iterator
		def hasNext = i.hasNext
		def next = i.next
	}
}


// == Libraries ==========================================


object Libraries {
	protected final val Vector2Expression     = """\s*\(\s*(\s*[-+]?\d+\.?\d*\s*)\s*,\s*(\s*[-+]?\d+\.?\d*\s*)\s*\)\s*""".r
	protected final val DoubleExpression      = """([+-]?\d+\.?\d*?)""".r
	protected final val PositiveIntExpression = """\s*(\+?\d+)\s*""".r
	protected final val WaitExpression        = """\s*wait\s*\(\s*(\d+)\s*\)\s*""".r

	def apply(gl:SGL):Libraries = new Libraries(gl)
}


/** The set of libraries for shaders, textures and models. */
class Libraries(gl:SGL) {
	import Libraries._
	
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

	/** Behavior resources. */
	val behaviors = BehaviorLibrary(gl)

	/** Add a new resource in the corresponding library. */
	def addResource(res:ResourceDescriptor[AnyRef]) {
		res match {
			case r:ShaderResource   ⇒ shaders.add(r)
			case r:TextureResource  ⇒ textures.add(r)
			case r:ModelResource    ⇒ models.add(r)
			case r:FontResource     ⇒ fonts.add(r)
			case r:ArmatureResource ⇒ armatures.add(r)
			case r:BehaviorResource ⇒ behaviors.add(r)
			case _ ⇒ throw NoSuchResourceException("unknown kind of resource %s".format(res))
		}
	}

	/** Add a new resource in the corresponding library. */
	def +=(res:ResourceDescriptor[AnyRef]) { addResource(res) }

	/** Add resources and pathes under the form of an XML file.
	  * The file is searched in the classpath using the class loader
	  * resources. For example if it is at the root of the class path,
	  * put a / in front of the file name. */
	def addResources(fileName:String) { 
		val res = getClass.getResource(fileName)

		if(res eq null)
			throw new IOException("cannot find resource %s".format(fileName))

		addResources(XML.load(res))
	}

	/** Add resources and pathes under the form of an XML file.
	  *
	  * The format is the following:
	  *   
	  *		<resources>
	  *			<pathes>
	  *				<shader>path/to/shaders</shader>
	  *				<tex>path/to/texturess</tex>
	  *				<armature>path/to/armatures</armature>
	  *				<tex>there/can/be/several/pathes</tex>
	  *             <behavior>path/to/sifz/behavior/descriptions</behavior>
	  *			</pathes>
	  *			<shaders>
	  *				<shader id="mandatoryId" vert="vertexShader" frag="fragmentShader"/>
	  *			</shader>
	  *			<tex>
	  *				<tex id="mandatoryId" res="resource"/>
	  *				<tex id="anId" res="aResource" mipmap="sameAsTexMipMap" minfilter="" magfilter="" alpha="" wrap=""/>
	  *			</texs>
	  *         <armatures>
	  *             <armature id="man" tex="man-tex" shader="man-shader" svg="man-svg" scale="1.0"/>
	  *         </armatures>
	  *         <behaviors>
	  *             <in-parallel      id="S" arm="S" behaviors="S,S,S,S,S"/>
	  *				<in-sequence      id="S" arm="S" behaviors="S,S,S,S,S"/>
	  *             <loop             id="S" arm="S" limit="L" behaviors="S,S,S,S,S"/>
	  *             <switch           id="S" arm="S" joints="S,S,S,S,S" duration="L"/>
	  *             <lerp-to-angle    id="S" arm="S" joint="S" value="" duration="L"/>
	  *             <lerp-to-position id="S" arm="S" joint="S" value="(D,D)" duration="L"/>
	  *				<lerp-to-scale    id="S" arm="S" joint="S" value="(D,D)" duration="L"/>
	  *             <lerp-move        id="S" arm="S" joint="S" value="(D,D)", duration="L"/>
	  *             <lerp-keys        id="S" arm="S" filename="S" scale="D"/>
	  *				<wait             id="S" arm="S" duration="L"/>
	  *         </behaviors>
	  *		</resources>
	  *
	  * See the [[TexParams]] class for an explanation of the attributes of tex elements. */
	def addResources(xml:Elem) {
		Timer.timer.measure("Library: addResources()") {
			parsePathes(   xml \\ "pathes")
			parseShaders(  xml \\ "shaders")
			parseTexs(     xml \\ "texs")
			parseArmatures(xml \\ "armatures")
			parseBehaviors(xml \\ "behaviors")
		}
	}

	protected def parsePathes(nodes:NodeSeq) {
		nodes \\ "shader"   foreach { Shader.path           += _.text }		
		nodes \\ "tex"      foreach { Texture.path          += _.text }
		nodes \\ "armature" foreach { Armature.path         += _.text }
		nodes \\ "behavior" foreach { ArmatureBehavior.path += _.text }
	}

	protected def parseShaders(nodes:NodeSeq) {
		nodes \\ "shader" foreach { shader ⇒
			shaders.add(ShaderResource(
				(shader \\ "@id").text,
				(shader \\ "@vert").text,
				(shader \\ "@frag").text))
		}
	}

	protected def parseTexs(nodes:NodeSeq) {
		nodes \\ "tex" foreach { tex ⇒
			val id  = (tex \\ "@id").text
			val res = (tex \\ "@res").text
			val params = TexParams(
				mipMap = (tex \\ "@mipmap").text.toLowerCase match {
						case "load"     ⇒ TexMipMap.Load
						case "generate" ⇒ TexMipMap.Generate
						case _          ⇒ TexMipMap.No
					},
				minFilter = (tex \\ "@minfilter").text.toLowerCase match {
						case "nearest"                 ⇒ TexMin.Nearest
						case "nearestandmipmapnearest" ⇒ TexMin.NearestAndMipMapNearest
						case "linearandmipmapnearest"  ⇒ TexMin.LinearAndMipMapNearest
						case "nearestandmipmaplinear"  ⇒ TexMin.NearestAndMipMapLinear
						case "linearandmipmaplinear"   ⇒ TexMin.LinearAndMipMapLinear
						case _                         ⇒ TexMin.Linear
					},
				magFilter = (tex \\ "@magfilter").text.toLowerCase match {
						case "nearest" ⇒ TexMag.Nearest
						case _         ⇒ TexMag.Linear
					},
				alpha = (tex \\ "@alpha").text.toLowerCase match {
						case "premultiply" ⇒ TexAlpha.Premultiply
						case _             ⇒ TexAlpha.Nop
					},
				wrap = (tex \\ "@wrap").text.toLowerCase match {
						case "clamp"          ⇒ TexWrap.Clamp
						case "mirroredrepeat" ⇒ TexWrap.MirroredRepeat
						case _                ⇒ TexWrap.Repeat
					}
			)

			textures.add(TextureResource(id, res, params))
		}
	}

	protected def parseArmatures(nodes:NodeSeq) {
		import Libraries._
		nodes \\ "armature" foreach { armature ⇒
			armatures.add(ArmatureResource(
				(armature \\ "@id").text,
				(armature \\ "@tex").text,
				(armature \\ "@shader").text,
				(armature \\ "@svg").text,
				(armature \\ "@armatureid").text,
				this,
				(armature \\ "@scale").text match {
					case DoubleExpression(dbl)    => dbl.toDouble
//					case PositiveIntExpression(i) => i.toInt
					case _                        => 1.0
				}))
		}
	}

	/** Parse the behaviors. */
	protected def parseBehaviors(nodes:NodeSeq) {
		// TODO handle errors !!!!!

	  	nodes foreach { node =>
		  	node.child foreach { child =>
			  	child match {
			  		case elem:Elem => {
			  			try {
					  		val armature = armatures.get(gl, (elem \\ "@arm").text)
					  		val name     = (elem \\ "@id").text
					  		
					  		elem match {
					  			case node:Node if node.label == "in-parallel" => {
					  				behaviors += BehaviorResource(name, InParallel(
					  					name, parseArray((node \\ "@behaviors").text):_*))
					  			}
					  			case node:Node if node.label == "in-sequence" => {
					  				behaviors += BehaviorResource(name, InSequence(
					  					name, parseArray((node \\ "@behaviors").text):_*))
					  			}
					  			case node:Node if node.label == "loop" => {
					  				behaviors += BehaviorResource(name, Loop(
					  					name, optInt((node \\ "@limit").text), parseArray((node \\ "@behaviors").text):_*))
					  			}
					  			case node:Node if node.label == "switch" => {
					  				behaviors += BehaviorResource(name, Switch(
					  					name, (node \\ "@duration").text.toLong, parseJoints(armature, (node \\ "@joints").text):_*))
					  			}
					  			case node:Node if node.label == "lerp-to-angle" => {
					  				behaviors += BehaviorResource(name, LerpToAngle(
					  					name, armature \\ (node \\ "@joint").text,
					  					(node \\ "@value").text.toDouble, (node \\ "@duration").text.toLong))
					  			}
					  			case node:Node if node.label == "lerp-to-position" => {
					  				behaviors += BehaviorResource(name, LerpToPosition(
					  					name, armature \\ (node \\ "@joint").text,
					  					parseVector2((node \\ "@value").text), (node \\ "@duration").text.toLong))
					  			}
					  			case node:Node if node.label == "lerp-to-scale" => {
					  				behaviors += BehaviorResource(name, LerpToScale(
					  					name, armature \\ (node \\ "@joint").text,
					  					parseVector2((node \\ "@value").text), (node \\ "@duration").text.toLong))
					  			}
					  			case node:Node if node.label == "lerp-move" => {
					  				behaviors += BehaviorResource(name, LerpMove(
					  					name, armature \\ (node \\ "@joint").text,
					  					parseVector2((node \\ "@value").text), (node \\ "@duration").text.toLong))
					  			}
					  			case node:Node if node.label == "lerp-keys" => {
					  				behaviors += BehaviorResource(name, LerpKeyArmature(
					  					name, armature, (node \\ "@filename").text, (node \\ "@scale").text.toDouble))
					  			}
					  			case node:Node if node.label == "wait" => {
					  				behaviors += BehaviorResource(name, Wait(name, (node \\ "@duration").text.toLong))
					  			}
					  			case _ => throw new RuntimeException("unknown behavior %s".format(node.label))
				  			}
						} catch {
							case e:Throwable => {
								throw new ResourcesParseException("parse error in '%s' (cause: %s)".format(elem.toString, e.getMessage),e)
							}
						}
				  	}
				  	case _ => {}
				}
	  		}
	  	}
	}

	/** Return the position integer stored in `intExp` or -1 if not an integer. */
	protected def optInt(intExp:String):Int = intExp match {
		case PositiveIntExpression(i) => i.toInt
		case _                        => -1
	}

	/** Parse two double numbers inside parenthesis separated by a comma. */
	protected def parseVector2(pos:String):(Double,Double) = pos match {
		case Vector2Expression(a,b) => (a.toDouble,b.toDouble) 
		case _ => throw new RuntimeException("cannot parse position '%s'".format(pos))
	}

	/** Parse an array of behavior names separated by commas. */
	protected def parseArray(behaviorList:String):Array[ArmatureBehavior] = behaviorList.split(",").map { s =>
	 	s match {
	 		case WaitExpression(duration) => {
	 			val id = s"wait(${duration})"
	 			
	 			if(! behaviors.contains(id))
	 				behaviors.add(BehaviorResource(id, Wait(id, duration.toLong)))

	 			behaviors.get(gl,id)
	 		}
	 		case _ => {
	 			behaviors.get(gl,s.trim)
	 		}
	 	}
	}

	/** Parse an array of joint name in the `armature` separated by commas. */
	protected def parseJoints(armature:Armature, jointList:String):Array[Joint] = jointList.split(",").map(s => armature \\ s.trim)

	override def toString():String = {
		val result = new StringBuilder()

		result ++= "shaders   (%d)%n".format(shaders.size)
		result ++= "textures  (%d)%n".format(textures.size)
		result ++= "models    (%d)%n".format(models.size)
		result ++= "fonts     (%d)%n".format(fonts.size)
		result ++= "behaviors (%d)%n".format(behaviors.size)
		result ++= "armatures (%d)%n".format(armatures.size)

		result.toString
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


object ModelResource {
	def apply(name:String, fileName:String, geometry:String):ModelResource = new ModelResource(name,fileName, geometry)
	def apply(name:String, mesh:Mesh):ModelResource = new ModelResource(name, mesh)
}

class ModelResource(name:String, mesh:Mesh, aFileName:String = "", aGeometry:String = "") extends ResourceDescriptor[Mesh](name) {
	private[this] var data:Mesh = mesh

	private[this] var fileName = aFileName

	private[this] var geometry = aGeometry

	def this(name:String, fileName:String, geometry:String) {
		this(name, null, fileName, geometry)
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

class ModelLibrary(gl:SGL) extends Library[Mesh](gl)


// == Fonts ============================================


object FontResource { def apply(name:String,fontName:String,size:Int):FontResource = new FontResource(name,fontName,size) }

class FontResource(name:String, val fontName:String, val size:Int) extends ResourceDescriptor[GLFont](name) {
	private[this] var data:GLFont = null

	def value(gl:SGL):GLFont = {
		throw NoSuchResourceException("TODO")
	}
}

object FontLibrary { def apply(gl:SGL):FontLibrary = new FontLibrary(gl) }

class FontLibrary(gl:SGL) extends Library[GLFont](gl)


// == Armatures ========================================


object ArmatureResource {
	def apply(name:String, texRes:String, shaderRes:String, fileName:String, armatureId:String, libraries:Libraries, scale:Double=1.0):ArmatureResource = {
		new ArmatureResource(name, texRes, shaderRes, fileName, armatureId, libraries, scale)
	}
}

class ArmatureResource(
			name:String,
			texRes:String,
			shaderRes:String,
			fileName:String,
			armatureId:String,
			val libraries:Libraries,
			private var data:Armature,
			scale:Double=1.0) extends ResourceDescriptor[Armature](name) {

	def this(name:String,
			 texRes:String,
			 shaderRes:String, fileName:String, armatureId:String, libraries:Libraries, scale:Double) {
		this(name, texRes, shaderRes, fileName, armatureId, libraries, null, scale)
	}

	def this(name:String, armature:Armature, libraries:Libraries, scale:Double) {
		this(name, armature.texResource, armature.shaderResource, "Armature", null, libraries, armature, scale)
	}

	def value(gl:SGL):Armature = {
		if(data eq null) {
			try {
				data = Armature.loader.open(name, texRes, shaderRes, fileName, armatureId, scale)
				data.init(gl, libraries)
			} catch {
				case e:IOException => throw NoSuchResourceException(e.getMessage, e)
			}
		} 

		data
	}
}

object ArmatureLibrary { def apply(gl:SGL):ArmatureLibrary = new ArmatureLibrary(gl) }

class ArmatureLibrary(gl:SGL) extends Library[Armature](gl)


// == Behaviors ========================================


object BehaviorResource {
	def apply(name:String, behavior:ArmatureBehavior):BehaviorResource = {
		new BehaviorResource(name, behavior)
	}
}

class BehaviorResource(name:String, val data:ArmatureBehavior) extends ResourceDescriptor[ArmatureBehavior](name) {
	def value(gl:SGL):ArmatureBehavior = data
}

object BehaviorLibrary { def apply(gl:SGL):BehaviorLibrary = new BehaviorLibrary(gl) }

class BehaviorLibrary(gl:SGL) extends Library[ArmatureBehavior](gl)

