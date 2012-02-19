#version 330

struct Light {
	vec3 pos;
	float intensity;
	float ambient;
	float specular;
};

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
	vec4 S;
	
	mat3  TBN = transpose(mat3(T, B, N));
	vec3  n   = normalize((2 * (texture(tex.normal, UV.st).rgb)) - 1);
	vec3  l   = normalize(TBN * L);
	float d   = max(dot(n, l), 0);
	vec3  r   = normalize(reflect(-l, n));
	float s   = pow(max(dot(n, r), 0), lights[0].specular);
	float dd  = LL * LL;

	C = texture(tex.color, UV.st);
	S = vec4(1, 1, 1, 1);

	C = ((C * d * lights[0].intensity) / dd)
	  + ((S * s * lights[0].intensity) / dd)
	  + ( C *     lights[0].ambient);
}
