package org.sofa.gfx.mesh

import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.math.{Point3, Rgba, Vector3}
import scala.math._

/** A set of `count` disjoint line segments. */
class LinesMesh(val gl:SGL, val count:Int) extends Mesh {

    protected var V:MeshAttribute = addAttributeVertex 

    protected var C:MeshAttribute = _ 

    // -- Mesh Interface -----------------------------------------------
    
    def vertexCount:Int = count * elementsPerPrimitive

    def elementsPerPrimitive:Int = 2

    def drawAs():Int = gl.LINES

    // -- Edition -----------------------------------------------------

    /** Represents a line of the mesh, an allows modification of this line, mainly position and color. */
    class Line(var line:Int) {
    	def ab(x0:Float, y0:Float, z0:Float, x1:Float, y1:Float, z1:Float):Line = { setLine(line, x0, y0, z0, x1, y1, z1); this }
    	def ab(p0:Point3, p1:Point3):Line = { setLine(line, p0, p1); this }
    	def ab(p:Point3, v:Vector3):Line = { setLine(line, p, v); this }
    	def rgb(r:Float, g:Float, b:Float):Line = { setColor(line, r, g, b, 1f); this }
    	def rgba(r:Float, g:Float, b:Float, a:Float):Line = { setColor(line, r, g, b, a); this }
    	def rgba(rgba:Rgba):Line = { setColor(line, rgba); this }
    }

    /** A dummy object to avoir reallocate it at each call to `line(i)`. */
    protected val line = new Line(-1)

    /** Access to the i-th line for modification. */
    def line(i:Int):Line = { line.line = i; line }

    /** Set the i-th line as the line between points (`x0`,`y0`,`z0`) and (`x1`,`y1`,`z1`). */
    def setLine(i:Int, x0:Float, y0:Float, z0:Float, x1:Float, y1:Float, z1:Float):LinesMesh = {
        val pos  = i * V.components * 2
        val data = V.data
        
        data(pos+0) = x0
        data(pos+1) = y0
        data(pos+2) = z0
        data(pos+3) = x1
        data(pos+4) = y1
        data(pos+5) = z1

        V.range(i*2, i*2 + 2)

        this
    }

	/** Set the i-th line as the line between points `a` and `b`. */    
    def setLine(i:Int, a:Point3, b:Point3):LinesMesh = {
    	setLine(i, a.x.toFloat, a.y.toFloat, a.z.toFloat, b.x.toFloat, b.y.toFloat, b.z.toFloat)
    	this
    }

    /** Set the i-th line as the line between points `p` and `p+v`. */
    def setLine(i:Int, p:Point3, v:Vector3):LinesMesh = {
    	setLine(i, p.x.toFloat, p.y.toFloat, p.z.toFloat, (p.x+v.x).toFloat, (p.y+v.y).toFloat, (p.z+v.z).toFloat)
    	this
    }

    /** Set the `color` of all lines. */
    def setColor(color:Rgba) {
    	val data = C.data
    	var i = 0
    	val n = vertexCount * C.components
    	val r = color.red.toFloat
    	val g = color.green.toFloat
    	val b = color.blue.toFloat
    	val a = color.alpha.toFloat

    	while(i < n) {
    		data(i+0) = r
    		data(i+1) = g
    		data(i+2) = b
    		data(i+3) = a

    		i+= 4
    	}

    	C.range(0, vertexCount)
    }

    /** Set the `color` of the `i`-th line. */
    def setColor(i:Int, color:Rgba):LinesMesh = { setColor(i, color, color); this }

    /** Set the color `c0` of the first end and `c1` of the other end of the `i`-th line. */
    def setColor(i:Int, c0:Rgba, c1:Rgba):LinesMesh = {
        setColor(i, c0.red.toFloat, c0.green.toFloat, c0.blue.toFloat, c0.alpha.toFloat,
                    c1.red.toFloat, c1.green.toFloat, c1.blue.toFloat, c1.alpha.toFloat)
        this
    }

    /** Set the color (`r`, `g`, `b`, `a`) of the `i`-th line. */
    def setColor(i:Int, r:Float, g:Float, b:Float, a:Float):LinesMesh = {
        setColor(i, r,g,b,a, r,g,b,a)
        this
    }

    /** Set the color (`ra`, `ga`, `ba`, `aa`) of the first end and (`rb`, `gb`, `bb`, `ab`)
      * of the other end of the `i`-th line. */
    def setColor(i:Int, ra:Float, ga:Float, ba:Float, aa:Float,
                        rb:Float, gb:Float, bb:Float, ab:Float):LinesMesh = {
    	if(C eq null) {
    		throw new NoSuchVertexAttributeException("no color attribute in the lines mesh, add it before")
    	}

        val pos  = i * C.components * 2
        val data = C.data

        data(pos+0) = ra
        data(pos+1) = ga
        data(pos+2) = ba
        data(pos+3) = aa

        data(pos+4) = rb
        data(pos+5) = gb
        data(pos+6) = bb
        data(pos+7) = ab

        C.range(i*2, i*2 + 2)

        this
    }

