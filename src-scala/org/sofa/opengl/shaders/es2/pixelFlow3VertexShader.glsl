#version 120

attribute vec3 position;
attribute vec4 color;
attribute vec3 normal;

uniform mat4 projection;
uniform mat4 modelview;
uniform mat3 nmodelview;
uniform vec4 lightDir;

uniform float lightIntensity;
uniform float ambientIntensity;

varying vec4 ex_Color;

void main(void) {
	vec4 p = projection * (modelview * vec4(position, 1));
	vec3 n = normalize(nmodelview * normal);
	float cosAngleIncidence = clamp(dot(n, vec3(lightDir)), 0, 1);
	
	gl_Position = p;
	ex_Color    = (color * cosAngleIncidence * lightIntensity) + (color * ambientIntensity);
}