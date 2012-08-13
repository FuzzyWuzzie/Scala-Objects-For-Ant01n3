#version 120
#include <es2/lightStruct.glsl>

attribute vec3 position;
attribute vec3 normal;

uniform mat4 MVP;
uniform mat4 MV;
uniform mat3 MV3x3;

uniform Light light;

varying vec3 vNormal;
varying vec3 vPosition;

void main(void) {
	vPosition   = vec3(MV * vec4(position, 1));
	vNormal     = normalize(MV3x3 * normal);
	gl_Position = MVP * vec4(position, 1);
}
