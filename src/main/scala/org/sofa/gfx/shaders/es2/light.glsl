#ifndef LIGHT_UTILS
#define LIGHT_UTILS


/** Compute the diffuse coefficient.
  * @param N the normal to the surface.
  * @param L the light ray.
  * @return the diffuse coefficient. */
float diffuse(inout vec3 N, inout vec3 L) {
	return max(dot(N, L), 0.0);
}


/** Compute the specular coefficient.
  * @param N the normal to the surface.
  * @param L the light ray.
  * @param roughness the roughness of the specular dot.
  * @return the specular coefficient. */
float specular(inout vec3 N, inout vec3 L, float roughness) {
	vec3 R = normalize(reflect(-L, N));
	return pow(max(dot(N, R), 0.0), roughness);
}


#endif