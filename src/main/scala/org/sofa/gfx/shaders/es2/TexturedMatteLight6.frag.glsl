#version 110

#include <ColoredLightStruct.glsl>

varying vec3 P;
varying vec3 N;
varying vec2 T;

uniform ColoredLight L[6];
uniform sampler2D color;

#include <ColoredLight.glsl>

void main() {
	gl_FragColor = coloredLightMatte6(P, N, texture2D(color, T.st), L);
}