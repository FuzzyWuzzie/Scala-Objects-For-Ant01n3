#ifndef COLORED_LIGHT
#define COLORED_LIGHT
#include <light.glsl>


// ------------------------------------------------------------------------------------

/** Utility function to compute a single colored light plastic (diffuse, specular and ambient).
  * @param P The position.
  * @param N The normal to the lighted surface, considered normalized.
  * @param C The color of the surface.
  * @param light The ColoredLight parameters.
  * @param i The importance of this light source. */
vec4 coloredLightPlastici(inout vec3 P, inout vec3 N, inout vec4 C, inout ColoredLight light, float i) {
	vec3  L  = light.P - P;
	float d  = length(L); 

	L = normalize(L);

	float D = diffuse(N, L);
	float S = specular(N, L, light.R);

	d = 1.0 / (light.Ac + light.Al * d + light.Aq * d * d);

	return vec4(C.rgb * 
			( (light.Ka *     light.Ca.rgb) 
			+ (light.Kd * D * light.Cd.rgb * d)) 
			+ (light.Ks * S * light.Cs.rgb * d) , C.a) * i;
}


/** Utility function to compute a single colored light matte (diffuse and ambient).
  * @param P The position.
  * @param N The normal to the lighted surface, considered normalized.
  * @param C The color of the surface.
  * @param light The ColoredLight parameters.
  * @param i The importance of this light source. */
vec4 coloredLightMattei(inout vec3 P, inout vec3 N, inout vec4 C, inout ColoredLight light, float i) {
	vec3  L  = light.P - P;
	float d  = length(L); 

	L = normalize(L);

	float D = diffuse(N, L);

	d = 1.0 / (light.Ac + light.Al * d + light.Aq * d * d);

	return vec4(C.rgb * 
			( (light.Ka *     light.Ca.rgb) 
			+ (light.Kd * D * light.Ca.rgb * d)), C.a) * i;
}


// ------------------------------------------------------------------------------------


/** Compute a single color light source on a plastic surface with diffuse, specular an ambient. */
vec4 coloredLightPlastic(in vec3 P, in vec3 N, in vec4 C, in ColoredLight light) {
	return coloredLightPlastici(P, N, C, light, 1.0);
}


/** Compute two color light sources on a plastic surface with diffuse, specular an ambient. */
vec4 coloredLightPlastic2(vec3 P, vec3 N, vec4 C, in ColoredLight light[2]) {
	// Avoid loops.
	return coloredLightPlastici(P, N, C, light[0], 0.5)
	     + coloredLightPlastici(P, N, C, light[1], 0.5);
}


/** Compute four color light sources on a plastic surface with diffuse, specular an ambient. */
vec4 coloredLightPlastic4(in vec3 P, in vec3 N, in vec4 C, in ColoredLight light[4]) {
	// Avoid loops.
	return coloredLightPlastici(P, N, C, light[0], 0.25)
	     + coloredLightPlastici(P, N, C, light[1], 0.25)
	     + coloredLightPlastici(P, N, C, light[2], 0.25)
	     + coloredLightPlastici(P, N, C, light[3], 0.25);
}


/** Compute six color light sources on a plastic surface with diffuse, specular an ambient. */
vec4 coloredLightPlastic6(in vec3 P, in vec3 N, in vec4 C, in ColoredLight light[6]) {
	// Avoid loops.
	return coloredLightPlastici(P, N, C, light[0], 0.1666)
	     + coloredLightPlastici(P, N, C, light[1], 0.1666)
	     + coloredLightPlastici(P, N, C, light[2], 0.1666)
	     + coloredLightPlastici(P, N, C, light[3], 0.1666)
	     + coloredLightPlastici(P, N, C, light[4], 0.1666)
	     + coloredLightPlastici(P, N, C, light[5], 0.1666);
}


/** Compute height color light sources on a plastic surface with diffuse, specular an ambient. */
vec4 coloredLightPlastic8(in vec3 P, in vec3 N, in vec4 C, in ColoredLight light[8]) {
	// Avoid loops.
	return coloredLightPlastici(P, N, C, light[0], 0.125)
	     + coloredLightPlastici(P, N, C, light[1], 0.125)
	     + coloredLightPlastici(P, N, C, light[2], 0.125)
	     + coloredLightPlastici(P, N, C, light[3], 0.125)
	     + coloredLightPlastici(P, N, C, light[4], 0.125)
	     + coloredLightPlastici(P, N, C, light[5], 0.125)
	     + coloredLightPlastici(P, N, C, light[6], 0.125)
	     + coloredLightPlastici(P, N, C, light[7], 0.125);
}


// ------------------------------------------------------------------------------------


vec4 coloredLightMatte(vec3 P, vec3 N, vec4 C, in ColoredLight light) {
	return coloredLightMattei(P, N, C, light, 1.0);
}


vec4 coloredLightMatte2(vec3 P, vec3 N, vec4 C, in ColoredLight light[2]) {
	return coloredLightMattei(P, N, C, light[0], 0.5)
	     + coloredLightMattei(P, N, C, light[1], 0.5);
}


vec4 coloredLightMatte4(vec3 P, vec3 N, vec4 C, in ColoredLight light[4]) {
	return coloredLightMattei(P, N, C, light[0], 0.25)
	     + coloredLightMattei(P, N, C, light[1], 0.25)
	     + coloredLightMattei(P, N, C, light[2], 0.25)
	     + coloredLightMattei(P, N, C, light[3], 0.25);
}


vec4 coloredLightMatte6(vec3 P, vec3 N, vec4 C, in ColoredLight light[6]) {
	return coloredLightMattei(P, N, C, light[0], 0.1666)
	     + coloredLightMattei(P, N, C, light[1], 0.1666)
	     + coloredLightMattei(P, N, C, light[2], 0.1666)
	     + coloredLightMattei(P, N, C, light[3], 0.1666)
	     + coloredLightMattei(P, N, C, light[4], 0.1666)
	     + coloredLightMattei(P, N, C, light[5], 0.1666);
}


vec4 coloredLightMatte8(vec3 P, vec3 N, vec4 C, in ColoredLight light[8]) {
	return coloredLightMattei(P, N, C, light[0], 0.125)
	     + coloredLightMattei(P, N, C, light[1], 0.125)
	     + coloredLightMattei(P, N, C, light[2], 0.125)
	     + coloredLightMattei(P, N, C, light[3], 0.125)
	     + coloredLightMattei(P, N, C, light[4], 0.125)
	     + coloredLightMattei(P, N, C, light[5], 0.125)
	     + coloredLightMattei(P, N, C, light[6], 0.125)
	     + coloredLightMattei(P, N, C, light[7], 0.125);
}


// ------------------------------------------------------------------------------------


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
	return vec4(C.rgb * ((light.Ka * CA) + (light.Kd * D * CD * d) + (light.Ks * S * CS * d)), C.a);
}


#endif
//COLORED_LIGHT