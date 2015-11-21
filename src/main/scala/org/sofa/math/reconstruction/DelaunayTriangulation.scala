package org.sofa.math.reconstruction

import scala.math._
import scala.io.BufferedSource
import scala.collection.mutable.ArrayBuffer

import java.io.{File, InputStream, FileInputStream, FileOutputStream, PrintStream, IOException}

import org.sofa.Timer
import org.sofa.collection.{ReuseArrayBuffer, Indexed}
import org.sofa.math.{Point3, Vector3, Rgba}
import org.sofa.math.{Rgba, Point2, Point3, Point4, Vector2, Vector3, Plane, Line, Triangle, IndexedTriangle, Edge, IndexedEdge, Circle}


/** DelaunayTriangulation companion object. */
object DelaunayTriangulation {
	def apply(fileName:String, scaleFactor:Double, yFactor:Double):DelaunayTriangulation = {
		if(fileName.endsWith(".xyz")) {
			PointCloud.readFileXYZ(fileName, new DelaunayTriangulation(scaleFactor, yFactor)).asInstanceOf[DelaunayTriangulation]
		} else {
			throw new RuntimeException("only '.xyz' files are accepted")
		}
	}

	def main(args:Array[String]) {
		options(args.toList)
		delaunayCmdLine
	}

	private[this] var inputXYZ:String = _

	private[this] var outputOBJ:String = _

	private def options(list:List[String]) {
		list match {
			case Nil => {}
			case "-out" :: fname :: tail => { outputOBJ = fname; options(tail) }
			case a :: tail => { if(a.startsWith("-")) usage("Unknown option '%s'.".format(a)) else { inputXYZ = a; options(tail) } }
		}
	}

	private def usage(message:String) {
		if(message ne null)
			printf("%s%n%n".format(message))
		println("Usage: delaunay <input.xyz>")
		println("       <input.xyz>          A XYZ file.")
		println("       -out <filename>      Output a file in the given filename as a wavefront OBJ format.")
		println("                            If this option is not given the data is output on standard output.")
		sys.exit(1)
	}

	private def delaunayCmdLine() {
		var cloud:DelaunayTriangulation = null
		if(inputXYZ eq null) {
			printf("* Generating...%n")
			cloud = new DelaunayTriangulation(1.0, 1.0)
			var i = 0
			while(i < 100) {
				cloud.addPoint(Point3.random(0, 500, 0, 20, 0, 500))
				i += 1
			}
		} else {
			printf("* Reading...%n")
			cloud = DelaunayTriangulation(inputXYZ, 1.0, 1.0)
			printf("* Rescaling...%n")
			cloud.rescale(1000, true)
			cloud.swapYZ()
		}

		printf("* Triangulating...%n")
		cloud.triangulation()
		printf("* Output...")
		cloud.toObj(outputOBJ)
		printf(" OK%n")
	}
}


/** A point cloud able to compute a Delaunay triangulation. */
class DelaunayTriangulation(scaleFactor:Double, yFactor:Double) extends PointCloud(scaleFactor, yFactor) {

	/** Generated triangles that may be removed later. This is the TMP list. */
	protected val tmpTriangles = new ReuseArrayBuffer[DelaunayTriangle]()

	/** Triangles that will appear in the final triangulation. This is the OK list. */
	val triangles = new ReuseArrayBuffer[DelaunayTriangle]()

	/** Temporary buffer for bad triangles, avoid to reallocate it. A bad triangle
	  * is a triangle whose circumcircle contains one of the three points of another
	  * triangle. */
	private[this] val bad = new ArrayBuffer[DelaunayTriangle]()

	/** Temporary buffer for ok triangles, avoid to reallocate it. An OK triangle
	  * is a triangle whose circumcircle is no more succeptible to contain one of
	  * the points of another triangle. This is due to the fact we sort points
	  * along X. */
	private[this] val ok = new ArrayBuffer[DelaunayTriangle]()

