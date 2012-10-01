package org.sofa.math

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

/** A triangle. */
class TriangleSimple(val p0:Point3, val p1:Point3, val p2:Point3) {}

/** Build a surface from an iso-surface provided by an evaluation function and an iso-level
  * parameter.
  * 
  * The technique used is the "marching cubes" algorithm, as described here:
  * http://en.wikipedia.org/wiki/Marching_cubes.
  * 
  * Most of the code and ideas for this come from the 
  * http://paulbourke.net/geometry/polygonise/  web page.
  * 
  * Lots of optimizations possible:
  * 	- Each cube share a face with another and therefore points are shared. However
  *       they are not merged in the triangle set returned. It could greatly improve
  *       the memory consumption and exchanges between cpu and gpu when drawing the surface.
  *     - The normals to the triangles are not given, and must be computed. This could be
  *       done automatically with less computation.
  *     - Knowing which points are shared, we could generate normals that smooth the surface.
  */
class IsoSurfaceSimple(val cellSize:Double) {
	
	/** Set of computed triangles. */
	val tri = new ArrayBuffer[TriangleSimple]()
	
	/** The triangles computed by the last calls to addCubesAT(). */
	def triangles:Seq[TriangleSimple] = tri
	
	/** Compute triangles faces from marching cubes in the area defined by the origin point
	  * (`xx`,`yy`,`zz`) and the number of cuves (in the three axe directions) provided
	  * by `count`. The `eval()` function allows to evaluate the iso-surface. A point is considered
	  * above the iso-surface if the `eval()` function returns a value greater than `isoLevel`. */
	def addCubesAt(xx:Double, yy:Double, zz:Double, count:Int, eval:(Point3)=>Double, isoLevel:Double) {
		for(z <- 0 until count) {
			for(y <- 0 until count) {
				for(x <- 0 until count) {
					addCubeAt(xx+x*cellSize, yy+y*cellSize, zz+z*cellSize, eval, isoLevel)
				}
			}
		}
	}
	
	/** Compute triangles faces from a single cube in the area defined by the origin point
	  * the `cellSize`. The `eval()` function allows to evaluate the iso-surface. A point is considered
	  * above the iso-surface if the `eval()` function returns a value greater than `isoLevel`. */
	def addCubeAt(xx:Double, yy:Double, zz:Double, eval:(Point3)=>Double, isoLevel:Double) {
		import IsoSurface._
		
		val xxx = xx+cellSize
		val yyy = yy+cellSize
		val zzz = zz+cellSize
		
		val p0 = Point3(xx,  yy,  zz )
		val p1 = Point3(xxx, yy,  zz )
		val p2 = Point3(xxx, yy,  zzz)
		val p3 = Point3(xx,  yy,  zzz)
		val p4 = Point3(xx,  yyy, zz )
		val p5 = Point3(xxx, yyy, zz )
		val p6 = Point3(xxx, yyy, zzz)
		val p7 = Point3(xx,  yyy, zzz)
		
		val v0 = eval(p0)
		val v1 = eval(p1)
		val v2 = eval(p2)
		val v3 = eval(p3)
		val v4 = eval(p4)
		val v5 = eval(p5)
		val v6 = eval(p6)
		val v7 = eval(p7)

		// Determine the index in the edge table which tells
		// us which vertices are inside of the surface.
		
		var cubeIndex = 0
		
		if(v0<isoLevel) cubeIndex |=   1 	// p0
		if(v1<isoLevel) cubeIndex |=   2	// p1
		if(v2<isoLevel) cubeIndex |=   4	// p2
		if(v3<isoLevel) cubeIndex |=   8	// p3
		if(v4<isoLevel) cubeIndex |=  16	// p4
		if(v5<isoLevel) cubeIndex |=  32	// p5
		if(v6<isoLevel) cubeIndex |=  64	// p6
		if(v7<isoLevel) cubeIndex |= 128	// p7
		
		var vertList = new Array[Point3](12)	// 12 possible vertices
		val idx = edgeTable(cubeIndex)
		
		if(idx != 0) {	// If the cube is not entirely out or in the surface.
			
			// Find the vertices where the surface intersects the cube.
			
			if((idx&   1) != 0) vertList( 0) = vertexInterp(isoLevel, p0, p1, v0, v1)
			if((idx&   2) != 0) vertList( 1) = vertexInterp(isoLevel, p1, p2, v1, v2)
			if((idx&   4) != 0) vertList( 2) = vertexInterp(isoLevel, p2, p3, v2, v3)
			if((idx&   8) != 0) vertList( 3) = vertexInterp(isoLevel, p3, p0, v3, v0)
			
			if((idx&  16) != 0) vertList( 4) = vertexInterp(isoLevel, p4, p5, v4, v5)
			if((idx&  32) != 0) vertList( 5) = vertexInterp(isoLevel, p5, p6, v5, v6)
			if((idx&  64) != 0) vertList( 6) = vertexInterp(isoLevel, p6, p7, v6, v7)
			if((idx& 128) != 0) vertList( 7) = vertexInterp(isoLevel, p7, p4, v7, v4)
			
			if((idx& 256) != 0) vertList( 8) = vertexInterp(isoLevel, p0, p4, v0, v4)
			if((idx& 512) != 0) vertList( 9) = vertexInterp(isoLevel, p1, p5, v1, v5)
			if((idx&1024) != 0) vertList(10) = vertexInterp(isoLevel, p2, p6, v2, v6)
			if((idx&2048) != 0) vertList(11) = vertexInterp(isoLevel, p3, p7, v3, v7)
			
			// Create the triangle.
			
			var i = 0
			
			while(triTable(cubeIndex)(i) != -1) {
				tri += new TriangleSimple(
						vertList(triTable(cubeIndex)(i)), 
						vertList(triTable(cubeIndex)(i+1)),
						vertList(triTable(cubeIndex)(i+2)))
				i += 3
			}
		}
	}
	
