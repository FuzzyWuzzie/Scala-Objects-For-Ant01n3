# TODO

## Priority list and roadmap:

UI

0. dirtyRender & dirtyPosition
    - Make this more general in Avatars ?
1. Display Lists

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

* Update mesh and meshes to be generic on attributes -> example in TrianglesMesh.
    - update the updateVertexArray() methods to take VertexAttributes or Strings as parameters for attribute names.

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

## Library/Resources

* Allow the `Library` to be actor based, loading elements in the background ? Return a Future instead of an item ? How to handle the fact OpenGL is not thread safe ?
* Change the loadresources of Libraries, the XML lib of Scala is very slow. A dedicated format ? JSON ??
    - JSON done but loading is still very slow -> use a dedicated parser done with Jackson.

## Surface/Renderer

* Allow the renderer to choose if base/high-level events are sent, as in surfaces.

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

* An "action bar" à la Android ?
* Lists should indicate we cannot scroll more.
* UI avatars should have visual transitions to appear and end.
    - More than this an animation is needed -> behaviors ?
* Be able to create interfaces using a dedicated JSON format or some other DSL.
    - Add a message to the RendererActor that handles such things, maybe from a separate JSON, but also using a DSL.
* Use the idea of "Display Lists" (Android, OpenGL).
    - For text each UI elements builds a display list of text (backed by a GLText ?). If the elment changes, the display list is rebuilt, else the display list is kept. The display list contains ALL the text the element needs (or the element builds several display lists if one changes often and others not).
    - For graphics its the same, build a display list of graphical elements needed by the element. If the element does not changes, the list is reused, else it is rebuilt. 
    - Display list items can contain the actual MVP at time or construction and a set of basic commands (shader, mesh)...
    - One can consider two changes in display list items : only the MVP changed but not the contents, or both.
        + add dirtyMVP and dirtyRender flags ?
* Use the idea of "invalidation" (Android),
    - In addition to the needRender flag (general), and the layoutRequest flag (UI), we can use an invalidate flag that tells that the display list of the element needs to be rebuilt.

## OpenGL

* Would a shader "current shader" or "current texture" avoid lots of bindings and provide performance gains ?
    - Done.
* Add buffers for some often used glGet.
* We could improve Space when there is no projection, can avoid a matrix mult at each mvp recompute.


## Text

* We probably can improve a lot text layer by avoiding to redo the same things (mvp, color, etc) at each string.
* A way to do this, maybe, is to create a giant string for each font, since we know we will have to draw the text in one pass at the end.
    - The giant string would position directly texts at their position on a pixel surface,
    - Each point would have a color associated, allowing to create text effect,
    - The counterpart would be that we cannot reuse strings, we would have to recreate it, each time.

## CSS

* Add a CSS style sheet system on the avatar hierarchy.

## Android

* Modify the TextureLoader (or others ?) to handle both resources in assets but maybe also in res/raw.
    - This would allow fast load of textures from AssetFileDescriptor with NIO instead of using the InputStream byte read.s
    - See the TODO in AndroidImageTEXLoader.
