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
import org.sofa.math.{ Rgba, Vector3, Vector4, Point3, Matrix4, Box3, Box3Default, Box3PosCentered, Box3Sized }

import org.sofa.opengl.akka.SurfaceExecutorService
import org.sofa.opengl.{ SGL, Camera, VertexArray, ShaderProgram, Texture, Shader, WhiteLight, ColoredLight, Light, Space }
import org.sofa.opengl.mesh.{ Mesh, PlaneMesh, VertexAttribute }
import org.sofa.opengl.actor.renderer.{ Renderer, Screen, Avatar, AvatarName, AvatarBaseStates, RendererActor, RendererController, DefaultAvatarFactory, DefaultAvatarMixed, AvatarSpaceState, AvatarRenderState }
import org.sofa.opengl.actor.renderer.backend.RendererNewt
import org.sofa.opengl.actor.renderer.avatar.ui.{ UIAvatarFactory, UIPaint }


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

		val root  = AvatarName("root")
		val persp = AvatarName("root.persp")
		val plane = AvatarName("root.persp.plane")

		renderer ! AddAvatar("ui.root", root)
		renderer ! AddAvatar("ui.perspective", persp)
		renderer ! AddAvatar("ui.plane", plane)

		renderer ! ChangeAvatar(plane, MeshUserAttributesAvatar.Configure(2, 2,
						"ground-shader", "ground-color", "ground-mask"))
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
	case class Configure(nVertX:Int, nVertY:Int, shader:String,
					textureColor:String, textureMask:String) extends AvatarRenderState {}	
}


class MeshUserAttributesAvatar(name:AvatarName, screen:Screen) extends UIPaint(name, screen) {
	import VertexAttribute._

	protected[this] var ground:PlaneMesh = null

	protected[this] var groundShader:ShaderProgram = null

	protected[this] var groundColor:Texture = null

	protected[this] var groundMask:Texture = null

	override def changeRender(newState:AvatarRenderState) {
		newState match {
			case MeshUserAttributesAvatar.Configure(nx, ny, shaderId, colorId, maskId) ⇒ {
				val screen = self.screen
				val gl = screen.gl

				if(ground ne null) ground.dispose

				ground       = new PlaneMesh(nx, ny, 1, 1)
				groundShader = screen.libraries.shaders.get(gl, shaderId)
				groundColor  = screen.libraries.textures.get(gl, colorId)
				groundMask   = screen.libraries.textures.get(gl, maskId)

				ground.newVertexArray(gl, groundShader, Vertex -> "position", TexCoord -> "texCoords")
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
			//groundColor.bindUniform(gl.TEXTURE0, groundShader, "texColor")
			//groundMask.bindUniform(gl.TEXTURE1, groundShader, "texMask")
			groundShader.uniformTexture(gl.TEXTURE0, groundColor, "texColor")
			groundShader.uniformTexture(gl.TEXTURE1, groundMask, "texMask")
			ground.draw(gl)
		}
	}
}