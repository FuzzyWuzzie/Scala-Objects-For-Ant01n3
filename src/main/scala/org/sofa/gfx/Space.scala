package org.sofa.gfx

import scala.math._

import org.sofa.math._
import org.sofa.math.Axis._

/** Define a stack of projection and model-view matrices.
  *
  * This mimicks the behavior of OpenGL 1 and 2 with matrices.
  * 
  * The space maintains a projection and a model-view matrix (stacks of it). The model-view matrix
  * allows to "move" the camera in space. The projection matrix allows to specify the camera viewing
  * properties like the aperture and type of projection. It also defines a far clipping plane,
  * so that objects too far away are not drawn.
  * 
  * It stores a view port in pixels that express the rendering canvas dimensions. This canvas also
  * allows to control the aspect-ratio of the camera.
  */
trait Space {
    /** How to pass from view space to camera space. */
    val projection = MatrixStack(Matrix4())
    
    /** How to pass from model to view space. */
    val modelview = MatrixStack(Matrix4())

    /** The top projection matrix times the top modelview matrix. We avoid to recompute or reallocate
      * it since it is used very often. Memory/speed compromise. */
    val mvp = new Matrix4()

    /** Inverse of the top-most mvp, computed only when needed. */
    protected var inverseMVP:Matrix4 = null

    /** Flag indicating if the modelview or projection changed since the last MVP compute. */
    protected var needRecomputeMVP = true
        
    /** View port size in pixels. */
    var viewportPx = Vector2(800, 600)
    
    /** Maximum depth of the view (far clip-plane position). */
    var maxDepth = 100.0

    /** Viewport width in pixels. */
    def viewportWidth:Double = viewportPx.x

    /** Change the viewport width in pixels. */
    def viewportWidth_= (value:Double):Unit = { viewportPx.x = value } 

    /** Viewport height in pixels. */
    def viewportHeight:Double = viewportPx.y

    /** Change the viewport height in pixels. */
    def viewportHeight_= (value:Double):Unit = { viewportPx.y = value }

    /** The viewport in pixels. */
    def viewport:(Double,Double) = (viewportPx.x, viewportPx.y)

    /** Change the viewport in pixels. */
    def viewport_=(values:(Double,Double)):Unit = { viewportPx.set(values._1, values._2) }
    
    /** Set the width and height of the output view-port in pixels. */
    def viewportPx(width:Double, height:Double) { viewportPx.set(width, height) }
    
    /** Obtain the current view-port width / height ratio. */
    def viewportRatio:Double = viewportPx.x / viewportPx.y


    /** Erase the projection matrix with a new projection using the given frustum
      * specifications. */
    def frustum(axes:Axes) {
        projection.setIdentity
        maxDepth = axes.z.from
        projection.frustum(axes.x.from, axes.x.to, axes.y.from, axes.y.to, axes.z.to, axes.z.from)
        needRecomputeMVP = true
    }
    
    /** Erase the projection matrix with a new projection using the given frustum
      * specifications. */
    def frustum(left:Double, right:Double, bottom:Double, top:Double, near:Double) {
        projection.setIdentity
        projection.frustum(left, right, bottom, top, near, maxDepth)
        needRecomputeMVP = true
    }

    /** Erase the projection matrix with a new projection using the given orthographic
      * specifications. */
    def orthographic(axes:Axes) {
    	projection.setIdentity
    	maxDepth = axes.z.from
    	projection.orthographic(axes.x.from, axes.x.to, axes.y.from, axes.y.to, axes.z.to, axes.z.from)
        needRecomputeMVP = true
    }

    /** Erase the projection matrix with a new projection using the given orthographic
      * specifications. */
    def orthographic(left:Double, right:Double, bottom:Double, top:Double, near:Double, far:Double) {
        projection.setIdentity
        maxDepth = far
        projection.orthographic(left, right, bottom, top, near, far)
        needRecomputeMVP = true
    }

    def orthographicPixels(near:Double = 1, far:Double = -1) {
    	projection.setIdentity
    	maxDepth = far
    	projection.orthographic(0, viewportPx.x, 0, viewportPx.y, near, far)
    	needRecomputeMVP = true
    }

    /** Reset the modelview to the identity. This must be called before each display
      * of the scene, before any transformation are made to the model view matrix. */
    def viewIdentity() {
    	modelview.setIdentity
        needRecomputeMVP = true
    }

    /** Transform the given `point` using the top most matrix. */
    def transform(point:Point4):Point4 = {
    	recomputeMVP
    	mvp.mult(point)
    }

    /** If neede recompute the inverse of the top most mvp matrix, and transform the given point by this inverse. */
    def inverseTransform(point:Point4):Point4 = {
    	recomputeMVP

    	if(inverseMVP eq null) {
    		inverseMVP = mvp.inverse
    	}

    	mvp.mult(point)
    }
    
    def pushProjection() { projection.push; inverseMVP = null }

    def popProjection() { projection.pop; needRecomputeMVP = true; inverseMVP = null }

    def pushpopProjection(code: => Unit):Unit = {
    	pushProjection
    	code
    	popProjection
    }

