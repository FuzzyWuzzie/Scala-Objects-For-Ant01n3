#version 110

attribute vec3 position;
attribute vec2 texCoords;

uniform mat4 MVP;

varying vec2 X;

void main(void) {
	gl_Position = MVP * vec4(position, 1);
	X = texCoords;
}