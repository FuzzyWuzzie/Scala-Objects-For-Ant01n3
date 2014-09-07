package org.sofa.gfx.mesh

import org.sofa.nio._
import org.sofa.gfx._
import org.sofa.math.{Point3, Rgba, Vector3}
import scala.math._

/** A set of disjoint line segments. */
class LinesMesh(val count:Int) extends Mesh {
    protected[this] lazy val V:MeshAttribute = addMeshAttribute(VertexAttribute.Vertex, 3)// = FloatBuffer(count * 3 * 2)

    protected[this] lazy val C:MeshAttribute = addMeshAttribute(VertexAttribute.Color, 4)// = FloatBuffer(count * 4 * 2)

    // protected var cbeg = 0

    // protected var cend = count

    // protected var vbeg = 0

    // protected var vend = count

    // -- Mesh Interface -----------------------------------------------
    
    def vertexCount:Int = count * elementsPerPrimitive

    def elementsPerPrimitive:Int = 2

    def drawAs(gl:SGL):Int = gl.LINES

    // override def attribute(name:String):FloatBuffer = {
    // 	VertexAttribute.withName(name) match {
    // 		case VertexAttribute.Vertex => V
    // 		case VertexAttribute.Color  => C
    // 		case _                      => super.attribute(name) //throw new RuntimeException("no %s attribute in this mesh".format(name))
    // 	}
    // }

    // override def attributeCount():Int = 2 + super.attributeCount

    // override def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.Color.toString) ++ super.attributes
        
    // override def components(name:String):Int = {
    // 	VertexAttribute.withName(name) match {
    // 		case VertexAttribute.Vertex => 3
    // 		case VertexAttribute.Color  => 4
    // 		case _                      => super.components(name) //throw new RuntimeException("no %s attribute in this mesh".format(name))
    // 	}

    // }

    // override def has(name:String):Boolean = {
    // 	VertexAttribute.withName(name) match {
    // 		case VertexAttribute.Vertex => true
    // 		case VertexAttribute.Color  => true
    // 		case _                      => super.has(name)// false
    // 	}
    // }

    // -- Edition -----------------------------------------------------

    class Line(var line:Int) {
    	def ab(x0:Float, y0:Float, z0:Float, x1:Float, y1:Float, z1:Float):Line = { setLine(line, x0, y0, z0, x1, y1, z1); this }
    	def ab(p0:Point3, p1:Point3):Line = { setLine(line, p0, p1); this }
    	def ab(p:Point3, v:Vector3):Line = { setLine(line, p, v); this }
    	def rgba(r:Float, g:Float, b:Float, a:Float):Line = { setColor(line, r, g, b, a); this }
    	def rgb(r:Float, g:Float, b:Float):Line = { setColor(line, r, g, b, 1f); this }
    	def rgba(rgba:Rgba):Line = { setColor(line, rgba); this }
    }

    protected[this] val line = new Line(-1)

    def line(i:Int):Line = { line.line = i; line }

