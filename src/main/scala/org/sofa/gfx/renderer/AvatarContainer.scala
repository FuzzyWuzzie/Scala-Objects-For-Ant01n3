package org.sofa.gfx.renderer

import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet}
import akka.actor.{ActorRef}

import org.sofa.math.{Vector3, Point3, NumberSeq3, Rgba}
import org.sofa.collection.{SpatialCube, SpatialHash, SpatialHashException}


/** Something that can contain avatars and helpers to render and animate them. */
trait AvatarContainer extends Iterable[Avatar] {

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

	/** Apply the given `code` to each sub-avatar in the render filter. If there is
	  * no render filter this is applied to each sub-avatar. */
	def foreachFilteredSub(code:(Avatar)=>Unit)

	/** Apply code to each avatar, to find the first one that returns true. */
	def findSub(code:(Avatar)=>Boolean):Option[Avatar]

	/** Find an avatar in this one or in its hierarchy of sub-avatars. */
	def avatar(name:AvatarName, prefix:Int = -1):Option[Avatar]

	/** Filter which direct sub-avatars are rendered and in which order.
	  * Once added, the filter is requested to run at next frame. Automatic
	  * filter requests will be issued when a new sub-avatar is added or one
	  * is removed. The filter is free to generate more requests automatically
	  * (it could run at each frame) or for specific events. The usual behavior
	  * is to run only when first set, or when an avatar is added or removed.
	  * You can request a filtering using `renderFilterRequest()`.
	  * Pass a null filter to remove it.
	  */
	def renderFilter(filter:RenderFilter) 

	/** Request the render filter (if any) be called at next frame.
	  * This allows to re-run the render filter. By default the filter is run only
	  * at start. Automatic filter requests are issued when a new avatar is added or when
	  * one is removed. */
	def renderFilterRequest()

	/** Access to all sub-avatars. */
	def iterator:Iterator[Avatar]

	/** Access to avatars in the render filter. If there is no filter render,
	  * this provides access to all sub-avatars. */
	def filteredIterator:Iterator[Avatar]
}


/** An avatar container based on a hash map.
  *
  * Avantage: fast modification.
  * Disadvantage: slow browsing, no order, no indexed access.
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
				if(self ne null) // for screen
					self.space.subCountChanged(1)
				avatar
			} else {
				subs.get(path(prefix-1)) match {
					case Some(avatar) => avatar.addSub(path, avatarType, prefix + 1)
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
					if(self ne null)	// for screen
						self.space.subCountChanged(-1)
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

	def renderSubs() { if(subs ne null) subs.foreach { _._2.render } }

	def animateSubs() { if(subs ne null) subs.foreach { _._2.animate } }

	def foreachSub(code:(Avatar)=>Unit) { if(subs ne null) subs.foreach { item => code(item._2) } }

	def foreachFilteredSub(code:(Avatar)=>Unit) { throw new RuntimeException("TODO foreachFilteredSub()") }

	def findSub(code:(Avatar)=>Boolean):Option[Avatar] = { if(subs ne null) {
			subs.find { a => code(a._2) } match {
				case Some(i) => Some(i._2)
				case None => None
			}
		} else {
			None
		}
	}

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

	def iterator:Iterator[Avatar] = subs.valuesIterator

	def filteredIterator:Iterator[Avatar] = throw new RuntimeException("TODO filteredIterator()")

	def renderFilter(filter:RenderFilter) { throw new RuntimeException("TODO renderFilter()") }

	def renderFilterRequest() { throw new RuntimeException("TODO renderFilterRequest()") }
}


/** An avatar container based on an array.
  *
  * Advantage: allow very fast browsing of the set of avatars, indexed access, ordering.
  * Disadvantage: updating maybe a little slower.
  */
trait AvatarContainerArray extends AvatarContainer {
	val screen:Screen

	/** All sub-avatars, null as long as there are no sub avatars. */
	protected[this] var subs:ArrayBuffer[Avatar] = null

	/** The render filter or null if node. */
	protected[this] var filter:RenderFilter = null

	/** The set of avatars to render if filtered, else null. If the filter is empty,
	  * this is not null. */
	protected[this] var renderedSubs:IndexedSeq[Avatar] = null

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
				if(self ne null) // for screen
					self.space.subCountChanged(1)
				if(filter ne null)
					filter.requestFiltering
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
					if(self ne null) // for screen
						self.space.subCountChanged(-1)
					// if(renderedSubs ne null) {
					// 	val i = renderedSubs.indexWhere(_.name == path)
					// 	if(i >= 0) renderedSubs.remove(i)
					// }
					if(filter ne null)
						filter.requestFiltering
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
			if((filter ne null) && filter.isDirty)
				renderedSubs = filter.filter(iterator)

