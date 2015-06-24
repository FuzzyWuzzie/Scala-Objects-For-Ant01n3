#version 110
//precision highp float;
//precision highp int;

varying vec3 vNormal;
varying vec3 vPosition;
varying vec4 vColor;
varying vec3 vBary;

#include <es2/lightStruct.glsl>
uniform Light light;

void main(void) {
	vec3  l;
	float D;

	l         = light.pos - vPosition;						// Light vector
	D         = length(l);									// Light distance
	l         = normalize(l);
	vec3    n = normalize(vNormal);
	float   d = max(dot(n, l), 0.0);						// Diffuse
	vec3    r = normalize(reflect(-l, n));					// Reflection vector
	float   s = pow(max(dot(n, r), 0.0), light.specular);	// Specular
	float  DD = D * D;										// D^2
	float   a = vColor.a;
	vec3    C = vec3(vColor.rgb);

	C = ((C * d * light.intensity) / DD)
	  + ((vec3(1,1,1) * s * light.intensity) / DD)
	  + (C * light.ambient);

	if(any(lessThan(vBary, vec3(0.05)))) {
    	gl_FragColor = vec4(C.rgb, a);
	} else {
		vec3 v = smoothstep(0.05, 0.2, vBary);
		float f = 1-min(min(v.x, v.y), v.z);
    	gl_FragColor = vec4(C.rgb*(0.5+f*0.5), a);
	}
}
