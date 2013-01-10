#version 120
#include <es2/boneStruct.glsl>

attribute vec3 position;

uniform mat4 MVP;

void main(void) {
	gl_Position = MVP * vec4(position, 1);
}
