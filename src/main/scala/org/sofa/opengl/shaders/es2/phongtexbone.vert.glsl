#version 120
#include <es2/boneStruct.glsl>

// Input:
attribute vec3 position;
attribute vec3 normal;
attribute vec4 tangent;
attribute vec2 texCoords;
attribute vec4 boneIndex;
attribute vec4 boneWeight;

// Global:
uniform mat4 MVP;		// Perspective * View * Model,
uniform mat4 MV;		// View * Model,
uniform mat3 MV3x3;		// Upper 3x3 matrix of MV, without scaling or translation,
uniform Bone bone[6];	// Set of bone matrices.

// Output:
varying vec3 N;		// Normal,
varying vec3 T;		// Tangent,
varying vec3 B;		// Bi-Tangent,
varying vec2 X;		// Texture Coordinates,
varying vec3 P;		// Position

#include <es2/applyBones.glsl>

void main(void) {
	vec4  p;
	vec3  n;
	vec3  t;

	applyBones(position, normal, vec3(tangent.xyz), p, n, t);

	P = vec3(MV * p);
	N = normalize(MV3x3 * n); 
	T = normalize(MV3x3 * t);
	B = normalize(cross(T, N) * tangent.w);	// w = tangent handedness
	X = texCoords;
	
	gl_Position = MVP * p;
}
