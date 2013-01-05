package org.sofa.opengl.backend

import org.sofa.math.{Vector3, Point3}
import org.sofa.opengl.Camera
import org.sofa.opengl.surface.{Surface, MotionEvent, BasicCameraController}

class BasicTouchController(camera:Camera) extends BasicCameraController(camera) {
	val oldPos = Point3(0,0,0)
	val vector = Vector3()
	override def motion(surface:Surface, event:MotionEvent) {
		if(event.isStart) {
			oldPos.set(event.x, event.y, event.pressure)
		} else if(event.isEnd) {
			vector.set(event.x-oldPos.x, event.y-oldPos.y, 0)
			oldPos.set(event.x, event.y, event.pressure)
			camera.rotateViewHorizontal(vector.x*0.005)
			camera.rotateViewVertical(-vector.y*0.005)
		} else {
			vector.set(event.x-oldPos.x, event.y-oldPos.y, 0)
			oldPos.set(event.x, event.y, event.pressure)
			camera.rotateViewHorizontal(vector.x*0.005)
			camera.rotateViewVertical(-vector.y*0.005)
		}
	}
}