package org.sofa.gfx

import java.io.IOException
import java.net.URL

import scala.collection.mutable.HashMap

import org.sofa.{FileLoader, Timer}
import org.sofa.gfx.text.{GLFont, GLString}
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


/** When a resource cannot be loaded. */
case class NoSuchResourceException(msg:String,nested:Throwable=null) extends Exception(msg,nested)


/** When the resources XML file cannot be properly parsed. */
case class ResourcesParseException(msg:String,nested:Throwable=null) extends Exception(msg,nested)


/** A resource. */
abstract class ResourceDescriptor[+T](val id:String) {
	/** The resource (lazily load it when needed). Throws NoSuchResourceException
	  * if the resource cannot be loaded. */
	def value(gl:SGL):T

	/** To ensure dealocation of NIO parts. Called automatically. By default does nothing. */
	def forget(gl:SGL) {}
}


/** A set of resources of a given type. */
abstract class Library[T](val gl:SGL) extends Iterable[(String,ResourceDescriptor[T])] {
	/** Set of loaded resources. */
	protected[this] val library = new HashMap[String,ResourceDescriptor[T]]

	/** True if the library contains a resource by the given `name`. */
	def contains(name:String):Boolean = library.contains(name)

	/** Add a new resource to the library. If `load` is true, the resource is 
	  * loaded when added, else it is loaded lazily at first access to its value. */
	def add(newResource:ResourceDescriptor[T], load:Boolean = false) { library += (newResource.id -> newResource); if(load) newResource.value(gl) }

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

	/** Retrieve a resource if it already exists, else add it via a description and return it. */
	def getOrAdd(gl:SGL, name:String, newResource:ResourceDescriptor[T]):T = library.get(name) match {
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

	/** Remove and free a previously loaded resource. */
	def forget(gl:SGL, name:String) {
		library.get(name) match {
			case Some(res) => {
				res.forget(gl)					
				library -= name
			}
			case None => {
				throw new NoSuchResourceException("resource '%s' unknown, or cannot load it.".format(name))
			}
		}
	}

	def iterator = new Iterator[(String,ResourceDescriptor[T])] {
		var i = library.iterator
		def hasNext = i.hasNext
		def next = i.next
	}

	override def toString():String = s"""Library[${library.keys.mkString(",")}]"""
}


// == Libraries ==========================================


object Libraries {
	protected final val Vector2Expression     = """\s*\(\s*(\s*[-+]?\d+\.?\d*\s*)\s*,\s*(\s*[-+]?\d+\.?\d*\s*)\s*\)\s*""".r
	protected final val DoubleExpression      = """([+-]?\d+\.?\d*?)""".r
	protected final val PositiveIntExpression = """\s*(\+?\d+)\s*""".r

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

	/** Type faces & font resources. */
	val typeFaces = TypeFaceLibrary(gl)

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
			case r:TypeFaceResource ⇒ typeFaces.add(r)
			case r:ArmatureResource ⇒ armatures.add(r)
			case r:BehaviorResource ⇒ behaviors.add(r)
			case _ ⇒ throw NoSuchResourceException("unknown kind of resource %s".format(res))
		}
	}

	/** Add a new resource in the corresponding library. */
	def +=(res:ResourceDescriptor[AnyRef]) { addResource(res) }

	/** Add resources and pathes under the form of an XML file or JSON file.
	  * The file is searched in the classpath using the class loader
	  * resources. For example if it is at the root of the class path,
	  * put a / in front of the file name. The file must end with `.xml`
	  * or `.json` to be loaded. */
	def addResources(fileName:String) { 
		val res = getClass.getResource(fileName)

		if(res eq null)
			throw new IOException("cannot find resource %s".format(fileName))

		if(fileName.endsWith(".xml"))
			addResources(XML.load(res))
		else if(fileName.endsWith(".json"))
			addResourcesJSON(res)
		else throw new ResourcesParseException(s"don't know how to process file ${fileName}, use '.xml' or '.json' extension to indicate format.")
	}

