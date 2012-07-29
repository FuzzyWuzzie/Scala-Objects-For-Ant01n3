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
    def apply(from:java.awt.Color):Rgba = new Rgba(from.getRed/255.0, from.getGreen/255.0, from.getBlue/255.0, from.getAlpha/255.0)
    def apply(r:Double, g:Double, b:Double, a:Double):Rgba = new Rgba(r, g, b, a)
    def apply(r:Double, g:Double, b:Double):Rgba = new Rgba(r, g, b, 1)
}

class Rgba(
	var red:Double, 
	var green:Double,
	var blue:Double,
	var alpha:Double) {
}