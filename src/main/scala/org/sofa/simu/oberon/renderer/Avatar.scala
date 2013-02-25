package org.sofa.simu.oberon.renderer

import org.sofa.math.Rgba

/** When an avatar is not found. */
case class NoSuchAvatarException(msg:String) extends Exception(msg)

/** When an axis does not exist in an avatar or screen. */
case class NoSuchAxisException(msg:String) extends Exception(msg)

/** Create actor representators. */
trait AvatarFactory {
	def screenFor(name:String, screenType:String):Screen
	def avatarFor(name:String, avatarType:String):Avatar
}

/** Base for each renderable thing in (avatars, screens, etc.). */
trait Renderable {
	/** called before the first render. */
	def begin()

	/** Render one frame of the screen. */
	def render()

	/** Change the state of automatically animated "things". */
	def animate()

	/** Called after the last render. */
	def end()
}

/** Graphical representation of an actor in the renderer. */
abstract class Avatar(val name:String) extends Renderable {
	def change(axis:String, values:AnyRef*)
}

/** Specific avatar that implements a clickable element. */
class Button(name:String, screen:Screen) extends Avatar(name) {
	def begin() {

	}

	def change(axis:String, values:AnyRef*) {

	}

	def render() {

	}

	def animate() {

	}

	def end() {

	}
}