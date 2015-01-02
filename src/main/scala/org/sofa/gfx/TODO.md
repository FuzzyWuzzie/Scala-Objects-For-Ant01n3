# TODO

## Priority list and roadmap:

UI

0. A layer in UIList that allows to scroll easily.
1. dirtyRender & dirtyPosition
    - Make this more general in Avatars ?
2. Display Lists

Game

0. Creation and representation of the map in the model.
1. Displacement on the map
    - On the model side.
    - On the avatar side, we provides simple ways to displace avatars.
2. Add a more efficient ordered rendering in AvatarContainer.
3. Add selective rendering.
4. Picking.

## General

* Rename SOFA, but to what ?
    - Alpha α ?
    - Gamma Ɣ ?
    - Phi φ ?
    - SOFA Σοφα ?
* Instead of rename, maybe cut it in several sub-libs
    - GFX ƔφX
    - SET Σ
    - MATH
    - NIO

## NIO

* backup a float buffer by a java array with faster read/write accesses.
    - When the buffer is requested, copy back all non data to the corresponding NIO buffer.
    - Make tests to see if this is faster.
    - NIO buffers are such a pain...

## Avatars

* Rationale on the new *ordered and filtered rendering* in `AvatarContainer`:
    - It is possible to specify a `RenderFilter`.
    - The render filter selects which sub-avatars are rendered and in which order.
    - There are actually two ways to specify the filter:
        + a list of names.
        + a predicate and an order (select and sort).
    - Actually filters are objects that have a "dirty" flag.
        + This flag is set automatically when the filter is first set and when avatars are added or removed.
        + There is a "requestFiltering" option that allows avatars or users to ask the filter to run anew.
    - -> the next thing to determine is how to choose when this flag is set ?
        + With events ? (AvatarAdded, AvatarRemoved, Frame, AvatarAnimated ?)
        + Uniquely with the requestFiltering call ?
* Handle *picking* -> allow avatars to register in a spaceHash ? 
    - a general one ? one per sub-avatar ? one in a specific avatar ?
    - When several avatars are picked, how to sort them ?
    - -> this is specific to each avatar sub-library (UI, Game, etc.) Partly done in UI.

## Mesh

* Mesh was always thought as a factory to produce vertex arrays. Two problems :
    - With dynamic meshes, come the idea that the last produced vertex array can be updated with the changes occuring in the mesh. This is quite confusing.
    - Meshes copy their data from Nio buffers to OpenGL buffers. However OpenGL allows to map a buffer to avoid copy, it would be far more efficient if the mesh buffers were maps from OpenGL buffers. -> this seems to be usable only on OpenGL ES 3.0. Therefore this has to be optionnal.
* Wouldn't it be better if Mesh was tied to a hidden vertex array ?
    - It would have draw methods.
    - It would handle efficiently its data internally when possible.
    - It would allocate its own vertex array(s) when needed.
    - It would be linked with the shaders used to generate the vertex array(s).
* Wouldn't it be better if mesh attributes where tied to the ArrayBuffer ? 
    - This would allow eventually to always use the array buffer as storage by mapping it.
    - This requires a big change -> passing to OpenGL ES 3.0

Plan :
    - Add map to ScalaGL and ArrayBuffer.
    - Allow meshes to share their attributes.
    - Change mesh to use only one vertex array.
    - Change one mesh to use it instead.
    - Adapt all meshes to use it.

## Behaviors/Armature

* Extend behavior to Avatars in order to move/scale/rotate them ?
    - Started : the generic behaviors have been put in a dedicated library.
* Handle animation of particles in behaviors ?
    - Allow a part of an armature to be instanced multiple times for particles ?
    - Already possible with almost no change in the Armature.
* Add `Composition` behavior to activate several behaviors at will.
* Add `InParallelLoop` that runs several behaviors in parallel repeating them as fast as possible.
    The current way is to put a "InParallel" in a "Loop", but this forces to wait for the end
    of the longest behavior in the "InParallel" behavior.
* Add `FromVariable` behavior that scale a value (scale, translation, angle) to an external variable. Question : how to specify how to reach/read the variable. (via an interface on objects that can be named in the library ? --> AnimatedValues ?)
* Uses the new TextureFramebuffer of AvatarRender to store the armature result when non-animating ?

## Library/Resources

