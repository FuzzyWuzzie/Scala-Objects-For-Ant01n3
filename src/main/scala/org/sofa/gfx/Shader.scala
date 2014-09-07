package org.sofa.gfx

import java.io.{File, InputStream, FileInputStream, IOException}
import scala.collection.mutable._
import org.sofa.math._
import org.sofa.nio._
import org.sofa.FileLoader

case class ShaderCompilationException(msg:String, nested:Throwable=null) extends Exception(msg, nested)
case class ShaderLinkException(msg:String, nested:Throwable=null) extends Exception(msg, nested)
case class ShaderAttributeException(msg:String, nested:Throwable=null) extends Exception(msg, nested)


/** Shader companion object.
  *
  * This contains methods to convert shaders to and from version 100 to version 300 in
  * order to work with Desktop and ES 2.0 mobile versions. The convertion naturally
  * works only for the common subset of features. */
object Shader {
	var convertToCurrentShaderVersion = true

	var currentlyBound:ShaderProgram = null

    /** A regular expression that matches any line that contains an include. */
    val IncludeMatcher = "#include\\s+<([^>]+)>\\s*".r

    /** A regular expression that matches the version and catches the number. */
    val VersionMatcher = """#version\s+([0-9]+)\s*""".r

    val AttributeMatcher = """\s*attribute\s+(.+)""".r

    val VaryingMatcher = """\s*varying\s+(.+)""".r

    val InMatcher = """\s*in\s+(.+)""".r

    val OutMatcher = """\s*out\s+(.+)""".r

    /** The set of paths to try to load a shader. */
    val path = scala.collection.mutable.ArrayBuffer[String]()

    /** A way to load a shader source. */
    var loader:ShaderLoader = new DefaultShaderLoader()
  
    /** Transform a text file into an array of strings.
      *
      * This version understands a "#include <file>" directive that will include the content of
      * another file into the text of the shader. */
    def fileToArrayOfStrings(gl:SGL, file:String, shaderType:Int):Array[String] = {
        streamToArrayOfStrings(gl, loader.open(file), shaderType)
    }
    /** Transform a text file from a stream into an array of strings, one cell in the array per line.
      * 
      * This version understands a "#include <file>" directive that will include the content of
      * another file into the text of the shader. */
    def streamToArrayOfStrings(gl:SGL, in:InputStream, shaderType:Int):Array[String] = {
    	if(convertToCurrentShaderVersion)
    	     streamToArrayOfStringsConvert(gl, in, shaderType, 0)
    	else streamToArrayOfStringsSimple(gl, in, shaderType)
	}

	/** Transform a text file from a stream into an array of strings, one cell in the array per line 
	  * and convert each line so that the shader uses the syntax corresponding to the actual GLSL
	  * version of the drivers.
	  *
	  * This operation, maps the "#version" at the start of the shader to the real one,
	  * and apply several transforms to use the correct syntax.
      * 
      * This version understands a "#include <file>" directive that will include the content of
      * another file into the text of the shader. */
	def streamToArrayOfStringsConvert(gl:SGL, in:InputStream, shaderType:Int, depth:Int):Array[String] = { 
        val buf = new scala.collection.mutable.ArrayBuffer[String]
        val src = new scala.io.BufferedSource(in)
        val version = gl.ShaderVersion match {
        	case VersionMatcher(number) => number.toInt
        	case _ => throw new RuntimeException("cannot interpret gl context shader version %s".format(gl.ShaderVersion))
        }

        src.getLines.foreach { line =>
        	line match {
        		case IncludeMatcher(fileName) => {
	        	    buf ++= streamToArrayOfStringsConvert(gl, loader.open(fileName), shaderType, depth+1)
        		}
        		case VersionMatcher(number) => {
        			buf += "%s%n".format(gl.ShaderVersion)
        		}
        		case AttributeMatcher(restOfLine) => {
        			if(version < 130)
        			     buf += "%s%n".format(line)
        			else buf += "in %s%n".format(restOfLine)
        		}
        		case VaryingMatcher(restOfLine) => {
        			if(version < 130)
        			     buf += "%s%n".format(line)
        			else {
        				shaderType match {
        					case gl.FRAGMENT_SHADER => buf += "in %s%n".format(restOfLine)
        					case _                  => buf += "out %s%n".format(restOfLine)

        				}
        			}
        		}
        		case InMatcher(restOfLine) => {
        			if(version >= 130)
        			     buf += "%s%n".format(line)
        			else buf += "attribute %s%n".format(restOfLine)
        		}
        		case OutMatcher(restOfLine) => {
        			if(version >= 130)
        			     buf += "%s%n".format(line)
        			else buf += "varying %s%n".format(restOfLine)
        		}
        		case _ => {
        			if(shaderType == gl.FRAGMENT_SHADER) {
        				if(version >= 130) {
        					var l:String = null
        					l = line.replace("gl_FragColor", "mgl_FragColor")
        					l = l.replace("texture2D", "texture")
        					buf += "%s%n".format(l)
        				} else {
        					buf += "%s%n".format(line)
        				}
        			} else {
        				buf += "%s%n".format(line)
        			}
        		}
        	}
        }

        if(depth == 0 && shaderType == gl.FRAGMENT_SHADER) {
        	if(version >= 130) {
        		buf.insert(1, "out vec4 mgl_FragColor;%n".format())
        	}
        }
        if(version < 130 && depth == 0) {
        	buf.insert(1, "precision mediump float;%n".format())
        	buf.insert(1, "precision mediump int;%n".format())
        }

        // Console.err.println("** %s:".format(if(shaderType==gl.VERTEX_SHADER) "vertex shader" else "fragment shader"))
        // buf.foreach {line => Console.err.print("    %s".format(line))}

        buf.toArray
    }

