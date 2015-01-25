#version 110

#include <es2/ColoredLightStruct.glsl>

varying vec3 P;
varying vec3 N;
varying vec4 C;

uniform ColoredLight L;

#include <es2/ColoredLight.glsl>

void main() {
	gl_FragColor = coloredLightPlastic(P, N, C, L);
	//gl_FragColor = vec4(L.Cd.rgb*L.Kd, 1);
}