	/** Triangulate the point cloud using a Bowyer-Watson algorithm.
	  *
	  * The triangulation is stored in the `triangles` field.
	  *
	  * Although the Bowyer-Watson method is naively in O(n^2), if points
	  * are sorted along X, which is the case here, we greatly reduce the
	  * triangulation time by pruning some triangles that we are sure will
	  * appear in the final triangulation.
	  *
	  * If the `mergePoints` paramter is true, the method tries to find
	  * superposed points along the XZ plane. Such points may produce
	  * degenerate triangles and may produce bad triangulation. If you
	  * know this * is not the case, you can gain some time by disabling
	  * this.
	  *
	  * Interesting resource: "http://paulbourke.net/papers/triangulate/" */
	def triangulation(mergePoints:Boolean = true) {
		triangles.clear()
		tmpTriangles.clear()

		val timer = new Timer()
		val hole = ArrayBuffer[IndexedEdge]()
		
		timer.measureStart("triangulation")
		
		// Remove superposed points that may create invalid triangles.

		if(mergePoints) {
			val n = points.size
			timer.measureStart("mergeDouble")
			mergeClosePoints(closePointsXZ, 0.01)
			timer.measureEnd("mergeDouble")
			printf("* removed %d double points%n", n - points.size)
		}

		// Sort on X each point to allow a classification between
		// OK triangles and TMP triangles. This is the optimization
		// that makes the algorithm extremely fast.
		
		timer.measureStart("sorting")
		sortOnX()
		timer.measureEnd("sorting")			
		
		// Enclose the point cloud is two super-triangles forming a rectangle.

		addSuperTriangles()
		
		// Insert each point (excepted those of the super triangles) one at a time.

		var i = 0
		val n = points.size - 4
		printf("* triangulate [%d points]", n)

		while(i < n) {
			if(i%1000 == 0) printf("[%d]", i)
			if(i%10000 == 0) { printf(" -> %d OK -> %d TMP%n", triangles.size, tmpTriangles.size); timer.printAvgs("Triangulation") }

			val p = points(i)
			
			// Find bad triangles in TMP triangle list

			timer.measureStart("findBadTriangles")
			val bad = findBadTriangles(p)
			timer.measureEnd("findBadTriangles")
			
			// Polygonal hole.

			timer.measureStart("polygonalHole")
			hole.clear
			bad.foreach { t => nonSharedEdgesOfTriangle(t, bad, hole) }
			timer.measureEnd("polygonalHole")
			
			// Retriangulate.

			timer.measureStart("retriangulate")
			retriangulateHole(hole, i)
			timer.measureEnd("retriangulate")

			i += 1
		}

		// Move any remaining tmp triangle into the OK triangles set.

		ok.clear()
		tmpTriangles.foreach { ok += _ }
		ok.foreach { okTriangle(_) }

		timer.measureEnd("triangulation")
		printf(" OK%n")
		timer.printAvgs("Triangulation")
	}

	/** Add a triangle in the TMP list. */
	protected def addTriangle(t:DelaunayTriangle) { tmpTriangles += t }

	/** Remove a triangle of the TMP list. */
	protected def delTriangle(t:DelaunayTriangle) { tmpTriangles.remove(t.index) }

	/** Make a triangle OK by removing it from the TMP list and putting it in the OK list.
	  * Such a triangle circumcicle will never contain one of the three points of the
	  * future inserted triangles due to the sorting along X of points. */
	protected def okTriangle(t:DelaunayTriangle) {
		tmpTriangles.remove(t.index)
		triangles += t
	}

	/** Add two "super" triangles that enclose the whole point cloud and that
	  * form by construction a valid Delaunay triangulation. */
	protected def addSuperTriangles() {
		// Use the property of rectangles to be a triangulation on the two diagonals.
		// Add four points englobing the cloud and make a rectangle with two initial
		// triangles.

		val p = points.size
		val off = 10
		val minx = min.x
		val minz = min.z
		val maxx = max.x
		val maxz = max.z

		addPoint(minx-off, 0, minz-off)
		addPoint(maxx+off, 0, minz-off)
		addPoint(maxx+off, 0, maxz+off)
		addPoint(minx-off, 0, maxz+off)

		addTriangle(new DelaunayTriangle(p, p+1, p+2, points))
		addTriangle(new DelaunayTriangle(p, p+2, p+3, points))
	}

	/** Find the triangles in the TMP list whose circumcircle contain point `p`.
	  * Put each of these triangles in the `bad` list. Among these triangles
	  * find those whose distance between their circumcircle center and `p` is
	  * larger than the circumcircle radius. Such triangles will never be
	  * considered again since points are sorted along X, and put them in
	  * the OK list. */
	protected def findBadTriangles(p:Point3):Seq[DelaunayTriangle] = {
		bad.clear()
		ok.clear()

		tmpTriangles.foreach { t =>
				val c = t.circumcircleXZ
				if(c.isInside(p.x, p.z)) bad += t
				else if(p.x-c.center.x > c.radius) ok += t
		}

		// Never again consider this triangle, we are past its circumcenter absissa.

		ok.foreach { okTriangle(_) }

		// Remove bad triangles.

		bad.foreach { delTriangle(_) }
		bad
	}

	/** Find edges of triangle `t` that are not shared by any other triangle
	  * in the `bad` list and add these edges to `hole`. Repeated on all
	  * triangles of `bad` this defines the boudary of the `bad` triangle
	  * group. */
	protected def nonSharedEdgesOfTriangle(t:Triangle, bad:Seq[Triangle], hole:ArrayBuffer[IndexedEdge]) {
		var edge = t.edge0.asInstanceOf[IndexedEdge]
		if(!isSharedWithTriangles(t, edge, bad)) hole += edge
		edge = t.edge1.asInstanceOf[IndexedEdge]
		if(!isSharedWithTriangles(t, edge, bad)) hole += edge
		edge = t.edge2.asInstanceOf[IndexedEdge]
		if(!isSharedWithTriangles(t, edge, bad)) hole += edge
	}

