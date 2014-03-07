package org.sofa.opengl.backend

import java.nio.{Buffer,IntBuffer=>NioIntBuffer}
import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.math._
import android.opengl.{GLES20, GLU}
import android.util.Log
import android.graphics.BitmapFactory

class SGLAndroidES20(var ShaderVersion:String) extends SGL {
    private[this] val ib1 = NioIntBuffer.allocate(1)

// Awful constants
	
	val DEPTH_TEST:Int = GLES20.GL_DEPTH_TEST
	val BLEND:Int = GLES20.GL_BLEND
    val SRC_ALPHA:Int = GLES20.GL_SRC_ALPHA
    val BLEND_SRC:Int = GLES20.GL_BLEND_SRC_ALPHA
    val BLEND_DST:Int = GLES20.GL_BLEND_DST_ALPHA
    val ONE:Int = GLES20.GL_ONE
    val ONE_MINUS_SRC_ALPHA:Int = GLES20.GL_ONE_MINUS_SRC_ALPHA
    val CULL_FACE:Int = GLES20.GL_CULL_FACE
    val BACK:Int = GLES20.GL_BACK
    val CW:Int = GLES20.GL_CW
    val CCW:Int = GLES20.GL_CCW
    
    val COLOR_BUFFER_BIT:Int = GLES20.GL_COLOR_BUFFER_BIT
    val DEPTH_BUFFER_BIT:Int = GLES20.GL_DEPTH_BUFFER_BIT
    val FRONT_AND_BACK:Int = GLES20.GL_FRONT_AND_BACK
    val FRONT_FACE:Int = GLES20.GL_FRONT_FACE
    val FILL:Int = -1
    val LINE:Int = -1
    val LINE_SMOOTH:Int = -1
    val UNPACK_ALIGNMENT:Int = GLES20.GL_UNPACK_ALIGNMENT

	val NEVER:Int = GLES20.GL_NEVER
	val LESS:Int = GLES20.GL_LESS
	val EQUAL:Int = GLES20.GL_EQUAL
	val LEQUAL:Int = GLES20.GL_LEQUAL
	val GREATER:Int = GLES20.GL_GREATER
	val NOTEQUAL:Int = GLES20.GL_NOTEQUAL
	val GEQUAL:Int = GLES20.GL_GEQUAL
	val ALWAYS:Int = GLES20.GL_ALWAYS
    
    val TEXTURE_2D:Int = GLES20.GL_TEXTURE_2D
    val TEXTURE0:Int = GLES20.GL_TEXTURE0
    val TEXTURE1:Int = GLES20.GL_TEXTURE1
    val TEXTURE2:Int = GLES20.GL_TEXTURE2
    val TEXTURE_MIN_FILTER:Int = GLES20.GL_TEXTURE_MIN_FILTER
    val TEXTURE_MAG_FILTER:Int = GLES20.GL_TEXTURE_MAG_FILTER
    val TEXTURE_WRAP_S:Int = GLES20.GL_TEXTURE_WRAP_S
    val TEXTURE_WRAP_T:Int = GLES20.GL_TEXTURE_WRAP_T
//    val TEXTURE_BASE_LEVEL:Int = GLES20.GL_TEXTURE_BASE_LEVEL
//    val TEXTURE_MAX_LEVEL:Int = GLES20.GL_TEXTURE_MAX_LEVEL
    val LINEAR_MIPMAP_NEAREST:Int = GLES20.GL_LINEAR_MIPMAP_NEAREST
    val NEAREST_MIPMAP_NEAREST:Int = GLES20.GL_NEAREST_MIPMAP_NEAREST
    val LINEAR_MIPMAP_LINEAR:Int = GLES20.GL_LINEAR_MIPMAP_LINEAR
    val NEAREST_MIPMAP_LINEAR:Int = GLES20.GL_NEAREST_MIPMAP_LINEAR
    val LINEAR:Int = GLES20.GL_LINEAR
    val REPEAT:Int = GLES20.GL_REPEAT
    val CLAMP_TO_EDGE:Int = GLES20.GL_CLAMP_TO_EDGE
    val MIRRORED_REPEAT:Int = GLES20.GL_MIRRORED_REPEAT
    val NEAREST:Int = GLES20.GL_NEAREST
    val DEPTH_COMPONENT:Int = GLES20.GL_DEPTH_COMPONENT
    val FRAMEBUFFER:Int = GLES20.GL_FRAMEBUFFER
    val DEPTH_ATTACHMENT:Int = GLES20.GL_DEPTH_ATTACHMENT
    val COLOR_ATTACHMENT0:Int = GLES20.GL_COLOR_ATTACHMENT0
    val FRAMEBUFFER_COMPLETE:Int = GLES20.GL_FRAMEBUFFER_COMPLETE
    val FRAMEBUFFER_INCOMPLETE_ATTACHMENT:Int = GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT
    val FRAMEBUFFER_INCOMPLETE_DIMENSIONS:Int = GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS
    val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:Int = GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT
    val FRAMEBUFFER_UNSUPPORTED:Int = GLES20.GL_FRAMEBUFFER_UNSUPPORTED

