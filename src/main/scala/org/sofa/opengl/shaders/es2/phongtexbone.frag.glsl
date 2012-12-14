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

#include <es2/whiteLight.glsl>

void main(void) {							// TBN and not TNB since most nmap textures uses Z up, therefore we invert Z (B) and Y (N).
	mat3  TBN = transpose(mat3(T, B, N));	// Matrix to transform from model view space into tangent space (to transpose here is to inverse, since the basis is orthogonal, but faster).
	vec3  n   = normalize((2 * (texture2D(texNormal, X.st).rgb)) - 1);
	vec4  c   = texture2D(texColor, X.st);

	gl_FragColor = singleWhiteLight(P, n, c, TBN);
}
