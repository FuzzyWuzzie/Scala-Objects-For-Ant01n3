#version 110

#include <ColoredLightStruct.glsl>

varying vec3 P;
varying vec3 N;
varying vec4 C;

uniform ColoredLight L[2];

#include <ColoredLight.glsl>

void main() {
	gl_FragColor = coloredLightPlastic2(P, N, C, L);
}