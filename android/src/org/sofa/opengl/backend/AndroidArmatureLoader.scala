package org.sofa.opengl.backend

import android.content.res.Resources
import org.sofa.backend.AndroidLoader
import org.sofa.opengl.armature.{Armature, ArmatureLoader, SVGArmatureLoader, ARMArmatureLoader}

/** Default loader for shaders, based on files and the include path.
  * This loader tries to open the given resource directly, then if not
  * found, tries to find it in each of the pathes provided by the include
  * path. If not found it throws an IOException. */
class AndroidArmatureLoader(val resources:Resources) extends AndroidLoader with ArmatureLoader {
	private[this] lazy val SVGLoader = new SVGArmatureLoader()
	private[this] lazy val ARMLoader = new ARMArmatureLoader()

    def open(name:String, texRes:String, shaderRes:String,
    		 resource:String, armatureId:String="Armature", scale:Double = 1.0):Armature = {
		
		if(resource.endsWith(".arm")) {
			ARMLoader.load(resources.getAssets.open(searchInAssets(resource, Armature.path)), resource, texRes, shaderRes, scale)
		} else {
			SVGLoader.load(name, texRes, shaderRes, resources.getAssets.open(searchInAssets(resource, Armature.path)), armatureId, scale)
		}
    }
}
