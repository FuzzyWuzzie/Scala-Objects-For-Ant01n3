package org.sofa

import java.io.{File, IOException}

/** Utility to find files inside a set of pathes. */
trait FileLoader {
	/** Locate a file inside one of the given pathes, or at the current working directory.
	  * If found, return a file, else throw an IOException. */
	def findFile(resource:String, paths:Seq[String]):File = {
        var file = new File(resource)

        if(file.exists) {
            file
        } else {
            val sep = sys.props.get("file.separator").get
            
            //paths.foreach { path => printf("Searching for %s%n", new File("%s%s%s".format(path, sep, resource)))}

            paths.find(path => (new File("%s%s%s".format(path, sep, resource))).exists) match {
                case path:Some[String] => { new File("%s%s%s".format(path.get,sep,resource)) }
                case None => { throw new IOException("cannot locate %s (path = %s)".format(resource, paths.mkString(":"))) }
            }
        }
    }

    /** Try to find a path inside one of the given pathes, or at the current working directory.
      * If found, return a string representing the path with the resource, else throw an
      * IOException. */
    def findPath(resource:String, paths:Seq[String]):String = {
    	var res  = resource
    	var file = new File(res)

        if(!file.exists) {
            val sep = sys.props.get("file.separator").get

            //paths.foreach { path => printf("Searching for %s%n", new File("%s%s%s".format(path, sep, res)))}

            paths.find(path => (new File("%s%s%s".format(path, sep, res))).exists) match {
                case path:Some[String] => { res = "%s%s%s".format(path.get, sep, res) }
                case None => { throw new IOException("cannot locate %s (path = %s)".format(res, paths.mkString(":"))) }
            }
        }  

        res
    }

}