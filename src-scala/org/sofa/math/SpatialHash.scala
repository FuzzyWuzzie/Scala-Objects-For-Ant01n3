package org.sofa.math

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Set
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

/** An integer position in "bucket" space that can be easily hashed with
  * a specific hash function. */
class HashPoint3(val x:Int, val y:Int, val z:Int) {	
	override def hashCode():Int = {
		 (x*73856093)^(y*19349663)^(z*83492791)	
	}
	
	override def equals(other:Any):Boolean = {
		var result = false
		if(other.isInstanceOf[HashPoint3]) {
			val o = other.asInstanceOf[HashPoint3]
			result = o.x == x && o.y == y && o.z == z
		}
		result
	}
	
	override def toString():String = "[%d, %d, %d]".format(x, y, z)
}

/** HashPoint3 companion object. */
object HashPoint3 {
	def apply(x:Int, y:Int, z:Int):HashPoint3 = new HashPoint3(x,y,z)
}

/** An object that can be handled by the spatial hash. */
trait SpatialObject { 
	/** Set of areas the object appears in. */
	protected var buckets:HashSet[Bucket] = null
	
	/** True if this object is a volume, rather than a single point. */
	def isVolume:Boolean
	
	/** The position of the left-bottom-front point of the bounding box of the object. */
	def from:Point3
	
	/** The position of the right-top-back point of the bounding box of the object. */
	def to:Point3

	/** The object appears in the given spatial area. */
	def addBucket(bucket:Bucket) {
		if(buckets eq null)
			buckets = new HashSet[Bucket]()
		
		buckets += bucket 
	}
	
	/** The object is removed from the spatial hash.
	  * This returns a list of buckets were the object
	  * was previously. */
	def removeBuckets():Set[Bucket] = {
		assert((buckets ne null) && (!buckets.isEmpty))
		val b = buckets
		buckets = null
		b
	}
}

/** A spatial object that is infinitely small. */
trait SpatialPoint extends SpatialObject {
	override def isVolume = false
}

/** A spatial object that occupies a given region of space, defined
  * by a bounding box. */
trait SpatialCube extends SpatialObject {
	override def isVolume = true
}

/** A space area that can contain spatial objects. A spatial object
  * can appear in several spatial areas if it is larger or overlaps
  * spatial areas. */
class Bucket(val position:HashPoint3) extends HashSet[SpatialObject] {}

/** A spatial indexing that place objects in cubic areas or "buckets"
  * that have a given side size.
  * 
  * The idea behind the spatial hash is to divide the 3D space into cubes
  * named buckets and forming a grid. The coordinates in the grid are called
  * "bucket space" whereas the objects the user places in the buckets lies in
  * a "user space". A bucket exists only if there are
  * some object at the positions of the "user" space it contains.
  * 
  * For example,
  * all objects between "user" coordinates (0,0,0) and (bucketSize,bucketSize,bucketSize)
  * are in the bucket whose "buckets" coordinates are (0,0,0). But all objects
  * between (bucketSize,0,0) and (bucketSize*2,bucketSize,bucketSize) "user" coordinates are
  * at "bucket" coordinates (1,0,0). In the same way, all objects in between "user" coordinates
  * (-1,-1,-1) and (0,0,0) are at "bucket" coordinates (-1,-1,-1). Etc.
  *  
  * Object added to the spatial hash must inherit the `SpatialObject`
  * trait, and be either a `SpatialPoint`, an infinitely small point,
  * or a `SpatialVolume` that is a an object that occupies a given
  * bounding box in the "user" space.
  * 
  * Each time an object is added to the spatial index, either
  * a bucket already exist for this position and the object is
  * added in it, or a bucket is created. If the object is a volume,
  * it may be added to all the buckets it intersects.
  * 
  * Each time an object is removed from the spatial index, its
  * bucket (or buckets) is cleared from its reference, and if 
  * the bucket becomes empty, the bucket is removed.
  * 
  * The idea behind the spatial hash is that buckets are identified
  * by their coordinates in a hash table (with a special hash
  * code function for the triplets of coordinates). Therefore there
  * are no limits in the space that you can index, objects can
  * be arbitrarily distant from one another, without consuming much
  * space. */
class SpatialHash[T<:SpatialObject](val bucketSize:Double) {

