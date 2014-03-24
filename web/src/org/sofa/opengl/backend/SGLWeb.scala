package org.sofa.opengl.backend

import scala.scalajs.js

import java.nio.{Buffer,IntBuffer=>NioIntBuffer}
import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.math._


class SGLWeb(val gl:WebGLRenderingContext, var ShaderVersion:String) extends SGL {
    //private[this] val ib1 = NioIntBuffer.allocate(1)

// Awful constants
	
	val DEPTH_TEST:Int = gl.DEPTH_TEST.toInt
	val BLEND:Int = gl.BLEND.toInt
    val SRC_ALPHA:Int = gl.SRC_ALPHA.toInt
    val BLEND_SRC:Int = gl.BLEND_SRC_RGB.toInt		// ???
    val BLEND_DST:Int = gl.BLEND_DST_RGB.toInt		// ???
    val ONE:Int = gl.ONE.toInt
    val ONE_MINUS_SRC_ALPHA:Int = gl.ONE_MINUS_SRC_ALPHA.toInt
    val CULL_FACE:Int = gl.CULL_FACE.toInt
    val BACK:Int = gl.BACK.toInt
    val CW:Int = gl.CW.toInt
    val CCW:Int = gl.CCW.toInt
    
    val COLOR_BUFFER_BIT:Int = gl.COLOR_BUFFER_BIT.toInt
    val DEPTH_BUFFER_BIT:Int = gl.DEPTH_BUFFER_BIT.toInt
    val FRONT_AND_BACK:Int = gl.FRONT_AND_BACK.toInt
    val FRONT_FACE:Int = gl.FRONT_FACE.toInt
    val FILL:Int = -1
    val LINE:Int = -1
    val LINE_SMOOTH:Int = -1
    val UNPACK_ALIGNMENT:Int = gl.UNPACK_ALIGNMENT.toInt

	val NEVER:Int = gl.NEVER.toInt
	val LESS:Int = gl.LESS.toInt
	val EQUAL:Int = gl.EQUAL.toInt
	val LEQUAL:Int = gl.LEQUAL.toInt
	val GREATER:Int = gl.GREATER.toInt
	val NOTEQUAL:Int = gl.NOTEQUAL.toInt
	val GEQUAL:Int = gl.GEQUAL.toInt
	val ALWAYS:Int = gl.ALWAYS.toInt
    
    val TEXTURE_2D:Int = gl.TEXTURE_2D.toInt
    val TEXTURE0:Int = gl.TEXTURE0.toInt
    val TEXTURE1:Int = gl.TEXTURE1.toInt
    val TEXTURE2:Int = gl.TEXTURE2.toInt
    val TEXTURE_MIN_FILTER:Int = gl.TEXTURE_MIN_FILTER.toInt
    val TEXTURE_MAG_FILTER:Int = gl.TEXTURE_MAG_FILTER.toInt
    val TEXTURE_WRAP_S:Int = gl.TEXTURE_WRAP_S.toInt
    val TEXTURE_WRAP_T:Int = gl.TEXTURE_WRAP_T.toInt
//    val TEXTURE_BASE_LEVEL:Int = -1
//    val TEXTURE_MAX_LEVEL:Int = -1
    val LINEAR_MIPMAP_NEAREST:Int = gl.LINEAR_MIPMAP_NEAREST.toInt
    val NEAREST_MIPMAP_NEAREST:Int = gl.NEAREST_MIPMAP_NEAREST.toInt
    val LINEAR_MIPMAP_LINEAR:Int = gl.LINEAR_MIPMAP_LINEAR.toInt
    val NEAREST_MIPMAP_LINEAR:Int = gl.NEAREST_MIPMAP_LINEAR.toInt
    val LINEAR:Int = gl.LINEAR.toInt
    val REPEAT:Int = gl.REPEAT.toInt
    val CLAMP_TO_EDGE:Int = gl.CLAMP_TO_EDGE.toInt
    val MIRRORED_REPEAT:Int = gl.MIRRORED_REPEAT.toInt
    val NEAREST:Int = gl.NEAREST.toInt
    val DEPTH_COMPONENT:Int = gl.DEPTH_COMPONENT.toInt
    val FRAMEBUFFER:Int = gl.FRAMEBUFFER.toInt
    val DEPTH_ATTACHMENT:Int = gl.DEPTH_ATTACHMENT.toInt
    val COLOR_ATTACHMENT0:Int = gl.COLOR_ATTACHMENT0.toInt
    val FRAMEBUFFER_COMPLETE:Int = gl.FRAMEBUFFER_COMPLETE.toInt
    val FRAMEBUFFER_INCOMPLETE_ATTACHMENT:Int = gl.FRAMEBUFFER_INCOMPLETE_ATTACHMENT.toInt
    val FRAMEBUFFER_INCOMPLETE_DIMENSIONS:Int = gl.FRAMEBUFFER_INCOMPLETE_DIMENSIONS.toInt
    val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:Int = gl.FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT.toInt
    val FRAMEBUFFER_UNSUPPORTED:Int = gl.FRAMEBUFFER_UNSUPPORTED.toInt

