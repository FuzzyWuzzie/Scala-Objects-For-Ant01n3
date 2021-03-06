/** Structure corresponding to the SOFA HemisphereLight class. 
  *
  * The light intensity is given by the skyColor. */
struct HemisphereLight {
	vec3 P;
	vec4 skyColor;
	vec4 groundColor;
};