    /** Transform a text file from a stream into an array of strings, one cell in the array per line.
      * 
      * This version understands a "#include <file>" directive that will include the content of
      * another file into the text of the shader. */
    def streamToArrayOfStringsSimple(gl:SGL, in:InputStream, shaderType:Int):Array[String] = { 
        val buf = new scala.collection.mutable.ArrayBuffer[String]
        val src = new scala.io.BufferedSource(in)

        src.getLines.foreach { line =>
        	if(line.startsWith("#include")) {
        	    val fileName = line match {
        	        case IncludeMatcher(file) => file 
        	        case _                    => throw new RuntimeException("invalid include statement '%s'".format(line))
        	    }
        	    buf ++= streamToArrayOfStrings(gl, loader.open(fileName), shaderType)
        	} else {
        		buf += "%s%n".format(line)
        	}
        }

        buf.toArray
    }
}


/** Pluggable loader for shader sources. */
trait ShaderLoader extends FileLoader {
    /** Try to open a resource, or throw an IOException if not available. */
    def open(resource:String):InputStream
}


/** Default loader for shaders, based on files and the include path.
  * This loader tries to open the given resource directly, then if not
  * found, tries to find it in each of the pathes provided by the include
  * path. If not found it throws an IOException. */
class DefaultShaderLoader extends ShaderLoader {
    def open(resource:String):InputStream = {
    	new FileInputStream(findFile(resource, Shader.path))
    }
}


/** ShaderProgram companion object. */
object ShaderProgram {
    /** Create a new shader program from a vertex shader file and a fragment shader file. */
    def apply(gl:SGL, name:String, vertexShaderFileName:String, fragmentShaderFileName:String):ShaderProgram = {
    	new ShaderProgram(gl, name,
                new VertexShader(gl, name, vertexShaderFileName),
    			new FragmentShader(gl, name, fragmentShaderFileName))
    }

    /** Create a new shader program from a vertex shader source, and a fragment shader source. */
    def apply(gl:SGL, name:String, vertexShaderStrings:Array[String], fragmentShaderStrings:Array[String]):ShaderProgram = {
    	new ShaderProgram(gl, name,
    			new VertexShader(gl, name, vertexShaderStrings),
    			new FragmentShader(gl, name, fragmentShaderStrings))
    }
}


/** Represents a shader, either vertex, fragment or geometry.
 *  
 * @param gl The SGL instance.
 * @param source An array of lines of code. 
 */
abstract class Shader(gl:SGL, val name:String, val source:Array[String]) extends OpenGLObject(gl) {
    import gl._

    /** Kind of shader, vertex, fragment or geometry ? */
    protected val shaderType:Int
    
    /** Upload the source, compile it and check errors. */
    protected def init() {
        checkErrors
        super.init(createShader(shaderType))
        checkErrors
        shaderSource(oid, source)
        checkErrors
        compileShader(oid)
        checkErrors
        
        if(!getShaderCompileStatus(oid)) {
            val log = getShaderInfoLog(oid)

            val logLines     = log.split("\n")
            val errMatch     = ".*".r
            val osxMatch     = "ERROR:\\s*(\\d+):\\s*(\\d+):(.*)".r
            val androidMatch = "(\\d+):(\\d+):[^:]+:(.*)".r

            printErrorHeader(logLines.length)

            logLines.foreach { line =>
            	errMatch.findFirstIn(line) match {
            		case Some(osxMatch(col,line,msg))     => printError(col.toInt, line.toInt-1, msg)
            		case Some(androidMatch(col,line,msg)) => printError(col.toInt, line.toInt-1, msg)
            		case Some(other)                      => Console.err;println("Error : %s (%s)".format(line, other))
            		case None                             => Console.err.println("Error : %s".format(line))
            	}
            }
//        	Console.err.println(log)
        	throw ShaderCompilationException("Cannot compile shader %s (see log above)".format(name))
        }
    }
    
