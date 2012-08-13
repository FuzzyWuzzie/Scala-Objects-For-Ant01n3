package org.sofa.opengl.io.collada

import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeSeq
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import org.sofa.math.Rgba
import org.sofa.math.Vector3
import org.sofa.opengl.mesh.EditableMesh
import org.sofa.opengl.mesh.Mesh
import scala.collection.mutable.HashSet
import org.sofa.opengl.mesh.MeshDrawMode

object ColladaInput { def main(args:Array[String]) { (new ColladaInput).test } }

class ColladaInput {
	def test() {
		process(scala.xml.XML.loadFile("/Users/antoine/Desktop/Suzanne.dae").child)
		process(scala.xml.XML.loadFile("/Users/antoine/Desktop/duck_triangulate.dae").child)
	}
	
	def process(root:NodeSeq) {
		val file = new File(root)
		
		println(file)
		
		file.library.geometries.foreach { geometry =>
			println("  geometry %s".format(geometry._1))
			geometry._2.meshes.foreach { mesh =>
				println("    mesh %s".format(mesh._1))
				mesh._2.faces.toMesh
			}
		}
		
		println("----------------")
	}
}