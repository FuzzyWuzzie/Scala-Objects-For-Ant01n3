#version 110
//precision highp float;
//precision highp int;

varying vec2 X;

uniform sampler2D texColor;
uniform sampler2D texMask;
uniform vec3 sunDir;

void main(void) {
	float diffuse   = 0.5;
	float shininess = 1.0;
	float specular  = 1.0;
	float ambient   = 1.0 -  diffuse;
	vec3  l         = normalize(sunDir);

	vec4  c = texture2D(texColor, X.st);											// Base color.
	vec3  n = texture2D(texMask, X.st).xyz;											// Normal from mask.
	float d = max(dot(n, l), 0.0);													// Diffuse coef.
	vec3  r = reflect(-l, n);														// Reflect vector.
	float s = pow(max(dot(r, vec3(0.57735, 0.57735, 0.57735)), 0.0), shininess);	// Specular coef.	Observer is constant.

	d = clamp(d * diffuse + ambient, 0.0, 1.0);										// Mix diffuse and ambient.
	s = s * specular;

	if(c.a > 0.95) 	// We consider a specular always white, so just add 's'. But only for non transparent pixels.
	     gl_FragColor = vec4(c.r * d + s, c.g * d + s, c.b * d + s, c.a);
	else gl_FragColor = vec4(c.r * d,     c.g * d,     c.b * d,     c.a);
}