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
	vec3  c  = vec3(C.rgb);

	out_Color = vec4((c * d * lights[0].intensity) / dd, C.a)
	          + vec4((vec3(1, 1, 1) * s * lights[0].intensity) / dd, 0)
	          + vec4(c * lights[0].ambient, 0);
}
