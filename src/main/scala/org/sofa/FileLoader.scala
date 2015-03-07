package org.sofa

import java.io.{InputStream, FileInputStream, File}
import scala.collection.mutable.ArrayBuffer


object FileLoader {
	val path = new ArrayBuffer[String]()

	var loader:FileLoader = new DefaultFileLoader()
}


trait FileLoader {
	def open(resource:String):InputStream
}


class DefaultFileLoader extends FileLoader with Loader {
	def open(resource:String):InputStream = {
		new FileInputStream(findFile(resource, FileLoader.path))
	}
}