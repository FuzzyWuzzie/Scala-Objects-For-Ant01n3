# Philosophy #

Here are some ideas used in the conception of this package.

The idea is to provide a framework to create simulations, mostly intended toward games but also any kind of thing that need a graphical representation (GUI, games, simulations).

The driving idea is to separate the simulation logic from the rendering. Old idea : Model view controller. However, here using actors.

Therefore, the simulation logic is implemented in terms of actors. The rendering logic is a single actor that drives a renderer system with its dedicated thread. In the middle a model actor (or several) represents the world where entities of the simulation are run :

               Client Part               |          UI Part
                                         |     
    +------------+                       |        +----------+
    | Controller |  <--------------------|------> | Renderer |
    +------------+                       |        +----------+
         ^                               |             ^
         |               +-------+       |             |
         ±-------------->| Model | <-----|-------------+
                         +-------+       |
                                         |
    A pool of threads                    |   One unique identified thread

## Model

The model represents the state of things of the simulation. There will exist several such models, for each elements of the simulation. These models will be tied to one or more avatars (or avatar hierarchies) and will allow to drive one or more of their features.

## Controller

The controller is a set of actors that implement the logic of the simulation. They are completely free and unspecified. They use models to know the state of the simulation and change it and receive messages from the model.

## Renderer

The renderer is a system mostly composed of :

- Screens
- Avatars
- Events

                                                +--- Avatars
                                               /
                                +-- Avatars --+----- Avatars
    +----------+               /               \
    | Renderer | ---> screen -+                 +--- Avatars
    +----------+  

The screens are the surface on which avatars are rendered. There is only one screen at a time.

Inside screens, avatars are things that have a graphical representation. They are arranged as a hierarchy. Usually they also describe a "space". That is an avatar decides how its sub avatars are placed and moved (for example, some avatars may be seen as 2D grids where each position is only a cell, other may be a 3D view with a perspective others are graphs, etc.). Such avatars should have a pendant as a "Model" agent used in the client part and allowing to use the space of the avatar.

Events are things that occurs either on the screen or on avatars and that are passed to "acquaintances". Acquaintances are most of the time the actor (controller) associated with the avatar.