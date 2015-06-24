package org.sofa

import java.io.{InputStream, File, FileInputStream, IOException}


/** Utility to find files inside a set of pathes. */
trait Loader {
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
                case None => throw new IOException("cannot locate %s (path = %s)".format(resource, paths.mkString(":")))
            }
        }
    }

    /** Locate a file or resource inside one of the given paths, or at the current working directory.
      * If fond, return a, input stream, else throw an IOException. */
    def findStream(resource:String, paths:Seq[String]):InputStream = {
        var file = new File(resource)

        if(file.exists) {
            new FileInputStream(file)
        } else {
            val sep = sys.props.get("file.separator").get
            
            //paths.foreach { path => printf("Searching for %s%n", new File("%s%s%s".format(path, sep, resource)))}

            paths.find(path => (new File("%s%s%s".format(path, sep, resource))).exists) match {
                case path:Some[String] => { new FileInputStream(new File("%s%s%s".format(path.get,sep,resource))) }
                case None => { 
                	// Try in the resources directory
                	paths.find { path => 
                		(getClass.getResource("%s%s%s".format(path, sep, resource)) ne null)
                	} match {
                		case None => throw new IOException("cannot locate %s (path = %s)".format(resource, paths.mkString(":")))
                		case path:Some[String] => { 
	                		getClass.getResource("%s%s%s".format(path.get, sep, resource)).openStream
                		}
                	}
                }
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