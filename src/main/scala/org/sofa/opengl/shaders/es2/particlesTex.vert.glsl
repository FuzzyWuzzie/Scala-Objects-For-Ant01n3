#version 120

attribute vec3 position;
attribute vec2 texCoords;

uniform mat4 MVP;
uniform mat4 MV;
uniform mat3 MV3x3;

varying vec2 X;

void main(void) {
	gl_Position = MVP * vec4(position, 1);
	X = texCoords;
}
