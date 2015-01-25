#version 120

attribute vec3 position;
attribute vec3 normal;
attribute vec4 color;

uniform mat4 MVP;
uniform mat4 MV;
uniform mat3 MV3x3;

varying vec3 N;
varying vec3 P;
varying vec4 C;

void main(void) {
	P = vec3(MV * vec4(position, 1));
	N = normalize(MV3x3 * normal);
	C = color;

	gl_Position = MVP * vec4(position, 1);
}