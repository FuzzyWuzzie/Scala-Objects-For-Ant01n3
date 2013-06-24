package org.sofa.opengl

import scala.math._

import org.sofa.math._


// -- Point of View -----------------------------------------------------------------------


/** Objects of this class simulate a point of view (eye) and a looked at point (focus). It
  * can be freely positioned in Cartesian coordinates. It can also be positioned in spherical
  * coordinates around the focus. Its banking can be controlled using its up vector. 
  * The object automatically updates the coordinates in Cartesian or spherical forms, when
  * the other is changed so that both are always available.
  *
  * In spherical coordinates, we represent [[theta]] as the angle on the horizontal
  * axis, [[phi]] as the angle on the vertical axis, and [[radius]] as the distance from
  * the focus point.
  *
  * The object also stores a "eye angle" that represent the half angle between two eyes for
  * stereo representations. */
class PointOfView {

    /** Up direction, for camera banking. */
    var up = Vector3(0, 1,  0)
    
    /** Position of the camera eye in spherical coordinates. */
    var sphericalEye = Vector3(0, 0,  1)
    
    /** Position of the camera eye in Cartesian coordinates. */
    var cartesianEye = Vector3(0, 0, -1)

    /** The looked at point. */
    var focus = Vector3(0, 0, 0)

    /** Distance between the two eyes divided by two. This value is used
      * by [[lookAtLeftEye]] and [[lookAtRightEye]]. */
    var eyeAngle = Pi / 64.0

	/** Distance from the eye to the focus. */    
    def radius_= (value:Double):Unit = { sphericalEye.z = value; cartesianFromSpherical }
    
	/** Distance from the eye to the focus. */    
    def radius:Double = sphericalEye.z

	/** Horizontal spherical eye coordinate in radians, positive values go CCW. */    
    def theta_= (value:Double):Unit = { sphericalEye.x = value; cartesianFromSpherical }
    
	/** Horizontal spherical eye coordinate in radians. */    
    def theta:Double = sphericalEye.x
    
    /** Vertical spherical eye coordinate in radians, positive values go CW. */
    def phi_= (value:Double):Unit = { sphericalEye.y = value; cartesianFromSpherical }
    
    /** Vertical spherical eye coordinate in radians. */
    def phi:Double = sphericalEye.y

    /** Set the coordinates of the camera "eye" in spherical coordinates. This will be used
      * to define the "view" part of the model-view matrix when using [[setupView()]].
      * The coordinates (0,0,x) (with x > 0) are just above the world, aligned with the
      * y axis. Positive values for theta angle go CCW and positive values for phi go CW.
      * Angles are in radians. */
    def eyeSpherical(theta:Double, phi:Double, radius:Double) {
       sphericalEye.set(theta, phi, radius)
       cartesianFromSpherical
    }
    
    /** Set the coordinates of the camera "eye" in Cartesian coordinates. This will be used
      * to define the "view" part of the model-view matrix when using [[setupView()]]. */
    def eyeCartesian(x:Double, y:Double, z:Double) {
        cartesianEye.set(x, y, z)
        sphericalFromCartesian
    }
    
    /** Modify the coordinates of the camera "eye" by rotating horizontally around its focus point
      * of the given amount `delta`. */
    def rotateEyeHorizontal(delta:Double) {
        sphericalEye.x += delta
        cartesianFromSpherical
    } 
    
    /** Modify the coordinates of the camera "eye" by rotating vertically around its focus point
      * of the given amount `delta`. */
    def rotateEyeVertical(delta:Double) {
        sphericalEye.y += delta
        cartesianFromSpherical
    }

    /** Modify the coodinates of the camera "eye" by rotating horizontally and vertically its focus point
      * of the amount (`thetaDouble`, `phiDelta`). */
    def rotateEye(thetaDelta:Double, phiDelta:Double) {
    	sphericalEye.x += thetaDelta
    	sphericalEye.y += phiDelta
    	cartesianFromSpherical
    }
    
    /** Modify the distance between the camera "eye" and the focus point from the given amount
      * `delta`. */
    def eyeTraveling(delta:Double) {
        sphericalEye.z += delta
        cartesianFromSpherical
    }
    
    /** Set the focus point (looked-at point) at `(x, y, z)`. */
    def setFocus(x:Double, y:Double, z:Double) = focus.set(x, y, z)
    
    /** Set the focus point (looked-at point) at `(p)`. */
    def setFocus(p:NumberSeq3) = focus.copy(p)
    
    protected def cartesianFromSpherical() {
        cartesianEye.x = sphericalEye.z * cos(sphericalEye.x) * sin(sphericalEye.y)
        cartesianEye.z = sphericalEye.z * sin(sphericalEye.x) * sin(sphericalEye.y)
        cartesianEye.y = sphericalEye.z * cos(sphericalEye.y)
    	//Console.err.println("cart(%s) -> sphe(%s)".format(cartesianEye, sphericalEye))
    }
    
