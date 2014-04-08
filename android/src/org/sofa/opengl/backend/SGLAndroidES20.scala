package org.sofa.opengl.backend

import java.nio.{Buffer,IntBuffer=>NioIntBuffer,FloatBuffer=>NioFloatBuffer,DoubleBuffer=>NioDoubleBuffer,ByteBuffer=>NioByteBuffer}
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

    val INT:Int = GLES20.GL_INT
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

    val POINTS:Int = GLES20.GL_POINTS
    val LINE_STRIP:Int = GLES20.GL_LINE_STRIP
    val LINES:Int = GLES20.GL_LINES
    val LINE_LOOP:Int = GLES20.GL_LINE_LOOP
    val TRIANGLES:Int = GLES20.GL_TRIANGLES
    val TRIANGLE_STRIP:Int = GLES20.GL_TRIANGLE_STRIP
    val TRIANGLE_FAN:Int = GLES20.GL_TRIANGLE_FAN

    val EXTENSIONS:Int = GLES20.GL_EXTENSIONS

// Info
    
    def isES:Boolean = true

    def getInteger(param:Int):Int = {
    	GLES20.glGetIntegerv(param, ib1)
    	ib1.get(0)
    }

    def isEnabled(param:Int):Boolean = GLES20.glIsEnabled(param)

 // Vertex arrays
	
	def createVertexArray():AnyRef = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	def deleteVertexArray(id:AnyRef) = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	def bindVertexArray(id:AnyRef) = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	
	def enableVertexAttribArray(index:Int) = GLES20.glEnableVertexAttribArray(index)
	def disableVertexAttribArray(index:Int) = GLES20.glDisableVertexAttribArray(index)
	def vertexAttribPointer(number:Int, size:Int, typ:Int, b:Boolean, i:Int, j:Int) = GLES20.glVertexAttribPointer(number, size, typ, b, i, j)
	def vertexAttribPointer(number:Int, attributeSize:Int, attributeType:Int, b:Boolean, size:Int, data:Buffer) = GLES20.glVertexAttribPointer(number, attributeSize, attributeType, b, size, data)
    def drawArrays(mode:Int, i:Int, size:Int) = GLES20.glDrawArrays(mode, i, size)
    def drawElements(mode:Int, count:Int, i:Int, offset:Int) = GLES20.glDrawElements(mode, count, i, offset)
    def multiDrawArrays(mode:Int, firsts:IntBuffer, counts:IntBuffer, primcount:Int) = throw new RuntimeException("no multi draw arrays in GL ES 2.0, too bad")

	// Textures
    
    def createTexture:AnyRef = {
	    GLES20.glGenTextures(1, ib1)
	    ib1.get(0).asInstanceOf[Integer]
	}
	def bindTexture(target:Int, id:AnyRef) = GLES20.glBindTexture(target, if(id eq null) 0 else id.asInstanceOf[Integer].toInt)	
	def deleteTexture(id:AnyRef) {
	    ib1.put(0, id.asInstanceOf[Integer].toInt)
	    GLES20.glDeleteTextures(1, ib1)
	}
	
	def activeTexture(texture:Int) = GLES20.glActiveTexture(texture)
	def texParameter(target:Int, name:Int, param:Float) = GLES20.glTexParameterf(target, name, param)
	def texParameter(target:Int, name:Int, param:Int) = GLES20.glTexParameteri(target, name, param)
	def texParameter(target:Int, name:Int, params:FloatBuffer) = GLES20.glTexParameterfv(target, name, params)
	def texParameter(target:Int, name:Int, params:IntBuffer) = GLES20.glTexParameteriv(target, name, params)
	def texImage1D(target:Int, level:Int, internalFormat:Int, width:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = throw new RuntimeException("no texImage1D in GL ES 2.0, too bad")
	def texImage2D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = GLES20.glTexImage2D(target, level ,internalFormat, width, height, border, format, theType, data)
    def texImage3D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, depth:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = throw new RuntimeException("no texImage3D in GL ES 2.0, too bad")
    def generateMipmaps(target:Int) = GLES20.glGenerateMipmap(target)

    def createFramebuffer:AnyRef = {
    	GLES20.glGenFramebuffers(1, ib1)
    	ib1.get(0).asInstanceOf[Integer]
    }

    def deleteFramebuffer(id:AnyRef) {
    	ib1.put(0, id.asInstanceOf[Integer].toInt)
    	GLES20.glDeleteFramebuffers(1, ib1)
    }

    def bindFramebuffer(target:Int, id:AnyRef) = GLES20.glBindFramebuffer(target, if(id eq null) 0 else id.asInstanceOf[Integer].toInt)
    def framebufferTexture2D(target:Int,attachment:Int, textarget:Int, textureId:AnyRef, level:Int) = GLES20.glFramebufferTexture2D(target,attachment,textarget,textureId.asInstanceOf[Integer].toInt,level)
    def checkFramebufferStatus(target:Int):Int = GLES20.glCheckFramebufferStatus(target)

 // Buffers
	
	def createBuffer():AnyRef = {
	    GLES20.glGenBuffers(1, ib1)
	    ib1.get(0).asInstanceOf[Integer]
	}

	def bindBuffer(target:Int, id:AnyRef) = GLES20.glBindBuffer(target, if(id eq null) 0 else id.asInstanceOf[Integer].toInt)
	
	def deleteBuffer(id:AnyRef) {
	    ib1.put(0, id.asInstanceOf[Integer].toInt)
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

	def bufferSubData(target:Int, offset:Int, size:Int, data:ByteBuffer, alsoPositionInData:Boolean) {
		val buffer = data.buffer.asInstanceOf[NioByteBuffer]
		if(alsoPositionInData) { buffer.clear; buffer.position(offset) }
		GLES20.glBufferSubData(target, offset*8, size*8, buffer)
		if(alsoPositionInData) buffer.clear
	}

	def bufferSubData(target:Int, offset:Int, size:Int, data:DoubleBuffer, alsoPositionInData:Boolean) {
		val buffer = data.buffer.asInstanceOf[NioDoubleBuffer]
		if(alsoPositionInData) { buffer.clear; buffer.position(offset) }
		GLES20.glBufferSubData(target, offset*8, size*8, buffer)
		if(alsoPositionInData) buffer.clear
	}

	def bufferSubData(target:Int, offset:Int, size:Int, data:FloatBuffer, alsoPositionInData:Boolean) {
		val buffer = data.buffer.asInstanceOf[NioFloatBuffer]
		if(alsoPositionInData) { buffer.clear; buffer.position(offset) }
		GLES20.glBufferSubData(target, offset*4, size*4, buffer)
		if(alsoPositionInData) buffer.clear
	}

	def bufferSubData(target:Int, offset:Int, size:Int, data:IntBuffer, alsoPositionInData:Boolean) {
		val buffer = data.buffer.asInstanceOf[NioIntBuffer]
		if(alsoPositionInData) { buffer.clear; buffer.position(offset) }
		GLES20.glBufferSubData(target, offset, size, buffer)
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
	
	def createShader(shaderType:Int):AnyRef = GLES20.glCreateShader(shaderType).asInstanceOf[Integer]
	def getShaderCompileStatus(id:AnyRef):Boolean = { getShader(id, GLES20.GL_COMPILE_STATUS) == GLES20.GL_TRUE }
	def getShader(id:AnyRef, status:Int):Int = {
	    GLES20.glGetShaderiv(id.asInstanceOf[Integer].toInt, status, ib1)
	    ib1.get(0)
	}
	def getShaderInfoLog(id:AnyRef):String = GLES20.glGetShaderInfoLog(id.asInstanceOf[Integer].toInt)
	def shaderSource(id:AnyRef, source:Array[String]) = {
	    val buf = new StringBuffer
	    source.foreach { line => buf.append(line) }
		GLES20.glShaderSource(id.asInstanceOf[Integer].toInt, buf.toString)   
	}
	def shaderSource(id:AnyRef, source:String) = GLES20.glShaderSource(id.asInstanceOf[Integer].toInt, source)
	def compileShader(id:AnyRef) = GLES20.glCompileShader(id.asInstanceOf[Integer].toInt)
	def deleteShader(id:AnyRef) = GLES20.glDeleteShader(id.asInstanceOf[Integer].toInt)
	
	def createProgram():AnyRef = GLES20.glCreateProgram().asInstanceOf[Integer]
	def getProgram(id:AnyRef, status:Int):Int = {
		GLES20.glGetProgramiv(id.asInstanceOf[Integer].toInt, status, ib1)
		ib1.get(0)
	}
	def getProgramLinkStatus(id:AnyRef):Boolean = { getProgram(id, GLES20.GL_LINK_STATUS) == GLES20.GL_TRUE }
	def getProgramInfoLog(id:AnyRef):String = GLES20.glGetProgramInfoLog(id.asInstanceOf[Integer].toInt)
    def attachShader(id:AnyRef, shaderId:AnyRef) = GLES20.glAttachShader(id.asInstanceOf[Integer].toInt, shaderId.asInstanceOf[Integer].toInt)
    def detachShader(id:AnyRef, shaderId:AnyRef) = GLES20.glDetachShader(id.asInstanceOf[Integer].toInt, shaderId.asInstanceOf[Integer].toInt)
	def linkProgram(id:AnyRef) = GLES20.glLinkProgram(id.asInstanceOf[Integer].toInt)
    def useProgram(id:AnyRef) = GLES20.glUseProgram(if(id eq null) 0 else id.asInstanceOf[Integer].toInt)
    def deleteProgram(id:AnyRef) = GLES20.glDeleteProgram(id.asInstanceOf[Integer].toInt)
    
    def getAttribLocation(programId:AnyRef, attribute:String):Int = GLES20.glGetAttribLocation(id.asInstanceOf[Integer].toInt, attribute)
    def getUniformLocation(programId:AnyRef, variable:String):AnyRef = {
    	val l = GLES20.glGetUniformLocation(id.asInstanceOf[Integer].toInt, variable).asInstanceOf[Integer]
    	if(l < 0) null else l
    } 

    def uniform(loc:AnyRef, i:Int) = GLES20.glUniform1i(loc.asInstanceOf[Integer].toInt, i)
    def uniform(loc:AnyRef, i:Int, j:Int) = GLES20.glUniform2i(loc.asInstanceOf[Integer].toInt, i, j)
    def uniform(loc:AnyRef, i:Int, j:Int, k:Int) = GLES20.glUniform3i(loc.asInstanceOf[Integer].toInt, i, j, k)
    def uniform(loc:AnyRef, i:Int, j:Int, k:Int, l:Int) = GLES20.glUniform4i(loc.asInstanceOf[Integer].toInt, i, j, k, l)
    def uniform(loc:AnyRef, i:Float) = GLES20.glUniform1f(loc.asInstanceOf[Integer].toInt, i)
    def uniform(loc:AnyRef, i:Float, j:Float) = GLES20.glUniform2f(loc.asInstanceOf[Integer].toInt, i, j)
    def uniform(loc:AnyRef, i:Float, j:Float, k:Float) = GLES20.glUniform3f(loc.asInstanceOf[Integer].toInt, i, j, k)
    def uniform(loc:AnyRef, i:Float, j:Float, k:Float, l:Float) = GLES20.glUniform4f(loc.asInstanceOf[Integer].toInt, i, j, k, l)
    def uniform(loc:AnyRef, color:Rgba) = GLES20.glUniform4f(loc.asInstanceOf[Integer].toInt, color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
    def uniform(loc:AnyRef, i:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:AnyRef, i:Double, j:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:AnyRef, i:Double, j:Double, k:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:AnyRef, i:Double, j:Double, k:Double, l:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:FloatBuffer) = GLES20.glUniformMatrix3fv(loc.asInstanceOf[Integer].toInt, i, b, buffer.buffer)
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:DoubleBuffer) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:FloatBuffer) = GLES20.glUniformMatrix4fv(loc.asInstanceOf[Integer].toInt, i, b, buffer.buffer)
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:DoubleBuffer) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:Array[Float]) = GLES20.glUniformMatrix3fv(loc.asInstanceOf[Integer].toInt, i, b, buffer, 0)
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:Array[Float]) = GLES20.glUniformMatrix4fv(loc.asInstanceOf[Integer].toInt, i, b, buffer, 0)
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