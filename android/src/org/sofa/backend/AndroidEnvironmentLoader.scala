package org.sofa.backend

import org.sofa.{Environment, EnvironmentLoader}
import java.io.{InputStream, IOException}
import android.content.res.Resources


class AndroidEnvironmentLoader(val resources:Resources) extends EnvironmentLoader {
    def open(resource:String):InputStream = {
        if(exists("", resource)) {
            resources.getAssets.open(resource)
        } else {
            Environment.path.find(path => exists(path, resource)) match {
                case path:Some[String] => { resources.getAssets.open("%s/%s".format(path.get,resource)) }
                case None => { throw new IOException("cannot open environment resource %s".format(resource)) }
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
