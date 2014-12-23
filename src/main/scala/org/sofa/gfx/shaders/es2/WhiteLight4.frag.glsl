#version 110

#include <es2/whiteLightStruct.glsl>

varying vec3 V;
varying vec3 N;
varying vec4 C;

uniform WhiteLight L[4];

#include <es2/whiteLight.glsl>

void main() {
	gl_FragColor = whiteLightPhong4(V, N, C, L);
}