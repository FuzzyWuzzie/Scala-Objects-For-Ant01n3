package org.sofa.opengl.actor.renderer

import scala.collection.mutable.{ArrayBuffer, HashMap}
import akka.actor.{ActorRef}

import org.sofa.math.{Vector3, Point3, NumberSeq3, Rgba}
import org.sofa.collection.{SpatialCube, SpatialHash, SpatialHashException}


/** Something that can contain avatars and helpers to render and animate them. */
trait AvatarContainer {

	/** Number of sub-avatars. */
	def subCount:Int

	/** True if there is at leas one sub-avatar. */
	def hasSubs:Boolean

	/** Add a sub-avatar in this avatar or its sub-avatars (depending on `name`). The avatar
	  * is created using `type`. */
	def addSub(path:AvatarName, avatarType:String, prefix:Int = -1):Avatar

	/** Remove a sub-avatar either in this avatar or its sub-avatars (depending on `name`). */
	def removeSub(path:AvatarName, prefix:Int = -1):Avatar

	/** Render the sub-avatars. */
	def renderSubs()

	/** Animate the sub-avatars. */
	def animateSubs()

	/** Apply the given code to each sub avatar. */
	def foreachSub(code:(Avatar)=>Unit)

	/** Find an avatar in this one or in its hierarchy of sub-avatars. */
	def avatar(name:AvatarName, prefix:Int = -1):Option[Avatar]
}


/** An avatar container based on a hash map.
  *
  * Avantage: fast modification.
  * Disadvantage: slow browsing.
  */
trait AvatarContainerHashMap extends AvatarContainer {
	val screen:Screen

	// All sub-avatars, null as long as there are no sub avatars. */
	protected var subs:HashMap[String,Avatar] = null

	protected def self:Avatar

	def subCount = if(subs ne null) subs.size else 0

	def hasSubs = ((subs ne null) && (subs.size > 0))

	def addSub(path:AvatarName, avatarType:String, prefix:Int = -1):Avatar = {
		if(prefix < 0) {
			addSub(path, avatarType, 1)	// Not the index in the path, but length of the prefix.
		} else {
			if(subs == null) subs = new HashMap[String,Avatar]()

			if(prefix >= path.size) {
				if(subs.contains(path.suffix)) throw new AvatarNameException("avatar %s already contain a sub named %s".format(self.name, path))
				val avatar = screen.createAvatar(path, avatarType)
				avatar.reparent(self)
				subs += (path.suffix -> avatar)
				avatar
			} else {
				subs.get(path(prefix-1)) match {
					case Some(avatar) =>  avatar.addSub(path, avatarType, prefix + 1)
					case None         => throw new NoSuchAvatarException("%s (%s)".format(path.toString, path(prefix)))
				}
			}
		}	
	}

	def removeSub(path:AvatarName, prefix:Int = -1):Avatar = {
		if(prefix < 0) {
			removeSub(path, 1)
		} else {
			if(subs ne null) {
				if(prefix == path.size) {
					val a = subs.remove(path(prefix-1)).getOrElse(throw new NoSuchAvatarException("%s".format(path.toString)))
					if(subs.isEmpty) subs = null
					a
				} else {
					subs.get(path(prefix-1)) match {
						case Some(avatar) => avatar.removeSub(path, prefix + 1)
						case None => throw new NoSuchAvatarException("%s (%s)".format(path.toString, path(prefix-1)))
					}
				}
			} else {
				throw new NoSuchAvatarException("%s".format(path.toString))
			}
		} 
	}

	def renderSubs() { subs.foreach { _._2.render } }

	def animateSubs() { subs.foreach { _._2.animate } }

	def foreachSub(code:(Avatar)=>Unit) { subs.foreach { item => code(item._2) } }

	def avatar(name:AvatarName, prefix:Int = -1):Option[Avatar] = {
		if(prefix < 0) {
			avatar(name, 1)
		} else {
			if(subs ne null) {
				if(prefix == name.size) {
					subs.get(name(prefix-1))
				} else {
					subs.get(name(prefix-1)) match {
						case Some(avatar) => avatar.avatar(name, prefix + 1)
						case None         => None
					}
				}
			} else {
				throw new NoSuchAvatarException("%s".format(name.toString))
			}
		}
	}
}


