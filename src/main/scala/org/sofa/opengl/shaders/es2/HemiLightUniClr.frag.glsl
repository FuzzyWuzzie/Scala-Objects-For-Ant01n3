#version 120
#include <es2/hemisphereLightStruct.glsl>

varying vec3 N;
varying vec3 P;

uniform HemisphereLight hemilight;
uniform vec4 color;

#include <es2/hemiLight.glsl>

void main(void) {
	gl_FragColor = hemiLight(hemilight, P, N, color, vec4(0,0,0,1));
}
