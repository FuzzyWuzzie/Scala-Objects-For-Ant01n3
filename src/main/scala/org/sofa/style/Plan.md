ZenGarden
=========

Goal: provide a way to attribute style to elements of something that is drawn, in very much the same way CSS is used to attribute style to a web page elements.

Goal: to be very general on what we apply style to.

    +----------------------+
    | A   +---------+ +---+| A: parent none Classes: View
    |     | B       | |D  || B: parent A    Classes: View, important
    |     |    +---+| +---+| C: parent B    Classes: Button, flashy
    |     |    |C  || +---+| D: parent A    Classes: Button
    |     |    +---+| |E  || E: parent A    Classes: Button
    |     +---------+ +---+|
    +----------------------+

    A
    |
    +----B
    |    |
    |    +----C
    |
    +----D
    |
    +----E

Vocabulary
----------

* __Stylable__: A part of the GUI that can be attributed a style. A stylable is defined by an unique identifier, the identifier of a unique parent and a set of classes.
* __Style property__: A element of the style (color, text, shape for example).
* __Style value__: The value for a property.
* __Inheritance__: A style that inherit all the properties of a parent style that it does not itself explicitely specify.
* __Agregation__: A style that is in fact a group of several styles, the properties when defined in more than one agregated styles are chosen according to a precedence rule.
* __Style class__: A set of style properties, indeed equivalent to a base style. Used as this is defined in the CSS vocabulary.
* __Base style__: a non agregated style, that contains only its style property values. Equivalent to a class.
* __Agregated style__: a set of base styles and a link to a parent agregated style.
* __Parent__: for stylables, the parent in the Yggdrasil tree, for (agregated) styles the style of the parent stylable.
* __Agregated property__: a style property defined in an agregated style, found using precedence rules.
* __Base property__: a style property only defined in a base style and not in its parents.

Fundamental difference with HTML CSS
------------------------------------

* __No positionning__. At the contrary of CSS, ZenGarden will never specify a placement for stylables. This is the role of another component of the Yggdrasil UI, named Layouts.
* __Only classes are considered__, selectors are indeed classes.

Cascading
---------

Styles will be cascaded according to several precedence rules. Elements will be attributed only one style, composed according to their position in the Yggrasil tree and their classes:

* __Inheritance__: Stylables can inherit one parent.
* __Agregation__: Stylables can pertain to several classes.

Stylables are attributed a style according to their classes and parent.

Then, style sheets can come from distinct sources, styles sheets are merged when added, and stylables can dynamically and easily change style, by simply being moved from one style to another. Styles can appear and disappear dynamically.

The stylesheet syntax is reminiscent of the CSS syntax, but with some differences. For example:

    root {
    }
    View {
        text-size: 8pt;
        fill-color: grey;
    }
    View.important {
        fill-color: red;
    }
    View#A {
        fill-color: lightgrey;
    }
    Button {
        fill-color: blue;
    }
    flashy {
        fill-color: yellow;
    }
    Button:click {
        fill-color: red;
    }
    Button {
        stroke-width: 1px;
    }

The selectors (`View`, `Button`, `flashy`) are in fact classes at the level of the style sheet. The `View.important` class is a class that can only by applied to views. The `flashy` class in contrast is a class that can be applyied to any stylable (as well as `Button`, and `View`). Internally, style classes will be represented by _base styles_.

The notation `View#A` creates a class that can only by applied to the stylable with identifier `A`. Indeed, the notation `.` and `#` merely allow to express constraints on which stylables a class can apply to. The notation `.flashy` could be used instead of `flashy` since selectors are also classes, they have the same meaning. Identically, the notation `#A` could be used for any element with identifier `A`.

The notation `Button:click` allows to define styles that are applyied only when a special condition in the interaction system occurs. Like `.` and `#` they only apply to the class they are preceded by, and the notation `:Button` for example is possible to avoid restricting to a given class.

Like other classes, these events will be formed by aggregated styles,
and switched on the Stylable they target when such an event occurs.

The `root` style is the base style any other style inherits (we cannot use `body`, we do not know how the stylables will be organized).

Each class has a precedence order. The "selector" classes (i.e. `View` above) all have the lower precedence. The "constrained" classes
(i.e. `View#A` above) have a higher precedence.

Note: Generally, but this is only a convention, the main class starts with a upper case letter (`View`, `Button`...) whereas other classes start with a lower case letter.

Node: You can repeat a style in the style sheet, and the style properties will be merged, like for `Button` above, for example.

How styles are created and stored
----------------------------------

The styling mechanism is cut in two main parts:
1. The __style sheet__, made of base styles only.
2. The __style map__, made of agregated styles assigned to one or more stylables.

A base style is merely a map of style property names and values.

An agregated style corresponds to all stylables that have the same set of classes and the same parent. It is made of all the base styles corresponding to the classes of the stylables, and of a link to one parent.

Bases styles are created at the moment the style sheet file is read. As styles can be repeated in the file, the base style objects may be updated twice or more. If there are some constraints or events, they will be stored in the style at this step.

The set of all these base styles make up the style sheet object. New files can be read and appended to this sheet object.

The style map references the style sheet, and is notified when it changes. The style map at start does not contain any agregated styles. As soon as stylables are added, or when a styleable is updated (its class changes, an event occurs), a corresponding agregated style is searched, and if it does not exist created. The styleable is then linked with this agregated style. At each link creation or deletion, a event is sent from the style map to the stylable.

All agregated styles are identified by a key that is created according to the stylables it links. The key is constructed this way:

    [name of parent class]|[name of classes sorted in precence order, then in alphabetical order, separated by comas]

For example in the style sheet above, the aggregated styles are:

    A:    root|A,View
    B:    A|important,View
    C:    B|flashy,Button
    D,E:  A|flashy,Button

Note: to avoid creating a lot of agregated styles, one could consider creating styles for classes only, then linking parents and agregated classes style.

When one or more events occurs, a new styling identifier is created for the styleable and the stylable is added to this agregated style which inherit the stylable own agregated style.

Style properties
----------------

As we do not know by advance what we will add style to, we also do not know the style properties we will define for these unknown stylable elements. The idea of zen-garden is to make this an open process. A style is very much like a hash map. The style mechanism will handle the precedence rules, the cascading, the link between stylables and their style etc. However the interpretation of style is let to the responsability of the rendering process that is completely decoupled of the style sheet. Indeed the zen-garden must be able to be used in a wide variety of environements: GUI, plotting, graphing, design, 2D, 3D, etc.

FAQ
---

* _Why using only classes and no selectors ?_: Because we do not know by advance to wich kinds of elements we will apply styles to.

Open questions:
---------------

* For some stylable, it may be interesting to ignore the style of the parent.
* In fact, do we really need the parenting ?
* The stylable will have to maintain a list of classes. Why not using this list of classes to define the precedence ?
* Do we introduce the notion of precedence for several style sheets ?
* How are "events" represented ?