	/** Interpolate the point position along a vertex of a marching cube defined by points
	  * `p0` and `p1` using the values `v0` and `v1` for the iso-values at this two
	  * respective points. */
	protected def vertexInterp(isoLevel:Double, p0:Point3, p1:Point3, v0:Double, v1:Double):Point3 = {
		import math._
//		Point3(
//			p0.x + (p1.x-p0.x)/2,
//			p0.y + (p1.y-p0.y)/2,
//			p0.z + (p1.z-p0.z)/2)
		var p:Point3 = null
		
		if     (abs(isoLevel-v0) < 0.0001) { p = p0 }
		else if(abs(isoLevel-v1) < 0.0001) { p = p1 }
		else if(abs(v0-v1)       < 0.0001) { p = p0 }
		else {
			var mu = (isoLevel - v0) / (v1 - v0)
			p = Point3(
					p0.x + mu * (p1.x - p0.x),
					p0.y + mu * (p1.y - p0.y),
					p0.z + mu * (p1.z - p0.z))
		}
		
		p
	}
}

//-----------------------------------------------------------------------------------
//-----------------------------------------------------------------------------------
//-----------------------------------------------------------------------------------

class IsoCube(val index:Int, val pos:HashPoint3, val surface:IsoSurface) {

	/** Index in the points set of the surface of the points used to evaluate the iso-surface. */
	val points = new Array[Int](8)
	
	/** Index in the triPoints set of the surface of the points used to build triangles. */
	val triPoints = new Array[Int](12)
	
	/** The triangles inside this cube. */
	var triangles:ArrayBuffer[IsoTriangle] = null
	
	def this(index:Int, x:Int, y:Int, z:Int, surface:IsoSurface) { this(index,HashPoint3(x,y,z),surface) }
	
	override def toString():String = "cube[%s, {%s}]".format(pos, points.mkString(","))
	
	def isEmpty:Boolean = (triangles == null || triangles.isEmpty)
	
