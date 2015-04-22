#version 110

precision highp float;

attribute vec3 position;

uniform mat4 MVP;

varying vec2 P;

void main(void) {
	P = vec2(position.x, position.y);
	gl_Position = MVP * vec4(position, 1);
}