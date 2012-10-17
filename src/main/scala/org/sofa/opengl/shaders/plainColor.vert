#version 330

layout(location=0) in vec3 position;
layout(location=1) in vec4 color;

uniform mat4 MVP;

smooth out vec4 ex_Color;

void main(void) {
	gl_Position = MVP * vec4(position, 1);
	ex_Color    = color;
}
