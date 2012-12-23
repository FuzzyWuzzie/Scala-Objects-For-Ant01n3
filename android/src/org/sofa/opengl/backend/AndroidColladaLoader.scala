package org.sofa.opengl.backend

import org.sofa.opengl.io.collada.{ColladaLoader,ColladaFile}
import scala.xml.{XML, NodeSeq}
import android.content.res.Resources

class AndroidColladaLoader(val resources:Resources) extends ColladaLoader {
    def open(resource:String):NodeSeq = {
        var file = new java.io.File(resource)
        if(exists("", resource)) {
            scala.xml.XML.load(resources.getAssets.open(resource)).child
        } else {
            ColladaFile.path.find(path => exists(path, resource)) match {
                case path:Some[String] => { scala.xml.XML.load(resources.getAssets.open("%s/%s".format(path.get,resource))).child }
                case None => { throw new java.io.IOException("cannot locate Collada file %s (path %s)".format(resource, ColladaFile.path.mkString(":"))) }
            }
        }
    }

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