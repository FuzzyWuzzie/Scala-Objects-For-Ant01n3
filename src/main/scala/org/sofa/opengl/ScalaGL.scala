package org.sofa.opengl

import java.nio.{Buffer, ByteBuffer=>NioByteBuffer, FloatBuffer=>NioFloatBuffer, DoubleBuffer=>NioDoubleBuffer, IntBuffer=>NioIntBuffer}
import org.sofa.nio._
import org.sofa.math.Rgba

/** Wrapper around OpenGL to provide basic functions from 3.1 an ES2.
  * 
  * The basic idea is to provide enough OpenGL to code both on the desktop and on Android. */
abstract class SGL {
// Awful constants
    
    val DEPTH_TEST:Int
    val BLEND:Int
    val SRC_ALPHA:Int
    val ONE_MINUS_SRC_ALPHA:Int
    val CULL_FACE:Int
    val BACK:Int
    val CW:Int
    val CCW:Int
    
    val COLOR_BUFFER_BIT:Int
    val DEPTH_BUFFER_BIT:Int
    val FRONT_AND_BACK:Int
    val FILL:Int
    val LINE:Int
    val UNPACK_ALIGNMENT:Int

    val TEXTURE_2D:Int
    val TEXTURE0:Int
    val TEXTURE1:Int
    val TEXTURE2:Int
    val TEXTURE_MIN_FILTER:Int
    val TEXTURE_MAG_FILTER:Int
    val TEXTURE_WRAP_S:Int
    val TEXTURE_WRAP_T:Int
    val LINEAR_MIPMAP_LINEAR:Int
    val LINEAR:Int
    val REPEAT:Int
    val CLAMP_TO_EDGE:Int
    val NEAREST:Int
    val DEPTH_COMPONENT:Int
    val FRAMEBUFFER:Int
    val DEPTH_ATTACHMENT:Int
    val COLOR_ATTACHMENT0:Int
    val FRAMEBUFFER_COMPLETE:Int
    val FRAMEBUFFER_INCOMPLETE_ATTACHMENT:Int
    val FRAMEBUFFER_INCOMPLETE_DIMENSIONS:Int
    val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:Int
    val FRAMEBUFFER_UNSUPPORTED:Int
    
    val UNSIGNED_BYTE:Int
    val UNSIGNED_INT:Int
    val UNSIGNED_SHORT:Int
    val FLOAT:Int
    val DOUBLE:Int
    val RGBA:Int
    val LUMINANCE:Int
    val LUMINANCE_ALPHA:Int
    val ALPHA:Int
    
    val ELEMENT_ARRAY_BUFFER:Int
    val ARRAY_BUFFER:Int

    val VERTEX_SHADER:Int
    val FRAGMENT_SHADER:Int
    
    val TRIANGLES:Int
    
    val EXTENSIONS:Int
    
    val STATIC_DRAW:Int
//    val STREAM_DRAW:Int
    val DYNAMIC_DRAW:Int
            
    val PROGRAM_POINT_SIZE:Int

// Info
    
    def isES:Boolean

    def getInteger(param:Int):Int
    
// Vertex arrays
	
	def genVertexArray():Int
	def deleteVertexArray(id:Int)
	
	def bindVertexArray(id:Int)
	def enableVertexAttribArray(id:Int)
	def disableVertexAttribArray(id:Int)
	def vertexAttribPointer(number:Int, size:Int, typ:Int, b:Boolean, i:Int, j:Int)
	def vertexAttribPointer(number:Int, attributeSize:Int, attributeType:Int, b:Boolean, size:Int, data:Buffer)
    def drawArrays(mode:Int, i:Int, size:Int)
    def drawElements(mode:Int, count:Int, i:Int, offset:Int)
    def multiDrawArrays(mode:Int, firsts:IntBuffer, counts:IntBuffer, primcount:Int)

// Textures
    
    def genTexture:Int
	def deleteTexture(id:Int)
	
	def activeTexture(texture:Int)
	def bindTexture(target:Int, id:Int)
	def texParameter(target:Int, name:Int, param:Float)
	def texParameter(target:Int, name:Int, param:Int)
	def texParameter(target:Int, name:Int, params:FloatBuffer)
	def texParameter(target:Int, name:Int, params:IntBuffer)
	def texImage1D(target:Int, level:Int, internalFormat:Int, width:Int, border:Int, format:Int, theType:Int, data:ByteBuffer)
	def texImage2D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, border:Int, format:Int, theType:Int, data:ByteBuffer)
    def texImage3D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, depth:Int, border:Int, format:Int, theType:Int, data:ByteBuffer)
    def generateMipmaps(target:Int)

    def genFramebuffer:Int
    def deleteFramebuffer(id:Int)
    def bindFramebuffer(target:Int, id:Int)
    def framebufferTexture2D(target:Int,attachment:Int, textarget:Int, texture:Int, level:Int)
    def checkFramebufferStatus(target:Int):Int
    