    val UNSIGNED_BYTE:Int = GLES20.GL_UNSIGNED_BYTE
    val UNSIGNED_INT:Int = GLES20.GL_UNSIGNED_INT
    val UNSIGNED_SHORT:Int = GLES20.GL_UNSIGNED_SHORT
    val FLOAT:Int = GLES20.GL_FLOAT
    val DOUBLE:Int = -1
    val RGBA:Int = GLES20.GL_RGBA
    val LUMINANCE:Int = GLES20.GL_LUMINANCE
    val LUMINANCE_ALPHA:Int = GLES20.GL_LUMINANCE_ALPHA
    val ALPHA:Int = GLES20.GL_ALPHA

    val ELEMENT_ARRAY_BUFFER:Int = GLES20.GL_ELEMENT_ARRAY_BUFFER
    val ARRAY_BUFFER:Int = GLES20.GL_ARRAY_BUFFER
    val STATIC_DRAW:Int = GLES20.GL_STATIC_DRAW
    val DYNAMIC_DRAW:Int = GLES20.GL_DYNAMIC_DRAW
    val PROGRAM_POINT_SIZE:Int = -1 //GLES20.GL_PROGRAM_POINT_SIZE

    val VERTEX_SHADER:Int = GLES20.GL_VERTEX_SHADER
    val FRAGMENT_SHADER:Int = GLES20.GL_FRAGMENT_SHADER

    val TRIANGLES:Int = GLES20.GL_TRIANGLES

    val EXTENSIONS:Int = GLES20.GL_EXTENSIONS

// Info
    
    def isES:Boolean = true

    def getInteger(param:Int):Int = {
    	GLES20.glGetIntegerv(param, ib1)
    	ib1.get(0)
    }

 // Vertex arrays
	
	def genVertexArray():Int = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	def deleteVertexArray(id:Int) = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	def bindVertexArray(id:Int) = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	
	def enableVertexAttribArray(id:Int) = GLES20.glEnableVertexAttribArray(id)
	def disableVertexAttribArray(id:Int) = GLES20.glDisableVertexAttribArray(id)
	def vertexAttribPointer(number:Int, size:Int, typ:Int, b:Boolean, i:Int, j:Int) = GLES20.glVertexAttribPointer(number, size, typ, b, i, j)
	def vertexAttribPointer(number:Int, attributeSize:Int, attributeType:Int, b:Boolean, size:Int, data:Buffer) = GLES20.glVertexAttribPointer(number, attributeSize, attributeType, b, size, data)
    def drawArrays(mode:Int, i:Int, size:Int) = GLES20.glDrawArrays(mode, i, size)
    def drawElements(mode:Int, count:Int, i:Int, offset:Int) = GLES20.glDrawElements(mode, count, i, offset)
    def multiDrawArrays(mode:Int, firsts:IntBuffer, counts:IntBuffer, primcount:Int) = throw new RuntimeException("no multi draw arrays in GL ES 2.0, too bad")

	// Textures
    
    def genTexture:Int = {
	    GLES20.glGenTextures(1, ib1)
	    ib1.get(0)
	}
	
	def deleteTexture(id:Int) {
	    ib1.put(0, id)
	    GLES20.glDeleteTextures(1, ib1)
	}
	