    protected def sphericalFromCartesian() {
      	sphericalEye.z = sqrt(cartesianEye.x*cartesianEye.x + cartesianEye.y*cartesianEye.y + cartesianEye.z*cartesianEye.z)
    	sphericalEye.y = acos(cartesianEye.y / sphericalEye.z)
    	sphericalEye.x = atan(cartesianEye.z / cartesianEye.x)
    	//Console.err.println("sphe(%s) -> cart(%s)".format(sphericalEye, cartesianEye))
    }

    override def toString() = "lookAt{cartesian(%s) spherical(%s) focus(%s) up(%s)}".format(cartesianEye, sphericalEye, focus, up)
}


// -- Camera ------------------------------------------------------------------------------


object Camera { def apply() = new Camera() }


/** A camera analogy for OpenGL.
  *
  * Objects of this class simulate a camera point of view (eye) and looked at point (focus). It
  * can be freely positioned in Cartesian coordinates, and will automatically point at the focus.
  * It can also be positioned in spherical coordinates around the focus. Its banking can be
  * controlled using its up vector.
  * 
  * The camera maintains a projection and a model-view matrix. The model-view matrix allows to
  * "move" the camera in space. The projection matrix allows to specify the camera viewing
  * properties like the aperture. It also defines a far clipping plane, so that objects too far
  * away are not drawn.
  * 
  * It stores a view port in pixels that express the rendering canvas dimensions. This canvas also
  * allows to control the aspect-ratio of the camera. 
  */
class Camera extends PointOfView {
    /** How to pass from view space to camera space. */
    val projection:Matrix4 = Matrix4()
    
    /** How to pass from model to view space. */
    val modelview = MatrixStack(Matrix4())

    /** The projection matrix times the modelview matrix. We avoid to recompute or reallocate
      * it since it is used very often. Memory/speed compromise. */
    val mvp = new Matrix4()

    /** Flag indicating if the modelview or projection changed since the last MVP compute. */
    protected var needRecomputeMVP = true
        
    /** View port size in pixels. */
    var viewportPx = Vector2(800, 600)
    
    /** Maximum depth of the view (far clip-plane position). */
    var maxDepth = 100.0

    def viewportWidth:Double = viewportPx.x

    def viewportWidth_= (value:Double):Unit = { viewportPx.x = value } 

    def viewportHeight:Double = viewportPx.y

    def viewportHeight_= (value:Double):Unit = { viewportPx.y = value }

    def viewport:(Double,Double) = (viewportPx.x, viewportPx.y)

    def viewport_=(values:(Double,Double)):Unit = { viewportPx.set(values._1, values._2) }
    
    /** Set the width and height of the output view-port in pixels. */
    def viewportPx(width:Double, height:Double) {
        viewportPx.set(width, height);
    }
    
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
    
    /** Define a model-view matrix according to the eye position and pointing at the
      * focus point. This erases the model-view matrix at the top of the stack and copy in
      * it the new "look-at" matrix.
      * 
      * This method must be called before any transform done on the "model", usually first
      * before drawing anything. It is suitable for a 3D environment with a perspective
      * transform. For an orthographic transform, you should not need this (it should work
      * however, if you know what you do).
      * 
      * This method does not empty the model-view matrix stack. */
    def lookAt() {
        modelview.setIdentity
        modelview.lookAt(cartesianEye, focus, up)
        needRecomputeMVP = true
    }

    /** Same as [[lookAt]], but offset the eye around the focus point by `eyeAngle` to
      * represent the scene as seen from the left eye. */
    def lookAtLeftEye() {
    	val t = theta
    	theta = t + eyeAngle
    	lookAt
    	theta = t
    }

    /** Same as [[lookAt]], but offset the eye around the focus point by `eyeAngle` to
      * represent the scene as seen from the right eye. */
    def lookAtRightEye() {
    	val t = theta
    	theta = t - eyeAngle
    	lookAt
    	theta = t
    }

    /** Reset the modelview to the identity. This must be called before each display
      * of the scene, before any transformation are made to the model view matrix. */
    def viewIdentity() {
    	modelview.setIdentity
        needRecomputeMVP = true
    }
    
    /** Push a copy of the current model-view matrix in the model-view matrix stack. */
    def push() {
        modelview.push
    }
    
    /** Pop the top-most model-view matrix from the model-view matrix stack and restore the
      * previous one as the current top mode-view matrix. */
    def pop() {
        modelview.pop
        needRecomputeMVP = true
    }
    
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

    override def toString() = "cam{cartesian(%s) spherical(%s) focus(%s) up(%s) vp(%s)}".format(cartesianEye, sphericalEye, focus, up, viewportPx)
}