#version 120

attribute vec3 position;
attribute vec3 normal;

uniform mat4 MVP;
uniform mat4 MV;
uniform mat3 MV3x3;

varying vec3 N;
varying vec3 P;

void main(void) {
	P = vec3(MV * vec4(position, 1));
	N = normalize(MV3x3 * normal);

	gl_Position = MVP * vec4(position, 1);
}