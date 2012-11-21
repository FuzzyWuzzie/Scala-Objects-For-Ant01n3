#version 120
#include <es2/lightStruct.glsl>

varying vec3 vNormal;
varying vec3 vPosition;
varying vec2 vTexCoords;

uniform sampler2D texColor;
uniform Light light;

void main(void) {
	vec3  l;
	float D;

	l         = light.pos - vPosition;					// Light vector
	D         = length(l);								// Light distance
	l         = normalize(l);
	vec3    n = normalize(vNormal);
	float   d = max(dot(n, l), 0);						// Diffuse
	vec3    r = normalize(reflect(-l, n));				// Reflection vector
	float   s = pow(max(dot(n, r), 0), light.specular);	// Specular
	float  DD = D * D;									// D^2
	vec4   CC = texture2D(texColor, vTexCoords.st);
	vec3    C = CC.rgb;
	float   a = CC.a;

	C = ((C * d * light.intensity) / DD)
	  + ((vec3(1,1,1) * s * light.intensity) / DD)
	  + (C * light.ambient);

	gl_FragColor = vec4(C.rgb, a);
}