#version 110
#include <es2/ColoredLightStruct.glsl>

varying vec3 V;
varying vec3 N;
varying vec4 C;

uniform ColoredLight L;

#include <es2/ColoredLight.glsl>

void main() {
	gl_FragColor = singleColoredLightPlastic(V, N, C, L);
}