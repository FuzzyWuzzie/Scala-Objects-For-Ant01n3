#version 120

attribute vec3 position;
attribute vec4 color;

uniform mat4 MVP;

varying vec4 ex_Color;

void main(void) {
	gl_Position = MVP * vec4(position, 1);
	ex_Color = color;
}