	def eval(nb:Array[IsoCube], eval:(Point3)=>Double, isoLevel:Double) {
		import IsoSurface._
		
		// For the height points, we look at neighbor cubes to find if they have
		// already been computed, to avoid a costly call to the eval() function.
		// If not, the cubePoint() method will add the point in the surface,
		// in this cube, and evaluate it and store this value in the surface.
		
		val p0 = cubePoint(0, nb, eval); val v0 = surface.values(p0)
		val p1 = cubePoint(1, nb, eval); val v1 = surface.values(p1)
		val p2 = cubePoint(2, nb, eval); val v2 = surface.values(p2)
		val p3 = cubePoint(3, nb, eval); val v3 = surface.values(p3)
		
		val p4 = cubePoint(4, nb, eval); val v4 = surface.values(p4)
		val p5 = cubePoint(5, nb, eval); val v5 = surface.values(p5)
		val p6 = cubePoint(6, nb, eval); val v6 = surface.values(p6)
		val p7 = cubePoint(7, nb, eval); val v7 = surface.values(p7)
		
		// We have the points and their values using the index returned in each pX and vX.
		// Now, determine the index in the edge table which tells us which vertices
		// are inside of the surface.
		
		var cubeIndex = 0
		
		if(v0<isoLevel) cubeIndex |=   1 	// p0
		if(v1<isoLevel) cubeIndex |=   2	// p1
		if(v2<isoLevel) cubeIndex |=   4	// p2
		if(v3<isoLevel) cubeIndex |=   8	// p3
		if(v4<isoLevel) cubeIndex |=  16	// p4
		if(v5<isoLevel) cubeIndex |=  32	// p5
		if(v6<isoLevel) cubeIndex |=  64	// p6
		if(v7<isoLevel) cubeIndex |= 128	// p7

		val idx = edgeTable(cubeIndex)
		
		if(idx != 0) {	// If the cube is not entirely out or in the surface.
			
			// Find the vertices where the surface intersects the cube.
			
			if((idx&   1) != 0) triPoints( 0) = vertexInterp(isoLevel, 0, p0, p1, v0, v1, nb)
			if((idx&   2) != 0) triPoints( 1) = vertexInterp(isoLevel, 1, p1, p2, v1, v2, nb)
			if((idx&   4) != 0) triPoints( 2) = vertexInterp(isoLevel, 2, p2, p3, v2, v3, nb)
			if((idx&   8) != 0) triPoints( 3) = vertexInterp(isoLevel, 3, p3, p0, v3, v0, nb)
			
			if((idx&  16) != 0) triPoints( 4) = vertexInterp(isoLevel, 4, p4, p5, v4, v5, nb)
			if((idx&  32) != 0) triPoints( 5) = vertexInterp(isoLevel, 5, p5, p6, v5, v6, nb)
			if((idx&  64) != 0) triPoints( 6) = vertexInterp(isoLevel, 6, p6, p7, v6, v7, nb)
			if((idx& 128) != 0) triPoints( 7) = vertexInterp(isoLevel, 7, p7, p4, v7, v4, nb)
			
			if((idx& 256) != 0) triPoints( 8) = vertexInterp(isoLevel, 8, p0, p4, v0, v4, nb)
			if((idx& 512) != 0) triPoints( 9) = vertexInterp(isoLevel, 9, p1, p5, v1, v5, nb)
			if((idx&1024) != 0) triPoints(10) = vertexInterp(isoLevel, 10, p2, p6, v2, v6, nb)
			if((idx&2048) != 0) triPoints(11) = vertexInterp(isoLevel, 11, p3, p7, v3, v7, nb)
			
			// Create the triangle.
			
			var i = 0
			
			while(triTable(cubeIndex)(i) != -1) {
				var a = triPoints(triTable(cubeIndex)(i))
				var b = triPoints(triTable(cubeIndex)(i+1))
				var c = triPoints(triTable(cubeIndex)(i+2))
				
				// Create the triangle
				
				if(triangles eq null) triangles = new ArrayBuffer[IsoTriangle]()
				
				val j = triangles.size
				triangles += new IsoTriangle(a, b, c)
				
				surface.triangleCount += 1

				if(surface.autoNormals) {
					// Reference the triangle in the points to triangles set.
					// XXX We may probably use a simple array, and not a hash set here XXX
				
					if(surface.pointsTri(a) eq null) surface.pointsTri(a) = new HashSet[(Int,Int)]()
					if(surface.pointsTri(b) eq null) surface.pointsTri(b) = new HashSet[(Int,Int)]()
					if(surface.pointsTri(c) eq null) surface.pointsTri(c) = new HashSet[(Int,Int)]()
				
					surface.pointsTri(a) += ((index, j))
					surface.pointsTri(b) += ((index, j))
					surface.pointsTri(c) += ((index, j))
				}
				i += 3
			}
		}
	}
	
	protected def cubePoint(p:Int, nb:Array[IsoCube], eval:(Point3)=>Double):Int = {
		import IsoSurface._
		
		val neighbor = pointOverlap(p)
		
		if(nb(neighbor(0)._1) ne null) nb(neighbor(0)._1).points(neighbor(0)._2) else
		if(nb(neighbor(1)._1) ne null) nb(neighbor(1)._1).points(neighbor(1)._2) else
		if(nb(neighbor(2)._1) ne null) nb(neighbor(2)._1).points(neighbor(2)._2) else
		if(nb(neighbor(3)._1) ne null) nb(neighbor(3)._1).points(neighbor(3)._2) else
		if(nb(neighbor(4)._1) ne null) nb(neighbor(4)._1).points(neighbor(4)._2) else
		if(nb(neighbor(5)._1) ne null) nb(neighbor(5)._1).points(neighbor(5)._2) else
		if(nb(neighbor(6)._1) ne null) nb(neighbor(6)._1).points(neighbor(6)._2) else {
			val i = surface.points.size
			val np = Point3((pos.x+cubeCoos(p)._1)*surface.cellSize,
					        (pos.y+cubeCoos(p)._2)*surface.cellSize,
					        (pos.z+cubeCoos(p)._3)*surface.cellSize)
			surface.points += np
			surface.values += eval(surface.points(i))
			points(p) = i
			
			assert(surface.points.size == surface.values.size)
			
			i
		}
	} 
	
