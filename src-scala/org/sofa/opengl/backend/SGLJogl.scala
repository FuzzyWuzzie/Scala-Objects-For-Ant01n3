package org.sofa.opengl.backend

import org.sofa.opengl._
import org.sofa.math.Rgba
import java.nio.{Buffer,ByteBuffer=>NioByteBuffer, FloatBuffer=>NioFloatBuffer, DoubleBuffer=>NioDoubleBuffer, IntBuffer=>NioIntBuffer}
import javax.media.opengl.glu._
import javax.media.opengl._
import org.sofa.nio._
import GL._
import GL2._
import GL2GL3._
import GL2ES2._
import GL3._ 

/** Wrapper around a GL version 3.1 instance.
  * 
  * The goal is to provide an easy access to some OpenGL methods, facilitating the use of NIO
  * buffers for example.
  */
class SGLJogl(val gl:GL3, val glu:GLU) extends SGL {
	private[this] val ib1 = NioIntBuffer.allocate(1)
	
	import gl._
	import glu._

// Awful constants
	
	val DEPTH_TEST:Int = GL.GL_DEPTH_TEST
	val BLEND:Int = GL.GL_BLEND
    val SRC_ALPHA:Int = GL.GL_SRC_ALPHA
    val ONE_MINUS_SRC_ALPHA:Int = GL.GL_ONE_MINUS_SRC_ALPHA
    val CULL_FACE:Int = GL.GL_CULL_FACE
    val BACK:Int = GL.GL_BACK
    val CW:Int = GL.GL_CW
    val CCW:Int = GL.GL_CCW
    
    val COLOR_BUFFER_BIT:Int = GL.GL_COLOR_BUFFER_BIT
    val DEPTH_BUFFER_BIT:Int = GL.GL_DEPTH_BUFFER_BIT
    val FRONT_AND_BACK:Int = GL.GL_FRONT_AND_BACK
    val FILL:Int = GL2GL3.GL_FILL
    val LINE:Int = GL2GL3.GL_LINE
    
    val TEXTURE_2D:Int = GL.GL_TEXTURE_2D
    val TEXTURE0:Int = GL.GL_TEXTURE0
    val TEXTURE1:Int = GL.GL_TEXTURE1
    val TEXTURE2:Int = GL.GL_TEXTURE2
    val TEXTURE_MIN_FILTER:Int = GL.GL_TEXTURE_MIN_FILTER
    val TEXTURE_MAG_FILTER:Int = GL.GL_TEXTURE_MAG_FILTER
    val TEXTURE_WRAP_S:Int = GL.GL_TEXTURE_WRAP_S
    val TEXTURE_WRAP_T:Int = GL.GL_TEXTURE_WRAP_T
    val LINEAR_MIPMAP_LINEAR:Int = GL.GL_LINEAR_MIPMAP_LINEAR
    val LINEAR:Int = GL.GL_LINEAR
    val REPEAT:Int = GL.GL_REPEAT

    val UNSIGNED_BYTE:Int = GL.GL_UNSIGNED_BYTE
    val UNSIGNED_INT:Int = GL.GL_UNSIGNED_INT
    val FLOAT:Int = GL.GL_FLOAT
    val DOUBLE:Int = -1
    val RGBA:Int = GL.GL_RGBA

    val ELEMENT_ARRAY_BUFFER:Int = GL.GL_ELEMENT_ARRAY_BUFFER
    val ARRAY_BUFFER:Int = GL.GL_ARRAY_BUFFER
    val STATIC_DRAW:Int = GL.GL_STATIC_DRAW

    val VERTEX_SHADER:Int = GL2ES2.GL_VERTEX_SHADER
    val FRAGMENT_SHADER:Int = GL2ES2.GL_FRAGMENT_SHADER

    val TRIANGLES:Int = GL.GL_TRIANGLES

    val EXTENSIONS:Int = GL.GL_EXTENSIONS

// Info
    
    def isES:Boolean = false

// Vertex arrays
	
