#version 110

attribute vec3 position;

uniform mat4 MV;
uniform mat3 MV3x3;
uniform mat4 MVP;

void main(void) {
	gl_Position = MVP * vec4(position, 1);
}
