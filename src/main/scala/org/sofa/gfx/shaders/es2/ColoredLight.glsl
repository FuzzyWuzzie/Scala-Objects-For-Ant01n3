#ifndef COLORED_LIGHT
#define COLORED_LIGHT
#include <es2/light.glsl>

vec4 singleColoredLightPlastic(vec3 P, vec3 N, vec4 C) {
	vec3  L  = light.pos - P;
	float d  = length(L); 

	L = normalize(L);
	N = normalize(N);

	float D  = diffuse(N, L);
	float S  = specular(N, L, light.roughness);
	vec3  CS = light.specular.rgb;
	vec3  CD = light.diffuse.rgb;
	vec3  CA = light.ambient.rgb;

	d = 1.0 / (light.constAtt + light.linAtt * d + light.quadAtt * d * d);

	return vec4(C.rgb * ((light.Ka * CA * d) + (light.Kd * D * CD * d)) + (light.Ks * S * CS * d), C.a);
}

/*
vec4 singleColoredLightPlastic(in vec3 P, in vec3 N, in vec4 C, in mat3 TBN) {
	vec3  L  = whitelight.pos - P;
	float d  = length(L);
	      L  = normalize(TBN * L);
	float D  = diffuse(N, L);
	float S  = specular(N, L, whitelight.specular);
	vec3  CC = C.rgb;
	vec3  SS = vec3(1,1,1);

	d = d * d;

	return vec4(
		((CC * D * whitelight.intensity) / d)
	  + ((SS * S * whitelight.intensity) / d)
	  + ( CC * whitelight.ambient), C.a);
}
*/
vec4 singleColoredLightMatte(vec3 P, vec3 N, vec4 C) {
	vec3  L  = light.pos - P;
	float d  = length(L); 

	L = normalize(L);
	N = normalize(N);

	float D  = diffuse(N, L);
	vec3  CD = light.diffuse.rgb;
	vec3  CA = light.ambient.rgb;

	d = 1.0 / (light.constAtt + light.linAtt * d + light.quadAtt * d * d);

	return vec4(C.rgb * ((light.Ka * CA * d) + (light.Kd * D * CD * d)), C.a);
}

vec4 singleColoredLightMetal(vec3 P, vec3 N, vec4 C) {
	vec3  L  = light.pos - P;
	float d  = length(L); 

	L = normalize(L);
	N = normalize(N);

	float D  = diffuse(N, L);
	float S  = specular(N, L, light.roughness);
	vec3  CS = light.specular.rgb;
	vec3  CD = light.diffuse.rgb;
	vec3  CA = light.ambient.rgb;

	d = 1.0 / (light.constAtt + light.linAtt * d + light.quadAtt * d * d);

	// Yes, only the parenthesis changed, specular is comprised in the mult with
	// the color.
	return vec4(C.rgb * ((light.Ka * CA * d) + (light.Kd * D * CD * d) + (light.Ks * S * CS * d)), C.a);
}


#endif COLORED_LIGHT