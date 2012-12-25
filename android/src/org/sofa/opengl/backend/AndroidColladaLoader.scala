package org.sofa.opengl.backend

import org.sofa.opengl.io.collada.{ColladaLoader,ColladaFile}
import scala.xml.{XML, NodeSeq}
import android.content.res.Resources

class AndroidColladaLoader(val resources:Resources) extends ColladaLoader with AndroidLoader {
    def open(resource:String):NodeSeq = {
    	scala.xml.XML.load(searchInAssets(resource, ColladaFile.path)).child
    }
}