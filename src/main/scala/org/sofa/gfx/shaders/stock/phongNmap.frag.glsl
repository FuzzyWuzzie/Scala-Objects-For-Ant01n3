#version 330
#include <lightStruct.glsl>

struct Texture {
	sampler2D color;
	sampler2D normal;
};

// Input:
in vec3 N;
in vec3 T;
in vec3 B;
in vec3 L;
in vec2 UV;
in float LL;

// Global:
uniform mat3 MV3x3;
uniform Light lights[1];
uniform Texture tex;

// Output:
out vec4 C;

void main(void) {
	mat3  TBN = transpose(mat3(T, B, N));
	vec3  n   = normalize((2 * (texture(tex.normal, UV.st).rgb)) - 1);
	vec3  l   = normalize(TBN * L);
	float d   = max(dot(n, l), 0);
	vec3  r   = normalize(reflect(-l, n));
	float s   = pow(max(dot(n, r), 0), lights[0].specular);
	float dd  = LL * LL;

	C = texture(tex.color, UV.st);
	vec3 c = vec3(C.rgb);
	vec3 S = vec3(1, 1, 1);

	C = vec4((c * d * lights[0].intensity) / dd, C.a)
	  + vec4((S * s * lights[0].intensity) / dd, 0)
	  + vec4( c *     lights[0].ambient, 0);
}
