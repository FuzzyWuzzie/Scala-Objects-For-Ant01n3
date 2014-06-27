package org.sofa.opengl.mesh

import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.math.{Point3, Rgba, Vector3}
import scala.math._

/** A set of disjoint line segments. */
class LinesMesh(val count:Int) extends Mesh {
    protected lazy val V:FloatBuffer = FloatBuffer(count * 3 * 2)

    protected lazy val C:FloatBuffer = FloatBuffer(count * 4 * 2)

    protected var cbeg = 0

    protected var cend = count

    protected var vbeg = 0

    protected var vend = count

    // -- Mesh Interface -----------------------------------------------
    
    def vertexCount:Int = count * 2

    override def attribute(name:String):FloatBuffer = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => V
    		case VertexAttribute.Color  => C
    		case _                      => super.attribute(name) //throw new RuntimeException("no %s attribute in this mesh".format(name))
    	}
    }

    override def attributeCount():Int = 2 + super.attributeCount

    override def attributes():Array[String] = Array[String](VertexAttribute.Vertex.toString, VertexAttribute.Color.toString) ++ super.attributes
        
    override def components(name:String):Int = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => 3
    		case VertexAttribute.Color  => 4
    		case _                      => super.components(name) //throw new RuntimeException("no %s attribute in this mesh".format(name))
    	}

    }

    override def has(name:String):Boolean = {
    	VertexAttribute.withName(name) match {
    		case VertexAttribute.Vertex => true
    		case VertexAttribute.Color  => true
    		case _                      => super.has(name)// false
    	}
    }

    def drawAs(gl:SGL):Int = gl.LINES

    // -- Edition -----------------------------------------------------

    /** Set the i-th line as the line between points (`x0`,`y0`,`z0`) and (`x1`,`y1`,`z1`). */
    def setLine(i:Int, x0:Float, y0:Float, z0:Float, x1:Float, y1:Float, z1:Float) {
        val pos = i*3*2
        
        V(pos+0) = x0
        V(pos+1) = y0
        V(pos+2) = z0
        V(pos+3) = x1
        V(pos+4) = y1
        V(pos+5) = z1

        if(i < vbeg) vbeg = i
        if(i+1 > vend) vend = i+1    	
    }

	/** Set the i-th line as the line between points `a` and `b`. */    
    def setLine(i:Int, a:Point3, b:Point3) {
    	setLine(i, a.x.toFloat, a.y.toFloat, a.z.toFloat, b.x.toFloat, b.y.toFloat, b.z.toFloat) }

    /** Set the i-th line as the line between points `p` and `p+v`. */
    def setLine(i:Int, p:Point3, v:Vector3) {
    	setLine(i, p.x.toFloat, p.y.toFloat, p.z.toFloat, (p.x+v.x).toFloat, (p.y+v.y).toFloat, (p.z+v.z).toFloat) }

    def setColor(i:Int, c:Rgba) { setColor(i, c, c) }

    def setColor(i:Int, c0:Rgba, c1:Rgba) {
        setColor(i, c0.red.toFloat, c0.green.toFloat, c0.blue.toFloat, c0.alpha.toFloat,
                    c1.red.toFloat, c1.green.toFloat, c1.blue.toFloat, c1.alpha.toFloat)
    }

    def setColor(i:Int, r:Float, g:Float, b:Float, a:Float) {
        setColor(i, r,g,b,a, r,g,b,a)
    }

    def setColor(i:Int, ra:Float, ga:Float, ba:Float, aa:Float,
                        rb:Float, gb:Float, bb:Float, ab:Float) {
        val pos = i*4*2

        C(pos+0) = ra
        C(pos+1) = ga
        C(pos+2) = ba
        C(pos+3) = aa

        C(pos+4) = rb
        C(pos+5) = gb
        C(pos+6) = bb
        C(pos+7) = ab

        if(i < cbeg) cbeg = i
        if(i+1 > cend) cend = i+1
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
        vbeg = count; vend = 0
	}

    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
      * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL) { updateVertexArray(gl, true, true) }
     
    /** Update the last vertex array created with newVertexArray(). Tries to update only what changed to
      * avoid moving data between the CPU and GPU. */
    def updateVertexArray(gl:SGL, updateVertices:Boolean, updateColors:Boolean) {
        if(va ne null) {
            if(vend > vbeg) {
                if(vbeg == 0 && vend == count)
                     va.buffer(VertexAttribute.Vertex.toString).update(V)
                else va.buffer(VertexAttribute.Vertex.toString).update(vbeg*2, vend*2, V)
                
                vbeg = count
                vend = 0
            }
            if(cend > cbeg) {
                if(cbeg == 0 && cend == count)
                     va.buffer(VertexAttribute.Color.toString).update(C)
                else va.buffer(VertexAttribute.Color.toString).update(cbeg*2, cend*2, C)
                
                cbeg = count
                cend = 0                
            }
        }
    }
}