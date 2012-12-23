#version 110
#include <es2/lightStruct.glsl>

attribute vec3 position;
attribute vec3 normal;
attribute vec2 texCoords;

uniform mat4 MVP;
uniform mat4 MV;
uniform mat3 MV3x3;

uniform Light light;

varying vec3 vNormal;
varying vec3 vPosition;
varying vec2 vTexCoords;

void main(void) {
	vPosition   = vec3(MV * vec4(position, 1));
	vNormal     = normalize(MV3x3 * normal);
	vTexCoords  = texCoords;
	gl_Position = MVP * vec4(position, 1);
}
