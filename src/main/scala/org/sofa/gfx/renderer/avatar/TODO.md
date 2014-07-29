# TODO

## Priority list and roadmap:

1. Creation and representation of the map in the model.
2. Displacement on the map
    - On the model side.
    - On the avatar side, we provides simple ways to displace avatars.
3. Add a more efficient ordered rendering in AvatarContainer.
4. Add selective rendering.
5. Picking.

## Avatars

* Handle *ordered rendering* in AvatarContainer.
    - It is handled, but probably not efficient : a Z-sort (indeed Y here) before each frame.
* Handle *selective rendering*, based on a visibility function (use a SpaceHash ?)
* Handle *picking -> allow avatars to register in a spaceHash ? 
    - a general one ? one per sub-avatar ? one in a specific avatar ?
    - When several avatars are picked, how to sort them ?

## Mesh

* Update mesh and meshes to be generic on attributes -> example in TrianglesMesh.
    - update the updateVertexArray() methods to take VertexAttributes or Strings as parameters for attribute names.

## Behaviors/Armature

* Extend behavior to Avatars in order to move/scale/rotate them ?
    - Started : the generic behaviors have been put in a dedicated library.
* Handle animation of particles in behaviors ?
    - Allow a part of an armature to be instanced multiple times for particles ?
    - Already possible with almost no change in the Armature.
* Add "Composition" behavior to activate several behaviors at will.
* Add "InParallelLoop" that runs several behaviors in parallel repeating them as fast as possible.
    The current way is to put a "InParallel" in a "Loop", but this forces to wait for the end
    of the longest behavior in the "InParallel" behavior.
* Add "FromVariable" behavior that scale a value (scale, translation, angle) to an external variable. Question : how to specify how to reach/read the variable. (via an interface on objects that can be named in the library ? --> AnimatedValues ?)

## Library/Resources

* Allow the Library to be actor based, loading elements in the background ? Return a Future instead of an item ? How to handle the fact OpenGL is not thread safe ?
* Change the loadresources of Libraries, the XML lib of Scala is very slow. A dedicated format ? JSON ??
    - JSON done but loading is still very slow -> use a dedicated parser done with Jackson.

## Surface/Renderer

* Allow the renderer to choose if base/high-level events are sent, as in surfaces.

## Events

* Shortcuts
* Define units in Scale and Scroll to better interpret moves
    - Use percents of the screen ?
    - Use pixels (depend on resolution) ?
    - Other ?

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

## CSS

* Add a CSS style sheet system on the avatar hierarchy.

## Android

* Modify the TextureLoader (or others ?) to handle both resources in assets but maybe also in res/raw.
    - This would allow fast load of textures from AssetFileDescriptor with NIO instead of using the InputStream byte read.s
    - See the TODO in AndroidImageTEXLoader.
