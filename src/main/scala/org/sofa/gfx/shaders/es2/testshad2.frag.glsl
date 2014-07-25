#version 110
#include <es2/ColoredLightStruct.glsl>

varying vec3 P;
varying vec3 N;
varying vec2 X;
varying vec4 C;
varying vec3 T;

uniform ColoredLight light;

#include <es2/ColoredLight.glsl>

void main() {
	gl_FragColor = singleColoredLightMatte(P, N, C);
}