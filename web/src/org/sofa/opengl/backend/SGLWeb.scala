package org.sofa.opengl.backend

import scala.scalajs.js

import org.sofa.nio.backend.{ArrayBuffer, ArrayBufferView, Float32Array}
import java.nio.{Buffer,IntBuffer=>NioIntBuffer}
import org.sofa.nio._
import org.sofa.opengl._
import org.sofa.math._


class SGLWeb(val gl:WebGLRenderingContext, var ShaderVersion:String) extends SGL {

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

    val INT:Int = gl.INT.toInt
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

    val POINTS:Int = gl.POINTS.toInt
    val LINE_STRIP:Int = gl.LINE_STRIP.toInt
    val LINES:Int = gl.LINES.toInt
    val LINE_LOOP:Int = gl.LINE_LOOP.toInt
    val TRIANGLES:Int = gl.TRIANGLES.toInt
    val TRIANGLE_STRIP:Int = gl.TRIANGLE_STRIP.toInt
    val TRIANGLE_FAN:Int = gl.TRIANGLE_FAN.toInt

    val EXTENSIONS:Int = -1

// Info
    
    def isES:Boolean = true

    def getInteger(param:Int):Int = {
    	// GLES20.glGetIntegerv(param, ib1)
    	// ib1.get(0)
    	-1
    }

    def isEnabled(param:Int):Boolean = gl.isEnabled(param)

// Vertex arrays
	
	def createVertexArray():AnyRef = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	def deleteVertexArray(id:AnyRef) = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	def bindVertexArray(id:AnyRef) = throw new RuntimeException("no vertex arrays in GL ES 2.0, too bad")
	
	def enableVertexAttribArray(index:Int) = gl.enableVertexAttribArray(index)
	def disableVertexAttribArray(index:Int) = gl.disableVertexAttribArray(index)
	def vertexAttribPointer(number:Int, size:Int, typ:Int, b:Boolean, i:Int, j:Int) = gl.vertexAttribPointer(number, size, typ, b, i, j)
	def vertexAttribPointer(number:Int, attributeSize:Int, attributeType:Int, b:Boolean, size:Int, data:Buffer) = throw new RuntimeException("no vertexAttribPointer with data buffer in WebGL")//gl.vertexAttribPointer(number, attributeSize, attributeType, b, size, data)
    def drawArrays(mode:Int, i:Int, size:Int) = gl.drawArrays(mode, i, size)
    def drawElements(mode:Int, count:Int, i:Int, offset:Int) = gl.drawElements(mode, count, i, offset)
    def multiDrawArrays(mode:Int, firsts:IntBuffer, counts:IntBuffer, primcount:Int) = throw new RuntimeException("no multi draw arrays in GL ES 2.0, too bad")

	// Textures
    
    def createTexture:AnyRef = gl.createTexture
	
	def deleteTexture(id:AnyRef) { gl.deleteTexture(id.asInstanceOf[js.Any]) }
	
	def activeTexture(texture:Int) = gl.activeTexture(texture)
	def bindTexture(target:Int, id:AnyRef) = gl.bindTexture(target, id.asInstanceOf[js.Any])
	def texParameter(target:Int, name:Int, param:Float) = gl.texParameterf(target, name, param)
	def texParameter(target:Int, name:Int, param:Int) = gl.texParameteri(target, name, param)
	def texParameter(target:Int, name:Int, params:FloatBuffer) = throw new RuntimeException("texParameterfv not supported in WebGL")//gl.texParameterfv(target, name, params)
	def texParameter(target:Int, name:Int, params:IntBuffer) = throw new RuntimeException("texParameterfv not supported in WebGL")//gl.texParameteriv(target, name, params)
	def texImage1D(target:Int, level:Int, internalFormat:Int, width:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = throw new RuntimeException("no texImage1D in GL ES 2.0, too bad")
	def texImage2D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = gl.texImage2D(target, level ,internalFormat, width, height, border, format, theType, data.buffer.asInstanceOf[ArrayBufferView])
    def texImage3D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, depth:Int, border:Int, format:Int, theType:Int, data:ByteBuffer) = throw new RuntimeException("no texImage3D in GL ES 2.0, too bad")
    def generateMipmaps(target:Int) = gl.generateMipmap(target)

    def createFramebuffer:AnyRef = gl.createFramebuffer

    def deleteFramebuffer(id:AnyRef) { gl.deleteFramebuffer(id.asInstanceOf[js.Any]) }

    def bindFramebuffer(target:Int, id:AnyRef) = gl.bindFramebuffer(target, id.asInstanceOf[js.Any])
    def framebufferTexture2D(target:Int,attachment:Int, textarget:Int, texture:AnyRef, level:Int) = gl.framebufferTexture2D(target,attachment,textarget,texture.asInstanceOf[js.Any],level)
    def checkFramebufferStatus(target:Int):Int = gl.checkFramebufferStatus(target).toInt

 // Buffers
	
	def createBuffer():AnyRef = gl.createBuffer

	def bindBuffer(target:Int, id:AnyRef) = gl.bindBuffer(target, id.asInstanceOf[js.Any])
	
