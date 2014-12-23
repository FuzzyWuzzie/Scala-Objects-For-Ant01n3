/** Compute the lighted color of position P with normal N, according to
  * hemisphere light hemilight, using as colors the ground and sky colors
  * of the hemilight. */
vec4 hemiLightWhithSky(in HemisphereLight hemilight, vec3 P, vec3 N) {
   vec3  L = normalize(hemilight.P - P);
   float A = dot(N, L) * 0.5 + 0.5;
   
   return mix(hemilight.groundColor, hemilight.skyColor, A);
}

/** Compute the lighted color of position P with normal N, according to
  * hemisphere light hemilight, using as colors the ground
  * of the hemilight and a given color. */
vec4 hemiLight(in HemisphereLight hemilight, vec3 P, vec3 N, vec4 color) {
   vec3  L = normalize(hemilight.P - P);
   float A = dot(N, L) * 0.5 + 0.5;
   
   return mix(hemilight.groundColor, color, A);
}

/** Compute the lighted color of position P with normal N, according to
  * hemisphere light hemilight, using as colors given color and shadow
  * color. */
vec4 hemiLight(in HemisphereLight hemilight, vec3 P, vec3 N, vec4 color, vec4 shadow) {
   vec3  L = normalize(hemilight.P - P);
   float A = dot(N, L) * 0.5 + 0.5;
   
   return mix(shadow, color, A);
}