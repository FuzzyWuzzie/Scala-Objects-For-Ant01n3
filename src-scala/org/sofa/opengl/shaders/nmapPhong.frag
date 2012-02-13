#version 330

// Input:
in vec3 N;		// Normal,
in vec3 T;		// Tangent,
in vec3 B;		// Bi-Tangent,
in vec3 L;		// Light Direction,
in vec2 X;		// Texture Coordinates,
in float LL;	// Light Distance.

// Global:
uniform mat3 MV3x3;				// Upper 3x3 matrix of MV, without scaling or translation,
uniform float lightIntensity;	// Light intensity [0..1[,
uniform float ambientIntensity;	// Ambient light intensity [0..1[,
uniform float specularPow;		// Specular spot importance,
uniform sampler2D texColor;		// Color texture,
uniform sampler2D texNormal;	// Normal map texture,
uniform sampler2D texSpec;		// Specular texture.

// Output:
out vec4 C;

void main(void) {							// TBN and not TNB since most nmap textures uses Z up, therefore we invert Z (B) and Y (N).
	mat3  TBN = transpose(mat3(T, B, N));	// Matrix to transform from model view space into tangent space (to transpose here is to inverse, since the basis is orthogonal, but faster).
	vec3  n   = normalize((2 * (texture(texNormal, X.st).rgb)) - 1);
    vec3  l   = normalize(TBN * L);			// Pass the light direction into tangent space.
	float d   = max(dot(n, l), 0);			// Diffuse component.
	
	vec3  r   = normalize(reflect(-l, n));
	float s   = pow(max(dot(n, r), 0), specularPow);

	float dd  = LL * LL;						// The light decrease inversely with the square of the distance.

//	vec4 S = texture(texSpec, X.st);
	C = texture(texColor, X.st);
	vec4 S = vec4(1,1,1,1);// texture(texSpec, X.st);
//	C = vec4(0.9, 0.4, 0.3, 1);

	C = ((C * d * lightIntensity) / dd)
	  + ((S * s * lightIntensity) / dd)
	  + (C * ambientIntensity);
}