    val UNSIGNED_BYTE:Int = gl.UNSIGNED_BYTE.toInt
    val UNSIGNED_INT:Int = gl.UNSIGNED_INT.toInt
    val UNSIGNED_SHORT:Int = gl.UNSIGNED_SHORT.toInt
    val FLOAT:Int = gl.FLOAT.toInt
    val DOUBLE:Int = -1
    val RGBA:Int = gl.RGBA.toInt
    val LUMINANCE:Int = gl.LUMINANCE.toInt
    val LUMINANCE_ALPHA:Int = gl.LUMINANCE_ALPHA.toInt
    val ALPHA:Int = gl.ALPHA.toInt

    val ELEMENT_ARRAY_BUFFER:Int = gl.ELEMENT_ARRAY_BUFFER.toInt
    val ARRAY_BUFFER:Int = gl.ARRAY_BUFFER.toInt
    val STATIC_DRAW:Int = gl.STATIC_DRAW.toInt
    val DYNAMIC_DRAW:Int = gl.DYNAMIC_DRAW.toInt
    val PROGRAM_POINT_SIZE:Int = -1

    val VERTEX_SHADER:Int = gl.VERTEX_SHADER.toInt
    val FRAGMENT_SHADER:Int = gl.FRAGMENT_SHADER.toInt

    val TRIANGLES:Int = gl.TRIANGLES.toInt

    val EXTENSIONS:Int = -1

// Info
    
    def isES:Boolean = true

    def getInteger(param:Int):Int = {
    	// GLES20.glGetIntegerv(param, ib1)
    	// ib1.get(0)
    	-1
    }

    def isEnabled(param:Int):Boolean = false//GLES20.glIsEnabled(param)

 // Vertex arrays
	
	def genVertexArray():Int = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	def deleteVertexArray(id:Int) = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	def bindVertexArray(id:Int) = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	
	def enableVertexAttribArray(id:Int) = {}//GLES20.glEnableVertexAttribArray(id)
	def disableVertexAttribArray(id:Int) = {}// GLES20.glDisableVertexAttribArray(id)
	def vertexAttribPointer(number:Int, size:Int, typ:Int, b:Boolean, i:Int, j:Int) = {}//GLES20.glVertexAttribPointer(number, size, typ, b, i, j)
	def vertexAttribPointer(number:Int, attributeSize:Int, attributeType:Int, b:Boolean, size:Int, data:Buffer) = {}//GLES20.glVertexAttribPointer(number, attributeSize, attributeType, b, size, data)
    def drawArrays(mode:Int, i:Int, size:Int) = {}//GLES20.glDrawArrays(mode, i, size)
    def drawElements(mode:Int, count:Int, i:Int, offset:Int) = {}//GLES20.glDrawElements(mode, count, i, offset)
    def multiDrawArrays(mode:Int, firsts:IntBuffer, counts:IntBuffer, primcount:Int) = {} //throw new RuntimeException("no multi draw arrays in GL ES 2.0, too bad")

	// Textures
    
    def genTexture:Int = {
	    // GLES20.glGenTextures(1, ib1)
	    // ib1.get(0)
	    -1
	}
	
	def deleteTexture(id:Int) {
	    // ib1.put(0, id)
	    // GLES20.glDeleteTextures(1, ib1)
	}
	
