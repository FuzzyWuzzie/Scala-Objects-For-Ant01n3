package org.sofa.gfx

import scala.math._

import org.sofa.math._
import org.sofa.math.Axis._

// -- Point of View -----------------------------------------------------------------------


/** Objects of this class simulate a point of view (eye) and a looked at point (focus). It
  * can be freely positioned in Cartesian coordinates. It can also be positioned in spherical
  * coordinates around the focus. Its banking can be controlled using its up vector. 
  *
  * The object automatically updates the coordinates in Cartesian or spherical forms, when
  * the other is changed so that both are always available.
  *
  * In spherical coordinates, we represent [[theta]] as the angle on the horizontal
  * axis, [[phi]] as the angle on the vertical axis, and [[radius]] as the distance from
  * the focus point.
  *
  * Angles are expressed in radians.
  *
  * The [[theta]] angle origin is at [[radius]] units aligned on the positive X axis.
  * When you increment [[theta]] you first go toward Z positive (trigonometric 
  * direction, on the XZ plane).
  *
  * The [[phi]] angle origin is at [[radius]] units aligned on the positive Y axis.
  * When you increment [[phi]], you first got toward Z positive (trigonometric
  * direction on the YZ plane). 
  *
  * The object also stores a "eye angle" that represent the half angle between two eyes for
  * stereo representations. 
  */
trait PointOfView {

    /** Up direction, for camera banking. */
    val up = Vector3(0, 1,  0)
    
    /** Position of the camera eye in spherical coordinates. */
    val sphericalEye = Vector3(0, 0,  1)
    
    /** Position of the camera eye in Cartesian coordinates. */
    val cartesianEye = Vector3(0, 0, -1)

    /** The looked at point. */
    val focus = Vector3(0, 0, 0)

    /** Distance between the two eyes divided by two. This value is used
      * by `lookAtLeftEye` and `lookAtRightEye`. */
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
      * to define the "view" part of the model-view matrix when using `setupView()`.
      * The coordinates (0,0,x) (with x > 0) are just above the world, aligned with the
      * y axis. Positive values for theta angle go CCW and positive values for phi go CW.
      * Angles are in radians. */
    def eyeSpherical(theta:Double, phi:Double, radius:Double) {
       sphericalEye.set(theta, phi, radius)
       cartesianFromSpherical
    }
    
    /** Set the coordinates of the camera "eye" in Cartesian coordinates. This will be used
      * to define the "view" part of the model-view matrix when using `setupView()`. */
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
  * The camera fully implement a [[PointOfView]] and has the same abilities (the position
  * of the viewer is called "eye", the looked-at point is the "focus" and an "up" vector
  * allows to bank the camera).
  * 
  * The camera maintains a projection and a model-view matrix. The model-view matrix allows to
  * "move" the camera in space. The projection matrix allows to specify the camera viewing
  * properties like the aperture and type of projection. It also defines a far clipping plane,
  * so that objects too far away are not drawn.
  * 
  * It stores a view port in pixels that express the rendering canvas dimensions. This canvas also
  * allows to control the aspect-ratio of the camera. 
  */
class Camera extends Space with PointOfView {
    
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
    def lookAt() { lookAt(cartesianEye, focus, up) }

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

    override def toString() = "cam{cartesian(%s) spherical(%s) focus(%s) up(%s) vp(%s)}".format(cartesianEye, sphericalEye, focus, up, viewport)
}


/** Identical to the [[Camera]] object but allows to wrap a [[Space]] instance to
  * reuse it as an orbiting camera. Use the `setSpace()` method to specify the space
  * to use. The `lookAt()`  method will overwrite the top-most model-view matrix. */
class CameraSpace extends PointOfView {
	protected[this] var space:Space = null

	def setSpace(space:Space) { this.space = space }

	def lookAt() {
		if(space ne null)
			space.lookAt(cartesianEye, focus, up)
	}

	def lookAtLeftEye() {
		val t = theta
		theta = t + eyeAngle
		lookAt
		theta = t
	}

	def lookAtRightEye() {
		val t = theta
		theta = t - eyeAngle
		lookAt
		theta = t
	}
}