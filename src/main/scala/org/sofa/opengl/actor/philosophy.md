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

The model is in fact a world actor. There is no central repository of the what we could qualify of "state of the simulation" since each actor also stores its state and in some way a part of the model. It is largely unspecified, some tools are provided to help use it and make the bridge with the screens used as visual models and spaces.

## Controller

The controller is a set of actors that implement the logic of the simulation. They are completely free and unspecified.

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