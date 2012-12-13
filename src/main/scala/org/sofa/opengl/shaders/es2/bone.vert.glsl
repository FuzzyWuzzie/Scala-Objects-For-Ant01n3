#version 110
#include <boneStruct.glsl>

attribute vec3 position;
attribute vec4 boneIndex;
attribute vec4 boneWeight;

uniform mat4 MV;
uniform mat3 MV3x3;
uniform mat4 MVP;
uniform Bone bone[3];

varying vec4 ex_Color;

// This shader is a very simple one allowing to experiement on bones.
// It will deform a mesh with bones and weights, and color each vertex
// according to a mix of the colors of each bone influencing the vertex,
// proportionnally to the bone weights.

void main(void) {
	int   b0 = int(boneIndex.x);
	int   b1 = int(boneIndex.y);
	int   b2 = int(boneIndex.z);
	int   b3 = int(boneIndex.w);

	vec4  P = vec4(0,0,0,0);
	vec4  C = vec4(0,0,0,0);

	// By construction we impose to have at max. 4 bones
	// that influence a vertex. Anyway or system will ensure
	// this and distribute the weights on the four most
	// influent bones.

	if(b0 >= 0) {
		P += bone[b0].MV * vec4(position, 1) * boneWeight.x;
		C += bone[b0].color * boneWeight.x;
	}
	if(b1 >= 0) {
		P += bone[b1].MV * vec4(position, 1) * boneWeight.y;
		C += bone[b1].color * boneWeight.y;
	} 
	if(b2 >= 0) {
		P += bone[b2].MV * vec4(position, 1) * boneWeight.z;
		C += bone[b2].color * boneWeight.z;
	} 
	if(b3 >= 0) {
		P += bone[b3].MV * vec4(position, 1) * boneWeight.w;
		C += bone[b3].color * boneWeight.w;
	} 

	ex_Color    = vec4(C.rgb, C.a*0.5);
	gl_Position = MVP * P;
}
