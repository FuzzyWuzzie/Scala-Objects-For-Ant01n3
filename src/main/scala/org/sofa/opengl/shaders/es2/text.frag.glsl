#version 110

//precision highp float;
//precision highp int;

varying vec2 vTexCoords;

uniform sampler2D texColor;
uniform vec4 textColor;

void main(void) {
	vec3  CC = textColor.rgb;
	float at = texture2D(texColor, vTexCoords.st).a;
	float ac = textColor.a;

	gl_FragColor = vec4(CC.r, CC.g, CC.b, at*ac);
}
