#version 110

// Input
attribute vec3 position;
attribute vec4 color;
attribute vec3 normal;
attribute vec3 offset;

// Global
uniform mat4 MV;
uniform mat3 MV3x3;
uniform mat4 MVP;

// Output
varying vec3 P;
varying vec4 C;
varying vec3 N;

void main() {
	vec4 p = vec4(position + offset, 1);

	P = vec3(MV * p);
	N = normalize(MV3x3 * normal);
	C = color;

	gl_Position = MVP * p;
}

