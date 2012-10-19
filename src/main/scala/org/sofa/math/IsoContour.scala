package org.sofa.math

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

/** One iso-square of the marching squares algorithm used by the `IsoContour`.
  *
  * An iso-square evaluates the surface implicit function at its height vertices
  * and computes from this one or more segments that intersect it. To do so,
  * this implementation uses a set or neighbor squares. If the neighbors are
  * present, it will not re-evaluate the implicit function (often costly) and
  * will also avoid recomputing the interpolation of segment points onto its
  * edges. */
class IsoSquare(val index:Int, val pos:HashPoint3, val contour:IsoContour) {
	/** Index in the points set of the contour of the points used to evaluate the iso-contour. */
	val points = new Array[Int](4)

	/** Index in the segPoints set of the contour of the points used to build segments. */
	val segPoints = new Array[Int](4)

	/** The segments inside this square. */
	var segments:ArrayBuffer[IsoSegment] = null

	def this(index:Int, x:Int, y:Int, contour:IsoContour) { this(index,HashPoint3(x,y,0),contour) }

	def isEmpty:Boolean = ((segments eq null) || segments.isEmpty)

	override def toString():String = {
		val vals = points.map { i => "%5.2f".format(contour.values(i)) }
		"square[%d %d { %s }]%s".format(pos.x, pos.y, vals.mkString(" | "), if(isEmpty)" (empty)" else "")
	}

	/** Evaluate this square and compute its segments (intersection by the contour).
	  * The `nb` array is an array of 8 potential neighbors that may already have
	  * been computed. The `eval` function allows to evaluate the contour implicit
	  * function. The `isoLevel` value allows to know at which point the contour
	  * passes in the square. */
	def eval(nb:Array[IsoSquare], eval:(Point2)=>Double, isoLevel:Double) {
		import IsoContour._

		val p0 = squarePoint(0, nb, eval); val v0 = contour.values(p0)
		val p1 = squarePoint(1, nb, eval); val v1 = contour.values(p0)
		val p2 = squarePoint(2, nb, eval); val v2 = contour.values(p0)
		val p3 = squarePoint(3, nb, eval); val v3 = contour.values(p0)

		var squareIndex = 0

		if(v0 < isoLevel) squareIndex |= 1 // p0
        if(v1 < isoLevel) squareIndex |= 2 // p1
        if(v2 < isoLevel) squareIndex |= 4 // p2
        if(v3 < isoLevel) squareIndex |= 8 // p3

        val idx = edgeTable(squareIndex)

        if(idx != 0) {
        	// Find the vertices where the contour intersects the square.

        	if((idx& 1) != 0) segPoints(0) = vertexInterp(isoLevel, 0, p0, p1, v0, v1, nb)
        	if((idx& 2) != 0) segPoints(1) = vertexInterp(isoLevel, 1, p1, p2, v1, v2, nb)
        	if((idx& 4) != 0) segPoints(2) = vertexInterp(isoLevel, 2, p2, p3, v2, v3, nb)
        	if((idx& 8) != 0) segPoints(3) = vertexInterp(isoLevel, 3, p3, p0, v3, v0, nb)

        	// Create the segments.

        	var i = 0

        	while(segmentTable(squareIndex)(i) != -1) {
        		var a = segPoints(segmentTable(squareIndex)(i))
        		var b = segPoints(segmentTable(squareIndex)(i+1))

        		// Create the segment.

        		if(segments eq null) segments = new ArrayBuffer[IsoSegment]()

        		segments += new IsoSegment(a, b)

        		contour.segmentCount += 1

        		i += 2
        	}
        }
	}

