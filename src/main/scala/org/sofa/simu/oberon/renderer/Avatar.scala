package org.sofa.simu.oberon.renderer

import org.sofa.math.{Vector3, Point3, NumberSeq3, Rgba, SpatialCube, SpatialHash, SpatialHashException}

/** When an avatar is not found. */
case class NoSuchAvatarException(msg:String) extends Exception(msg)

/** When an axis does not exist in an avatar or screen. */
case class NoSuchAxisException(msg:String) extends Exception(msg)

/** When a value change on an axis failed. */
case class InvalidValuesException(msg:String) extends Exception(msg)

/** Create actor representators. */
trait AvatarFactory {
	def screenFor(name:String, screenType:String):Screen
	def avatarFor(name:String, avatarType:String, indexed:Boolean):Avatar
}

/** Base for each renderable thing in (avatars, screens, etc.). */
trait Renderable {
	/** called before the first render. */
	def begin()

	/** Render one frame of the screen. */
	def render()

	/** Change the state of automatically animated "things". */
	def animate()

	/** Called after the last render. */
	def end()
}

/** Graphical representation of an actor in the renderer. */
abstract class Avatar(val name:String) extends Renderable {

	/** Avatar center. */
	val pos = Point3(0,0,0)

	/** Avatar size. */
	val size = Vector3(1,1,1)

	/** True after begin and before end. */
	protected var rendering = false

	/** Change the avatar position. */
	def changePosition(newPos:NumberSeq3) {
		pos.copy(newPos) 
		if(isIndexed) index.changedPosition
	}

	/** Change the avatar size. */
	def changeSize(newSize:NumberSeq3) {
		size.copy(newSize)
		if(isIndexed) index.changedSize
	}

	/** By default throw a NoSuchAxisException for any axis. */
	def change(axis:String, values:AnyRef*) {
		throw NoSuchAxisException("avatar %s has no axis named %s".format(name, axis))
	}

	/** By default set the rendering flag to true. */
	def begin() { rendering = true }
	
	/** By default set the rendering flag to false. */
	def end() { rendering = false }

	/** If true the avatar needs a spatial indexation. */
	def isIndexed:Boolean = false

	/**  The avatar index, if present (see isIndexed()) the avatar needs spatial indexing. */
	def index():AvatarIndex = { throw SpatialHashException("this avatar does not support indexation") }

	// == Utility ==============================================================

	protected def getNumberSeq3(values:AnyRef*):NumberSeq3 = getNumberSeq3(0, values:_*)

	protected def getNumberSeq3(i:Int, values:AnyRef*):NumberSeq3 = {
		if(i < values.length && values(i).isInstanceOf[Vector3]) {
			values(i).asInstanceOf[Vector3]
		} else {
			throw InvalidValuesException("awaiting number seq 3")
		}
	}
}

/** If defined by an avatar, allows to quickly find it using a spatial index. */
class AvatarIndex(val avatar:Avatar) extends SpatialCube {
	/** Avatar lower point. */
	val from:Point3 = Point3(-0.5, -0.5, -0.5)

	/** Avatar upper point. */
	val to:Point3 = Point3(0.5, 0.5, 0.5)

	/** The screen indexing us. */
	var spash:Screen#SpatialIndex = null

	/** The avatar changed position. */
	def changedPosition() { updateFromTo }

	/** The avatar changed size. */
	def changedSize() { updateFromTo }

	/** True if the bounding box contains the given point. */	
	def contains(p:Point3):Boolean = contains(p.x, p.y, p.z)

	/** True if the bounding box contains the given point. */
	def contains(x:Double, y:Double, z:Double):Boolean = {
		  (x >= from.x && x <= to.x
		&& y >= from.y && y <= to.y
		&& z >= from.z && z <= to.z)
	}

	/** Update the `from` and `to` fields from the `pos` and `size` fields of the avatar. */
	protected def updateFromTo() {
		val pos = avatar.pos
		val siz = avatar.size

		from.set(pos.x - siz.x/2, pos.y - siz.y/2, pos.z - siz.z/2)
		to.set(  pos.x + siz.x/2, pos.y + siz.y/2, pos.z + siz.z/2)
Console.err.println("(%s) %s -> %s".format(avatar.name, from, to))
		if(spash ne null) {
			spash.move(this)
		}
	}

	override def toString():String = "idx(%s)".format(avatar.name)
}