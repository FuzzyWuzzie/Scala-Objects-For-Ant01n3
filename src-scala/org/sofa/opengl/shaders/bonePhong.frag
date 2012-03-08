#version 330
#include <lightStruct.glsl>

in vec3 N;
in vec3 L;
in vec4 C;
in float LL;

uniform Light lights[1];

out vec4 out_Color;

void main(void) {
	vec3  l  = normalize(L);
	vec3  n  = normalize(N);
	float d  = max(dot(n, l), 0);
	vec3  r  = normalize(reflect(-l, n));
	float s  = pow(max(dot(n, r), 0), lights[0].specular);
	float dd = LL * LL;

	out_Color = ((C * d * lights[0].intensity) / dd)
	          + ((vec4(1, 1, 1, 1) * s * lights[0].intensity) / dd)
	          +  (C * lights[0].ambient);
}
