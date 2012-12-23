package org.sofa.opengl.backend

import org.sofa.opengl.SGL
import org.sofa.opengl.Texture

import android.graphics.Bitmap
import android.opengl.GLUtils

class TextureAndroidES20(gl:SGL, bitmap:Bitmap, doMipmaps:Boolean)
	extends Texture(gl, gl.TEXTURE_2D, bitmap.getWidth, bitmap.getHeight, 0) {

    initFromBitmap
    
    def initFromBitmap() {
        GLUtils.texImage2D(gl.TEXTURE_2D, 0, bitmap, 0)
        if(doMipmaps)
            gl.generateMipmaps(gl.TEXTURE_2D)
    }
}