// package org.sofa.gfx.text

// import org.sofa.gfx.SGL
// import scala.collection.mutable.{HashMap, PriorityQueue}
// //import System._


// /** A fixed size cache of [[GLString]] elements for a specific [[GLFont]] that
//   * are indexed by their usage count.
//   *
//   * The more often a string of text is used, the more it has chances to be conserved. The process is
//   * hidden by the cache, that creates and destroys text items when necessary.
//   *
//   * @param maxItems maximum number of items per priority levels.
//   * @param priorityLevels Number of buckets of items (from lower to higher priority).
//   * @param checkOldItemsEveryMs Check for items that are older than `oldItemMs` and remove them as soon as this parameter milliseconds are ellapsed.
//   * @param oldItemsMs Items are considered old (and can possibly be removed) when not used since this time in milliseconds.
//   */
// class Cache[K, T](val maxItems:Int, val priorityLevels:Int = 10, val checkOldItemsEveryMs:Long = 5000, val oldItemMs:Long = 4900) {

// 	protected val priorities = new Array[CachePriorityBucket[K, T]](priorityLevels)

// 	protected val set = new HashMap[K, CacheItem[T]]()

// 	protected var itemCount = 0

// 	protected var lastStepTime = 0L

// 	init()

// 	protected def init() {
// 		lastStepTime = System.currentTimeMillis

// 		for(i <- 0 until priorityLevels) {
// 			priorities(i) = new CachePriorityBucket[K, T](i)
// 		}
// 	}

// 	def get(key:K):Option[T] = {
// 		set.get(key) match {
// 			case Some(item) => {
// 				item.age = System.currentTimeMillis
				
// 				if(item.priority < priorityLevels-1) {
// 					priorities(item.priority) -= key
// 					item.priority += 1
// 					priorities(item.priority) += (key -> item)
// 				}
				
// 				Some(item.value)
// 			}
// 			case None => {
// 				None
// 			}
// 		}
// 	}

// 	def add(key:K, item:T) {
// 		purgeCache
// 		val cacheItem = new CacheItem(0, System.currentTimeMillis, item)
// 		priorities(0).add(key, cacheItem)
// 		set += (key -> cacheItem)
// 		itemCount += 1
// 	}

// 	/** Remove an item in the least priority level. */
// 	protected def purgeCache() {
// 		if(itemCount >= maxItems*priorityLevels) {
// 			var i = 0
// 			while(priorities(i).isEmpty) { i+= 1 }
// 			set -= priorities(i).removeOneItem
// 			itemCount -= 1
// 		}
// 	}

// 	def step() {
// 		val T = System.currentTimeMillis

// 		if(T - lastStepTime >= checkOldItemsEveryMs) {

// 		}

// 		lastStepTime = T
// 	}

// 	/** Number of items per cache level. */
// 	def itemsPerLevel:Array[Int] = priorities.map { item => item.size }

// 	/** Total number of items in the cache. */
// 	def size = itemCount
// }


// /** An item in the cache.
//   *
//   * @param priority The actual priority value (level in the cache).
//   * @param age The time in milliseconds when the item was last used.
//   * @param value The item content.
//   */
// class ChacheItem[T](var priority:Int, var age:Long, val value:T) {}


// /** A bucket of elements at a given level of priority. */
// class CachePriorityBucket[K, T](val priority:Int) extends HashMap[K, CacheItem[T]] {
	
// 	/** Remove a random element and return its key. */
// 	def removeOneItem():K = {
// 		var key = null
// 		var older = System.currentTimeMillis
		
// 		foreach { item =>
// 			if(item._2.age < older) {
// 				key = item._1
// 				older = item._2.age
// 			}
// 		}

// 		val item = get(key) match {
// 			case Some(item) => item
// 			case None       => throw new RuntimeException("impossible ?")
// 		}

// 		this -= key

// 		item
// 	}

// 	def removeItemsOlderThan(age:Long):ArrayBuffer[K] = {
// 		val removed = new ArrayBuffer[K]()

// 		items.for
// 	}
// }