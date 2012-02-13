#version 330

in vec3 vNormal;
in vec3 vLightDir;
in vec4 vColor;
in float vLightDist;

uniform float lightIntensity;
uniform float ambientIntensity;
uniform float specularPow;

out vec4 out_Color;

void main(void) {
    vec3  l = normalize(vLightDir);
	vec3  n = normalize(vNormal);
	float d = max(dot(n, l), 0);
	vec3  r = normalize(reflect(-l, n));
	float s = pow(max(dot(n, r), 0), specularPow);
	float dd = vLightDist * vLightDist;

	out_Color = ((vColor * d * lightIntensity) / dd)
	          + ((vec4(1,1,1,1) * s * lightIntensity) / dd)
	          + (vColor * ambientIntensity);
}
