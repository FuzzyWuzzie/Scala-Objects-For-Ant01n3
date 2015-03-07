#version 110

#include <ColoredLightStruct.glsl>

varying vec3 P;
varying vec3 N;
varying vec4 C;

uniform ColoredLight L[4];

#include <ColoredLight.glsl>

void main() {
	gl_FragColor = coloredLightMatte4(P, N, C, L);
}