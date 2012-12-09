package org.sofa.opengl.test

import scala.math._
import org.sofa.opengl._
import org.sofa.opengl.mesh._
import org.sofa.opengl.surface._
import org.sofa.nio._
import org.sofa.math._
import javax.media.opengl.GLProfile

object NormalMap {
	def main(args:Array[String]):Unit = (new NormalMap)
}

class NormalMap extends SurfaceRenderer {
    var gl:SGL = null
    var surface:Surface = null
    
    val projection:Matrix4 = new Matrix4
    val modelview = new MatrixStack(new Matrix4)
    
    var nmapShader:ShaderProgram = null 
    
    val planeMesh = new PlaneMesh(2, 2, 4, 4)
    var plane:VertexArray = null
    val tubeMesh = new CylinderMesh(0.5f, 1, 16, 1)
    var tube:VertexArray = null
    
    var uvTex:Texture = null
    var specTex:Texture = null
    var nmapTex:Texture = null

    var camera:Camera = null
    var ctrl:BasicCameraController = null
    val clearColor = Rgba.black
    val light1 = Vector4(2, 2, 2, 1)
    
    build
    
	def build() {
//	    val caps = new javax.media.opengl.GLCapabilities(GLProfile.get(GLProfile.GL3))
	    val caps = new javax.media.opengl.GLCapabilities(GLProfile.getGL2ES2)

		Texture.path += "textures/"	    
		Shader.path += "src/main/scala/org/sofa/opengl/shaders/"

	    caps.setDoubleBuffered(true)
	    caps.setHardwareAccelerated(true)
	    caps.setSampleBuffers(true)
	    caps.setRedBits(8)
	    caps.setGreenBits(8)
	    caps.setBlueBits(8)
	    caps.setAlphaBits(8)
	    caps.setNumSamples(4)

        camera         = Camera()
	    ctrl           = new MyCameraController(camera, light1)
	    initSurface    = initializeSurface
	    frame          = display
	    surfaceChanged = reshape
	    key            = ctrl.key
	    motion         = ctrl.motion
	    scroll         = ctrl.scroll
	    close          = { surface => sys.exit }
	    surface        = new org.sofa.opengl.backend.SurfaceNewt(this, camera, "Normal mapping", caps,
	    					org.sofa.opengl.backend.SurfaceNewtGLBackend.GL2ES2)
//	    					org.sofa.opengl.backend.SurfaceNewtGLBackend.GL3)
	}
	
	def initializeSurface(sgl:SGL, surface:Surface) {
	    gl = sgl
	    
	    gl.clearColor(clearColor)
	    gl.clearDepth(1f)
	    gl.enable(gl.DEPTH_TEST)
	    //gl.polygonMode(GL_FRONT_AND_BACK, GL_LINE)
	    
	    gl.enable(gl.CULL_FACE)
        gl.cullFace(gl.BACK)
        gl.frontFace(gl.CW)

	    setup(surface)
	}
	
