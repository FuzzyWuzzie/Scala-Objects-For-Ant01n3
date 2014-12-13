package org.sofa.gfx

import java.nio.{Buffer, ByteBuffer=>NioByteBuffer, FloatBuffer=>NioFloatBuffer, DoubleBuffer=>NioDoubleBuffer, IntBuffer=>NioIntBuffer}
import org.sofa.nio._
import org.sofa.math.Rgba

/** Wrapper around OpenGL to provide basic functions from 3.1 an ES 3.
  * 
  * The basic idea is to provide enough OpenGL to code both on the desktop and on Android. 
  *
  * Only one version is provided ! This is intentional the idea is to stay at a level where
  * mainstream hardware is supported. Actually, OpenGL 3.1 and ES 3.0. */
abstract class SGL {
// Awful constants
    
    val DEPTH_TEST:Int
    val BLEND:Int
    val SRC_ALPHA:Int
    val BLEND_SRC:Int
    val BLEND_DST:Int
    val ONE:Int
    val ONE_MINUS_SRC_ALPHA:Int
    val CULL_FACE:Int
    val BACK:Int
    val CW:Int
    val CCW:Int
    val SCISSOR_TEST:Int
    
    val COLOR_BUFFER_BIT:Int
    val DEPTH_BUFFER_BIT:Int
    val FRONT_AND_BACK:Int
    val FRONT_FACE:Int
    val FILL:Int
    val LINE:Int
    val LINE_SMOOTH:Int
    val UNPACK_ALIGNMENT:Int

    val TEXTURE_2D:Int
    val TEXTURE_2D_MULTISAMPLE:Int
    val TEXTURE0:Int
    val TEXTURE1:Int
    val TEXTURE2:Int
    val TEXTURE_MIN_FILTER:Int
    val TEXTURE_MAG_FILTER:Int
    val TEXTURE_WRAP_S:Int
    val TEXTURE_WRAP_T:Int
//    val TEXTURE_BASE_LEVEL:Int
//    val TEXTURE_MAX_LEVEL:Int
    val LINEAR_MIPMAP_NEAREST:Int
    val NEAREST_MIPMAP_NEAREST:Int
    val LINEAR_MIPMAP_LINEAR:Int
    val NEAREST_MIPMAP_LINEAR:Int
    val LINEAR:Int
    val REPEAT:Int
    val CLAMP_TO_EDGE:Int
    val MIRRORED_REPEAT:Int
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
    
	val NEVER:Int
	val LESS:Int 
	val EQUAL:Int 
	val LEQUAL:Int 
	val GREATER:Int 
	val NOTEQUAL:Int 
	val GEQUAL:Int 
	val ALWAYS:Int 

	val INT:Int
    val UNSIGNED_BYTE:Int
    val UNSIGNED_INT:Int
    val UNSIGNED_SHORT:Int
    val FLOAT:Int
    val DOUBLE:Int
    val RGBA:Int
    val RGBA8:Int
    val LUMINANCE:Int
    val LUMINANCE_ALPHA:Int
    val ALPHA:Int

    val ELEMENT_ARRAY_BUFFER:Int
    val ARRAY_BUFFER:Int

    val VERTEX_SHADER:Int
    val FRAGMENT_SHADER:Int
    val GEOMETRY_SHADER:Int
    
    val POINTS:Int
    val LINE_STRIP:Int
    val LINES:Int
    val LINE_LOOP:Int
    val TRIANGLES:Int
    val TRIANGLE_STRIP:Int
    val TRIANGLE_FAN:Int
    
    val EXTENSIONS:Int
    
    val STATIC_DRAW:Int
//    val STREAM_DRAW:Int
    val DYNAMIC_DRAW:Int
            
    val PROGRAM_POINT_SIZE:Int

    var ShaderVersion:String

// Info
    
    def isES:Boolean

    def getInteger(param:Int):Int

    def isEnabled(param:Int):Boolean
    
// Vertex arrays
	
	def createVertexArray():AnyRef
	def deleteVertexArray(id:AnyRef)
	
	def bindVertexArray(id:AnyRef)
	def enableVertexAttribArray(index:Int)
	def disableVertexAttribArray(index:Int)
	def vertexAttribPointer(number:Int, size:Int, typ:Int, b:Boolean, i:Int, j:Int)
	//def vertexAttribPointer(number:Int, attributeSize:Int, attributeType:Int, b:Boolean, size:Int, data:Buffer)
    def drawArrays(mode:Int, i:Int, size:Int)
    def drawElements(mode:Int, count:Int, i:Int, offset:Int)
    def multiDrawArrays(mode:Int, firsts:IntBuffer, counts:IntBuffer, primcount:Int)

// Textures
    
    def createTexture:AnyRef
	def bindTexture(target:Int, id:AnyRef)
	def deleteTexture(id:AnyRef)

