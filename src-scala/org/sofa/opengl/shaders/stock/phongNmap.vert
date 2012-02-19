#version 330

struct Light {
	vec3 pos;
	float intensity;
	float ambient;
	float specular;
};

// Input:
layout(location=0) in vec3 pos;			// Position.
layout(location=2) in vec3 normal;		// Normal.
layout(location=3) in vec3 tangent;		// Tangent.
layout(location=4) in vec2 texPos;		// Texture.

// Global:
uniform mat4 MVP;						// Perspective * View * Model.
uniform mat4 MV;						// View * Model.
uniform mat3 MV3x3;						// Upper 3x3 MV, without scaling and translation.
uniform Light lights[1]; 

// Output:
smooth out vec3 N;						// Normal out.
smooth out vec3 T;						// Tangent out.
smooth out vec3 B;						// Bi-tangent out.
smooth out vec3 L;						// Light direction out.
smooth out vec2 UV;						// Texture coordinates out.
smooth out float LL;					// Light distance out.

void main(void) {
	vec4 P;

	P  = MV * vec4(pos, 1);
	N  = normalize(MV3x3 * normal);
	T  = normalize(MV3x3 * tangent);
	B  = normalize(cross(T, N));
	L  = lights[0].pos - vec3(P);
	LL = length(L);
	L  = normalize(L);
	UV = texPos;

	gl_Position = MVP * vec4(pos, 1);
}
