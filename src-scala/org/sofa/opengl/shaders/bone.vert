#version 330

struct Bone {
	mat4 MV;
	mat3 MV3x3;
	vec4 color;
};

layout(location=0) in vec3 position;
layout(location=1) in float boneIndex;

uniform mat4 MV;
uniform mat3 MV3x3;
uniform mat4 MVP;
uniform Bone bone[3];

smooth out vec4 ex_Color;

void main(void) {
	uint b = uint(boneIndex);
	vec4 P = bone[b].MV * vec4(position, 1);
	
	ex_Color    = bone[b].color;
	gl_Position = MVP * P;
}
