package org.sofa.simu.oberon.renderer

import org.sofa.math.Rgba

/** When an avatar is not found. */
case class NoSuchAvatar(msg:String) extends Exception(msg)

/** Create actor representators. */
trait AvatarFactory {
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
trait Avatar extends Renderable {
}

/** Specific avatar that implements a clickable element. */
class Button(screen:Screen) extends Avatar {
	def begin() {

	}

	def render() {

	}

	def animate() {

	}

	def end() {
		
	}
}