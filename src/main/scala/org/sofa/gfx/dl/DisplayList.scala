package org.sofa.gfx.dl

import org.sofa.gfx.SGL


/** Base trait for a display list. */
trait DisplayList {
	/** Render the list with the given space. */
	def render(gl:SGL)

	/** Release graphic resource. */
	def dispose(gl:SGL)
}