#version 110

#include <es2/whiteLightStruct.glsl>

varying vec3 P;
varying vec3 N;
varying vec4 C;

uniform WhiteLight whitelight;

#include <es2/whiteLight.glsl>

void main() {
	gl_FragColor = singleWhiteLightPhong(P, N, C);
}