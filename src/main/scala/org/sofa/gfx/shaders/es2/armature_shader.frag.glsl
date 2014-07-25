#version 110
//precision highp float;
//precision highp int;

varying vec2 X;

uniform sampler2D texColor;
uniform float highlight;

void main(void) {
	vec4  col = texture2D(texColor, X.st);

	if(highlight != 0.0)
		col.r = 1.0*col.a;

	gl_FragColor = col;
}