	/** Set of buckets. */
	val buckets = new HashMap[HashPoint3,Bucket]()

	/** Transform "user" space coordinates into "bucket" space coordinates. */
	def hash(p:Point3):HashPoint3 = hash(p.x, p.y, p.z)
		
	/** Transform "user" space coordinates into "bucket" space coordinates. */
	def hash(x:Double, y:Double, z:Double):HashPoint3 = {
		var xx = (x / bucketSize)
		var yy = (y / bucketSize)
		var zz = (z / bucketSize)
		
		if(xx < 0) xx -= 1
		if(yy < 0) yy -= 1
		if(zz < 0) zz -= 1
		
		new HashPoint3(xx.toInt, yy.toInt, zz.toInt)
	}
	
	/** Add a new object in the spatial index. If the object is point,
	  * it will occupy only one bucket (creating it if needed). If the object
	  * is a volume, it will occupy all the buckets it intersects. */
	def add(thing:T) {
		if(thing.isVolume) {
			val p0 = hash(thing.from)
			val p1 = hash(thing.to)
			
			for(z <- p0.z to p1.z) {
				for(y <- p0.y to p1.y) {
					for(x <- p0.x to p0.x) {
						val p = HashPoint3(x,y,z) //hash(x,y,z)
						var b = buckets.get(p).getOrElse(new Bucket(p))
						
						b += thing
						thing.addBucket(b)
						buckets += ((p, b))
					}
				}
			}
		} else {
			val p = hash(thing.from)
			var b = buckets.get(p).getOrElse(new Bucket(p))
		
			b += thing		
			thing.addBucket(b)
			buckets += ((p,b))
		}
	}
	
	/** Remove an object from the index. If the object is the last
	  * one in its bucket(s), the bucket(s) is(are) removed. */
	def remove(thing:T) {
		// We cannot hash the thing to know its bucket, since in
		// between the thing may have moved. Therefore we use the
		// set of bucket stored in the thing that tells inside
		// which bucket the thing appears.

		thing.removeBuckets.foreach { bucket =>
			bucket.remove(thing)
			if(bucket.isEmpty) {
				buckets.remove(bucket.position)
			}
		}
	}
	
	/** Remove and re-add the given thing, therefore updating the
	  * buckets it pertains to. This is fast for non-volume
	  * things, and may slow down for volume things that may
	  * span across several buckets. You can avoid using it if
	  * you known the thing does not moved. */
	def move(thing:T) {
		remove(thing)
		add(thing)
	}
	
	/** All the neighbor objects in all the buckets intersecting
	  * the cube area of `size` side, around the point (`x`,`y`,`z`).
	  * The returned objects may therefore be out of the cube defined
	  * by the parameters. */
	def neighborsInBox(size:Double, x:Double, y:Double, z:Double):Set[T] = {
		val s2     = size/2
		val bucks  = getBuckets(x-s2, y-s2, z-s2, x+s2, y+s2, z+s2)
		val result = new HashSet[T]
		
//		bucks.foreach { bucket =>
//			result ++= bucket
//		}
		
		result
	}
	
	/** Get the bucket which contains "user" coordinates (`x`,`y`,`z`).
	  * Buckets have their own coordinates and have indices at
	  * integer positions in a 3D space, where each integer
	  * position is a multiple of `cellSize`. */
	def getBucket(x:Double, y:Double, z:Double):Bucket = buckets.get(hash(x,y,z)).getOrElse(null)

	/** All the buckets containing or intersecting the bounding box defined by point
	  * (`x0`,`y0`,`z0`)  and point (`x1`,`y1`,`z1`). The first point must be
	  * the lower-left-front point and the second point must be the top-right-back
	  * point (in other words, the coordinates of the second point must be
	  * greater than the coordinates of the first point). */
	def getBuckets(x0:Double, y0:Double, z0:Double, x1:Double, y1:Double, z1:Double):ArrayBuffer[Bucket] = {
		val from = hash(x0, y0, z0)
		val to   = hash(x1, y1, z1)
		val res  = new ArrayBuffer[Bucket]

		for(z <- from.z to to.z) {
			for(y <- from.y to to.y) {
				for(x <- from.x to to.x) {
					val b = buckets.get(HashPoint3(x,y,z)).getOrElse(null)
					if(b ne null)
						res += b
				}
			}
		}
		
		res
	}
}