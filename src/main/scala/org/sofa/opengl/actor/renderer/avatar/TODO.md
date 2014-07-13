# TODO

## Priority list and roadmap:

1. Displacement on the map
    - On the model side.
    - On the avatar side, we provides simple ways to displace avatars.
2. Add ordered rendering in AvatarContainer.
3. Add selective rendering
4. Picking
5. Test on Android

## Avatars

* Handle *ordered rendering* in AvatarContainer.
* Handle *selective rendering*, based on a visibility function (use a SpaceHash ?)
* Handle *picking -> allow avatars to register in a spaceHash ? 
    - a general one ? one per sub-avatar ? one in a specific avatar ?
    - When several avatars are picked, how to sort them ?

## Mesh

* Update mesh and meshes to be generic on attributes -> example in TrianglesMesh.
    - update the updateVertexArray() methods to take VertexAttributes or Strings as parameters for attribute names.

## Behaviors/Armature

* Handle animation of particles in behaviors ?
    - Allow a part of an armature to be instanced multiple times for particles ?
* Add "Composition" behavior to activate several behaviors at will.
 Add "InParallelLoop" that runs several behaviors in parallel repeating them as fast as possible.
    The current way is to put a "InParallel" in a "Loop", but this forces to wait for the end
    of the longest behavior in the "InParallel" behavior.
* Add "FromVariable" behavior that scale a value (scale, translation, angle) to an external variable. Question : how to specify how to reach/read the variable. (via an interface on objects that can be named in the library ? --> AnimatedValues ?)
* Change the loadresources of Libraries, the XML lib of Scala is very slow. A dedicated format ? JSON ??

## Library/Resources

* Allow the Library to be actor based, loading elements in the background ? Return a Future instead of an item ? How to handle the fact OpenGL is not thread safe ?
* Allow to forget texture resources after they are uploaded to OpenGL to save memory.

## Game

* Handle paths inside each cell of the cell grids, allowing Entities to follow these paths. Use a graph ? Use only "sub-cells" ?
* A *level file* with the right reader to load levels. An ASCII map ?

## CSS

* Add a CSS style sheet system on the avatar hierarchy.
