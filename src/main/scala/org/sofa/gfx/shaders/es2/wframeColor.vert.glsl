#version 110

attribute vec3 position;
attribute vec4 color;
attribute vec3 normal;
attribute vec3 barycoord;

uniform mat4 MVP;
uniform mat4 MV;
uniform mat3 MV3x3;

#include <es2/lightStruct.glsl>
uniform Light light;

varying vec3 vNormal;
varying vec3 vPosition;
varying vec4 vColor;
varying vec3 vBary;

void main(void) {
	vPosition   = vec3(MV * vec4(position, 1));
	vNormal     = normalize(MV3x3 * normal);
	vColor      = color;
	vBary       = barycoord;
	gl_Position = MVP * vec4(position, 1);
}