/** An avatar container based on an array.
  *
  * Advantage: allow very fast browsing of the set of avatars.
  * Disadvantage: updating maybe a little slower.
  */
trait AvatarContainerArray extends AvatarContainer {
	val screen:Screen

	/** All sub-avatars, null as long as there are no sub avatars. */
	protected var subs:ArrayBuffer[Avatar] = null

	protected def self:Avatar

	def subCount = if(subs ne null) subs.size else 0

	def hasSubs = ((subs ne null) && (subs.size > 0))

	/** Add an avatar in this array or in a sub-avatar, depending on the path. */
	def addSub(path:AvatarName, avatarType:String, prefix:Int = -1):Avatar = {
		// We do not use avatar() to find a possibly deep avatar, in order to let
		// each avatar define its own container implementation.

		if(prefix < 0) {
			addSub(path, avatarType, 1)	// prefix is not the index but the length of the prefix actually used
		} else {
			if(subs == null) subs = new ArrayBuffer[Avatar]()
		
			if(prefix == path.size) {
				if(subs.indexWhere(_.name == path) >= 0) throw new AvatarNameException("avatar %s already contain a sub named %s".format(self.name, path))
				val avatar = screen.createAvatar(path, avatarType)
				avatar.reparent(self)
				subs += avatar
				avatar
			} else {
				subs.find(_.name.equalPrefix(prefix, path)) match {
					case Some(avatar) => avatar.addSub(path, avatarType, prefix + 1)
					case None         => throw new NoSuchAvatarException("%s (%s)".format(path.toString, path(prefix-1)))
				}
			}
		}
	}

	def removeSub(path:AvatarName, prefix:Int = -1):Avatar = {
		// We do not use avatar() to find a possibly deep avatar, in order to let
		// each avatar define its own container implementation.

		if(prefix < 0) {
			removeSub(path, 1)
		} else {
			if(subs ne null) {
				if(prefix == path.size) {
					// Best way I found to be notified if the element does not exist.
					val i = subs.indexWhere(_.name == path)
					val a = if(i >= 0) subs.remove(i)
					        else throw new NoSuchAvatarException("%s".format(path.toString))
					if(subs.isEmpty) subs = null
					a
				} else {
					subs.find(_.name.equalPrefix(prefix, path)) match {
						case Some(avatar) => avatar.removeSub(path, prefix + 1)
						case None         => throw new NoSuchAvatarException("%s (%s)".format(path.toString, path(prefix-1)))
					} 
				}
			} else {
				throw new NoSuchAvatarException("%s".format(path.toString))
			}
		}
	}

	def renderSubs() {
		if(subs ne null) {
			var i = 0
			val n = subs.size
			while(i < n) { subs(i).render(); i += 1 }	// for performance reasons.
		}
	}

	def animateSubs() {
		if(subs ne null) {
			var i = 0
			val n = subs.size
			while(i < n) { subs(i).animate(); i += 1 }	// for performance reasons.
		}
	}

	def foreachSub(code:(Avatar)=>Unit) { 
		if(subs ne null) {
			var i = 0
			val n = subs.size
			while(i < n) { code(subs(i)); i += 1 }	// for performance reasons.
		}
	}

	def avatar(name:AvatarName, prefix:Int = -1):Option[Avatar] = {
		if(prefix < 0) {
			avatar(name, 1) // Not the index of the avatar, but length of prefix.
		} else {
			if(prefix == name.length) {
				subs.find(_.name == name)
			} else {
				subs.find(_.name.equalPrefix(prefix, name)) match {
					case Some(avatar) => avatar.avatar(name, prefix + 1)
					case None         => None
				}
			}
		}
	}

	def hierarchyToString(depth:Int=0):String = {
		val buf = new StringBuilder()

		buf ++= "  " * depth
		buf ++= self.name.suffix
		buf ++= "%n".format()

		foreachSub { buf ++= _.hierarchyToString(depth + 1) }

		buf.toString
	}
}