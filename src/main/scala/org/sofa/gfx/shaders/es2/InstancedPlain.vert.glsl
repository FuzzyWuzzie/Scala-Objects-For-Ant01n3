#version 110

// Input
attribute vec3 position;
attribute vec4 color;
attribute vec3 offset;

// Global
uniform mat4 MVP;

// Output
varying vec4 vColor;

void main() {
	vec4 p = vec4(position + offset, 1);
	vColor = color;
	vColor = vec4(1,0,0,1);
	gl_Position = MVP * p;
}