// -- XML config loader -----------------------------------------------------------------

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
	  *				<font>path/to/fonts/files</font>
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
		// Timer.timer.measure("Library: addResources()") {
		// 	Timer.timer.measure("Library: path ") { parsePathes(   xml \\ "pathes") }
		// 	Timer.timer.measure("Library: shad ") { parseShaders(  xml \\ "shaders") }
		// 	Timer.timer.measure("Library: texs ") { parseTexs(     xml \\ "texs") }
		// 	Timer.timer.measure("Library: arms ") { parseArmatures(xml \\ "armatures") }
		// 	Timer.timer.measure("Library: beha ") { parseBehaviors(xml \\ "behaviors") }
		// }
		parsePathes(   xml \\ "pathes")
		parseShaders(  xml \\ "shaders")
		parseTexs(     xml \\ "texs")
		parseArmatures(xml \\ "armatures")
		parseBehaviors(xml \\ "behaviors")
	}

	protected def parsePathes(nodes:NodeSeq) {
		nodes \ "shader"   foreach { Shader.path           += _.text }		
		nodes \ "tex"      foreach { Texture.path          += _.text }
		nodes \ "armature" foreach { Armature.path         += _.text }
		nodes \ "behavior" foreach { ArmatureBehavior.path += _.text }
		nodes \ "font"     foreach { GLFont.path           += _.text }
	}

	protected def parseShaders(nodes:NodeSeq) {
		nodes \\ "shader" foreach { shader ⇒
			var geom = (shader \\ "@geom").text
			geom = if(geom == "") null else geom

			shaders.add(ShaderResource(
				(shader \\ "@id").text,
				(shader \\ "@vert").text,
				(shader \\ "@frag").text,
				geom))
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
				(armature \\ "@scale").text match {
					case DoubleExpression(dbl)    => dbl.toDouble
//					case PositiveIntExpression(i) => i.toInt
					case _                        => 1.0
				}, this, null))
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
					  		val armature = (elem \\ "@arm").text
					  		val name     = (elem \\ "@id").text
					  		
					  		elem match {
					  			case node:Node => {
					  				if(node.label == "in-parallel") {
					  					behaviors += BehaviorResource(name, InParallelDesc(
					  						name, parseArray((node \\ "@behaviors").text)), this)
					  				} else if(node.label == "in-sequence") {
					  					behaviors += BehaviorResource(name, InSequenceDesc(
					  						name, parseArray((node \\ "@behaviors").text)), this)
					  				} else if(node.label == "loop") {
					  					behaviors += BehaviorResource(name, LoopDesc(
					  						name, optInt((node \\ "@limit").text), parseArray((node \\ "@behaviors").text)), this)
					  				} else if(node.label == "switch") {
					  					behaviors += BehaviorResource(name, SwitchDesc(
					  						name, armature, parseJoints((node \\ "@joints").text), (node \\ "@duration").text.toLong), this)
					  				} else if(node.label == "lerp-to-angle") {
					  					behaviors += BehaviorResource(name, LerpToAngleDesc(
					  						name, armature, (node \\ "@joint").text,
					  						(node \\ "@value").text.toDouble, (node \\ "@duration").text.toLong), this)
					  				} else if(node.label == "lerp-to-position") {
					  					behaviors += BehaviorResource(name, LerpToPositionDesc(
					  						name, armature, (node \\ "@joint").text,
					  							parseVector2((node \\ "@value").text), (node \\ "@duration").text.toLong), this)
					  				} else if(node.label == "lerp-to-scale") {
					  					behaviors += BehaviorResource(name, LerpToScaleDesc(
					  						name, armature, (node \\ "@joint").text,
					  							parseVector2((node \\ "@value").text), (node \\ "@duration").text.toLong), this)
					  				} else if(node.label == "lerp-move") {
					  					behaviors += BehaviorResource(name, LerpMoveDesc(
					  						name, armature, (node \\ "@joint").text,
					  							parseVector2((node \\ "@value").text), (node \\ "@duration").text.toLong), this)
					  				} else if(node.label == "lerp-keys") {
					  					behaviors += BehaviorResource(name, LerpKeysDesc(
					  						name, armature, (node \\ "@filename").text, (node \\ "@scale").text.toDouble), this)
					  				} else if(node.label == "wait") {
					  					behaviors += BehaviorResource(name, WaitDesc(name, (node \\ "@duration").text.toLong), this)
					  				}
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
	protected def parseArray(behaviorList:String):Array[String] = behaviorList.split(",").map(_.trim)

	/** Parse an array of joint name in the `armature` separated by commas. */
	protected def parseJoints(jointList:String):Array[String] = jointList.split(",").map(_.trim)

	override def toString():String = {
		val result = new StringBuilder()

		result ++= "shaders    (%d)%n".format(shaders.size)
		result ++= "textures   (%d)%n".format(textures.size)
		result ++= "models     (%d)%n".format(models.size)
		result ++= "type faces (%d)%n".format(typeFaces.size)
		result ++= "behaviors  (%d)%n".format(behaviors.size)
		result ++= "armatures  (%d)%n".format(armatures.size)

		result.toString
	}

// -- JSON config loader (POJO mode (or POSO ?)) ---------------------------------------

	protected[this] var jsonMapper:ObjectMapper with ScalaObjectMapper = null

	/** Load the resources in JSON format.
	  *
	  * The format must follow exactly this scheme:
	  * 
	  *{
	  *	"pathes": {	
	  *		"shader":   [ "S", "S" ],
	  *		"texture":  [ "S", "S" ],
	  *		"armature": [ "S", "S" ],
	  *		"behavior": [ "S", "S" ],
	  *     "font":     [ "S", "S" ]
	  *	},
	  *	"shaders": [
	  *		{ "id": "S", "vertex": "S", "fragment": "S", "geometry": "S" }
	  *	],
	  *	"textures": [
	  *		{ "id": "S", "res": "S", "mipMap": "S", "minFilter": "S", "magFilter": "S", "alpha": "S", "wrap": "S" }
	  *	],
	  *	"armatures": [
	  *		{ "id": "S", "tex": "S", "shader": "S", "src": "S", "scale": "0.0" }
	  *	],
	  *	"behaviors": {
	  *		"in_parallel":      [ { "id": "S", "behaviors": [ "S" ,"S" ] } ],
	  *		"in_sequence":      [ { "id": "S", "behaviors": [ "S", "S" ]  } ],
	  *		"loop":             [ { "id": "S", "limit": 0, "behaviors": [ "S", "S" ] } ],
	  *		"waits":            [ { "id": "S", "duration": 0 } ]
	  *		"switch":           [ { "id": "S", "arm": "S", "joints": [ "S", "S" ], "duration": 0 } ],
	  *		"lerp_to_angle":    [ { "id": "S", "arm": "S", "joint": "S", "value": 0.0, "duration": 0 } ],
	  *		"lerp_to_position": [ { "id": "S", "arm": "S", "joint": "S", "value": [ 0.0, 0.0 ], "duration": 0 } ],
	  *		"lerp_to_scale":    [ { "id": "S", "arm": "S", "joint": "S", "value": [ 0.0, 0.0 ], "duration": 0 } ],
	  *		"lerp_move":        [ { "id": "S", "arm": "S", "joint": "S", "value": [ 0.0, 0.0 ], "duration": 0 } ],
	  *		"lerp_keys":        [ { "id": "S", "arm": "S", "src": "S", "scale": 0.0 } ],
	  *   }
	  * }
	  *
	  * Be careful, when the parse expects a double, you cannot pass an int, always add a dot to the number.
	  * Case matters. The parser accepts you forget some elements. For example on textures you can
	  * forget "mipMap", "minFilter", "magFilter", "alpha" and "wrap" if not needed. For loop, you can
	  * forget "limit" (set to zero by default) for infinite loops.
	  */
	def addResourcesJSON(jsonSrc:URL) {
		if(jsonMapper eq null) {
//			Timer.timer.measure("addResJSON.modules") {
				jsonMapper = new ObjectMapper() with ScalaObjectMapper

				jsonMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true)

				jsonMapper.registerModule(DefaultScalaModule)
				jsonMapper.registerSubtypes(classOf[Pathes], classOf[ShaderResource],
					classOf[ArmatureResource], classOf[TextureResource],
					classOf[BehaviorsDesc], classOf[InParallelDesc], classOf[InSequenceDesc],
					classOf[LoopDesc], classOf[SwitchDesc], classOf[LerpToAngleDesc],
					classOf[LerpToPositionDesc], classOf[LerpToScaleDesc],
					classOf[LerpMoveDesc], classOf[LerpKeysDesc], classOf[WaitDesc])
//			}
		}
		
//		Timer.timer.measure("addResJSON") {
			val conf:SOFAConf = jsonMapper.readValue(jsonSrc, classOf[SOFAConf])

			conf.pathes.shader   foreach { Shader.path           += _ }		
			conf.pathes.texture  foreach { Texture.path          += _ }
			conf.pathes.armature foreach { Armature.path         += _ }
			conf.pathes.behavior foreach { ArmatureBehavior.path += _ }
			conf.shaders foreach { shaders += _ }
			conf.textures foreach { t =>
				textures += TextureResource(t.id, t.res, TexParams(
						alpha     = TexAlpha.fromString(t.alpha),
						minFilter = TexMin.fromString(t.minFilter),
						magFilter = TexMag.fromString(t.magFilter),
						mipMap    = TexMipMap.fromString(t.mipMap),
						wrap      = TexWrap.fromString(t.wrap)))
				}
			conf.armatures foreach { a =>
				armatures += ArmatureResource(a.id, a.tex, a.shader,
						a.src, a.armatureid, a.scale, this)
				}
			def processBehavior[T<:BehaviorDesc](list:Array[T]) {
				if(list ne null) list.foreach { b => behaviors += BehaviorResource(b.id, b, this) }
			}	
			processBehavior(conf.behaviors.in_parallel     )
			processBehavior(conf.behaviors.in_sequence     )
			processBehavior(conf.behaviors.loop            )
			processBehavior(conf.behaviors.switch          )
			processBehavior(conf.behaviors.lerp_to_angle   )
			processBehavior(conf.behaviors.lerp_to_position)
			processBehavior(conf.behaviors.lerp_to_scale   )
			processBehavior(conf.behaviors.lerp_move       )
			processBehavior(conf.behaviors.lerp_keys       )
			processBehavior(conf.behaviors.waits           )
//		}

	}

}