	/** Interpolate the point position along a vertex of a marching cube defined by points
	  * `p0` and `p1` using the values `v0` and `v1` for the iso-values at this two
	  * respective points. */
	protected def vertexInterp(isoLevel:Double, edge:Int, p0:Int, p1:Int, v0:Double, v1:Double, nb:Array[IsoCube]):Int = {
		import math._
		
		// Find the neighbor cubes that may share the edge we evaluate.
		
		var i = cubeEdge(edge, nb)
		
		// If not found evaluate the edge, add the point.
		
		if(i < 0) {
			var p:Point3 = null
			val P0 = surface.points(p0)
			val P1 = surface.points(p1)

			if(IsoSurface.interpolation) {
				// Interpolated on the edge.
				
				if     (abs(isoLevel-v0) < 0.0001) { p = P0 }
				else if(abs(isoLevel-v1) < 0.0001) { p = P1 }
				else if(abs(v0-v1)       < 0.0001) { p = P0 }
				else {
					var mu = (isoLevel - v0) / (v1 - v0)
					p = Point3(
						P0.x + mu * (P1.x - P0.x),
						P0.y + mu * (P1.y - P0.y),
						P0.z + mu * (P1.z - P0.z))
				}
			} else {
				// In the middle of the edge.
				
				p = Point3(
					P0.x + 0.5 * (P1.x-P0.x),
					P0.y + 0.5 * (P1.y-P0.y),
					P0.z + 0.5 * (P1.z-P0.z))
			}
			
			// Add the point.
			
			i = surface.triPoints.size
			surface.triPoints += p
			surface.pointsTri += null

			assert(surface.triPoints.size == surface.pointsTri.size)
			
			if(surface.autoNormals) {
				surface.normals   += null
			
				assert(surface.triPoints.size == surface.normals.size)
			}			
		}
		
		i
	}
	
	protected def cubeEdge(edge:Int, nb:Array[IsoCube]):Int = {
		import IsoSurface._
		
		if(nb(edgeOverlap(edge)(0)._1) ne null) nb(edgeOverlap(edge)(0)._1).triPoints(edgeOverlap(edge)(0)._2) else
		if(nb(edgeOverlap(edge)(0)._1) ne null) nb(edgeOverlap(edge)(0)._1).triPoints(edgeOverlap(edge)(0)._2) else
		if(nb(edgeOverlap(edge)(0)._1) ne null) nb(edgeOverlap(edge)(0)._1).triPoints(edgeOverlap(edge)(0)._2) else {
			-1			
		}
	}
}

class IsoTriangle(val a:Int, val b:Int, val c:Int) {
	var normal:Vector3 = null
	
	def getNormal(surface:IsoSurface):Vector3 = {
		if(normal eq null) {
			val p0 = surface.triPoints(a)
			val p1 = surface.triPoints(b)
			val p2 = surface.triPoints(c)
			
			val v0 = Vector3(p0, p1)
			val v1 = Vector3(p0, p2)
			
			normal = v1 X v0
			normal.normalize
		}
		
		normal
	}
	
	override def toString():String = "tri[%d, %d, %d]".format(a, b, c)
}

class IsoSurface(val cellSize:Double) {
	var autoNormals = true
	
	/** Set of evaluation points, where the iso-surface values are taken. */
	val points = new ArrayBuffer[Point3]()
	
	/** Values of the iso-surface, each value correspond to a point in `points`. */
	val values = new ArrayBuffer[Double]()
	
	/** Set of interpolated triangle points, the points forming the triangles. */
	val triPoints = new ArrayBuffer[Point3]()
	
	/** For each point in `triPoints` this lists the pairs (cube,triangle) connected to this point. */
	val pointsTri = new ArrayBuffer[HashSet[(Int,Int)]]()
	
	/** A normal for each point in `triPoints`. */
	val normals = new ArrayBuffer[Vector3]()
	
	/** The set of cubes used to evaluate the surface. */
	val cubes = new ArrayBuffer[IsoCube]()
	
	/** List of non-empty cubes. */
	val nonEmptyCubes = new ArrayBuffer[IsoCube]()
	
	/** The hash map of cubes indexed by their position in integer space. */
	val spaceHash = new HashMap[HashPoint3,IsoCube]()
	
	/** Number of triangles computed by adding cubes. */
	var triangleCount = 0
	
	def autoComputeNormals(on:Boolean) {
		autoNormals = on
	}
	
	def addCubesAt(x:Int, y:Int, z:Int, countX:Int, countY:Int, countZ:Int, eval:(Point3)=>Double, isoLevel:Double) {
		var zz = 0
		var yy = 0
		var xx = 0
		
//Console.err.println("adding cubes at (%d,%d,%d) count (%d,%d,%d):".format(x,y,z, countX, countY, countZ))
		while(zz < countZ) {
			yy = 0
			while(yy < countY) {
				xx = 0
				while(xx < countX) {
//Console.err.print("  + (%d,%d,%d)".format(x+xx, y+yy, z+zz) )
					val cube = addCubeAt(x+xx, y+yy, z+zz, eval, isoLevel)
//Console.err.println(" OK => %s".format(if(cube.isEmpty)"empty" else "%d triangles".format(cube.triangles.size)))
					xx += 1
				}
				yy += 1
			}
			zz += 1
		}
		
//		for(zz<-0 until countZ) {
//			for(yy<-0 until countY) {
//				for(xx<-0 until countX) {
//					addCubeAt(x+xx, y+yy, z+zz, eval, isoLevel)
//				}
//			}
//		}
	}
	
	def computeNormals() {
		if(autoNormals) {
			var i = 0
		
			while(i < pointsTri.length) {
				val normal = Vector3(0,0,0)
			
				pointsTri(i).foreach { item =>  
					val triangle = cubes(item._1).triangles(item._2)
					normal += triangle.getNormal(this)
				}
			
				normal.normalize
				normals(i) = normal
			
				i += 1
			}
		}
	}
	