	def setup(surface:Surface) {
		import VertexAttribute._

	    camera.viewCartesian(2, 2, 2)
	    
	    nmapShader = new ShaderProgram(gl, "normal map phong",
	            new VertexShader(gl, "nmap phong", "es2/nmapPhong.vert"),
	            new FragmentShader(gl, "nmap phong", "es2/nmapPhong.frag"))
//	            new VertexShader(gl, "src-scala/org/sofa/opengl/shaders/nmapPhong.vert"),
//	            new FragmentShader(gl, "src-scala/org/sofa/opengl/shaders/nmapPhong.frag"))
	    
	    reshape(surface)
	    
	    tubeMesh.setTopDiskColor(Rgba.yellow)
	    tubeMesh.setBottomDiskColor(Rgba.yellow)
	    //tubeMesh.setDiskColor(4, Rgba.red)
	    tubeMesh.setCylinderColor(Rgba.blue);
	    planeMesh.setColor(Rgba.magenta)
	    
	    plane = planeMesh.newVertexArray(gl, nmapShader, Vertex -> "position", Normal -> "normal", Tangent -> "tangent", TexCoord -> "texCoords")
	    tube  = tubeMesh.newVertexArray( gl, nmapShader, Vertex -> "position", Normal -> "normal", Tangent -> "tangent", TexCoord -> "texCoords")

	    uvTex = new Texture(gl, "color.png", true)	    
//	    uvTex = new Texture(gl, "stone_wall__.jpg", true)
//	    uvTex = new Texture(gl, "textures/face.jpg", true)
	    uvTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    uvTex.wrap(gl.REPEAT)

	    specTex = new Texture(gl, "specular.png", true)
	    specTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    specTex.wrap(gl.REPEAT)
	    
//	    nmapTex = new Texture(gl, "NormalFlat.png", false)
//	    nmapTex = new Texture(gl, "facenrm.jpg", true)
//	    nmapTex = new Texture(gl, "stone_wall_normal_map__.jpg", true)
		nmapTex = new Texture(gl, "normal.png", true)
	    nmapTex.minMagFilter(gl.LINEAR_MIPMAP_LINEAR, gl.LINEAR)
	    nmapTex.wrap(gl.REPEAT)
	}
	
	def reshape(surface:Surface) {
	    camera.viewportPx(surface.width, surface.height)
        var ratio = camera.viewportRatio
        
        gl.viewport(0, 0, surface.width, surface.height)
        camera.frustum(-ratio, ratio, -1, 1, 1)
	}
	
	def display(surface:Surface) {
	    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
	    
	    nmapShader.use

	    camera.viewLookAt
	    
	    setupLights
	    setupTextures
	    
	    camera.uniformMVP(nmapShader)
	    plane.draw(planeMesh.drawAs)
/*	    tube.draw(tubeMesh.drawAs)

	    camera.pushpop {
		    camera.translateModel(0, 1.1, 0)
		    camera.uniformMVP(nmapShader)
		    tube.draw(tubeMesh.drawAs)
	    }
*/
	    camera.pushpop {
		    camera.translateModel(-1, 0, -1)
		    camera.uniformMVP(nmapShader)
		    tube.draw(tubeMesh.drawAs)
		    
		    camera.translateModel(0, 1.1, 0)
		    camera.uniformMVP(nmapShader)
		    tube.draw(tubeMesh.drawAs)
	    }
	    
	    surface.swapBuffers
	}
	
	def setupLights() {
	    // We need to position the light by ourself, but avoid doing
	    // it at each pixel in the shader.
	    
	    nmapShader.uniform("lightPos", Vector3(camera.modelview.top * light1))
	    nmapShader.uniform("lightIntensity", 5f)
	    nmapShader.uniform("ambientIntensity", 0.1f)
	    nmapShader.uniform("specularPow", 16f)
	}
	
	def setupTextures() {
	    uvTex.bindTo(gl.TEXTURE0)
	    nmapShader.uniform("texColor", 0)	// Texture Unit 0
//	    specTex.bindTo(gl.TEXTURE1)
//	    nmapShader.uniform("texSpec", 1)	// Texture Unit 1
	    nmapTex.bindTo(gl.TEXTURE2)
	    nmapShader.uniform("texNormal", 2)	// Texture Unit 2
	}
}

class MyCameraController(camera:Camera, light:Vector4) extends BasicCameraController(camera) {
    override def key(surface:Surface, keyEvent:KeyEvent) {
        import keyEvent.ActionChar._
        if(keyEvent.isShiftDown) {
            if(! keyEvent.isPrintable) {
                keyEvent.actionChar match {
                    case Up       => { light.x -= 0.1 }
                    case Down     => { light.x += 0.1 }
                    case Right    => { light.z -= 0.1 }
                    case Left     => { light.z += 0.1 }
                    case PageUp   => { light.y += 0.1 }
                    case PageDown => { light.y -= 0.1 }
                    case _ => {}
                }
            }
        } else {
            super.key(surface, keyEvent)
        }
    }
}