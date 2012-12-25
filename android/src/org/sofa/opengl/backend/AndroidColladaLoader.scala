package org.sofa.opengl.backend

import org.sofa.opengl.io.collada.{ColladaLoader,ColladaFile}
import scala.xml.{XML, NodeSeq}
import android.content.res.Resources

class AndroidColladaLoader(val resources:Resources) extends ColladaLoader with AndroidLoader {
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
}