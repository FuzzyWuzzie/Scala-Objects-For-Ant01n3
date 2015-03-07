package org.sofa.backend

import java.io.{InputStream}
import org.sofa.FileLoader
import android.content.res.Resources


class AndroidFileLoader(val resources:Resources) extends FileLoader with AndroidLoader {
	def open(resource:String):InputStream = {
		resources.getAssets.open(searchInAssets(resource, FileLoader.path))
	}
}