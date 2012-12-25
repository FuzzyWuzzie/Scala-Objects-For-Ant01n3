package org.sofa.opengl.backend

import org.sofa.opengl.{Shader, ShaderLoader}
import java.io.{InputStream, IOException}
import android.content.res.Resources
import org.sofa.backend.AndroidLoader

class AndroidShaderLoader(val resources:Resources) extends ShaderLoader with AndroidLoader {
	def open(resource:String):InputStream = {
		resources.getAssets.open(searchInAssets(resource, Shader.path))
	}
}