#version 110
//precision highp float;
//precision highp int;

varying vec2 X;

uniform sampler2D texColor;

void main(void) {
/*	vec4  color = texture2D(texColor, X);
	vec3  rgb   = color.rgb;
	float a     = color.a;

	if(a > 0.7) a = a;
	else a = 0.0;

	gl_FragColor = vec4(rgb, a);

	gl_FragColor = vec4(color.rgb, color.a*0.9);
*/	gl_FragColor = texture2D(texColor, X);
}
