#version 110

varying vec2 vTexCoords;

uniform sampler2D texColor;
uniform vec4 textColor;

/** Shader suitable for text with the JDK.
  * It supports premultiplied textures, provided the textColor is
  * also premultiplied by its own alpha. */
void main(void) {
	gl_FragColor = texture2D(texColor, vTexCoords.st) * textColor;
}