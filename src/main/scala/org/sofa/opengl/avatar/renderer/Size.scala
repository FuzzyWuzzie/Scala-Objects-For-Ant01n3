package org.sofa.opengl.avatar.renderer

/** Allow to specify sizes for avatar.
  * A size object express a size using a given set of descriptors, some
  * give absolute measures, some are measures compared to a resource,
  * or another avatar. */
trait Size {}

/** Absolute value for the size in 3D of an avatar, the valuers are expressed in game units. */
case class SizeTriplet(x:Double, y:Double, z:Double) extends Size {}

/** Take the value from the given texture resource. The width will be `scale` game units the
  * height will be `scale` time the ratio height/width of the texture, and the z dimension 
  * will be zero. */
case class SizeFromTextureWidth(scale:Double, fromTexture:String) extends Size {}

/** Take the value from the given texture resource. The height will be `scale` game units the
  * width will be `scale` time the ratio width/height of the texture, and the z dimension 
  * will be zero. */
case class SizeFromTextureHeight(scale:Double, fromTexture:String) extends Size {}

/** Take the width of the screen, mutlitply it by the given `scale`. This will be
  * the width of the resulting texture. The height is this width times the ratio
  * height/width of the given texture `fromTexture`. The z dimension will be zero. */
case class SizeFromScreenWidth(scale:Double, fromTexture:String) extends Size {}

/** Take the height of the screen, mutlitply it by the given `scale`. This will be
  * the height of the resulting texture. The width is this height times the ratio
  * width/height of the given texture `fromTexture`. The z dimension will be zero. */
case class SizeFromScreenHeight(scale:Double, fromTexture:String) extends Size {}