	def activeTexture(texture:Int) = {}//GLES20.glActiveTexture(texture)
	def bindTexture(target:Int, id:Int) = {}//GLES20.glBindTexture(target, id)
	def texParameter(target:Int, name:Int, param:Float) = {}//GLES20.glTexParameterf(target, name, param)
	def texParameter(target:Int, name:Int, param:Int) = {}//GLES20.glTexParameteri(target, name, param)
	def texParameter(target:Int, name:Int, params:FloatBuffer) = {}//GLES20.glTexParameterfv(target, name, params)
	def texParameter(target:Int, name:Int, params:IntBuffer) = {}//GLES20.glTexParameteriv(target, name, params)
	def texImage1D(target:Int, level:Int, internalFormat:Int, width:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = throw new RuntimeException("no texImage1D in GL ES 2.0, too bad")
	def texImage2D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = {}//GLES20.glTexImage2D(target, level ,internalFormat, width, height, border, format, theType, data)
    def texImage3D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, depth:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = throw new RuntimeException("no texImage3D in GL ES 2.0, too bad")
    def generateMipmaps(target:Int) = {}//GLES20.glGenerateMipmap(target)

    def genFramebuffer:Int = {
    	// GLES20.glGenFramebuffers(1, ib1)
    	// ib1.get(0)
    	-1
    }

    def deleteFramebuffer(id:Int) {
    	// ib1.put(0, id)
    	// GLES20.glDeleteFramebuffers(1, ib1)
    }

    def bindFramebuffer(target:Int, id:Int) = {}//GLES20.glBindFramebuffer(target, id)
    def framebufferTexture2D(target:Int,attachment:Int, textarget:Int, texture:Int, level:Int) = {}//GLES20.glFramebufferTexture2D(target,attachment,textarget,texture,level)
    def checkFramebufferStatus(target:Int):Int = -1//GLES20.glCheckFramebufferStatus(target)

 // Buffers
	
	def genBuffer():Int = {
	    // GLES20.glGenBuffers(1, ib1)
	    // ib1.get(0)
	    -1
	}
	
	def deleteBuffer(id:Int) {
	    // ib1.put(0, id)
	    // GLES20.glDeleteBuffers(1, ib1)
	}

	def bufferData(target:Int, data:DoubleBuffer, mode:Int) {
//	    GLES20.glBufferData(target, data.size*8, data.buffer, mode)
	}
	
	def bufferData(target:Int, data:Array[Double], mode:Int) {
//	    bufferData(target, new DoubleBuffer(data), mode)
	}

	def bufferData(target:Int, data:FloatBuffer, mode:Int) {
//	    GLES20.glBufferData(target, data.size*4, data.buffer, mode)
	}
	
	def bufferData(target:Int, data:Array[Float], mode:Int) {
//	    bufferData(target, new FloatBuffer(data), mode)
	}

	def bufferData(target:Int, data:IntBuffer, mode:Int) {
//	    GLES20.glBufferData(target, data.size*4, data.buffer, mode)
	}
	
	def bufferData(target:Int, data:Array[Int], mode:Int) {
//	    bufferData(target, new IntBuffer(data), mode)
	}

	def bufferData(target:Int, data:NioBuffer, mode:Int) {
	    // if(data.isByte) {
	    //     bufferData(target, data.asInstanceOf[ByteBuffer], mode)
	    // } else if(data.isInt) {
	    //     bufferData(target, data.asInstanceOf[IntBuffer], mode)
	    // } else if(data.isFloat) {
	    //     bufferData(target, data.asInstanceOf[FloatBuffer], mode)
	    // } else if(data.isDouble) {
	    //     bufferData(target, data.asInstanceOf[DoubleBuffer], mode)
	    // } else {
	    //     throw new RuntimeException("Unknown Nio buffer data type")
	    // }
	}

	def bufferSubData(target:Int, offset:Int, size:Int, data:DoubleBuffer, alsoPositionIndata:Boolean) {
//		GLES20.glBufferSubData(target, offset*8, size*8, data.buffer)
	}

	def bufferSubData(target:Int, offset:Int, size:Int, data:FloatBuffer, alsoPositionIndata:Boolean) {
//		GLES20.glBufferSubData(target, offset*4, size*4, data.buffer)
	}

	def bufferSubData(target:Int, offset:Int, size:Int, data:IntBuffer, alsoPositionIndata:Boolean) {
//		GLES20.glBufferSubData(target, offset, size, data.buffer)
	}

	def bufferSubData(target:Int, offset:Int, size:Int, data:ByteBuffer, alsoPositionIndata:Boolean) {
//		GLES20.glBufferSubData(target, offset, size, data.buffer)
	}
	
	def bufferSubData(target:Int, offset:Int, size:Int, data:NioBuffer, alsoPositionIndata:Boolean) {
	    // if(data.isByte) {
	    //     bufferSubData(target, offset, size, data.asInstanceOf[ByteBuffer])
	    // } else if(data.isInt) {
	    //     bufferSubData(target, offset, size, data.asInstanceOf[IntBuffer])
	    // } else if(data.isFloat) {
	    //     bufferSubData(target, offset, size, data.asInstanceOf[FloatBuffer])
	    // } else if(data.isDouble) {
	    //     bufferSubData(target, offset, size, data.asInstanceOf[DoubleBuffer])
	    // } else {
	    //     throw new RuntimeException("Unknown Nio buffer data type")
	    // }
	}

	def bindBuffer(target:Int, id:Int) = {}//GLES20.glBindBuffer(target, id)

// Shaders
	
	def createShader(shaderType:Int):Int = { -1 }//GLES20.glCreateShader(shaderType)
	
	def createProgram():Int = { -1 }//GLES20.glCreateProgram()
	
	def getShaderCompileStatus(id:Int):Boolean = false//{ getShader(id, GLES20.GL_COMPILE_STATUS) == GLES20.GL_TRUE }
	
	def getProgramLinkStatus(id:Int):Boolean = false//{ getProgram(id, GLES20.GL_LINK_STATUS) == GLES20.GL_TRUE }

	def getShader(id:Int, status:Int):Int = {
	    // GLES20.glGetShaderiv(id, status, ib1)
	    // ib1.get(0)
	    -1
	}
	
	def getShaderInfoLog(id:Int):String = { "" }//GLES20.glGetShaderInfoLog(id)
	
	def shaderSource(id:Int, source:Array[String]) = {
	 //    val buf = new StringBuffer
	 //    source.foreach { line => buf.append(line) }
		// GLES20.glShaderSource(id, buf.toString)   
	}
	
	def getProgram(id:Int, status:Int):Int = {
		// GLES20.glGetProgramiv(id, status, ib1)
		// ib1.get(0)
		-1
	}

	def getProgramInfoLog(id:Int):String = { "" }//GLES20.glGetProgramInfoLog(id)
	
	def shaderSource(id:Int, source:String) = {}//GLES20.glShaderSource(id, source)
	def compileShader(id:Int) = {}//GLES20.glCompileShader(id)
	def deleteShader(id:Int) = {}//GLES20.glDeleteShader(id)
    def attachShader(id:Int, shaderId:Int) = {}//GLES20.glAttachShader(id, shaderId)
    def linkProgram(id:Int) = {}//GLES20.glLinkProgram(id)
    def useProgram(id:Int) = {}//GLES20.glUseProgram(id)
    def detachShader(id:Int, shaderId:Int) = {}//GLES20.glDetachShader(id, shaderId)
    def deleteProgram(id:Int) = {}//GLES20.glDeleteProgram(id)
    def getUniformLocation(id:Int, variable:String):Int = -1//GLES20.glGetUniformLocation(id, variable)
    def uniform(loc:Int, i:Int) = {}//GLES20.glUniform1i(loc, i)
    def uniform(loc:Int, i:Int, j:Int) = {}//GLES20.glUniform2i(loc, i, j)
    def uniform(loc:Int, i:Int, j:Int, k:Int) = {}//GLES20.glUniform3i(loc, i, j, k)
    def uniform(loc:Int, i:Int, j:Int, k:Int, l:Int) = {}//GLES20.glUniform4i(loc, i, j, k, l)
    def uniform(loc:Int, i:Float) = {}//GLES20.glUniform1f(loc, i)
    def uniform(loc:Int, i:Float, j:Float) = {}//GLES20.glUniform2f(loc, i, j)
    def uniform(loc:Int, i:Float, j:Float, k:Float) = {}//GLES20.glUniform3f(loc, i, j, k)
    def uniform(loc:Int, i:Float, j:Float, k:Float, l:Float) = {}//GLES20.glUniform4f(loc, i, j, k, l)
    def uniform(loc:Int, color:Rgba) = {}//GLES20.glUniform4f(loc, color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
    def uniform(loc:Int, i:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:Int, i:Double, j:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:Int, i:Double, j:Double, k:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:Int, i:Double, j:Double, k:Double, l:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix3(loc:Int, i:Int, b:Boolean, buffer:FloatBuffer) = {}//GLES20.glUniformMatrix3fv(loc, i, b, buffer.buffer)
    def uniformMatrix3(loc:Int, i:Int, b:Boolean, buffer:DoubleBuffer) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix4(loc:Int, i:Int, b:Boolean, buffer:FloatBuffer) = {}//GLES20.glUniformMatrix4fv(loc, i, b, buffer.buffer)
    def uniformMatrix4(loc:Int, i:Int, b:Boolean, buffer:DoubleBuffer) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix3(loc:Int, i:Int, b:Boolean, buffer:Array[Float]) = {}//GLES20.glUniformMatrix3fv(loc, i, b, buffer, 0)
    def uniformMatrix4(loc:Int, i:Int, b:Boolean, buffer:Array[Float]) = {}//GLES20.glUniformMatrix4fv(loc, i, b, buffer, 0)
    def uniform(loc:Int, v:Array[Float]) {
        // if(     v.size==1) uniform(loc, v(0))
        // else if(v.size==2) uniform(loc, v(0), v(1))
        // else if(v.size==3) uniform(loc, v(0), v(1), v(2))
        // else if(v.size==4) uniform(loc, v(0), v(1), v(2), v(3))
    }
    def uniform(loc:Int, v:Array[Double]) {
        // if(     v.size==1) uniform(loc, v(0).toFloat)
        // else if(v.size==2) uniform(loc, v(0).toFloat, v(1).toFloat)
        // else if(v.size==3) uniform(loc, v(0).toFloat, v(1).toFloat, v(2).toFloat)
        // else if(v.size==4) uniform(loc, v(0).toFloat, v(1).toFloat, v(2).toFloat, v(3).toFloat)
    }
    def uniform(loc:Int, v:FloatBuffer) {
        // v.size match {
        //     case 1 => uniform(loc, v(0))
        //     case 2 => uniform(loc, v(0), v(1))
        //     case 3 => uniform(loc, v(0), v(1), v(2))
        //     case 4 => uniform(loc, v(0), v(1), v(2), v(3))
        //     case _ => throw new RuntimeException("uniform with more than 4 values?")
        // }
    }
    def uniform(loc:Int, v:DoubleBuffer) {
        // v.size match {
        //     case 1 => uniform(loc, v(0))
        //     case 2 => uniform(loc, v(0), v(1))
        //     case 3 => uniform(loc, v(0), v(1), v(2))
        //     case 4 => uniform(loc, v(0), v(1), v(2), v(3))
        //     case _ => throw new RuntimeException("uniform with more than 4 values?")
        // }
    }
    def getAttribLocation(id:Int, attribute:String):Int = -1//GLES20.glGetAttribLocation(id, attribute)

// Basic API
	
    def getError:Int = -1//GLES20.glGetError
	def getString(i:Int):String = ""//GLES20.glGetString(i)
    def clear(mode:Int) = gl.clear(mode)
	def clearColor(r:Float, g:Float, b:Float, a:Float) = gl.clearColor(r, g, b, a)
	def clearColor(color:Rgba) = gl.clearColor(color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
	def clearColor(color:java.awt.Color) = throw new RuntimeException("no awt colors in Android")
    def clearDepth(value:Float) = {}//GLES20.glClearDepthf(value)
    def viewport(x:Int, y:Int, width:Int, height:Int) = {}//GLES20.glViewport(x, y, width, height)
    def enable(i:Int) = {}//GLES20.glEnable(i)
    def disable(i:Int) = {}//GLES20.glDisable(i)
    def cullFace(i:Int) = {}//GLES20.glCullFace(i)
    def frontFace(i:Int) = {}//GLES20.glFrontFace(i)
    def lineWidth(width:Float) = {}//GLES20.glLineWidth(width)
    def lineWidth(width:Double) = {}//GLES20.glLineWidth(width.toFloat)
    def blendEquation(mode:Int) = {}//GLES20.glBlendEquation(mode)
    def blendFunc(src:Int, dst:Int) = {}//GLES20.glBlendFunc(src, dst)
    def depthFunc(op:Int) = {}//GLES20.glDepthFunc(op)
    def polygonMode(face:Int, mode:Int) = throw new RuntimeException("no polygonMode in GL ES 20, too bad")

    def pixelStore(param:Int, value:Int) = {}//GLES20.glPixelStorei(param, value)
    
// Utilities
    
    def printInfos() {
//	    println("OpenGL version  %s".format(getString(GLES20.GL_VERSION)))
//      println("       glsl     %s".format(getString(GLES20.GL_SHADING_LANGUAGE_VERSION)))
//      println("       renderer %s".format(getString(GLES20.GL_RENDERER)))
//      println("       vendor   %s".format(getString(GLES20.GL_VENDOR)))
	}
	
	def checkErrors() {
//	    val err = GLES20.glGetError
//	    if(err != GLES20.GL_NO_ERROR) {
//	        throw new RuntimeException("OpenGL error : %s".format(GLU.gluErrorString(err)))
//	    }
	}
}