	def nearestCubePos(x:Double, y:Double, z:Double):HashPoint3 = {
		var xx = x/cellSize
		var yy = y/cellSize
		var zz = z/cellSize
		
//		if(xx < 0) xx -= 1
//		if(yy < 0) yy -= 1
//		if(zz < 0) zz -= 1
		
		HashPoint3(xx.toInt, yy.toInt, zz.toInt)
	}
	
	def addCubeAt(x:Int, y:Int, z:Int, eval:(Point3)=>Double, isoLevel:Double):IsoCube = {
		val p = HashPoint3(x,y,z)
//if(spaceHash.contains(p)) Console.err.print (" => already present") else Console.err.print(" => ## creating ##")
		spaceHash.get(p).getOrElse {
			val i = cubes.size
			var cube = new IsoCube(i, p, this)
			
			// Find the potential 26 surrounding cubes
			var neighbors = findNeighborCubes(p)
			
			cube.eval(neighbors, eval, isoLevel)
			
			spaceHash += ((p, cube))
			cubes += cube
			
			if(!cube.isEmpty) {
				nonEmptyCubes += cube
			}
			
			cube
		}
	}
	
	protected val nbCb = new Array[IsoCube](27)
	
	protected def findNeighborCubes(p:HashPoint3):Array[IsoCube] = {
		import IsoSurface._
		var i = 0
		var nn = 0

		while(i < neighborCubes.length) {
			if(i != 13) {
				val n = neighborCubes(i)
				val pp= HashPoint3(p.x+n._1, p.y+n._2, p.z+n._3)
				nbCb(i) = spaceHash.get(pp).getOrElse(null)
				if(nbCb(i) ne null) nn += 1
			} else {
				nbCb(i) = null
			}
			i += 1
		}
		nbCb
	}
}

object IsoSurface {

	val interpolation = true
	
	/** Coordinates of each cube point in the cube as multiples of cellSize. */
	val cubeCoos = Array[Tuple3[Int,Int,Int]] (
			(0,0,0),	// 0
			(1,0,0),	// 1
			(1,0,1),	// 2
			(0,0,1),	// 3
			(0,1,0),	// 4
			(1,1,0),	// 5
			(1,1,1),	// 6
			(0,1,1)		// 7
	)
	
	/** Fasten the search for neighbors. */
	val neighborCubes = Array[Tuple3[Int,Int,Int]] (
		(-1, -1, -1),		// 0
		( 0, -1, -1),		// 1
		( 1, -1, -1),		// 2
		(-1,  0, -1),		// 3
		( 0,  0, -1),		// 4
		( 1,  0, -1),		// 5
		(-1,  1, -1),		// 6
		( 0,  1, -1),		// 7
		( 1,  1, -1),		// 8
		//--------------------------
		(-1, -1,  0),		// 9 
		( 0, -1,  0),		// 10
		( 1, -1,  0),		// 11
		(-1,  0,  0),		// 12
		( 0,  0,  0),		// 13 XXX center
		( 1,  0,  0),		// 14
		(-1,  1,  0),		// 15
		( 0,  1,  0),		// 16
		( 1,  1,  0),		// 17
		//--------------------------
		(-1, -1,  1),		// 18
		( 0, -1,  1),		// 19
		( 1, -1,  1),		// 20
		(-1,  0,  1),		// 21
		( 0,  0,  1),		// 22
		( 1,  0,  1),		// 23
		(-1,  1,  1),		// 24
		( 0,  1,  1),		// 25
		( 1,  1,  1)		// 26
	)
	
	/** Fasten the search for overlapping points of a given point for a cube.
	  * Suppose you search the points of neighbor cubes that overlap point 0.
	  * You look at the first cell of this array and find an array of 2-tuples.
	  * Each of these tuples points at a neighbor cube, then at the point in this
	  * neighbor cube that is overlapping point 0. */
	val pointOverlap = Array[Array[Tuple2[Int,Int]]](
		Array[Tuple2[Int,Int]]( ( 0, 6), ( 1, 7), ( 3, 2), ( 4, 3), ( 9, 5), (10, 4), (12, 1) ),	// p0
		Array[Tuple2[Int,Int]]( ( 1, 6), ( 2, 7), ( 4, 2), ( 5, 3), (10, 5), (11, 4), (14, 0) ),	// p1
		Array[Tuple2[Int,Int]]( (10, 6), (11, 7), (14, 3), (19, 5), (20, 4), (22, 1), (23, 0) ),	// p2
		Array[Tuple2[Int,Int]]( ( 9, 6), (10, 7), (12, 2), (18, 5), (19, 4), (21, 1), (22, 0) ),	// p3
		Array[Tuple2[Int,Int]]( ( 3, 6), ( 4, 7), ( 6, 2), ( 7, 3), (12, 5), (15, 1), (16, 0) ),	// p4
		Array[Tuple2[Int,Int]]( ( 4, 6), ( 5, 7), ( 7, 2), ( 8, 3), (14, 4), (16, 1), (17, 0) ),	// p5
		Array[Tuple2[Int,Int]]( (14, 7), (16, 2), (17, 3), (22, 5), (23, 4), (25, 1), (26, 0) ),	// p6
		Array[Tuple2[Int,Int]]( (12, 6), (15, 2), (16, 3), (21, 5), (22, 4), (24, 1), (25, 0) )		// p7
	)
	
