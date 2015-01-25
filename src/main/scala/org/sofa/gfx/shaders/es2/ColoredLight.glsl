#ifndef COLORED_LIGHT
#define COLORED_LIGHT
#include <es2/light.glsl>


vec4 coloredLightPlastic(vec3 P, vec3 N, vec4 C, in ColoredLight light) {
	vec3  L  = light.P - P;
	float d  = length(L); 

	L = normalize(L);
	N = normalize(N);

	float D  = diffuse(N, L);
	float S  = specular(N, L, light.R);
	vec3  CS = light.Cs.rgb;
	vec3  CD = light.Cd.rgb;
	vec3  CA = light.Ca.rgb;

	d = 1.0 / (light.Ac + light.Al * d + light.Aq * d * d);

	return vec4(C.rgb * ((light.Ka * CA * d) + (light.Kd * D * CD * d)) + (light.Ks * S * CS * d), C.a);
}


vec4 coloredLightPlastic2(vec3 P, vec3 N, vec4 C, in ColoredLight light[2]) {
	vec4 R[2];

	for(int i=0; i<2; i++) {
		vec3  L  = light[i].P - P;
		float d  = length(L); 

		L = normalize(L);
		N = normalize(N);

		float D  = diffuse(N, L);
		float S  = specular(N, L, light[i].R);
		vec3  CS = light[i].Cs.rgb;
		vec3  CD = light[i].Cd.rgb;
		vec3  CA = light[i].Ca.rgb;

		d = 1.0 / (light[i].Ac + light[i].Al * d + light[i].Aq * d * d);

		R[i] = vec4(C.rgb * ((light[i].Ka * CA * d) + (light[i].Kd * D * CD * d)) + (light[i].Ks * S * CS * d), C.a);
	}

	return mix(R[0], R[1], 0.5);
}


vec4 coloredLightPlastic4(vec3 P, vec3 N, vec4 C, in ColoredLight light[4]) {
	vec4 R[4];

	for(int i=0; i<4; i++) {
		vec3  L  = light[i].P - P;
		float d  = length(L); 

		L = normalize(L);
		N = normalize(N);

		float D  = diffuse(N, L);
		float S  = specular(N, L, light[i].R);
		vec3  CS = light[i].Cs.rgb;
		vec3  CD = light[i].Cd.rgb;
		vec3  CA = light[i].Ca.rgb;

		d = 1.0 / (light[i].Ac + light[i].Al * d + light[i].Aq * d * d);

		R[i] = vec4(C.rgb * ((light[i].Ka * CA * d) + (light[i].Kd * D * CD * d)) + (light[i].Ks * S * CS * d), C.a);
		// Using a single vec4 R, and doing R+= vec4 * 0.25 is ultra slow ... why ?
	}

	return mix(mix(R[0], R[1], 0.5), mix(R[2], R[3], 0.5), 0.5);
}


vec4 coloredLightPlastic8(vec3 P, vec3 N, vec4 C, in ColoredLight light[8]) {
	vec4 R[8];

	for(int i=0; i<8; i++) {
		vec3  L  = light[i].P - P;
		float d  = length(L); 

		L = normalize(L);
		N = normalize(N);

		float D  = diffuse(N, L);
		float S  = specular(N, L, light[i].R);
		vec3  CS = light[i].Cs.rgb;
		vec3  CD = light[i].Cd.rgb;
		vec3  CA = light[i].Ca.rgb;

		d = 1.0 / (light[i].Ac + light[i].Al * d + light[i].Aq * d * d);

		R[i] = vec4(C.rgb * ((light[i].Ka * CA * d) + (light[i].Kd * D * CD * d)) + (light[i].Ks * S * CS * d), C.a);
	}

	return mix(mix(mix(R[0], R[1], 0.5), mix(R[2], R[3], 0.5), 0.5), mix(mix(R[4], R[5], 0.5), mix(R[6], R[7], 0.5), 0.5), 0.5);
}



vec4 singleColoredLightMatte(vec3 P, vec3 N, vec4 C, in ColoredLight light) {
	vec3  L  = light.P - P;
	float d  = length(L); 

	L = normalize(L);
	N = normalize(N);

	float D  = diffuse(N, L);
	vec3  CD = light.Cd.rgb;
	vec3  CA = light.Ca.rgb;

	d = 1.0 / (light.Ac + light.Al * d + light.Aq * d * d);

	return vec4(C.rgb * ((light.Ka * CA * d) + (light.Kd * D * CD * d)), C.a);
}


vec4 singleColoredLightMetal(vec3 P, vec3 N, vec4 C, in ColoredLight light) {
	vec3  L  = light.P - P;
	float d  = length(L); 

	L = normalize(L);
	N = normalize(N);

	float D  = diffuse(N, L);
	float S  = specular(N, L, light.R);
	vec3  CS = light.Cs.rgb;
	vec3  CD = light.Cd.rgb;
	vec3  CA = light.Ca.rgb;

	d = 1.0 / (light.Ac + light.Al * d + light.Aq * d * d);

	// Yes, only the parenthesis changed, specular is comprised in the mult with
	// the color.
	return vec4(C.rgb * ((light.Ka * CA * d) + (light.Kd * D * CD * d) + (light.Ks * S * CS * d)), C.a);
}


#endif COLORED_LIGHT