#version 110
#include <es2/whiteLightStruct.glsl>

varying vec3 P;
varying vec3 N;
varying vec2 X;
varying vec4 C;
varying vec3 T;

uniform WhiteLight whitelight;

#include <es2/whiteLight.glsl>

// Let's test a simple phong.
void main() {
	gl_FragColor = singleWhiteLightPhong(P, N, C);
}