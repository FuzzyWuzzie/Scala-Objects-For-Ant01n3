#version 110

attribute vec3 position;
attribute vec2 texCoords;
attribute vec3 moving;

uniform mat4 MVP;
uniform vec2 texDisplacement;

varying vec2 vTexCoords;

void main(void) {
	if(moving.x != 0)
	     vTexCoords = texCoords - texDisplacement;
	else vTexCoords = texCoords;

	gl_Position = MVP * vec4(position, 1);
}
