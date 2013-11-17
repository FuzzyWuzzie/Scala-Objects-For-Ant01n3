package org.sofa.collection


import scala.collection.mutable.{Set, ArrayBuffer, HashMap, HashSet}

import org.sofa.math.{Point3, Point2}


/** An integer position in "bucket" space that can be easily hashed with
  * a specific hash function. */
class HashPoint3(val x:Int, val y:Int, val z:Int) {	
	override def hashCode():Int = { (x*73856093)^(y*19349663)^(z*83492791) }
	
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
	
	/** Number of buckets occupied by this object. */
	def bucketCount:Int
	
	/** True if this object is a volume, rather than a single point. */
	def isVolume:Boolean
	
	/** The position of the left-bottom-front point of the bounding box of the object. */
	def from:Point3
	
	/** The position of the right-top-back point of the bounding box of the object. */
	def to:Point3

	/** The object appears in the given spatial area. */
	def addBucket(bucket:Bucket[SpatialObject,SpatialPoint,SpatialCube])
	
	/** The object is removed from the spatial hash.
	  * This returns a list of buckets were the object
	  * was previously. */
	def removeBuckets(bucketSet:HashMap[HashPoint3,Bucket[SpatialObject,SpatialPoint,SpatialCube]])
}


/** A spatial object that is infinitely small. */
trait SpatialPoint extends SpatialObject {
	/** The area where the spatial object as a point appears. */
	protected var bucket:Bucket[SpatialObject,SpatialPoint,SpatialCube] = null

	override def isVolume = false

	override def bucketCount:Int = 1

	def addBucket(bucket:Bucket[SpatialObject,SpatialPoint,SpatialCube]) {
		assert(this.bucket eq null)

		this.bucket = bucket		
	}
	
	def removeBuckets(bucketSet:HashMap[HashPoint3,Bucket[SpatialObject,SpatialPoint,SpatialCube]]) {
		assert(bucket ne null)
		bucket.removePoint(this)

		if(bucket.isEmpty) {
		 	bucketSet.remove(bucket.position)
		}

		bucket = null
	}
}


/** A spatial object that occupies a given region of space, defined
  * by a bounding box. */
trait SpatialCube extends SpatialObject {
	/** Set of areas the object appears in. */
	protected var buckets:HashSet[Bucket[SpatialObject,SpatialPoint,SpatialCube]] = null

	override def isVolume = true

	override def bucketCount:Int = if(buckets ne null) buckets.size else 0

	def addBucket(bucket:Bucket[SpatialObject,SpatialPoint,SpatialCube]) {
		if(buckets eq null)
			buckets = new HashSet[Bucket[SpatialObject,SpatialPoint,SpatialCube]]()
		
		buckets += bucket 
	}
	
	def removeBuckets(bucketSet:HashMap[HashPoint3,Bucket[SpatialObject,SpatialPoint,SpatialCube]]) {
		assert(buckets ne null)
		assert(!buckets.isEmpty)
		buckets.foreach { bucket =>
			bucket.removeVolume(this)
			
			if(bucket.isEmpty) {
			 	bucketSet.remove(bucket.position)
			}
		}
		buckets.clear
	}
}


/** A space area that can contain spatial objects. A spatial object
  * can appear in several spatial areas if it is larger or overlaps
  * spatial areas. */
class Bucket[T<:SpatialObject,P<:SpatialPoint,V<:SpatialCube](val position:HashPoint3) {
	var points:HashSet[P] = null

	var volumes:HashSet[V] = null

	def addPoint(point:P) {
		if(points eq null) points = new HashSet[P]()
		points += point
	}

	def addVolume(volume:V) {
		if(volumes eq null) volumes = new HashSet[V]()
		volumes += volume
	}

	def removePoint(point:P) {
		points -= point
	}

	def removeVolume(volume:V) {
		volumes -= volume
	}

	/** Number of points in the bucket. */
	def pointCount:Int = if(points ne null) points.size else 0
	
	/** Number of volume objects in the bucket. */
	def volumeCount:Int = if(volumes ne null) volumes.size else 0

