TODO
====

Avatars
-------

* Handle ordered rendering in AvatarContainer.
* Handle selective rendering, based on a visibility function (use a SpaceHash ?)
* Add "Composition" behavior to activate several behaviors at will.
* Add "FromVariable" behavior that scale a value (scale, translation, angle) to an external variable. Question : how to specify how to reach/read the variable. (via an interface on objects that can be named in the library ? --> AnimatedValues ?)

Game
----

* Handle paths inside each cell of the cell grids, allowing Entities to follow these paths (use a graph ?).