	/** Fasten the search for overlapping edges of a given edge for a cube.
	  * Suppose you search the edges of neighbor cubes that overlap edge 0.
	  * You look a the first cell of this array and find an array of 2-tuples.
	  * Each of these tuples points at a neighbor cube, then at the edge in this
	  * neighbor cube that is overlapping edge 0. */
	val edgeOverlap = Array[Array[Tuple2[Int,Int]]](
		Array[Tuple2[Int,Int]]( ( 1, 6), ( 4, 2), (10, 4)),	// e0
		Array[Tuple2[Int,Int]]( (10, 5), (11, 7), (14, 3)),	// e1
		Array[Tuple2[Int,Int]]( (10, 6), (19, 4), (22, 0)),	// e2
		Array[Tuple2[Int,Int]]( ( 9, 5), (10, 7), (12, 1)),	// e3
		Array[Tuple2[Int,Int]]( ( 4, 6), ( 7, 2), (16, 0)),	// e4
		Array[Tuple2[Int,Int]]( (14, 7), (16, 1), (17, 3)),	// e5
		Array[Tuple2[Int,Int]]( (16, 2), (22, 4), (25, 0)),	// e6
		Array[Tuple2[Int,Int]]( (12, 5), (15, 1), (16, 3)),	// e7
		Array[Tuple2[Int,Int]]( ( 3,10), ( 4,11), (12, 9)),	// e8
		Array[Tuple2[Int,Int]]( ( 4,10), ( 5,11), (14, 8)),	// e9
		Array[Tuple2[Int,Int]]( (14,11), (22, 9), (23, 8)),	// e10
		Array[Tuple2[Int,Int]]( (12,10), (21, 9), (22, 8))	// e11
	)
	
	val edgeTable = Array[Int] (
		0x0  , 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
		0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
		0x190, 0x99 , 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
		0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
		0x230, 0x339, 0x33 , 0x13a, 0x636, 0x73f, 0x435, 0x53c,
		0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
		0x3a0, 0x2a9, 0x1a3, 0xaa , 0x7a6, 0x6af, 0x5a5, 0x4ac,
		0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
		0x460, 0x569, 0x663, 0x76a, 0x66 , 0x16f, 0x265, 0x36c,
		0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
		0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0xff , 0x3f5, 0x2fc,
		0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
		0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x55 , 0x15c,
		0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
		0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0xcc ,
		0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
		0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
		0xcc , 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
		0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
		0x15c, 0x55 , 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
		0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
		0x2fc, 0x3f5, 0xff , 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
		0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
		0x36c, 0x265, 0x16f, 0x66 , 0x76a, 0x663, 0x569, 0x460,
		0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
		0x4ac, 0x5a5, 0x6af, 0x7a6, 0xaa , 0x1a3, 0x2a9, 0x3a0,
		0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
		0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x33 , 0x339, 0x230,
		0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
		0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x99 , 0x190,
		0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
		0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x0  )