	protected def squarePoint(p:Int, nb:Array[IsoSquare], eval:(Point2)=>Double):Int = {
		import IsoContour._

		val neighbor = pointOverlap(p)

		if(nb(neighbor(0)._1) ne null) points(p) = nb(neighbor(0)._1).points(neighbor(0)._2) else
		if(nb(neighbor(1)._1) ne null) points(p) = nb(neighbor(1)._1).points(neighbor(1)._2) else
		if(nb(neighbor(2)._1) ne null) points(p) = nb(neighbor(2)._1).points(neighbor(2)._2) else {
			val i = contour.points.size
			val np = Point2((pos.x+squareCoos(p)._1)*contour.cellSize,
				            (pos.y+squareCoos(p)._2)*contour.cellSize)
			contour.points += np
			contour.values += eval(contour.points(i))
			points(p) = i

			assert(contour.points.size == contour.values.size)
		}

		points(p)
	}

	/** Interpolate the point position along a edge of a marching square defined by points
	  * `p0` and `p1` using the values `v0` and `v1` for the iso-values at these two
	  * respective points. */
	protected def vertexInterp(isoLevel:Double, edge:Int, p0:Int, p1:Int, v0:Double, v1:Double, nb:Array[IsoSquare]):Int = {
		import math._

		var i = squareEdge(edge, nb)

		if(i < 0) {
			val P0 = contour.points(p0)
			val P1 = contour.points(p1)
			val p  = if(IsoContour.interpolation) {
				if     (abs(isoLevel-v0) < 0.0001) { P0 }
				else if(abs(isoLevel-v1) < 0.0001) { P1 }
				else if(abs(v0-v1)       < 0.0001) { P0 }
				else {
					val mu = (isoLevel - v0) / (v1 - v0)
					assert(mu >= 0 && mu <= 1)
					Point2(P0.x + mu * (P1.x - P0.x),
						   P0.y + mu * (P1.y - P0.y))
				}
			} else {
				Point2(P0.x + 0.5 * (P1.x-P0.x),
					   P0.y + 0.5 * (P1.y-P0.y))
			}
		}

		i
	}

	protected def squareEdge(edge:Int, nb:Array[IsoSquare]):Int = {
		import IsoContour._

		if(nb(edgeOverlap(edge)._1) ne null) {
			nb(edgeOverlap(edge)._1).segPoints(edgeOverlap(edge)._2) 
		} else {
			-1
		}
	}
}

/** A simple segment in the iso-contour, it references points in the `segPoints`field of
  * the `IsoContour`. */
class IsoSegment(val a:Int, val b:Int) {}

/** Build a contour from an iso-contour provided by an evaluation function and an iso-level
  * parameter.
  *
  * The technique used is the "marching squares" algorithm.
  * 
  * Most of the code and ideas for this come from the 
  * http://paulbourke.net/geometry/polygonise/  web page as well as the C++ implementation
  * at http://www.idevgames.com/forums/thread-8761.html.
  *
  * This implementation used a space-hash to locate the marching squares already computed.
  * Each marching square is associated with a position in a spatial space where coordinates
  * are integers (the indices of the squares in the two axes). The `cellSize`
  * parameter gives the side of a square. Therefore the real coordinates of each square are
  * given by multiplying its coordinates by `cellSize`.   
  * 
  * The implementation tries to ensure that no square will be computed twice using this spacehash.
  * This allows the addSquaresAt() method to take as argument an area of space to explore, but
  * avoiding to compute several times zones that may already have been computed by a previous
  * call, if they intersect.
  * 
  * Furthermore, as most often in these algorithms, the evaluation function for the contour is
  * quite costly, we store in each square the four evaluations (for each vertex of the square)
  * and use the space hash to find the neighbors of a square that would already have been computed
  * to avoid calling too often the evaluation function. As each square share its vertices with 8
  * neighbors the economy is important.
  * 
  * Using these points, the marching squares algorithm will need to compute segments inside squares.
  * These segments uses vertices interpolated on the edges of the square. We can also reuse the
  * computation of the neighbor squares if already computed.
  * 
  * The result of the algorithm is a set of segments that share points (in the `segPoints`
  * field). */
