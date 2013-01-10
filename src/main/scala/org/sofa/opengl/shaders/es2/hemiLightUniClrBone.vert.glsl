#version 120
#include <es2/boneStruct.glsl>

attribute vec3 position;
attribute vec3 normal;
attribute vec4 boneIndex;
attribute vec4 boneWeight;

uniform mat4 MVP;
uniform mat4 MV;
uniform mat3 MV3x3;
uniform Bone bone[6];

varying vec3 N;
varying vec3 P;

#include <es2/applyBones.glsl>
	
void main(void) {
	vec4  p;
	vec3  n;

	applyBones(position, normal, p, n);

	P = vec3(MV * p);
	N = normalize(MV3x3 * n);

	gl_Position = MVP * p;
}