package org.sofa.math


/** Represent a bounding box inside a 3D space.
  *
  * This is a pure interface, it does not tell how the data is stored.
  * The position can be seen as the origin of the object. The from and to
  * points define a bounding box. Note that the position can be outside of
  * this box. the size is always equal to the distance along x, y and z between
  * the from and to points.
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

	/** Change the position. */
	def setPosition(x:Double, y:Double, z:Double)

	/** Change the bounding box. It does not change the position (that can be outside of the box). */
	def setBox(fromx:Double, fromy:Double, fromz:Double, tox:Double, toy:Double, toz:Double)

	/** Set the size, the way it changes the bounding box is not specified (how from and to point move). 
	  * The position is not changed. */
	def setSize(width:Double, height:Double, depth:Double)
}


/** A default implementation of [[Box3]] that stores all fields. 
  * It maintains the size according to the `from` and `to` fields.
  */
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
}


/** A basic implementation of [[Box3]] that resizes according to its `from` position.
  * It does not store a position, and uses its `from` field that coincide with it.
  * It does not stored a size that is deduced from the `from` and `to` fields. */
class Box3From extends Box3 {
	val from = Point3(0,0,0)

	val to = Point3(1,1,1)

	def pos = from

	def size = Vector3(to.x-from.x, to.y-from.y, to.z-from.z)

	def setPosition(x:Double, y:Double, z:Double) = from.set(x, y, z)

	def setBox(fromx:Double, fromy:Double, fromz:Double, tox:Double, toy:Double, toz:Double) {
		from.set(fromx, fromy, fromz)
		to.set(tox, toy, toz)
	}

	def setSize(width:Double, height:Double, depth:Double) = to.set(from.x + width, from.y + height, from.z + depth)
}


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
}
