#ifndef WHITE_LIGHT
#define WHITE_LIGHT
#include <es2/light.glsl>

// -- PHONG -------------------------------------------

/** Compute the color of vertex at position P, with normal N with absolute color C.
  *
  * This is a basic goureau (if called in the vertex shader) or phong
  * (if called in the fragment shader) model.
  *
  * In order to work a uniform "WhiteLight whitelight;" must be declared using the
  * structure in "whiteLightStruct.glsl".
  *
  * Return: the lighted color.
  */
vec4 singleWhiteLightPhong(vec3 P, vec3 N, vec4 C) {
	vec3  L  = whitelight.pos - P;
	float d  = length(L);
	      L  = normalize(L);
	      N  = normalize(N);
	float D  = diffuse(N, L);
	float S  = specular(N, L, whitelight.specular);
	vec3  SS = vec3(1, 1, 1);
	vec3  CC = C.rgb;

	d = d * d;

	return vec4(
			  ((CC * D * whitelight.intensity) / d)
			+ ((SS * S * whitelight.intensity) / d)
			+ ( CC     * whitelight.ambient), C.a);
}

/** Compute the color of vertex at position P, with normal N with absolute color C
  * and Tangent space basis TBN.
  *
  * This is a basic goureau (if called in the vertex shader) or phong
  * (if called in the fragment shader) model with n-map support.
  *
  * In order to work a uniform "WhiteLight whitelight;" must be declared using the
  * structure in "whiteLightStruct.glsl".
  *
  * Return: the lighted color.
  */
vec4 singleWhiteLightPhong(in vec3 P, in vec3 N, in vec4 C, in mat3 TBN) {
	vec3  L  = whitelight.pos - P;
	float d  = length(L);
	      L  = normalize(TBN * L);
	      N  = normalize(N);
	float D  = diffuse(N, L);
	float S  = specular(N, L, whitelight.specular);
	vec3  CC = C.rgb;
	vec3  SS = vec3(1,1,1);

	d = d * d;

	return vec4(
		((CC * D * whitelight.intensity) / d)
	  + ((SS * S * whitelight.intensity) / d)
	  + ( CC     * whitelight.ambient), C.a);
}

// -- MATTE ------------------------------------------------

/** Compute the color of vertex at position P, with normal N with absolute color C.
  *
  * In order to work a uniform "WhiteLight whitelight;" must be declared using the
  * structure in "whiteLightStruct.glsl".
  *
  * Return: the lighted color.
  */
vec4 singleWhiteLightMatte(vec3 P, vec3 N, vec4 C) {
	vec3  L  = whitelight.pos - P;
	float d  = length(L);
	      L  = normalize(L);
	      N  = normalize(N);
	float D  = diffuse(N, L);
	vec3  CC = C.rgb;

	d = d * d;

	return vec4(
			  ((CC * D * whitelight.intensity) / d)
			+ ( CC     * whitelight.ambient), C.a);
}


#endif