    /** Set the i-th line as the line between points (`x0`,`y0`,`z0`) and (`x1`,`y1`,`z1`). */
    def setLine(i:Int, x0:Float, y0:Float, z0:Float, x1:Float, y1:Float, z1:Float):LinesMesh = {
        val pos  = i * V.components * 2
        val data = V.theData
        
        data(pos+0) = x0
        data(pos+1) = y0
        data(pos+2) = z0
        data(pos+3) = x1
        data(pos+4) = y1
        data(pos+5) = z1

        if(i/2     < V.beg) V.beg = i/2
        if(i/2 + 2 > V.end) V.end = i/2 + 2

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

    def setColor(i:Int, c:Rgba):LinesMesh = { setColor(i, c, c); this }

    def setColor(i:Int, c0:Rgba, c1:Rgba):LinesMesh = {
        setColor(i, c0.red.toFloat, c0.green.toFloat, c0.blue.toFloat, c0.alpha.toFloat,
                    c1.red.toFloat, c1.green.toFloat, c1.blue.toFloat, c1.alpha.toFloat)
        this
    }

    def setColor(i:Int, r:Float, g:Float, b:Float, a:Float):LinesMesh = {
        setColor(i, r,g,b,a, r,g,b,a)
        this
    }

    def setColor(i:Int, ra:Float, ga:Float, ba:Float, aa:Float,
                        rb:Float, gb:Float, bb:Float, ab:Float):LinesMesh = {
        val pos  = i * C.components * 2
        val data = C.theData

        data(pos+0) = ra
        data(pos+1) = ga
        data(pos+2) = ba
        data(pos+3) = aa

        data(pos+4) = rb
        data(pos+5) = gb
        data(pos+6) = bb
        data(pos+7) = ab

        if(i/2     < C.beg) C.beg = i/2
        if(i/2 + 1 > C.end) C.end = i/2 + 1

        this
    }

    /** Create an Horizontal ruler.
      *
      * The left bottom edge is positionned at (`xStart`, `yStart`), and graduations go from `yStart` to `yStart`+`height`.
      * Each `bigMarkers` graduation the graduation will have 2*`height` and each `smallMarkers` graduation the
      * graduation will have 1.5*`height`. The `space` count the number of units between graduations. The `color` is the
      * color for all lines, excepted big and small graduations which are colored by `colorBig` and `colorSmall`
      * respectively. */
    def horizontalRuler(xStart:Float, yStart:Float, height:Float, bigMarkers:Int, smallMarkers:Int, space:Int, color:Rgba, colorBig:Rgba, colorSmall:Rgba) {
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

	def setXYGrid(w:Float, h:Float,
		originx:Float, originy:Float, 
		countx:Int, county:Int, 
		incrx:Float, incry:Float, 
		color:Rgba, xAxisColor:Rgba = Rgba.Red, yAxisColor:Rgba = Rgba.Green) {
		var i = 0
		var x = originx - (incrx * (countx/2))
		var y = originy - (incry * (county/2))

		while(i <= countx) {
			setLine(i, x, originy-h, 0, x, originy+h, 0)

			if(x > -0.0001 && x < 0.0001)
			     setColor(i, xAxisColor)
			else setColor(i, color)

			x += incrx
			i += 1
		}

		while(i <= countx+county+1) {
			setLine(i, originx-w, y, 0, originx+w, y, 0)

			if(y > -0.0001 && y < 0.0001)
			     setColor(i, yAxisColor)
			else setColor(i, color)

			y += incry
			i += 1
		}
	}

    // -- Dynamic mesh --------------------------------------------------

    override def beforeNewVertexArray() {
        if(has(VertexAttribute.Color))  C.resetMarkers
        if(has(VertexAttribute.Vertex)) V.resetMarkers
	}

    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
      * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true) }
     
    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
      * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL, updateVertices:Boolean=true, updateColors:Boolean=true) {
    	if(va ne null) {
    		if(updateVertices) V.update(va)
    		if(updateColors)   C.update(va)
    	}
        // if(va ne null) {
        //     if(vend > vbeg) {
        //         if(vbeg == 0 && vend == count)
        //              va.buffer(VertexAttribute.Vertex.toString).update(V)
        //         else va.buffer(VertexAttribute.Vertex.toString).update(vbeg*2, vend*2, V)
                
        //         vbeg = count
        //         vend = 0
        //     }
        //     if(cend > cbeg) {
        //         if(cbeg == 0 && cend == count)
        //              va.buffer(VertexAttribute.Color.toString).update(C)
        //         else va.buffer(VertexAttribute.Color.toString).update(cbeg*2, cend*2, C)
                
        //         cbeg = count
        //         cend = 0                
        //     }
        // }
    }

	/** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
	  * avoid moving data between the CPU and GPU. You may give a boolean for each buffer in the vertex array
	  * that you want to update or not. */
    def updateVertexArray(gl:SGL, attributes:String*) {
    	if(va ne null) {
    		attributes.foreach { meshAttribute(_).update(va) }
    	}
    }
}