package org.sofa.opengl.avatar.renderer

import scala.collection.mutable.{HashMap}

import org.sofa.math.{Vector3, NumberSeq3}
import org.sofa.collection.SpatialCube
import org.sofa.opengl.{Texture, ShaderProgram}
import org.sofa.opengl.mesh.{PlaneMesh, VertexAttribute}

/** Specific 2D avatar. */
abstract class Sprite(name:String, screen:Screen) extends Avatar(name, screen) {}