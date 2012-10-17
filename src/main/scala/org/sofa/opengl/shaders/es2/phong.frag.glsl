#version 120
#include <es2/lightStruct.glsl>

varying vec3 vNormal;
varying vec3 vLightDir;
varying vec4 vColor;
varying float vLightDist;

uniform Light light;

void main(void) {
    vec3  l = normalize(vLightDir);
	vec3  n = normalize(vNormal);
	float d = max(dot(n, l), 0);
	vec3  r = normalize(reflect(-l, n));
	float s = pow(max(dot(n, r), 0), light.specular);
	float dd = vLightDist * vLightDist;

	gl_FragColor = ((vColor * d * light.intensity) / dd)
	          + ((vec4(1,1,1,1) * s * light.intensity) / dd)
	          + (vColor * light.ambient);
}
