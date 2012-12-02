#version 120
#include <es2/lightStruct.glsl>

varying vec3 vNormal;
varying vec3 vPosition;
varying vec2 vTexCoords;

uniform sampler2D texColor;
uniform Light light;

void main(void) {
	vec3  l;
	float D;

	// Compute specular highlight.

	l         = light.pos - vPosition;					// Light vector
	D         = length(l);								// Light distance
	l         = normalize(l);
	vec3    n = normalize(vNormal);
//	float   d = max(dot(n, l), 0);						// Diffuse
	vec3    r = normalize(reflect(-l, n));				// Reflection vector
	float   s = pow(max(dot(n, r), 0), light.specular);	// Specular
	float  DD = D * D;									// D^2
	
	// Compute intensity from the texture.

	float I = texture2D(texColor, vTexCoords.st).a;		// Texture alpha = intensity.

	// Create the "led" pattern.

	float u = fract(vTexCoords.s*200);					// UV coordinates times number
	float v = fract(vTexCoords.t*100);					//   of dots per line and column.

	if(u > 0.5) { u = (0.5-(u-0.5))*2; } else { u *= 2; }
	if(v > 0.5) { v = (0.5-(v-0.5))*2; } else { v *= 2; }

	// Final intensity and color.

	float i = u*v*I;
	vec3  B = vec3(i, i*0.7, 0);

	// Decrease the specular highlight.

	// Compute the final color.

	B = B								
	  + ((vec3(1,1,1) * s * light.intensity * 0.05) / DD)	// Specular.
	  + (B * light.ambient);						// Ambient.

	gl_FragColor = vec4(B.rgb, 1);
}