package org.sofa.iopengl.actor.renderer.avatar.game

import scala.math._
import org.sofa.math._

object TestIsoValues extends App {

	// val colorUp    = (124, 212, 188)
	// val colorLeft  = ( 33,  74, 187)
	// val colorRight = (201,  74, 187)

	val colorUp    = (136, 234, 207)
	val colorLeft  = ( 38,  83, 207)
	val colorRight = (222,  83, 207)

	val isoRotation = Matrix4()
	
	val rotationx = acos(sqrt(2.0/3.0))

	isoRotation.setIdentity
	// println(s"iso =\n${isoRotation}")

	// isoRotation(1,1) = 0
	// isoRotation(1,2) = -1
	// isoRotation(2,1) = 1
	// isoRotation(2,2) = 0

	// println(s"iso =\n${isoRotation}")

	// coordonates are already passed from blender space to our space.
	// rotate around Y (our space) by 45 degrees to align Z so that it is perpendicular to the screen, going out.
	isoRotation.rotate(math.Pi/4.0, 0, 1, 0)
	// rotate arount X (our space) by approx. 35.2 degrees to align Y with (0,1,0) going up.
	isoRotation.rotate(-rotationx, 1, 0, 0)

	var iso = isoRotation.top3x3
	
	println(s"rot = ${toDegrees(rotationx)}")
	println(s"iso =\n${iso}")

	convertToNormal(colorUp,    "up    ")
	convertToNormal(colorLeft,  "left  ")
	convertToNormal(colorRight, "right ")

	def convertToNormal(src:(Int,Int,Int), id:String) {
		val c = Rgba(src._1, src._2, src._3)
		val n = c.toNormal; //n.fromBlender;
		val N = iso * n

		N.normalize
		//N.fromBlender

		printf("%s (%3d %3d %3d) -> %s ---> %s -> %s%n", id, src._1, src._2, src._3, c, n.toShortString, N.toShortString)
		//println(s"${id} (${src._1}, ${src._2}, ${src._3}) -> ${c} -> ${n} -> ${N}")
	}
}