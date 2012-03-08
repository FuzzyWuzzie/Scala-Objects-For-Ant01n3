#version 330

uniform vec4 uniformColor;

out vec4 out_Color;

void main(void) {
	out_Color = uniformColor;
}
