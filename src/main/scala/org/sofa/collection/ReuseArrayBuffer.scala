package org.sofa.collection

import scala.collection.mutable.ArrayBuffer


/** Trait for elements that have a mutable index inside a sequence. */
trait Indexed {
	def index:Int
	def setIndex(i:Int)
}


/** An array buffer whose remove operation pivotes with the last entry of the array,
  * instead of offsetting the end of the array. This works only with elements
  * that implement [[Indexed]]. 
  *
  * TODO: check that all inserting and removing operations are overriden.
  */
class ReuseArrayBuffer[A <: Indexed] extends ArrayBuffer[A] {
	override def update(i:Int, elem:A) {
		elem.setIndex(i)
		super.update(i, elem)
	}
	override def remove(i:Int):A = {
		val n = size
		if(i >= n || i < 0) {
			throw new ArrayIndexOutOfBoundsException("invalid index %d".format(i))
		} else if(i == n-1) {
			this(i).setIndex(-1)
			super.remove(i)
		} else {
			this(i).setIndex(-1)
			update(i, this(n-1))
			super.remove(n-1)
		}
	}
	override def +=(elem:A) = {
		val n = size
		elem.setIndex(n)
		super.+=(elem)
	}
}