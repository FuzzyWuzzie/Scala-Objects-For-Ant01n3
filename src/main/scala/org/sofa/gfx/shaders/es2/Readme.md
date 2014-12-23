# Shaders


## « Good practices »

The names of the attributes as seen from the program that use the shader:

- vertex    Position of the vertex (3)
- color     Color of the vertex (4)
- normal    Normal to the surface at the vertex (3)
- tangent   Tangent to the surface at the vertex (3)
- bitangent Bi-tangent, forms a base with normal and tangent (3)
- texture   Texture UV at the vertex (2)
- bone      Bone index (1 or more)
- weight    Bone weight (several)
- position  Position of instance (3, used in instanced rendering)

The names of the sames attributes used as varying between shader stages:

- V   vertex     
- C   color      
- N   normal     
- X   texture    
- T   tangent    
- B   bi-tangent 
- O   bone       
- W   weight     
- P   position   

Names of the uniforms often used:

- MV    modelview matrix (4 x 4)
- MVP   modelview x perspective matrix (4 x 4)
- MV3x3 modelview matrix (3 x 3)
- L     Light (as a name or array)

Lights:

- P           The light position
- C           The light single color (replace diffuse, specular and ambient colors) 
- Cd          The light diffuse color
- Cs          The specular color
- Ca          The ambient color
- Kd          Diffuse coefficient
- Ks          Specular coefficient
- Ka          Ambient coefficient
- R           The specular roughness
- Ac          Constant attenuation
- Al          Linear attenuation
- Aq          Quadratic attenuation

## Up to date

Here is a list of up-to-date shaders.

### Phong (vertex)

Imports: vertex, color, normal.

Globals: MV, MVP, MV3x3.

Exports: V, N and C.

### InstancedPhong (vertex)

Imports: vertex, color, normal, position.

Globals: MV, MVP, MV3x3.

Exports V, N and C.

### WhileLight (frag, 1, 2, 4)

Imports: V, N and C.

Globals: L (of type WhiteLight).

Includes: whiteLightStruct.glsl, whitelight.glsl

### ColoredLight (frag, 1, 2, 4)

Imports: V, N, C.

Globals: L (of type ColoredLight)

Includes: coloredLightStruct.glsl, coloredLight.glsl