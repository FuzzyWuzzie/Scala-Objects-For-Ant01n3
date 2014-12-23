#ifndef COLORED_LIGHT_STRUCT
#define COLORED_LIGHT_STRUCT

struct ColoredLight {
	vec3 P;
	vec4 Cd;
	vec4 Cs;
	vec4 Ca;
	float Kd;
	float Ks;
	float Ka;
	float R;
	float Ac;
	float Al;
	float Aq;
};

#endif COLORED_LIGHT_STRUCT
