package org.sofa.math

object Rgba {
    val black   = new Rgba(0, 0, 0, 1)
    val white   = new Rgba(1, 1, 1, 1)
    val red     = new Rgba(1, 0, 0, 1)
    val green   = new Rgba(0, 1, 0, 1)
    val blue    = new Rgba(0, 0, 1, 1)
    val cyan    = new Rgba(0, 1, 1, 1)
    val magenta = new Rgba(1, 0, 1, 1)
    val yellow  = new Rgba(1, 1, 0, 1)
    val grey10  = new Rgba(0.1, 0.1, 0.1, 1)
    val grey20  = new Rgba(0.2, 0.2, 0.2, 1)
    val grey30  = new Rgba(0.3, 0.3, 0.3, 1)
    val grey40  = new Rgba(0.4, 0.4, 0.4, 1)
    val grey50  = new Rgba(0.5, 0.5, 0.5, 1)
    val grey60  = new Rgba(0.6, 0.6, 0.6, 1)
    val grey70  = new Rgba(0.7, 0.7, 0.7, 1)
    val grey80  = new Rgba(0.8, 0.8, 0.8, 1)
    val grey90  = new Rgba(0.9, 0.9, 0.9, 1)

    def apply(from:NumberSeq4):Rgba = new Rgba(from.x, from.y, from.z, from.w)

    def apply(from:NumberSeq3):Rgba = new Rgba(from.x, from.y, from.z, 1)

    def apply(from:(Double,Double,Double,Double)) = new Rgba(from._1, from._2, from._3, from._4)

    def apply(from:(Double,Double,Double)) = new Rgba(from._1, from._2, from._3, 1)

    def apply(from:java.awt.Color):Rgba = new Rgba(from.getRed/255.0, from.getGreen/255.0, from.getBlue/255.0, from.getAlpha/255.0)
    
    def apply(r:Double, g:Double, b:Double, a:Double):Rgba = new Rgba(r, g, b, a)
    
    def apply(r:Double, g:Double, b:Double):Rgba = new Rgba(r, g, b, 1)

    def unapply(from:Rgba):(Double,Double,Double,Double) = from.rgba
}

/* This should maybe inherit NumberSeq4 ??! */

/** A simple color description made of four double numbers.
  * Meaningfull values are between 0 and 1, but no automatic clamping is done,
  * and larger values are allowed. */
class Rgba(
	var red:Double, 
	var green:Double,
	var blue:Double,
	var alpha:Double) {
	
	def * (factor:Double):Rgba = {
		new Rgba(red*factor, green*factor, blue*factor, alpha*factor)
	}

	def *= (factor:Double):Rgba = {
		red   *= factor
		green *= factor
		blue  *= factor
		this
	}

	def + (other:Rgba):Rgba = {
		new Rgba(red + other.red, green + other.green, blue + other.blue, alpha + other.alpha)
	}

	def += (other:Rgba):Rgba = {
		red   += other.red
		green += other.green
		blue  += other.blue
		alpha += other.alpha
		this
	}

	def rgb:(Double,Double,Double) = (red,green,blue)

	def rgb_=(rgb:(Double,Double,Double)) { red = rgb._1; green = rgb._2; blue = rgb._3 }

	def rgba:(Double,Double,Double,Double) = (red,green,blue,alpha)

	def rgba_=:(rgba:(Double,Double,Double,Double)) {  red = rgba._1; green = rgba._2; blue = rgba._3; alpha = rgba._4 }

	/** Ensure each component is between 0 and 1 inclusive. */
	def clamp() {
		if(red   > 1) red   = 1.0 else if(red   < 0) red   = 0.0
		if(green > 1) green = 1.0 else if(green < 0) green = 0.0
		if(blue  > 1) blue  = 1.0 else if(blue  < 0) blue  = 0.0
		if(alpha > 1) alpha = 1.0 else if(alpha < 0) alpha = 0.0
	}

	// def mixWith(other:Rgva, factor:Double) {
	//		
	// }

	override def toString():String = "RGBA[%f, %f, %f, %f]".format(red, green, blue, alpha)
}