	def activeTexture(texture:Int) = GLES20.glActiveTexture(texture)
	def bindTexture(target:Int, id:Int) = GLES20.glBindTexture(target, id)
	def texParameter(target:Int, name:Int, param:Float) = GLES20.glTexParameterf(target, name, param)
	def texParameter(target:Int, name:Int, param:Int) = GLES20.glTexParameteri(target, name, param)
	def texParameter(target:Int, name:Int, params:FloatBuffer) = GLES20.glTexParameterfv(target, name, params)
	def texParameter(target:Int, name:Int, params:IntBuffer) = GLES20.glTexParameteriv(target, name, params)
	def texImage1D(target:Int, level:Int, internalFormat:Int, width:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = throw new RuntimeException("no texImage1D in GL ES 2.0, too bad")
	def texImage2D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = GLES20.glTexImage2D(target, level ,internalFormat, width, height, border, format, theType, data)
    def texImage3D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, depth:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = throw new RuntimeException("no texImage3D in GL ES 2.0, too bad")
    def generateMipmaps(target:Int) = GLES20.glGenerateMipmap(target)

    def genFramebuffer:Int = {
    	GLES20.glGenFramebuffers(1, ib1)
    	ib1.get(0)
    }

    def deleteFramebuffer(id:Int) {
    	ib1.put(0, id)
    	GLES20.glDeleteFramebuffers(1, ib1)
    }

    def bindFramebuffer(target:Int, id:Int) = GLES20.glBindFramebuffer(target, id)
    def framebufferTexture2D(target:Int,attachment:Int, textarget:Int, texture:Int, level:Int) = GLES20.glFramebufferTexture2D(target,attachment,textarget,texture,level)
    def checkFramebufferStatus(target:Int):Int = GLES20.glCheckFramebufferStatus(target)

 // Buffers
	
	def genBuffer():Int = {
	    GLES20.glGenBuffers(1, ib1)
	    ib1.get(0)
	}
	
	def deleteBuffer(id:Int) {
	    ib1.put(0, id)
	    GLES20.glDeleteBuffers(1, ib1)
	}

	def bufferData(target:Int, data:DoubleBuffer, mode:Int) {
	    GLES20.glBufferData(target, data.size*8, data.buffer, mode)
	}
	
	def bufferData(target:Int, data:Array[Double], mode:Int) {
	    bufferData(target, new DoubleBuffer(data), mode)
	}

	def bufferData(target:Int, data:FloatBuffer, mode:Int) {
	    GLES20.glBufferData(target, data.size*4, data.buffer, mode)
	}
	
	def bufferData(target:Int, data:Array[Float], mode:Int) {
	    bufferData(target, new FloatBuffer(data), mode)
	}

	def bufferData(target:Int, data:IntBuffer, mode:Int) {
	    GLES20.glBufferData(target, data.size*4, data.buffer, mode)
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

	def bufferSubData(target:Int, offset:Int, size:Int, data:DoubleBuffer) {
		GLES20.glBufferSubData(target, offset*8, size*8, data.buffer)
	}

	def bufferSubData(target:Int, offset:Int, size:Int, data:FloatBuffer) {
		GLES20.glBufferSubData(target, offset*4, size*4, data.buffer)
	}

	def bufferSubData(target:Int, offset:Int, size:Int, data:IntBuffer) {
		GLES20.glBufferSubData(target, offset, size, data.buffer)
	}
	
	def bufferSubData(target:Int, offset:Int, size:Int, data:NioBuffer) {
	    if(data.isByte) {
	        bufferSubData(target, offset, size, data.asInstanceOf[ByteBuffer])
	    } else if(data.isInt) {
	        bufferSubData(target, offset, size, data.asInstanceOf[IntBuffer])
	    } else if(data.isFloat) {
	        bufferSubData(target, offset, size, data.asInstanceOf[FloatBuffer])
	    } else if(data.isDouble) {
	        bufferSubData(target, offset, size, data.asInstanceOf[DoubleBuffer])
	    } else {
	        throw new RuntimeException("Unknown Nio buffer data type")
	    }
	}

	def bindBuffer(target:Int, id:Int) = GLES20.glBindBuffer(target, id)

// Shaders
	
	def createShader(shaderType:Int):Int = GLES20.glCreateShader(shaderType)
	
	def createProgram():Int = GLES20.glCreateProgram()
	
	def getShaderCompileStatus(id:Int):Boolean = { getShader(id, GLES20.GL_COMPILE_STATUS) == GLES20.GL_TRUE }
	
	def getProgramLinkStatus(id:Int):Boolean = { getProgram(id, GLES20.GL_LINK_STATUS) == GLES20.GL_TRUE }

	def getShader(id:Int, status:Int):Int = {
	    GLES20.glGetShaderiv(id, status, ib1)
	    ib1.get(0)
	}
	
	def getShaderInfoLog(id:Int):String = GLES20.glGetShaderInfoLog(id)
	
	def shaderSource(id:Int, source:Array[String]) = {
	    val buf = new StringBuffer
	    source.foreach { line => buf.append(line) }
		GLES20.glShaderSource(id, buf.toString)   
	}
	
	def getProgram(id:Int, status:Int):Int = {
		GLES20.glGetProgramiv(id, status, ib1)
		ib1.get(0)
	}

	def getProgramInfoLog(id:Int):String = GLES20.glGetProgramInfoLog(id)
	
	def shaderSource(id:Int, source:String) = GLES20.glShaderSource(id, source)
	def compileShader(id:Int) = GLES20.glCompileShader(id)
	def deleteShader(id:Int) = GLES20.glDeleteShader(id)
    def attachShader(id:Int, shaderId:Int) = GLES20.glAttachShader(id, shaderId)
    def linkProgram(id:Int) = GLES20.glLinkProgram(id)
    def useProgram(id:Int) = GLES20.glUseProgram(id)
    def detachShader(id:Int, shaderId:Int) = GLES20.glDetachShader(id, shaderId)
    def deleteProgram(id:Int) = GLES20.glDeleteProgram(id)
    def getUniformLocation(id:Int, variable:String):Int = GLES20.glGetUniformLocation(id, variable)
    def uniform(loc:Int, i:Int) = GLES20.glUniform1i(loc, i)
    def uniform(loc:Int, i:Int, j:Int) = GLES20.glUniform2i(loc, i, j)
    def uniform(loc:Int, i:Int, j:Int, k:Int) = GLES20.glUniform3i(loc, i, j, k)
    def uniform(loc:Int, i:Int, j:Int, k:Int, l:Int) = GLES20.glUniform4i(loc, i, j, k, l)
    def uniform(loc:Int, i:Float) = GLES20.glUniform1f(loc, i)
    def uniform(loc:Int, i:Float, j:Float) = GLES20.glUniform2f(loc, i, j)
    def uniform(loc:Int, i:Float, j:Float, k:Float) = GLES20.glUniform3f(loc, i, j, k)
    def uniform(loc:Int, i:Float, j:Float, k:Float, l:Float) = GLES20.glUniform4f(loc, i, j, k, l)
    def uniform(loc:Int, color:Rgba) = GLES20.glUniform4f(loc, color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
    def uniform(loc:Int, i:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:Int, i:Double, j:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:Int, i:Double, j:Double, k:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:Int, i:Double, j:Double, k:Double, l:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix3(loc:Int, i:Int, b:Boolean, buffer:FloatBuffer) = GLES20.glUniformMatrix3fv(loc, i, b, buffer.buffer)
    def uniformMatrix3(loc:Int, i:Int, b:Boolean, buffer:DoubleBuffer) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix4(loc:Int, i:Int, b:Boolean, buffer:FloatBuffer) = GLES20.glUniformMatrix4fv(loc, i, b, buffer.buffer)
    def uniformMatrix4(loc:Int, i:Int, b:Boolean, buffer:DoubleBuffer) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix3(loc:Int, i:Int, b:Boolean, buffer:Array[Float]) = GLES20.glUniformMatrix3fv(loc, i, b, buffer, 0)
    def uniformMatrix4(loc:Int, i:Int, b:Boolean, buffer:Array[Float]) = GLES20.glUniformMatrix4fv(loc, i, b, buffer, 0)
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
    def getAttribLocation(id:Int, attribute:String):Int = GLES20.glGetAttribLocation(id, attribute)

// Basic API
	
    def getError:Int = GLES20.glGetError
	def getString(i:Int):String = GLES20.glGetString(i)
    def clear(mode:Int) = GLES20.glClear(mode)
	def clearColor(r:Float, g:Float, b:Float, a:Float) = GLES20.glClearColor(r, g, b, a)
	def clearColor(color:Rgba) = GLES20.glClearColor(color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
	def clearColor(color:java.awt.Color) = throw new RuntimeException("no awt colors in Android")
    def clearDepth(value:Float) = GLES20.glClearDepthf(value)
    def viewport(x:Int, y:Int, width:Int, height:Int) = GLES20.glViewport(x, y, width, height)
    def enable(i:Int) = GLES20.glEnable(i)
    def disable(i:Int) = GLES20.glDisable(i)
    def cullFace(i:Int) = GLES20.glCullFace(i)
    def frontFace(i:Int) = GLES20.glFrontFace(i)
    def lineWidth(width:Float) = GLES20.glLineWidth(width)
    def lineWidth(width:Double) = GLES20.glLineWidth(width.toFloat)
    def blendEquation(mode:Int) = GLES20.glBlendEquation(mode)
    def blendFunc(src:Int, dst:Int) = GLES20.glBlendFunc(src, dst)
    def depthFunc(op:Int) = GLES20.glDepthFunc(op)
    def polygonMode(face:Int, mode:Int) = throw new RuntimeException("no polygonMode in GL ES 20, too bad")

    def pixelStore(param:Int, value:Int) = GLES20.glPixelStorei(param, value)
    
// Utilities
    
    def printInfos() {
	    println("OpenGL version  %s".format(getString(GLES20.GL_VERSION)))
        println("       glsl     %s".format(getString(GLES20.GL_SHADING_LANGUAGE_VERSION)))
        println("       renderer %s".format(getString(GLES20.GL_RENDERER)))
        println("       vendor   %s".format(getString(GLES20.GL_VENDOR)))
	}
	
	def checkErrors() {
	    val err = GLES20.glGetError
	    if(err != GLES20.GL_NO_ERROR) {
	        throw new RuntimeException("OpenGL error : %s".format(GLU.gluErrorString(err)))
	    }
	}
}