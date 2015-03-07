#version 110

#include <ColoredLightStruct.glsl>

varying vec3 P;
varying vec3 N;
varying vec4 C;

uniform ColoredLight L[8];

#include <ColoredLight.glsl>

void main() {
	gl_FragColor = coloredLightMatte8(P, N, C, L);
}