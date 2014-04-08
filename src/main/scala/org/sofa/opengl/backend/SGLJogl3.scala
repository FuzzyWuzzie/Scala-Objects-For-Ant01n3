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
import GL2ES1._
import GL2ES2._
import GL3._ 

/** Wrapper around a GL version 3.1 instance.
  * 
  * The goal is to provide an easy access to some OpenGL methods, facilitating the use of NIO
  * buffers for example.
  */
class SGLJogl3(val gl:GL3, val glu:GLU, var ShaderVersion:String) extends SGL {
	private[this] val ib1 = NioIntBuffer.allocate(1)
	
	import gl._
	import glu._

// Awful constants
	
	val DEPTH_TEST:Int = GL.GL_DEPTH_TEST
	val BLEND:Int = GL.GL_BLEND
    val SRC_ALPHA:Int = GL.GL_SRC_ALPHA
    val BLEND_SRC:Int = GL.GL_BLEND_SRC
    val BLEND_DST:Int = GL.GL_BLEND_DST
    val ONE:Int = GL.GL_ONE
    val ONE_MINUS_SRC_ALPHA:Int = GL.GL_ONE_MINUS_SRC_ALPHA
    val CULL_FACE:Int = GL.GL_CULL_FACE
    val BACK:Int = GL.GL_BACK
    val CW:Int = GL.GL_CW
    val CCW:Int = GL.GL_CCW
    
    val COLOR_BUFFER_BIT:Int = GL.GL_COLOR_BUFFER_BIT
    val DEPTH_BUFFER_BIT:Int = GL.GL_DEPTH_BUFFER_BIT
    val FRONT_AND_BACK:Int = GL.GL_FRONT_AND_BACK
    val FRONT_FACE:Int = GL.GL_FRONT_FACE
    val FILL:Int = GL2GL3.GL_FILL
    val LINE:Int = GL2GL3.GL_LINE
    val LINE_SMOOTH:Int = GL.GL_LINE_SMOOTH
    val UNPACK_ALIGNMENT:Int = GL.GL_UNPACK_ALIGNMENT

	val NEVER:Int = GL_NEVER
	val LESS:Int = GL_LESS
	val EQUAL:Int = GL_EQUAL
	val LEQUAL:Int = GL_LEQUAL
	val GREATER:Int = GL_GREATER
	val NOTEQUAL:Int = GL_NOTEQUAL
	val GEQUAL:Int = GL_GEQUAL
	val ALWAYS:Int = GL_ALWAYS
    
    val TEXTURE_2D:Int = GL.GL_TEXTURE_2D
    val TEXTURE0:Int = GL.GL_TEXTURE0
    val TEXTURE1:Int = GL.GL_TEXTURE1
    val TEXTURE2:Int = GL.GL_TEXTURE2
    val TEXTURE_MIN_FILTER:Int = GL.GL_TEXTURE_MIN_FILTER
    val TEXTURE_MAG_FILTER:Int = GL.GL_TEXTURE_MAG_FILTER
    val TEXTURE_WRAP_S:Int = GL.GL_TEXTURE_WRAP_S
    val TEXTURE_WRAP_T:Int = GL.GL_TEXTURE_WRAP_T
//    val TEXTURE_BASE_LEVEL:Int = GL.GL_TEXTURE_BASE_LEVEL
//    val TEXTURE_MAX_LEVEL:Int = GL.GL_TEXTURE_MAX_LEVEL
    val LINEAR_MIPMAP_NEAREST:Int = GL.GL_LINEAR_MIPMAP_NEAREST
    val NEAREST_MIPMAP_NEAREST:Int = GL.GL_NEAREST_MIPMAP_NEAREST
    val LINEAR_MIPMAP_LINEAR:Int = GL.GL_LINEAR_MIPMAP_LINEAR
    val NEAREST_MIPMAP_LINEAR:Int = GL.GL_NEAREST_MIPMAP_LINEAR
    val LINEAR:Int = GL.GL_LINEAR
    val REPEAT:Int = GL.GL_REPEAT
    val CLAMP_TO_EDGE:Int = GL.GL_CLAMP_TO_EDGE
    val MIRRORED_REPEAT:Int = GL.GL_MIRRORED_REPEAT
    val NEAREST:Int = GL.GL_NEAREST
    val DEPTH_COMPONENT:Int = GL2ES2.GL_DEPTH_COMPONENT
    val FRAMEBUFFER:Int = GL.GL_FRAMEBUFFER
    val DEPTH_ATTACHMENT:Int = GL.GL_DEPTH_ATTACHMENT
    val COLOR_ATTACHMENT0:Int = GL.GL_COLOR_ATTACHMENT0
    val FRAMEBUFFER_COMPLETE:Int = GL.GL_FRAMEBUFFER_COMPLETE
    val FRAMEBUFFER_INCOMPLETE_ATTACHMENT:Int = GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT
    val FRAMEBUFFER_INCOMPLETE_DIMENSIONS:Int = GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS
    val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:Int = GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT
    val FRAMEBUFFER_UNSUPPORTED:Int = GL.GL_FRAMEBUFFER_UNSUPPORTED

