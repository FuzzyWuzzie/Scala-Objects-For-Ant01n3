#version 110
//precision highp float;
//precision highp int;

varying vec2 X;

uniform sampler2D texColor;

void main(void) {
	vec4 color = texture2D(texColor, X);
	gl_FragColor = vec4(color.rgb, color.a*0.9);
}
