#version 110

// Input
attribute vec3 position;
attribute vec4 color;
attribute vec3 normal;

#include <ColoredLightStruct.glsl>

// Global
uniform mat4 MV;
uniform mat3 MV3x3;
uniform mat4 MVP;
uniform ColoredLight L[4];

// Output
varying vec4 C;

// Lighting functions
#include <ColoredLight.glsl>


void main() {
	C = coloredLightPlastic4(
			vec3(MV * vec4(position, 1)),	// P
			normalize(MV3x3 * normal),		// N
			color, L);

	gl_Position = MVP * vec4(position, 1);
}