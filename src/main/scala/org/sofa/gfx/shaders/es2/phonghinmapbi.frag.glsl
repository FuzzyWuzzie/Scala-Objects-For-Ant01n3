#version 120

// Input:
varying vec3 N;		// Normal,
varying vec3 T;		// Tangent,
varying vec3 B;		// Bi-Tangent,
varying vec2 X;		// Texture Coordinates,
varying vec3 P;		// Position

// Global:
uniform mat3 MV3x3;				// Upper 3x3 matrix of MV, without scaling or translation,
uniform vec3 lightPos;			// Position of a light already in MV space.
uniform float lightIntensity;	// Light intensity [0..1[,
uniform float ambientIntensity;	// Ambient light intensity [0..1[,
uniform float specularPow;		// Specular spot importance,
uniform sampler2D texColor;		// Color texture,
uniform sampler2D texNormal;	// Normal map texture,
uniform sampler2D texSpec;		// Specular texture.

void main(void) {							// TBN and not TNB since most nmap textures uses Z up, therefore we invert Z (B) and Y (N).
	mat3  TBN = transpose(mat3(T, B, N));	// Matrix to transform from model view space into tangent space (to transpose here is to inverse, since the basis is orthogonal, but faster).
	vec3  n   = normalize((2 * (texture2D(texNormal, X.st).rgb)) - 1);
	vec3  l   = lightPos - P;
	float ll  = length(l);
	      l   = normalize(TBN * l);
	float d   = max(dot(n, l), 0);			// Diffuse component.
	 
	vec3  r   = normalize(reflect(-l, n));
	float s   = pow(max(dot(n, r), 0), specularPow);

	float dd  = ll * ll;					// The light decrease inversely with the square of the distance.

//	vec4 S = texture2D(texSpec, X.st);
	vec4 C = texture2D(texColor, X.st);
	vec4 S = vec4(1,1,1,1);// texture(texSpec, X.st);
//	C = vec4(0.9, 0.4, 0.3, 1);

	gl_FragColor = ((C * d * lightIntensity) / dd)
	             + ((S * s * lightIntensity) / dd)
	             + (C * ambientIntensity);
}
