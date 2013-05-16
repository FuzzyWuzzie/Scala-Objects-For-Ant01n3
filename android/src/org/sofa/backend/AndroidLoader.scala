package org.sofa.backend

import android.content.res.Resources

/** Base trait for loaders on Android. */
trait AndroidLoader {
	val resources:Resources

	/** Remove any space before and after the path and any trailing slash. */
	protected def trim(path:String):String = {
		var p = path.trim
		if(p.endsWith("/")) p.substring(0, p.length-1) else p
	} 

	/** Tests if the given resource in the given path exists in the assets. */
	protected def exists(path:String, resource:String):Boolean = {
		// Android does not support trailing '/' in path names nor it support
		// double slashes ... we have to create a clean path.

 		var pos = resource.lastIndexOf('/')
		var newPath = trim(path)
		var newName = resource.trim

		if(pos >= 0) {
			// We cut the path/resource name anew, since the resource can also contain path
			// separators.
		
			val fileName = if(newPath.length>0) "%s/%s".format(newPath, newName) else newName
			pos          = fileName.lastIndexOf('/')
		    newName      = fileName
		    newPath      = ""

			if(pos >= 0) {
				newPath = fileName.substring(0, pos)
				newName = fileName.substring(pos+1)
			}
		}

		resources.getAssets.list(newPath).contains(newName)
	}	

	/** True if the given file exists in the assets. */
	def exists(fullPathFileName:String):Boolean = {
		val name = fullPathFileName.trim
		val pos  = name.lastIndexOf('/')
		val path = if(pos>0) name.substring(0, pos) else ""
		val file = if(pos>0) name.substring(pos+1) else name

		resources.getAssets.list(path).contains(file)
	}

	/** Search for the given resource in the set of pathes. Return the
	  * first full path that is present in the assets or throw an IO exception
	  * if not found. */
	protected def searchInAssets(resource:String, path:Seq[String]):String = {
		val res = resource.trim

		if(exists("", res)) {
			resource
		} else {
			path.find(path => exists(path, res)) match {
				case path:Some[String] => { "%s/%s".format(trim(path.get),res) }
				case None => { throw new java.io.IOException("cannot open resource %s (path=%s)".format(resource, path.mkString(":"))) }
			}
		}
	}
}