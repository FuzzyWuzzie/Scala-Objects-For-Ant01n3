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

	/** The triangles inside this square. */
	var triangles:ArrayBuffer[IsoContourTriangle] = null

	def this(index:Int, x:Int, y:Int, contour:IsoContour) { this(index,HashPoint3(x,y,0),contour) }

	def isEmpty:Boolean = ((!hasSegments) && (!hasTriangles))

	def hasSegments:Boolean = ((segments ne null) && segments.size > 0)

	def hasTriangles:Boolean = ((triangles ne null) && triangles.size > 0)

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
		val p1 = squarePoint(1, nb, eval); val v1 = contour.values(p1)
		val p2 = squarePoint(2, nb, eval); val v2 = contour.values(p2)
		val p3 = squarePoint(3, nb, eval); val v3 = contour.values(p3)

		var squareIndex = 0

		if(v0 < isoLevel) squareIndex |= 1 // p0
        if(v1 < isoLevel) squareIndex |= 2 // p1
        if(v2 < isoLevel) squareIndex |= 4 // p2
        if(v3 < isoLevel) squareIndex |= 8 // p3

        val idx = edgeTable(squareIndex)

        if(idx > 0 && idx < 16) {
        	// Find the vertices where the contour intersects the square.

        	if((idx & 1) != 0) segPoints(0) = vertexInterp(isoLevel, 0, p0, p1, v0, v1, nb)
        	if((idx & 2) != 0) segPoints(1) = vertexInterp(isoLevel, 1, p1, p2, v1, v2, nb)
        	if((idx & 4) != 0) segPoints(2) = vertexInterp(isoLevel, 2, p2, p3, v2, v3, nb)
        	if((idx & 8) != 0) segPoints(3) = vertexInterp(isoLevel, 3, p3, p0, v3, v0, nb)

        	// Create the segments.

        	var i = 0

        	while(segmentTable(squareIndex)(i) != -1) {
        		val a = segPoints(segmentTable(squareIndex)(i))
        		val b = segPoints(segmentTable(squareIndex)(i+1))

        		// Create the segment.

        		if(segments eq null) segments = new ArrayBuffer[IsoSegment]()

        		segments += new IsoSegment(a, b)

        		contour.segmentCount += 1

        		i += 2
        	}

        	if(contour.computeSurfaceFlag) {
        		i = 0
        		while(triangleTable(squareIndex)(i) != -1) {
        			var a = triangleTable(squareIndex)(i)
        			var b = triangleTable(squareIndex)(i+1)
        			var c = triangleTable(squareIndex)(i+2)

        			if(a >= 10) { a = -(points(a-10)+1) } else { a = segPoints(a) }
        			if(b >= 10) { b = -(points(b-10)+1) } else { b = segPoints(b) }
        			if(c >= 10) { c = -(points(c-10)+1) } else { c = segPoints(c) }

        			if(triangles eq null) triangles = new ArrayBuffer[IsoContourTriangle]()

        			triangles += new IsoContourTriangle(a, b, c)

        			contour.triangleCount += 1

        			i += 3
        		}
        	}
        } else if(contour.computeSurfaceFlag && idx == 0) {
        	// It should not be needed to add this code, the general code
        	// above should work.
        	var a = -(points(0)+1)
        	var b = -(points(1)+1)
        	var c = -(points(2)+1)
        	var d = -(points(3)+1)

        	if(triangles eq null) triangles = new ArrayBuffer[IsoContourTriangle]()

        	triangles += new IsoContourTriangle(a, c, b)
        	triangles += new IsoContourTriangle(a, d, c)

        	contour.triangleCount += 2
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

			// Add the point.

			i = contour.segPoints.size
			contour.segPoints += p
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

class IsoContourTriangle(val a:Int, val b:Int, val c:Int) {}

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

	/** The set of squares used to evaluate the surface. */
	val squares = new ArrayBuffer[IsoSquare]()
	
	/** List of non-empty squares. */
	val nonEmptySquares = new ArrayBuffer[IsoSquare]()
	
	/** The hash map of squares indexed by their position in integer space. */
	val spaceHash = new HashMap[HashPoint3,IsoSquare]()

	/** Number of segments computed by adding squares (segments are stored
	  * independently in squares). */	
	var segmentCount = 0

	/** Number of triangles computed by adding squares (triangles are stored
	  * independently in squares). */
	var triangleCount = 0

	/** If true, triangles are also computed from segments. They create a surface
	  * that fills the iso-contour. */
	var computeSurfaceFlag = false

	/** The neighbor squares array, to avoid creating it at each new square insertion. */
	protected val nbSq = new Array[IsoSquare](9)

	/** Browse each point used by segments. The indices are important as segments
	  * reference these indices and neighbor segments share the points. */
	def foreachSegmentPoint(code:(Int, Point2)=>Unit) {
		var i = 0
		val n = segPoints.size
		while(i < n) {
			code(i, segPoints(i))
			i += 1
		}
	}
	
	/** Browse each segment computed to reconstruct the surface. Each segment pertains
	  * to a square (but a square can own several segments), and each segment has a unique
	  * index. Each segment is a set of two integer indices that reference the points
	  * given by `foreachSegmentPoint()` */
	def foreachSegment(code:(Int,IsoSquare,IsoSegment)=>Unit) {
		var i = 0
		var c = 0
		var nc = nonEmptySquares.size
		while(c < nc) {
			val square = nonEmptySquares(c)
			if(square.hasSegments) {
				var t = 0
				var nt = square.segments.size
				while(t < nt) {
					code(i, square, square.segments(t))
					t += 1
					i += 1
				} 
			}
			c += 1
		}
	}

	/** Enable the computation of the surface or disable it. The surface is a set of
	  * triangles that fill the iso-contour. */
	def computeSurface(on:Boolean) {
		computeSurfaceFlag = on
	}

	/** Add a set of squares in a given portion of space. The portion of space is given in
	  * marching squares space, where (0,0) is the origin that describe a square between
	  * points (0,0) and (1,1). The squares have a side whose length is `cellSize`.
	  * The start position is given by `x`, `y`. from this position `countX` squares
	  * are added along X and `countY` squares along the Y axis.
	  * If this portion of space already contains squares computed by a previous call
	  * to this method, they are not computed anew. */
	def addSquaresAt(x:Int, y:Int, countX:Int, countY:Int, eval:(Point2)=>Double, isoLevel:Double) {
		var xx = 0
		var yy = 0
		
		while(yy < countY) {
			xx = 0
			while(xx < countX) {
				val square = addSquareAt(x+xx, y+yy, eval, isoLevel)
				xx += 1
			}
			yy += 1
		}
	}

	/** The nearest square position fromt the real coordinates. */
	def nearestSquarePos(x:Double, y:Double):HashPoint3 = {
		var xx = x/cellSize
		var yy = y/cellSize
		
		HashPoint3(xx.toInt, yy.toInt, 0)
	}

	/** Add a single marching-square at the given location in iso-square space.
	  * 
	  * The iso-square
	  * space is a discretization of the user space where coordinates are integers in
	  * the two dimensions, each at `cellSize` of each other. For example a square at
	  * (1,0) will occupy be at (cellSize,0) in user space and occupy the iso-square
	  * space between coordinates (1,0) and (2,1). The square will use the `eval`
	  * function to compute the iso-contour value at each of its four vertices. If
	  * the surface is above the `isoLevel` given for some points but not others, the
	  * square will contain some segments that describe the intersection of the surface
	  * with its space.
	  * 
	  * Calling several times this function on the same square computes it only once. */
	def addSquareAt(x:Int, y:Int, eval:(Point2)=>Double, isoLevel:Double):IsoSquare = {
		val p = HashPoint3(x,y,0)

		spaceHash.get(p).getOrElse {
			val i = squares.size
			var square = new IsoSquare(i, p, this)

			// Find the potential 8 surrounding squares
			var neighbors = findNeighborSquares(p)
			
			square.eval(neighbors, eval, isoLevel)
			
			spaceHash += ((p, square))
			squares += square
			
			if(!square.isEmpty) {
				nonEmptySquares += square
			}

			square
		}
	}
	
	/** Find the 8 potential neighbors of a square, if already present. This fills
	  * the `nb` array and returns it. Each neighbor in this array is either null
	  * or present. */
	protected def findNeighborSquares(p:HashPoint3):Array[IsoSquare] = {
		import IsoContour._
		var i = 0

		while(i < neighborSquares.length) {
			if(i != 4) {
				val n   = neighborSquares(i)
				val pp  = HashPoint3(p.x+n._1, p.y+n._2, 0)
				nbSq(i) = spaceHash.get(pp).getOrElse(null)
			} else {
				nbSq(i) = null
			}
			i += 1
		}

		nbSq
	}
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

	/** Knowing the value of the points that are above the isoLevel, map
	  * the indices of the edges that are used to build the segments.
	  * For example if only point 0 is above isoLevel we have
	  * an index of 0001. In this case the two segments to look at are
	  * given by 0x9 that is 1001, the segment 3 and 0 (around point 0). */
	val edgeTable = Array[Int] (
		0x0,     // 0000
        0x9,     // 1001
        0x3,     // 0011
        0xa,     // 1010
        0x6,     // 0110 
        0xf,     // 1111
        0x5,     // 0101
        0xc,     // 1100
        0xc,     // 1100
        0x5,     // 0101
        0xf,     // 1111
        0x6,     // 0110
        0xa,     // 1010
        0x3,     // 0011
        0x9,     // 1001
        16	     // 1111
	)

	/**  */
	val segmentTable = Array[Array[Int]] (
		Array[Int](-1,-1,-1,-1,-1),		// 0 0000
        Array[Int]( 0, 3,-1,-1,-1),		// 1 0001
        Array[Int]( 1, 0,-1,-1,-1),		// 2 0010
        Array[Int]( 1, 3,-1,-1,-1),		// 3 0011
        Array[Int]( 2, 1,-1,-1,-1),		// 4 0100
        Array[Int]( 2, 1, 0, 3,-1),		// 5 0101
        Array[Int]( 2, 0,-1,-1,-1),		// 6 0110
        Array[Int]( 2, 3,-1,-1,-1),		// 7 0111
        Array[Int]( 3, 2,-1,-1,-1),		// 8 1000
        Array[Int]( 0, 2,-1,-1,-1),		// 9 1001
        Array[Int]( 1, 0, 3, 2,-1),		// A 1010
        Array[Int]( 1, 2,-1,-1,-1),		// B 1011
        Array[Int]( 3, 1,-1,-1,-1),		// C 1100
        Array[Int]( 0, 1,-1,-1,-1),		// D 1101
    	Array[Int]( 3, 0,-1,-1,-1),		// E 1110
        Array[Int](-1,-1,-1,-1,-1)		// F 1111
	)

	/** Triangles. Numbers less than 10 are segments. Number greater or
	  * equal to 10 are points index minus 10. */
	val triangleTable = Array[Array[Int]] (
		Array[Int](-1,-1,-1, -1,-1,-1, -1,-1,-1, -1,-1,-1, -1),	// 0000 0
		
		Array[Int]( 0,12,11,  0, 3,12,  3,13,12, -1,-1,-1, -1),	// 1110 10
		Array[Int](10,13, 0,  0,13, 1,  1,13,12, -1,-1,-1, -1),	// 1101	11	
		Array[Int]( 3,13, 1,  1,13,12, -1,-1,-1, -1,-1,-1, -1),	// 1100 7
		Array[Int](10, 1,11, 10, 2, 1, 10,13, 2, -1,-1,-1, -1),	// 1011 12

		Array[Int](10, 3, 0,  1, 2,12, -1,        0, 3, 1,  1, 3, 2, -1),	// 0101 14
		Array[Int](10, 2, 0, 10,13, 2, -1,-1,-1, -1,-1,-1, -1),	// 1001 8
		Array[Int]( 2, 3,13, -1,-1,-1, -1,-1,-1, -1,-1,-1, -1),	// 1000 4
		Array[Int]( 3,11,10,  3, 2,11,  2,12,11, -1,-1,-1, -1),	// 0111 9

		Array[Int]( 0,12,11,  0, 2,12, -1,-1,-1, -1,-1,-1, -1),	// 0110	6
		Array[Int]( 3,13, 2,  0, 1,11, -1,        0, 3, 2,  0, 2, 1, -1),	// 1010 13
		Array[Int]( 1, 2,12, -1,-1,-1, -1,-1,-1, -1,-1,-1, -1),	// 0100 3
		Array[Int]( 3,11,10,  3, 1,11, -1,-1,-1, -1,-1,-1, -1),	// 0011	5

		Array[Int]( 0, 1,11, -1,-1,-1, -1,-1,-1, -1,-1,-1, -1),	// 0010 2
		Array[Int]( 3, 0,10, -1,-1,-1, -1,-1,-1, -1,-1,-1, -1),	// 0001 1
		
		Array[Int](10,13,11, 11,13,12, -1,-1,-1, -1,-1,-1, -1)	// 1111 15
	)
}