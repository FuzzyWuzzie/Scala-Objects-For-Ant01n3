#ifndef LIGHT_UTILS
#define LIGHT_UTILS

/** Compute the diffuse coefficient. */
float diffuse(inout vec3 N, inout vec3 L) {
	return max(dot(N, L), 0.0);
}

/** Compute the specular coefficient. */
float specular(inout vec3 N, inout vec3 L, float roughness) {
	vec3 R = normalize(reflect(-L, N));
	return pow(max(dot(N, R), 0.0), roughness);
}

#endif