package org.sofa.backend

import org.sofa.{Environment, EnvironmentLoader}
import java.io.{InputStream, IOException}
import android.content.res.Resources

class AndroidEnvironmentLoader(val resources:Resources) extends EnvironmentLoader with AndroidLoader {
    def open(resource:String):InputStream = {
    	resources.getAssets.open(searchInAssets(resource, Environment.path))
    }
}