package org.sofa.opengl

import java.nio.{ByteBuffer=>NioByteBuffer, FloatBuffer=>NioFloatBuffer, DoubleBuffer=>NioDoubleBuffer, IntBuffer=>NioIntBuffer}
import javax.media.opengl.glu._
import javax.media.opengl._
import org.sofa.nio._
import GL._
import GL2._
import GL2GL3._
import GL2ES2._
import GL3._ 
import java.awt.Color

/** Wrapper around a GL version 3.1 instance.
  * 
  * The goal is to provide an easy access to some OpenGL methods, facilitating the use of NIO
  * buffers for example.
  */
class SGL(val gl:GL3, val glu:GLU)  {
	private[this] val ib1 = NioIntBuffer.allocate(1)
	
	import gl._
	import glu._
	
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
	def vertexAttribPointer(number:Int, size:Int, typ:Int, b:Boolean, i:Int, j:Int) = glVertexAttribPointer(number, size, typ, b, i, j)
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

	def bindBuffer(mode:Int, id:Int) = glBindBuffer(mode, id)

// Shaders
	
	def getShaderCompileStatus(id:Int):Boolean = {
	    glGetShaderiv(id, GL_COMPILE_STATUS, ib1)
	    ib1.get(0) == GL_TRUE
	}
	
	def getShaderInfoLogLength(id:Int):Int = {
	    glGetShaderiv(id, GL_INFO_LOG_LENGTH, ib1)
	    ib1.get(0)
	}
	
	def getShaderInfoLog(id:Int):String = {
		val len = getShaderInfoLogLength(id)
		val data = NioByteBuffer.allocate(len)
	    gl.glGetShaderInfoLog(id, len, ib1, data)
	    new String(data.array)
	}
	
	def shaderSource(id:Int, source:Array[String]) = glShaderSource(id, source.size, source, null)
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
    def uniform(loc:Int, color:Color) = glUniform4f(loc, color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, color.getAlpha()/255f)
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
//    def uniform(loc:Int, v:FloatBuffer) {
//        v.size match {
//            case 1 => glUniform1fv(loc, 1, v.buffer)
//            case 2 => glUniform2fv(loc, 2, v.buffer)
//            case 3 => glUniform3fv(loc, 3, v.buffer)
//            case 4 => glUniform4fv(loc, 4, v.buffer)
//            case _ => throw new RuntimeException("uniform with more than 4 values?")
//        }
//    }
//    def uniform(loc:Int, v:DoubleBuffer) {
//        v.size match {
//            case 1 => glUniform1dv(loc, 1, v.buffer)
//            case 2 => glUniform2dv(loc, 2, v.buffer)
//            case 3 => glUniform3dv(loc, 3, v.buffer)
//            case 4 => glUniform4dv(loc, 4, v.buffer)
//            case _ => throw new RuntimeException("uniform with more than 4 values?")
//        }
//    }

// Basic API
	
	def getString(i:Int) = glGetString(i)
    def clear(mode:Int) = glClear(mode)
	def clearColor(r:Float, g:Float, b:Float, a:Float) = glClearColor(r, g, b, a)
	def clearColor(color:Color) = glClearColor(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, color.getAlpha()/255f)
    def clearDepth(value:Float) = glClearDepth(value)
    def viewport(x:Int, y:Int, width:Int, height:Int) = glViewport(x, y, width, height)
    def enable(i:Int) = glEnable(i)
    def cullFace(i:Int) = glCullFace(i)
    def frontFace(i:Int) = glFrontFace(i)
    def lineWidth(width:Float) = glLineWidth(width)
    def lineWidth(width:Double) = glLineWidth(width.toFloat)
    def blendEquation(mode:Int) = glBlendEquation(mode)
    def blendFunc(src:Int, dst:Int) = glBlendFunc(src, dst)
    def polygonMode(face:Int, mode:Int) = glPolygonMode(face, mode)

    def pixelStore(param:Int, value:Int) = glPixelStorei(param, value)
    
// Utilities
    
    /** Display the OpenGL version, the GLSL version, the renderer name, and renderer vendor
      * on standard output.
      */
    def printInfos() {
	    println("OpenGL version  %s".format(getString(GL_VERSION)))
        println("       glsl     %s".format(getString(GL_SHADING_LANGUAGE_VERSION)))
        println("       renderer %s".format(getString(GL_RENDERER)))
        println("       vendor   %s".format(getString(GL_VENDOR)))
	}
	
	/** Check any potential error recorded by the GL.
	  * 
	  * If an error flag is found, a runtime exception is raised.
	  */
	def checkErrors() {
	    val err = glGetError
	    if(err != GL_NO_ERROR) {
	        throw new RuntimeException(gluErrorString(err))
	    }
	}
}
