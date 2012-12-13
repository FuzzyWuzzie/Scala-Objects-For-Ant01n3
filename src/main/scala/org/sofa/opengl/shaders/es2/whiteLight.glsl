/** Compute the color of vertex at position P, with normal N with absolute color C.
  *
  * This is a basic goureau(if called in the vertex shader) or phong
  * (if called in the fragment shader) model.
  *
  * In order to work a uniform "WhiteLight whitelight;" must be declared using the
  * structure in "whiteLightStruct.glsl".
  *
  * Return: the lighted color.
  */
vec4 singleWhiteLight(vec3 P, vec3 N, vec4 C) {
	vec3  L = whitelight.pos - P;
	float d = length(L);
	float D = max(dot(N, L), 0.0);
	vec3  R = normalize(reflect(-L, N));
	float S = pow(max(dot(N, R), 0.0), whitelight.specular);

	d = d * d;

	vec3  SS = vec3(1, 1, 1);
	vec3  CC = C.rgb;
	float A  = C.a;

	return vec4(
			  ((CC * D * whitelight.intensity) / d)
			+ ((SS * S * whitelight.intensity) / d)
			+ ( CC * whitelight.ambient), A);
}