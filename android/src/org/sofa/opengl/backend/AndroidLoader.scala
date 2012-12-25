package org.sofa.opengl.backend

import android.content.res.Resources

/** Base trait for loaders on Android. */
trait AndroidLoader {
	val resources:Resources

	/** Tests if the given resource in the given path exists in the assets. */
	protected def exists(path:String, resource:String):Boolean = {
		// We cut the path/resource name anew, since the resource can also contain path
		// separators.
		
		val fileName = if(path.length>0) "%s/%s".format(path, resource) else resource
		val pos      = fileName.lastIndexOf('/')
		var newName  = fileName
		var newPath  = ""

		if(pos >= 0) {
			newPath = fileName.substring(0, pos)
			newName = fileName.substring(pos+1)
		}		

		resources.getAssets.list(newPath).contains(newName)
	}	
}