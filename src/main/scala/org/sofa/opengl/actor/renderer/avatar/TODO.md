TODO
====

Avatars
-------

* Handle ordered rendering in AvatarContainer.
* Handle selective rendering, based on a visibility function (use a SpaceHash ?)
* Add "Composition" behavior to activate several behaviors at will.
* Add "InParallelLoop" that runs several behaviors in parallel repeating them as fast as possible.
    The current way is to put a "InParallel" in a "Loop", but this forces to wait for the end
    of the longest behavior in the "InParallel" behavior.
* Add "FromVariable" behavior that scale a value (scale, translation, angle) to an external variable. Question : how to specify how to reach/read the variable. (via an interface on objects that can be named in the library ? --> AnimatedValues ?)
* Allow the Library to be actor based, loading elements in the background ? Return a Future instead of an item ? How to handle the fact OpenGL is not thread safe ?
* Add a better Armature loader from a specific format. The format could be created from the old SVG format into a more compact and much faster simple text format. A possible format for a joint:
    // id   z   area           pivot  anchor  visible sub-joints
    ["name", 0, (x1,y1,x2,y2), (x,y), (x,y),  true,   {[..], [..], ..}]

Game
----

* Handle paths inside each cell of the cell grids, allowing Entities to follow these paths. Use a graph ? Use only "sub-cells" ?