	val triTable = Array[Array[Int]] (
		Array[Int](-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 8, 3, 9, 8, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 8, 3, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 2, 10, 0, 2, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](2, 8, 3, 2, 10, 8, 10, 9, 8, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 11, 2, 8, 11, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 9, 0, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 11, 2, 1, 9, 11, 9, 8, 11, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 10, 1, 11, 10, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 10, 1, 0, 8, 10, 8, 11, 10, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 9, 0, 3, 11, 9, 11, 10, 9, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 3, 0, 7, 3, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 1, 9, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 1, 9, 4, 7, 1, 7, 3, 1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 2, 10, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 4, 7, 3, 0, 4, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 2, 10, 9, 0, 2, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4, -1, -1, -1, -1),
		Array[Int](8, 4, 7, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](11, 4, 7, 11, 2, 4, 2, 0, 4, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 0, 1, 8, 4, 7, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1, -1, -1, -1, -1),
		Array[Int](3, 10, 1, 3, 11, 10, 7, 8, 4, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4, -1, -1, -1, -1),
		Array[Int](4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3, -1, -1, -1, -1),
		Array[Int](4, 7, 11, 4, 11, 9, 9, 11, 10, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 5, 4, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 5, 4, 1, 5, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](8, 5, 4, 8, 3, 5, 3, 1, 5, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 2, 10, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 0, 8, 1, 2, 10, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](5, 2, 10, 5, 4, 2, 4, 0, 2, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8, -1, -1, -1, -1),
		Array[Int](9, 5, 4, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 11, 2, 0, 8, 11, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 5, 4, 0, 1, 5, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5, -1, -1, -1, -1),
		Array[Int](10, 3, 11, 10, 1, 3, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10, -1, -1, -1, -1),
		Array[Int](5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3, -1, -1, -1, -1),
		Array[Int](5, 4, 8, 5, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 7, 8, 5, 7, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 3, 0, 9, 5, 3, 5, 7, 3, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 7, 8, 0, 1, 7, 1, 5, 7, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 7, 8, 9, 5, 7, 10, 1, 2, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3, -1, -1, -1, -1),
		Array[Int](8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2, -1, -1, -1, -1),
		Array[Int](2, 10, 5, 2, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](7, 9, 5, 7, 8, 9, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11, -1, -1, -1, -1),
		Array[Int](2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7, -1, -1, -1, -1),
		Array[Int](11, 2, 1, 11, 1, 7, 7, 1, 5, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11, -1, -1, -1, -1),
		Array[Int](5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0, -1),
		Array[Int](11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0, -1),
		Array[Int](11, 10, 5, 7, 11, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 8, 3, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 0, 1, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 8, 3, 1, 9, 8, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 6, 5, 2, 6, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 6, 5, 1, 2, 6, 3, 0, 8, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 6, 5, 9, 0, 6, 0, 2, 6, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8, -1, -1, -1, -1),
		Array[Int](2, 3, 11, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](11, 0, 8, 11, 2, 0, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 1, 9, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11, -1, -1, -1, -1),
		Array[Int](6, 3, 11, 6, 5, 3, 5, 1, 3, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6, -1, -1, -1, -1),
		Array[Int](3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9, -1, -1, -1, -1),
		Array[Int](6, 5, 9, 6, 9, 11, 11, 9, 8, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](5, 10, 6, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 3, 0, 4, 7, 3, 6, 5, 10, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 9, 0, 5, 10, 6, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4, -1, -1, -1, -1),
		Array[Int](6, 1, 2, 6, 5, 1, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7, -1, -1, -1, -1),
		Array[Int](8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6, -1, -1, -1, -1),
		Array[Int](7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9, -1),
		Array[Int](3, 11, 2, 7, 8, 4, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11, -1, -1, -1, -1),
		Array[Int](0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1),
		Array[Int](9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6, -1),
		Array[Int](8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6, -1, -1, -1, -1),
		Array[Int](5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11, -1),
		Array[Int](0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7, -1),
		Array[Int](6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9, -1, -1, -1, -1),
		Array[Int](10, 4, 9, 6, 4, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 10, 6, 4, 9, 10, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](10, 0, 1, 10, 6, 0, 6, 4, 0, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10, -1, -1, -1, -1),
		Array[Int](1, 4, 9, 1, 2, 4, 2, 6, 4, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4, -1, -1, -1, -1),
		Array[Int](0, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](8, 3, 2, 8, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](10, 4, 9, 10, 6, 4, 11, 2, 3, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6, -1, -1, -1, -1),
		Array[Int](3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10, -1, -1, -1, -1),
		Array[Int](6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1, -1),
		Array[Int](9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3, -1, -1, -1, -1),
		Array[Int](8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1, -1),
		Array[Int](3, 11, 6, 3, 6, 0, 0, 6, 4, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](6, 4, 8, 11, 6, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](7, 10, 6, 7, 8, 10, 8, 9, 10, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10, -1, -1, -1, -1),
		Array[Int](10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0, -1, -1, -1, -1),
		Array[Int](10, 6, 7, 10, 7, 1, 1, 7, 3, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7, -1, -1, -1, -1),
		Array[Int](2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9, -1),
		Array[Int](7, 8, 0, 7, 0, 6, 6, 0, 2, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](7, 3, 2, 6, 7, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7, -1, -1, -1, -1),
		Array[Int](2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7, -1),
		Array[Int](1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11, -1),
		Array[Int](11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1, -1, -1, -1, -1),
		Array[Int](8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6, -1),
		Array[Int](0, 9, 1, 11, 6, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0, -1, -1, -1, -1),
		Array[Int](7, 11, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 0, 8, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 1, 9, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](8, 1, 9, 8, 3, 1, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](10, 1, 2, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 2, 10, 3, 0, 8, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](2, 9, 0, 2, 10, 9, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8, -1, -1, -1, -1),
		Array[Int](7, 2, 3, 6, 2, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](7, 0, 8, 7, 6, 0, 6, 2, 0, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](2, 7, 6, 2, 3, 7, 0, 1, 9, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6, -1, -1, -1, -1),
		Array[Int](10, 7, 6, 10, 1, 7, 1, 3, 7, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8, -1, -1, -1, -1),
		Array[Int](0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7, -1, -1, -1, -1),
		Array[Int](7, 6, 10, 7, 10, 8, 8, 10, 9, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](6, 8, 4, 11, 8, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 6, 11, 3, 0, 6, 0, 4, 6, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](8, 6, 11, 8, 4, 6, 9, 0, 1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6, -1, -1, -1, -1),
		Array[Int](6, 8, 4, 6, 11, 8, 2, 10, 1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6, -1, -1, -1, -1),
		Array[Int](4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9, -1, -1, -1, -1),
		Array[Int](10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3, -1),
		Array[Int](8, 2, 3, 8, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8, -1, -1, -1, -1),
		Array[Int](1, 9, 4, 1, 4, 2, 2, 4, 6, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1, -1, -1, -1, -1),
		Array[Int](10, 1, 0, 10, 0, 6, 6, 0, 4, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3, -1),
		Array[Int](10, 9, 4, 6, 10, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 9, 5, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 8, 3, 4, 9, 5, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](5, 0, 1, 5, 4, 0, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5, -1, -1, -1, -1),
		Array[Int](9, 5, 4, 10, 1, 2, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5, -1, -1, -1, -1),
		Array[Int](7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2, -1, -1, -1, -1),
		Array[Int](3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6, -1),
		Array[Int](7, 2, 3, 7, 6, 2, 5, 4, 9, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7, -1, -1, -1, -1),
		Array[Int](3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0, -1, -1, -1, -1),
		Array[Int](6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8, -1),
		Array[Int](9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7, -1, -1, -1, -1),
		Array[Int](1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4, -1),
		Array[Int](4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10, -1),
		Array[Int](7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10, -1, -1, -1, -1),
		Array[Int](6, 9, 5, 6, 11, 9, 11, 8, 9, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5, -1, -1, -1, -1),
		Array[Int](0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11, -1, -1, -1, -1),
		Array[Int](6, 11, 3, 6, 3, 5, 5, 3, 1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6, -1, -1, -1, -1),
		Array[Int](0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10, -1),
		Array[Int](11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5, -1),
		Array[Int](6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3, -1, -1, -1, -1),
		Array[Int](5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2, -1, -1, -1, -1),
		Array[Int](9, 5, 6, 9, 6, 0, 0, 6, 2, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8, -1),
		Array[Int](1, 5, 6, 2, 1, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6, -1),
		Array[Int](10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0, -1, -1, -1, -1),
		Array[Int](0, 3, 8, 5, 6, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](10, 5, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](11, 5, 10, 7, 5, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](11, 5, 10, 11, 7, 5, 8, 3, 0, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](5, 11, 7, 5, 10, 11, 1, 9, 0, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1, -1, -1, -1, -1),
		Array[Int](11, 1, 2, 11, 7, 1, 7, 5, 1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11, -1, -1, -1, -1),
		Array[Int](9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7, -1, -1, -1, -1),
		Array[Int](7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2, -1),
		Array[Int](2, 5, 10, 2, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5, -1, -1, -1, -1),
		Array[Int](9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2, -1, -1, -1, -1),
		Array[Int](9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2, -1),
		Array[Int](1, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 8, 7, 0, 7, 1, 1, 7, 5, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 0, 3, 9, 3, 5, 5, 3, 7, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 8, 7, 5, 9, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](5, 8, 4, 5, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0, -1, -1, -1, -1),
		Array[Int](0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5, -1, -1, -1, -1),
		Array[Int](10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4, -1),
		Array[Int](2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8, -1, -1, -1, -1),
		Array[Int](0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11, -1),
		Array[Int](0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5, -1),
		Array[Int](9, 4, 5, 2, 11, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4, -1, -1, -1, -1),
		Array[Int](5, 10, 2, 5, 2, 4, 4, 2, 0, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9, -1),
		Array[Int](5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2, -1, -1, -1, -1),
		Array[Int](8, 4, 5, 8, 5, 3, 3, 5, 1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 4, 5, 1, 0, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5, -1, -1, -1, -1),
		Array[Int](9, 4, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 11, 7, 4, 9, 11, 9, 10, 11, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11, -1, -1, -1, -1),
		Array[Int](1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11, -1, -1, -1, -1),
		Array[Int](3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4, -1),
		Array[Int](4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2, -1, -1, -1, -1),
		Array[Int](9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3, -1),
		Array[Int](11, 7, 4, 11, 4, 2, 2, 4, 0, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4, -1, -1, -1, -1),
		Array[Int](2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9, -1, -1, -1, -1),
		Array[Int](9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7, -1),
		Array[Int](3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10, -1),
		Array[Int](1, 10, 2, 8, 7, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 9, 1, 4, 1, 7, 7, 1, 3, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1, -1, -1, -1, -1),
		Array[Int](4, 0, 3, 7, 4, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](4, 8, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 0, 9, 3, 9, 11, 11, 9, 10, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 1, 10, 0, 10, 8, 8, 10, 11, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 1, 10, 11, 3, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 2, 11, 1, 11, 9, 9, 11, 8, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9, -1, -1, -1, -1),
		Array[Int](0, 2, 11, 8, 0, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](3, 2, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](2, 3, 8, 2, 8, 10, 10, 8, 9, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](9, 10, 2, 0, 9, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8, -1, -1, -1, -1),
		Array[Int](1, 10, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](1, 3, 8, 9, 1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 9, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](0, 3, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
		Array[Int](-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1)
	)
}