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
uniform float specularPow;

smooth out vec4 ex_Color;							// Output toward fragments.

void main(void) {
	vec4  p = MV * vec4(position, 1);								// We work in ModelView space for illumination.
	vec3  e = normalize(-p.xyz);									// Vector toward the camera (to compute specular area, in MV space, the camera is always at (0,0,0)).
	vec3  n = normalize(MV3x3 * normal);							// Normal, without the scaling and translation.
	vec3  l = lightPos - vec3(p);									// Light direction.
	float d = length(l); d = d*d;									// Distance from the vertex to the light.
	l = normalize(l);												// Light direction vector.
	float a = max(dot(n, l), 0);									// Angle of light incidence compared to normal.
	float s = pow(max(dot(e, reflect(-l, n)), 0), specularPow);		// Angle of light indicence compared to eye.

	gl_Position = MVP * vec4(position, 1);
	ex_Color    =   ((color * a * lightIntensity) / d) 				// The diffuse part.
				  + ((vec4(1,1,1,1) * lightIntensity * s ) / d)		// The specular part.
	              + ((color * ambientIntensity));					// The ambient part.
}
