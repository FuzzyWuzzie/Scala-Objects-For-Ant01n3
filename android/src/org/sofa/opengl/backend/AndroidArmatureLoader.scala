package org.sofa.opengl.backend

import android.content.res.Resources
import org.sofa.backend.AndroidLoader
import org.sofa.simu.oberon.renderer.{Armature, ArmatureLoader, SVGArmatureLoader}

/** Default loader for shaders, based on files and the include path.
  * This loader tries to open the given resource directly, then if not
  * found, tries to find it in each of the pathes provided by the include
  * path. If not found it throws an IOException. */
class AndroidArmatureLoader(val resources:Resources) extends AndroidLoader with ArmatureLoader {
	private[this] val SVGLoader = new SVGArmatureLoader()

    def open(name:String, texRes:String, shaderRes:String, resource:String):Armature = {
        SVGLoader.load(name, texRes, shaderRes, searchInAssets(resource, Armature.path))
    }
}
