#version 110

varying vec2 vTexCoords;
varying vec4 vColor;

uniform sampler2D texColor;


/** Shader suitable for GLText instances. */
void main(void) {
	gl_FragColor = texture2D(texColor, vTexCoords.st) * vColor;
}