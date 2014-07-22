#version 110

attribute vec3 position;
attribute vec2 texCoords;
attribute vec3 user;

uniform mat4 MVP;
uniform vec3 texOffset;

varying vec2 vTexCoords;

void main(void) {
	if(user.x != 0.0)
	     vTexCoords = texCoords - texOffset.xy;
	else vTexCoords = texCoords;

	gl_Position = MVP * vec4(position, 1);
}
