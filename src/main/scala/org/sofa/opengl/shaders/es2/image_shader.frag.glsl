#version 110

varying vec2 vTexCoords;

uniform sampler2D texColor;

void main(void) {
	gl_FragColor = texture2D(texColor, vTexCoords.st);
}