	def genVertexArray():Int = {
	    glGenVertexArrays(1, ib1)
	    ib1.get(0)
	}
	
	def deleteVertexArray(id:Int) {
	    ib1.put(0, id)
	    glDeleteVertexArrays(1, ib1)
	}
	
	def bindVertexArray(id:Int) = glBindVertexArray(id)
	def enableVertexAttribArray(id:Int) = glEnableVertexAttribArray(id)
	def disableVertexAttribArray(id:Int) = glDisableVertexAttribArray(id)
	def vertexAttribPointer(number:Int, attributeSize:Int, attributeType:Int, b:Boolean, size:Int, j:Int) = glVertexAttribPointer(number, attributeSize, attributeType, b, size, j)
	def vertexAttribPointer(number:Int, attributeSize:Int, attributeType:Int, b:Boolean, size:Int, data:Buffer) = glVertexAttribPointer(number, attributeSize, attributeType, b, size, data)
    def drawArrays(mode:Int, i:Int, size:Int) = glDrawArrays(mode, i, size)
    def drawElements(mode:Int, count:Int, i:Int, offset:Int) = glDrawElements(mode, count, i, offset)
    def multiDrawArrays(mode:Int, firsts:IntBuffer, counts:IntBuffer, primcount:Int) = glMultiDrawArrays(mode, firsts, counts, primcount)

// Textures
    
    def genTexture:Int = {
	    glGenTextures(1, ib1)
	    ib1.get(0)
	}
	
	def deleteTexture(id:Int) {
	    ib1.put(0, id)
	    glDeleteTextures(1, ib1)
	}
	