    val INT:Int = GL2ES2.GL_INT
    val UNSIGNED_BYTE:Int = GL.GL_UNSIGNED_BYTE
    val UNSIGNED_INT:Int = GL.GL_UNSIGNED_INT
    val UNSIGNED_SHORT:Int = GL.GL_UNSIGNED_SHORT
    val FLOAT:Int = GL.GL_FLOAT
    val DOUBLE:Int = -1
    val RGBA:Int = GL.GL_RGBA
    val LUMINANCE:Int = GL.GL_LUMINANCE
    val LUMINANCE_ALPHA:Int = GL.GL_LUMINANCE_ALPHA
    val ALPHA:Int = GL.GL_ALPHA

    val ELEMENT_ARRAY_BUFFER:Int = GL.GL_ELEMENT_ARRAY_BUFFER
    val ARRAY_BUFFER:Int = GL.GL_ARRAY_BUFFER

    val VERTEX_SHADER:Int = GL2ES2.GL_VERTEX_SHADER
    val FRAGMENT_SHADER:Int = GL2ES2.GL_FRAGMENT_SHADER

    val POINTS:Int = GL.GL_POINTS
    val LINE_STRIP:Int = GL.GL_LINE_STRIP
    val LINES:Int = GL.GL_LINES
    val LINE_LOOP:Int = GL.GL_LINE_LOOP
    val TRIANGLES:Int = GL.GL_TRIANGLES
    val TRIANGLE_STRIP:Int = GL.GL_TRIANGLE_STRIP
    val TRIANGLE_FAN:Int = GL.GL_TRIANGLE_FAN

    val EXTENSIONS:Int = GL.GL_EXTENSIONS

    val STATIC_DRAW:Int = GL.GL_STATIC_DRAW
//    val STREAM_DRAW:Int = GL.GL_STREAM_DRAW
    val DYNAMIC_DRAW:Int = GL.GL_DYNAMIC_DRAW
        
    val PROGRAM_POINT_SIZE:Int = GL3.GL_PROGRAM_POINT_SIZE

// Info
    
    def isES:Boolean = false

    def getInteger(param:Int):Int = {
    	glGetIntegerv(param, ib1)
    	ib1.get(0)
    }

    def isEnabled(param:Int):Boolean = glIsEnabled(param)

// Vertex arrays
	
	def createVertexArray():AnyRef = {
	    glGenVertexArrays(1, ib1)
	    ib1.get(0).asInstanceOf[Integer]
	}
	
	def deleteVertexArray(id:AnyRef) {
	    ib1.put(0, id.asInstanceOf[Integer].toInt)
	    glDeleteVertexArrays(1, ib1)
	}
	
	def bindVertexArray(id:AnyRef) = glBindVertexArray(if(id eq null) 0 else id.asInstanceOf[Integer].toInt)
	def enableVertexAttribArray(index:Int) = glEnableVertexAttribArray(index)
	def disableVertexAttribArray(index:Int) = glDisableVertexAttribArray(index)
	def vertexAttribPointer(number:Int, attributeSize:Int, attributeType:Int, b:Boolean, size:Int, j:Int) = glVertexAttribPointer(number, attributeSize, attributeType, b, size, j)
//	def vertexAttribPointer(number:Int, attributeSize:Int, attributeType:Int, b:Boolean, size:Int, data:Buffer) = glVertexAttribPointer(number, attributeSize, attributeType, b, size, data)
    def drawArrays(mode:Int, i:Int, size:Int) = glDrawArrays(mode, i, size)
    def drawElements(mode:Int, count:Int, i:Int, offset:Int) = glDrawElements(mode, count, i, offset)
    def multiDrawArrays(mode:Int, firsts:IntBuffer, counts:IntBuffer, primcount:Int) = glMultiDrawArrays(mode, firsts.buffer.asInstanceOf[java.nio.IntBuffer], counts.buffer.asInstanceOf[java.nio.IntBuffer], primcount)

// Textures
    
