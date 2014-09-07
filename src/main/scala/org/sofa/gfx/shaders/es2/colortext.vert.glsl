#version 110

attribute vec3 position;
attribute vec2 texCoords;
attribute vec4 color;

uniform mat4 MVP;

varying vec2 vTexCoords;
varying vec4 vColor;


void main(void) {
	vTexCoords  = texCoords;
	vColor      = color;
	gl_Position = MVP * vec4(position, 1);
}
