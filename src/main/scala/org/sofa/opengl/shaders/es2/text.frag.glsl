#version 110

varying vec2 vTexCoords;

uniform sampler2D texColor;
uniform vec4 textColor;

/** Shader suitable for text with the JDK. */
void main(void) {
	vec4 C = texture2D(texColor, vTexCoords.st);

	gl_FragColor = vec4(C.r-(1.0-textColor.r), C.g-(1.0-textColor.g), C.b-(1.0-textColor.b), C.a*textColor.a);
}