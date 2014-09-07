// package org.sofa.gfx.displaylist



// trait DisplayItem {
// 	/** The model-view-projection at time of creation. */
// 	val mvp = Matrix4()

// 	/** Change the model-view-projection. */
// 	def space(space:Space) { this.mvp.copy(space.top) }

// 	/** Render the list with the given space. */
// 	def render(gl:SGL)
// }


// object DisplayList {
// 	final val MaxStepsBeforeCompact = 100
// 	final val MaxRemovedForCompact = 512
// }


// class DisplayList(val gl:SGL) {
// 	protected[this] var dp = new ArrayBuffer[DisplayItem]()

// 	protected[this] var steps = 0

// 	protected[this] var removed = 0

// 	def register(item:DisplayItem):Int = {
// 		dp += item
// 		dp.length - 1
// 	}

// 	def remove(itemId:Int):DisplayItem = {
// 		val item = dp(itemId)
// 		dp(itemId) = null
// 		removed += 1
// 		item
// 	}

// 	def render() {
// 		val n = dp.length - removed
// 		var i = 0
// 		while(i < n) {
// 			if(dp(i) ne null)
// 				dp(i).render(gl)
// 			i += 1
// 		}

// 		step += 1
// 		compact
// 	}

// 	protected def compact() {
// 		if(step > MaxStepsBeforeCompact) {
// 			if(removed > MaxRemovedForCompact) {
// 				val n     = dp.length
// 				val newDp = new ArrayBuffer[DisplayItem](n - removed)
// 				var i     = 0

// 				while(i < n) {
// 					if(dp(i) ne null)
// 						newDp += dp(i)
// 					i += 1
// 				}

// 				dp = newDp
// 				removed = 0
// 			}
// 			step = 0
// 		}
// 	}
// }


