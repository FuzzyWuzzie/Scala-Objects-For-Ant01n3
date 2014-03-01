package org.sofa.opengl.actor.renderer


/** Set of messages sent back by avatars when something occurs on them. */
object Acquaintance {
	/** An avatar has been touched. */
	case class TouchEvent(from:AvatarName, isStart:Boolean, isEnd:Boolean)	
}


// -- Events -----------------------------------------------------------------------------------------------


/** Renderer event sent when the avatar is touched. */
case class TouchEvent(x:Double, y:Double, z:Double, isStart:Boolean, isEnd:Boolean) {
	override def toString():String = "touch[%.2f %.2f %.2f%s]".format(x,y,z, if(isStart)" start"else if(isEnd)" end" else "")
}