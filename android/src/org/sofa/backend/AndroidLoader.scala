package org.sofa.backend

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
Console.err.println("## looking for '%s / %s' in {%s}".format(newPath, newName, resources.getAssets.list(newPath).mkString(",")))
		resources.getAssets.list(newPath).contains(newName)
	}	

	/** Search for the given resource in the set of pathes. Return the
	  * first full path that is present in the assets or throw an IO exception
	  * if not found. */
	protected def searchInAssets(resource:String, path:Seq[String]):String = {
		if(exists("", resource)) {
			resource
		} else {
			path.find(path => exists(path, resource)) match {
				case path:Some[String] => { "%s/%s".format(path.get,resource) }
				case None => { throw new java.io.IOException("cannot open resource %s".format(resource)) }
			}
		}
	}
}