vec4 hemiLightWhithSky(vec3 P, vec3 N) {
   vec3  L = normalize(hemilight.pos - P);
   float A = dot(N, L) * 0.5 + 0.5;
   
   return mix(hemilight.groundColor, hemilight.skyColor, A);
}

vec4 hemiLight(vec3 P, vec3 N, vec4 C) {
   vec3  L = normalize(hemilight.pos - P);
   float A = dot(N, L) * 0.5 + 0.5;
   
   return mix(hemilight.groundColor, color, A);
}