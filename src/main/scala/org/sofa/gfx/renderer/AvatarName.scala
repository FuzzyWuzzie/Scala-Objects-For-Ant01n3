package org.sofa.gfx.renderer


/** When an avatar name is already used. */
case class AvatarNameException(msg:String) extends Exception(msg)


/** The fully qualified name of an avatar in the avatar hierarchy.
  *
  * Avatars can be nested. The fully qualified name of an avatar is made of its name
  * prefixed by the name of its parent avatars up in the hierachy as a path, all
  * separated by dots.
  *
  * This class allows to represent such a fully qualified name and go from the 
  * string representation to instances of this class and the reverse.
  *
  * An avatar name is considered invariant.
  *
  * @constructor Build an avatar name from an array whose reference is kept as content, the array is considered immutable.
  * @param path The fully qualified name of the avatar as an array of strings.
  */
class AvatarName(protected val path:Array[String]) {

	require(path ne null)
	require(path.length > 0)

	/** As it is used in equals and hashCode, an as it is immutable, store it lazyly. */
	private[this] lazy val stringRep:String = path.mkString(".")

	/** Build the fully qualified name of the avatar using a string of names 
	  * separated by dots. */
	def this(pathStr:String) { this(pathStr.split("\\.")) }

	/** Build the fully qualified name of the avatar using a set of strings. */
	def this(names:String*) { this(names.toArray) }

	/** Number of components of the avatar path. */
	def length = path.length

	/** Number of components of the avatar path. */
	def size = path.length

	/** Number of components of the avatar path, therefore depth in the hierarchy. */
	def depth =  path.length

	/** I-th component of the avatar path. */
	def apply(i:Int):String = path(i)

	/** Sub-avatar path minus the first name. */
	def prefix:AvatarName = new AvatarName(path.dropRight(1))

	/** Same as suffix. */
	def name:String = suffix

	/** Identifier of the avatar without its parent avatars path. */
	def suffix:String = path(path.length-1)

	override def equals (other:Any):Boolean = {
		other match {
			case o:AvatarName => {
				if(o.length == length) {
					var i  = 0
					val n  = length
					var ok = true

					while(ok && i < n) {
						ok = path(i) == o.path(i)
						i += 1
					}

					ok
				} else {
					false
				} 
			}
			case _ => false
		}
	}
	
    override def hashCode:Int = stringRep.hashCode

	/** True if this and the other `name` share the same prefix up to `length`.
	  * `length` should be > 0. */
	def equalPrefix(length:Int, name:AvatarName):Boolean = {
		require(length > 0)

		if(length <= name.length && length <= path.length) {
			var i  = 0
			var ok = true
		
			while(ok && i < length) {
				ok = (path(i) == name.path(i))
				i += 1
			}

			ok
		} else {
			false
		}
	}

	override def toString:String = stringRep
}


/** AvatarName companion object. */
object AvatarName {

	def apply(path:Array[String]):AvatarName = new AvatarName(path)

	def apply(names:String*):AvatarName = new AvatarName(names:_*)

	def apply(pathStr:String):AvatarName = { new AvatarName(pathStr)}

	def unapply(name:AvatarName):String = name.toString

//	def unapply(name:AvatarName):Array[String]
}