    /** Create an Horizontal ruler in the XY plane.
      *
      * The left bottom edge is positionned at (`xStart`, `yStart`), and graduations go from
      * `yStart` to `yStart`+`height`. Each `bigMarkers` graduation the graduation will have
      * 2*`height` and each `smallMarkers` graduation the graduation will have 1.5*`height`.
      * The `space` count the number of units between graduations. The `color` is the color
      * for all lines, excepted big and small graduations which are colored by `colorBig` and
      * `colorSmall` respectively. */
    def horizontalRuler(
    	xStart:Float, yStart:Float,
    	height:Float,
    	bigMarkers:Int, smallMarkers:Int,
    	space:Int,
    	color:Rgba, colorBig:Rgba, colorSmall:Rgba) {

		var i = 0
		var x = xStart
		val y = yStart

		while(i < count) {
			if(i % bigMarkers == 0) {
				setLine(i, x+0.5f, y, 0, x+0.5f, y+height*2f, 0)
				setColor(i, colorBig)
			} else if(i % smallMarkers == 0) {
				setLine(i, x+0.5f, y, 0, x+0.5f, y+height*1.5f, 0)
				setColor(i, colorSmall)
			} else {
				setLine(i, x+0.5f, y, 0, x+0.5f, y+height, 0)			
				setColor(i, color)
			}
			
			i += 1
			x += space
		}
	}

	/** Create a grid in the XY plane. See `setGrid()`. */
	def setXYGrid(
		w:Float,       h:Float,
		originx:Float, originy:Float, 
		countx:Int,    county:Int, 
		incrx:Float,   incry:Float, 
		color:Rgba, 
		xAxisColor:Rgba = Rgba.Red,
		yAxisColor:Rgba = Rgba.Green) {
		setColoredGrid(w, h, originx, originy, countx, county, incrx, incry, color, xAxisColor, yAxisColor, false)
	}

	/** Create a grid in the XZ plane. See `setGrid()`. */
	def setXZGrid(
		w:Float,       h:Float,
		originx:Float, originy:Float, 
		countx:Int,    county:Int, 
		incrx:Float,   incry:Float, 
		color:Rgba, 
		xAxisColor:Rgba = Rgba.Red,
		yAxisColor:Rgba = Rgba.Green) {
		setColoredGrid(w, h, originx, originy, countx, county, incrx, incry, color, xAxisColor, yAxisColor, true)
	}

	/** Create a grid in the XY plane.
	  * See `setGrid()`, here the color is changed for the X and Y axis independently. */
	def setColoredGrid(
		w:Float,       h:Float,
		originx:Float, originy:Float, 
		countx:Int,    county:Int, 
		incrx:Float,   incry:Float, 
		color:Rgba, 
		xAxisColor:Rgba = Rgba.Red,
		yAxisColor:Rgba = Rgba.Green,
		alongXZ:Boolean = false) {
		setGrid(
			w, h, originx, originy, countx, county, incrx, incry,
			{ x => if(x == countx/2) yAxisColor else color },
			{ y => if(y == county/2) xAxisColor else color },
			alongXZ
		)
	}

	/** Create a grid in the XY plane.
	  *
	  * `w` and `h` express the width of horizontal lines and height of vertical lines
	  * respectively. `originx` and `originy` express the position of the center of the grid.
	  * `countx` and `county` give the number of lines along X and Y. `incrx` and `incry`
	  * express the distance between each line segment along X and Y. Finally, `colorx`
	  * and `colory` gives functors that takes a line number as argument and return a color.
	  * They allow to choose which color to use for each grid line. Be careful, this is
	  * not the x or y value that is passed, but the increment (for precision reasons)
	  * between 0 and `countx` or `county`.
	  *
	  * By default, the grid is along the XY plane, passing `true` to the `alongXZ`
	  * argument will put the grid in the XZ plane.
	  *
	  * For example, to draw a grid of 10x10 cells, with therefore 11 lines along each 
	  * axis, with only the X and Y axes red and green, you could allocate a 
	  * `val lines = new LinesMesh(gl, 121)`, an can for example use :
	  * 
	  *     lines.setGrid(10, 10, 0, 0, 10, 10, 1, 1, 
	  *					{ x => if(x==5) Rgba.Red else Rgba.Grey50 },
	  *					{ y => if(y==5) Rgba.Green else Rgba.Grey50 },
	  *					false)
	  */
	def setGrid(
		w:Float,       h:Float,
		originx:Float, originy:Float, 
		countx:Int,    county:Int, 
		incrx:Float,   incry:Float, 
		colorx: (Int) => Rgba, 
		colory: (Int) => Rgba,
		alongXZ:Boolean = false) {

		var i = 0
		var x = originx - (incrx * (countx/2))
		var y = originy - (incry * (county/2))
		val ww = w / 2f
		val hh = h / 2f

		while(i <= countx) {
			if(alongXZ)
			     setLine(i, x, 0, originy - hh, x, 0, originy + hh)
			else setLine(i, x, originy - hh, 0, x, originy + hh, 0)

			setColor(i, colorx(i))

			x += incrx
			i += 1
		}

		while(i <= countx+county+1) {
			if(alongXZ)
			     setLine(i, originx - ww, 0, y, originx + ww, 0, y)
			else setLine(i, originx - ww, y, 0, originx + ww, y, 0)

			setColor(i, colory(i-countx-1))

			y += incry
			i += 1
		}
	}

	protected def addAttributeVertex():MeshAttribute = {
		if(V eq null)
			V = addMeshAttribute(VertexAttribute.Position, 3)

		V
	}

	/** Add a vertex attribute to store color in the mesh. Needed if you use `setXYGrid()` or
	  * `horizontalRuler()` */
	def addAttributeColor():MeshAttribute = {
		if(C eq null)
			C = addMeshAttribute(VertexAttribute.Color, 4)
		C
	}
}