// -- JSON config loading ----------------------------------
// -- POJO mode

case class SOFAConf(
	pathes:Pathes,
	shaders:Array[ShaderResource],
	textures:Array[TextureDesc],
	armatures:Array[ArmatureDesc],
	behaviors:BehaviorsDesc) {}

case class Pathes(
	shader:Array[String],
	texture:Array[String],
	armature:Array[String],
	behavior:Array[String],
	font:Array[String]) {}

case class TextureDesc(
	id:String,
	res:String,
	mipMap:String,
	minFilter:String,
	magFilter:String,
	alpha:String,
	wrap:String) {}

case class ArmatureDesc(
	id:String,
	tex:String,
	shader:String,
	src:String,
	armatureid:String,
	scale:Double)

case class BehaviorsDesc(
	in_parallel:Array[InParallelDesc],
	in_sequence:Array[InSequenceDesc],
	loop:Array[LoopDesc],
	switch:Array[SwitchDesc],
	lerp_to_angle:Array[LerpToAngleDesc],
	lerp_to_position:Array[LerpToPositionDesc],
	lerp_to_scale:Array[LerpToScaleDesc],
	lerp_move:Array[LerpMoveDesc],
	lerp_keys:Array[LerpKeysDesc],
	waits:Array[WaitDesc] ) {}