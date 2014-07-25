#version 110

attribute vec3 position;
attribute vec4 color;

uniform mat4 MVP;
uniform mat4 MV;
uniform mat3 MV3x3;

uniform float pointSize;

varying vec4 vColor;

void main(void) {
	vColor = color;
	gl_Position = MVP * vec4(position, 1);
	gl_PointSize = pointSize / gl_Position.w;
}