class IsoContour(val cellSize:Double) {
	/** Set of evaluation points, where the iso-contour values are taken. */
	val points = new ArrayBuffer[Point2]()

	/** Values of the iso-contour, each value correspond to a point in `points`. */
	val values = new ArrayBuffer[Double]()

	/** Set of interpolated segment points, the points forming the segments of the contour. */
	val segPoints = new ArrayBuffer[Point2]()

	/** Number of segments computed by adding squares (segments are stored
	  * independently in squares). */	
	var segmentCount = 0
}

object IsoContour {
	val interpolation = true

	/** Coordinates of each square point in the square as multiples of cellSize. */
	val squareCoos = Array[Tuple2[Int,Int]] (
		(0, 0),	// 0
		(1, 0),	// 1
		(1, 1),	// 2
		(0, 1)	// 3
	)

	/** Fasten the search for neighbors. */
	val neighborSquares = Array[Tuple2[Int,Int]] (
		(-1,-1),	// 0
		( 0,-1),	// 1
		( 1,-1),	// 2
		// -------------
		(-1, 0),	// 3
		( 0, 0),	// 4 XXX center
		( 1, 0),	// 5
		// -------------
		(-1, 1),	// 6
		( 0, 1),	// 7
		( 1, 1)		// 8
	)

	/** Fasten the search for overlapping points of a given point for a square.
	  * Suppose you search the points of neighbor squares that overlap point 0.
	  * You look at the first cell of theis array and find an array of 2-tuples.
	  * Each of these tuples points at a neighbor square, then at the point in
	  * this neighbor square that is overlapping point 0. */
	val pointOverlap = Array[Array[Tuple2[Int,Int]]] (
		Array[Tuple2[Int,Int]]( (0,2), (1,3), (3,1) ),	// p0
		Array[Tuple2[Int,Int]]( (1,2), (2,3), (5,0) ),	// p1
		Array[Tuple2[Int,Int]]( (5,3), (7,1), (8,0) ),	// p2
		Array[Tuple2[Int,Int]]( (3,2), (6,1), (7,0) )	// p3
	)

	/** Fasten the search for overlapping edges of a given edge for a square.
	  * Suppose you search the edges of neighbor square that overlap edge 0.
	  * You look at the first cell of this array and find a 2-tuple.
	  * This 2-tuple points at a neighbor square, and then at the edge in
	  * this neighbor square taht is overlapping edge 0. */
	val edgeOverlap = Array[Tuple2[Int,Int]] (
		(1,2),		// e0
		(5,3),		// e1
		(7,0),		// e2
		(3,1)		// e3
	)

	val edgeTable = Array[Int] (
		0x0,     //0000,
        0x9,     //1001,
        0x3,     //0011
        0xa,     //1010
        0x6,     //0110, 
        0xf,     //1111,
        0x5,     //0101
        0xc,     //1100
        0xc,     //1100
        0x5,     //0101
        0xf,     //1111,
        0x6,     //0110,
        0xa,     //1010
        0x3,     //0011
        0x9,     //1001,
        0x0	     //0000
	)

	val segmentTable = Array[Array[Int]] (
		Array[Int](-1,-1,-1,-1,-1),
        Array[Int](0,3,-1,-1,-1),
        Array[Int](1,0,-1,-1,-1),
        Array[Int](1,3,-1,-1,-1),
        Array[Int](2,1,-1,-1,-1),
        Array[Int](2,1,0,3,-1),
        Array[Int](2,0,-1,-1,-1),
        Array[Int](2,3,-1,-1,-1),
        Array[Int](3,2,-1,-1,-1),
        Array[Int](0,2,-1,-1,-1),
        Array[Int](1,0,3,2,-1),
        Array[Int](1,2,-1,-1,-1),
        Array[Int](3,1,-1,-1,-1),
        Array[Int](0,1,-1,-1,-1),
    	Array[Int](3,0,-1,-1,-1),
        Array[Int](-1,-1,-1,-1,-1)
	)
}