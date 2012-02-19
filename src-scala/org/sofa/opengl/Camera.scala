package org.sofa.opengl

import scala.math._

import org.sofa.math._

object Camera {
    def apply() = new Camera()
}

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
class Camera {
    /** How to pass from view space to camera space. */
    val projection:Matrix4 = Matrix4()
    
    /** How to pass from model to view space. */
    val modelview = MatrixStack(Matrix4())

    /** Up direction, for camera banking. */
    var up = Vector3(0, 1,  0)
    
    /** Position of the camera eye in spherical coordinates. */
    var sphericalEye = Vector3(0, 0,  1)
    
    /** Position of the camera eye in Cartesian coordinates. */
    var cartesianEye = Vector3(0, 0, -1)
    
    /** The looked at point. */
    var focus = Vector3(0, 0, 0)
    
    /** View port size in pixels. */
    var viewportPx = Vector2(800, 600)
    
    /** Maximum depth of the view (far clip-plane position). */
    var maxDepth = 1000.0

    def viewSpherical(theta:Double, phi:Double, radius:Double) {
       sphericalEye.set(theta, phi, radius)
       cartesianFromSpherical
    }
    
    def viewCartesian(x:Double, y:Double, z:Double) {
        cartesianEye.set(x, y, z)
        sphericalFromCartesian
    }
    
    def rotateViewHorizontal(delta:Double) {
        sphericalEye.x += delta
        cartesianFromSpherical
    } 
    
    def rotateViewVertical(delta:Double) {
        sphericalEye.y += delta
        cartesianFromSpherical
    }
    
    def zoomView(delta:Double) {
        sphericalEye.z += delta
        cartesianFromSpherical
    }
    
    protected def cartesianFromSpherical() {
        cartesianEye.x = sphericalEye.z * cos(sphericalEye.x) * sin(sphericalEye.y)
        cartesianEye.z = sphericalEye.z * sin(sphericalEye.x) * sin(sphericalEye.y)
        cartesianEye.y = sphericalEye.z * cos(sphericalEye.y)
    }
    
    protected def sphericalFromCartesian() {
        sphericalEye.z = sqrt(cartesianEye.x*cartesianEye.x + cartesianEye.y*cartesianEye.y + cartesianEye.z*cartesianEye.z)
    	sphericalEye.x = acos(cartesianEye.z / sphericalEye.z)
    	sphericalEye.y = atan(cartesianEye.y / cartesianEye.x)
    }
    
    def viewportPx(width:Double, height:Double) {
        viewportPx.set(width, height);
    }
    
    def viewportRatio:Double = viewportPx.x / viewportPx.y
    
    def frustum(left:Double, right:Double, up:Double, down:Double, near:Double) {
        projection.setIdentity
        projection.frustum(left, right, up, down, near, maxDepth)
    }
    
    def setupView() {
        modelview.setIdentity
        modelview.lookAt(cartesianEye, focus, up)
    }
    
    def push() {
        modelview.push
    }
    
    def pop() {
        modelview.pop
    }
    
    def pushpop(code: => Unit):Unit = {
        push
        code
        pop
    }
    
    def translateModel(dx:Double, dy:Double, dz:Double) {
    	modelview.translate(dx, dy, dz)
    }
    
    def translateModel(of:Vector3) {
        modelview.translate(of)
    }
    
    def rotateModel(angle:Double, x:Double, y:Double, z:Double) {
        modelview.rotate(angle, x, y, z)
    }
    
    def rotateModel(angle:Double, axis:Vector3) {
        modelview.rotate(angle, axis)
    }
    
    def scaleModel(sx:Double, sy:Double, sz:Double) {
        modelview.scale(sx, sy, sz)
    }
    
    def scaleModel(by:Vector3) {
        modelview.scale(by)
    }
}