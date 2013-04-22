package org.sofa.simu.oberon.renderer

import scala.collection.mutable.{HashMap}

import org.sofa.math.{Vector3, NumberSeq3, SpatialCube}
import org.sofa.opengl.{Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, VertexAttribute}

/** Specific avatar that implements a clickable element. */
abstract class Sprite(name:String, screen:Screen) extends Avatar(name, screen) {}