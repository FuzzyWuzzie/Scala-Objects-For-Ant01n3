package org.sofa.gfx.io.collada

import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeSeq
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import org.sofa.math.Rgba
import org.sofa.math.Vector3
import org.sofa.gfx.mesh.EditableMesh
import org.sofa.gfx.mesh.Mesh
import scala.collection.mutable.HashSet

object ColladaInput { def main(args:Array[String]) { (new ColladaInput).test } }

class ColladaInput {
	def test() {
		ColladaFile.path += "/Users/antoine/Documents/Art/Sculptures/Blender/"

		val file = ColladaFile("Suzanne.dae")
		
		println(file)
		println(file.library.geometry("Monkey").get.mesh.toMesh)
	}
}