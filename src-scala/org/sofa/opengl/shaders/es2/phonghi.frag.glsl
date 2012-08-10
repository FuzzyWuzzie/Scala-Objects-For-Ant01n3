#version 120
#include <es2/lightStruct.glsl>

varying vec3 vNormal;
varying vec3 vPosition;
varying vec4 vColor;

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


	gl_FragColor = ((vColor * d * light.intensity) / DD)
	             + ((vec4(1,1,1,1) * s * light.intensity) / DD)
		 		 + (vColor * light.ambient);
}
