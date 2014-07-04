package org.sofa.opengl.test

import scala.math._

import javax.media.opengl._
import javax.media.opengl.glu._
import com.jogamp.opengl.util._
import com.jogamp.newt.event._
import com.jogamp.newt.opengl._

import scala.compat.Platform
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.{ ActorSystem, Actor, ActorRef, Props, ReceiveTimeout }

import org.sofa.nio._
import org.sofa.math.{ Rgba, Vector2, Vector3, Vector4, Point3, Matrix4, Box3, Box3Default, Box3PosCentered, Box3Sized }

import org.sofa.opengl.akka.SurfaceExecutorService
import org.sofa.opengl.{ SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, WhiteLight, ColoredLight, Light, Space }
import org.sofa.opengl.mesh.{ Mesh, TrianglesMesh, VertexAttribute, AxisMesh }
import org.sofa.opengl.actor.renderer.{ Renderer, Screen, Avatar, AvatarName, AvatarBaseStates, RendererActor, RendererController, DefaultAvatarFactory, DefaultAvatarMixed, AvatarSpaceState, AvatarRenderState }
import org.sofa.opengl.actor.renderer.backend.RendererNewt
import org.sofa.opengl.actor.renderer.avatar.ui.{ UIAvatarFactory, UIPaint, UIPerspectiveStates }


object TestMeshUserAttributes extends App {
	final val Title = "MeshUserAttributes"

	SurfaceExecutorService.configure

	val system = ActorSystem(Title)

	val test = system.actorOf(Props[TestMeshUserAttributes], name = Title)

	test ! "start"
}


class TestMeshUserAttributes extends Actor {
	import RendererActor._

	protected[this] var renderer: ActorRef = null

	def receive = {
		case "start" ⇒ {
			RendererActor(context, self,
				Renderer(new UIAvatarFactory() -> new TestMeshUserAttributesAvatarFactory()),
					TestMeshUserAttributes.Title, 800, 600, 50, true, false, 4)
		}
		case RendererController.Start(renderer) ⇒ {
			this.renderer = renderer
			initTest
			context.setReceiveTimeout(100 milliseconds)
		}
		case RendererController.Exit ⇒ {
			println("renderer stoped")
			stopTest
			renderer = null
		}
		case ReceiveTimeout ⇒ {
			controlTest
		}
		case x ⇒ {
			println(s"unknown message %{x}")
		}
	}

	protected def initTest() {
		renderer ! AddResources("/TestMeshUserAttributes.xml")
		renderer ! AddScreen("main-screen")
		renderer ! SwitchScreen("main-screen")

		val root   = AvatarName("root")
		val persp  = AvatarName("root.persp")
		val ground = AvatarName("root.persp.ground")

		renderer ! AddAvatar("ui.root", root)
		renderer ! AddAvatar("ui.perspective", persp)
		renderer ! AddAvatar("ui.mesh-user-attributes", ground)

		renderer ! ChangeAvatar(ground, MeshUserAttributesAvatar.Configure("ground-shader", "ground-color"))
		renderer ! ChangeAvatar(persp, UIPerspectiveStates.Focus(Vector3(0, 0, 0)))
		renderer ! ChangeAvatar(persp, UIPerspectiveStates.Orbit(Vector3(Pi/2, Pi/2, 3)))
	}

	protected def stopTest() {
		sys.exit
	}

	protected def controlTest() {

	}
}


class TestMeshUserAttributesAvatarFactory extends DefaultAvatarFactory {
	override def avatarFor(name: AvatarName, screen: Screen, kind: String): Avatar = {
		kind match {
			case "ui.mesh-user-attributes" ⇒ new MeshUserAttributesAvatar(name, screen)
			case _ ⇒ chainAvatarFor(name, screen, kind)
		}
	}
}


object MeshUserAttributesAvatar {
	case class Configure(shader:String, textureColor:String) extends AvatarRenderState {}
}


class MeshUserAttributesAvatar(name:AvatarName, screen:Screen) extends UIPaint(name, screen) {
	import VertexAttribute._

	protected[this] var axis:AxisMesh = null

	protected[this] var ground:TrianglesMesh = null

	protected[this] var colorShader:ShaderProgram = null

	protected[this] var groundShader:ShaderProgram = null

	protected[this] var groundColor:Texture = null

	protected[this] var texDisplacement = Vector2(0, 0)

	protected[this] var dir = 0.005

	override def animateRender() {
		texDisplacement.x = texDisplacement.x + dir

		if(texDisplacement.x >= 0.1) { texDisplacement.x = 0;  }
	}