	def deleteBuffer(id:AnyRef) = gl.deleteBuffer(id.asInstanceOf[js.Any])

	def bufferData(target:Int, data:DoubleBuffer, mode:Int) { gl.bufferData(target, /*data.size*8,*/ data.buffer.asInstanceOf[ArrayBufferView], mode) }
	def bufferData(target:Int, data:Array[Double], mode:Int) { bufferData(target, DoubleBuffer(data), mode) }
	def bufferData(target:Int, data:FloatBuffer, mode:Int) { gl.bufferData(target, /*data.size*4,*/ data.buffer.asInstanceOf[ArrayBufferView], mode) }
	def bufferData(target:Int, data:Array[Float], mode:Int) { bufferData(target, FloatBuffer(data), mode) }
	def bufferData(target:Int, data:IntBuffer, mode:Int) { gl.bufferData(target, /*data.size*4,*/ data.buffer.asInstanceOf[ArrayBufferView], mode) }
	def bufferData(target:Int, data:Array[Int], mode:Int) { bufferData(target, IntBuffer(data), mode) }
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

	def bufferSubData(target:Int, offset:Int, size:Int, data:DoubleBuffer, alsoPositionIndata:Boolean) { gl.bufferSubData(target, offset*8, /*size*8,*/ data.buffer.asInstanceOf[ArrayBufferView]) }
	def bufferSubData(target:Int, offset:Int, size:Int, data:FloatBuffer, alsoPositionIndata:Boolean) { gl.bufferSubData(target, offset*4, /*size*4,*/ data.buffer.asInstanceOf[ArrayBufferView]) }
	def bufferSubData(target:Int, offset:Int, size:Int, data:IntBuffer, alsoPositionIndata:Boolean) { gl.bufferSubData(target, offset*4, /*size,*/ data.buffer.asInstanceOf[ArrayBufferView]) }
	def bufferSubData(target:Int, offset:Int, size:Int, data:ByteBuffer, alsoPositionIndata:Boolean) { gl.bufferSubData(target, offset, /*size,*/ data.buffer.asInstanceOf[ArrayBufferView]) }
	def bufferSubData(target:Int, offset:Int, size:Int, data:NioBuffer, alsoPositionIndata:Boolean) {
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

// Shaders
	
	def createShader(shaderType:Int):AnyRef = gl.createShader(shaderType)
	
	def createProgram():AnyRef = gl.createProgram
	
	def getShaderCompileStatus(id:AnyRef):Boolean = gl.getShaderParameter(id.asInstanceOf[js.Any], gl.COMPILE_STATUS).asInstanceOf[js.Boolean]
	
	def getProgramLinkStatus(id:AnyRef):Boolean = gl.getProgramParameter(id.asInstanceOf[js.Any], gl.LINK_STATUS).asInstanceOf[js.Boolean]

	def getShader(id:AnyRef, status:Int):Int = gl.getShaderParameter(id.asInstanceOf[js.Any], status).asInstanceOf[js.Number].toInt
	
	def getShaderInfoLog(id:AnyRef):String = gl.getShaderInfoLog(id.asInstanceOf[js.Any])
	
	def shaderSource(id:AnyRef, source:Array[String]) = {
	    val buf = new StringBuffer
	    source.foreach { line => buf.append(line) }
		gl.shaderSource(id.asInstanceOf[js.Any], buf.toString)
	}
	
	def getProgram(id:AnyRef, status:Int):Int = gl.getProgramParameter(id.asInstanceOf[js.Any], status).asInstanceOf[js.Number].toInt

	def getProgramInfoLog(id:AnyRef):String = gl.getProgramInfoLog(id.asInstanceOf[js.Any])
	
	def shaderSource(id:AnyRef, source:String) = gl.shaderSource(id.asInstanceOf[js.Any], source)
	def compileShader(id:AnyRef) = gl.compileShader(id.asInstanceOf[js.Any])
	def deleteShader(id:AnyRef) = gl.deleteShader(id.asInstanceOf[js.Any])
    def attachShader(id:AnyRef, shaderId:AnyRef) = gl.attachShader(id.asInstanceOf[js.Any], shaderId.asInstanceOf[js.Any])
    def linkProgram(id:AnyRef) = gl.linkProgram(id.asInstanceOf[js.Any])
    def useProgram(id:AnyRef) = gl.useProgram(id.asInstanceOf[js.Any])
    def detachShader(id:AnyRef, shaderId:AnyRef) = gl.detachShader(id.asInstanceOf[js.Any], shaderId.asInstanceOf[js.Any])
    def deleteProgram(id:AnyRef) = gl.deleteProgram(id.asInstanceOf[js.Any])
    
    /** To avoid re-allocating a FloatBuffer at each uniformMatrix4 call with an array. */
    protected[this] val tmpM4 = FloatBuffer(16)
    
    /** To avoid re-allocating a FloatBuffer at each uniformMatrix3 call with an array. */
    protected[this] val tmpM3 = FloatBuffer(9)

    def getUniformLocation(id:AnyRef, variable:String):AnyRef = gl.getUniformLocation(id.asInstanceOf[js.Any], variable)
    def uniform(loc:AnyRef, i:Int) = gl.uniform1i(loc.asInstanceOf[js.Any], i)
    def uniform(loc:AnyRef, i:Int, j:Int) = gl.uniform2i(loc.asInstanceOf[js.Any], i, j)
    def uniform(loc:AnyRef, i:Int, j:Int, k:Int) = gl.uniform3i(loc.asInstanceOf[js.Any], i, j, k)
    def uniform(loc:AnyRef, i:Int, j:Int, k:Int, l:Int) = gl.uniform4i(loc.asInstanceOf[js.Any], i, j, k, l)
    def uniform(loc:AnyRef, i:Float) = gl.uniform1f(loc.asInstanceOf[js.Any], i)
    def uniform(loc:AnyRef, i:Float, j:Float) = gl.uniform2f(loc.asInstanceOf[js.Any], i, j)
    def uniform(loc:AnyRef, i:Float, j:Float, k:Float) = gl.uniform3f(loc.asInstanceOf[js.Any], i, j, k)
    def uniform(loc:AnyRef, i:Float, j:Float, k:Float, l:Float) = gl.uniform4f(loc.asInstanceOf[js.Any], i, j, k, l)
    def uniform(loc:AnyRef, color:Rgba) = gl.uniform4f(loc.asInstanceOf[js.Any], color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
    def uniform(loc:AnyRef, i:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:AnyRef, i:Double, j:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:AnyRef, i:Double, j:Double, k:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniform(loc:AnyRef, i:Double, j:Double, k:Double, l:Double) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:FloatBuffer) = gl.uniformMatrix3fv(loc.asInstanceOf[js.Any], b, buffer.buffer.asInstanceOf[Float32Array])
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:DoubleBuffer) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:FloatBuffer) = gl.uniformMatrix4fv(loc.asInstanceOf[js.Any], b, buffer.buffer.asInstanceOf[Float32Array])
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:DoubleBuffer) = throw new RuntimeException("too bad no double for shaders in GL ES 2.0")
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:Array[Float]) = { tmpM3.copy(buffer); uniformMatrix3(loc, i, b, tmpM3) }
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:Array[Float]) = { tmpM4.copy(buffer); uniformMatrix4(loc, i, b, tmpM4) }
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
    def getAttribLocation(id:AnyRef, attribute:String):Int = gl.getAttribLocation(id.asInstanceOf[js.Any], attribute).asInstanceOf[js.Number].toInt