	/** Is the given `edge` of triangle `t` shared with one of the triangles of the `bad` list ? */
	protected def isSharedWithTriangles(t:Triangle, edge:IndexedEdge, bad:Seq[Triangle]):Boolean = {
		var shared = false
		var i = 0
		val n = bad.size
		while(i < n && !shared) {
			val bt = bad(i)
			if(bt ne t) shared = t.isShared(edge, bad(i))
			i += 1
		}
		shared
	}

	/** Create new triangles in the given `hole` around new point `p`. */
	protected def retriangulateHole(hole:ArrayBuffer[IndexedEdge], p:Int) {
		hole.foreach { edge => addTriangle(new DelaunayTriangle(p, edge.i0, edge.i1, points)) }
	}

	override def toObj(fileName:String) {
		var fname = fileName
		var out = System.out

		if( fname ne null) {
			if(!fname.endsWith(".obj")) fname = "%s.obj".format(fname)
			out = new PrintStream(new FileOutputStream(fname))
		}

		points foreach { p =>
			out.print("v %f %f %f%n".format(p.x-min.x, p.y-min.y, p.z).replace(",", "."))
		}

		if(triangles.size > 0) {
			val normals = computeNormals()

			normals foreach { n =>
				out.print("vn %f %f %f%n".format(n.x, n.y, n.z).replace(",", "."))
			}

			triangles foreach { t =>
				out.print("f %d//%d %d//%d %d//%d%n".format(t.i0+1, t.i0+1, t.i1+1, t.i1+1, t.i2+1, t.i2+1))
				//out.print("f %d %d %d%n".format(t.i0, t.i1, t.i2))
			}
		} else {
			printf("* No triangles to output...")
		}

		out.flush
		out.close
	}

	/** Points to triangles relationship.
	  *
	  * The process in O(n) with n * the number of triangles. The size of
	  * the array is the number of points, and for each point there is an
	  * array of triangle indices.
	  * 
	  * Note : We could compute this as the triangulation goes, but it would
	  * cost time for triangles that will be removed later. */
	def pointToTriangle():Array[ArrayBuffer[Int]] = {
		var t = 0
		val n = triangles.size
		val p2t = new Array[ArrayBuffer[Int]](points.size)

		while(t < n) {
			val tri = triangles(t)
			
			if(p2t(tri.i0) eq null) p2t(tri.i0) = new ArrayBuffer[Int]()
			if(p2t(tri.i1) eq null) p2t(tri.i1) = new ArrayBuffer[Int]()
			if(p2t(tri.i2) eq null) p2t(tri.i2) = new ArrayBuffer[Int]()
			
			p2t(tri.i0) += t
			p2t(tri.i1) += t
			p2t(tri.i2) += t
			
			t += 1
		}

		p2t
	}

	/** Compute a normal for each point as the average normal of
	  * each triangle sharing this point. The result is therefore
	  * and array of normal with each index  */
	def computeNormals():Array[Vector3] = {
		val p2t = pointToTriangle

		// Compute the normals of each triangle

		var t = 0
		val nt = triangles.size
		val tnormals = new Array[Vector3](nt)
		
		while(t < nt) {
			tnormals(t) = triangles(t).normal
			t += 1
		}

		// For each point find all the participating triangle normals
		// and average them.

		var i = 0
		val np = points.size
		val normals = new Array[Vector3](np)

		while(i < np) {
			val N = Vector3(0,0,0)
			if(p2t(i) ne null) p2t(i).foreach { p => N += tnormals(p) } else N.set(1,0,0)
			N.normalize
			normals(i) = N
			i += 1
		}

		normals
	}
}


/** A triangle "by index" usable in a ReuseArrayBuffer, with caching of the
  * circumcicle to avoid recomputations. */
class DelaunayTriangle(i0:Int,i1:Int,i2:Int,points:IndexedSeq[Point3]) extends IndexedTriangle(i0,i1,i2,points) with Indexed {
	var index = -1
	
	override val edge0 = IndexedEdge(i0, i1, points)
	override val edge1 = IndexedEdge(i1, i2, points)
	override val edge2 = IndexedEdge(i2, i0, points)
	
	private[this] var ccircle:Circle = _
	
	// circumcircleXZ	
	// val min = Point3(ccircle.center.x-ccircle.radius, 0.1, ccircle.center.y-ccircle.radius)
	// val max = Point3(ccircle.center.x+ccircle.radius, 0.1, ccircle.center.y+ccircle.radius)
	// if(colinearPointsXZ()) throw new RuntimeException("triangle with colinear points !")

	def setIndex(i:Int) { index = i }

	override def circumcircleXZ = { if(ccircle eq null) { ccircle = super.circumcircleXZ }; ccircle }
}
