# TODO

## Priority list and roadmap:

1. Creation and representation of the map in the model.
2. Displacement on the map
    - On the model side.
    - On the avatar side, we provides simple ways to displace avatars.
3. Add a more efficient ordered rendering in AvatarContainer.
4. Add selective rendering.
5. Picking.

## General

* Rename SOFA, but to what ?
    - Alpha α ?
    - Gamma Ɣ ?
    - Phi φ ?
    - SOFA Σοφα ?

## Avatars

* Handle *ordered rendering* in AvatarContainer.
    - It is handled, but probably not efficient : a Z-sort (indeed Y here) before each frame.
    - -> This is specific to games.
* Handle *selective rendering*, based on a visibility function (use a SpaceHash ?)
    - -> this is specific to each avatar sub-library (UI, Game, ect.) Partly done in UI.
* Handle *picking -> allow avatars to register in a spaceHash ? 
    - a general one ? one per sub-avatar ? one in a specific avatar ?
    - When several avatars are picked, how to sort them ?
    - -> this is specific to each avatar sub-library (UI, Game, etc.) Partly done in UI.

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

## UI

* Actually the DPC is used to scale the UI, elements keep their physical size in the metric system.
    - This means that chaning screen will change the size in pixels of elements, but conserve the metric size.
    - With JOGL this measure can change if the application changes monitor (multi-monitor setting), and the Root element will scale accordingly and all its descendants with it.
    - It remains a problem with fonts. Although fonts are expressed in pica points that have a correspondance in the real world, most OSes do not consider points as such. Therefore a text at 12pts will not have the same physical size on two screens with the same resolution but not the same physical size (say a 24inch monitor and a 17inch monitor). That is a shame, but how to circumvent it ?
* An "action bar" à la Android ?
* Lists must show a scroll indicator when more elements than visible.
* Lists should indicate we cannot scroll more.
* UI avatars should have visual transitions to appear and end.
    - More than this an animation is needed -> behaviors ?
* An animateSpace that avoids to redo positionning at each frame, see under.
* A layout system that avoid computing the layouts at each frame.
    - A way to do this is to use dirty bits to indicate an avatar needs to relayout its sub-avatars. It can work in the reverse : a sub can tell its parent it changed, and therefore the parent needs to relayout.
    - How to do this properly ?
    - Example : the list needs to layout its sub-items only when one is added or removed.
* Be able to create interfaces using a dedicated JSON format or some other DSL.
    - Add a message to the RendererActor that handles such things, maybe from a separate JSON, but also using a DSL.

## OpenGL

* Would a shader "current shader" or "current texture" avoid lots of bindings and provide performance gains ?

## Text

* Improve text efficiency, profiling shows we spend a lot of time drawing and remaking strings.
* Do some tests and benchmarks.

## CSS

* Add a CSS style sheet system on the avatar hierarchy.

## Android

* Modify the TextureLoader (or others ?) to handle both resources in assets but maybe also in res/raw.
    - This would allow fast load of textures from AssetFileDescriptor with NIO instead of using the InputStream byte read.s
    - See the TODO in AndroidImageTEXLoader.
