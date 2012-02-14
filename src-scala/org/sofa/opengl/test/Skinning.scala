package org.sofa.opengl.test

import scala.math._
import java.awt.Color

import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.nio._
import org.sofa.math._
import org.sofa.opengl._
import org.sofa.opengl.mesh._

import GL._
import GL2._
import GL2ES2._
import GL2GL3._
import GL3._ 

object Skinning {
    def main(args:Array[String]):Unit = (new Skinning).show
}

class Skinning {
    var gl:SGL = null
    
    val projection:Matrix4 = Matrix4()
    val modelview = MatrixStack(Matrix4())
    
	def show() {
	    
	}
}