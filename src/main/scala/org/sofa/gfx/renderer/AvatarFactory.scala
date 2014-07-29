package org.sofa.gfx.renderer


/** Factory for avatars and screens. */
trait AvatarFactory {
	/** Avatar factory to use when no avatar or screen is matched by the given kind. */
	protected[this] var chained:AvatarFactory = null

	/** Should produce a screen of the given `screenType`. If the screen 
	  * type is not given, a default screen is created. It is rarely needed
	  * to override the default screen type. */
	def screenFor(name:String, renderer:Renderer, screenType:String = "default"):Screen

	/** Should produce an avatar of the given `avatarType`. */
	def avatarFor(name:AvatarName, screen:Screen, avatarType:String):Avatar

	/** Indicate which avatar factory is to be used when no avatar or screen matches the given kind.
	  * This method returns a reference to this object (not the chained one). This allows to write
	  * things like `new AvatarFactoryA() -> new AvatarFactoryB() -> newAvatarFactoryC()` for
	  * example where factory A has precedence on B that in turns has precedence on C. The value
	  * of such an expression is the reference on factory A, which knows the full chain. */
	def -> (chain:AvatarFactory):AvatarFactory = { chained = chain; this }

	/** Utility to propagate a call to `screenFor()` in the chain. */
	protected def chainScreenFor(name:String, renderer:Renderer, screenType:String = "default"):Screen = {
		if(chained ne null) chained.screenFor(name, renderer, screenType)
		else throw new NoSuchScreenException(s"screen type ${screenType} unknown in this avatar factory chain")
	}

	/** Utility to propagate a call to `avatarFor()` in the chain. */
	protected def chainAvatarFor(name:AvatarName, screen:Screen, avatarType:String):Avatar = {
		if(chained ne null) chained.avatarFor(name, screen, avatarType)
		else throw new NoSuchAvatarException(s"avatar type ${avatarType} unknown in this avatar factory chain")
	}
}


class DefaultAvatarFactory extends AvatarFactory {
	def screenFor(name:String, renderer:Renderer, screenType:String = "default"):Screen = screenType match {
		case "default" => new DefaultScreen(name, renderer)
		case _         => chainScreenFor(name, renderer, screenType)
	}

	def avatarFor(name:AvatarName, screen:Screen, avatarType:String):Avatar = chainAvatarFor(name, screen, avatarType)
}