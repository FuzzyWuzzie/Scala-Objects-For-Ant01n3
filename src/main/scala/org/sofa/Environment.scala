package org.sofa

import java.lang.reflect.Method
import java.io.{File, FileNotFoundException, InputStream, FileInputStream, IOException}
import scala.collection.mutable.{HashMap, ArrayBuffer}
import scala.io.Source


/** Representation of a set of parameters that can be read from the command line
  * or configuration files and automatically stored in objects.
  * 
  * The environment is a class that represents a set of key-value pairs. */
class Environment {
// Attribute
  
	/** The set of parameters. */
	protected val parameters = new HashMap[String,String]
 
	/** Name of the default configuration file. */
	protected val configFileName = "config"

// Attribute
 
	protected final val cmdSimpleParamRE = "-([^-=]+)".r
	protected final val cmdEqualParamRE  = "-?([^-=]+)=(.+)".r
	protected final val fileEqualParamRE = "\\s*([^#=\\s]+)\\s*=\\s*([^\\s]+)\\s*".r
	protected final val fileCommentRE    = "\\s*#(.*)\\s*".r
	protected final val fileEmptyRE      = "(\\s*)".r

// Access

 	/** Number of parameters usable. */
 	def size:Int = parameters.size
  
 	/** A parameter accessed by its key. */
 	def parameter(key:String):Option[String] = parameters.get( key )
  
    /** A parameter cast as a boolean if possible (when impossible, false is returned). */
 	def booleanParameter(key:String):Boolean = parameters.get(key) match {
		case None      => false
		case x:Some[_] => asBoolean(x.get)
	}
  
	/** A parameter cast as an integer if possible (when impossible, 0 is returned). */
	def integerParameter(key:String):Int = try {
		parameters.get(key) match {
		  case None => 0
		  case x:Some[_] => (x.get).toInt
		}
	} catch {
		case e:NumberFormatException => 0
	}
 
	/** A parameter cast as a double if possible (when impossible, NaN is returned). */
	def realParameter(key:String):Double = try {
		parameters.get(key) match {
			case None      => Double.NaN
			case x:Some[_] => (x.get).toDouble
		}
	} catch {
		case e:NumberFormatException => Double.NaN
	}
 
// Commands

	/** Read a configuration file from an existing input stream and process each field trying to store them as parameters.
      * If a line cannot be read, it is merely ignored, no exception is thrown. This behaviour has
      * been chosen to be as error tolerant as possible. */
	def readConfigFile(input:InputStream) { readConfigFile(Source.fromInputStream(input)) }
 
	/** Read a configuration file from a file name and process each field trying to store them as parameters.
      * If a line cannot be read, it is merely ignored, no exception is thrown. This behaviour has
      * been chosen to be as error tolerant as possible. The fileName must match a resource that is available
      * in the current environment path. It depends on the default loader of the environment how the resources
      * are searched. By default, files are searched on the local file system, you can either pass a fully
      * qualified path name, or the name of something in the environment path. */
	def readConfigFile(fileName:String) {
		try {
			readConfigFile(Source.fromInputStream(Environment.loader.open(fileName))) 
		} catch {
			case e:FileNotFoundException => { printf("Environment: command file %s not found%n", fileName) }
		}
	}

	/** Read a configuration file from an existing source and process each field trying to store them as parameters.
      * If a line cannot be read, it is merely ignored, no exception is thrown. This behaviour has
      * been chosen to be as error tolerant as possible. */
	def readConfigFile(source:Source) {
		try {
			source.getLines.foreach {
				case fileEqualParamRE(key,value) => parameters.put(key, value)
				case fileCommentRE(value)        => { /* ignore */ }
				case fileEmptyRE(value)          => { /* ignore */ }
				case value:String                => printf("Environment: line %s cannot be parsed%n", value)
			}
		} catch {
			case e:Exception => { printf("Environment: %s%n", e.getMessage); e.printStackTrace }
			case e:AnyRef => { throw e }
		}

	}
 
	/** Read the command line and process each field, trying to store them as parameters. */
	def readCommandLine(args:Array[String]) {
		args.foreach {
			case cmdEqualParamRE(key:String,value) => parameters.put(key, value)
			case cmdSimpleParamRE(key)             => parameters.put(key, "true")
			case word:String                       => readConfigFile(word)
		}
	}
 
    /** Output all the defined parameters to the given output stream. */
	def printParameters(out:java.io.PrintStream) {
    	parameters.foreach { out.printf("%s%n", _) }
    }
 
 	/** Initialise each field of the given object that have a match in the actual
      * parameter set. */
 	def initializeFieldsOf(thing:AnyRef) {
 		thing.getClass.getMethods.foreach { method =>
 			var name = method.getName

 			if(name.endsWith("_$eq")) {	// The Scala way.
	 			
	 			name = name.substring(0, name.length-4)
     
	 			val types:Array[Class[_]] = method.getParameterTypes
	    
	 			if(types.length == 1) parameters.get(name) match {
	 				case Some(value) => invokeSetMethod(thing, method, types(0).getName, value)
	 				case None => {}
	 			}
 			}
 		}
 	}
  
    /** Invoke the given setter `method` with the given `value` cast as `atype` on `thing`. */
 	protected def invokeSetMethod( thing:AnyRef, method:Method, atype:String, value:String ) {
 	  // TODO not as clean as I would like it to be, since it uses string for the type names,
 	  // How to do this cleanly in Scala since the types to test are parameterised and type
 	  // parameters are erased ?
 	  try {
 	    atype match {
 	      case "boolean"          => { method.invoke(thing, asBoolean( value ).asInstanceOf[Object] ) }
 	      case "short"            => { method.invoke(thing, value.toShort.asInstanceOf[Object]      ) }
 	      case "int"              => { method.invoke(thing, value.toInt.asInstanceOf[Object]        ) }
 	      case "long"             => { method.invoke(thing, value.toLong.asInstanceOf[Object]       ) }
          case "float"            => { method.invoke(thing, value.toFloat.asInstanceOf[Object]      ) }
          case "double"           => { method.invoke(thing, value.toDouble.asInstanceOf[Object]     ) }
          case "java.lang.String" => { method.invoke(thing, value) }
          case _                  => { printf("Environment: cannot set field %s of type %s%n", method.getName, atype) }
 	    }
 	  } catch {
 	    case e:NumberFormatException => { printf("Environment: cannot set field %s value %s is not %s%n",
 	    	method.getName, value, atype) }
 	  }
 	}
  
 	/** Several ways to convert a string to a boolean. */
 	protected def asBoolean(value:String):Boolean = value.toLowerCase match {
 	    case "1"    => true
 	    case "true" => true
 	    case "on"   => true
 	    case "yes"  => true
 	    case _      => false
 	}
}

/** A singleton environment that plays the role of default environment. */
object Environment extends Environment {
	/** Path to look for configuration files. */
	var path = new ArrayBuffer[String]()

	var loader:EnvironmentLoader = new DefaultEnvironmentLoader()
}

trait EnvironmentLoader {
	def open(resource:String):InputStream 
}

class DefaultEnvironmentLoader extends EnvironmentLoader {
    def open(resource:String):InputStream = {
        var file = new File(resource)
        if(file.exists) {
            new FileInputStream(file)
        } else {
            val sep = sys.props.get("file.separator").get

            Environment.path.find(path => (new File("%s%s%s".format(path, sep, resource))).exists) match {
                case path:Some[String] => {new FileInputStream(new File("%s%s%s".format(path.get,sep,resource))) }
                case None => { throw new IOException("cannot locate environment file %s".format(resource)) }
            }
        }
    }
}