	/** No element in the bucket ? */
	def isEmpty():Boolean = (((points eq null) || points.isEmpty) && ((volumes eq null) || volumes.isEmpty))
	
	override def hashCode():Int = {
		 (position.x*73856093)^(position.y*19349663)^(position.z*83492791)	
	}
	
	override def equals(other:Any):Boolean = {
		var result = false
		if(other.isInstanceOf[Bucket[T,P,V]]) {
			val o = other.asInstanceOf[Bucket[T,P,V]]
			result = o.position.x == position.x && o.position.y == position.y && o.position.z == position.z
		}
		result
	}

	override def toString():String = "Bucket(%d pts, %d vols)[%s]".format(pointCount, volumeCount, position)
}


case class SpatialHashException(msg:String) extends Exception(msg)


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
  * Bucket space coordinates are integers.
  *  
  * Object added to the spatial hash must inherit the `SpatialObject`
  * trait, and be either a `SpatialPoint`, an infinitely small point,
  * or a `SpatialVolume` that is a an object that occupies a given
  * bounding box in the "user" space (more interesting volumes, or lines,
  * or spheres can be added later).
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
  * space.
  * 
  * Specific methods allow to retrieve objects around a point or another
  * object. They work both in 3D or in 2D in the XY plane. Therefore,
  * although this class is made for 3D, it can easily be used in 2D
  * without speed loss (since, most of the time, this is the search
  * for neighbors that is the most time consuming). */
class SpatialHash[T<:SpatialObject, P<:SpatialPoint, V<:SpatialCube](val bucketSize:Double) {

	protected var points:Int = 0

	protected var volumes:Int = 0

	/** Set of buckets. */
	val buckets = new HashMap[HashPoint3,Bucket[T,P,V]]()

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

	/** Number of points and volumes inside the hash. */
	def size():Int = (points + volumes)

	/** Number of points in the hash (not counting volumes). */
	def pointCount():Int = points

	/** Number of volumes in the hash (not counting points). */
	def volumeCount():Int =  volumes

	/** See add(). */
	def += (thing:T) { add(thing) }

	/** See remove(). */
	def -= (thing:T) { remove(thing) }

	/** Add a new object in the spatial index. If the object is point,
	  * it will occupy only one bucket (creating it if needed). If the object
	  * is a volume, it will occupy all the buckets it intersects. */
	def add(thing:T) {
		if(thing.isVolume) {
			val p0 = hash(thing.from)
			val p1 = hash(thing.to)
			
			for(z <- p0.z to p1.z) {
				for(y <- p0.y to p1.y) {
					for(x <- p0.x to p1.x) {
						val p = HashPoint3(x,y,z) //hash(x,y,z)
						var b = buckets.get(p).getOrElse({new Bucket(p)})
						
						b.addVolume(thing.asInstanceOf[V])
						thing.addBucket(b.asInstanceOf[Bucket[SpatialObject,SpatialPoint,SpatialCube]])
						buckets += ((p, b))
					}
				}
			}

			volumes += 1
		} else {
			val p = hash(thing.from)
			var b = buckets.get(p).getOrElse(new Bucket(p))
		
			b.addPoint(thing.asInstanceOf[P])
			thing.addBucket(b.asInstanceOf[Bucket[SpatialObject,SpatialPoint,SpatialCube]])
			buckets += ((p,b))

			points += 1
		}
	}
	
