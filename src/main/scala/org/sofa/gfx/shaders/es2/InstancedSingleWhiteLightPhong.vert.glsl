#version 110

// Input
attribute vec3 vertex;
attribute vec4 color;
attribute vec3 normal;
attribute vec3 position;

// Global
uniform mat4 MV;
uniform mat3 MV3x3;
uniform mat4 MVP;

// Output
varying vec3 V;
varying vec4 C;
varying vec3 N;

void main() {
	vec3 v = vec3(vertex.x+position.x, vertex.y+position.y, vertex.z+position.z);

	V = vec3(MV * vec4(v, 1));
	N = normalize(MV3x3 * normal);
	C = color;

	gl_Position = MVP * vec4(v, 1);
}

