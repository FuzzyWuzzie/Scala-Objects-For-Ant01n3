package org.sofa.opengl.test

import scala.math._
import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import org.sofa.nio._
import org.sofa.math.{Rgba, Vector3, Vector4}
import org.sofa.opengl.{SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, WhiteLight, ColoredLight, Light}
import org.sofa.opengl.io.collada.{ColladaFile}
import org.sofa.opengl.surface.{Surface, SurfaceRenderer, BasicCameraController}
import org.sofa.opengl.mesh.{PlaneMesh, Mesh, EditableMesh, VertexAttribute}

object TestShaders {
    def main(args:Array[String]):Unit = (new TestShaders)
}

class TestShaders extends SurfaceRenderer {
// General
    
    var gl:SGL = null
    var surface:Surface = null
	
// View
    
    var camera:Camera = null
    var ctrl:BasicCameraController = null
    
// Geometry

    var ground:VertexArray = null
    var uvsphere:VertexArray = null
    
    val groundMesh = new PlaneMesh(2, 2, 4, 4)
    var uvsphereMesh:Mesh = null
        
// Shading

	val whiteLight1 = WhiteLight(-2,2,-1.5, 1f, 32f, 0.1f)
	val coloredLight1 = ColoredLight(-2,2,0.5, Rgba.White, Rgba.White, Rgba.White*0.1, 1, 1, 1, 32, 0, 0.5, 1)
	val coloredLight2 = ColoredLight(-2,2,1.5, Rgba.White, Rgba.White, Rgba.White*0.1, 1, 1, 1, 32, 0, 0.5, 1)
	val coloredLight3 = ColoredLight(-2,2,-0.5, Rgba.White, Rgba.White, Rgba.White*0.1, 1, 1, 1, 32, 0, 0.5, 1)

	val color1 = Rgba(1,0.8,0.1,1)
	val color2 = Rgba(0.1,1,0.8,1)
	val color3 = Rgba(0.1,0.8,1,1)
	val color4 = Rgba(1,0.1,0.8,1)

    val shaderDesc = Array[(String,String,Rgba,Light)](
    	("es2/testshad1.vert.glsl", "es2/testshad1.frag.glsl", color1, whiteLight1),
    	("es2/testshad1.vert.glsl", "es2/testshad1.frag.glsl", color1, whiteLight1),
    	("es2/testshad1.vert.glsl", "es2/testshad1.frag.glsl", color1, whiteLight1),
    	("es2/testshad1.vert.glsl", "es2/testshad1.frag.glsl", color1, whiteLight1),
    	
    	("es2/testshad1.vert.glsl", "es2/testshad2.frag.glsl", color2, coloredLight3),
    	("es2/testshad1.vert.glsl", "es2/testshad2.frag.glsl", color2, coloredLight3),
    	("es2/testshad1.vert.glsl", "es2/testshad2.frag.glsl", color2, coloredLight3),
    	("es2/testshad1.vert.glsl", "es2/testshad2.frag.glsl", color2, coloredLight3),
    	
    	("es2/testshad1.vert.glsl", "es2/testshad3.frag.glsl", color3, coloredLight1),
    	("es2/testshad1.vert.glsl", "es2/testshad3.frag.glsl", color3, coloredLight1),
    	("es2/testshad1.vert.glsl", "es2/testshad3.frag.glsl", color3, coloredLight1),
    	("es2/testshad1.vert.glsl", "es2/testshad3.frag.glsl", color3, coloredLight1),

    	("es2/testshad1.vert.glsl", "es2/testshad4.frag.glsl", color4, coloredLight2),
    	("es2/testshad1.vert.glsl", "es2/testshad4.frag.glsl", color4, coloredLight2),
    	("es2/testshad1.vert.glsl", "es2/testshad4.frag.glsl", color4, coloredLight2),
    	("es2/testshad1.vert.glsl", "es2/testshad4.frag.glsl", color4, coloredLight2)
    )    

    val ShaderCount = shaderDesc.length

    var shader:Array[ShaderProgram] = new Array[ShaderProgram](ShaderCount)
    var groundShader:ShaderProgram = null

    var light:Array[Light] = new Array[Light](ShaderCount)
        
    val clearColor = Rgba.Grey30
    var groundColor:Texture = null
    var groundNMap:Texture = null
    var uvsphereColor:Texture = null


// Go
        
    build()

    private def build() {
	    camera   = Camera(); camera.viewportPx(1280, 800)
	    val caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2))
	    
		caps.setDoubleBuffered(true)
		caps.setHardwareAccelerated(true)
		caps.setSampleBuffers(true)
		caps.setNumSamples(4)

        ctrl           = new BasicCameraController(camera)
	    initSurface    = initializeSuface
	    frame          = display
	    surfaceChanged = reshape
	    close          = { surface => sys.exit }
	    key            = ctrl.key
	    motion         = ctrl.motion
	    scroll         = ctrl.scroll
	    surface        = new org.sofa.opengl.backend.SurfaceNewt(this,
	    					camera, "Shaders", caps,
	    					org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)
	}
    