* Allow the `Library` to be actor based, loading elements in the background ? Return a Future instead of an item ? How to handle the fact OpenGL is not thread safe ?
* Change the loadresources of Libraries, the XML lib of Scala is very slow. A dedicated format ? JSON ??
    - JSON done but loading is still very slow -> use a dedicated parser done with Jackson.

## Surface/Renderer

* Allow the renderer to choose if base/high-level events are sent, as in surfaces.
* Actually the surface newt uses GL2ES2 ... should use GL3 -> bug in VertexArrays -> Bugs in SOFAtest

## Events

* Shortcuts

## Game

* Handle paths inside each cell of the cell grids, allowing Entities to follow these paths. Use a graph ? Use only "sub-cells" ?
    - This could be only a hash-space based set of obstacles, and a grid space ?
    - However how do we do our A* on this ?
    - Avatar could be equiped to move linearly between two points at a given speed.
    - The model is where the complex calculus of displacement takes place.
    - Events are exchanged between avatars and the model to tell when the avatar reached its position ?
        + The other way to do it is to tell the avatar that at the given time it must be at a given point.
        + The avatar computes the displacement based on the frame time.
* A *level file* with the right reader to load levels. An ASCII map ?
    - a format to store it on disk.
    - a way to represent it on memory.
    - a way to communicate with entities actors ? (constraints, events...)


## UI

* The way avatars know they must be rendered anew or layout anew is still to refine.
    - It worked as long as avatars dediced to render themselves only or not, but with layers and off-screen buffers, a parent avatar may  buffer a whole hierarchy of sub-avatars. Therefore when one of the sub-avatars change, we need a way to re-render the hierarchy at least from the closest parent that have a layer.
        + This could be done by going up to the hierarchy until the first "layer" parent. But what if there is no such parent ?
        + This could be done as in Android with an invalidation of an area and the re-render of all elements that intersect this area ?
        + Each sub-avatar may know the closest parent that have a layer ? Hacky.
* A way to tell if an avatar is opaque or not ?
* A way to choose if a `glClear` is needed or not. Most of the time the whole UI fills the screen.
* An "action bar" à la Android ?
* Lists should indicate we cannot scroll more.
* UI avatars should have visual transitions to appear and end.
    - More than this an animation is needed -> behaviors ?
* Be able to create interfaces using a dedicated JSON format or some other DSL.
    - Add a message to the RendererActor that handles such things, maybe from a separate JSON, but also using a DSL.
* Fix the layer bug with integer pixels for areas that are do not fit.
    - Identify the error ?? How ?
* All layers must be invalidated and rebuild if the surface is resized.

## Display lists

* When drawing, one can either draw the things if they changed, and doing this memorize the resulting drawing commands, or use this memory of previously issued commands to draw faster (avoiding to recompute transformation matrices, graphic code, etc.).
    - The basis for this could be a `DisplayList` trait (DL) that allows to firt compile an object, then redraw it faster.
    - Such objects are stored in the avatar renderers directly.
    - This is the renderer that decides when to compile/udapte the DL or to use it to redraw.


## Math

* A way to avoid a "Matrix4.toFloatArray" ???
    - Maybe with `@specialized`, we could have both matrices in Float or Doubles ?


## OpenGL

* Provide a new SGLAndroid for ES 3.0 and make new features testable (we woul need multi-sampling and glBlit for example to speed up text a log).
* Add buffers for some often used `glGet` inside the SGL implementations, mays pushs and pops ?.
* TextureFrameBuffer may allocate a depht buffer per frame buffer, however depth buffers can be shared.
    - The problem is the size of this depth buffer. 
    - Allocate only one such buffer large enough ?


## Text

* GLText draw/render and drawAt/renderAt distinction is not very good. They are completely distinct though. The various `render` and `draw` implemenations always draw in "pixel" space, whereas the `drawAt` and `renderAt` draw in any space.
* I realize that this is not the rendering of text that takes so much time, but *preparation* to render text, building of the vertex arrays of text, updating, changing space and computing MVP matrix. 
    - FBO and glBlitFrameBufer may be far faster when compositing text, however glBlitFrameBuffer is supported only in ES 3.0.
    - We could try an experimental way to do this as another interface to GLText when the ES 3.0 implementation of android SGL is ready ?


## CSS

* Add a CSS style sheet system on the avatar hierarchy.


## Android

* Modify the TextureLoader (or others ?) to handle both resources in assets but maybe also in res/raw.
    - This would allow fast load of textures from AssetFileDescriptor with NIO instead of using the InputStream byte read.s
    - See the TODO in AndroidImageTEXLoader.
