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
vec4 singleWhiteLightPhong(vec3 P, vec3 N, vec4 C, in WhiteLight whitelight) {
	vec3  L  = whitelight.P - P;
	float d  = length(L);
	      L  = normalize(L);
	      N  = normalize(N);
	float D  = diffuse(N, L);
	float S  = specular(N, L, whitelight.R);
	vec3  SS = vec3(1, 1, 1);
	vec3  CC = C.rgb;

	d = d * d;

	return vec4(
			  ((CC * D * whitelight.Kd) / d)
			+ ((SS * S * whitelight.Ks) / d)
			+ ( CC     * whitelight.Ka), C.a);
}

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
vec4 whiteLightPhong2(vec3 P, vec3 N, vec4 C, in WhiteLight whitelight[2]) {
	vec4  R[2];

	for(int i=0; i<2; i++) {
		vec3  L  = whitelight[i].P - P;
		float d  = length(L);
		      L  = normalize(L);
		      N  = normalize(N);
		float D  = diffuse(N, L);
		float S  = specular(N, L, whitelight[i].R);
		vec3  SS = vec3(1, 1, 1);
		vec3  CC = C.rgb;

		d = d * d;

		R[i] = vec4(
			  ((CC * D * whitelight[i].Kd) / d)
			+ ((SS * S * whitelight[i].Ks) / d)
			+ ( CC     * whitelight[i].Ka), C.a);
	}

	return mix(R[0], R[1], 0.5);
}

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
vec4 whiteLightPhong4(vec3 P, vec3 N, vec4 C, in WhiteLight whitelight[4]) {
	vec4  R[4];

	for(int i=0; i<4; i++) {
		vec3  L  = whitelight[i].P - P;
		float d  = length(L);
		      L  = normalize(L);
		      N  = normalize(N);
		float D  = diffuse(N, L);
		float S  = specular(N, L, whitelight[i].R);
		vec3  SS = vec3(1, 1, 1);
		vec3  CC = C.rgb;

		d = d * d;

		R[i] = vec4(
			  ((CC * D * whitelight[i].Kd) / d)
			+ ((SS * S * whitelight[i].Ks) / d)
			+ ( CC     * whitelight[i].Ka), C.a);
	}

	return mix(mix(R[0], R[1], 0.5), mix(R[2], R[3], 0.5), 0.5);
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
vec4 singleWhiteLightPhong(in vec3 P, in vec3 N, in vec4 C, in mat3 TBN, in WhiteLight whitelight) {
	vec3  L  = whitelight.P - P;
	float d  = length(L);
	      L  = normalize(TBN * L);
	      N  = normalize(N);
	float D  = diffuse(N, L);
	float S  = specular(N, L, whitelight.R);
	vec3  CC = C.rgb;
	vec3  SS = vec3(1,1,1);

	d = d * d;

	return vec4(
		((CC * D * whitelight.Kd) / d)
	  + ((SS * S * whitelight.Ks) / d)
	  + ( CC     * whitelight.Ka), C.a);
}

// -- MATTE ------------------------------------------------

/** Compute the color of vertex at position P, with normal N with absolute color C.
  *
  * In order to work a uniform "WhiteLight whitelight;" must be declared using the
  * structure in "whiteLightStruct.glsl".
  *
  * Return: the lighted color.
  */
vec4 singleWhiteLightMatte(vec3 P, vec3 N, vec4 C, in WhiteLight whitelight) {
	vec3  L  = whitelight.P - P;
	float d  = length(L);
	      L  = normalize(L);
	      N  = normalize(N);
	float D  = diffuse(N, L);
	vec3  CC = C.rgb;

	d = d * d;

	return vec4(
			  ((CC * D * whitelight.Kd) / d)
			+ ( CC     * whitelight.Ka), C.a);
}


#endif