			val s = if(renderedSubs ne null) renderedSubs else subs
			var i = 0
			val n = s.size
			while(i < n) { s(i).render(); i += 1 }	// for performance reasons.
		}
	}

	def renderVisibleSubs():Int = {
		var visible = 0
		
		if(subs ne null) {
			if((filter ne null) && filter.isDirty)
				renderedSubs = filter.filter(iterator)

			val space = self.space
			val s = if(renderedSubs ne null) renderedSubs else subs
			var i = 0
			val n = s.size

			while(i < n) {
				if(space.isVisible(s(i))) {
					s(i).render
					visible += 1
				}
				i += 1
			}
		}

		visible
	}

	def animateSubs() {
		if(subs ne null) {
			var i = 0
			val n = subs.size
			while(i < n) { subs(i).animate(); i += 1 }	// for performance reasons.
		}
	}

	def animateVisibleSubs():Int = {
		var visible = 0
		
		if(subs ne null) {
			val space = self.space
			var i = 0
			val n = subs.size

			while(i < n) {
				if(space.isVisible(subs(i))) {
					subs(i).animate
					visible += 1
				}
				i += 1
			}
		}

		visible		
	}

	def foreachSub(code:(Avatar)=>Unit) { 
		if(subs ne null) {
			var i = 0
			val n = subs.size
			while(i < n) { code(subs(i)); i += 1 }	// for performance reasons.
		}
	}

	def foreachFilteredSub(code:(Avatar)=>Unit) {
		if(renderedSubs ne null) {
			var i = 0
			val n = renderedSubs.size
			while(i < n) { code(renderedSubs(i)); i += 1 }
		} else {
			foreachSub(code)
		}
	}

	def findSub(code:(Avatar)=>Boolean):Option[Avatar] = {
		if(subs ne null) {
			subs.find(code)
		} else {
			None
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

	def iterator:Iterator[Avatar] = subs.iterator

	def filteredIterator:Iterator[Avatar] = if(renderedSubs ne null) renderedSubs.iterator else iterator

	def renderFilter(filter:RenderFilter) {
		this.filter = filter
		filter.requestFiltering
	}

	def renderFilterRequest() {
		if(filter ne null) filter.requestFiltering
	}
}


/** Filter to apply to avatars when an [[AvatarContainer]] renders
  * them. The filter selects which avatars will be rendered and in
  * which order. */
trait RenderFilter {
	/** True if the `filter()` method needs to be called. This method
	  * will reset this flag to false. */
	def isDirty:Boolean

	/** Set the `isDirty` flag to false. */
	def requestFiltering()

	/** Take a set of `avatars` an return a filtered sequence
	  * in a specific order. This set the `isDirty` flag to false.
	  * If the filter selects nothing, an empty sequence is returned,
	  * it is never null. */
	def filter(avatars:Iterator[Avatar]):IndexedSeq[Avatar]
}


/** Base implementation of a render filter, only deals with the dirty flag. */
abstract class RenderFilterBase extends RenderFilter {
	protected[this] var dirty:Boolean = true

	def isDirty = dirty
	
	def requestFiltering() { dirty = true }
}


/** A render filter that selects only a set of avatar based on an ordered set of names.
  * The order is given by the sequence of names. The names are the suffixes of the avatar
  * name for each sub-avatar. */
class RenderFilterByName(
	val suffixes:Array[String])
		extends RenderFilterBase {

	def filter(avatars:Iterator[Avatar]):IndexedSeq[Avatar] = {
		val map = new HashMap[String, Avatar] 
		val res = new ArrayBuffer[Avatar]
		var i   = 0
		val n   = suffixes.length

		while(avatars.hasNext) {
			val avatar = avatars.next
			map += (avatar.name.suffix -> avatar)
		}

		while(i < n) {
			map.get(suffixes(i)) match {
				case Some(avatar) => { res += avatar }
				case None => {}
			}
			i += 1
		}

		dirty = false

		res
	}
}


/** A render filter that allows to select a set of sub-avatars using a `predicate`
  * and can `order` this set of selected avatars using an ordering. If `predicate`
  * is null, the ordering is applied to all sub-avatars. If `order` is null, the
  * `predicate` is applied and the order remains the original order of the parent
  * avatar (by default it is insertion order, but avatar implementations can change
  * this). */
class RenderFilterByPredicate(
	val predicate:(Avatar)=>Boolean,
	val order:(Avatar, Avatar)=>Boolean)
		extends RenderFilterBase {

	def filter(avatars:Iterator[Avatar]):IndexedSeq[Avatar] = {
		val res = new ArrayBuffer[Avatar]

		if(predicate ne null) {
			while(avatars.hasNext) {
				val avatar = avatars.next
				if(predicate(avatar))
					res += avatar
			}
		} else {
			res ++= avatars
		}

		if(order ne null) {
			res.sortWith(order)
		}

		dirty = false

		res
	}
}