package org.sofa.backend

import android.util.Log


/** A facility to log debug messages. */
trait SOFALog {
	/** Default tag for messages. */
	final val Tag = "SOFA"
	
	/** Send a debug `message`. */
	def debug(message:String) { Log.d(Tag, message) }
}
