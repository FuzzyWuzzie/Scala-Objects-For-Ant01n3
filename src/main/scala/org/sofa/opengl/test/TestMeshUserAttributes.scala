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


/** How to add specific attributes used by a shader here to select which vertices
  * will animated a texture.
  *
  * This is also a demonstration of a test using actors and the new Renderer/Avatar
  * system. */
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

		renderer ! ChangeAvatar(ground, MeshUserAttributesAvatar.Configure())
		renderer ! ChangeAvatar(persp, UIPerspectiveStates.Focus(Vector3(0, 0, 0)))
		renderer ! ChangeAvatar(persp, UIPerspectiveStates.Orbit(Vector3(Pi/2, Pi/2, 3)))
	}

	protected def stopTest() { sys.exit }

	protected def controlTest() {}
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
	case class Configure() extends AvatarRenderState {}
}


class MeshUserAttributesAvatar(name:AvatarName, screen:Screen) extends UIPaint(name, screen) {
	import VertexAttribute._

	protected[this] var axis:AxisMesh = null

	protected[this] var ground:TrianglesMesh = null

	protected[this] var colorShader:ShaderProgram = null

	protected[this] var groundShader:ShaderProgram = null

	protected[this] var groundColor:Texture = null

	protected[this] var texDisplacement = Vector2(0, 0)

	protected[this] var dir = 0.001

	override def animateRender() {
		texDisplacement.x = texDisplacement.x + dir * sqrt(3)
		texDisplacement.y = texDisplacement.y + dir * 1

		if(texDisplacement.x >= 0.1) { texDisplacement.x = 0; texDisplacement.y = 0 }
	}

	override def changeRender(newState:AvatarRenderState) {
		newState match {
			case MeshUserAttributesAvatar.Configure() ⇒ {
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
				groundShader = screen.libraries.shaders.get(gl, "ground-shader")
				groundColor  = screen.libraries.textures.get(gl, "ground-color")
				colorShader  = screen.libraries.shaders.get(gl, "color-shader")

				val w2 = sqrt(3).toFloat
				val h2 = 1f

				val u0 = 0.027f
				val u1 = 0.243f
				val u2 = 0.460f
				val v0 = 0.390f
				val v1 = 0.515f
				val v2 = 0.640f

				ground.addAttribute("moving", 3)

				ground v( 0)  xyz(-w2, -h2, 0f)  uv(u0, v0)  user("moving", 0, 0, 0)	// Triangle 0
				ground v( 1)  xyz( 0f, -h2, 0f)  uv(u1, v0)  user("moving", 0, 0, 0)
				ground v( 2)  xyz(-w2,  0f, 0f)  uv(u0, v1)  user("moving", 0, 0, 0)
				ground v( 3)  xyz( 0f, -h2, 0f)  uv(u1, v0)  user("moving", 0, 0, 0)	// Triangle 1
				ground v( 4)  xyz( w2, -h2, 0f)  uv(u2, v0)  user("moving", 0, 0, 0)
				ground v( 5)  xyz( w2,  0f, 0f)  uv(u2, v1)  user("moving", 0, 0, 0)
				ground v( 6)  xyz(-w2,  0f, 0f)  uv(u0, v1)  user("moving", 0, 0, 0)	// Triangle 2
				ground v( 7)  xyz( 0f,  h2, 0f)  uv(u1, v2)  user("moving", 0, 0, 0)
				ground v( 8)  xyz(-w2,  h2, 0f)  uv(u0, v2)  user("moving", 0, 0, 0)
				ground v( 9)  xyz( w2,  0f, 0f)  uv(u2, v1)  user("moving",  0, 0, 0)	// Triangle 3
				ground v(10)  xyz( w2,  h2, 0f)  uv(u2, v2)  user("moving", 0, 0, 0)
				ground v(11)  xyz( 0f,  h2, 0f)  uv(u1, v2)  user("moving", 0, 0, 0)
				ground v(12)  xyz( 0f, -h2, 0f)  uv(u1, v0)  user("moving", 1, 0, 0)	// Triangle 4
				ground v(13)  xyz( 0f,  h2, 0f)  uv(u1, v2)  user("moving", 1, 0, 0)
				ground v(14)  xyz(-w2,  0f, 0f)  uv(u0, v1)  user("moving", 1, 0, 0)
				ground v(15)  xyz( 0f, -h2, 0f)  uv(u1, v0)  user("moving", 1, 0, 0)	// Triangle 5
				ground v(16)  xyz( w2,  0f, 0f)  uv(u2, v1)  user("moving", 1, 0, 0)
				ground v(17)  xyz( 0f,  h2, 0f)  uv(u1, v2)  user("moving", 1, 0, 0)

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