	def activeTexture(texture:Int) = glActiveTexture(texture)
	def bindTexture(target:Int, id:Int) = glBindTexture(target, id)
	def texParameter(target:Int, name:Int, param:Float) = glTexParameterf(target, name, param)
	def texParameter(target:Int, name:Int, param:Int) = glTexParameteri(target, name, param)
	def texParameter(target:Int, name:Int, params:FloatBuffer) = glTexParameterfv(target, name, params)
	def texParameter(target:Int, name:Int, params:IntBuffer) = glTexParameteriv(target, name, params)
	def texImage1D(target:Int, level:Int, internalFormat:Int, width:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = glTexImage1D(target, level, internalFormat, width, border, format, theType, data)
	def texImage2D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = glTexImage2D(target, level ,internalFormat, width, height, border, format, theType, data)
    def texImage3D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, depth:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = glTexImage3D(target, level, internalFormat, width, height, depth, border, format, theType, data)
    def generateMipmaps(target:Int) = glGenerateMipmap(target)
    
// Buffers
	
	def genBuffer():Int = {
	    glGenBuffers(1, ib1)
	    ib1.get(0)
	}
	
	def deleteBuffer(id:Int) {
	    ib1.put(0, id)
	    glDeleteBuffers(1, ib1)
	}

	def bufferData(target:Int, data:DoubleBuffer, mode:Int) {
	    gl.glBufferData(target, data.size*8, data.buffer, mode)
	}
	
	def bufferData(target:Int, data:Array[Double], mode:Int) {
	    bufferData(target, new DoubleBuffer(data), mode)
	}

	def bufferData(target:Int, data:FloatBuffer, mode:Int) {
	    gl.glBufferData(target, data.size*4, data.buffer, mode)
	}
	
	def bufferData(target:Int, data:Array[Float], mode:Int) {
	    bufferData(target, new FloatBuffer(data), mode)
	}

	def bufferData(target:Int, data:IntBuffer, mode:Int) {
	    gl.glBufferData(target, data.size*4, data.buffer, mode)
	}
	
	def bufferData(target:Int, data:Array[Int], mode:Int) {
	    bufferData(target, new IntBuffer(data), mode)
	}

	def bufferData(target:Int, data:NioBuffer, mode:Int) {
	    if(data.isByte) {
	        bufferData(target, data.asInstanceOf[ByteBuffer], mode)
	    } else if(data.isInt) {
	        bufferData(target, data.asInstanceOf[IntBuffer], mode)
	    } else if(data.isFloat) {
	        bufferData(target, data.asInstanceOf[FloatBuffer], mode)
	    } else if(data.isDouble) {
	        bufferData(target, data.asInstanceOf[DoubleBuffer], mode)
	    } else {
	        throw new RuntimeException("Unknown Nio buffer data type")
	    }
	}
	
	def bindBuffer(mode:Int, id:Int) = glBindBuffer(mode, id)

// Shaders
	
	def createShader(shaderType:Int):Int = glCreateShader(shaderType)
	
	def createProgram():Int = glCreateProgram()
	
	def getShaderCompileStatus(id:Int):Boolean = { getShader(id, GL_COMPILE_STATUS) == GL_TRUE }
	
	def getProgramLinkStatus(id:Int):Boolean = { getProgram(id, GL_LINK_STATUS) == GL_TRUE }
	
	def getShader(id:Int, status:Int):Int = {
	    glGetShaderiv(id, GL_INFO_LOG_LENGTH, ib1)
	    ib1.get(0)
	}
	
	def getShaderInfoLog(id:Int):String = {
		val len = getShader(id, GL_INFO_LOG_LENGTH)
		val data = NioByteBuffer.allocate(len)
	    gl.glGetShaderInfoLog(id, len, ib1, data)
	    new String(data.array)
	}
	
	def getProgram(id:Int, status:Int):Int = {
		glGetProgramiv(id, status, ib1)
		ib1.get(0)
	}
	
	def getProgramInfoLog(id:Int):String = {
	    val len = getProgram(id, GL_INFO_LOG_LENGTH)
	    val data = NioByteBuffer.allocate(len)
	    gl.glGetProgramInfoLog(id, len, ib1, data)
	    new String(data.array)
	}
	
	def shaderSource(id:Int, source:Array[String]) = glShaderSource(id, source.size, source, null)
	def shaderSource(id:Int, source:String) = glShaderSource(id, 1, Array[String](source), null)
	def compileShader(id:Int) = glCompileShader(id)
	def deleteShader(id:Int) = glDeleteShader(id)
    def attachShader(id:Int, shaderId:Int) = glAttachShader(id, shaderId)
    def linkProgram(id:Int) = glLinkProgram(id)
    def useProgram(id:Int) = glUseProgram(id)
    def detachShader(id:Int, shaderId:Int) = glDetachShader(id, shaderId)
    def deleteProgram(id:Int) = glDeleteProgram(id)
    def getUniformLocation(id:Int, variable:String):Int = glGetUniformLocation(id, variable)
    def uniform(loc:Int, i:Int) = glUniform1i(loc, i)
    def uniform(loc:Int, i:Int, j:Int) = glUniform2i(loc, i, j)
    def uniform(loc:Int, i:Int, j:Int, k:Int) = glUniform3i(loc, i, j, k)
    def uniform(loc:Int, i:Int, j:Int, k:Int, l:Int) = glUniform4i(loc, i, j, k, l)
    def uniform(loc:Int, i:Float) = glUniform1f(loc, i)
    def uniform(loc:Int, i:Float, j:Float) = glUniform2f(loc, i, j)
    def uniform(loc:Int, i:Float, j:Float, k:Float) = glUniform3f(loc, i, j, k)
    def uniform(loc:Int, i:Float, j:Float, k:Float, l:Float) = glUniform4f(loc, i, j, k, l)
    def uniform(loc:Int, color:Rgba) = glUniform4f(loc, color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
    def uniform(loc:Int, i:Double) = glUniform1d(loc, i)
    def uniform(loc:Int, i:Double, j:Double) = glUniform2d(loc, i, j)
    def uniform(loc:Int, i:Double, j:Double, k:Double) = glUniform3d(loc, i, j, k)
    def uniform(loc:Int, i:Double, j:Double, k:Double, l:Double) = glUniform4d(loc, i, j, k, l)
    def uniformMatrix3(loc:Int, i:Int, b:Boolean, buffer:FloatBuffer) = glUniformMatrix3fv(loc, i, b, buffer.buffer)
    def uniformMatrix3(loc:Int, i:Int, b:Boolean, buffer:DoubleBuffer) = glUniformMatrix3dv(loc, i, b, buffer.buffer)
    def uniformMatrix4(loc:Int, i:Int, b:Boolean, buffer:FloatBuffer) = glUniformMatrix4fv(loc, i, b, buffer.buffer)
    def uniformMatrix4(loc:Int, i:Int, b:Boolean, buffer:DoubleBuffer) = glUniformMatrix4dv(loc, i, b, buffer.buffer)
    def uniform(loc:Int, v:Array[Float]) {
        if(     v.size==1) uniform(loc, v(0))
        else if(v.size==2) uniform(loc, v(0), v(1))
        else if(v.size==3) uniform(loc, v(0), v(1), v(2))
        else if(v.size==4) uniform(loc, v(0), v(1), v(2), v(3))
    }
    def uniform(loc:Int, v:Array[Double]) {
        if(     v.size==1) uniform(loc, v(0).toFloat)
        else if(v.size==2) uniform(loc, v(0).toFloat, v(1).toFloat)
        else if(v.size==3) uniform(loc, v(0).toFloat, v(1).toFloat, v(2).toFloat)
        else if(v.size==4) uniform(loc, v(0).toFloat, v(1).toFloat, v(2).toFloat, v(3).toFloat)
    }
    def uniform(loc:Int, v:FloatBuffer) {
        v.size match {
            case 1 => uniform(loc, v(0))
            case 2 => uniform(loc, v(0), v(1))
            case 3 => uniform(loc, v(0), v(1), v(2))
            case 4 => uniform(loc, v(0), v(1), v(2), v(3))
            case _ => throw new RuntimeException("uniform with more than 4 values?")
        }
    }
    def uniform(loc:Int, v:DoubleBuffer) {
        v.size match {
            case 1 => uniform(loc, v(0))
            case 2 => uniform(loc, v(0), v(1))
            case 3 => uniform(loc, v(0), v(1), v(2))
            case 4 => uniform(loc, v(0), v(1), v(2), v(3))
            case _ => throw new RuntimeException("uniform with more than 4 values?")
        }
    }
    def getAttribLocation(id:Int, attribute:String):Int = glGetAttribLocation(id, attribute)

// Basic API
	
    def getError:Int = glGetError
	def getString(i:Int):String = glGetString(i)
    def clear(mode:Int) = glClear(mode)
	def clearColor(r:Float, g:Float, b:Float, a:Float) = glClearColor(r, g, b, a)
	def clearColor(color:Rgba) = glClearColor(color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
    def clearDepth(value:Float) = glClearDepth(value)
    def viewport(x:Int, y:Int, width:Int, height:Int) = glViewport(x, y, width, height)
    def enable(i:Int) = glEnable(i)
    def disable(i:Int) = glDisable(i)
    def cullFace(i:Int) = glCullFace(i)
    def frontFace(i:Int) = glFrontFace(i)
    def lineWidth(width:Float) = glLineWidth(width)
    def lineWidth(width:Double) = glLineWidth(width.toFloat)
    def blendEquation(mode:Int) = glBlendEquation(mode)
    def blendFunc(src:Int, dst:Int) = glBlendFunc(src, dst)
    def polygonMode(face:Int, mode:Int) = glPolygonMode(face, mode)

    def pixelStore(param:Int, value:Int) = glPixelStorei(param, value)
    
// Utilities
    
    def printInfos() {
	    println("OpenGL version  %s".format(getString(GL_VERSION)))
        println("       glsl     %s".format(getString(GL_SHADING_LANGUAGE_VERSION)))
        println("       renderer %s".format(getString(GL_RENDERER)))
        println("       vendor   %s".format(getString(GL_VENDOR)))
	}
	
	def checkErrors() {
	    val err = glGetError
	    if(err != GL_NO_ERROR) {
	        throw new RuntimeException(gluErrorString(err))
	    }
	}
}