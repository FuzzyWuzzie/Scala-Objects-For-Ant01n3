#version 110
#include <es2/boneStruct.glsl>

attribute vec3 position;
attribute vec3 normal;
attribute vec4 boneIndex;
attribute vec4 boneWeight;

uniform mat4 MV;
uniform mat3 MV3x3;
uniform mat4 MVP;
uniform Bone bone[3];

varying vec3 N;	// Normal,
varying vec3 P;	// Position,
varying vec4 C;	// Color from the bones.

#include <es2/applyBones.glsl>

void main(void) {
	vec4 p;
	vec3 n;
	vec4 c;

	applyBonesWithColor(p, n, c);

	P = vec3(MV * p);
	N = normalize(MV3x3 * n); 
	C = vec4(c.rgb, c.a*0.9);

	gl_Position = MVP * p;
}
