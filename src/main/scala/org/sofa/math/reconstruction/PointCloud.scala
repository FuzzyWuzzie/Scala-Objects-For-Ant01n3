package org.sofa.math.reconstruction

import scala.math._
import scala.io.BufferedSource
import scala.collection.mutable.ArrayBuffer

import org.sofa.math.{Point3, Vector3, Rgba}

import java.io.{File, InputStream, FileInputStream, FileOutputStream, PrintStream, IOException}


/** Point cloud companion object. */
object PointCloud {

	private final val PointDesc = """\s*([0-9]+[\.,]?[0-9]*)\s+([0-9]+[\.,]?[0-9]*)\s+(-?[0-9]+[\.,]?[0-9]*)\s*""".r
	private final val Empty     = """\s*""".r

	/** Create a point cloud from a XYZ file.
	  * `scaleFactor` allows scale the cloud. `yFactor` allows to only enlarge heights.
	  * The two factors can be given at the same time. */
	def apply(fileName:String, scaleFactor:Double = 1.0, yFactor:Double = 1.0):PointCloud = {
		if(fileName.endsWith(".xyz")) {
			readFileXYZ(fileName, new PointCloud(scaleFactor, yFactor))
		} else {
			throw new RuntimeException("only '.xyz' files are accepted")
		}
	}

	/** Created a [[PointCloud]] from a CSV or ASC file. */
	def readFileXYZ(fileName:String, cloud:PointCloud):PointCloud = {
		val src    = new BufferedSource(new FileInputStream(fileName))
		var curRow = 0
		var i = 0

		src.getLines.foreach { _ match {
			case PointDesc(x, y, z) => { cloud.addPoint(x.toDouble, y.toDouble, z.toDouble) }
			case Empty()            => {}
			case line               => { printf("Unrecognized line '%s'%n", line) }
		}}

		cloud
	}

	var input = ""
	var output:String = null

	def main(args:Array[String]) {
		params(args.toList)

		if(input eq null) {
			throw new RuntimeException("ypu must specify an input...")
		}

		printf("[Reading %s]".format(input))
		val cloud = PointCloud(input, 1.0, 1.0)
		printf("[%d points]%n".format(cloud.points.size))
		printf("[Writing %s]%n".format(if(output ne null) output else "<stdout>"))
		cloud.toObj(output)
	}

	def params(args:List[String]) { args match {
		case Nil => {}
		case "-src" :: file :: tail => { input = file; params(tail) }
		case "-dst" :: file :: tail => { output = file; params(tail) }
		case a :: tail => { throw new RuntimeException("unknwon argument '%s'...".format(a)) }
	}}

}


/** A set of points considered as a cloud and methods to handle them
  *
  * `scaleFactor` allows scale the cloud. `yFactor` allows to only enlarge heights.
  * The two factors can be given at the same time. 
  *
  * Point clouds are considered by convention in a coordinate system where X positive
  * goes right, Y positive goes up, and Z positive goes toward the viewer, hence a
  * right handed coordinate system.
  */
class PointCloud(scaleFactor:Double, yFactor:Double) {
	
	/** The point data. */	
	var points = new ArrayBuffer[Point3]()

	/** The minimum coordinates (not part of the cloud). */
	val min = Point3(Double.MaxValue, Double.MaxValue, Double.MaxValue)

	/** The maximium coordinates (not part of the cloud). */
	var max = Point3(Double.MinValue, Double.MinValue, Double.MinValue)

	protected def checkMin(p:Point3) { checkMin(p.x, p.y, p.z) }

	protected def checkMin(x:Double, y:Double, z:Double) { 
		if(x < min.x) min.x = x 
		if(x > max.x) max.x = x
		if(y < min.y) min.y = y
		if(y > max.y) max.y = y
		if(z < min.z) min.z = z
		if(z > max.z) max.z = z			
	}

	/** Number of points. */
	def size:Int = points.size

	/** `i`-th point. */
	def point(i:Int) = points(i)

	/** `i`-th point. */
	def apply(i:Int) = points(i)

	/** Add a point `p` at index `size()`. */
	def addPoint(p:Point3) { addPoint(p.x, p.y, p.z) }

	/** Add a point (`x`, `y`, `z`) at index `size()`. */
	def addPoint(x:Double, y:Double, z:Double) {
		points += Point3(x, y, z)
		checkMin(x, y, z)
	}

	/** Sort the point cloud so that points are ordered by their X coordinate,
	  * the lower ones first. */
	def sortOnX() { points = points.sortWith { (a, b) => a.x < b.x } }

	/** Swap each point Y and Z coordinates. */
	def swapYZ() { points.foreach { p => p.set(p.x, p.z, p.y) } }

