package org.sofa.opengl

import scala.collection.mutable._
import org.sofa.math._
import org.sofa.nio._

object Shader {
    def fileToArrayOfStrings(file:String):Array[String] = {
        val buf = new scala.collection.mutable.ArrayBuffer[String]
        val src = new scala.io.BufferedSource(new java.io.FileInputStream(file))
        src.getLines.foreach { line => buf += "%s%n".format(line) }
        buf.toArray
    }
}

abstract class Shader(gl:SGL, val source:Array[String]) extends OpenGLObject(gl) {
    import gl._

    def this(gl:SGL, sourceFile:String) = {
        this(gl, Shader.fileToArrayOfStrings(sourceFile))
    }
    
    protected val shaderType:Int
    
    protected def init() {
        super.init(createShader(shaderType))
        checkErrors
        shaderSource(oid, source)
        compileShader(oid)
        checkErrors
        
        if(!getShaderCompileStatus(oid)) {
        	Console.err.println(getShaderInfoLog(oid))
        	throw new RuntimeException("Cannot compile shader")
        }
    }
    
    override def dispose() {
        checkId
        deleteShader(oid)
        super.dispose
    }
}

class VertexShader(gl:SGL, source:Array[String]) extends Shader(gl, source) {
    protected val shaderType = gl.VERTEX_SHADER
    
    init
    
    def this(gl:SGL, fileSource:String) {
        this(gl, Shader.fileToArrayOfStrings(fileSource))
    }
}

class FragmentShader(gl:SGL, source:Array[String]) extends Shader(gl, source) {
    protected val shaderType = gl.FRAGMENT_SHADER
    
    init
    
    def this(gl:SGL, fileSource:String) {
        this(gl, Shader.fileToArrayOfStrings(fileSource))
    }
}

class ShaderProgram(gl:SGL, shdrs:Shader*) extends OpenGLObject(gl) {
    import gl._
    
    protected val shaders = shdrs.toArray
    
    protected val uniformLocations = new HashMap[String, Int]
    
    init
    
    protected def init() {
        super.init(createProgram)
        shaders.foreach { shader => attachShader(oid, shader.id) }
        linkProgram(oid)
        checkErrors
    }
    
    def use() {
        checkId
        useProgram(oid)
    }
    
    override def dispose() {
        checkId
        useProgram(0)
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

    def uniformMatrix(variable:String, matrix:Matrix4#ReturnType) {
        uniformMatrix(variable, matrix.toFloatBuffer)	// Matrices in shaders are made of floats
        checkErrors										// No way to use a double !!
    }

    def uniformMatrix(variable:String, matrix:Matrix4) {
        uniformMatrix(variable, matrix.toFloatBuffer)	// Matrices in shaders are made of floats
        checkErrors										// No way to use a double !!
    }

    def uniformMatrix(variable:String, matrix:Matrix3) {
        uniformMatrix(variable, matrix.toFloatBuffer)
        checkErrors
    }

    def uniformMatrix(variable:String, matrix:FloatBuffer) {
        checkId
        val loc = getUniformLocation(variable)
        if(matrix.size == 9)
            uniformMatrix3(loc, 1, false, matrix)
        else if(matrix.size == 16)
            uniformMatrix4(loc, 1, false, matrix)
        else throw new RuntimeException("matrix must be 9 (3x3) or 16 (4x4) floats");
    }
    
    def getUniformLocation(variable:String):Int = {
        checkId
        useProgram(oid)
        val loc = gl.getUniformLocation(oid, variable)
        checkErrors
        uniformLocations.put(variable, loc)
        loc
    }
}