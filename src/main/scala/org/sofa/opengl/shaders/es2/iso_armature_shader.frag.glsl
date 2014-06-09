#version 110
//precision highp float;
//precision highp int;

varying vec2 X;

uniform sampler2D texColor;
uniform sampler2D texMask;
uniform float highlight;
uniform vec3 lightDir;

void main(void) {
	vec4  col = texture2D(texColor, X.st);
	vec3  msk = texture2D(texMask, X.st).xyz;
	float dif = max(dot(msk,normalize(lightDir)),0.0);

	dif *= 0.5;
	dif += 0.5;

	dif = max(dif, 0.0);
	dif = min(dif, 1.0);

	if(highlight != 0.0)
		col.r = 1.0*col.a;

	if(dif >= 0) 
	     gl_FragColor = vec4(col.r * dif, col.g * dif, col.b * dif, col.a);
	else gl_FragColor = vec4(0, 0, 0, col.a);
}