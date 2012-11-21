#version 120

attribute vec3 position;
attribute vec2 texCoords;

uniform mat4 MVP;

varying vec2 vTexCoords;

void main(void) {
	vTexCoords  = texCoords;
	gl_Position = MVP * vec4(position, 1);
}
