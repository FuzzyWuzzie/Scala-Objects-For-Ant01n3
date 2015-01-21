#version 120

#include <es2/lightStruct.glsl>

attribute vec3 position;
attribute vec4 color;
attribute vec3 normal;

uniform mat4 MVP;
uniform mat4 MV;
uniform mat3 MV3x3;

uniform Light light;

varying vec3 vNormal;
varying vec3 vLightDir;
varying vec4 vColor;
varying float vLightDist;

void main(void) {
	vec4      p = MV * vec4(position, 1);
	vNormal     = normalize(MV3x3 * normal);
	vLightDir   = light.pos - vec3(p);
	vLightDist  = length(vLightDir);
	vLightDir   = normalize(vLightDir);
	vColor      = color;
	gl_Position = MVP * vec4(position, 1);
}