// Rendering
    
	def initializeSuface(gl:SGL, surface:Surface) {
	    Shader.path      += "/Users/antoine/Documents/Programs/SOFA/src/main/scala/org/sofa/opengl/shaders/"
	    Texture.path     += "/Users/antoine/Documents/Programs/SOFA/textures"
		ColladaFile.path += "/Users/antoine/Documents/Art/Sculptures/Blender/"

	    initGL(gl)
	    initTextures
        initShaders
        initModels
	    initGeometry
	    
	    camera.viewCartesian(0, 5, 4)
	    camera.setFocus(0, 0, 0)
	    reshape(surface)
	}

	protected def initGL(sgl:SGL) {
	    gl = sgl
	    
        gl.clearColor(clearColor)
	    gl.clearDepth(1f)
	    gl.enable(gl.DEPTH_TEST)
	    
	    gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CW)
        
        gl.disable(gl.BLEND)
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
	}
	
	protected def initShaders() {
		var i = 0

		while(i < ShaderCount) {
			light(i)  = shaderDesc(i)._4
			shader(i) = ShaderProgram(gl, "shader%d".format(i), shaderDesc(i)._1, shaderDesc(i)._2)
			i += 1
		}

	    groundShader = ShaderProgram(gl, "plain", "es2/plainColor.vert.glsl", "es2/plainColor.frag.glsl")
	}

	protected def initModels() {
		val model = new ColladaFile("UVSphere.dae")

		model.library.geometry("Icosphere").get.mesh.mergeVertices(true)
		model.library.geometry("Icosphere").get.mesh.blenderToOpenGL(true)
		uvsphereMesh = model.library.geometry("Icosphere").get.mesh.toMesh
		uvsphereMesh.asInstanceOf[EditableMesh].autoComputeTangents(true)
	}
	
	protected def initGeometry() {
		import VertexAttribute._

		groundMesh.setTextureRepeat(30, 30)

	    ground   = groundMesh.newVertexArray(  gl, groundShader, Vertex -> "position", Color -> "color")// Normal -> "normal", Tangent -> "tangent", TexCoord -> "texCoords")
	    uvsphere = uvsphereMesh.newVertexArray(gl, shader(0),    Vertex -> "position", Normal -> "normal", Tangent -> "tangent", TexCoord -> "texCoord")
	}
	
	protected def initTextures() {
//	    groundColor = new Texture(gl, "textures/Ground.png", true)
	    // groundColor.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    // groundColor.wrap(gl.REPEAT)

	    // groundNMap = new Texture(gl, "textures/GroundNMap.png", true)
	    // groundNMap.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    // groundNMap.wrap(gl.REPEAT)

	    // uvsphereColor = new Texture(gl, "textures/CheckerBoard.png", true)
	    // uvsphereColor.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    // uvsphereColor.wrap(gl.REPEAT)
	}
	
	def reshape(surface:Surface) {
	    camera.viewportPx(surface.width, surface.height)
        var ratio = camera.viewportRatio
        gl.viewport(0, 0, camera.viewportPx.x.toInt, camera.viewportPx.y.toInt)
        camera.frustum(-ratio, ratio, -1, 1, 2)
	}
	
	def display(surface:Surface) {
	    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
	    
	    camera.viewLookAt

		// Ground

	  	gl.enable(gl.BLEND)
	    groundShader.use
	    //useTextures(groundShader, groundColor, groundNMap)
	    camera.setUniformMVP(groundShader)
	    ground.draw(groundMesh.drawAs)
		gl.disable(gl.BLEND)

		gl.frontFace(gl.CCW)

		var x = -1.5
		var y =  1.0
		var z = -1.5

		val xmax = 1.5
		val zmax = 1.5
		var i = 0

		while(z <= zmax) {
			x = -1.5
			while(x <= xmax) {
				if(i < ShaderCount) {
					shader(i).use
					shader(i).uniform("uniColor", shaderDesc(i)._3)
					light(i).uniform(shader(i), camera)
					camera.pushpop {
						camera.translateModel(x, y, z)
						camera.scaleModel(0.45, 0.45, 0.45)
						camera.uniformMVP(shader(i))
						uvsphere.draw(uvsphereMesh.drawAs)
					}

					i += 1
				}
				x += 1.0
			}
			z += 1.0
		}


		gl.frontFace(gl.CW)
	    
	    // Ok

	    surface.swapBuffers
	    gl.checkErrors
	}
	
	def useTextures(shader:ShaderProgram, color:Texture, nmap:Texture) {
	   	color.bindTo(gl.TEXTURE0)
	    shader.uniform("texColor", 0)
	    nmap.bindTo(gl.TEXTURE1)
	    shader.uniform("texNormal", 1)
	}
}