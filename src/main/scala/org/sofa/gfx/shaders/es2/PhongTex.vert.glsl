#version 110

// Input
attribute vec3 position;
attribute vec3 normal;
attribute vec2 texCoords;

// Global
uniform mat4 MV;
uniform mat3 MV3x3;
uniform mat4 MVP;

// Output
varying vec3 P;
varying vec3 N;
varying vec2 T;

void main() {
	P = vec3(MV * vec4(position, 1));
	N = normalize(MV3x3 * normal);
	T = texCoords;

	gl_Position = MVP * vec4(position, 1);
}

