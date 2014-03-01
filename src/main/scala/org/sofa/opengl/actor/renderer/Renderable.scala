package org.sofa.opengl.actor.renderer


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