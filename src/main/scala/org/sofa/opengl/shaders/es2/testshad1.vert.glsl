#version 110

// Global
uniform mat4 MV;
uniform mat3 MV3x3;
uniform mat4 MVP;
uniform vec4 uniColor;

// Input
attribute vec3 position;
attribute vec3 normal;
attribute vec3 tangent;
attribute vec2 texCoord;

// Output
varying vec3 P;
varying vec4 C;
varying vec3 N;
varying vec3 T;
varying vec2 X;

void main() {
	P = vec3(MV * vec4(position, 1));
	N = normalize(MV3x3 * normal);
	T = normalize(MV3x3 * tangent);
	X = texCoord;
	C = uniColor;

	gl_Position = MVP * vec4(position, 1);
}