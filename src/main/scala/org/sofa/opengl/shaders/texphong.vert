#version 330

layout(location=0) in vec3 position;
layout(location=2) in vec3 normal;
layout(location=4) in vec2 texCoords;

uniform mat4 MVP;
uniform mat4 MV;
uniform mat3 MV3x3;

uniform vec3 lightPos;

smooth out vec3 vNormal;
smooth out vec3 vLightDir;
smooth out float vLightDist;
smooth out vec2 vTexCoords;

void main(void) {
	vec4      p = MV * vec4(position, 1);
	vNormal     = normalize(MV3x3 * normal);
	vLightDir   = lightPos - vec3(p);
	vLightDist  = length(vLightDir);
	vLightDir   = normalize(vLightDir);
	gl_Position = MVP * vec4(position, 1);
	vTexCoords  = texCoords;
}