    /** Release the shader. */
    override def dispose() {
        checkId
        deleteShader(oid)
        super.dispose
    }

    protected def printErrorHeader(count:Int) {
    	Console.err.println("Found %d errors in shader '%s' (%s):".format(count, name, shaderType))
    }

    protected def printError(col:Int, line:Int, msg:String) {
    	Console.err.println("%4d:%4d > %s".format(col,line,msg))
    	if(line>0)
    		Console.err.print("          |   %s".format(source(line-1)))
    	Console.err.print("          | * %s".format(source(line)))
    	if(line+1 < source.length)
    		Console.err.print("          |   %s".format(source(line+1)))    	
    }
}


/** A vertex shader/ */
class VertexShader(gl:SGL, name:String, source:Array[String]) extends Shader(gl, name, source) {
    protected val shaderType = gl.VERTEX_SHADER
    
    init
    
    /** Try to open the given `sourceFile` on the file system and compile it. */
    def this(gl:SGL, name:String, fileSource:String) = this(gl, name, Shader.fileToArrayOfStrings(gl, fileSource, gl.VERTEX_SHADER))
    
    /** Try to read a shader source from the given input `stream` and compile it. */
    def this(gl:SGL, name:String, stream:java.io.InputStream) = this(gl, name, Shader.streamToArrayOfStrings(gl, stream, gl.VERTEX_SHADER))
}


/** A fragment shader. */
class FragmentShader(gl:SGL, name:String, source:Array[String]) extends Shader(gl, name, source) {
    protected val shaderType = gl.FRAGMENT_SHADER
    
    init
    
    /** Try to open the given `sourceFile` on the file system and compile it. */
    def this(gl:SGL, name:String, fileSource:String) = this(gl, name, Shader.fileToArrayOfStrings(gl, fileSource, gl.FRAGMENT_SHADER))

    /** Try to read a shader source from the given input `stream` and compile it. */
    def this(gl:SGL, name:String, stream:java.io.InputStream) = this(gl, name, Shader.streamToArrayOfStrings(gl, stream, gl.FRAGMENT_SHADER))
}


/** Composition of several shaders into a program. */
class ShaderProgram(gl:SGL, val name:String, shdrs:Shader*) extends OpenGLObject(gl) {
    import gl._
    
    /** Set of shaders. */
    protected val shaders = shdrs.toArray
    
    /** Locations of each uniform variable in the shader. */
    protected val uniformLocations = new HashMap[String, AnyRef]
    
    /** Location of each vertex attribute variable in the shader. */
    protected val attributeLocations = new HashMap[String, Int]
    
    init
    
    protected def init() {
        super.init(createProgram)
        shaders.foreach { shader => attachShader(oid, shader.id) }
        linkProgram(oid)
        
        if(! getProgramLinkStatus(oid)) {
            val log = "error"
            //val log = getProgramInfoLog(oid)
            //Console.err.println(log)
            throw ShaderLinkException("Cannot link shaders program %s:%n%s".format(name, log))
        }
        
        checkErrors
    }
    
    def use() {
//    	if(Shader.currentlyBound ne this) {
        	checkId
       		useProgram(oid)
  //     		Shader.currentlyBound = this
   // 	}
    }
    
    override def dispose() {
        checkId
        useProgram(null)
        shaders.foreach { shader =>
            detachShader(oid, shader.id)
            shader.dispose
        }
        deleteProgram(oid)
        super.dispose
    }
    
    def uniform(variable:String, value:Int) {
        checkId
        val loc = getUniformLocation(variable)
        gl.uniform(loc, value)
        checkErrors
    }
    
    def uniform(variable:String, value:Float) {
        checkId
        val loc = getUniformLocation(variable)
        gl.uniform(loc, value)
        checkErrors
    }
    
    def uniform(variable:String, value1:Int, value2:Int, value3:Int) {
        checkId
        val loc = getUniformLocation(variable)
        gl.uniform(loc, value1, value2, value3)
        checkErrors
    }
    
