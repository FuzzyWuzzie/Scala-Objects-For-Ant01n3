package org.sofa.opengl.backend

import org.sofa.opengl.{Shader, ShaderLoader}
import java.io.{InputStream, IOException}
import android.content.res.Resources

class AndroidShaderLoader(val resources:Resources) extends ShaderLoader with AndroidLoader {
	def open(resource:String):InputStream = {
		if(exists("", resource)) {
			resources.getAssets.open(resource)
		} else {
			Shader.path.find(path => exists(path, resource)) match {
				case path:Some[String] => { resources.getAssets.open("%s/%s".format(path.get,resource)) }
				case None => { throw new IOException("cannot open shader resource %s".format(resource)) }
			}
		}
	}
}