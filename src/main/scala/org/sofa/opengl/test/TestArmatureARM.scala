package org.sofa.opengl.test

import org.sofa.Timer
import org.sofa.opengl.armature._

object TestArmatureARM extends App {
	final val ArmatureSVGFile = "/Users/antoine/Documents/Art/Images/Bruce_Art/Robot.svg"
	final val ArmatureARMFile = "/Users/antoine/Desktop/Robot.arm"

	(new TestArmatureARM()).run
}

class TestArmatureARM {
	import TestArmatureARM._

	def run() {
		val SVGLoader = new SVGArmatureLoader()
		val ARMLoader = new ARMArmatureLoader()

		Timer.timer.measure("SVG load") {
			SVGLoader.convertToARM("robot", ArmatureSVGFile, "Armature", 1.0, ArmatureARMFile)
		}
		Timer.timer.measure("ARM load") {
			ARMLoader.load("robot", "", "", ArmatureARMFile, "Armature", 1.0)
		}
		Timer.timer.printAvgs("SVG -> ARM")
	}
}