    def uniform(variable:String, value1:Float, value2:Float, value3:Float) {
        checkId
        val loc = getUniformLocation(variable)
        gl.uniform(loc, value1, value2, value3)
        checkErrors
    }
    
    def uniform(variable:String, value1:Float, value2:Float, value3:Float, value4:Float) {
        checkId
        val loc = getUniformLocation(variable)
        gl.uniform(loc, value1, value2, value3, value4)
        checkErrors
    }
    
    def uniform(variable:String, color:Rgba) {
        checkId
        val loc = getUniformLocation(variable)
        gl.uniform(loc, color)
        checkErrors
    }
    
    def uniform(variable:String, v:Array[Float]) {
        checkId
        gl.uniform(getUniformLocation(variable), v)
        checkErrors
    }
    
    def uniform(variable:String, v:Array[Double]) {
        checkId
        gl.uniform(getUniformLocation(variable), v)
        checkErrors
    }

    def uniform(variable:String, v:FloatBuffer) {
        checkId
        gl.uniform(getUniformLocation(variable), v)
        checkErrors
    }
    
    def uniform(variable:String, v:DoubleBuffer) {
        checkId
        gl.uniform(getUniformLocation(variable), v)
        checkErrors
    }
    
    def uniform(variable:String, v:NumberSeq) {
        checkId
        gl.uniform(getUniformLocation(variable), v.toFloatArray)	// Cannot pass Nio double or float arrays yet.
        checkErrors
    }

    // def uniformMatrix(variable:String, matrix:Matrix4#ReturnType) {
    //     uniformMatrix(variable, matrix.toFloatBuffer)	// Matrices in shaders are made of floats
    //     checkErrors										// No way to use a double !!
    // }

    def uniformMatrix(variable:String, matrix:Matrix4) {
        uniformMatrix(variable, matrix.toFloatArray)    // It seems that using regular arrays is faster !
//        uniformMatrix(variable, matrix.toFloatBuffer)	// Matrices in shaders are made of floats
        checkErrors										// No way to use a double !!
    }

    def uniformMatrix(variable:String, matrix:Matrix3) {
        uniformMatrix(variable, matrix.toFloatArray)
//        uniformMatrix(variable, matrix.toFloatBuffer)
        checkErrors
    }

    def uniformMatrix(variable:String, matrix:FloatBuffer) {
        checkId
        val loc = getUniformLocation(variable)
        if(matrix.size == 9)
            uniformMatrix3(loc, 1, false, matrix)
        else if(matrix.size == 16)
            uniformMatrix4(loc, 1, false, matrix)
        else throw ShaderAttributeException("matrix must be 9 (3x3) or 16 (4x4) floats");
    }

    def uniformMatrix(variable:String, matrix:Array[Float]) {
        checkId
        val loc = getUniformLocation(variable)
        if(matrix.length == 9)
            uniformMatrix3(loc, 1, false, matrix)
        else if(matrix.length == 16)
            uniformMatrix4(loc, 1, false, matrix)
        else throw ShaderAttributeException("matrix must be 9 (3x3) or 16 (4x4) floats");
    }

    def uniformTexture(texture:Texture, uniformName:String) { uniformTexture(gl.TEXTURE0, texture, uniformName) }

    def uniformTexture(textureUnit:Int, texture:Texture, uniformName:String) {
    	val pos = textureUnit match {
			case gl.TEXTURE0 => 0
			case gl.TEXTURE1 => 1
			case gl.TEXTURE2 => 2
			case _ => throw new RuntimeException("cannot handle more than 3 texture unit yet")
		}

		activeTexture(textureUnit)
		texture.bind
		uniform(uniformName, pos)
    }
    
    def getAttribLocation(variable:String):Int = {
        var loc = attributeLocations.get(variable).getOrElse {
        	checkId
        	useProgram(oid)
            val l = gl.getAttribLocation(oid, variable) 
            checkErrors
            if(l >= 0) {
            	attributeLocations.put(variable, l)
            } else {
            	throw ShaderAttributeException("Cannot find attribute %s in program %s".format(variable, name))
            }
            l
        }
        loc
    }
    
    def getUniformLocation(variable:String):AnyRef = {
        val loc = uniformLocations.get(variable).getOrElse {
        	checkId
        	useProgram(oid)
            var l = gl.getUniformLocation(oid, variable)
            checkErrors
            if(l ne null) {
            	uniformLocations.put(variable, l)
            } else {
            	throw ShaderAttributeException("Cannot find uniform %s in program %s".format(variable, name))
            }
            l
        }
        //uniformLocations.put(variable, loc)
        loc
    }
}