#version 120

// Input:
attribute vec3 position;
attribute vec3 normal;
attribute vec3 tangent;
attribute vec3 bitangent;
attribute vec2 texCoords;

// Global:
uniform mat4 MVP;		// Perspective * View * Model,
uniform mat4 MV;		// View * Model,
uniform mat3 MV3x3;		// Upper 3x3 matrix of MV, without scaling or translation,

// Output:
varying vec3 N;		// Normal,
varying vec3 T;		// Tangent,
varying vec3 B;		// Bi-Tangent,
varying vec2 X;		// Texture Coordinates,
varying vec3 P;		// Position

void main(void) {
//	vec4 P; // Vertex position in MVP

	N  = normalize(MV3x3 * normal);
	T  = normalize(MV3x3 * tangent);
	B  = normalize(MV3x3 * bitangent);
	P  = vec3(MV * vec4(position, 1));
	X  = texCoords;
	
	gl_Position = MVP * vec4(position, 1);
}