// Basic API
	
    def getError:Int = gl.getError().toInt
	def getString(i:Int):String = gl.getParameter(i).asInstanceOf[js.String]
    def clear(mode:Int) = gl.clear(mode)
	def clearColor(r:Float, g:Float, b:Float, a:Float) = gl.clearColor(r, g, b, a)
	def clearColor(color:Rgba) = gl.clearColor(color.red.toFloat, color.green.toFloat, color.blue.toFloat, color.alpha.toFloat)
	def clearColor(color:java.awt.Color) = throw new RuntimeException("no awt colors in Android")
    def clearDepth(value:Float) = gl.clearDepth(value)
    def viewport(x:Int, y:Int, width:Int, height:Int) = gl.viewport(x, y, width, height)
    def enable(i:Int) = gl.enable(i)
    def disable(i:Int) = gl.disable(i)
    def cullFace(i:Int) = gl.cullFace(i)
    def frontFace(i:Int) = gl.frontFace(i)
    def lineWidth(width:Float) = gl.lineWidth(width)
    def lineWidth(width:Double) = gl.lineWidth(width.toFloat)
    def blendEquation(mode:Int) = gl.blendEquation(mode)
    def blendFunc(src:Int, dst:Int) = gl.blendFunc(src, dst)
    def depthFunc(op:Int) = gl.depthFunc(op)
    def polygonMode(face:Int, mode:Int) = throw new RuntimeException("no polygonMode in GL ES 20, too bad")

    def pixelStore(param:Int, value:Int) = gl.pixelStorei(param, value)
    
// Utilities
    
    def printInfos() {
	    println("OpenGL version  %s".format(getString(gl.VERSION.toInt)))
		println("       glsl     %s".format(getString(gl.SHADING_LANGUAGE_VERSION.toInt)))
		println("       renderer %s".format(getString(gl.RENDERER.toInt)))
		println("       vendor   %s".format(getString(gl.VENDOR.toInt)))
	}
	
	def checkErrors() {
		val err = gl.getError
		if(err != gl.NO_ERROR) {
			var msg = err match {
				case gl.INVALID_ENUM => "invalid enum"
				case gl.INVALID_VALUE => "invalid value"
				case gl.INVALID_OPERATION => "invalid operation"
				case gl.INVALID_FRAMEBUFFER_OPERATION => "invalid framebuffer operation"
				case gl.OUT_OF_MEMORY => "out of memory"
				case _ => "unknown error"
			}
	        throw new RuntimeException("OpenGL error : %s".format(msg))
	    }
	}
}