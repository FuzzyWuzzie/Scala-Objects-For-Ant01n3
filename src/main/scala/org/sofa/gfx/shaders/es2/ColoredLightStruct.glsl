#ifndef COLORED_LIGHT_STRUCT
#define COLORED_LIGHT_STRUCT

struct ColoredLight {
	vec3 pos;
	vec4 diffuse;
	vec4 specular;
	vec4 ambient;
	float Kd;
	float Ks;
	float Ka;
	float roughness;
	float constAtt;
	float linAtt;
	float quadAtt;
};

#endif COLORED_LIGHT_STRUCT
