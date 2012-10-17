#version 330
#include <lightStruct.glsl>
#include <boneStruct.glsl>

layout(location=0) in vec3 position;
layout(location=1) in vec3 normal;
layout(location=2) in float boneIndex;

uniform mat4 MV;
uniform mat3 MV3x3;
uniform mat4 MVP;
uniform Bone bone[3];
uniform Light lights[1];

smooth out vec3 N;
smooth out vec3 L;
smooth out vec4 C;
smooth out float LL;

void main(void) {
	uint b;
	vec3 n;
	vec4 p;
	vec4 P;
	
	b  = uint(boneIndex);
	p  = bone[b].MV * vec4(position, 1);
	n  = bone[b].MV3x3 * normal;

	P  = MV * p;
	N  = normalize(MV3x3 * n);
	L  = lights[0].pos - vec3(P);
	LL = length(L);
	L  = normalize(L);
    C  = bone[b].color;

	gl_Position = MVP * p;
}