    def createTexture:AnyRef = {
	    glGenTextures(1, ib1)
	    ib1.get(0).asInstanceOf[Integer]
	}
	def bindTexture(target:Int, id:AnyRef) = glBindTexture(target, if(id eq null) 0 else id.asInstanceOf[Integer].toInt)	
	def deleteTexture(id:AnyRef) {
	    ib1.put(0, id.asInstanceOf[Integer].toInt)
	    glDeleteTextures(1, ib1)
	}

	def activeTexture(texture:Int) = glActiveTexture(texture)
	def texParameter(target:Int, name:Int, param:Float) = glTexParameterf(target, name, param)
	def texParameter(target:Int, name:Int, param:Int) = glTexParameteri(target, name, param)
	def texParameter(target:Int, name:Int, params:FloatBuffer) = glTexParameterfv(target, name, params.buffer.asInstanceOf[java.nio.FloatBuffer])
	def texParameter(target:Int, name:Int, params:IntBuffer) = glTexParameteriv(target, name, params.buffer.asInstanceOf[java.nio.IntBuffer])
	def texImage1D(target:Int, level:Int, internalFormat:Int, width:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = glTexImage1D(target, level, internalFormat, width, border, format, theType, data.buffer.asInstanceOf[java.nio.ByteBuffer])
	def texImage2D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = glTexImage2D(target, level ,internalFormat, width, height, border, format, theType, data.buffer.asInstanceOf[java.nio.ByteBuffer])
    def texImage3D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, depth:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = glTexImage3D(target, level, internalFormat, width, height, depth, border, format, theType, data.buffer.asInstanceOf[java.nio.ByteBuffer])
    def generateMipmaps(target:Int) = glGenerateMipmap(target)

    def createFramebuffer:AnyRef = {
    	glGenFramebuffers(1, ib1)
    	ib1.get(0).asInstanceOf[Integer]
    }

    def deleteFramebuffer(id:AnyRef) {
    	ib1.put(0, id.asInstanceOf[Integer].toInt)
    	glDeleteFramebuffers(1, ib1)
    }

    def bindFramebuffer(target:Int, id:AnyRef) = glBindFramebuffer(target, if(id eq null) 0 else id.asInstanceOf[Integer].toInt)
    def framebufferTexture2D(target:Int,attachment:Int, textarget:Int, textureId:AnyRef, level:Int) = glFramebufferTexture2D(target,attachment,textarget,textureId.asInstanceOf[Integer].toInt,level)
    def checkFramebufferStatus(target:Int):Int = glCheckFramebufferStatus(target)

// Buffers
	
	def createBuffer():AnyRef = {
	    glGenBuffers(1, ib1)
	    ib1.get(0).asInstanceOf[Integer]
	}

	def bindBuffer(mode:Int, id:AnyRef) = glBindBuffer(mode, if(id eq null) 0 else id.asInstanceOf[Integer].toInt)
	
	def deleteBuffer(id:AnyRef) {
	    ib1.put(0, id.asInstanceOf[Integer].toInt)
	    glDeleteBuffers(1, ib1)
	}

	def bufferData(target:Int, data:DoubleBuffer, mode:Int) {
	    gl.glBufferData(target, data.size*8, data.buffer.asInstanceOf[java.nio.DoubleBuffer], mode)
	}
	
	def bufferData(target:Int, data:Array[Double], mode:Int) {
	    bufferData(target, DoubleBuffer(data), mode)
	}

	def bufferData(target:Int, data:FloatBuffer, mode:Int) {
	    gl.glBufferData(target, data.size*4, data.buffer.asInstanceOf[java.nio.FloatBuffer], mode)
	}
	
	def bufferData(target:Int, data:Array[Float], mode:Int) {
	    bufferData(target, FloatBuffer(data), mode)
	}

	def bufferData(target:Int, data:IntBuffer, mode:Int) {
	    gl.glBufferData(target, data.size*4, data.buffer.asInstanceOf[java.nio.IntBuffer], mode)
	}
	
	def bufferData(target:Int, data:Array[Int], mode:Int) {
	    bufferData(target, IntBuffer(data), mode)
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

	def bufferSubData(target:Int, offset:Int, size:Int, data:ByteBuffer, alsoPositionInData:Boolean) {
		val buffer = data.buffer.asInstanceOf[NioByteBuffer]
		if(alsoPositionInData) { buffer.clear; buffer.position(offset) }
		gl.glBufferSubData(target, offset*8, size*8, buffer)
		if(alsoPositionInData) buffer.clear
	}
		
	def bufferSubData(target:Int, offset:Int, size:Int, data:DoubleBuffer, alsoPositionInData:Boolean) {
		val buffer = data.buffer.asInstanceOf[NioDoubleBuffer]
		if(alsoPositionInData) { buffer.clear; buffer.position(offset) }
		gl.glBufferSubData(target, offset*8, size*8, buffer)
		if(alsoPositionInData) buffer.clear
	}

	def bufferSubData(target:Int, offset:Int, size:Int, data:FloatBuffer, alsoPositionInData:Boolean) {
		val buffer = data.buffer.asInstanceOf[NioFloatBuffer]
		if(alsoPositionInData) { buffer.clear; buffer.position(offset) }
		gl.glBufferSubData(target, offset*4, size*4, buffer)
		if(alsoPositionInData) buffer.clear
	}

	def bufferSubData(target:Int, offset:Int, size:Int, data:IntBuffer, alsoPositionInData:Boolean) {
		val buffer = data.buffer.asInstanceOf[NioIntBuffer]
		if(alsoPositionInData) { buffer.clear; buffer.position(offset) }
		gl.glBufferSubData(target, offset*4, size*4, buffer)
		if(alsoPositionInData) buffer.clear
	}
	
	def bufferSubData(target:Int, offset:Int, size:Int, data:NioBuffer, alsoPositionInData:Boolean) {
	    if(data.isByte) {
	        bufferSubData(target, offset, size, data.asInstanceOf[ByteBuffer], alsoPositionInData)
	    } else if(data.isInt) {
	        bufferSubData(target, offset, size, data.asInstanceOf[IntBuffer], alsoPositionInData)
	    } else if(data.isFloat) {
	        bufferSubData(target, offset, size, data.asInstanceOf[FloatBuffer], alsoPositionInData)
	    } else if(data.isDouble) {
	        bufferSubData(target, offset, size, data.asInstanceOf[DoubleBuffer], alsoPositionInData)
	    } else {
	        throw new RuntimeException("Unknown Nio buffer data type")
	    }
	}

// Shaders
	
	def createShader(shaderType:Int):AnyRef = glCreateShader(shaderType).asInstanceOf[Integer]
	def getShaderCompileStatus(id:AnyRef):Boolean = { getShader(id, GL_COMPILE_STATUS) == GL_TRUE }
	def getShader(id:AnyRef, status:Int):Int = {
	    glGetShaderiv(id.asInstanceOf[Integer].toInt, status, ib1)
	    ib1.get(0)
	}
	def getShaderInfoLog(id:AnyRef):String = {
		val len = getShader(id, GL_INFO_LOG_LENGTH)
		val data = NioByteBuffer.allocate(len)
	    gl.glGetShaderInfoLog(id.asInstanceOf[Integer].toInt, len, ib1, data)
	    new String(data.array)
	}
	def shaderSource(id:AnyRef, source:Array[String]) = glShaderSource(id.asInstanceOf[Integer].toInt, source.size, source, null)
	def shaderSource(id:AnyRef, source:String) = glShaderSource(id.asInstanceOf[Integer].toInt, 1, Array[String](source), null)
	def compileShader(id:AnyRef) = glCompileShader(id.asInstanceOf[Integer].toInt)
	def deleteShader(id:AnyRef) = glDeleteShader(id.asInstanceOf[Integer].toInt)
	
	def createProgram():AnyRef = glCreateProgram().asInstanceOf[Integer]
	def getProgram(id:AnyRef, status:Int):Int = {
		glGetProgramiv(id.asInstanceOf[Integer].toInt, status, ib1)
		ib1.get(0)
	}
	def getProgramLinkStatus(id:AnyRef):Boolean = { getProgram(id, GL_LINK_STATUS) == GL_TRUE }
	def getProgramInfoLog(id:AnyRef):String = {
	    val len = getProgram(id, GL_INFO_LOG_LENGTH)
	    val data = NioByteBuffer.allocate(len)
	    gl.glGetProgramInfoLog(id.asInstanceOf[Integer].toInt, len, ib1, data)
	    new String(data.array)
	}
    def attachShader(id:AnyRef, shaderId:AnyRef) = glAttachShader(id.asInstanceOf[Integer].toInt, shaderId.asInstanceOf[Integer].toInt)
    def detachShader(id:AnyRef, shaderId:AnyRef) = glDetachShader(id.asInstanceOf[Integer].toInt, shaderId.asInstanceOf[Integer].toInt)
    def linkProgram(id:AnyRef) = glLinkProgram(id.asInstanceOf[Integer].toInt)
    def useProgram(id:AnyRef) = glUseProgram(if(id eq null) 0 else id.asInstanceOf[Integer].toInt)
    def deleteProgram(id:AnyRef) = glDeleteProgram(id.asInstanceOf[Integer].toInt)

    def getAttribLocation(id:AnyRef, attribute:String):Int = glGetAttribLocation(id.asInstanceOf[Integer].toInt, attribute)
    def getUniformLocation(id:AnyRef, variable:String):AnyRef = {
    	val l = glGetUniformLocation(id.asInstanceOf[Integer].toInt, variable).asInstanceOf[Integer]
    	if(l < 0) null else l
    }

    def uniform(loc:AnyRef, i:Int) = glUniform1i(loc.asInstanceOf[Integer].toInt, i)
    def uniform(loc:AnyRef, i:Int, j:Int) = glUniform2i(loc.asInstanceOf[Integer].toInt, i, j)
    def uniform(loc:AnyRef, i:Int, j:Int, k:Int) = glUniform3i(loc.asInstanceOf[Integer].toInt, i, j, k)
    def uniform(loc:AnyRef, i:Int, j:Int, k:Int, l:Int) = glUniform4i(loc.asInstanceOf[Integer].toInt, i, j, k, l)
    def uniform(loc:AnyRef, i:Float) = glUniform1f(loc.asInstanceOf[Integer].toInt, i)
    def uniform(loc:AnyRef, i:Float, j:Float) = glUniform2f(loc.asInstanceOf[Integer].toInt, i, j)
    def uniform(loc:AnyRef, i:Float, j:Float, k:Float) = glUniform3f(loc.asInstanceOf[Integer].toInt, i, j, k)
    def uniform(loc:AnyRef, i:Float, j:Float, k:Float, l:Float) = glUniform4f(loc.asInstanceOf[Integer].toInt, i, j, k, l)
    def uniform(loc:AnyRef, color:Rgba) = glUniform4f(loc.asInstanceOf[Integer].toInt, color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
    def uniform(loc:AnyRef, i:Double) = throw new RuntimeException("no double values in shaders for GL 3 too bad")//glUniform1d(loc.asInstanceOf[Integer].toInt, i)
    def uniform(loc:AnyRef, i:Double, j:Double) = throw new RuntimeException("no double values in shaders for GL 3 too bad")//glUniform2d(loc.asInstanceOf[Integer].toInt, i, j)
    def uniform(loc:AnyRef, i:Double, j:Double, k:Double) = throw new RuntimeException("no double values in shaders for GL 3 too bad")//glUniform3d(loc.asInstanceOf[Integer].toInt, i, j, k)
    def uniform(loc:AnyRef, i:Double, j:Double, k:Double, l:Double) = throw new RuntimeException("no double values in shaders for GL 3 too bad")//glUniform4d(loc.asInstanceOf[Integer].toInt, i, j, k, l)
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:FloatBuffer) = glUniformMatrix3fv(loc.asInstanceOf[Integer].toInt, i, b, buffer.buffer.asInstanceOf[java.nio.FloatBuffer])
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:DoubleBuffer) = throw new RuntimeException("no double values in shaders for GL 3 too bad")//glUniformMatrix3dv(loc.asInstanceOf[Integer].toInt, i, b, buffer.buffer)
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:FloatBuffer) = glUniformMatrix4fv(loc.asInstanceOf[Integer].toInt, i, b, buffer.buffer.asInstanceOf[java.nio.FloatBuffer])
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:DoubleBuffer) = throw new RuntimeException("no double values in shaders for GL 3 too bad")//glUniformMatrix4dv(loc.asInstanceOf[Integer].toInt, i, b, buffer.buffer)
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:Array[Float]) = glUniformMatrix3fv(loc.asInstanceOf[Integer].toInt, i, b, buffer, 0)
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:Array[Float]) = glUniformMatrix4fv(loc.asInstanceOf[Integer].toInt, i, b, buffer, 0)
    def uniform(loc:AnyRef, v:Array[Float]) {
        if(     v.size==1) uniform(loc, v(0))
        else if(v.size==2) uniform(loc, v(0), v(1))
        else if(v.size==3) uniform(loc, v(0), v(1), v(2))
        else if(v.size==4) uniform(loc, v(0), v(1), v(2), v(3))
    }
    def uniform(loc:AnyRef, v:Array[Double]) {
        if(     v.size==1) uniform(loc, v(0).toFloat)
        else if(v.size==2) uniform(loc, v(0).toFloat, v(1).toFloat)
        else if(v.size==3) uniform(loc, v(0).toFloat, v(1).toFloat, v(2).toFloat)
        else if(v.size==4) uniform(loc, v(0).toFloat, v(1).toFloat, v(2).toFloat, v(3).toFloat)
    }
    def uniform(loc:AnyRef, v:FloatBuffer) {
        v.size match {
            case 1 => uniform(loc, v(0))
            case 2 => uniform(loc, v(0), v(1))
            case 3 => uniform(loc, v(0), v(1), v(2))
            case 4 => uniform(loc, v(0), v(1), v(2), v(3))
            case _ => throw new RuntimeException("uniform with more than 4 values?")
        }
    }
    def uniform(loc:AnyRef, v:DoubleBuffer) {
        v.size match {
            case 1 => uniform(loc, v(0))
            case 2 => uniform(loc, v(0), v(1))
            case 3 => uniform(loc, v(0), v(1), v(2))
            case 4 => uniform(loc, v(0), v(1), v(2), v(3))
            case _ => throw new RuntimeException("uniform with more than 4 values?")
        }
    }

