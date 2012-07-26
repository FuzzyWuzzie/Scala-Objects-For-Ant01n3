#version 120

attribute vec3 position;
attribute vec4 color;
attribute vec3 normal;
//attribute vec3 tangent;

uniform mat4 projection;
uniform mat4 modelview;
uniform mat3 nmodelview;

uniform vec4 lightPos;
uniform float lightIntensity;
uniform float ambientIntensity;

varying vec4 ex_Color;

void main(void) {
	vec4 p = modelview * vec4(position, 1);
	vec3 n = normalize(nmodelview * normal);
	vec3 lightDir = normalize(vec3(lightPos) - vec3(p));
	float cosAngleIncidence = clamp(dot(n, vec3(lightDir)), 0, 1);
	
	gl_Position = projection * p;
	ex_Color    = (color * cosAngleIncidence * lightIntensity) + (color * ambientIntensity);
}