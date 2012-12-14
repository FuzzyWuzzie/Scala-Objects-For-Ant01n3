#version 120
#include <es2/whiteLightStruct.glsl>

// Input:
varying vec3 N;		// Normal,
varying vec3 T;		// Tangent,
varying vec3 B;		// Bi-Tangent,
varying vec2 X;		// Texture Coordinates,
varying vec3 P;		// Position

// Global:
uniform mat3 MV3x3;				// Upper 3x3 matrix of MV, without scaling or translation,
uniform WhiteLight whitelight;
uniform sampler2D texColor;		// Color texture,
uniform sampler2D texNormal;	// Normal map texture,

vec4 singleWhiteLight(vec3 P, vec3 N, vec4 C) {
	vec3  L = whitelight.pos - P;
	float d = length(L);
	float D = max(dot(N, L), 0.0);
	vec3  R = normalize(reflect(-L, N));
	float S = pow(max(dot(N, R), 0.0), whitelight.specular);

	d = d * d;

	vec3  SS = vec3(1, 1, 1);
	vec3  CC = C.rgb;
	float A  = C.a;

	return vec4(
			  ((CC * D * whitelight.intensity) / d)
			+ ((SS * S * whitelight.intensity) / d)
			+ ( CC * whitelight.ambient), A);
}

vec4 singleWhiteLight(in vec3 P, in vec3 N, in vec4 C, in mat3 TBN) {
	vec3  L  = whitelight.pos - P;
	float d  = length(L);
	      L  = normalize(TBN * L);
	float D  = max(dot(N, L), 0);
	vec3  R  = normalize(reflect(-L, N));
	float S  = pow(max(dot(N, R), 0), whitelight.specular);
	vec3  CC = C.rgb;
	vec3  SS = vec3(1,1,1);

	d = d * d;

	return vec4(
		((CC * D * whitelight.intensity) / d)
	  + ((SS * S * whitelight.intensity) / d)
	  + ( CC * whitelight.ambient), C.a);
}

void main(void) {							// TBN and not TNB since most nmap textures uses Z up, therefore we invert Z (B) and Y (N).
	mat3  TBN = transpose(mat3(T, B, N));	// Matrix to transform from model view space into tangent space (to transpose here is to inverse, since the basis is orthogonal, but faster).
	vec3  n   = normalize((2 * (texture2D(texNormal, X.st).rgb)) - 1);
	vec4  c   = texture2D(texColor, X.st);

	gl_FragColor = singleWhiteLight(P, n, c, TBN);
}
