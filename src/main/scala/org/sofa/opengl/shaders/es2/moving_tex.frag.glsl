#version 110

varying vec2 vTexCoords;

uniform sampler2D texColor;

void main(void) {
	vec4 c = texture2D(texColor, vTexCoords.st);
	gl_FragColor = c;
}