    /** Push a copy of the current model-view matrix in the model-view matrix stack. */
    def push() { modelview.push; inverseMVP = null }
    
    /** Pop the top-most model-view matrix from the model-view matrix stack and restore the
      * previous one as the current top mode-view matrix. */
    def pop() { modelview.pop; needRecomputeMVP = true; inverseMVP = null }
    
    /** Push a copy of the current model-view matrix on top of the model-view matrix stack, and
      * execute the given code, then pop the top matrix and restore the previous one. */
    def pushpop(code: => Unit):Unit = {
        push
        code
        pop
    }
    
    /** Apply a translation of vector `(dx, dy, dz)` to the current model-view matrix. */
    def translate(dx:Double, dy:Double, dz:Double) {
    	modelview.translate(dx, dy, dz)
        needRecomputeMVP = true
    }
    
    /** Apply a translation of vector `of` to the current model-view matrix. */
    def translate(of:NumberSeq3) {
        modelview.translate(of)
        needRecomputeMVP = true
    }
    
    /** Apply a rotation of `angle` around axis `(x, y, z)` to the current model-view matrix. */
    def rotate(angle:Double, x:Double, y:Double, z:Double) {
        modelview.rotate(angle, x, y, z)
        needRecomputeMVP = true
    }
    
    /** Apply a rotation of `angle` around `axis` to the current model-view matrix. */
    def rotate(angle:Double, axis:NumberSeq3) {
        modelview.rotate(angle, axis)
        needRecomputeMVP = true
    }

    /** Apply a rotation of `angle` around `axis` to the current model-view matrix. */
    def rotate(angle:Double, axis:Axis) {
    	modelview.rotate(angle, axis)
    	needRecomputeMVP = true
    }
    
    /** Scale the current model-view matrix by coefficients `(sx, sy, sz)`. */
    def scale(sx:Double, sy:Double, sz:Double) {
        modelview.scale(sx, sy, sz)
        needRecomputeMVP = true
    }
    
    /** Scale the current model-view matrix by coefficients from the vector `by`. */
    def scale(by:NumberSeq3) {
        modelview.scale(by)
        needRecomputeMVP = true
    }

    /** Define a model-view matrix according to the `eye` position and pointing at the
      * `focusPoint`. This erases the model-view matrix at the top of the stack and copy in
      * it the new "look-at" matrix.
      *
      * The `up` vector allow to bank the camera, most often, as it name suggests, it points
      * up (0,1,0).
      * 
      * This method must be called before any transform done on the "model", usually first
      * before drawing anything. It is suitable for a 3D environment with a perspective
      * transform. For an orthographic transform, you should not need this (it should work
      * however, if you know what you do).
      * 
      * This method does not empty the model-view matrix stack. */
    def lookAt(eye:Vector3, focusPoint:Vector3, up:Vector3) {
        modelview.setIdentity
        modelview.lookAt(eye, focusPoint, up)
        needRecomputeMVP = true
    }

    /** Same as lookAt(eye, focusPoint, up), but takes its information from [[PointOfView]] instance. */
    def lookAt(pov:PointOfView) {
    	lookAt(pov.cartesianEye, pov.focus, pov.up)
    }
    
    /** Apply the given `transform` to the current model-view matrix. */
    def transform(transform:Matrix4) {
        modelview.multBy(transform)
        needRecomputeMVP = true
    }

    /** Build the MVP from the projection and modelview matrices. */
    protected def recomputeMVP() {
    	if(needRecomputeMVP) {
        	mvp.copy(projection)
        	mvp.multBy(modelview)
        	needRecomputeMVP = false
        }    	
    }
    
    /** Store as uniform variables the model-view matrix, the model-view*projection matrix (mvp)
      * and the top 3x3 matrix extracted from the model-view matrix in the given shader.
      * 
      * These matrices are stored under the default names "MV" for the model-view, "MVP" for the
      * perspective * model-view and "MV3x3" for the top 3x3 model-view matrix.
      */
    def uniform(shader:ShaderProgram, mvName:String="MV", mv3x3Name:String="MV3x3", mvpName:String="MVP") {
		uniformMV(shader, mvName)
		uniformMV3x3(shader, mv3x3Name)
		uniformMVP(shader, mvpName)
    }

    /** Store as uniform variable the model-view*projection matrix (mvp). The default uniform
      * name is "MVP". */
    def uniformMVP(shader:ShaderProgram, mvpName:String="MVP") {
        recomputeMVP
        shader.uniformMatrix(mvpName, mvp)
    }
    	
    /** Store as uniform variable the model-view matrix. The default uniform name is "MV". */
    def uniformMV(shader:ShaderProgram, mvName:String="MV") { shader.uniformMatrix(mvName, modelview.top) }
    
    /** Store as uniform variable the top 3x3 matrix of the model-view. The default uniform name is "MV3x3". */
    def uniformMV3x3(shader:ShaderProgram, mv3x3Name:String="MV3x3") { shader.uniformMatrix(mv3x3Name, modelview.top3x3) }   
}