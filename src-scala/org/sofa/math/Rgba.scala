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
    def apply(from:java.awt.Color):Rgba = new Rgba(from.getRed/255.0, from.getGreen/255.0, from.getBlue/255.0, from.getAlpha/255.0)
}

class Rgba(
	var red:Double, 
	var green:Double,
	var blue:Double,
	var alpha:Double) {
	
	
	
}