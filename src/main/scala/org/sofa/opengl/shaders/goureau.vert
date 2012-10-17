#version 330

layout(location=0) in vec3 position;				// Vertex position in model space.
layout(location=1) in vec4 color;					// Vertex color.
layout(location=2) in vec3 normal;					// Vertex normal.

uniform mat4 MVP;									// Perspective * View * Model.
uniform mat4 MV;									// View * Model.
uniform mat3 MV3x3;									// View * Model upper 3x3, without scaling and translation.

uniform vec3 lightPos;								// Light position, already in (its) View * Model space.
uniform float lightIntensity;						// Intensity of the light.
uniform float ambientIntensity;						// Intensity of the ambient light.

smooth out vec4 ex_Color;							// Output toward fragments.

void main(void) {
	vec4  p = MV * vec4(position, 1);				// We work in ModelView space for illumination.
	vec3  n = normalize(MV3x3 * normal);			// Normal, without the scaling and translation.
	vec3  l = lightPos - vec3(p);					// Light direction.
	float d = length(l);							// Distance from the vertex to the light.
	l = normalize(l);								// Light direction vector.
	float a = clamp(dot(n, l), 0, 1);				// Angle of incidence.

	gl_Position = MVP * vec4(position, 1);
	ex_Color    = ((color * a * lightIntensity) + (color * ambientIntensity)) / (d*d);
}