// Buffers
	
	def genBuffer():Int
	def deleteBuffer(id:Int)

	def bufferData(target:Int, data:DoubleBuffer, mode:Int)
	def bufferData(target:Int, data:Array[Double], mode:Int)
	def bufferData(target:Int, data:FloatBuffer, mode:Int)
	def bufferData(target:Int, data:Array[Float], mode:Int)
	def bufferData(target:Int, data:IntBuffer, mode:Int)
	def bufferData(target:Int, data:Array[Int], mode:Int)
    def bufferData(target:Int, data:NioBuffer, mode:Int)
    
    def bufferSubData(target:Int, offset:Int, size:Int, data:DoubleBuffer)
    def bufferSubData(target:Int, offset:Int, size:Int, data:FloatBuffer)
    def bufferSubData(target:Int, offset:Int, size:Int, data:IntBuffer)
    def bufferSubData(target:Int, offset:Int, size:Int, data:NioBuffer)
	
    def bindBuffer(mode:Int, id:Int)

// Shaders
	
	def getShaderCompileStatus(id:Int):Boolean
	def getShader(id:Int, status:Int):Int
	def getShaderInfoLog(id:Int):String 
	def getProgram(id:Int, status:Int):Int
	def getProgramLinkStatus(id:Int):Boolean
	def getProgramInfoLog(id:Int):String
	
	def createShader(shaderType:Int):Int
	def createProgram():Int
	def shaderSource(id:Int, source:Array[String])
    def shaderSource(id:Int, source:String)
	def compileShader(id:Int)
	def deleteShader(id:Int)
    def attachShader(id:Int, shaderId:Int)
    def linkProgram(id:Int)
    def useProgram(id:Int)
    def detachShader(id:Int, shaderId:Int)
    def deleteProgram(id:Int)
    def getUniformLocation(id:Int, variable:String):Int
    def uniform(loc:Int, i:Int)
    def uniform(loc:Int, i:Int, j:Int)
    def uniform(loc:Int, i:Int, j:Int, k:Int)
    def uniform(loc:Int, i:Int, j:Int, k:Int, l:Int)
    def uniform(loc:Int, i:Float)
    def uniform(loc:Int, i:Float, j:Float)
    def uniform(loc:Int, i:Float, j:Float, k:Float)
    def uniform(loc:Int, i:Float, j:Float, k:Float, l:Float)
    def uniform(loc:Int, color:Rgba)
    def uniform(loc:Int, i:Double)
    def uniform(loc:Int, i:Double, j:Double)
    def uniform(loc:Int, i:Double, j:Double, k:Double)
    def uniform(loc:Int, i:Double, j:Double, k:Double, l:Double)
    def uniformMatrix3(loc:Int, i:Int, b:Boolean, buffer:FloatBuffer)
    def uniformMatrix3(loc:Int, i:Int, b:Boolean, buffer:DoubleBuffer)
    def uniformMatrix4(loc:Int, i:Int, b:Boolean, buffer:FloatBuffer)
    def uniformMatrix4(loc:Int, i:Int, b:Boolean, buffer:DoubleBuffer)
    def uniformMatrix3(loc:Int, i:Int, b:Boolean, buffer:Array[Float])
    def uniformMatrix4(loc:Int, i:Int, b:Boolean, buffer:Array[Float])
    def uniform(loc:Int, v:Array[Float])
    def uniform(loc:Int, v:Array[Double])
    def uniform(loc:Int, v:FloatBuffer)
    def uniform(loc:Int, v:DoubleBuffer)
    def getAttribLocation(id:Int, attribute:String):Int

// Basic API
	
    def getError:Int
	def getString(i:Int):String
    def clear(mode:Int)
	def clearColor(r:Float, g:Float, b:Float, a:Float)
	def clearColor(color:Rgba)
    def clearColor(color:java.awt.Color)
    def clearDepth(value:Float)
    def viewport(x:Int, y:Int, width:Int, height:Int)
    def enable(i:Int)
    def disable(i:Int)
    def cullFace(i:Int)
    def frontFace(i:Int)
    def lineWidth(width:Float)
    def lineWidth(width:Double)
    // def pointSize(size:Float)
    // def pointSize(size:Double)
    def blendEquation(mode:Int)
    def blendFunc(src:Int, dst:Int)
    def polygonMode(face:Int, mode:Int)
    def pixelStore(param:Int, value:Int)
    
// Utilities
    
    /** Display the OpenGL version, the GLSL version, the renderer name, and renderer vendor
      * on standard output.
      */
    def printInfos()
	
	/** Check any potential error recorded by the GL.
	  * 
	  * If an error flag is found, a runtime exception is raised.
	  */
	def checkErrors()
}