package org.sofa.gfx.backend

import org.sofa.gfx.io.collada.{ColladaLoader,ColladaFile}
import scala.xml.{XML, NodeSeq}
import android.content.res.Resources
import org.sofa.backend.AndroidLoader

class AndroidColladaLoader(val resources:Resources) extends ColladaLoader with AndroidLoader {
    def open(resource:String):NodeSeq = {
    	scala.xml.XML.load(resources.getAssets.open(searchInAssets(resource, ColladaFile.path))).child
    }
}