	def activeTexture(texture:Int)
	def texParameter(target:Int, name:Int, param:Float)
	def texParameter(target:Int, name:Int, param:Int)
	def texParameter(target:Int, name:Int, params:FloatBuffer)
	def texParameter(target:Int, name:Int, params:IntBuffer)
	def texImage1D(target:Int, level:Int, internalFormat:Int, width:Int, border:Int, format:Int, theType:Int, data:ByteBuffer)
	def texImage2D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, border:Int, format:Int, theType:Int, data:ByteBuffer)
    def texImage3D(target:Int, level:Int, internalFormat:Int, width:Int, height:Int, depth:Int, border:Int, format:Int, theType:Int, data:ByteBuffer)
    def hasTexImage2DMultisample:Boolean
    def texImage2DMultisample(target:Int, samples:Int, internalFormat:Int, width:Int, height:Int, fixedSampleLocations:Boolean)
    def generateMipmaps(target:Int)

    def createFramebuffer:AnyRef
    def deleteFramebuffer(id:AnyRef)
    def bindFramebuffer(target:Int, id:AnyRef)
    def framebufferTexture2D(target:Int,attachment:Int, textarget:Int, textureId:AnyRef, level:Int)
    def checkFramebufferStatus(target:Int):Int
    
// Buffers

	def createBuffer():AnyRef
    def bindBuffer(mode:Int, id:AnyRef)
	def deleteBuffer(id:AnyRef)

	def bufferData(target:Int, data:DoubleBuffer, mode:Int)
	def bufferData(target:Int, data:Array[Double], mode:Int)
	def bufferData(target:Int, data:FloatBuffer, mode:Int)
	def bufferData(target:Int, data:Array[Float], mode:Int)
	def bufferData(target:Int, data:IntBuffer, mode:Int)
	def bufferData(target:Int, data:Array[Int], mode:Int)
    def bufferData(target:Int, data:NioBuffer, mode:Int)

    def bufferSubData(target:Int, offset:Int, size:Int, data:DoubleBuffer) { bufferSubData(target, offset, size, data, true) }
    def bufferSubData(target:Int, offset:Int, size:Int, data:FloatBuffer) { bufferSubData(target, offset, size, data, true) }
    def bufferSubData(target:Int, offset:Int, size:Int, data:IntBuffer) { bufferSubData(target, offset, size, data, true) }
    def bufferSubData(target:Int, offset:Int, size:Int, data:ByteBuffer) { bufferSubData(target, offset, size, data, true) }
    
    def bufferSubData(target:Int, offset:Int, size:Int, data:DoubleBuffer, alsoPositionInData:Boolean)
    def bufferSubData(target:Int, offset:Int, size:Int, data:FloatBuffer, alsoPositionInData:Boolean)
    def bufferSubData(target:Int, offset:Int, size:Int, data:IntBuffer, alsoPositionInData:Boolean)
    def bufferSubData(target:Int, offset:Int, size:Int, data:ByteBuffer, alsoPositionInData:Boolean)

// Shaders
	
	def createShader(shaderType:Int):AnyRef
	def getShaderCompileStatus(id:AnyRef):Boolean
	def getShader(id:AnyRef, status:Int):Int
	def getShaderInfoLog(id:AnyRef):String 
	def shaderSource(id:AnyRef, source:Array[String])
    def shaderSource(id:AnyRef, source:String)
	def compileShader(id:AnyRef)
	def deleteShader(id:AnyRef)

	def createProgram():AnyRef
	def getProgram(id:AnyRef, status:Int):Int
	def getProgramLinkStatus(id:AnyRef):Boolean
	def getProgramInfoLog(id:AnyRef):String
    def attachShader(id:AnyRef, shaderId:AnyRef)
    def detachShader(id:AnyRef, shaderId:AnyRef)
    def linkProgram(id:AnyRef)
    def useProgram(id:AnyRef)
    def deleteProgram(id:AnyRef)	

    def getAttribLocation(id:AnyRef, attribute:String):Int
    def getUniformLocation(id:AnyRef, variable:String):AnyRef

    def uniform(loc:AnyRef, i:Int)
    def uniform(loc:AnyRef, i:Int, j:Int)
    def uniform(loc:AnyRef, i:Int, j:Int, k:Int)
    def uniform(loc:AnyRef, i:Int, j:Int, k:Int, l:Int)
    def uniform(loc:AnyRef, i:Float)
    def uniform(loc:AnyRef, i:Float, j:Float)
    def uniform(loc:AnyRef, i:Float, j:Float, k:Float)
    def uniform(loc:AnyRef, i:Float, j:Float, k:Float, l:Float)
    def uniform(loc:AnyRef, color:Rgba)
    def uniform(loc:AnyRef, i:Double)
    def uniform(loc:AnyRef, i:Double, j:Double)
    def uniform(loc:AnyRef, i:Double, j:Double, k:Double)
    def uniform(loc:AnyRef, i:Double, j:Double, k:Double, l:Double)
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:FloatBuffer)
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:DoubleBuffer)
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:FloatBuffer)
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:DoubleBuffer)
    def uniformMatrix3(loc:AnyRef, i:Int, b:Boolean, buffer:Array[Float])
    def uniformMatrix4(loc:AnyRef, i:Int, b:Boolean, buffer:Array[Float])
    def uniform(loc:AnyRef, v:Array[Float])
    def uniform(loc:AnyRef, v:Array[Double])
    def uniform(loc:AnyRef, v:FloatBuffer)
    def uniform(loc:AnyRef, v:DoubleBuffer)

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
    def depthFunc(op:Int)
    def polygonMode(face:Int, mode:Int)
    def pixelStore(param:Int, value:Int)
    def scissor(x:Int, y:Int, width:Int, height:Int)
    
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