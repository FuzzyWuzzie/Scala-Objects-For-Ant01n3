#version 120
#include <es2/hemisphereLightStruct.glsl>

varying vec3 N;
varying vec3 P;
varying vec4 C;

uniform HemisphereLight L;

#include <es2/hemiLight.glsl>

void main(void) {
	gl_FragColor = hemiLight(L, P, N, C, vec4(0,0,0,1));
}
