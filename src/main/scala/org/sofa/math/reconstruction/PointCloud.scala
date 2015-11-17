package org.sofa.math.reconstruction

import scala.math._
import scala.io.BufferedSource
import scala.collection.mutable.ArrayBuffer

import org.sofa.math.{Point3, Vector3, Rgba}

import java.io.{File, InputStream, FileInputStream, FileOutputStream, PrintStream, IOException}


object PointCloud {

	final val PointDesc = """\s*([0-9]+[\.,]?[0-9]*)\s+([0-9]+[\.,]?[0-9]*)\s+(-?[0-9]+[\.,]?[0-9]*)\s*""".r
	final val Empty     = """\s*""".r

	def apply(fileName:String, scaleFactor:Double, yFactor:Double):PointCloud = {
		if(fileName.endsWith(".xyz")) {
			readFileXYZ(fileName, new PointCloud(scaleFactor, yFactor))
		} else {
			throw new RuntimeException("only '.xyz' files are accepted")
		}
	}

	/** Created a [[HeightMap]] from a CSV or ASC file. */
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


class PointCloud(scaleFactor:Double, yFactor:Double) {
	val points = new ArrayBuffer[Point3]()

	val min = Point3(Double.MaxValue, Double.MaxValue, Double.MaxValue)

	var max = Point3(Double.MinValue, Double.MinValue, Double.MinValue)

	def size:Int = points.size

	def point(i:Int) = points(i)

	def apply(i:Int) = points(i)

	def addPoint(p:Point3) { addPoint(p.x, p.y, p.z) }

	def addPoint(x:Double, y:Double, z:Double) {
		points += Point3(x, y, z)

		if(x < min.x) min.x = x 
		if(x > max.x) max.x = x
		if(y < min.y) min.y = y
		if(y > max.y) max.y = y
		if(z < min.z) min.z = z
		if(z > max.z) max.z = z
	}

	def swapYZ() {
		points.foreach { p => p.set(p.x, p.z, p.y) }
	}

	def toOrigin() {
		points.foreach { p => p.set(p.x-min.x, p.y-min.y, p.z-min.z) }
		max.x -= min.x
		max.y -= min.y
		max.z -= min.z
		min.x = 0
		min.y = 0
		min.z = 0
	}

	def normalize(scale:Double) {
		toOrigin()
		val ratio = math.max(max.x, math.max(max.y, max.z))
		points.foreach { p => p.set((p.x/ratio)*scale, scale-((p.y/ratio)*scale), (p.z/ratio)*scale) }
		max.x = scale
		max.y = scale
		max.z = scale
	}

	def toObj(fileName:String) {
		val out = if(fileName eq null) System.out else new PrintStream(new FileOutputStream(fileName))
		points.foreach { p => out.print("v %f %f %f%n".format(p.x-min.x, p.y-min.y, p.z).replace(",", ".")) }
		out.flush
		out.close
	}
}