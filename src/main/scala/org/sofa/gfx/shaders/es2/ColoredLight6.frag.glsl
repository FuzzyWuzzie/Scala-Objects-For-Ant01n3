#version 110

#include <ColoredLightStruct.glsl>

varying vec3 P;
varying vec3 N;
varying vec4 C;

uniform ColoredLight L[6];

#include <ColoredLight.glsl>

void main() {
	gl_FragColor = coloredLightPlastic6(P, N, C, L);
}