	/** Remove an object from the index. If the object is the last
	  * one in its bucket(s), the bucket(s) is(are) removed. */
	def remove(thing:T) {
		// We cannot hash the thing to know its bucket, since in
		// between the thing may have moved. Therefore we use the
		// set of bucket stored in the thing that tells inside
		// which bucket the thing appears.

		val b = thing.removeBuckets(buckets.asInstanceOf[HashMap[HashPoint3,Bucket[SpatialObject,SpatialPoint,SpatialCube]]])
		// b.foreach { bucket =>
		// 	bucket.remove(thing)
		// 	if(bucket.isEmpty) {
		// 		buckets.remove(bucket.position)
		// 	}
		// }

		if(thing.isVolume) volumes -= 1
		else points -= 1
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
	  * the cube area of `size` side, around the object `thing`.
	  * The returned objects may therefore be out of the cube defined
	  * by the parameters. The neighbor set does not contain the
	  * `thing` element. */
	def neighborsInBox(thing:T, size:Double):Set[T] = {
		val s2     = size/2
		val p      = thing.from
		val result = new HashSet[T]

		getThings(result, p.x-s2, p.y-s2, p.z-s2, p.x+s2, p.y+s2, p.z+s2)		
		
		result -= thing		
		result
	}
	
	/** All the neighbor objects in all the buckets intersecting
	  * the cube area of `size` side, around the point (`p`).
	  * The returned objects may therefore be out of the cube defined
	  * by the parameters. */
	def neighborsInBox(p:Point3, size:Double):Set[T] = {
		val s2     = size/2
		val result = new HashSet[T]
		getThings(result, p.x-s2, p.y-s2, p.z-s2, p.x+s2, p.y+s2, p.z+s2)

		result
	}
	
	/** All the neighbor objects in all the buckets intersecting
	  * a 2D cube along the axes X and Y of `size` side around
	  * the object `thing`. The returned objects may therefore
	  * be out of the cube defined by the parameters. The neighbor
	  * set does not contain the `thing` element. */
	def neighborsInBoxXY(thing:T, size:Double):Set[T] = {
		val s2     = size/2
		val p      = thing.from
		val result = new HashSet[T]
	
		getThings(result, p.x-s2, p.y - s2, p.z, p.x+s2, p.y+s2, p.z)

		result -= thing
		result
	}
	
	/** All the neighbor objects in all the buckets intersecting
	  * a 2D cube along the axes X and Y of `size` side around
	  * the object `thing`. The returned objects may therefore
	  * be out of the cube defined by the parameters. */
	def neighborsInBoxXY(p:Point3, size:Double):Set[T] = {
		val s2     = size/2
		val result = new HashSet[T]
	
		getThings(result, p.x-s2, p.y - s2, p.z, p.x+s2, p.y+s2, p.z)
		
		result
	}
	
	/** All the neighbor objects in all the buckets intersecting
	  * a 2D cube along the axes X and Y of `size` side around
	  * the object `thing`. The returned objects may therefore
	  * be out of the cube defined by the parameters. */
	def neighborsInBoxXY(p:Point2, size:Double):Set[T] = {
		val s2     = size/2
		val result = new HashSet[T]
	
		getThings(result, p.x-s2, p.y - s2, 0, p.x+s2, p.y+s2, 0)
		
		result
	}

	def neighborsInBox(thing:T, size:Double, points:ArrayBuffer[P], volumes:HashSet[V]) {
		val s2 = size/2
		val p  = thing.from
		
		getThings(points, volumes, p.x-s2, p.y-s2, p.z-s2, p.x+s2, p.y+s2, p.z+s2)		
	}
	
	def neighborsInBox(p:Point3, size:Double, points:ArrayBuffer[P], volumes:HashSet[V]) {
		val s2 = size/2
	
		getThings(points, volumes, p.x-s2, p.y-s2, p.z-s2, p.x+s2, p.y+s2, p.z+s2)
	}
	
	def neighborsInBoxXY(thing:T, size:Double, points:ArrayBuffer[P], volumes:HashSet[V]) {
		val s2 = size/2
		val p  = thing.from
	
		getThings(points, volumes, p.x-s2, p.y - s2, p.z, p.x+s2, p.y+s2, p.z)
	}
	
	def neighborsInBoxXY(p:Point3, size:Double, points:ArrayBuffer[P], volumes:HashSet[V]) {
		val s2 = size/2
	
		getThings(points, volumes, p.x-s2, p.y - s2, p.z, p.x+s2, p.y+s2, p.z)
	}
	
	def neighborsInBoxXY(p:Point2, size:Double, points:ArrayBuffer[P], volumes:HashSet[V]) {
		val s2 = size/2
	
		getThings(points, volumes, p.x-s2, p.y - s2, p.x+s2, p.y+s2)
	}

	def neighborsInBoxRadius(thing:T, size:Double, radius:Double, points:ArrayBuffer[P], volumes:HashSet[V]) {
		val s2 = size/2
		val p  = thing.from

		getThingsRadius(points, volumes, thing, radius, p.x-s2, p.y-s2, p.z-s2, p.x+s2, p.y+s2, p.z+s2)
	}

	def neighborsInBoxRadiusXY(thing:T, size:Double, radius:Double, points:ArrayBuffer[P], volumes:HashSet[V]) {
		val s2 = size/2
		val p  = thing.from
	
		getThingsRadius(points, volumes, thing, radius, p.x-s2, p.y - s2, p.x+s2, p.y+s2)
	}

	
	/** Get the bucket which contains "user" coordinates (`x`,`y`,`z`).
	  * Buckets have their own coordinates and have indices at
	  * integer positions in a 3D space, where each integer
	  * position is a multiple of `cellSize`. */
	def getBucket(x:Double, y:Double, z:Double):Bucket[T,P,V] = buckets.get(hash(x,y,z)).getOrElse(null)

	
	/** All the objects in all the buckets containing or intersecting the bounding box defined by point
	  * (`x0`,`y0`,`z0`)  and point (`x1`,`y1`,`z1`). The first point must be
	  * the lower-left-front point and the second point must be the top-right-back
	  * point (in other words, the coordinates of the second point must be
	  * greater than the coordinates of the first point). */
	def getThings(things:HashSet[T], x0:Double, y0:Double, z0:Double, x1:Double, y1:Double, z1:Double) {
		val from = hash(x0, y0, z0)
		val to   = hash(x1, y1, z1)
		var z    = from.z

		while(z <= to.z) {
			var y = from.y
			while(y <= to.y) {
				var x = from.x
				while(x <= to.x) {
					val b = buckets.get(HashPoint3(x,y,z)).getOrElse(null)
					if(b ne null) {
						if(b.points ne null)
						things ++= b.points.asInstanceOf[HashSet[T]]
						if(b.volumes ne null)
						things ++= b.volumes.asInstanceOf[HashSet[T]]
					}
					x += 1
				}
				y += 1
			}
			z += 1
		}
	}

	def getThings(points:ArrayBuffer[P], volumes:HashSet[V], x0:Double, y0:Double, z0:Double, x1:Double, y1:Double, z1:Double) {
		val from = hash(x0, y0, z0)
		val to   = hash(x1, y1, z1)
		var z    = from.z

		while(z <= to.z) {
			var y = from.y
			while(y <= to.y) {
				var x = from.x
				while(x <= to.x) {
					val b = buckets.get(HashPoint3(x,y,z)).getOrElse(null)
					if(b ne null) {
						if(b.points ne null)
							points ++= b.points
						if((volumes ne null) && (b.volumes ne null))
							volumes ++= b.volumes
						// b.foreach { item =>
						//  	if(item.isVolume && (volumes ne null)) {
						//  		volumes += item.asInstanceOf[V]
						//  	} else {
						//  		points += item.asInstanceOf[P]
						//  	}
						// }
					}
					x += 1
				}
				y += 1
			}
			z += 1
		}
	}

	def getThings(points:ArrayBuffer[P], volumes:HashSet[V], x0:Double, y0:Double, x1:Double, y1:Double) {
		val from = hash(x0, y0, 0)
		val to   = hash(x1, y1, 0)

		var y = from.y
		while(y <= to.y) {
			var x = from.x
			while(x <= to.x) {
				val b = buckets.get(HashPoint3(x,y,0)).getOrElse(null)
				if(b ne null) {
					if(b.points ne null)
						points ++= b.points
					if((volumes ne null) && (b.volumes ne null))
						volumes ++= b.volumes
				}
				x += 1
			}
			y += 1
		}
	}

	def getThingsRadius(points:ArrayBuffer[P], volumes:HashSet[V], thing:T, radius:Double, x0:Double, y0:Double, x1:Double, y1:Double) {
		val from = hash(x0, y0, 0)
		val to   = hash(x1, y1, 0)
		val p    = thing.from
		var y    = from.y

		while(y <= to.y) {
			var x = from.x
			while(x <= to.x) {
				val b = buckets.get(HashPoint3(x,y,0)).getOrElse(null)
				if(b ne null) {
					if(b.points ne null) {
						b.points.foreach { point =>
							if(point ne thing) {
								val dx = point.from(0) - p(0)
								val dy = point.from(1) - p(1)
								val l = math.sqrt(dx*dx + dy*dy)
								//val l = point.from.distance(p)
								if(l < radius) {
									points += point
								}
							}
						}
					}
					if((volumes ne null) && (b.volumes ne null))
						volumes ++= b.volumes
				}
				x += 1
			}
			y += 1
		}
	}


	def getThingsRadius(points:ArrayBuffer[P], volumes:HashSet[V], thing:T, radius:Double, x0:Double, y0:Double, z0:Double, x1:Double, y1:Double, z1:Double) {
		val from = hash(x0, y0, z0)
		val to   = hash(x1, y1, z1)
		val p    = thing.from
		var z    = from.z
		while(z <= to.z) {
			var y = from.y
			while(y <= to.y) {
				var x = from.x
				while(x <= to.x) {
					val b = buckets.get(HashPoint3(x,y,z)).getOrElse(null)
					if(b ne null) {
						if(b.points ne null) {
							b.points.foreach { point =>
								if(point ne thing) {
									val dx = point.from(0) - p(0)
									val dy = point.from(1) - p(1)
									val dz = point.from(2) - p(2)
									val l = math.sqrt(dx*dx + dy*dy + dz*dz)
									//val l = point.from.distance(p)
									if(l < radius) {
										points += point
									}
								}
							}
						}
						if((volumes ne null) && (b.volumes ne null))
							volumes ++= b.volumes
					}
					x += 1
				}
				y += 1
			}
			z += 1
		}	
	}


	def getThingsRadius(p:Point2, radius:Double, x0:Double, y0:Double, x1:Double, y1:Double, f:(Double,P)=> Unit) {
		val from = hash(x0, y0, 0)
		val to   = hash(x1, y1, 0)
		//val p    = thing.from
		var y    = from.y
		while(y <= to.y) {
			var x = from.x
			while(x <= to.x) {
				val b = buckets.get(HashPoint3(x,y,0)).getOrElse(null)
				if(b ne null) {
					if(b.points ne null) {
						b.points.foreach { point =>
							//if(point ne thing) {
								//val l = point.from.distance(p)
								val dx = point.from(0) - p(0)
								val dy = point.from(1) - p(1)
								val l = math.sqrt(dx*dx + dy*dy)
								if(l < radius) {
									f(l,point)
									//points += point
								}
							//}
						}
					}
					//if((volumes ne null) && (b.volumes ne null))
					//	volumes ++= b.volumes
				}
				x += 1
			}
			y += 1
		}
	}


	/** All the buckets containing or intersecting the bounding box defined by point
	  * (`x0`,`y0`,`z0`)  and point (`x1`,`y1`,`z1`). The first point must be
	  * the lower-left-front point and the second point must be the top-right-back
	  * point (in other words, the coordinates of the second point must be
	  * greater than the coordinates of the first point). */
	def getBuckets(x0:Double, y0:Double, z0:Double, x1:Double, y1:Double, z1:Double):ArrayBuffer[Bucket[T,P,V]] = {
		val from = hash(x0, y0, z0)
		val to   = hash(x1, y1, z1)
		val res  = new ArrayBuffer[Bucket[T,P,V]]
		var z    = from.z

		while(z <= to.z) {
			var y = from.y
			while(y < to.y) {
				var x = from.x
				while(x < to.x) {
					val b = buckets.get(HashPoint3(x,y,z)).getOrElse(null)
					if(b ne null)
						res += b
					x += 1
				}
				y += 1
			}
			z += 1
		}
		
		res
	}
}