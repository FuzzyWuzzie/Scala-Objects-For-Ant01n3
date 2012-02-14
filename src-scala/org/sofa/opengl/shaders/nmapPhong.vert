#version 330

// Input:
layout(location=0) in vec3 position;
layout(location=2) in vec3 normal;
layout(location=3) in vec3 tangent;
layout(location=4) in vec2 texCoords;

// Global:
uniform mat4 MVP;		// Perspective * View * Model,
uniform mat4 MV;		// View * Model,
uniform mat3 MV3x3;		// Upper 3x3 matrix of MV, without scaling or translation,
uniform vec3 lightPos;	// Position of a light already in MV space.

// Output:
smooth out vec3 N;		// Normal,
smooth out vec3 T;		// Tangent,
smooth out vec3 B;		// Bi-Tangent,
smooth out vec3 L;		// Light Direction,
smooth out vec2 X;		// Texture Coordinates,
smooth out float LL;	// Light distance.

void main(void) {
	vec4 P; // Vertex position in MVP

	P  = MV * vec4(position, 1);
	N  = normalize(MV3x3 * normal);
	T  = normalize(MV3x3 * tangent);
	B  = normalize(cross(T, N));
	L  = lightPos - vec3(P);
	LL = length(L);
	L  = normalize(L);
	X  = texCoords;
	
	gl_Position = MVP * vec4(position, 1);
}
