#version 110
//precision highp float;
//precision highp int;

varying vec2 X;

uniform sampler2D texColor;
uniform float highlight;

void main(void) {
	vec4 c = texture2D(texColor, X);

	if(highlight != 0.0) {
		gl_FragColor = vec4(1.0*c.a, c.g, c.b, c.a);
	} else {
		gl_FragColor = c;
	}
}