// Basic API
	
    def getError:Int = glGetError
	def getString(i:Int):String = glGetString(i)
    def clear(mode:Int) = glClear(mode)
	def clearColor(r:Float, g:Float, b:Float, a:Float) = glClearColor(r, g, b, a)
	def clearColor(color:Rgba) = glClearColor(color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
	def clearColor(color:java.awt.Color) = glClearColor((color.getRed/255.0).toFloat, (color.getGreen/255.0).toFloat, (color.getBlue/255.0).toFloat, (color.getAlpha/255.0).toFloat)
    def clearDepth(value:Float) = glClearDepth(value)
    def viewport(x:Int, y:Int, width:Int, height:Int) = glViewport(x, y, width, height)
    def enable(i:Int) = glEnable(i)
    def disable(i:Int) = glDisable(i)
    def cullFace(i:Int) = glCullFace(i)
    def frontFace(i:Int) = glFrontFace(i)
    def lineWidth(width:Float) = glLineWidth(width)
    def lineWidth(width:Double) = glLineWidth(width.toFloat)
    // def pointSize(size:Float) = glPointSize(size)
    // def pointSize(size:Double) = glPointSize(size.toFloat)
    def blendEquation(mode:Int) = glBlendEquation(mode)
    def blendFunc(src:Int, dst:Int) = glBlendFunc(src, dst)
    def depthFunc(op:Int) = glDepthFunc(op)
    def polygonMode(face:Int, mode:Int) = glPolygonMode(face, mode)

    def pixelStore(param:Int, value:Int) = glPixelStorei(param, value)
    
// Utilities
    
    def printInfos() {
	    println("OpenGL version  %s".format(getString(GL_VERSION)))
        println("       glsl     %s".format(getString(GL_SHADING_LANGUAGE_VERSION)))
        println("       renderer %s".format(getString(GL_RENDERER)))
        println("       vendor   %s".format(getString(GL_VENDOR)))
        println("       profiles %s".format(GLProfile.GL_PROFILE_LIST_ALL.filter(GLProfile.isAvailable(_)).mkString(",")))
	}
	
	def checkErrors() {
	    val err = glGetError
	    if(err != GL_NO_ERROR) {
	        throw new RuntimeException(gluErrorString(err))
	    }
	}
}