	/** Move the whole cloud so that the minimum point is at (0,0,0). */
	def toOrigin() {
		points.foreach { p => p.set(p.x-min.x, p.y-min.y, p.z-min.z) }
		max.x -= min.x
		max.y -= min.y
		max.z -= min.z
		min.x = 0
		min.y = 0
		min.z = 0
	}

	/** Move the cloud at origin and resize it by `scale` factor. 
	  * If `invertY` is true, invert the Y coordinate (often seen in XYZ files). */
	def rescale(scale:Double, invertY:Boolean) {
		toOrigin()
		val ratio = math.max(max.x, math.max(max.y, max.z))
		if(invertY)
		     points.foreach { p => p.set((p.x/ratio)*scale, scale-((p.y/ratio)*scale), (p.z/ratio)*scale) }
		else points.foreach { p => p.set((p.x/ratio)*scale, (p.y/ratio)*scale, (p.z/ratio)*scale) }
		max.x = scale
		max.y = scale
		max.z = scale
	}

	/** Create a wavefront OBJ file named `fileName` containing only points.
	  * If `fileName` is null, output the file to the standard output. */
	def toObj(fileName:String) {
		val out = if(fileName eq null) System.out else new PrintStream(new FileOutputStream(fileName))
		points.foreach { p => out.print("v %f %f %f%n".format(p.x-min.x, p.y-min.y, p.z).replace(",", ".")) }
		out.flush
		out.close
	}

	/** Utility method to use with `mergeClosePoints()` to see
	  * if two points are close by `distance` one of another. */
	def closePoints(a:Point3, b:Point3, distance:Double):Boolean = {
		val x = b.x-a.x
		val y = b.y-a.y
		val z = b.z-a.z
		(x*x + y*y + z*z) < distance*distance
	}

	/** Utility method to use with `mergeClosePoints()` that compare
	  * point only for their X and Z axes to see if the two
	  * points are close by `distance` one of another. */
	def closePointsXZ(a:Point3, b:Point3, distance:Double):Boolean = {
		val x = b.x-a.x
		val z = b.z-a.z
		(x*x + z*z) < distance*distance
	}

	/** Locate too close points and merge them.
	  *
	  * This modifies the points set by removing points that are superposed.
	  *
	  * Points are considered one on another if the `close` function returns
	  * true. The `distance` parameter is passed to this function and is
	  * often considered the radius of closeness.
	  *
	  * Note that this method alter the points ordering, the minimum point,
	  * maximum point and the number of points. */
	def mergeClosePoints(close:(Point3,Point3,Double)=>Boolean, distance:Double = 0.001) {
		case class IndexedPoint(idx:Int, var tmp:Int)

		min.set(Double.MaxValue, Double.MaxValue, Double.MaxValue)
		max.set(Double.MinValue, Double.MinValue, Double.MinValue)

		// Sort all points along X. This allows to prune points
		// when comparing with the others, and make this fast.

		sortOnX

		val okPoints = new ArrayBuffer[Point3]()
		val tmpPoints = new ArrayBuffer[IndexedPoint]()
		val ok = new ArrayBuffer[IndexedPoint]()
		val n = points.size
		var i = 1

		tmpPoints += IndexedPoint(0, 0)

		// Insert points one by one.

		while(i < n) {
			// Browse tmp points to look for doubles with the
			// currently inserted point. Also, put
			// points that are too far along X in the ok list,
			// since points are sorted along X, these points
			// need no more to be considered.

			var j = 0
			var m = tmpPoints.size
			var found = false
			ok.clear()

			while(j < m) {
				if(points(i).x - points(tmpPoints(j).idx).x > distance) ok += tmpPoints(j)
				else if(!found) found = close(points(i), points(tmpPoints(j).idx), distance)
				j += 1
			}

			// Prune points that will now be too far to be superposed.
			// This is the main optimization making this process fast.
			// The TMP list removal is fast since we pivote removed
			// points with the last one of the TMP list and we know
			// the index of each point in the TMP list.

			ok.foreach { p =>
				okPoints += points(p.idx)
				if(p.tmp < m-1) {
					tmpPoints(m-1).tmp = p.tmp
					tmpPoints(p.tmp) = tmpPoints(m-1)
				}
				tmpPoints.remove(m-1)
				m -= 1
			}
			
			// Insert the point in the TMP list if no close match only.
			// Else merely ignore the point, its a double.

			if(!found) {
				checkMin(points(i))
				tmpPoints += IndexedPoint(i, tmpPoints.size)
			}

			i += 1
		}

		// Add remaining TMP points in the OK list.

		tmpPoints.foreach { p =>
			checkMin(points(p.idx))
			okPoints += points(p.idx) 
		}

		// Replace the old point list with the new.

		points = okPoints
	}
}
