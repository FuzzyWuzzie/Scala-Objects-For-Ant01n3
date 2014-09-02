package org.sofa.math


/** Represent a bounding box inside a 3D space.
  *
  * This is a pure interface, it does not tell how the data is stored.
  * The position can be seen as the origin of the object. The from and to
  * points define a bounding box. Note that the position can be outside of
  * this box. the size is always equal to the distance along x, y and z between
  * the from and to points.
  *
  * The `sizex`, etc., `posx` etc., `fromx` etc. and `tox', etc. are here for
  * efficiency reasons. As some fields do not exist in some implementation, they
  * incur repetitive creation of [[Point3]] or [[Vector3]] which in some rendering
  * loops may cost a lot.
  *
  * Be also careful that the values returned by `pos`, `from`, `to` and `size` are
  * read-only, you cannot expect change made to the returned point or vector to be
  * kept by the box. Use `setSize()`, `setBox()` and `setPosition()` to update the
  * box dimensions and position.
  */
trait Box3 {
	/** Position or origin of object. */
	def pos:Point3

	/** Lower left-bottom-front point. */
	def from:Point3

	/** Higher right-top-back point. */
	def to:Point3

	/** Overral dimensions (according to `from` and `to`) */
	def size:Vector3

	def sizex:Double

	def sizey:Double

	def sizez:Double

	def posx:Double

	def posy:Double

	def posz:Double

	def fromx:Double

	def fromy:Double

	def fromz:Double

	def tox:Double

	def toy:Double

	def toz:Double	

	/** Change the position. */
	def setPosition(x:Double, y:Double, z:Double)

	/** Change the bounding box. It does not change the position (that can be outside of the box). */
	def setBox(fromx:Double, fromy:Double, fromz:Double, tox:Double, toy:Double, toz:Double)

	/** Set the size, the way it changes the bounding box is not specified (how from and to point move). 
	  * The position is not changed. */
	def setSize(width:Double, height:Double, depth:Double)
}


/** A default implementation of [[Box3]] that stores all fields. 
  * It maintains the size according to the `from` and `to` fields. */
class Box3Default extends Box3 {
	val pos = Point3(0, 0, 0)

	val from = Point3(-1, -1, -1)

	val to = Point3(1, 1, 1)

	val size = Vector3(2, 2, 2)

	def setPosition(x:Double, y:Double, z:Double) = pos.set(x, y, z)

	def setBox(fromx:Double, fromy:Double, fromz:Double, tox:Double, toy:Double, toz:Double) {
		from.set(fromx, fromy, fromz)
		to.set(tox, toy, toz)
		size.set(tox-fromx, toy-fromy, toz-fromz)
	}

	def setSize(width:Double, height:Double, depth:Double) {
		size.set(width, height, depth)
		from.set(pos.x - width/2, pos.y + height/2, pos.z + depth/2)
		to.set(pos.x + width/2, to.y + height/2, to.z + depth/2)
	}

	def sizex:Double = size.x

	def sizey:Double = size.y

	def sizez:Double = size.z

	def posx:Double = pos.x

	def posy:Double = pos.y

	def posz:Double = pos.z

	def fromx:Double = from.x

	def fromy:Double = from.y

	def fromz:Double = from.z

	def tox:Double = to.x

	def toy:Double = to.y

	def toz:Double = to.z
}


/** A basic implementation of [[Box3]] that resizes according to its `from` position.
  * It does not store a position, and uses its `from` field that coincide with it.
  * It does not stored a `to` point that is deduced from the `size` and `from` fields.
  *
  * Ideal for things whose origin is at a corner. */
class Box3From extends Box3 {
	val from = Point3(0,0,0)

	val size = Vector3(1,1,1)

	def pos = from

	def to = Point3(from.x+size.x, from.y+size.y, from.z+size.z)

	def setPosition(x:Double, y:Double, z:Double) = from.set(x, y, z)

	def setBox(fromx:Double, fromy:Double, fromz:Double, tox:Double, toy:Double, toz:Double) {
		from.set(fromx, fromy, fromz)
		to.set(tox-fromx, toy-fromy, toz-fromz)
	}

	def setSize(width:Double, height:Double, depth:Double) = size.set(width, height, depth)

	def sizex:Double = size.x

	def sizey:Double = size.y

	def sizez:Double = size.z

	def posx:Double = from.x

	def posy:Double = from.y

	def posz:Double = from.z

	def fromx:Double = from.x

	def fromy:Double = from.y

	def fromz:Double = from.z

	def tox:Double = from.x + size.x

	def toy:Double = from.y + size.y

	def toz:Double = from.z + size.z
}


// TODO Box3PosCentered should be named Box3Sized and the reverse !!!


/** A basic implementation of [[Box3]] that resizes according to its `pos` field.
  * This implementation defines three fields, `from`, `to`, and `pos` fields. The
  * position can be outside of the bounding box. Size is deduced from the `from`
  * and `to` fields. It resizes around the position point locating it in the center. */
class Box3PosCentered extends Box3 {
	val pos = Point3(0,0,0)

	val from = Point3(-1,-1,-1)

	val to = Point3(1,1,1)

	def size = Vector3(to.x-from.x, to.y-from.y, to.z-from.z)

	def setPosition(x:Double, y:Double, z:Double) = pos.set(x, y, z)

	def setBox(fromx:Double, fromy:Double, fromz:Double, tox:Double, toy:Double, toz:Double) {
		from.set(fromx, fromy, fromz)
		to.set(tox, toy, toz)
	}

	def setSize(width:Double, height:Double, depth:Double) {
		from.set(pos.x - width/2, pos.y + height/2, pos.z + depth/2)
		to.set(pos.x + width/2, to.y + height/2, to.z + depth/2)
	}	

	def sizex:Double = to.x-from.x

	def sizey:Double = to.y-from.y

	def sizez:Double = to.z-from.z
	
	def posx:Double = pos.x

	def posy:Double = pos.y

	def posz:Double = pos.z

	def fromx:Double = from.x

	def fromy:Double = from.y

	def fromz:Double = from.z

	def tox:Double = to.x

	def toy:Double = to.y

	def toz:Double = to.z
}


/** A basic implementation of [[Box3]] that computes its `from` and `to`
  * points from its position and size. The position is at the center of the box. */
class Box3Sized extends Box3 {
	val pos = Point3(0, 0, 0)

	val size = Vector3(1, 1, 1)

	def from = Point3(pos.x-size(0)/2, pos.y-size(1)/2, pos.z-size(2)/2)
	
	def to = Point3(pos.x+size(0)/2, pos.y+size(1)/2, pos.z+size(2)/2)

	def setPosition(x:Double, y:Double, z:Double) = pos.set(x, y, z)

	def setBox(fromx:Double, fromy:Double, fromz:Double, tox:Double, toy:Double, toz:Double) {
		size.set(tox-fromx, toy-fromy, toz-fromz)
		pos.set(fromx+size(0)/2, from.y+size(1)/2, from.z+size(2)/2)
	}

	def setSize(width:Double, height:Double, depth:Double) {
		size.set(width, height, depth)
	}	

	def sizex:Double = size.x

	def sizey:Double = size.y

	def sizez:Double = size.z

	def posx:Double = pos.x

	def posy:Double = pos.y

	def posz:Double = pos.z

	def fromx:Double = pos.x-size(0)/2

	def fromy:Double = pos.y-size(1)/2

	def fromz:Double = pos.z-size(2)/2

	def tox:Double = pos.x+size(0)/2

	def toy:Double = pos.y+size(1)/2

	def toz:Double = pos.z+size(2)/2
}