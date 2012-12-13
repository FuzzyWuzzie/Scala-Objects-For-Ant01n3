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

void main(void) {							// TBN and not TNB since most nmap textures uses Z up, therefore we invert Z (B) and Y (N).
	mat3  TBN = transpose(mat3(T, B, N));	// Matrix to transform from model view space into tangent space (to transpose here is to inverse, since the basis is orthogonal, but faster).
	vec3  n   = normalize((2 * (texture2D(texNormal, X.st).rgb)) - 1);
	vec3  l   = whitelight.pos - P;
	float ll  = length(l);
	      l   = normalize(TBN * l);
	float d   = max(dot(n, l), 0);			// Diffuse component.
	
	vec3  r   = normalize(reflect(-l, n));
	float s   = pow(max(dot(n, r), 0), whitelight.specular);

	float dd  = ll * ll;					// The whitelight decrease inversely with the square of the distance.

	vec4 Ca= texture2D(texColor, X.st);
	vec3 C = texture2D(texColor, X.st).rgb;
	vec3 S = vec3(1,1,1);// texture(texSpec, X.st);

	gl_FragColor = vec4( ((C * d * whitelight.intensity) / dd)
	                   + ((S * s * whitelight.intensity) / dd)
	                   + (C * whitelight.ambient), Ca.a);
}
