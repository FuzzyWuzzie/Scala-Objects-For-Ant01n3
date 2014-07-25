#version 110

attribute vec3 position;
attribute vec4 color;

uniform mat4 MVP;

varying vec4 vColor;

void main(void) {
	vColor = color;
	gl_Position = MVP * vec4(position, 1);
}