	override def changeRender(newState:AvatarRenderState) {
		newState match {
			case MeshUserAttributesAvatar.Configure(shaderId, colorId) ⇒ {
				val screen = self.screen
				val gl = screen.gl

				if(ground ne null) ground.dispose

				//   +---+---+ y+h2
				//   |2 /|\ 3|
				//   | / | \ |   <-+
				//   |/  |  \|     |
				//   +  4|5  +  |  |
				//   |\  |  /|  +--+
				//   | \ | / |
				//   |0 \|/ 1|
				//   +---+---+ y-h2
				//  x-w2    x+w2

				axis         = new AxisMesh(2)
				ground       = new TrianglesMesh(6)
				groundShader = screen.libraries.shaders.get(gl, shaderId)
				groundColor  = screen.libraries.textures.get(gl, colorId)
				colorShader  = screen.libraries.shaders.get(gl, "color-shader")

				val w2 = sqrt(3).toFloat
				val h2 = 1f

				ground.addAttribute("moving", 3)

				// Triangle 0
				ground xyz (0,  -w2, -h2, 0f)  uv (0, 0f,   0f)    user("moving", 0,  0, 0, 0)
				ground xyz (1,   0f, -h2, 0f)  uv (1, 0.5f, 0f)    user("moving", 1,  0, 0, 0)
				ground xyz (2,  -w2,  0f, 0f)  uv (2, 0f,   0.5f)  user("moving", 2,  0, 0, 0)
				// Triangle 1
				ground xyz (3,   0f, -h2, 0f)  uv(3, 0.5f, 0f)     user("moving", 3,  0, 0, 0)
				ground xyz (4,   w2, -h2, 0f)  uv(4, 1f,   0f)     user("moving", 4,  0, 0, 0)
				ground xyz (5,   w2,  0f, 0f)  uv(5, 1f,   0.5f)   user("moving", 5,  0, 0, 0)
				// Triangle 2
				ground xyz (6,  -w2,  0f, 0f)  uv(6, 0f,   0.5f)   user("moving", 6,  0, 0, 0)
				ground xyz (7,   0f,  h2, 0f)  uv(7, 0.5f, 1f)     user("moving", 7,  0, 0, 0)
				ground xyz (8,  -w2,  h2, 0f)  uv(8, 0f,   1f)     user("moving", 8,  0, 0, 0)
				// Triangle 3
				ground xyz (9,   w2,  0f, 0f) uv(9,  1f,   0.5f)   user("moving", 9,  0, 0, 0)
				ground xyz (10,  w2,  h2, 0f) uv(10, 1f,   1f)     user("moving", 10, 0, 0, 0)
				ground xyz (11,  0f,  h2, 0f) uv(11, 0.5f, 1f)     user("moving", 11, 0, 0, 0)
				// Triangle 4
				ground xyz (12,  0f, -h2, 0f) uv(12, 0.5f, 0f)     user("moving", 12, 1, 0, 0)
				ground xyz (13,  0f,  h2, 0f) uv(13, 0.5f, 1f)     user("moving", 13, 1, 0, 0)
				ground xyz (14, -w2,  0f, 0f) uv(14, 0f,   0.5f)   user("moving", 14, 1, 0, 0)
				// Triangle 5
				ground xyz (15,  0f, -h2, 0f) uv(15, 0.5f, 0f)     user("moving", 15, 1, 0, 0)
				ground xyz (16,  w2,  0f, 0f) uv(16, 1f,   0.5f)   user("moving", 16, 1, 0, 0)
				ground xyz (17,  0f,  h2, 0f) uv(17, 0.5f, 1f)     user("moving", 17, 1, 0, 0)

				ground triangle (0, 0, 1, 2)
				ground triangle (1, 3, 4, 5)
				ground triangle (2, 6, 7, 8)
				ground triangle (3, 9, 10, 11)
				ground triangle (4, 12, 13, 14)
				ground triangle (5, 15, 16, 17)

				axis.newVertexArray(gl, colorShader, Vertex -> "position", Color -> "color")
				ground.newVertexArray(gl, groundShader, Vertex -> "position", TexCoord -> "texCoords", "moving" -> "moving")
			}
			case x ⇒ println("changeRender unknown message: ${x}")
		}
	}
	
	override def paint(gl:SGL, space:Space) {
		if(ground ne null) {
			gl.disable(gl.DEPTH_TEST)
			gl.enable(gl.BLEND)
			gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA)		// Premultiplied alpha

			groundShader.use
			groundShader.uniform("texDisplacement", texDisplacement)
			groundShader.uniformTexture(groundColor, "texColor")
			space.uniformMVP(groundShader)

			ground.draw(gl)
			colorShader.use
			space.uniformMVP(colorShader)
			axis.draw(gl)
		}
	}
}